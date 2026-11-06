package cn.org.aip.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.constant.HttpStatus;
import cn.org.aip.common.core.constant.SystemConstants;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.domain.R;
import cn.org.aip.common.core.enums.LoginType;
import cn.org.aip.common.core.exception.ServiceException;
import cn.org.aip.common.core.exception.user.UserException;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.mybatis.helper.DataPermissionHelper;
import cn.org.aip.common.sso.config.KeycloakProperties;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.system.domain.SysUser;
import cn.org.aip.system.domain.vo.SysDeptVo;
import cn.org.aip.system.mapper.SysUserMapper;
import cn.org.aip.system.service.ISysDeptService;
import cn.org.aip.web.domain.bo.KeycloakVerifyPasswordBo;
import cn.org.aip.web.domain.vo.KeycloakUserVo;
import cn.org.aip.web.domain.vo.KeycloakVerifyPasswordVo;
import cn.org.aip.web.service.SysLoginService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Keycloak User Storage SPI 专用的平台内部安全查询与校验接口
 * 必须校验 X-Internal-Secret 共享密钥，严禁暴露到公网
 *
 * @author Michelle.Chung
 */
@Slf4j
@SaIgnore
@Validated
@RestController
@RequestMapping("/internal/keycloak")
@RequiredArgsConstructor
public class KeycloakInternalController {

    public static final String HEADER_INTERNAL_SECRET = "X-Internal-Secret";

    private final KeycloakProperties keycloakProperties;
    private final SysUserMapper userMapper;
    private final ISysDeptService deptService;
    private final SysLoginService sysLoginService;

    /**
     * 密码校验 (仅由 Keycloak User Storage SPI 的 CredentialInputValidator 调用)
     */
    @PostMapping("/verify-password")
    public R<KeycloakVerifyPasswordVo> verifyPassword(
        @RequestHeader(value = HEADER_INTERNAL_SECRET, required = false) String secret,
        @Validated @RequestBody KeycloakVerifyPasswordBo bo) {

        checkInternalSecret(secret);

        SysUser user = findUserByUsernameOrEmployeeNo(bo.getUsername());
        if (user == null) {
            log.info("Keycloak verify-password: user {} not found", bo.getUsername());
            return R.ok(KeycloakVerifyPasswordVo.builder()
                .valid(false)
                .message("用户不存在")
                .build());
        }

        if (SystemConstants.DISABLE.equals(user.getStatus())) {
            log.info("Keycloak verify-password: user {} is blocked", bo.getUsername());
            return R.ok(KeycloakVerifyPasswordVo.builder()
                .valid(false)
                .status(user.getStatus())
                .message("用户已被停用")
                .build());
        }

        // 调用平台登录锁定与重试策略 (BCrypt 比对)
        try {
            sysLoginService.checkLogin(
                LoginType.PASSWORD,
                TenantConstants.DEFAULT_TENANT_ID,
                user.getUserName(),
                () -> !BCrypt.checkpw(bo.getPassword(), user.getPassword())
            );
        } catch (UserException ue) {
            log.warn("Keycloak verify-password: user {} checkLogin failed: {}", bo.getUsername(), ue.getMessage());
            return R.ok(KeycloakVerifyPasswordVo.builder()
                .valid(false)
                .message(ue.getMessage())
                .build());
        }

        return R.ok(KeycloakVerifyPasswordVo.builder()
            .valid(true)
            .userId(user.getUserId())
            .username(user.getUserName())
            .employeeNo(user.getEmployeeNo())
            .status(user.getStatus())
            .passwordResetRequired(user.getPasswordResetRequired())
            .build());
    }

    /**
     * 根据用户名/工号查询用户信息 (供 UserLookupProvider.getUserByUsername 调用)
     */
    @GetMapping("/users/{username}")
    public R<KeycloakUserVo> getUserByUsername(
        @RequestHeader(value = HEADER_INTERNAL_SECRET, required = false) String secret,
        @PathVariable("username") String username) {

        checkInternalSecret(secret);

        SysUser user = findUserByUsernameOrEmployeeNo(username);
        if (user == null) {
            return R.ok(null);
        }
        return R.ok(toKeycloakUserVo(user));
    }

    /**
     * 根据平台用户ID查询用户信息 (供 UserLookupProvider.getUserById 调用)
     */
    @GetMapping("/users/by-id/{id}")
    public R<KeycloakUserVo> getUserById(
        @RequestHeader(value = HEADER_INTERNAL_SECRET, required = false) String secret,
        @PathVariable("id") Long id) {

        checkInternalSecret(secret);

        SysUser user = DataPermissionHelper.ignore(() -> userMapper.selectById(id));
        if (user == null || "1".equals(user.getDelFlag())) {
            return R.ok(null);
        }
        return R.ok(toKeycloakUserVo(user));
    }

