package cn.org.aip.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.utils.MapstructUtils;
import cn.org.aip.common.core.utils.MessageUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.mybatis.core.page.PageQuery;
import cn.org.aip.common.mybatis.core.page.TableDataInfo;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.common.sso.constant.KeycloakConstants;
import cn.org.aip.common.sso.core.KeycloakAdminClient;
import cn.org.aip.common.sso.core.KeycloakRequestException;
import cn.org.aip.system.domain.SysApplication;
import cn.org.aip.system.domain.bo.SysApplicationBo;
import cn.org.aip.system.domain.vo.SysApplicationVo;
import cn.org.aip.system.mapper.SysApplicationMapper;
import cn.org.aip.system.service.ISysApplicationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Application configuration with a low-frequency, same-request Keycloak sync.
 * Configuration and PENDING commit before HTTP; a failed sync never rolls back saved configuration.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysApplicationServiceImpl implements ISysApplicationService {

    private static final Pattern APP_ID_PATTERN = Pattern.compile("^APP-(\\d+)$");
    private static final String APP_ID_PREFIX = "APP-";

    private final SysApplicationMapper baseMapper;
    private final KeycloakProperties keycloakProperties;
    private final KeycloakAdminClient keycloakAdminClient;
    // Uses the application's existing transaction manager / dynamic datasource.
    private final TransactionTemplate transactionTemplate;

    @Value("${keycloak.client-secret-key:aip-client-secret-aes-32byte-key}")
    private String aesKey;

    @Override
    public SysApplicationVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public SysApplicationVo queryByAppId(String appId) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysApplication>().eq(SysApplication::getAppId, appId));
    }

    @Override
    public TableDataInfo<SysApplicationVo> queryPageList(SysApplicationBo bo, PageQuery pageQuery) {
        Page<SysApplicationVo> result = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysApplicationVo> queryList(SysApplicationBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<SysApplication> buildQueryWrapper(SysApplicationBo bo) {
        LambdaQueryWrapper<SysApplication> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getAppId()), SysApplication::getAppId, bo.getAppId());
        lqw.like(StringUtils.isNotBlank(bo.getAppName()), SysApplication::getAppName, bo.getAppName());
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), SysApplication::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), SysApplication::getCategory, bo.getCategory());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysApplication::getStatus, bo.getStatus());
        lqw.orderByDesc(SysApplication::getCreateTime);
        lqw.orderByAsc(SysApplication::getId);
        return lqw;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SysApplicationVo insertByBo(SysApplicationBo bo) {
        validateUris(bo);
        String plainSecret = IdUtil.fastSimpleUUID();
        SysApplication app = transactionTemplate.execute(transaction -> {
            if (StringUtils.isBlank(bo.getAppId())) {
                bo.setAppId(generateAppId());
            }
            if (!checkAppIdUnique(bo)) {
                throw error("application.appId.exists", bo.getAppName());
            }
            SysApplication add = MapstructUtils.convert(bo, SysApplication.class);
            if (StringUtils.isBlank(add.getTenantId())) {
                add.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
            }
            add.setStatus(bo.getStatus() == null ? "0" : bo.getStatus());
            validateStatus(add, add.getStatus());
            add.setClientSecretEnc(encryptSecret(plainSecret));
            markPending(add);
            if (baseMapper.insert(add) != 1) {
                throw error("application.insert.error");
            }
            bo.setId(add.getId());
            return add;
        });
        SysApplicationVo result = attemptSync(app);
        result.setClientSecret(plainSecret);
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SysApplicationVo updateByBo(SysApplicationBo bo) {
        validateUris(bo);
        SysApplication app = transactionTemplate.execute(transaction -> {
            SysApplication existing = requireApplication(bo.getId());
            String status = bo.getStatus() == null ? existing.getStatus() : bo.getStatus();
            validateStatus(existing, status);
            // The application/client identifier and tenant cannot change after creation.
            bo.setAppId(existing.getAppId());
            bo.setTenantId(existing.getTenantId());
            SysApplication update = MapstructUtils.convert(bo, SysApplication.class);
            update.setStatus(status);
            if (bo.getPostLogoutRedirectUri() == null) {
                update.setPostLogoutRedirectUri(existing.getPostLogoutRedirectUri());
            }
            if (bo.getBackchannelLogoutUri() == null) {
                update.setBackchannelLogoutUri(existing.getBackchannelLogoutUri());
            }
            markPending(update);
            saveUpdate(update);
            return requireApplication(existing.getId());
        });
        return attemptSync(app);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SysApplicationVo updateApplicationStatus(Long id, String status) {
        SysApplication app = transactionTemplate.execute(transaction -> {
            SysApplication existing = requireApplication(id);
            validateStatus(existing, status);
            SysApplication update = new SysApplication();
            update.setId(id);
            update.setStatus(status);
            markPending(update);
            saveUpdate(update);
            return requireApplication(id);
        });
        return attemptSync(app);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SysApplicationVo syncApplication(Long id) {
        SysApplication app = transactionTemplate.execute(transaction -> {
            SysApplication existing = requireApplication(id);
            validateStatus(existing, existing.getStatus());
            SysApplication update = new SysApplication();
            update.setId(id);
            markPending(update);
            saveUpdate(update);
            return requireApplication(id);
        });
        return attemptSync(app);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String resetSecret(Long id) {
        SysApplication app = requireApplication(id);
        requireSyncAvailable();
        KeycloakAdminClient.ClientRepresentation remote = keycloakAdminClient.findClientByClientId(app.getAppId());
        if (remote == null) {
            throw error("application.sync.client.missing");
        }
        String secret = IdUtil.fastSimpleUUID();
        String encrypted = encryptSecret(secret);
        // Explicit operation only: do not rotate or enqueue a rotation during configuration retries.
        remote.setSecret(secret);
        keycloakAdminClient.updateClient(remote.getId(), remote);
        transactionTemplate.executeWithoutResult(transaction -> {
            SysApplication update = new SysApplication();
            update.setId(id);
            update.setClientSecretEnc(encrypted);
            saveUpdate(update);
        });
        return secret;
    }

    @Override
    public String getDecryptedSecret(String appId) {
        if (StringUtils.isBlank(appId)) {
            return null;
        }
        SysApplication app = baseMapper.selectOne(new LambdaQueryWrapper<SysApplication>().eq(SysApplication::getAppId, appId));
        if (app == null || StringUtils.isBlank(app.getClientSecretEnc())) {
            return null;
        }
        try {
            return decryptSecret(app.getClientSecretEnc());
        } catch (RuntimeException exception) {
            log.warn("Could not decrypt application secret for record {}", app.getId());
            return null;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        List<SysApplication> apps = ids.stream().map(this::requireApplication).toList();
        // Validate the entire batch before any destructive remote call.
        if (apps.stream().anyMatch(this::isPortal)) {
            throw error("application.portal.cannot.delete");
        }
        requireSyncAvailable();
        for (SysApplication app : apps) {
            KeycloakAdminClient.ClientRepresentation remote = keycloakAdminClient.findClientByClientId(app.getAppId());
            if (remote != null) {
                keycloakAdminClient.deleteClient(remote.getId());
            }
        }
        return transactionTemplate.execute(transaction -> baseMapper.deleteByIds(ids) > 0);
    }

    @Override
    public boolean checkAppIdUnique(SysApplicationBo bo) {
        Long count = baseMapper.countByAppId(bo.getAppId(), bo.getId());
        return count == null || count == 0;
    }

    /** Saves only a sync outcome after the configuration transaction has committed. */
    private SysApplicationVo attemptSync(SysApplication app) {
        String status = KeycloakConstants.SYNC_SUCCESS;
        String failure = "";
        try {
            syncClient(app);
        } catch (KeycloakRequestException exception) {
            status = KeycloakConstants.SYNC_FAILED;
            failure = exception.getMessage();
            log.warn("Application {} sync failed ({})", app.getId(), exception.getReason());
        } catch (RuntimeException exception) {
            status = KeycloakConstants.SYNC_FAILED;
            failure = MessageUtils.message("application.sync.failed");
            log.warn("Application {} sync failed ({})", app.getId(), exception.getClass().getSimpleName());
        }
        SysApplication outcome = new SysApplication();
        outcome.setId(app.getId());
        outcome.setSsoSyncStatus(status);
        outcome.setSsoSyncError(failure);
        try {
            transactionTemplate.executeWithoutResult(transaction -> saveUpdate(outcome));
        } catch (RuntimeException exception) {
            // The committed PENDING row remains retryable; never report a persisted SUCCESS here.
            outcome.setSsoSyncStatus(KeycloakConstants.SYNC_PENDING);
            outcome.setSsoSyncError(MessageUtils.message("application.sync.result.save.failed"));
            log.warn("Application {} sync outcome could not be saved", app.getId());
        }
        SysApplicationVo result = MapstructUtils.convert(app, SysApplicationVo.class);
        result.setSsoSyncStatus(outcome.getSsoSyncStatus());
        result.setSsoSyncError(outcome.getSsoSyncError());
        return result;
    }

    private void syncClient(SysApplication app) {
        requireSyncAvailable();
        List<String> redirects = JsonUtils.parseArray(normalizeRedirectUris(app.getRedirectUris()), String.class);
        String postLogout = normalizeOptionalUri(app.getPostLogoutRedirectUri(), "application.post.logout.uri.invalid");
        String backchannel = normalizeOptionalUri(app.getBackchannelLogoutUri(), "application.backchannel.uri.invalid");
        KeycloakAdminClient.ClientRepresentation remote = keycloakAdminClient.findClientByClientId(app.getAppId());
        boolean create = remote == null;
        if (create) {
            remote = new KeycloakAdminClient.ClientRepresentation();
            if (StringUtils.isBlank(app.getClientSecretEnc())) {
                throw new KeycloakRequestException(KeycloakRequestException.Reason.STORED_SECRET_UNAVAILABLE);
            }
            String secret;
            try {
                secret = decryptSecret(app.getClientSecretEnc());
            } catch (RuntimeException exception) {
                throw new KeycloakRequestException(KeycloakRequestException.Reason.STORED_SECRET_UNAVAILABLE);
            }
            if (StringUtils.isBlank(secret)) {
                throw new KeycloakRequestException(KeycloakRequestException.Reason.STORED_SECRET_UNAVAILABLE);
            }
            remote.setSecret(secret);
        } else {
            // A GET can contain the remote secret. Never send it during ordinary configuration sync.
            remote.setSecret(null);
        }
        remote.setClientId(app.getAppId());
        remote.setName(app.getAppName());
        remote.setDescription(app.getDescription());
        remote.setEnabled(!"1".equals(app.getStatus()));
        remote.setPublicClient(false);
        remote.setClientAuthenticatorType("client-secret");
        remote.setStandardFlowEnabled(true);
        remote.setImplicitFlowEnabled(false);
        remote.setDirectAccessGrantsEnabled(false);
        remote.setServiceAccountsEnabled(false);
        remote.setRedirectUris(redirects);
        remote.mutableAttributes().put(KeycloakConstants.ATTR_PKCE_METHOD, KeycloakConstants.PKCE_METHOD_S256);
        remote.mutableAttributes().put(KeycloakConstants.ATTR_BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true");
        remote.mutableAttributes().put(KeycloakConstants.ATTR_BACKCHANNEL_LOGOUT_REVOKE_OFFLINE, "true");
        // NULL is legacy/unowned. Empty is an intentional clear and must not activate fallback.
        if (postLogout != null) {
            remote.mutableAttributes().put(KeycloakConstants.ATTR_POST_LOGOUT_REDIRECT_URIS,
                postLogout.isEmpty() ? KeycloakConstants.POST_LOGOUT_REDIRECT_DENY : postLogout);
        }
        if (backchannel != null) {
            remote.mutableAttributes().put(KeycloakConstants.ATTR_BACKCHANNEL_LOGOUT_URL, backchannel);
        }
        if (create) {
            keycloakAdminClient.createClient(remote);
        } else {
            keycloakAdminClient.updateClient(remote.getId(), remote);
        }
    }

    private void requireSyncAvailable() {
        if (!keycloakProperties.isEnabled()) {
            throw new KeycloakRequestException(KeycloakRequestException.Reason.DISABLED);
        }
        if (StringUtils.isBlank(keycloakProperties.getAdminClientSecret())) {
            throw new KeycloakRequestException(KeycloakRequestException.Reason.ADMIN_UNCONFIGURED);
        }
    }

    private void markPending(SysApplication app) {
        app.setSsoSyncStatus(KeycloakConstants.SYNC_PENDING);
        app.setSsoSyncError("");
    }

    private SysApplication requireApplication(Long id) {
        if (id == null) {
            throw error("application.id.required");
        }
        SysApplication app = baseMapper.selectById(id);
        if (app == null) {
            throw error("application.not.exists");
        }
        return app;
    }

    private void saveUpdate(SysApplication update) {
        if (baseMapper.updateById(update) != 1) {
            throw error("application.update.error");
        }
    }

    private boolean isPortal(SysApplication app) {
        return TenantConstants.DEFAULT_TENANT_ID.equals(app.getAppId())
            || keycloakProperties.getClientId().equals(app.getAppId());
    }

    private void validateStatus(SysApplication app, String status) {
        if (!"0".equals(status) && !"1".equals(status)) {
            throw error("application.status.invalid");
        }
        if (isPortal(app) && "1".equals(status)) {
            throw error("application.portal.cannot.disable");
        }
    }

    private void validateUris(SysApplicationBo bo) {
        bo.setRedirectUris(normalizeRedirectUris(bo.getRedirectUris()));
        bo.setPostLogoutRedirectUri(normalizeOptionalUri(bo.getPostLogoutRedirectUri(), "application.post.logout.uri.invalid"));
        bo.setBackchannelLogoutUri(normalizeOptionalUri(bo.getBackchannelLogoutUri(), "application.backchannel.uri.invalid"));
    }

    private String normalizeOptionalUri(String value, String messageKey) {
        if (value == null) {
            return null;
        }
        if (value.length() > 500) {
            throw error(messageKey);
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            validateHttpUri(trimmed, messageKey);
        }
        return trimmed;
    }

    /** Accepts JSON arrays or legacy newline/comma lists; malformed JSON never falls back. */
    private String normalizeRedirectUris(String raw) {
        String messageKey = "application.redirect.uris.invalid";
        if (StringUtils.isBlank(raw)) {
            throw error(messageKey);
        }
        String trimmed = raw.trim();
        List<String> values = new ArrayList<>();
        if (trimmed.startsWith("[")) {
            try {
                JsonNode array = JsonUtils.getObjectMapper().reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(trimmed);
                if (!array.isArray()) {
                    throw error(messageKey);
                }
                for (JsonNode value : array) {
                    if (!value.isTextual()) {
                        throw error(messageKey);
                    }
                    values.add(value.textValue().trim());
                }
            } catch (Exception exception) {
                throw error(messageKey);
            }
        } else {
            for (String value : trimmed.split("[\\r\\n,]+")) {
                values.add(value.trim());
            }
        }
        if (values.isEmpty()) {
            throw error(messageKey);
        }
        values.forEach(value -> validateHttpUri(value, messageKey));
        return JsonUtils.toJsonString(values.stream().distinct().toList());
    }

    private void validateHttpUri(String value, String messageKey) {
        try {
            URI uri = new URI(value);
            if (value.length() > 500 || value.contains("*") || value.toLowerCase(java.util.Locale.ROOT).contains("%2a")
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                || uri.getPort() > 65535 || uri.getPort() == 0) {
                throw error(messageKey);
            }
        } catch (URISyntaxException exception) {
            throw error(messageKey);
        }
    }

    /** Generates the existing human-readable application code convention. */
    private synchronized String generateAppId() {
        List<String> appIds = baseMapper.selectAllAppIds();
        if (appIds == null) {
            appIds = Collections.emptyList();
        }
        Set<String> appIdSet = new HashSet<>(appIds);
        long maxNum = 0;
        for (String id : appIds) {
            if (StringUtils.isBlank(id)) {
                continue;
            }
            Matcher matcher = APP_ID_PATTERN.matcher(id.trim());
            if (matcher.matches()) {
                try {
                    maxNum = Math.max(maxNum, Long.parseLong(matcher.group(1)));
                } catch (NumberFormatException ignored) {
                    // Nonconforming historical identifiers do not participate in numbering.
                }
            }
        }
        long nextNum = maxNum + 1;
        String candidate = String.format("%s%03d", APP_ID_PREFIX, nextNum);
        while (appIdSet.contains(candidate)) {
            candidate = String.format("%s%03d", APP_ID_PREFIX, ++nextNum);
        }
        return candidate;
    }

    private ServiceException error(String key, Object... arguments) {
        return new ServiceException(MessageUtils.message(key, arguments));
    }

    private String encryptSecret(String plainSecret) {
        return SecureUtil.aes(aesKey.getBytes(StandardCharsets.UTF_8)).encryptBase64(plainSecret, StandardCharsets.UTF_8);
    }

    private String decryptSecret(String encSecret) {
        return SecureUtil.aes(aesKey.getBytes(StandardCharsets.UTF_8)).decryptStr(encSecret, StandardCharsets.UTF_8);
    }
}
