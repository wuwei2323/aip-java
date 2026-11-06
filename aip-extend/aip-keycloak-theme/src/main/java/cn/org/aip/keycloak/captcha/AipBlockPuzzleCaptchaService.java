package cn.org.aip.keycloak.captcha;

import com.anji.captcha.model.common.RepCodeEnum;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.impl.BlockPuzzleCaptchaServiceImpl;
import com.anji.captcha.util.ImageUtils;
import com.anji.captcha.util.StringUtils;

import java.awt.image.BufferedImage;

/**
 * 覆写 AJ-Captcha 的 {@code get()}，跳过底图文字水印绘制。
 *
 * <p>拼图滑块不需要文字水印；父类即使水印为空也会调用
 * {@code Graphics.setFont()} / {@code drawString()}，在缺少 fontconfig 的
 * Keycloak 精简容器中会抛出 {@code Fontconfig head is null}。
 * 图片裁剪、目标坐标生成与挑战缓存写入仍完全复用父类
 * {@link BlockPuzzleCaptchaServiceImpl#pictureTemplatesCut}。</p>
 *
 * @author AIP
 */
public class AipBlockPuzzleCaptchaService extends BlockPuzzleCaptchaServiceImpl {

    @Override
    public ResponseModel get(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            return ResponseModel.errorMsg(RepCodeEnum.NULL_ERROR);
        }
        BufferedImage original = ImageUtils.getOriginal();
        if (original == null) {
            return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_BASEMAP_NULL);
        }
        String slidingBlock = ImageUtils.getslidingBlock();
        if (StringUtils.isBlank(slidingBlock)) {
            return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_BASEMAP_NULL);
        }
        BufferedImage block = ImageUtils.getBase64StrToImage(slidingBlock);
        if (block == null) {
            return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_BASEMAP_NULL);
        }

        CaptchaVO data = pictureTemplatesCut(original, block, slidingBlock);
        if (data == null
            || StringUtils.isBlank(data.getJigsawImageBase64())
            || StringUtils.isBlank(data.getOriginalImageBase64())) {
            return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_ERROR);
        }
        return ResponseModel.successData(data);
    }
}