    /**
     * 分页搜索用户 (供 Keycloak 管理台 UserQueryProvider 调用)
     */
    @GetMapping("/users")
    public R<Map<String, Object>> searchUsers(
        @RequestHeader(value = HEADER_INTERNAL_SECRET, required = false) String secret,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "first", defaultValue = "0") int first,
        @RequestParam(value = "max", defaultValue = "20") int max) {

        checkInternalSecret(secret);

        int pageNum = (first / Math.max(max, 1)) + 1;
        Page<SysUser> page = new Page<>(pageNum, max);

        LambdaQueryWrapper<SysUser> lqw = new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getDelFlag, "0");
        if (StringUtils.isNotBlank(search)) {
            lqw.and(w -> w.like(SysUser::getUserName, search)
                .or().like(SysUser::getNickName, search)
                .or().like(SysUser::getEmployeeNo, search)
                .or().like(SysUser::getEmail, search)
                .or().like(SysUser::getPhonenumber, search));
        }
        lqw.orderByAsc(SysUser::getUserId);

        Page<SysUser> result = DataPermissionHelper.ignore(() -> userMapper.selectPage(page, lqw));
        List<KeycloakUserVo> rows = new ArrayList<>();
        if (result.getRecords() != null) {
            for (SysUser user : result.getRecords()) {
                rows.add(toKeycloakUserVo(user));
            }
        }

        Map<String, Object> map = new HashMap<>(2);
        map.put("total", result.getTotal());
        map.put("rows", rows);
        return R.ok(map);
    }

    private void checkInternalSecret(String secret) {
        String expected = keycloakProperties.getInternalSecret();
        if (StringUtils.isBlank(expected) || !StringUtils.equals(expected, secret)) {
            log.warn("Unauthorized access to /internal/keycloak, provided secret is invalid");
            throw new ServiceException("未授权的内部访问: 共享密钥无效", HttpStatus.FORBIDDEN);
        }
    }

    private SysUser findUserByUsernameOrEmployeeNo(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }
        return DataPermissionHelper.ignore(() -> userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .and(w -> w.eq(SysUser::getUserName, username)
                    .or().eq(SysUser::getEmployeeNo, username)
                    .or().eq(SysUser::getKeycloakUserId, username))
                .eq(SysUser::getDelFlag, "0")
                .last("LIMIT 1")
        ));
    }

    private KeycloakUserVo toKeycloakUserVo(SysUser user) {
        // Keycloak 内部调用不携带租户上下文，显式以门户默认应用执行属性转换，
        // 避免部门缓存管理器因取不到应用 id 而写错误日志并落入错误的缓存命名空间
        return TenantHelper.dynamic(TenantConstants.DEFAULT_TENANT_ID, () -> buildKeycloakUserVo(user));
    }

    private KeycloakUserVo buildKeycloakUserVo(SysUser user) {
        String deptName = "";
        if (ObjectUtil.isNotNull(user.getDeptId())) {
            Opt<SysDeptVo> deptOpt = Opt.of(user.getDeptId()).map(deptService::selectDeptById);
            deptName = deptOpt.map(SysDeptVo::getDeptName).orElse("");
        }

        // Keycloak 中的主用户名优先为工号，无工号时回退为平台账号
        String primaryUsername = StringUtils.isNotBlank(user.getEmployeeNo())
            ? user.getEmployeeNo() : user.getUserName();

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("aip_user_id", List.of(String.valueOf(user.getUserId())));
        if (StringUtils.isNotBlank(user.getEmployeeNo())) {
            attributes.put("employee_no", List.of(user.getEmployeeNo()));
        }
        if (user.getDeptId() != null) {
            attributes.put("dept_id", List.of(String.valueOf(user.getDeptId())));
        }
        if (StringUtils.isNotBlank(deptName)) {
            attributes.put("dept_name", List.of(deptName));
        }

        return KeycloakUserVo.builder()
            .id(String.valueOf(user.getUserId()))
            .username(primaryUsername)
            .userName(user.getUserName())
            .employeeNo(user.getEmployeeNo())
            .email(user.getEmail())
            .nickName(user.getNickName())
            .phonenumber(user.getPhonenumber())
            .enabled(SystemConstants.NORMAL.equals(user.getStatus()))
            .deptId(user.getDeptId())
            .deptName(deptName)
            .passwordResetRequired(user.getPasswordResetRequired())
            .attributes(attributes)
            .build();
    }

}
