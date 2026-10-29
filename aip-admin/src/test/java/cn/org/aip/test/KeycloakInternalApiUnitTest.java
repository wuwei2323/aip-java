package cn.org.aip.test;

import cn.hutool.crypto.digest.BCrypt;
import cn.org.aip.common.core.constant.HttpStatus;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.domain.R;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.mybatis.helper.DataPermissionHelper;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.system.domain.bo.SysUserBo;
import cn.org.aip.system.service.ISysUserService;
import cn.org.aip.web.controller.KeycloakInternalController;
import cn.org.aip.web.domain.bo.KeycloakVerifyPasswordBo;
import cn.org.aip.web.domain.vo.KeycloakUserVo;
import cn.org.aip.web.domain.vo.KeycloakVerifyPasswordVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 阶段三单测：Keycloak User Storage 内部查询与密码校验接口
 */
@SpringBootTest
@DisplayName("阶段三单测：Keycloak 内部查询与密码校验接口")
public class KeycloakInternalApiUnitTest {

    @Autowired
    private KeycloakInternalController internalController;

    @Autowired
    private KeycloakProperties keycloakProperties;

    @Autowired
    private ISysUserService userService;

    @Tag("dev")
    @DisplayName("测试内部接口共享密钥鉴权拦截")
    @Test
    public void testInternalSecretProtection() {
        KeycloakVerifyPasswordBo bo = new KeycloakVerifyPasswordBo();
        bo.setUsername("admin");
        bo.setPassword("123456");

        // 1. 无密钥调用拒绝
        ServiceException ex1 = Assertions.assertThrows(ServiceException.class, () -> {
            internalController.verifyPassword(null, bo);
        });
        Assertions.assertEquals(HttpStatus.FORBIDDEN, ex1.getCode());

        // 2. 错误密钥调用拒绝
        ServiceException ex2 = Assertions.assertThrows(ServiceException.class, () -> {
            internalController.verifyPassword("wrong-secret-token", bo);
        });
        Assertions.assertEquals(HttpStatus.FORBIDDEN, ex2.getCode());

        // 3. 正确密钥调用通过鉴权
        String validSecret = keycloakProperties.getInternalSecret();
        Assertions.assertDoesNotThrow(() -> {
            internalController.getUserByUsername(validSecret, "admin");
        });
    }

    @Tag("dev")
    @DisplayName("测试 Keycloak 密码校验（正确密码、错误密码、停用账号、强制改密标记）")
    @Test
    @Transactional
    public void testVerifyPasswordLogic() {
        String secret = keycloakProperties.getInternalSecret();
        String username = "kc_test_user_" + System.currentTimeMillis();
        String employeeNo = "EMP_" + (System.currentTimeMillis() % 1000000);
        String rawPassword = "Password123!";

        SysUserBo user = new SysUserBo();
        user.setUserName(username);
        user.setNickName("Keycloak测试用户");
        user.setEmployeeNo(employeeNo);
        user.setPassword(BCrypt.hashpw(rawPassword));
        user.setPasswordResetRequired(true); // 首次登录需强制改密
        user.setStatus(SystemConstants.NORMAL);
        user.setDeptId(103L);
        userService.insertUser(user);

        // 1. 正确密码校验通过，并携带 passwordResetRequired=true
        KeycloakVerifyPasswordBo validBo = new KeycloakVerifyPasswordBo();
        validBo.setUsername(employeeNo); // 支持按工号校验
        validBo.setPassword(rawPassword);

        R<KeycloakVerifyPasswordVo> validResp = internalController.verifyPassword(secret, validBo);
        Assertions.assertEquals(R.SUCCESS, validResp.getCode());
        Assertions.assertTrue(validResp.getData().isValid());
        Assertions.assertEquals(username, validResp.getData().getUsername());
        Assertions.assertEquals(employeeNo, validResp.getData().getEmployeeNo());
        Assertions.assertTrue(validResp.getData().getPasswordResetRequired());

        // 2. 错误密码校验失败
        KeycloakVerifyPasswordBo wrongBo = new KeycloakVerifyPasswordBo();
        wrongBo.setUsername(username);
        wrongBo.setPassword("wrong_password");

        R<KeycloakVerifyPasswordVo> wrongResp = internalController.verifyPassword(secret, wrongBo);
        Assertions.assertEquals(R.SUCCESS, wrongResp.getCode());
        Assertions.assertFalse(wrongResp.getData().isValid());

        // 3. 停用账号校验失败
        DataPermissionHelper.ignore(() -> userService.updateUserStatus(user.getUserId(), SystemConstants.DISABLE));

        R<KeycloakVerifyPasswordVo> blockedResp = internalController.verifyPassword(secret, validBo);
        Assertions.assertEquals(R.SUCCESS, blockedResp.getCode());
        Assertions.assertFalse(blockedResp.getData().isValid());
        Assertions.assertEquals(SystemConstants.DISABLE, blockedResp.getData().getStatus());
    }

    @Tag("dev")
    @DisplayName("测试根据用户名/工号/ID查询 Keycloak 用户视图及属性")
    @Test
    @Transactional
    public void testGetUserViewAndAttributes() {
        String secret = keycloakProperties.getInternalSecret();
        String username = "view_user_" + System.currentTimeMillis();
        String employeeNo = "EMP_VIEW_" + (System.currentTimeMillis() % 1000000);

        SysUserBo user = new SysUserBo();
        user.setUserName(username);
        user.setNickName("属性测试用户");
        user.setEmployeeNo(employeeNo);
        user.setEmail("view_user@test.com");
        user.setPassword(BCrypt.hashpw("123456"));
        user.setStatus(SystemConstants.NORMAL);
        user.setDeptId(103L);
        userService.insertUser(user);

        // 1. 按工号查询
        R<KeycloakUserVo> byEmpNoResp = internalController.getUserByUsername(secret, employeeNo);
        Assertions.assertEquals(R.SUCCESS, byEmpNoResp.getCode());
        KeycloakUserVo userVo = byEmpNoResp.getData();
        Assertions.assertNotNull(userVo);
        Assertions.assertEquals(employeeNo, userVo.getUsername()); // Keycloak 主账号优先工号
        Assertions.assertEquals(username, userVo.getUserName());
        Assertions.assertEquals("view_user@test.com", userVo.getEmail());
        Assertions.assertTrue(userVo.isEnabled());

        // 验证 attributes 声明映射
        Map<String, ?> attrs = userVo.getAttributes();
        Assertions.assertNotNull(attrs.get("employee_no"));
        Assertions.assertNotNull(attrs.get("aip_user_id"));
        Assertions.assertNotNull(attrs.get("dept_id"));

        // 2. 按平台用户ID查询
        R<KeycloakUserVo> byIdResp = internalController.getUserById(secret, user.getUserId());
        Assertions.assertEquals(R.SUCCESS, byIdResp.getCode());
        Assertions.assertNotNull(byIdResp.getData());
        Assertions.assertEquals(username, byIdResp.getData().getUserName());

        // 3. 分页搜索用户
        R<Map<String, Object>> searchResp = internalController.searchUsers(secret, username, 0, 10);
        Assertions.assertEquals(R.SUCCESS, searchResp.getCode());
        Assertions.assertTrue((Long) searchResp.getData().get("total") >= 1);
    }

}
