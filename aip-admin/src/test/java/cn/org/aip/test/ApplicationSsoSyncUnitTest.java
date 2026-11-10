package cn.org.aip.test;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.crypto.SecureUtil;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.json.utils.JsonUtils;
import cn.org.aip.common.log.annotation.Log;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.common.sso.constant.KeycloakConstants;
import cn.org.aip.common.sso.core.KeycloakAdminClient;
import cn.org.aip.common.sso.core.KeycloakAdminClient.ClientRepresentation;
import cn.org.aip.common.sso.core.KeycloakRequestException;
import cn.org.aip.system.controller.system.SysApplicationController;
import cn.org.aip.system.domain.SysApplication;
import cn.org.aip.system.domain.bo.SysApplicationBo;
import cn.org.aip.system.domain.vo.SysApplicationVo;
import cn.org.aip.system.mapper.SysApplicationMapper;
import cn.org.aip.system.service.impl.SysApplicationServiceImpl;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Database-free tests of saved configuration, explicit retries, ownership and secret protection. */
@Tag("dev")
class ApplicationSsoSyncUnitTest {

    private static final String AES_KEY = "0123456789abcdef0123456789abcdef";
    private SysApplicationMapper mapper;
    private KeycloakAdminClient admin;
    private KeycloakProperties properties;
    private SysApplicationServiceImpl service;
    private final Map<Long, SysApplication> rows = new HashMap<>();
    private LocalTransactionManager transactions;

    @BeforeAll
    static void initializeUtilities() {
        SsoUnitTestSupport.initialize();
    }

