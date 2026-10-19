package cn.org.aip.system.service;

import cn.org.aip.common.mybatis.core.page.PageQuery;
import cn.org.aip.common.mybatis.core.page.TableDataInfo;
import cn.org.aip.system.domain.bo.SysTenantBo;
import cn.org.aip.system.domain.vo.SysTenantVo;

import java.util.Collection;
import java.util.List;

/**
 * 应用Service接口
 *
 * @author Michelle.Chung
 */
public interface ISysTenantService {

    /**
     * 查询应用
     */
    SysTenantVo queryById(Long id);

    /**
     * 基于应用ID查询应用
     */
    SysTenantVo queryByTenantId(String tenantId);

    /**
     * 查询应用列表
     */
    TableDataInfo<SysTenantVo> queryPageList(SysTenantBo bo, PageQuery pageQuery);

    /**
     * 查询应用列表
     */
    List<SysTenantVo> queryList(SysTenantBo bo);

    /**
     * 新增应用
     */
    Boolean insertByBo(SysTenantBo bo);

    /**
     * 修改应用
     */
    Boolean updateByBo(SysTenantBo bo);

    /**
     * 修改应用状态
     */
    int updateTenantStatus(SysTenantBo bo);

    /**
     * 校验应用是否允许操作
     */
    void checkTenantAllowed(String tenantId);

    /**
     * 校验并批量删除应用信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验企业名称是否唯一
     */
    boolean checkCompanyNameUnique(SysTenantBo bo);

    /**
     * 校验账号余额
     */
    boolean checkAccountBalance(String tenantId);

    /**
     * 校验有效期
     */
    boolean checkExpireTime(String tenantId);

    /**
     * 同步应用套餐
     */
    Boolean syncTenantPackage(String tenantId, Long packageId);

    /**
     * 同步应用字典
     */
    void syncTenantDict();

    /**
     * 同步应用参数配置
     */
    void syncTenantConfig();
}
