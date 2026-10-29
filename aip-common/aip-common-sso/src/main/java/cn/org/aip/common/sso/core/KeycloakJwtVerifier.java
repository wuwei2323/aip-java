package cn.org.aip.common.sso.core;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.sso.config.KeycloakProperties;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Keycloak OIDC JWT 与 LogoutToken 本地验签器 (基于 Nimbus)
 *
 * @author Michelle.Chung
 */
@Slf4j
public class KeycloakJwtVerifier {

    public static final String BACKCHANNEL_LOGOUT_EVENT = "http://schemas.openid.net/event/backchannel-logout";

    private final KeycloakProperties properties;
    private final JWKSource<SecurityContext> jwkSource;

    public KeycloakJwtVerifier(KeycloakProperties properties) {
        this.properties = properties;
        this.jwkSource = createDefaultJwkSource(properties.getJwksUrl());
    }

    public KeycloakJwtVerifier(KeycloakProperties properties, JWKSource<SecurityContext> jwkSource) {
        this.properties = properties;
        this.jwkSource = jwkSource;
    }

    private static JWKSource<SecurityContext> createDefaultJwkSource(String jwksUrl) {
        try {
            return JWKSourceBuilder.create(new URL(jwksUrl))
                .cache(true)
                .rateLimited(false)
                .build();
        } catch (MalformedURLException e) {
            log.error("Invalid Keycloak JWKS URL: {}", jwksUrl, e);
            throw new ServiceException("Keycloak JWKS URL 配置错误");
        }
    }

    /**
     * 校验 ID Token / Access Token
     *
     * @param tokenString   JWT 字符串
     * @param expectedAud   期望的 Audience (客户端ID)，为空时不校验
     * @param expectedNonce 期望的 Nonce，为空时不校验
     * @return 校验通过的 Claims
     */
    public JWTClaimsSet verifyToken(String tokenString, String expectedAud, String expectedNonce) {
        if (StringUtils.isBlank(tokenString)) {
            throw new ServiceException("Token 不能为空");
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(tokenString);
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWSAlgorithm expectedJWSAlg = signedJWT.getHeader().getAlgorithm();
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, jwkSource);
            jwtProcessor.setJWSKeySelector(keySelector);

            JWTClaimsSet claims = jwtProcessor.process(signedJWT, null);

            // 1. 校验发行者 (iss)
            String issuer = claims.getIssuer();
            String expectedIssuer = properties.getIssuer();
            if (!StringUtils.equals(issuer, expectedIssuer)) {
                log.warn("JWT issuer mismatch: expected={}, actual={}", expectedIssuer, issuer);
                throw new ServiceException("Token 发行方不匹配");
            }

            // 2. 校验过期时间 (exp) 允许时钟偏差
            Date expirationTime = claims.getExpirationTime();
            long clockSkew = properties.getClockSkewSeconds() * 1000;
            long now = System.currentTimeMillis();
            if (expirationTime != null && (now - clockSkew) > expirationTime.getTime()) {
                log.warn("JWT expired: exp={}, now={}", expirationTime, new Date(now));
                throw new ServiceException("Token 已过期");
            }

            // 3. 校验生效时间 (nbf)
            Date notBeforeTime = claims.getNotBeforeTime();
            if (notBeforeTime != null && (now + clockSkew) < notBeforeTime.getTime()) {
                log.warn("JWT not yet valid: nbf={}, now={}", notBeforeTime, new Date(now));
                throw new ServiceException("Token 尚未生效");
            }

            // 4. 校验受众 (aud) 或 azp
            if (StringUtils.isNotBlank(expectedAud)) {
                List<String> audience = claims.getAudience();
                String azp = (String) claims.getClaim("azp");
                boolean audMatch = audience != null && audience.contains(expectedAud);
                boolean azpMatch = StringUtils.equals(azp, expectedAud);
                if (!audMatch && !azpMatch) {
                    log.warn("JWT audience mismatch: expectedAud={}, actualAud={}, azp={}", expectedAud, audience, azp);
                    throw new ServiceException("Token 受众不匹配");
                }
            }

            // 5. 校验 Nonce (ID Token 防重放)
            if (StringUtils.isNotBlank(expectedNonce)) {
                String nonce = (String) claims.getClaim("nonce");
                if (!StringUtils.equals(nonce, expectedNonce)) {
                    log.warn("JWT nonce mismatch: expected={}, actual={}", expectedNonce, nonce);
                    throw new ServiceException("Token nonce 校验失败");
                }
            }

            return claims;
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            log.error("JWT verification failed: {}", e.getMessage(), e);
            throw new ServiceException("Token 验签失败: " + e.getMessage());
        }
    }

    /**
     * 校验 OIDC Back-Channel Logout Token
     *
     * @param logoutTokenString Logout Token 字符串
     * @param expectedAud       期望的客户端ID
     * @return 校验通过的 Claims (含 sid / sub)
     */
    public JWTClaimsSet verifyLogoutToken(String logoutTokenString, String expectedAud) {
        if (StringUtils.isBlank(logoutTokenString)) {
            throw new ServiceException("Logout Token 不能为空");
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(logoutTokenString);
            JWSHeader header = signedJWT.getHeader();
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(header.getAlgorithm(), jwkSource);

            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(keySelector);

            JWTClaimsSet claims = jwtProcessor.process(signedJWT, null);

            // 1. 校验发行者
            String issuer = claims.getIssuer();
            if (!StringUtils.equals(issuer, properties.getIssuer())) {
                throw new ServiceException("Logout Token 发行方不匹配");
            }

            // 2. 校验受众
            if (StringUtils.isNotBlank(expectedAud)) {
                List<String> audience = claims.getAudience();
                if (audience == null || !audience.contains(expectedAud)) {
                    throw new ServiceException("Logout Token 受众不匹配");
                }
            }

            // 3. 规范要求：logout_token 严禁包含 nonce
            if (claims.getClaim("nonce") != null) {
                throw new ServiceException("Logout Token 违背规范包含 nonce");
            }

            // 4. 规范要求：必须包含 events 且含有 http://schemas.openid.net/event/backchannel-logout
            Object eventsObj = claims.getClaim("events");
            if (!(eventsObj instanceof Map)) {
                throw new ServiceException("Logout Token 缺少 events 声明");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> events = (Map<String, Object>) eventsObj;
            if (!events.containsKey(BACKCHANNEL_LOGOUT_EVENT)) {
                throw new ServiceException("Logout Token events 缺少 backchannel-logout 声明");
            }

            // 5. 规范要求：必须包含 sub 或 sid (至少一个)
            String sub = claims.getSubject();
            String sid = (String) claims.getClaim("sid");
            if (StringUtils.isBlank(sub) && StringUtils.isBlank(sid)) {
                throw new ServiceException("Logout Token 缺少 sub 与 sid 标识");
            }

            // 6. 校验 iat (防止重放历史旧登出)
            Date issueTime = claims.getIssueTime();
            long clockSkew = properties.getClockSkewSeconds() * 1000;
            long now = System.currentTimeMillis();
            if (issueTime != null && (issueTime.getTime() - clockSkew) > now) {
                throw new ServiceException("Logout Token 签发时间异常");
            }

            return claims;
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            log.error("Logout Token verification failed: {}", e.getMessage(), e);
            throw new ServiceException("Logout Token 验签失败: " + e.getMessage());
        }
    }

}
