package cn.org.aip.system.service;

import cn.org.aip.common.mybatis.core.page.PageQuery;
import cn.org.aip.common.mybatis.core.page.TableDataInfo;
import cn.org.aip.system.domain.bo.SysApplicationBo;
import cn.org.aip.system.domain.vo.SysApplicationVo;

import java.util.Collection;
import java.util.List;

/**
 * 应用注册 Service 接口
 *
 * @author Michelle.Chung
 */
public interface ISysApplicationService {

    /**
     * 查询应用详情
     */
    SysApplicationVo queryById(Long id);

    /**
     * 根据客户端ID查询应用
     */
    SysApplicationVo queryByAppId(String appId);

    /**
     * 根据租户编号查询应用
     */
    SysApplicationVo queryByTenantId(String tenantId);

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
    Boolean updateByBo(SysApplicationBo bo);

    /**
     * 修改应用状态
     */
    int updateApplicationStatus(Long id, String status);

    /**
     * 重置客户端密钥
     *
     * @param id 应用主键
     * @return 新的明文密钥
     */
    String resetSecret(Long id);

    /**
     * 批量删除应用
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验客户端ID是否唯一
     */
    boolean checkAppIdUnique(SysApplicationBo bo);

    /**
     * 校验租户编号是否唯一
     */
    boolean checkTenantIdUnique(SysApplicationBo bo);

}
