package cn.org.aip.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import cn.org.aip.common.core.constant.TenantConstants;
import cn.org.aip.common.core.domain.R;
import cn.org.aip.common.core.utils.MessageUtils;
import cn.org.aip.common.core.validate.AddGroup;
import cn.org.aip.common.core.validate.EditGroup;
import cn.org.aip.common.idempotent.annotation.RepeatSubmit;
import cn.org.aip.common.log.annotation.Log;
import cn.org.aip.common.log.enums.BusinessType;
import cn.org.aip.common.mybatis.core.page.PageQuery;
import cn.org.aip.common.mybatis.core.page.TableDataInfo;
import cn.org.aip.common.tenant.helper.TenantHelper;
import cn.org.aip.common.web.core.BaseController;
import cn.org.aip.system.domain.bo.SysApplicationBo;
import cn.org.aip.system.domain.vo.SysApplicationVo;
import cn.org.aip.system.service.ISysApplicationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用管理
 *
 * @author Michelle.Chung
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/application")
public class SysApplicationController extends BaseController {

    private final ISysApplicationService applicationService;

    /**
     * 查询应用列表
     */
    @SaCheckPermission("system:application:list")
    @GetMapping("/list")
    public TableDataInfo<SysApplicationVo> list(SysApplicationBo bo, PageQuery pageQuery) {
        return applicationService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取应用详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:application:query")
    @GetMapping("/{id}")
    public R<SysApplicationVo> getInfo(@NotNull(message = "主键不能为空")
                                       @PathVariable Long id) {
        return R.ok(applicationService.queryById(id));
    }

    /**
     * 新增应用
     */
    @SaCheckPermission("system:application:add")
    @Log(title = "应用管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<SysApplicationVo> add(@Validated(AddGroup.class) @RequestBody SysApplicationBo bo) {
        if (bo.getAppId() != null && !applicationService.checkAppIdUnique(bo)) {
            return R.fail(MessageUtils.message("application.appId.exists", bo.getAppName()));
        }
        return R.ok(applicationService.insertByBo(bo));
    }

    /**
     * 修改应用
     */
    @SaCheckPermission("system:application:edit")
    @Log(title = "应用管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysApplicationBo bo) {
        if (!applicationService.checkAppIdUnique(bo)) {
            return R.fail(MessageUtils.message("application.appId.edit.exists", bo.getAppName()));
        }
        return toAjax(applicationService.updateByBo(bo));
    }

    /**
     * 修改应用状态
     */
    @SaCheckPermission("system:application:edit")
    @Log(title = "应用管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysApplicationBo bo) {
        return toAjax(applicationService.updateApplicationStatus(bo.getId(), bo.getStatus()));
    }

    /**
     * 重置客户端密钥
     *
     * @param id 主键
     */
    @SaCheckPermission("system:application:resetSecret")
    @Log(title = "应用管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/resetSecret")
    public R<String> resetSecret(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(applicationService.resetSecret(id));
    }

    /**
     * 删除应用
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:application:remove")
    @Log(title = "应用管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(applicationService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 动态切换应用
     *
     * @param tenantId 应用ID
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @GetMapping("/dynamic/{tenantId}")
    public R<Void> dynamicTenant(@NotBlank(message = "应用ID不能为空") @PathVariable String tenantId) {
        TenantHelper.setDynamic(tenantId, true);
        return R.ok();
    }

    /**
     * 清除动态应用
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @GetMapping("/dynamic/clear")
    public R<Void> dynamicClear() {
        TenantHelper.clearDynamic();
        return R.ok();
    }

}
