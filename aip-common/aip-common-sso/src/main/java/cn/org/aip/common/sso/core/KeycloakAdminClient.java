package cn.org.aip.common.sso.core;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.sso.config.KeycloakProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.Serializable;
import java.util.*;

/**
 * Keycloak 管理端 REST API 客户端 (用于同步客户端配置，不涉及用户同步)
 *
 * @author Michelle.Chung
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private final KeycloakProperties properties;
    private final KeycloakTokenClient tokenClient;
    private final RestClient restClient = RestClient.create();

    private String getAdminAccessToken() {
        if (StringUtils.isBlank(properties.getAdminClientSecret())) {
            throw new ServiceException("未配置 Keycloak Admin Client Secret，无法调用管理台接口");
        }
        return tokenClient.getClientCredentialsToken(properties.getAdminClientId(), properties.getAdminClientSecret());
    }

    /**
     * 根据 clientId 查找 Keycloak 中的客户端信息 (含内部UUID id)
     */
    public ClientRepresentation findClientByClientId(String clientId) {
        String url = properties.getAdminApiBaseUrl() + "/clients?clientId=" + clientId;
        try {
            String token = getAdminAccessToken();
            String responseBody = restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(String.class);

            List<ClientRepresentation> list = JsonUtils.parseObject(responseBody, new TypeReference<List<ClientRepresentation>>() {
            });
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.warn("Failed to find Keycloak client {}: {}", clientId, e.getMessage());
            return null;
        }
    }

    /**
     * 在 Keycloak 中创建客户端
     */
    public void createClient(ClientRepresentation client) {
        String url = properties.getAdminApiBaseUrl() + "/clients";
        try {
            String token = getAdminAccessToken();
            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(client)
                .retrieve()
                .toBodilessEntity();

            log.info("Successfully created Keycloak client: {}", client.getClientId());
        } catch (Exception e) {
            log.error("Failed to create Keycloak client {}: {}", client.getClientId(), e.getMessage(), e);
            throw new ServiceException("下发 Keycloak Client 失败: " + e.getMessage());
        }
    }

    /**
     * 在 Keycloak 中更新客户端配置
     */
    public void updateClient(String internalId, ClientRepresentation client) {
        String url = properties.getAdminApiBaseUrl() + "/clients/" + internalId;
        try {
            String token = getAdminAccessToken();
            restClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(client)
                .retrieve()
                .toBodilessEntity();

            log.info("Successfully updated Keycloak client: {}", client.getClientId());
        } catch (Exception e) {
            log.error("Failed to update Keycloak client {}: {}", client.getClientId(), e.getMessage(), e);
            throw new ServiceException("更新 Keycloak Client 失败: " + e.getMessage());
        }
    }

    /**
     * 在 Keycloak 中删除客户端
     */
    public void deleteClient(String internalId) {
        String url = properties.getAdminApiBaseUrl() + "/clients/" + internalId;
        try {
            String token = getAdminAccessToken();
            restClient.delete()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity();

            log.info("Successfully deleted Keycloak client ID: {}", internalId);
        } catch (Exception e) {
            log.error("Failed to delete Keycloak client {}: {}", internalId, e.getMessage(), e);
            throw new ServiceException("删除 Keycloak Client 失败: " + e.getMessage());
        }
    }

    /**
     * 在 Keycloak 中重置客户端密钥
     *
     * @param internalId 客户端内部 UUID
     * @return 新的密钥值
     */
    public String regenerateClientSecret(String internalId) {
        String url = properties.getAdminApiBaseUrl() + "/clients/" + internalId + "/client-secret";
        try {
            String token = getAdminAccessToken();
            String responseBody = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(String.class);

            CredentialRepresentation credential = JsonUtils.parseObject(responseBody, CredentialRepresentation.class);
            return credential != null ? credential.getValue() : null;
        } catch (Exception e) {
            log.error("Failed to regenerate client secret for {}: {}", internalId, e.getMessage(), e);
            throw new ServiceException("重置 Keycloak Client 密钥失败: " + e.getMessage());
        }
    }

    /**
     * Keycloak Client 数据模型
     */
    @Data
    public static class ClientRepresentation implements Serializable {
        private String id;
        private String clientId;
        private String name;
        private String description;
        private Boolean enabled = true;
        private Boolean publicClient = false;
        private String clientAuthenticatorType = "client-secret";
        private String secret;
        private List<String> redirectUris = new ArrayList<>();
        private List<String> webOrigins = new ArrayList<>();
        private Boolean standardFlowEnabled = true;
        private Boolean implicitFlowEnabled = false;
        private Boolean directAccessGrantsEnabled = false;
        private Boolean serviceAccountsEnabled = false;
        private Map<String, String> attributes = new HashMap<>();
    }

    @Data
    public static class CredentialRepresentation implements Serializable {
        private String type;
        private String value;
    }

}