    @BeforeEach
    void setUp() {
        rows.clear();
        mapper = mock(SysApplicationMapper.class);
        admin = mock(KeycloakAdminClient.class);
        properties = new KeycloakProperties();
        properties.setAdminClientSecret("unit-test-admin-secret");
        transactions = new LocalTransactionManager();
        service = new SysApplicationServiceImpl(mapper, properties, admin, new TransactionTemplate(transactions));
        ReflectionTestUtils.setField(service, "aesKey", AES_KEY);
        when(mapper.countByAppId(anyString(), any())).thenReturn(0L);
        when(mapper.selectById(any())).thenAnswer(invocation -> copy(rows.get(invocation.getArgument(0))));
        when(mapper.insert(any(SysApplication.class))).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            SysApplication app = invocation.getArgument(0);
            app.setId(1L);
            rows.put(app.getId(), copy(app));
            return 1;
        });
        when(mapper.updateById(any(SysApplication.class))).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            SysApplication update = invocation.getArgument(0);
            SysApplication row = rows.get(update.getId());
            if (row == null) {
                return 0;
            }
            BeanWrapperImpl source = new BeanWrapperImpl(update);
            List<String> nulls = new ArrayList<>();
            for (var descriptor : source.getPropertyDescriptors()) {
                if (source.isReadableProperty(descriptor.getName()) && source.getPropertyValue(descriptor.getName()) == null) {
                    nulls.add(descriptor.getName());
                }
            }
            BeanUtils.copyProperties(update, row, nulls.toArray(String[]::new));
            return 1;
        });
    }

    @Test
    void failedCreateRemainsSavedAndReturnsSecretOnceThenRetryReusesPersistedSecret() {
        when(admin.findClientByClientId("APP-010")).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(1, transactions.commits);
            assertEquals("PENDING", rows.get(1L).getSsoSyncStatus());
            throw new KeycloakRequestException(KeycloakRequestException.Reason.UNAVAILABLE);
        });
        SysApplicationVo created = service.insertByBo(bo());
        assertEquals("FAILED", created.getSsoSyncStatus());
        assertEquals("FAILED", rows.get(1L).getSsoSyncStatus());
        assertNotNull(created.getClientSecret());
        assertNotEquals(created.getClientSecret(), rows.get(1L).getClientSecretEnc());
        String encrypted = rows.get(1L).getClientSecretEnc();
        assertEquals(created.getClientSecret(), decrypt(encrypted));
        verify(admin, never()).createClient(any());

        reset(admin);
        when(admin.findClientByClientId("APP-010")).thenReturn(null);
        doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            ClientRepresentation client = invocation.getArgument(0);
            assertEquals(created.getClientSecret(), client.getSecret());
            return null;
        }).when(admin).createClient(any());
        SysApplicationVo retried = service.syncApplication(1L);
        assertEquals("SUCCESS", retried.getSsoSyncStatus());
        assertEquals("", rows.get(1L).getSsoSyncError());
        assertNull(retried.getClientSecret());
        assertEquals(encrypted, rows.get(1L).getClientSecretEnc());
    }

    @Test
    void ordinaryRetryPreservesExistingSecretAndAllUnownedRemoteConfiguration() {
        seed();
        ClientRepresentation remote = remote();
        remote.setSecret("manually-managed-remote-secret");
        remote.setWebOrigins(List.of("https://manual.example"));
        remote.retainField("protocolMappers", List.of(Map.of("name", "manual-mapper")));
        remote.mutableAttributes().put("custom-attribute", "keep");
        remote.mutableAttributes().put(KeycloakConstants.ATTR_POST_LOGOUT_REDIRECT_URIS, "https://manual.example/logout");
        remote.mutableAttributes().put(KeycloakConstants.ATTR_BACKCHANNEL_LOGOUT_URL, "https://manual.example/backchannel");
        when(admin.findClientByClientId("APP-010")).thenReturn(remote);

        String encrypted = rows.get(1L).getClientSecretEnc();
        assertEquals("SUCCESS", service.syncApplication(1L).getSsoSyncStatus());
        assertEquals(encrypted, rows.get(1L).getClientSecretEnc());
        assertNull(remote.getSecret());
        assertEquals("https://manual.example/logout", remote.getAttributes().get(KeycloakConstants.ATTR_POST_LOGOUT_REDIRECT_URIS));
        assertEquals("https://manual.example/backchannel", remote.getAttributes().get(KeycloakConstants.ATTR_BACKCHANNEL_LOGOUT_URL));
        assertEquals("keep", remote.getAttributes().get("custom-attribute"));
        assertEquals(List.of("https://manual.example"), remote.getWebOrigins());
        assertTrue(remote.additionalFields().containsKey("protocolMappers"));
        assertEquals("S256", remote.getAttributes().get(KeycloakConstants.ATTR_PKCE_METHOD));
        assertFalse(remote.getPublicClient());
        assertFalse(remote.getImplicitFlowEnabled());
        assertFalse(remote.getDirectAccessGrantsEnabled());
        assertFalse(remote.getServiceAccountsEnabled());
        assertTrue(remote.getStandardFlowEnabled());
        verify(admin, never()).regenerateClientSecret(anyString());
    }

    @Test
    void nullOldRequestPreservesOwnedUrisAndEmptyStringExplicitlyDisablesThem() throws Exception {
        seed();
        rows.get(1L).setPostLogoutRedirectUri("http://192.168.43.100:81/signed-out");
        rows.get(1L).setBackchannelLogoutUri("http://192.168.43.101/logout");
        ClientRepresentation remote = remote();
        when(admin.findClientByClientId("APP-010")).thenReturn(remote);
        SysApplicationBo edit = bo();
        edit.setId(1L);
        assertEquals("http://192.168.43.100:81/signed-out", service.updateByBo(edit).getPostLogoutRedirectUri());
        assertEquals("http://192.168.43.101/logout", rows.get(1L).getBackchannelLogoutUri());
        edit.setPostLogoutRedirectUri("");
        edit.setBackchannelLogoutUri("");
        assertEquals("SUCCESS", service.updateByBo(edit).getSsoSyncStatus());
        assertEquals("", rows.get(1L).getPostLogoutRedirectUri());
        assertEquals("", rows.get(1L).getBackchannelLogoutUri());
        assertEquals("-", remote.getAttributes().get(KeycloakConstants.ATTR_POST_LOGOUT_REDIRECT_URIS));
        assertEquals("", remote.getAttributes().get(KeycloakConstants.ATTR_BACKCHANNEL_LOGOUT_URL));
        for (String field : List.of("postLogoutRedirectUri", "backchannelLogoutUri", "ssoSyncError")) {
            assertEquals(FieldStrategy.NOT_NULL, SysApplication.class.getDeclaredField(field)
                .getAnnotation(TableField.class).updateStrategy());
        }
    }

    @Test
    void failedEditAndStatusChangeRemainCommittedWithSafeFailureAndNoSecret() {
        seed();
        when(admin.findClientByClientId(anyString())).thenThrow(new RuntimeException("access_token=do-not-expose"));
        SysApplicationBo edit = bo();
        edit.setId(1L);
        edit.setAppName("Saved despite outage");
        SysApplicationVo result = service.updateByBo(edit);
        assertEquals("Saved despite outage", rows.get(1L).getAppName());
        assertEquals("FAILED", result.getSsoSyncStatus());
        assertFalse(result.getSsoSyncError().contains("do-not-expose"));
        assertNull(result.getClientSecret());
        result = service.updateApplicationStatus(1L, "1");
        assertEquals("1", rows.get(1L).getStatus());
        assertEquals("FAILED", result.getSsoSyncStatus());
        verify(admin, never()).createClient(any());
    }

    @Test
    void disabledOrUnconfiguredKeycloakIsVisibleFailureNotSuccess() {
        seed();
        properties.setEnabled(false);
        assertEquals("FAILED", service.syncApplication(1L).getSsoSyncStatus());
        assertFalse(rows.get(1L).getSsoSyncError().isBlank());
        properties.setEnabled(true);
        properties.setAdminClientSecret("");
        assertEquals("FAILED", service.syncApplication(1L).getSsoSyncStatus());
        verifyNoInteractions(admin);
    }

    @Test
    void missingOrCorruptHistoricalSecretNeverFallsBackToRandomSecret() {
        seed();
        for (String secret : new String[] {null, "", "not-a-valid-cipher"}) {
            rows.get(1L).setClientSecretEnc(secret);
            assertEquals("FAILED", service.syncApplication(1L).getSsoSyncStatus());
        }
        verify(admin, never()).createClient(any());
        verify(admin, never()).updateClient(anyString(), any());
    }

    @Test
    void resetRequiresRemoteSuccessAndNeverTreatsAbsenceOrLookupFailureAsSuccess() {
        seed();
        String encrypted = rows.get(1L).getClientSecretEnc();
        assertThrows(ServiceException.class, () -> service.resetSecret(1L));
        assertEquals(encrypted, rows.get(1L).getClientSecretEnc());
        when(admin.findClientByClientId(anyString())).thenThrow(new KeycloakRequestException(KeycloakRequestException.Reason.UNAVAILABLE));
        assertThrows(KeycloakRequestException.class, () -> service.resetSecret(1L));
        assertEquals(encrypted, rows.get(1L).getClientSecretEnc());
        doReturn(remote()).when(admin).findClientByClientId(anyString());
        doThrow(new KeycloakRequestException(KeycloakRequestException.Reason.HTTP_ERROR)).when(admin).updateClient(anyString(), any());
        assertThrows(KeycloakRequestException.class, () -> service.resetSecret(1L));
        assertEquals(encrypted, rows.get(1L).getClientSecretEnc());
    }

    @Test
    void successfulExplicitResetUpdatesRemoteBeforeDatabaseAndReturnsNewSecret() {
        seed();
        String original = rows.get(1L).getClientSecretEnc();
        when(admin.findClientByClientId(anyString())).thenReturn(remote());
        doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(original, rows.get(1L).getClientSecretEnc());
            return null;
        }).when(admin).updateClient(anyString(), any());
        String secret = service.resetSecret(1L);
        assertNotEquals(decrypt(original), secret);
        assertEquals(secret, decrypt(rows.get(1L).getClientSecretEnc()));
    }

    @Test
    void deletionCallsRemoteFirstAndFailureKeepsLocalRecord() {
        seed();
        when(admin.findClientByClientId(anyString())).thenReturn(remote());
        doThrow(new KeycloakRequestException(KeycloakRequestException.Reason.HTTP_ERROR)).when(admin).deleteClient(anyString());
        assertThrows(KeycloakRequestException.class, () -> service.deleteWithValidByIds(List.of(1L), true));
        assertTrue(rows.containsKey(1L));
        verify(mapper, never()).deleteByIds(anyCollection());
        reset(admin);
        when(admin.findClientByClientId(anyString())).thenThrow(new KeycloakRequestException(KeycloakRequestException.Reason.UNAVAILABLE));
        assertThrows(KeycloakRequestException.class, () -> service.deleteWithValidByIds(List.of(1L), true));
        verify(mapper, never()).deleteByIds(anyCollection());
    }

    @Test
    void outcomePersistenceFailureRemainsPendingAndStillReturnsCreatedSecret() {
        doThrow(new IllegalStateException("database unavailable")).when(mapper).updateById(any(SysApplication.class));
        SysApplicationVo result = service.insertByBo(bo());
        assertEquals("PENDING", rows.get(1L).getSsoSyncStatus());
        assertEquals("PENDING", result.getSsoSyncStatus());
        assertFalse(result.getSsoSyncError().isBlank());
        assertNotNull(result.getClientSecret());
        assertEquals(result.getClientSecret(), decrypt(rows.get(1L).getClientSecretEnc()));
    }

    @Test
    void successfulDeletionRemovesRemoteBeforeStartingLocalDeleteTransaction() {
        seed();
        when(admin.findClientByClientId(anyString())).thenReturn(remote());
        doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(rows.containsKey(1L));
            return null;
        }).when(admin).deleteClient("remote-uuid");
        when(mapper.deleteByIds(anyCollection())).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return 1;
        });
        assertTrue(service.deleteWithValidByIds(List.of(1L), true));
        var ordered = inOrder(admin, mapper);
        ordered.verify(admin).findClientByClientId("APP-010");
        ordered.verify(admin).deleteClient("remote-uuid");
        ordered.verify(mapper).deleteByIds(List.of(1L));
    }

    @Test
    void portalCannotBeDisabledViaEditOrStatusOrDeletedInBatch() {
        seed();
        rows.get(1L).setAppId("000000");
        SysApplicationBo edit = bo();
        edit.setId(1L);
        edit.setStatus("1");
        assertThrows(ServiceException.class, () -> service.updateByBo(edit));
        assertThrows(ServiceException.class, () -> service.updateApplicationStatus(1L, "1"));
        assertThrows(ServiceException.class, () -> service.deleteWithValidByIds(List.of(1L), false));
        assertEquals("0", rows.get(1L).getStatus());
        verifyNoInteractions(admin);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/relative", "javascript:alert(1)", "https://user:pass@host/path", "https://host/path#fragment",
        "https://host/*", "https://host/%2a", "https://", "https://host:99999/path", "http://bad host/a"})
    void rejectsMalformedOrUnsafeOptionalAndLoginUris(String uri) {
        SysApplicationBo request = bo();
        request.setPostLogoutRedirectUri(uri);
        assertThrows(ServiceException.class, () -> service.insertByBo(request));
        request.setPostLogoutRedirectUri(null);
        request.setBackchannelLogoutUri(uri);
        assertThrows(ServiceException.class, () -> service.insertByBo(request));
        request.setBackchannelLogoutUri(null);
        request.setRedirectUris(uri);
        assertThrows(ServiceException.class, () -> service.insertByBo(request));
        verify(mapper, never()).insert(any(SysApplication.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[https://host/callback]", "[\"https://host/callback\"", "[\"https://host/callback\",]",
        "[]", "[null]", "[123]", "[\"https://host/callback\"] garbage", ""})
    void malformedJsonLoginListNeverFallsBackToRawList(String value) {
        SysApplicationBo request = bo();
        request.setRedirectUris(value);
        assertThrows(ServiceException.class, () -> service.insertByBo(request));
        verify(mapper, never()).insert(any(SysApplication.class));
    }

    @Test
    void validatesLengthAndAllowsLanHttpForDevelopment() {
        SysApplicationBo request = bo();
        request.setPostLogoutRedirectUri("https://host/" + "a".repeat(500));
        assertThrows(ServiceException.class, () -> service.insertByBo(request));
        request.setPostLogoutRedirectUri("http://192.168.43.100:81/logout");
        request.setBackchannelLogoutUri("http://192.168.43.101:8080/backchannel");
        assertEquals("SUCCESS", service.insertByBo(request).getSsoSyncStatus());
    }

    @Test
    void syncStateIsReadOnlyAndControllerProtectsOneTimeSecretsAndRetryPermission() throws Exception {
        SysApplicationBo request = JsonUtils.parseObject("{\"ssoSyncStatus\":\"SUCCESS\",\"ssoSyncError\":\"forged\"}", SysApplicationBo.class);
        assertNull(request.getSsoSyncStatus());
        assertNull(request.getSsoSyncError());
        assertFalse(SysApplicationController.class.getMethod("add", SysApplicationBo.class).getAnnotation(Log.class).isSaveResponseData());
        assertFalse(SysApplicationController.class.getMethod("resetSecret", Long.class).getAnnotation(Log.class).isSaveResponseData());
        var sync = SysApplicationController.class.getMethod("sync", Long.class);
        assertArrayEquals(new String[] {"/{id}/sync"}, sync.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"system:application:edit"}, sync.getAnnotation(SaCheckPermission.class).value());
    }

    private SysApplicationBo bo() {
        SysApplicationBo request = new SysApplicationBo();
        request.setAppId("APP-010");
        request.setAppName("Hospital App");
        request.setCategory("hospital");
        request.setDescription("Test application");
        request.setEntryUrl("http://192.168.43.100:81");
        request.setRedirectUris("[\"http://192.168.43.100:81/callback\"]");
        request.setStatus("0");
        return request;
    }

    private void seed() {
        SysApplication app = new SysApplication();
        BeanUtils.copyProperties(bo(), app);
        app.setId(1L);
        app.setTenantId("000000");
        app.setClientSecretEnc(SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8)).encryptBase64("stored-client-secret"));
        rows.put(1L, app);
    }

    private ClientRepresentation remote() {
        ClientRepresentation client = new ClientRepresentation();
        client.setId("remote-uuid");
        client.setClientId("APP-010");
        return client;
    }

    private SysApplication copy(SysApplication source) {
        if (source == null) {
            return null;
        }
        SysApplication result = new SysApplication();
        BeanUtils.copyProperties(source, result);
        return result;
    }

    private String decrypt(String encrypted) {
        return SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8)).decryptStr(encrypted);
    }

    /** Actual Spring transaction boundaries without any external resource. */
    private static final class LocalTransactionManager extends AbstractPlatformTransactionManager {
        private int commits;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
