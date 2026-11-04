package cn.org.aip.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import cn.org.aip.system.domain.SysApplication;
import cn.org.aip.system.domain.bo.SysApplicationBo;
import cn.org.aip.system.domain.vo.SysApplicationVo;
import cn.org.aip.system.mapper.SysApplicationMapper;
import cn.org.aip.system.service.ISysApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 应用管理 Service 业务层处理
 *
 * @author Michelle.Chung
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
        LambdaQueryWrapper<SysApplication> lqw = buildQueryWrapper(bo);
        Page<SysApplicationVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysApplicationVo> queryList(SysApplicationBo bo) {
        LambdaQueryWrapper<SysApplication> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
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
    @Transactional(rollbackFor = Exception.class)
    public SysApplicationVo insertByBo(SysApplicationBo bo) {
        if (StringUtils.isBlank(bo.getAppId())) {
            bo.setAppId(generateAppId());
        }
        if (StringUtils.isBlank(bo.getTenantId())) {
            bo.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
        }
        if (!checkAppIdUnique(bo)) {
            throw new ServiceException(MessageUtils.message("application.appId.exists", bo.getAppName()));
        }
        bo.setRedirectUris(normalizeRedirectUris(bo.getRedirectUris()));

        // 统一自动生成高强度客户端密钥
        String plainSecret = IdUtil.fastSimpleUUID();
        SysApplication add = MapstructUtils.convert(bo, SysApplication.class);
        if (StringUtils.isBlank(add.getTenantId())) {
            add.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
        }
        add.setClientSecretEnc(encryptSecret(plainSecret));

        boolean flag = baseMapper.insert(add) > 0;
        if (!flag) {
            throw new ServiceException(MessageUtils.message("application.insert.error"));
        }
        bo.setId(add.getId());
        syncCreateKeycloakClient(add, plainSecret);

        SysApplicationVo vo = MapstructUtils.convert(add, SysApplicationVo.class);
        if (vo != null) {
            vo.setClientSecret(plainSecret);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysApplicationBo bo) {
        SysApplication existing = baseMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException(MessageUtils.message("application.not.exists"));
        }
        // 应用编码与租户编号创建后不可修改
        bo.setAppId(existing.getAppId());
        bo.setTenantId(existing.getTenantId() != null ? existing.getTenantId() : TenantConstants.DEFAULT_TENANT_ID);
        bo.setRedirectUris(normalizeRedirectUris(bo.getRedirectUris()));

        SysApplication update = MapstructUtils.convert(bo, SysApplication.class);
        update.setTenantId(existing.getTenantId() != null ? existing.getTenantId() : TenantConstants.DEFAULT_TENANT_ID);
        update.setClientSecretEnc(existing.getClientSecretEnc());

        boolean success = baseMapper.updateById(update) > 0;
        if (success) {
            syncUpdateKeycloakClient(update);
        }
        return success;
    }

    @Override
    public int updateApplicationStatus(Long id, String status) {
        SysApplication app = baseMapper.selectById(id);
        if (app == null) {
            throw new ServiceException(MessageUtils.message("application.not.exists"));
        }
        if (TenantConstants.DEFAULT_TENANT_ID.equals(app.getAppId()) && "1".equals(status)) {
            throw new ServiceException(MessageUtils.message("application.portal.cannot.disable"));
        }
        SysApplication update = new SysApplication();
        update.setId(id);
        update.setStatus(status);
        int rows = baseMapper.updateById(update);
        if (rows > 0) {
            syncUpdateKeycloakStatus(app.getAppId(), !"1".equals(status));
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetSecret(Long id) {
        SysApplication app = baseMapper.selectById(id);
        if (app == null) {
            throw new ServiceException(MessageUtils.message("application.not.exists"));
        }
        String newSecret = IdUtil.fastSimpleUUID();
        SysApplication update = new SysApplication();
        update.setId(id);
        update.setClientSecretEnc(encryptSecret(newSecret));
        baseMapper.updateById(update);
        syncResetKeycloakSecret(app.getAppId(), newSecret);
        return newSecret;
    }

    @Override
    public String getDecryptedSecret(String appId) {
        if (StringUtils.isBlank(appId)) {
            return null;
        }
        SysApplication app = baseMapper.selectOne(
            new LambdaQueryWrapper<SysApplication>().eq(SysApplication::getAppId, appId)
        );
        if (app == null || StringUtils.isBlank(app.getClientSecretEnc())) {
            return null;
        }
        try {
            return decryptSecret(app.getClientSecretEnc());
        } catch (Exception e) {
            log.warn("Failed to decrypt secret for app {}: {}", appId, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        for (Long id : ids) {
            SysApplication app = baseMapper.selectById(id);
            if (app != null && TenantConstants.DEFAULT_TENANT_ID.equals(app.getAppId())) {
                throw new ServiceException(MessageUtils.message("application.portal.cannot.delete"));
            }
            if (app != null) {
                syncDeleteKeycloakClient(app.getAppId());
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public boolean checkAppIdUnique(SysApplicationBo bo) {
        Long count = baseMapper.countByAppId(bo.getAppId(), bo.getId());
        return count == null || count == 0;
    }

    /**
     * 自动生成规律递增的应用编码 (格式: APP-001, APP-002, ...)
     */
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
                    long num = Long.parseLong(matcher.group(1));
                    if (num > maxNum) {
                        maxNum = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        long nextNum = maxNum + 1;
        String candidate = String.format("%s%03d", APP_ID_PREFIX, nextNum);
        while (appIdSet.contains(candidate)) {
            nextNum++;
            candidate = String.format("%s%03d", APP_ID_PREFIX, nextNum);
        }
        return candidate;
    }

    /**
     * 规范化重定向回调 URI 列表为 JSON 字符串
     */
    private String normalizeRedirectUris(String rawRedirectUris) {
        if (StringUtils.isBlank(rawRedirectUris)) {
            throw new ServiceException(MessageUtils.message("application.redirect.uris.invalid"));
        }
        List<String> list = new ArrayList<>();
        String trimmed = rawRedirectUris.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                list = JsonUtils.parseArray(trimmed, String.class);
            } catch (Exception ignored) {
            }
        }
        if (CollUtil.isEmpty(list)) {
            String[] split = trimmed.split("[\r\n,]+");
            for (String s : split) {
                if (StringUtils.isNotBlank(s)) {
                    list.add(s.trim());
                }
            }
        }
        list = list.stream().filter(StringUtils::isNotBlank).distinct().toList();
        if (CollUtil.isEmpty(list)) {
            throw new ServiceException(MessageUtils.message("application.redirect.uris.invalid"));
        }
        return JsonUtils.toJsonString(list);
    }

    private void syncCreateKeycloakClient(SysApplication app, String plainSecret) {
        if (!keycloakProperties.isEnabled() || StringUtils.isBlank(keycloakProperties.getAdminClientSecret())) {
            return;
        }
        try {
            KeycloakAdminClient.ClientRepresentation representation = new KeycloakAdminClient.ClientRepresentation();
            representation.setClientId(app.getAppId());
            representation.setName(app.getAppName());
            representation.setDescription(app.getDescription());
            representation.setEnabled(!"1".equals(app.getStatus()));
            representation.setPublicClient(false);
            representation.setClientAuthenticatorType("client-secret");
            representation.setSecret(plainSecret);
            representation.setStandardFlowEnabled(true);
            representation.setImplicitFlowEnabled(false);
            representation.setDirectAccessGrantsEnabled(false);
            representation.setServiceAccountsEnabled(false);

            representation.getAttributes().put(KeycloakConstants.ATTR_PKCE_METHOD, KeycloakConstants.PKCE_METHOD_S256);
            representation.getAttributes().put(KeycloakConstants.ATTR_BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true");
            representation.getAttributes().put(KeycloakConstants.ATTR_BACKCHANNEL_LOGOUT_REVOKE_OFFLINE, "true");

            if (StringUtils.isNotBlank(app.getRedirectUris())) {
                representation.setRedirectUris(JsonUtils.parseArray(app.getRedirectUris(), String.class));
            }

            KeycloakAdminClient.ClientRepresentation existing = keycloakAdminClient.findClientByClientId(app.getAppId());
            if (existing != null) {
                representation.setId(existing.getId());
                keycloakAdminClient.updateClient(existing.getId(), representation);
            } else {
                keycloakAdminClient.createClient(representation);
            }
        } catch (Exception e) {
            log.error("Failed to sync client creation to Keycloak: {}", e.getMessage(), e);
            throw new ServiceException(MessageUtils.message("application.sync.keycloak.create.error", e.getMessage()));
        }
    }

    private void syncUpdateKeycloakClient(SysApplication app) {
        if (!keycloakProperties.isEnabled() || StringUtils.isBlank(keycloakProperties.getAdminClientSecret())) {
            return;
        }
        try {
            KeycloakAdminClient.ClientRepresentation kcClient = keycloakAdminClient.findClientByClientId(app.getAppId());
            if (kcClient != null) {
                kcClient.setName(app.getAppName());
                kcClient.setDescription(app.getDescription());
                kcClient.setEnabled(!"1".equals(app.getStatus()));
                kcClient.setPublicClient(false);
                if (StringUtils.isNotBlank(app.getRedirectUris())) {
                    kcClient.setRedirectUris(JsonUtils.parseArray(app.getRedirectUris(), String.class));
                }
                kcClient.getAttributes().put(KeycloakConstants.ATTR_PKCE_METHOD, KeycloakConstants.PKCE_METHOD_S256);
                keycloakAdminClient.updateClient(kcClient.getId(), kcClient);
            } else {
                // Keycloak 侧若缺失客户端则按同一全局规则补建
                String secret = getDecryptedSecret(app.getAppId());
                if (StringUtils.isBlank(secret)) {
                    secret = IdUtil.fastSimpleUUID();
                }
                syncCreateKeycloakClient(app, secret);
            }
        } catch (Exception e) {
            log.error("Failed to sync client update to Keycloak: {}", e.getMessage(), e);
            throw new ServiceException(MessageUtils.message("application.sync.keycloak.update.error", e.getMessage()));
        }
    }

    private void syncUpdateKeycloakStatus(String appId, boolean enabled) {
        if (!keycloakProperties.isEnabled() || StringUtils.isBlank(keycloakProperties.getAdminClientSecret())) {
            return;
        }
        try {
            KeycloakAdminClient.ClientRepresentation kcClient = keycloakAdminClient.findClientByClientId(appId);
            if (kcClient != null) {
                kcClient.setEnabled(enabled);
                keycloakAdminClient.updateClient(kcClient.getId(), kcClient);
            }
        } catch (Exception e) {
            log.error("Failed to sync status update to Keycloak: {}", e.getMessage(), e);
            throw new ServiceException(MessageUtils.message("application.sync.keycloak.update.error", e.getMessage()));
        }
    }

    private void syncResetKeycloakSecret(String appId, String newSecret) {
        if (!keycloakProperties.isEnabled() || StringUtils.isBlank(keycloakProperties.getAdminClientSecret())) {
            return;
        }
        try {
            KeycloakAdminClient.ClientRepresentation kcClient = keycloakAdminClient.findClientByClientId(appId);
            if (kcClient != null) {
                kcClient.setSecret(newSecret);
                keycloakAdminClient.updateClient(kcClient.getId(), kcClient);
            }
        } catch (Exception e) {
            log.error("Failed to sync secret reset to Keycloak: {}", e.getMessage(), e);
            throw new ServiceException(MessageUtils.message("application.sync.keycloak.reset.error", e.getMessage()));
        }
    }

    private void syncDeleteKeycloakClient(String appId) {
        if (!keycloakProperties.isEnabled() || StringUtils.isBlank(keycloakProperties.getAdminClientSecret())) {
            return;
        }
        try {
            KeycloakAdminClient.ClientRepresentation kcClient = keycloakAdminClient.findClientByClientId(appId);
            if (kcClient != null) {
                keycloakAdminClient.deleteClient(kcClient.getId());
            }
        } catch (Exception e) {
            log.error("Failed to delete Keycloak client: {}", e.getMessage(), e);
            throw new ServiceException(MessageUtils.message("application.sync.keycloak.delete.error", e.getMessage()));
        }
    }

    private String encryptSecret(String plainSecret) {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        return SecureUtil.aes(keyBytes).encryptBase64(plainSecret, StandardCharsets.UTF_8);
    }

    private String decryptSecret(String encSecret) {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        return SecureUtil.aes(keyBytes).decryptStr(encSecret, StandardCharsets.UTF_8);
    }

}
