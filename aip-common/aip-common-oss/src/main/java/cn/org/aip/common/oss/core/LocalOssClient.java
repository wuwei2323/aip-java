package cn.org.aip.common.oss.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.config.properties.FileProperties;
import cn.org.aip.common.core.constant.Constants;
import cn.org.aip.common.core.constant.FileConstants;
import cn.org.aip.common.core.utils.DateUtils;
import cn.org.aip.common.core.utils.MessageUtils;
import cn.org.aip.common.core.utils.ServletUtils;
import cn.org.aip.common.core.utils.SpringUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.core.utils.file.FileUtils;
import cn.org.aip.common.oss.entity.UploadResult;
import cn.org.aip.common.oss.enums.AccessPolicyType;
import cn.org.aip.common.oss.exception.OssException;
import cn.org.aip.common.oss.properties.OssProperties;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 本地文件夹存储客户端实现
 *
 * @author AIP
 */
@Slf4j
public class LocalOssClient implements OssClient {

    private final String configKey;
    private final OssProperties properties;
    private final FileProperties fileProperties;

    public LocalOssClient(String configKey, OssProperties properties) {
        this(configKey, properties, getFilePropertiesFromContext());
    }

    public LocalOssClient(String configKey, OssProperties properties, FileProperties fileProperties) {
        this.configKey = configKey;
        this.properties = properties;
        this.fileProperties = fileProperties != null ? fileProperties : new FileProperties();
    }

    private static FileProperties getFilePropertiesFromContext() {
        try {
            return SpringUtils.getBean(FileProperties.class);
        } catch (Exception e) {
            log.warn("无法从Spring上下文获取FileProperties，将使用默认属性");
            return new FileProperties();
        }
    }

    private String getBasePath() {
        return fileProperties.getPath();
    }

    private File getLocalFile(String key) {
        String cleanKey = key.replace("\\", "/");
        if (cleanKey.startsWith("/")) {
            cleanKey = cleanKey.substring(1);
        }
        return new File(getBasePath(), cleanKey);
    }

    @Override
    public UploadResult upload(Path filePath, String key, String md5Digest, String contentType) {
        try {
            File targetFile = getLocalFile(key);
            FileUtil.mkdir(targetFile.getParentFile());
            FileUtil.copy(filePath.toFile(), targetFile, true);
            String eTag = StringUtils.isNotEmpty(md5Digest) ? md5Digest : IdUtil.fastSimpleUUID();
            return UploadResult.builder()
                .url(getUrl() + StringUtils.SLASH + key)
                .filename(key)
                .eTag(eTag)
                .build();
        } catch (Exception e) {
            throw new OssException(MessageUtils.message("upload.file.save.failed") + ": [" + e.getMessage() + "]");
        } finally {
            FileUtils.del(filePath);
        }
    }

    @Override
    public UploadResult upload(InputStream inputStream, String key, Long length, String contentType) {
        try {
            File targetFile = getLocalFile(key);
            FileUtil.mkdir(targetFile.getParentFile());
            FileUtil.writeFromStream(inputStream, targetFile);
            String eTag = IdUtil.fastSimpleUUID();
            return UploadResult.builder()
                .url(getUrl() + StringUtils.SLASH + key)
                .filename(key)
                .eTag(eTag)
                .build();
        } catch (Exception e) {
            throw new OssException(MessageUtils.message("upload.file.save.failed") + ": [" + e.getMessage() + "]");
        }
    }

    @Override
    public Path fileDownload(String path) {
        File targetFile = getLocalFile(removeBaseUrl(path));
        if (!targetFile.exists()) {
            throw new OssException(MessageUtils.message("upload.file.not.exists"));
        }
        return targetFile.toPath();
    }

