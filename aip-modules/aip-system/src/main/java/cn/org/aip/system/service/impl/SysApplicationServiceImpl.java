package cn.org.aip.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.constant.Constants;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.utils.MapstructUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.mybatis.core.page.PageQuery;
import cn.org.aip.common.mybatis.core.page.TableDataInfo;
import cn.org.aip.common.sso.config.KeycloakProperties;
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
import java.util.Collection;
import java.util.List;

/**
 * 应用注册 Service 业务层处理
 *
 * @author Michelle.Chung
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysApplicationServiceImpl implements ISysApplicationService {

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
    public SysApplicationVo queryByTenantId(String tenantId) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysApplication>().eq(SysApplication::getTenantId, tenantId));
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
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), SysApplication::getTenantId, bo.getTenantId());
        lqw.like(StringUtils.isNotBlank(bo.getAppId()), SysApplication::getAppId, bo.getAppId());
        lqw.like(StringUtils.isNotBlank(bo.getAppName()), SysApplication::getAppName, bo.getAppName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysApplication::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getClientAuthMethod()), SysApplication::getClientAuthMethod, bo.getClientAuthMethod());
        lqw.orderByAsc(SysApplication::getOrderNum);
        lqw.orderByDesc(SysApplication::getCreateTime);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysApplicationVo insertByBo(SysApplicationBo bo) {
        if (StringUtils.isBlank(bo.getAppId())) {
            bo.setAppId(bo.getTenantId());
        }
        if (StringUtils.isBlank(bo.getClientAuthMethod())) {
            bo.setClientAuthMethod("client_secret_basic");
        }
        if (StringUtils.isBlank(bo.getScopes())) {
            bo.setScopes("openid,profile,email");
        }
        if (StringUtils.isBlank(bo.getGrantTypes())) {
            bo.setGrantTypes("authorization_code,refresh_token");
        }
        if (bo.getAccessTokenTtlMinutes() == null) {
            bo.setAccessTokenTtlMinutes(30);
        }
        if (bo.getRefreshTokenTtlDays() == null) {
            bo.setRefreshTokenTtlDays(7);
        }
        if (StringUtils.isBlank(bo.getRequirePkce())) {
            bo.setRequirePkce("1");
        }

        String plainSecret = null;
        if (!"none".equalsIgnoreCase(bo.getClientAuthMethod())) {
            plainSecret = StringUtils.isNotBlank(bo.getClientSecret()) ? bo.getClientSecret() : IdUtil.fastSimpleUUID();
        }

        SysApplication add = MapstructUtils.convert(bo, SysApplication.class);
        if (StringUtils.isNotBlank(plainSecret)) {
            add.setClientSecretEnc(encryptSecret(plainSecret));
        }

        boolean flag = baseMapper.insert(add) > 0;
        if (!flag) {
            throw new ServiceException("新增应用失败");
        }
        bo.setId(add.getId());
        syncCreateKeycloakClient(bo, plainSecret);
        SysApplicationVo vo = MapstructUtils.convert(add, SysApplicationVo.class);
        if (vo != null && plainSecret != null) {
            vo.setClientSecret(plainSecret);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysApplicationBo bo) {
        SysApplication existing = baseMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("应用不存在");
        }
        SysApplication update = MapstructUtils.convert(bo, SysApplication.class);
        if (StringUtils.isNotBlank(bo.getClientSecret())) {
            update.setClientSecretEnc(encryptSecret(bo.getClientSecret()));
        } else {
            update.setClientSecretEnc(existing.getClientSecretEnc());
        }
        boolean success = baseMapper.updateById(update) > 0;
        if (success) {
            syncUpdateKeycloakClient(existing, bo);
        }
        return success;
    }

    @Override
    public int updateApplicationStatus(Long id, String status) {
        SysApplication app = baseMapper.selectById(id);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        if (TenantConstants.DEFAULT_TENANT_ID.equals(app.getTenantId()) && "1".equals(status)) {
            throw new ServiceException("不允许停用集成平台门户应用");
        }
        SysApplication update = new SysApplication();
        update.setId(id);
        update.setStatus(status);
        return baseMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetSecret(Long id) {
        SysApplication app = baseMapper.selectById(id);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        String newSecret = IdUtil.fastSimpleUUID();
        SysApplication update = new SysApplication();
        update.setId(id);
        update.setClientSecretEnc(encryptSecret(newSecret));
        baseMapper.updateById(update);
        syncResetKeycloakSecret(app.getAppId());
        return newSecret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        for (Long id : ids) {
            SysApplication app = baseMapper.selectById(id);
            if (app != null && TenantConstants.DEFAULT_TENANT_ID.equals(app.getTenantId())) {
                throw new ServiceException("不允许删除集成平台门户应用");
            }
            if (app != null) {
                syncDeleteKeycloakClient(app.getAppId());
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private void syncCreateKeycloakClient(SysApplicationBo bo, String plainSecret) {
        if (!keycloakProperties.isEnabled() || StringUtils.isBlank(keycloakProperties.getAdminClientSecret())) {
            return;
        }
        try {
            KeycloakAdminClient.ClientRepresentation representation = new KeycloakAdminClient.ClientRepresentation();
            representation.setClientId(bo.getAppId());
            representation.setName(bo.getAppName());
            representation.setEnabled(!"1".equals(bo.getStatus()));
            representation.setPublicClient("none".equalsIgnoreCase(bo.getClientAuthMethod()));
            representation.setSecret(plainSecret);
            if (StringUtils.isNotBlank(bo.getRedirectUris())) {
                representation.setRedirectUris(JsonUtils.parseArray(bo.getRedirectUris(), String.class));
            }
            if (StringUtils.isNotBlank(bo.getPostLogoutRedirectUris())) {
                representation.getAttributes().put("post.logout.redirect.uris", bo.getPostLogoutRedirectUris());
            }
            if ("1".equals(bo.getRequirePkce())) {
                representation.getAttributes().put("pkce.code.challenge.method", "S256");
            }
            representation.getAttributes().put("backchannel.logout.session.required", "true");
            representation.getAttributes().put("backchannel.logout.revoke.offline.tokens", "true");
            keycloakAdminClient.createClient(representation);
        } catch (Exception e) {
            log.error("Failed to sync client creation to Keycloak: {}", e.getMessage(), e);
            throw new ServiceException("同步 Keycloak Client 失败: " + e.getMessage());
        }
    }

    private void syncUpdateKeycloakClient(SysApplication existing, SysApplicationBo bo) {
        if (!keycloakProperties.isEnabled() || StringUtils.isBlank(keycloakProperties.getAdminClientSecret())) {
            return;
        }
        try {
            KeycloakAdminClient.ClientRepresentation kcClient = keycloakAdminClient.findClientByClientId(existing.getAppId());
            if (kcClient != null) {
                kcClient.setName(bo.getAppName());
                kcClient.setEnabled(!"1".equals(bo.getStatus()));
                kcClient.setPublicClient("none".equalsIgnoreCase(bo.getClientAuthMethod()));
                if (StringUtils.isNotBlank(bo.getRedirectUris())) {
                    kcClient.setRedirectUris(JsonUtils.parseArray(bo.getRedirectUris(), String.class));
                }
                if (StringUtils.isNotBlank(bo.getClientSecret())) {
                    kcClient.setSecret(bo.getClientSecret());
                }
                keycloakAdminClient.updateClient(kcClient.getId(), kcClient);
            }
        } catch (Exception e) {
            log.error("Failed to sync client update to Keycloak: {}", e.getMessage(), e);
            throw new ServiceException("同步更新 Keycloak Client 失败: " + e.getMessage());
        }
    }

    private void syncResetKeycloakSecret(String appId) {
        if (!keycloakProperties.isEnabled() || StringUtils.isBlank(keycloakProperties.getAdminClientSecret())) {
            return;
        }
        try {
            KeycloakAdminClient.ClientRepresentation kcClient = keycloakAdminClient.findClientByClientId(appId);
            if (kcClient != null) {
                keycloakAdminClient.regenerateClientSecret(kcClient.getId());
            }
        } catch (Exception e) {
            log.error("Failed to sync secret reset to Keycloak: {}", e.getMessage(), e);
            throw new ServiceException("同步重置 Keycloak Client 密钥失败: " + e.getMessage());
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
            throw new ServiceException("同步删除 Keycloak Client 失败: " + e.getMessage());
        }
    }

    @Override
    public boolean checkAppIdUnique(SysApplicationBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysApplication>()
            .eq(SysApplication::getAppId, bo.getAppId())
            .ne(ObjectUtil.isNotNull(bo.getId()), SysApplication::getId, bo.getId()));
        return !exist;
    }

    @Override
    public boolean checkTenantIdUnique(SysApplicationBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysApplication>()
            .eq(SysApplication::getTenantId, bo.getTenantId())
            .ne(ObjectUtil.isNotNull(bo.getId()), SysApplication::getId, bo.getId()));
        return !exist;
    }

    private String encryptSecret(String plainSecret) {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        return SecureUtil.aes(keyBytes).encryptBase64(plainSecret, StandardCharsets.UTF_8);
    }

}
