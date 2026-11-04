package cn.org.aip.common.core.config.properties;

import cn.org.aip.common.core.constant.FileConstants;
import cn.org.aip.common.core.utils.StringUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;

/**
 * 本地文件存储与静态资源映射配置属性
 *
 * @author AIP
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /**
     * 本地文件存储物理根目录，支持绝对路径或相对路径（如 ./uploadPath）
     */
    private String path = "./uploadPath";

    /**
     * 静态资源访问 URL 映射前缀，例如 /static
     */
    private String prefix = FileConstants.DEFAULT_RESOURCE_PREFIX;

    /**
     * 获取规范化物理路径（支持相对路径自动解析为当前工作目录下的绝对路径）
     */
    public String getPath() {
        if (StringUtils.isBlank(this.path)) {
            return "";
        }
        File file = new File(this.path);
        if (!file.isAbsolute()) {
            return Paths.get(this.path).toAbsolutePath().normalize().toString();
        }
        return Paths.get(this.path).normalize().toString();
    }
}
