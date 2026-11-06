package cn.org.aip.keycloak.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapter;

import java.util.*;
import java.util.stream.Stream;

/**
 * Keycloak UserModel 适配器（将平台用户视图映射为 Keycloak 身份模型）
 * 只读适配器，凭证与用户信息只能在集成平台维护
 *
 * @author Michelle.Chung
 */
public class AipUserAdapter extends AbstractUserAdapter {

    private final AipApiClient.UserDto userDto;
    private final String keycloakId;

    public AipUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, AipApiClient.UserDto userDto) {
        super(session, realm, model);
        this.userDto = userDto;
        this.keycloakId = StorageId.keycloakId(model, userDto.getId());
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        return new UserCredentialManager(session, realm, this);
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    @Override
    public String getUsername() {
        return userDto.getUsername();
    }

    @Override
    public void setUsername(String username) {
        throw new ReadOnlyException("平台用户为只读模型，请在集成平台修改用户账号");
    }

    @Override
    public String getEmail() {
        return userDto.getEmail();
    }

    @Override
    public void setEmail(String email) {
        throw new ReadOnlyException("请在集成平台修改用户邮箱");
    }

    @Override
    public String getFirstName() {
        return userDto.getNickName();
    }

    @Override
    public void setFirstName(String firstName) {
        throw new ReadOnlyException("请在集成平台修改用户姓名");
    }

    @Override
    public String getLastName() {
        return "";
    }

    @Override
    public void setLastName(String lastName) {
        throw new ReadOnlyException("请在集成平台修改用户姓名");
    }

    @Override
    public boolean isEnabled() {
        return userDto.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        throw new ReadOnlyException("请在集成平台启停账号");
    }

    @Override
    public String getFirstAttribute(String name) {
        List<String> list = getAttributes().get(name);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        List<String> list = getAttributes().get(name);
        return list != null ? list.stream() : Stream.empty();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = new HashMap<>();
        if (userDto.getAttributes() != null) {
            attrs.putAll(userDto.getAttributes());
        }
        if (userDto.getEmployeeNo() != null) {
            attrs.put("employee_no", List.of(userDto.getEmployeeNo()));
        }
        if (userDto.getId() != null) {
            attrs.put("aip_user_id", List.of(userDto.getId()));
        }
        if (userDto.getUserName() != null) {
            attrs.put("user_name", List.of(userDto.getUserName()));
        }
        // Keycloak 内置 profile/email scope 的 Mapper 按用户属性读取以下字段，
        // 联邦用户必须显式暴露，否则 ID Token / UserInfo 将缺失 preferred_username、email 等标准声明。
        if (userDto.getUsername() != null) {
            attrs.put("username", List.of(userDto.getUsername()));
        }
        if (userDto.getEmail() != null) {
            attrs.put("email", List.of(userDto.getEmail()));
        }
        if (userDto.getNickName() != null) {
            attrs.put("firstName", List.of(userDto.getNickName()));
        }
        if (userDto.getDeptId() != null) {
            attrs.put("dept_id", List.of(String.valueOf(userDto.getDeptId())));
        }
        if (userDto.getDeptName() != null) {
            attrs.put("dept_name", List.of(userDto.getDeptName()));
        }
        if (userDto.getPasswordResetRequired() != null) {
            attrs.put("password_reset_required", List.of(String.valueOf(userDto.getPasswordResetRequired())));
        }
        return attrs;
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        throw new ReadOnlyException("平台用户属性为只读");
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        throw new ReadOnlyException("平台用户属性为只读");
    }

    @Override
    public void removeAttribute(String name) {
        throw new ReadOnlyException("平台用户属性为只读");
    }

    public AipApiClient.UserDto getUserDto() {
        return userDto;
    }

}
