package cn.org.aip.common.oss.core;

import cn.hutool.core.util.IdUtil;
import cn.org.aip.common.core.utils.DateUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.oss.entity.UploadResult;
import cn.org.aip.common.oss.enums.AccessPolicyType;
import cn.org.aip.common.oss.properties.OssProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 对象存储客户端通用接口（支持 S3 协议云存储与本地文件夹存储）
 *
 * @author Lion Li / AIP
 */
public interface OssClient {

    /**
     * 上传文件到对象存储
     *
     * @param filePath    本地文件路径
     * @param key         对象键
     * @param md5Digest   MD5 哈希值（可选）
     * @param contentType 文件内容类型
     * @return UploadResult
     */
    UploadResult upload(Path filePath, String key, String md5Digest, String contentType);

    /**
     * 上传输入流到对象存储
     *
     * @param inputStream 要上传的输入流
     * @param key         对象键
     * @param length      流长度
     * @param contentType 内容类型
     * @return UploadResult
     */
    UploadResult upload(InputStream inputStream, String key, Long length, String contentType);

    /**
     * 下载文件到本地临时文件
     *
     * @param path 对象路径
     * @return 临时文件路径
     */
    Path fileDownload(String path);

    /**
     * 下载文件到输出流
     *
     * @param key      对象键
     * @param out      输出流
     * @param consumer 处理函数
     */
    void download(String key, OutputStream out, Consumer<Long> consumer);

    /**
     * 下载写出订阅器（S3 默认实现，Local 可选择实现或空）
     */
    default WriteOutSubscriber<OutputStream> download(String key, Consumer<Long> contentLengthConsumer) {
        return out -> download(key, out, contentLengthConsumer);
    }

    /**
     * 删除指定路径下文件
     *
     * @param path 文件完整路径或对象键
     */
    void delete(String path);

    /**
     * 创建下载预签名 URL
     *
     * @param objectKey   对象键
     * @param expiredTime 有效时长
     * @return URL 字符串
     */
    String createPresignedGetUrl(String objectKey, Duration expiredTime);

    /**
     * 创建上传预签名 URL
     *
     * @param objectKey   对象键
     * @param expiredTime 有效时长
     * @param metadata    元数据
     * @return URL 字符串
     */
    String createPresignedPutUrl(String objectKey, Duration expiredTime, Map<String, String> metadata);

    /**
     * 上传 byte[] 数据（带后缀）
     */
    UploadResult uploadSuffix(byte[] data, String suffix, String contentType);

    /**
     * 上传输入流（带后缀）
     */
    UploadResult uploadSuffix(InputStream inputStream, String suffix, Long length, String contentType);

    /**
     * 上传文件对象（带后缀）
     */
    UploadResult uploadSuffix(File file, String suffix);

    /**
     * 获取文件输入流
     */
    InputStream getObjectContent(String path) throws IOException;

    /**
     * 获取访问终端点
     */
    String getEndpoint();

    /**
     * 获取自定义域名
     */
    String getDomain();

    /**
     * 获取访问基础 URL
     */
    String getUrl();

    /**
     * 默认生成唯一文件相对路径
     */
    default String getPath(String prefix, String suffix) {
        String uuid = IdUtil.fastSimpleUUID();
        String datePath = DateUtils.datePath();
        return StringUtils.isNotEmpty(prefix) ?
            prefix + StringUtils.SLASH + datePath + StringUtils.SLASH + uuid + suffix : datePath + StringUtils.SLASH + uuid + suffix;
    }

    /**
     * 移除路径中的基础URL部分，得到相对路径
     */
    default String removeBaseUrl(String path) {
        return path.replace(getUrl() + StringUtils.SLASH, "");
    }

    /**
     * 获取配置标识
     */
    String getConfigKey();

    /**
     * 获取是否使用 HTTPS 协议
     */
    String getIsHttps();

    /**
     * 检查配置是否相同
     */
    boolean checkPropertiesSame(OssProperties properties);

    /**
     * 获取存储桶权限策略
     */
    AccessPolicyType getAccessPolicy();

}
