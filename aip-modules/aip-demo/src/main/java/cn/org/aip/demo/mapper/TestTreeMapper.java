package cn.org.aip.demo.mapper;

import cn.org.aip.common.mybatis.annotation.DataColumn;
import cn.org.aip.common.mybatis.annotation.DataPermission;
import cn.org.aip.common.mybatis.core.mapper.BaseMapperPlus;
import cn.org.aip.demo.domain.TestTree;
import cn.org.aip.demo.domain.vo.TestTreeVo;

/**
 * 测试树表Mapper接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public interface TestTreeMapper extends BaseMapperPlus<TestTree, TestTreeVo> {

}
