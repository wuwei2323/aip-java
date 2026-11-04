package cn.org.aip.system.mapper;

import cn.org.aip.common.mybatis.core.mapper.BaseMapperPlus;
import cn.org.aip.system.domain.SysApplication;
import cn.org.aip.system.domain.vo.SysApplicationVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 应用注册 Mapper 接口
 *
 * @author Michelle.Chung
 */
public interface SysApplicationMapper extends BaseMapperPlus<SysApplication, SysApplicationVo> {

    /**
     * 查询所有已存在的应用编码（包含逻辑删除的数据，防止全局唯一索引冲突）
     *
     * @return 应用编码列表
     */
    @Select("select app_id from sys_application")
    List<String> selectAllAppIds();

    /**
     * 统计指定应用编码是否存在（包含逻辑删除的数据）
     *
     * @param appId 应用编码
     * @param id 排除的主键ID
     * @return 存在记录数
     */
    @Select("<script>" +
            "select count(1) from sys_application where app_id = #{appId} " +
            "<if test=\"id != null\"> and id != #{id} </if>" +
            "</script>")
    Long countByAppId(@Param("appId") String appId, @Param("id") Long id);

}
