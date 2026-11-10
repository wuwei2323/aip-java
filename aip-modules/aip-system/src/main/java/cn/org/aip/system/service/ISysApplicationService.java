package cn.org.aip.system.service;

import cn.org.aip.common.mybatis.core.page.PageQuery;
import cn.org.aip.common.mybatis.core.page.TableDataInfo;
import cn.org.aip.system.domain.bo.SysApplicationBo;
import cn.org.aip.system.domain.vo.SysApplicationVo;

import java.util.Collection;
import java.util.List;

/**
 * 应用管理 Service 接口
 *
 * @author Michelle.Chung
 */
public interface ISysApplicationService {

    /**
     * 查询应用详情
     */
    SysApplicationVo queryById(Long id);

    /**
     * 根据应用编码(client_id)查询应用
     */
    SysApplicationVo queryByAppId(String appId);

    /**
     * 兼容方法：根据租户/应用编号查询应用
     */
    default SysApplicationVo queryByTenantId(String tenantId) {
        return queryByAppId(tenantId);
    }

    /**
     * 查询应用分页列表
     */
    TableDataInfo<SysApplicationVo> queryPageList(SysApplicationBo bo, PageQuery pageQuery);

    /**
     * 查询应用列表
     */
    List<SysApplicationVo> queryList(SysApplicationBo bo);

    /**
     * 新增应用
     */
    SysApplicationVo insertByBo(SysApplicationBo bo);

    /**
     * 修改应用
     */
    SysApplicationVo updateByBo(SysApplicationBo bo);

    /**
     * 修改应用状态，返回保存后的配置与本次同步结果。
     */
    SysApplicationVo updateApplicationStatus(Long id, String status);

    /**
     * 显式重试配置同步；不重置已有客户端密钥。
     *
     * @param id 应用主键
     * @return 同步结果，不包含明文密钥
     */
    SysApplicationVo syncApplication(Long id);

    /**
     * 重置客户端密钥
     *
     * @param id 应用主键
     * @return 新的明文密钥
     */
    String resetSecret(Long id);

    /**
     * 获取指定应用的明文密钥（内部调用，如门户）
     */
    String getDecryptedSecret(String appId);

    /**
     * 批量删除应用
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验应用编码是否唯一
     */
    boolean checkAppIdUnique(SysApplicationBo bo);

    /**
     * 校验租户编号是否唯一（兼容调用）
     */
    default boolean checkTenantIdUnique(SysApplicationBo bo) {
        return checkAppIdUnique(bo);
    }

}