    @Override
    public void download(String key, OutputStream out, Consumer<Long> consumer) {
        File targetFile = getLocalFile(key);
        if (!targetFile.exists() || !targetFile.isFile()) {
            throw new OssException(MessageUtils.message("upload.file.not.exists"));
        }
        if (consumer != null) {
            consumer.accept(targetFile.length());
        }
        try {
            FileUtil.writeToStream(targetFile, out);
        } catch (Exception e) {
            throw new OssException(MessageUtils.message("upload.file.read.failed") + ": [" + e.getMessage() + "]");
        }
    }

    @Override
    public void delete(String path) {
        try {
            File targetFile = getLocalFile(removeBaseUrl(path));
            if (targetFile.exists()) {
                FileUtils.del(targetFile);
            }
        } catch (Exception e) {
            throw new OssException(MessageUtils.message("upload.file.delete.failed") + ": [" + e.getMessage() + "]");
        }
    }

    @Override
    public String createPresignedGetUrl(String objectKey, Duration expiredTime) {
        return getUrl() + StringUtils.SLASH + objectKey;
    }

    @Override
    public String createPresignedPutUrl(String objectKey, Duration expiredTime, Map<String, String> metadata) {
        return getUrl() + StringUtils.SLASH + objectKey;
    }

    @Override
    public UploadResult uploadSuffix(byte[] data, String suffix, String contentType) {
        return upload(new ByteArrayInputStream(data), getPath(properties.getPrefix(), suffix), (long) data.length, contentType);
    }

    @Override
    public UploadResult uploadSuffix(InputStream inputStream, String suffix, Long length, String contentType) {
        return upload(inputStream, getPath(properties.getPrefix(), suffix), length, contentType);
    }

    @Override
    public UploadResult uploadSuffix(File file, String suffix) {
        return upload(file.toPath(), getPath(properties.getPrefix(), suffix), null, FileUtils.getMimeType(suffix));
    }

    @Override
    public InputStream getObjectContent(String path) throws IOException {
        File targetFile = getLocalFile(removeBaseUrl(path));
        if (!targetFile.exists()) {
            throw new OssException(MessageUtils.message("upload.file.not.exists"));
        }
        return Files.newInputStream(targetFile.toPath());
    }

    @Override
    public String getEndpoint() {
        return "local";
    }

    @Override
    public String getDomain() {
        return getUrl();
    }

    @Override
    public String getUrl() {
        String domain = properties.getDomain();
        String prefix = fileProperties.getPrefix();
        if (StringUtils.isBlank(prefix)) {
            prefix = FileConstants.DEFAULT_RESOURCE_PREFIX;
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        // 如果后台配置了明确的独立访问域名/CDN地址，则进行拼接
        // 否则返回纯相对路径（如 /static），彻底避免与协议、IP、端口强耦合，极大方便数据与物理文件跨环境迁移
        if (StringUtils.isNotBlank(domain)) {
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            return domain + prefix;
        }
        return prefix;
    }

    @Override
    public String getPath(String prefix, String suffix) {
        String uuid = IdUtil.fastSimpleUUID();
        String datePath = DateUtils.datePath();
        return StringUtils.isNotEmpty(prefix) ?
            prefix + StringUtils.SLASH + datePath + StringUtils.SLASH + uuid + suffix : datePath + StringUtils.SLASH + uuid + suffix;
    }

    @Override
    public String removeBaseUrl(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        String cleanPath = path;
        String urlPrefix = getUrl() + StringUtils.SLASH;
        if (cleanPath.contains(urlPrefix)) {
            cleanPath = cleanPath.substring(cleanPath.indexOf(urlPrefix) + urlPrefix.length());
        }
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        return cleanPath;
    }

    @Override
    public String getConfigKey() {
        return configKey;
    }

    @Override
    public String getIsHttps() {
        return Constants.HTTP;
    }

    @Override
    public boolean checkPropertiesSame(OssProperties properties) {
        return this.properties.equals(properties);
    }

    @Override
    public AccessPolicyType getAccessPolicy() {
        return AccessPolicyType.PUBLIC;
    }

}
