package cn.org.aip.test;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.system.mapper.SysTenantMapper;
import cn.org.aip.system.domain.SysTenant;
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
 * 阶段一单测：应用管理与菜单/用户改造
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

    @Autowired
    private SysTenantMapper tenantMapper;

    @Tag("dev")
    @DisplayName("测试系统唯一租户数据(集成平台 000000)正确存在")
    @Test
    public void testIntegrationPlatformTenantExists() {
        SysTenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getTenantId, TenantConstants.DEFAULT_TENANT_ID));
        Assertions.assertNotNull(tenant);
        Assertions.assertEquals("000000", tenant.getTenantId());
        Assertions.assertEquals("集成平台", tenant.getCompanyName());
    }

    @Tag("dev")
    @DisplayName("测试应用自动生成编码、新增、密钥AES加密与重置密钥")
    @Test
    @Transactional
    public void testApplicationCrudAndSecret() {
        SysApplicationBo bo = new SysApplicationBo();
        bo.setAppName("单元测试应用");
        bo.setCategory("system");
        bo.setDescription("单元测试应用简介");
        bo.setIcon("https://example.com/icon.png");
        bo.setEntryUrl("https://app.example.com");
        bo.setRedirectUris("https://app.example.com/oauth/callback");

        // 1. 新增应用 (未提供 appId，系统自动生成规律递增编码 APP-001 等，tenantId 默认为 000000)
        SysApplicationVo vo = applicationService.insertByBo(bo);
        Assertions.assertNotNull(vo);
        Assertions.assertNotNull(vo.getId());
        Assertions.assertNotNull(vo.getAppId());
        Assertions.assertTrue(vo.getAppId().matches("^APP-\\d{3,}$"));
        Assertions.assertEquals(TenantConstants.DEFAULT_TENANT_ID, vo.getTenantId()); // tenantId 默认是 000000
        Assertions.assertNotNull(vo.getClientSecret()); // 明文返回一次

        String generatedAppId = vo.getAppId();
        String originalSecret = vo.getClientSecret();

        // 2. 查库验证与明文密钥解密获取
        SysApplicationVo queried = applicationService.queryById(vo.getId());
        Assertions.assertNotNull(queried);
        Assertions.assertEquals("单元测试应用", queried.getAppName());
        Assertions.assertEquals("system", queried.getCategory());
        Assertions.assertEquals("单元测试应用简介", queried.getDescription());
        Assertions.assertEquals(generatedAppId, queried.getAppId());
        Assertions.assertEquals(TenantConstants.DEFAULT_TENANT_ID, queried.getTenantId());

        String decryptedSecret = applicationService.getDecryptedSecret(generatedAppId);
        Assertions.assertEquals(originalSecret, decryptedSecret);

        // 3. 重置密钥
        String newSecret = applicationService.resetSecret(vo.getId());
        Assertions.assertNotNull(newSecret);
        Assertions.assertNotEquals(originalSecret, newSecret);
        Assertions.assertEquals(newSecret, applicationService.getDecryptedSecret(generatedAppId));

        // 4. 应用编码唯一性校验
        SysApplicationBo duplicateApp = new SysApplicationBo();
        duplicateApp.setAppId(generatedAppId);
        Assertions.assertFalse(applicationService.checkAppIdUnique(duplicateApp));

        // 5. 清理测试应用（同步删除 Keycloak 客户端）
        applicationService.deleteWithValidByIds(List.of(vo.getId()), true);
    }

    @Tag("dev")
    @DisplayName("测试应用编码规律递增生成(APP-001, APP-002...)")
    @Test
    @Transactional
    public void testSequentialAppIdGeneration() {
        SysApplicationBo bo1 = new SysApplicationBo();
        bo1.setAppName("递增应用1");
        bo1.setCategory("system");
        bo1.setDescription("简介1");
        bo1.setIcon("https://example.com/icon1.png");
        bo1.setEntryUrl("https://app1.example.com");
        bo1.setRedirectUris("https://app1.example.com/oauth/callback");

        SysApplicationVo vo1 = applicationService.insertByBo(bo1);
        Assertions.assertNotNull(vo1.getAppId());
        Assertions.assertTrue(vo1.getAppId().matches("^APP-\\d{3,}$"));

        SysApplicationBo bo2 = new SysApplicationBo();
        bo2.setAppName("递增应用2");
        bo2.setCategory("system");
        bo2.setDescription("简介2");
        bo2.setIcon("https://example.com/icon2.png");
        bo2.setEntryUrl("https://app2.example.com");
        bo2.setRedirectUris("https://app2.example.com/oauth/callback");

        SysApplicationVo vo2 = applicationService.insertByBo(bo2);
        Assertions.assertNotNull(vo2.getAppId());
        Assertions.assertTrue(vo2.getAppId().matches("^APP-\\d{3,}$"));
        Assertions.assertEquals(TenantConstants.DEFAULT_TENANT_ID, vo1.getTenantId());
        Assertions.assertEquals(TenantConstants.DEFAULT_TENANT_ID, vo2.getTenantId());

        int num1 = Integer.parseInt(vo1.getAppId().substring(4));
        int num2 = Integer.parseInt(vo2.getAppId().substring(4));
        Assertions.assertEquals(num1 + 1, num2);

        // 清理测试应用（同步删除 Keycloak 客户端）
        applicationService.deleteWithValidByIds(List.of(vo1.getId(), vo2.getId()), true);
    }

    @Tag("dev")
    @DisplayName("测试集成平台门户应用受保护机制（不允许停用/删除）")
    @Test
    @Transactional
    public void testPortalProtection() {
        // 先确保有 000000 门户应用记录
        SysApplicationVo portal = applicationService.queryByAppId(TenantConstants.DEFAULT_TENANT_ID);
        if (portal == null) {
            SysApplicationBo bo = new SysApplicationBo();
            bo.setAppId(TenantConstants.DEFAULT_TENANT_ID);
            bo.setAppName("集成平台门户");
            bo.setCategory("system");
            bo.setDescription("集成平台门户系统");
            bo.setIcon("https://example.com/portal.png");
            bo.setEntryUrl("/");
            bo.setRedirectUris("https://localhost:81/sso/callback");
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
        menuC.setComponent("system/application/index");
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

    @Tag("dev")
    @DisplayName("测试菜单分组groupId增删改查与分组过滤")
    @Test
    @Transactional
    public void testMenuGroupIdCrudAndFilter() {
        // 1. 新增菜单不设置 groupId，默认应该为 1 (管理后台)
        SysMenuBo defaultMenu = new SysMenuBo();
        defaultMenu.setMenuName("默认分组菜单" + System.currentTimeMillis());
        defaultMenu.setParentId(0L);
        defaultMenu.setOrderNum(991);
        defaultMenu.setPath("testDefaultGroup");
        defaultMenu.setComponent("system/menu/index");
        defaultMenu.setMenuType(SystemConstants.TYPE_MENU);
        defaultMenu.setVisible("0");
        defaultMenu.setStatus("0");
        defaultMenu.setIsFrame("1");
        menuService.insertMenu(defaultMenu);
        Assertions.assertNotNull(defaultMenu.getMenuId());

        SysMenuVo savedDefault = menuService.selectMenuById(defaultMenu.getMenuId());
        Assertions.assertNotNull(savedDefault);
        Assertions.assertEquals(1L, savedDefault.getGroupId());

        // 2. 新增菜单指定 groupId = 2 (集成平台)
        SysMenuBo group2Menu = new SysMenuBo();
        group2Menu.setMenuName("集成平台菜单" + System.currentTimeMillis());
        group2Menu.setParentId(0L);
        group2Menu.setOrderNum(992);
        group2Menu.setPath("testGroup2");
        group2Menu.setComponent("system/menu/index");
        group2Menu.setMenuType(SystemConstants.TYPE_MENU);
        group2Menu.setVisible("0");
        group2Menu.setStatus("0");
        group2Menu.setIsFrame("1");
        group2Menu.setGroupId(2L);
        menuService.insertMenu(group2Menu);
        Assertions.assertNotNull(group2Menu.getMenuId());

        SysMenuVo savedGroup2 = menuService.selectMenuById(group2Menu.getMenuId());
        Assertions.assertNotNull(savedGroup2);
        Assertions.assertEquals(2L, savedGroup2.getGroupId());

        // 3. 修改菜单 groupId：从 2 改为 1
        group2Menu.setGroupId(1L);
        menuService.updateMenu(group2Menu);
        SysMenuVo updated = menuService.selectMenuById(group2Menu.getMenuId());
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(1L, updated.getGroupId());

        // 4. 按 groupId 过滤查询验证
        SysMenuBo queryBo1 = new SysMenuBo();
        queryBo1.setGroupId(1L);
        List<SysMenuVo> list1 = menuService.selectMenuList(queryBo1, 1L);
        Assertions.assertTrue(list1.stream().anyMatch(m -> m.getMenuId().equals(defaultMenu.getMenuId())));

        SysMenuBo queryBo2 = new SysMenuBo();
        queryBo2.setGroupId(2L);
        List<SysMenuVo> list2 = menuService.selectMenuList(queryBo2, 1L);
        Assertions.assertFalse(list2.stream().anyMatch(m -> m.getMenuId().equals(defaultMenu.getMenuId())));

        // 5. 验证 getRouters 对应的 selectMenuTreeByUserId(userId, groupId) 与 buildMenus 适配
        List<cn.org.aip.system.domain.SysMenu> routerMenusGroup1 = menuService.selectMenuTreeByUserId(1L, 1L);
        Assertions.assertTrue(routerMenusGroup1.stream().anyMatch(m -> m.getMenuId().equals(defaultMenu.getMenuId())));
        Assertions.assertFalse(routerMenusGroup1.stream().anyMatch(m -> m.getMenuId().equals(group2Menu.getMenuId())));

        List<cn.org.aip.system.domain.SysMenu> routerMenusGroup2 = menuService.selectMenuTreeByUserId(1L, 2L);
        Assertions.assertTrue(routerMenusGroup2.stream().anyMatch(m -> m.getMenuId().equals(group2Menu.getMenuId())));
        Assertions.assertFalse(routerMenusGroup2.stream().anyMatch(m -> m.getMenuId().equals(defaultMenu.getMenuId())));

        // 验证 buildMenus 生成的 RouterVo 与 MetaVo 包含 groupId
        List<cn.org.aip.system.domain.vo.RouterVo> routerVos = menuService.buildMenus(routerMenusGroup2);
        cn.org.aip.system.domain.vo.RouterVo targetRouter = routerVos.stream()
            .filter(r -> r.getName().contains(String.valueOf(group2Menu.getMenuId())))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(targetRouter);
        Assertions.assertEquals(2L, targetRouter.getGroupId());
        Assertions.assertEquals(2L, targetRouter.getMeta().getGroupId());

        // 6. 删除菜单
        menuService.deleteMenuById(defaultMenu.getMenuId());
        Assertions.assertNull(menuService.selectMenuById(defaultMenu.getMenuId()));
        menuService.deleteMenuById(group2Menu.getMenuId());
        Assertions.assertNull(menuService.selectMenuById(group2Menu.getMenuId()));
    }

}
