package cn.org.aip.keycloak.storage;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Keycloak AIP 用户存储 Provider 实现
 * 实现 UserLookupProvider (用户查询)、CredentialInputValidator (密码校验桥)、UserQueryProvider (管理台搜索)
 *
 * @author Michelle.Chung
 */
public class AipUserStorageProvider implements
    UserStorageProvider,
    UserLookupProvider,
    CredentialInputValidator,
    UserQueryProvider {

    private static final Logger logger = Logger.getLogger(AipUserStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final AipApiClient apiClient;

    public AipUserStorageProvider(KeycloakSession session, ComponentModel model, AipApiClient apiClient) {
        this.session = session;
        this.model = model;
        this.apiClient = apiClient;
    }

    @Override
    public void close() {
        // no-op
    }

    // -------------------------------------------------------------
    // UserLookupProvider
    // -------------------------------------------------------------

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id);
        logger.debugf("getUserById id=%s, externalId=%s", id, externalId);
        AipApiClient.UserDto userDto = apiClient.getUserById(externalId);
        if (userDto == null) {
            return null;
        }
        return new AipUserAdapter(session, realm, model, userDto);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.debugf("getUserByUsername username=%s", username);
        AipApiClient.UserDto userDto = apiClient.getUserByUsername(username);
        if (userDto == null) {
            return null;
        }
        return new AipUserAdapter(session, realm, model, userDto);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        logger.debugf("getUserByEmail email=%s", email);
        AipApiClient.UserDto userDto = apiClient.getUserByUsername(email);
        if (userDto == null) {
            return null;
        }
        return new AipUserAdapter(session, realm, model, userDto);
    }

    // -------------------------------------------------------------
    // CredentialInputValidator (密码校验桥)
    // -------------------------------------------------------------

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) {
            return false;
        }

        String username = user.getUsername();
        String password = input.getChallengeResponse();

        logger.debugf("Validating credential for user %s via platform API", username);
        AipApiClient.VerifyPasswordResult result = apiClient.verifyPassword(username, password);
        if (result == null || !result.isValid()) {
            String msg = (result != null) ? result.getMessage() : "验证接口无响应";
            logger.infof("Password verification failed for user %s: %s", username, msg);
            return false;
        }

        logger.infof("Password verification succeeded for user %s", username);
        return true;
    }

    // -------------------------------------------------------------
    // UserQueryProvider (支持 Keycloak 管理台按条件搜索)
    // -------------------------------------------------------------

    @Override
    public int getUsersCount(RealmModel realm) {
        List<AipApiClient.UserDto> list = apiClient.searchUsers(null, 0, 1);
        return list.size();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        int first = firstResult != null ? firstResult : 0;
        int max = maxResults != null ? maxResults : 20;
        List<AipApiClient.UserDto> list = apiClient.searchUsers(search, first, max);
        return list.stream().map(dto -> (UserModel) new AipUserAdapter(session, realm, model, dto));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        String search = null;
        if (params.containsKey(UserModel.SEARCH)) {
            search = params.get(UserModel.SEARCH);
        } else if (params.containsKey(UserModel.USERNAME)) {
            search = params.get(UserModel.USERNAME);
        } else if (params.containsKey(UserModel.EMAIL)) {
            search = params.get(UserModel.EMAIL);
        }
        return searchForUserStream(realm, search, firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return searchForUserStream(realm, attrValue, 0, 20);
    }

}
