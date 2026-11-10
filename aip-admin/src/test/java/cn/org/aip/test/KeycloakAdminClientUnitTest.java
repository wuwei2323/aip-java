package cn.org.aip.test;

import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.common.sso.core.KeycloakAdminClient;
import cn.org.aip.common.sso.core.KeycloakRequestException;
import cn.org.aip.common.sso.core.KeycloakTokenClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** HTTP requests are fully mocked; these tests never contact Keycloak or start a network server. */
@Tag("dev")
class KeycloakAdminClientUnitTest {

    private KeycloakProperties properties;
    private KeycloakTokenClient tokenClient;
    private KeycloakAdminClient admin;
    private MockRestServiceServer server;

    @BeforeAll
    static void initializeUtilities() {
        SsoUnitTestSupport.initialize();
    }

    @BeforeEach
    void setUp() {
        properties = new KeycloakProperties();
        properties.setServerUrl("http://keycloak.invalid");
        properties.setAdminClientSecret("test-admin-secret");
        tokenClient = mock(KeycloakTokenClient.class);
        when(tokenClient.getClientCredentialsToken(anyString(), anyString())).thenReturn("test-admin-token");
        admin = new KeycloakAdminClient(properties, tokenClient);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(admin, "restClient", builder.build());
    }

    @Test
    void onlySuccessfulEmptySearchMeansClientAbsent() {
        server.expect(requestTo("http://keycloak.invalid/admin/realms/aip/clients?clientId=APP-001&search=false"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertNull(admin.findClientByClientId("APP-001"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 409, 500, 503})
    void lookupHttpFailureNeverMeansAbsenceAndNeverExposesResponseBody(int status) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status))
            .body("access_token=do-not-expose; client_secret=unsafe").contentType(MediaType.TEXT_PLAIN));
        KeycloakRequestException error = assertThrows(KeycloakRequestException.class, () -> admin.findClientByClientId("APP-001"));
        assertFalse(error.getMessage().contains("do-not-expose"));
        assertFalse(error.getMessage().contains("unsafe"));
        assertNull(error.getCause());
        if (status == 401 || status == 403) {
            assertEquals(KeycloakRequestException.Reason.UNAUTHORIZED, error.getReason());
        }
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "bad-json", "[{\"id\":\"id\",\"clientId\":\"wrong-client\"}]"})
    void malformedAndMismatchedLookupResponsesAreFailures(String response) {
        server.expect(anything()).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertEquals(KeycloakRequestException.Reason.INVALID_RESPONSE,
            assertThrows(KeycloakRequestException.class, () -> admin.findClientByClientId("APP-001")).getReason());
        server.verify();
    }

    @Test
    void encodesExactClientIdThenFetchesFullDetailAndPreservesUnknownFields() throws Exception {
        String clientId = "client & /=+";
        String detail = """
            {"id":"internal-id","clientId":"client & /=+","name":"manual name",
             "webOrigins":["https://manual.example"],"frontchannelLogout":true,
             "protocolMappers":[{"name":"custom-mapper","config":{"custom":"value"}}],
             "defaultClientScopes":["profile","custom"],"optionalClientScopes":["offline_access"],
             "attributes":{"unknown":"keep","post.logout.redirect.uris":"https://manual.example/logout"}}
            """;
        server.expect(requestTo("http://keycloak.invalid/admin/realms/aip/clients?clientId=client%20%26%20%2F%3D%2B&search=false"))
            .andRespond(withSuccess("[{\"id\":\"internal-id\",\"clientId\":\"client & /=+\"}]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak.invalid/admin/realms/aip/clients/internal-id"))
            .andRespond(withSuccess(detail, MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.PUT)).andExpect(requestTo("http://keycloak.invalid/admin/realms/aip/clients/internal-id"))
            .andExpect(content().json(detail.replace("manual name", "new name")))
            .andRespond(withNoContent());
        KeycloakAdminClient.ClientRepresentation remote = admin.findClientByClientId(clientId);
        assertNotNull(remote);
        remote.setName("new name");
        admin.updateClient(remote.getId(), remote);
        server.verify();
    }

    @Test
    void missingFullDetailIsNotTreatedAsAbsence() {
        server.expect(anything()).andRespond(withSuccess("[{\"id\":\"uuid\",\"clientId\":\"APP-001\"}]", MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withResourceNotFound());
        assertThrows(KeycloakRequestException.class, () -> admin.findClientByClientId("APP-001"));
        server.verify();
    }

    @Test
    void absentDtoFieldsAreNotInventedOrSerializedAsEmptyCollectionsAndBooleans() throws Exception {
        KeycloakAdminClient.ClientRepresentation client = JsonUtils.parseObject("{\"clientId\":\"APP-001\"}", KeycloakAdminClient.ClientRepresentation.class);
        var tree = JsonUtils.getObjectMapper().readTree(JsonUtils.toJsonString(client));
        assertFalse(tree.has("enabled"));
        assertFalse(tree.has("webOrigins"));
        assertFalse(tree.has("redirectUris"));
        assertFalse(tree.has("standardFlowEnabled"));
        assertFalse(tree.has("attributes"));
        assertFalse(tree.has("additionalFields"));
    }

    @Test
    void transportTimeoutIsClassifiedAndRedacted() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("do-not-expose-secret")));
        KeycloakRequestException exception = assertThrows(KeycloakRequestException.class, () -> admin.findClientByClientId("APP-001"));
        assertEquals(KeycloakRequestException.Reason.UNAVAILABLE, exception.getReason());
        assertFalse(exception.getMessage().contains("do-not-expose"));
        assertNull(exception.getCause());
    }

    @Test
    void adminAndTokenTransportsHaveFiniteConfigurableTimeouts() {
        properties.setConnectTimeoutMillis(1234);
        properties.setReadTimeoutMillis(2345);
        assertTimeouts(new KeycloakAdminClient(properties, tokenClient), 1234, 2345);
        assertTimeouts(new KeycloakTokenClient(properties), 1234, 2345);
        properties.setConnectTimeoutMillis(0);
        properties.setReadTimeoutMillis(Integer.MAX_VALUE);
        assertTimeouts(new KeycloakTokenClient(properties), 1, 30000);
    }

    @Test
    void tokenErrorsDoNotExposeTokenEndpointResponseOrCause() {
        KeycloakTokenClient realTokenClient = new KeycloakTokenClient(properties);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer tokenServer = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(realTokenClient, "restClient", builder.build());
        tokenServer.expect(anything()).andRespond(withUnauthorizedRequest().body("client_secret=do-not-expose"));
        KeycloakRequestException exception = assertThrows(KeycloakRequestException.class,
            () -> realTokenClient.getClientCredentialsToken("admin", "test-secret"));
        assertEquals(KeycloakRequestException.Reason.UNAUTHORIZED, exception.getReason());
        assertFalse(exception.getMessage().contains("do-not-expose"));
        assertNull(exception.getCause());
        tokenServer.verify();
    }

    @Test
    void disabledOrUnconfiguredAdminNeverMakesRemoteRequest() {
        properties.setEnabled(false);
        assertEquals(KeycloakRequestException.Reason.DISABLED,
            assertThrows(KeycloakRequestException.class, () -> admin.findClientByClientId("APP-001")).getReason());
        properties.setEnabled(true);
        properties.setAdminClientSecret("");
        assertEquals(KeycloakRequestException.Reason.ADMIN_UNCONFIGURED,
            assertThrows(KeycloakRequestException.class, () -> admin.findClientByClientId("APP-001")).getReason());
        verifyNoInteractions(tokenClient);
        server.verify();
    }

    private void assertTimeouts(Object client, int connect, int read) {
        Object rest = ReflectionTestUtils.getField(client, "restClient");
        Object factory = ReflectionTestUtils.getField(rest, "clientRequestFactory");
        assertInstanceOf(SimpleClientHttpRequestFactory.class, factory);
        assertEquals(connect, ReflectionTestUtils.getField(factory, "connectTimeout"));
        assertEquals(read, ReflectionTestUtils.getField(factory, "readTimeout"));
    }
}
