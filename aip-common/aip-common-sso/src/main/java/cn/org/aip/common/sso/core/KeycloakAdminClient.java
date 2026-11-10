package cn.org.aip.common.sso.core;

import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.sso.config.KeycloakProperties;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.org.aip.common.sso.core.KeycloakRequestException.Reason;

/**
 * Bounded Keycloak admin requests with exact lookup and lossless client representations.
 * Only a successful empty search means absence; failures retain a safe classification.
 */
@Component
public class KeycloakAdminClient {

    private final KeycloakProperties properties;
    private final KeycloakTokenClient tokenClient;
    private final RestClient restClient;

    /**
     * Creates the admin transport with bounded connection and read timeouts.
     *
     * @param properties Keycloak connection settings
     * @param tokenClient admin credential token provider
     */
    public KeycloakAdminClient(KeycloakProperties properties, KeycloakTokenClient tokenClient) {
        this.properties = properties;
        this.tokenClient = tokenClient;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMillis());
        factory.setReadTimeout(properties.getReadTimeoutMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    private String getAdminAccessToken() {
        if (!properties.isEnabled()) {
            throw new KeycloakRequestException(Reason.DISABLED);
        }
        if (StringUtils.isBlank(properties.getAdminClientSecret())) {
            throw new KeycloakRequestException(Reason.ADMIN_UNCONFIGURED);
        }
        return tokenClient.getClientCredentialsToken(properties.getAdminClientId(), properties.getAdminClientSecret());
    }

    /**
     * Finds an exact client ID and retrieves its full representation before any update.
     *
     * @param clientId exact OIDC client identifier, encoded as one query parameter
     * @return full representation, or null only for a successful empty search
     * @throws KeycloakRequestException for lookup failures or inconsistent responses
     */
    public ClientRepresentation findClientByClientId(String clientId) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(properties.getAdminApiBaseUrl())
                .path("/clients").queryParam("clientId", "{clientId}").queryParam("search", false)
                .encode().buildAndExpand(clientId).toUri();
            String token = getAdminAccessToken();
            String body = restClient.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().body(String.class);
            List<ClientRepresentation> clients = parse(body, new TypeReference<List<ClientRepresentation>>() { });
            if (clients.isEmpty()) {
                return null;
            }
            List<ClientRepresentation> exact = clients.stream()
                .filter(client -> client != null && clientId.equals(client.getClientId())).toList();
            if (exact.size() != 1 || StringUtils.isBlank(exact.get(0).getId())) {
                throw new KeycloakRequestException(Reason.INVALID_RESPONSE);
            }
            String internalId = exact.get(0).getId();
            String detail = restClient.get().uri(clientUri(internalId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).retrieve().body(String.class);
            ClientRepresentation result = parse(detail, new TypeReference<ClientRepresentation>() { });
            if (!internalId.equals(result.getId()) || !clientId.equals(result.getClientId())) {
                throw new KeycloakRequestException(Reason.INVALID_RESPONSE);
            }
            return result;
        } catch (RuntimeException exception) {
            throw KeycloakRequestException.from(exception);
        }
    }

    /** @param client new client configuration including its persisted, decrypted secret */
    public void createClient(ClientRepresentation client) {
        try {
            restClient.post().uri(properties.getAdminApiBaseUrl() + "/clients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                .contentType(MediaType.APPLICATION_JSON).body(client).retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            throw KeycloakRequestException.from(exception);
        }
    }

    /**
     * Updates a full representation obtained by exact lookup; unowned fields remain intact.
     *
     * @param internalId Keycloak client UUID
     * @param client merged full representation
     */
    public void updateClient(String internalId, ClientRepresentation client) {
        try {
            restClient.put().uri(clientUri(internalId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                .contentType(MediaType.APPLICATION_JSON).body(client).retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            throw KeycloakRequestException.from(exception);
        }
    }

    /** @param internalId Keycloak client UUID to delete */
    public void deleteClient(String internalId) {
        try {
            restClient.delete().uri(clientUri(internalId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                .retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            throw KeycloakRequestException.from(exception);
        }
    }

    /**
     * Explicitly regenerates a remote secret; ordinary configuration sync never calls this.
     *
     * @param internalId Keycloak client UUID
     * @return new secret
     */
    public String regenerateClientSecret(String internalId) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(properties.getAdminApiBaseUrl())
                .path("/clients/{id}/client-secret").encode().buildAndExpand(internalId).toUri();
            String body = restClient.post().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                .retrieve().body(String.class);
            CredentialRepresentation credential = parse(body, new TypeReference<CredentialRepresentation>() { });
            if (StringUtils.isBlank(credential.getValue())) {
                throw new KeycloakRequestException(Reason.INVALID_RESPONSE);
            }
            return credential.getValue();
        } catch (RuntimeException exception) {
            throw KeycloakRequestException.from(exception);
        }
    }

    private URI clientUri(String internalId) {
        return UriComponentsBuilder.fromUriString(properties.getAdminApiBaseUrl())
            .path("/clients/{id}").encode().buildAndExpand(internalId).toUri();
    }

    private <T> T parse(String body, TypeReference<T> type) {
        try {
            T result = JsonUtils.parseObject(body, type);
            if (result == null) {
                throw new KeycloakRequestException(Reason.INVALID_RESPONSE);
            }
            return result;
        } catch (RuntimeException exception) {
            throw new KeycloakRequestException(Reason.INVALID_RESPONSE);
        }
    }

    /** Lossless client representation, with no defaults that could overwrite unowned data. */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientRepresentation implements Serializable {
        private String id;
        private String clientId;
        private String name;
        private String description;
        private Boolean enabled;
        private Boolean publicClient;
        private String clientAuthenticatorType;
        @ToString.Exclude
        private String secret;
        private List<String> redirectUris;
        private List<String> webOrigins;
        private Boolean standardFlowEnabled;
        private Boolean implicitFlowEnabled;
        private Boolean directAccessGrantsEnabled;
        private Boolean serviceAccountsEnabled;
        private Map<String, String> attributes;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ToString.Exclude
        private final Map<String, Object> additionalFields = new LinkedHashMap<>();

        /** @return mutable attributes, preserving every remotely configured attribute */
        public Map<String, String> mutableAttributes() {
            if (attributes == null) {
                attributes = new LinkedHashMap<>();
            }
            return attributes;
        }

        /** @return unknown top-level fields flattened into the outgoing representation */
        @JsonAnyGetter
        public Map<String, Object> additionalFields() {
            return additionalFields;
        }

        /**
         * Retains unknown fields such as protocol mappers and client scopes.
         *
         * @param name remote property name
         * @param value remote property value
         */
        @JsonAnySetter
        public void retainField(String name, Object value) {
            additionalFields.put(name, value);
        }
    }

    /** Keycloak secret regeneration response. */
    @Data
    public static class CredentialRepresentation implements Serializable {
        private String type;
        @ToString.Exclude
        private String value;
    }
}
