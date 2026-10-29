package cn.org.aip.test;

import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.system.domain.bo.SysApplicationBo;
import cn.org.aip.system.domain.bo.SysMenuBo;
import cn.org.aip.system.domain.bo.SysUserBo;
import cn.org.aip.system.domain.vo.SysApplicationVo;
import cn.org.aip.system.domain.vo.SysMenuVo;
import cn.org.aip.system.service.ISysApplicationService;
import cn.org.aip.system.service.ISysMenuService;
import cn.org.aip.system.service.ISysUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 阶段一单测：应用注册、菜单关联应用、用户工号字段
 */
@SpringBootTest
@DisplayName("阶段一单测：应用管理与菜单/用户改造")
public class ApplicationAndMenuUnitTest {

    @Autowired
    private ISysApplicationService applicationService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private ISysUserService userService;

    @Tag("dev")
    @DisplayName("测试应用新增、密钥AES加密与重置密钥")
    @Test
    @Transactional
    public void testApplicationCrudAndSecret() {
        String testTenantId = "app_" + (System.currentTimeMillis() % 10000000);
        SysApplicationBo bo = new SysApplicationBo();
        bo.setTenantId(testTenantId);
        bo.setAppName("单元测试子应用");
        bo.setClientAuthMethod("client_secret_basic");

        // 1. 新增应用
        SysApplicationVo vo = applicationService.insertByBo(bo);
        Assertions.assertNotNull(vo);
        Assertions.assertNotNull(vo.getId());
        Assertions.assertEquals(testTenantId, vo.getAppId()); // 默认 appId = tenantId
        Assertions.assertNotNull(vo.getClientSecret()); // 明文返回一次

        String originalSecret = vo.getClientSecret();

        // 2. 查库验证
        SysApplicationVo queried = applicationService.queryById(vo.getId());
        Assertions.assertNotNull(queried);
        Assertions.assertEquals("单元测试子应用", queried.getAppName());

        // 3. 重置密钥
        String newSecret = applicationService.resetSecret(vo.getId());
        Assertions.assertNotNull(newSecret);
        Assertions.assertNotEquals(originalSecret, newSecret);

        // 4. 客户端ID/租户唯一性校验
        SysApplicationBo duplicateTenant = new SysApplicationBo();
        duplicateTenant.setTenantId(testTenantId);
        Assertions.assertFalse(applicationService.checkTenantIdUnique(duplicateTenant));

        SysApplicationBo duplicateAppId = new SysApplicationBo();
        duplicateAppId.setAppId(testTenantId);
        Assertions.assertFalse(applicationService.checkAppIdUnique(duplicateAppId));
    }

    @Tag("dev")
    @DisplayName("测试集成平台门户应用受保护机制（不允许停用/删除）")
    @Test
    @Transactional
    public void testPortalProtection() {
        // 先确保有 000000 门户应用记录
        SysApplicationVo portal = applicationService.queryByTenantId(TenantConstants.DEFAULT_TENANT_ID);
        if (portal == null) {
            SysApplicationBo bo = new SysApplicationBo();
            bo.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
            bo.setAppId(TenantConstants.DEFAULT_TENANT_ID);
            bo.setAppName("集成平台门户");
            portal = applicationService.insertByBo(bo);
        }

        final Long portalId = portal.getId();

        // 1. 尝试删除门户
        Assertions.assertThrows(ServiceException.class, () -> {
            applicationService.deleteWithValidByIds(List.of(portalId), true);
        });

        // 2. 尝试停用门户
        Assertions.assertThrows(ServiceException.class, () -> {
            applicationService.updateApplicationStatus(portalId, "1");
        });
    }

    @Tag("dev")
    @DisplayName("测试菜单仅menu_type='C'可保留appId，其它类型强制清空")
    @Test
    @Transactional
    public void testMenuAppIdConstraint() {
        // 1. menu_type='C' 可以保存 appId
        SysMenuBo menuC = new SysMenuBo();
        menuC.setMenuName("测试子应用入口菜单" + System.currentTimeMillis());
        menuC.setParentId(0L);
        menuC.setOrderNum(999);
        menuC.setPath("testAppEntry");
        menuC.setComponent("system/tenant/index");
        menuC.setMenuType(SystemConstants.TYPE_MENU); // 'C'
        menuC.setAppId("app_test_123");
        menuC.setVisible("0");
        menuC.setStatus("0");
        menuC.setIsFrame("1");

        menuService.insertMenu(menuC);
        Assertions.assertNotNull(menuC.getMenuId());

        SysMenuVo savedC = menuService.selectMenuById(menuC.getMenuId());
        Assertions.assertNotNull(savedC);
        Assertions.assertEquals("app_test_123", savedC.getAppId());

        // 2. 修改为 menu_type='M'（目录），强制清空 appId
        menuC.setMenuType(SystemConstants.TYPE_DIR); // 'M'
        menuC.setComponent("");
        menuService.updateMenu(menuC);

        SysMenuVo updatedM = menuService.selectMenuById(menuC.getMenuId());
        Assertions.assertNotNull(updatedM);
        Assertions.assertNull(updatedM.getAppId());
    }

    @Tag("dev")
    @DisplayName("测试用户工号唯一性检查")
    @Test
    @Transactional
    public void testUserEmployeeNoUnique() {
        String empNo = "EMP_TEST_" + System.currentTimeMillis();

        SysUserBo userA = new SysUserBo();
        userA.setUserName("test_user_a_" + System.currentTimeMillis());
        userA.setNickName("测试用户A");
        userA.setDeptId(103L);
        userA.setPassword("123456");
        userA.setEmployeeNo(empNo);
        userService.insertUser(userA);

        // 重复工号检测
        SysUserBo userB = new SysUserBo();
        userB.setUserName("test_user_b_" + System.currentTimeMillis());
        userB.setEmployeeNo(empNo);
        Assertions.assertFalse(userService.checkEmployeeNoUnique(userB));

        // 不同工号检测
        SysUserBo userC = new SysUserBo();
        userC.setEmployeeNo(empNo + "_diff");
        Assertions.assertTrue(userService.checkEmployeeNoUnique(userC));

        // 空工号允许
        SysUserBo userEmpty = new SysUserBo();
        userEmpty.setEmployeeNo("");
        Assertions.assertTrue(userService.checkEmployeeNoUnique(userEmpty));
    }

}
