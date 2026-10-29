package cn.org.aip.keycloak.storage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.ReadOnlyException;

import java.util.List;
import java.util.Map;

/**
 * Keycloak SPI 适配器与 Provider 单元测试
 */
@DisplayName("Keycloak SPI 适配器与 Provider 单元测试")
public class AipUserAdapterTest {

    @Tag("dev")
    @DisplayName("测试 AipUserAdapter 属性映射与只读保护")
    @Test
    public void testAipUserAdapterMappingAndReadOnly() {
        ComponentModel model = new ComponentModel();
        model.setId("comp-123");

        AipApiClient.UserDto dto = new AipApiClient.UserDto();
        dto.setId("10086");
        dto.setUsername("EMP999");
        dto.setUserName("zhangsan");
        dto.setEmployeeNo("EMP999");
        dto.setEmail("zhangsan@hospital.com");
        dto.setNickName("张三");
        dto.setEnabled(true);
        dto.setDeptId(103L);
        dto.setDeptName("信息科");
        dto.setPasswordResetRequired(false);

        AipUserAdapter adapter = new AipUserAdapter(null, null, model, dto);

        // 1. 基础属性映射验证
        Assertions.assertEquals("f:comp-123:10086", adapter.getId());
        Assertions.assertEquals("EMP999", adapter.getUsername()); // 优先工号
        Assertions.assertEquals("zhangsan@hospital.com", adapter.getEmail());
        Assertions.assertEquals("张三", adapter.getFirstName());
        Assertions.assertTrue(adapter.isEnabled());

        // 2. 声明属性 attributes 验证
        Map<String, List<String>> attributes = adapter.getAttributes();
        Assertions.assertEquals("EMP999", adapter.getFirstAttribute("employee_no"));
        Assertions.assertEquals("10086", adapter.getFirstAttribute("aip_user_id"));
        Assertions.assertEquals("zhangsan", adapter.getFirstAttribute("user_name"));
        Assertions.assertEquals("103", adapter.getFirstAttribute("dept_id"));
        Assertions.assertEquals("信息科", adapter.getFirstAttribute("dept_name"));

        // 3. 只读保证 (严禁在 Keycloak 写回修改)
        Assertions.assertThrows(ReadOnlyException.class, () -> adapter.setUsername("new_username"));
        Assertions.assertThrows(ReadOnlyException.class, () -> adapter.setEmail("new@email.com"));
        Assertions.assertThrows(ReadOnlyException.class, () -> adapter.setFirstName("李四"));
        Assertions.assertThrows(ReadOnlyException.class, () -> adapter.setEnabled(false));
        Assertions.assertThrows(ReadOnlyException.class, () -> adapter.setSingleAttribute("custom", "val"));
    }

    @Tag("dev")
    @DisplayName("测试 AipUserStorageProviderFactory 标识与支持的凭证类型")
    @Test
    public void testFactoryAndCredentialType() {
        AipUserStorageProviderFactory factory = new AipUserStorageProviderFactory();
        Assertions.assertEquals("aip-user-storage", factory.getId());
        Assertions.assertFalse(factory.getConfigProperties().isEmpty());

        ComponentModel model = new ComponentModel();
        model.put(AipUserStorageConfig.CONFIG_API_URL, "http://localhost:8080");
        model.put(AipUserStorageConfig.CONFIG_SHARED_SECRET, "test-secret");
        AipUserStorageConfig config = new AipUserStorageConfig(model);

        AipUserStorageProvider provider = new AipUserStorageProvider(null, model, new AipApiClient(config));
        Assertions.assertTrue(provider.supportsCredentialType(PasswordCredentialModel.TYPE));
        Assertions.assertFalse(provider.supportsCredentialType("otp"));
    }

}
