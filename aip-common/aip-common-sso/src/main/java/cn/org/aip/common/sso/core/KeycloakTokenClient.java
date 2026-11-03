package cn.org.aip.common.sso.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.utils.MessageUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.sso.config.KeycloakProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.Serializable;

/**
 * Keycloak OIDC 令牌客户端 (授权码兑换与凭证换票)
 *
 * @author Michelle.Chung
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakTokenClient {

    private final KeycloakProperties properties;
    private final RestClient restClient = RestClient.create();

    /**
     * 授权码换取 Token (支持 PKCE)
     *
     * @param code         Keycloak 返回的授权码
     * @param codeVerifier PKCE 校验凭据
     * @param redirectUri  重定向地址
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥 (可为空)
     * @return 令牌响应
     */
    public OidcTokenResponse exchangeCode(String code, String codeVerifier, String redirectUri,
                                         String clientId, String clientSecret) {
        String tokenUrl = properties.getTokenUrl();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);
        formData.add("client_id", clientId);
        if (StringUtils.isNotBlank(clientSecret)) {
            formData.add("client_secret", clientSecret);
        }
        if (StringUtils.isNotBlank(codeVerifier)) {
            formData.add("code_verifier", codeVerifier);
        }

        try {
            String responseBody = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(String.class);

            return JsonUtils.parseObject(responseBody, OidcTokenResponse.class);
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            log.error("Failed to exchange code at {}: {}", tokenUrl, e.getMessage(), e);
            throw new ServiceException(MessageUtils.message("sso.token.client.exchange.error", e.getMessage()));
        }
    }

    /**
     * 获取管理台 Client Credentials Token
     *
     * @param clientId     Admin 客户端ID
     * @param clientSecret Admin 客户端密钥
     * @return Access Token
     */
    public String getClientCredentialsToken(String clientId, String clientSecret) {
        String tokenUrl = properties.getTokenUrl();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        try {
            String responseBody = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(String.class);

            OidcTokenResponse response = JsonUtils.parseObject(responseBody, OidcTokenResponse.class);
            if (response == null || StringUtils.isBlank(response.getAccessToken())) {
                throw new ServiceException(MessageUtils.message("sso.token.client.admin.token.empty"));
            }
            return response.getAccessToken();
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            log.error("Failed to get client_credentials token at {}: {}", tokenUrl, e.getMessage(), e);
            throw new ServiceException(MessageUtils.message("sso.token.client.admin.token.error", e.getMessage()));
        }
    }

    /**
     * 调用 Keycloak Admin API 删除指定会话 (彻底销毁 Keycloak 服务端用户会话)
     *
     * @param sessionId Keycloak 用户会话 ID (sid)
     */
    public void deleteSession(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return;
        }
        try {
            String adminToken = getClientCredentialsToken(properties.getAdminClientId(), properties.getAdminClientSecret());
            String adminUrl = properties.getAdminApiBaseUrl() + "/sessions/" + sessionId;
            restClient.delete()
                .uri(adminUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity();
            log.info("Successfully terminated Keycloak session {} via Admin API", sessionId);
        } catch (Exception e) {
            log.warn("Failed to terminate Keycloak session {} via Admin API: {}", sessionId, e.getMessage());
        }
    }

    /**
     * OIDC 令牌返回实体
     */
    @Data
    public static class OidcTokenResponse implements Serializable {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("id_token")
        private String idToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Long expiresIn;

        @JsonProperty("refresh_expires_in")
        private Long refreshExpiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("session_state")
        private String sessionState;

        private String scope;
    }

}
