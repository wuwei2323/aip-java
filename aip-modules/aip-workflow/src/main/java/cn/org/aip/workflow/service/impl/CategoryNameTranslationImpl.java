package cn.org.aip.workflow.service.impl;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.translation.annotation.TranslationType;
import cn.org.aip.common.translation.core.TranslationInterface;
import cn.org.aip.workflow.common.ConditionalOnEnable;
import cn.org.aip.workflow.common.constant.FlowConstant;
import cn.org.aip.workflow.service.IFlwCategoryService;
import org.springframework.stereotype.Service;

/**
 * 流程分类名称翻译实现
 *
 * @author AprilWind
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
@TranslationType(type = FlowConstant.CATEGORY_ID_TO_NAME)
public class CategoryNameTranslationImpl implements TranslationInterface<String> {

    private final IFlwCategoryService flwCategoryService;

    @Override
    public String translation(Object key, String other) {
        return flwCategoryService.selectCategoryNameById(Convert.toLong(key));
    }
}
