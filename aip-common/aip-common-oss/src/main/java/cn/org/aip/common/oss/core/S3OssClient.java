package cn.org.aip.common.oss.core;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import cn.org.aip.common.core.constant.Constants;
import cn.org.aip.common.core.utils.DateUtils;
import cn.org.aip.common.core.utils.StringUtils;
import cn.org.aip.common.core.utils.file.FileUtils;
import cn.org.aip.common.oss.constant.OssConstant;
import cn.org.aip.common.oss.entity.UploadResult;
import cn.org.aip.common.oss.enums.AccessPolicyType;
import cn.org.aip.common.oss.exception.OssException;
import cn.org.aip.common.oss.properties.OssProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.*;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * S3 存储协议客户端实现（所有兼容S3协议的云厂商均支持：阿里云 腾讯云 七牛云 minio 等）
 *
 * @author AprilWind / AIP
 */
@Slf4j
public class S3OssClient implements OssClient {

    /**
     * 服务商
     */
    private final String configKey;

    /**
     * 配置属性
     */
    private final OssProperties properties;

    /**
     * Amazon S3 异步客户端
     */
    private final S3AsyncClient client;

    /**
     * 用于管理 S3 数据传输的高级工具
     */
    private final S3TransferManager transferManager;

    /**
     * AWS S3 预签名 URL 的生成器
     */
    private final S3Presigner presigner;

    /**
     * 构造方法
     *
     * @param configKey     配置键
     * @param ossProperties Oss配置属性
     */
    public S3OssClient(String configKey, OssProperties ossProperties) {
        this.configKey = configKey;
        this.properties = ossProperties;
        try {
            // 创建 AWS 认证信息
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));

            // MinIO 使用 HTTPS 限制使用域名访问，站点填域名。需要启用路径样式访问
            boolean isStyle = !StringUtils.containsAny(properties.getEndpoint(), OssConstant.CLOUD_SERVICE);

            // 创建AWS基于 Netty 的 S3 客户端
            this.client = S3AsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create(getEndpoint()))
                .region(of())
                .forcePathStyle(isStyle)
                .httpClient(NettyNioAsyncHttpClient.builder()
                    .connectionTimeout(Duration.ofSeconds(60))
                    .connectionAcquisitionTimeout(Duration.ofSeconds(30))
                    .maxConcurrency(100)
                    .maxPendingConnectionAcquires(1000)
                    .build())
                .build();

            //AWS基于 CRT 的 S3 AsyncClient 实例用作 S3 传输管理器的底层客户端
            this.transferManager = S3TransferManager.builder().s3Client(this.client).build();

            // 创建 S3 配置对象
            S3Configuration config = S3Configuration.builder().chunkedEncodingEnabled(false)
                .pathStyleAccessEnabled(isStyle).build();

            // 创建 预签名 URL 的生成器 实例，用于生成 S3 预签名 URL
            this.presigner = S3Presigner.builder()
                .region(of())
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create(getDomain()))
                .serviceConfiguration(config)
                .build();

        } catch (Exception e) {
            if (e instanceof OssException) {
                throw e;
            }
            throw new OssException("配置错误! 请检查系统配置:[" + e.getMessage() + "]");
        }
    }

    @Override
    public UploadResult upload(Path filePath, String key, String md5Digest, String contentType) {
        try {
            // 构建上传请求对象
            FileUpload fileUpload = transferManager.uploadFile(
                x -> x.putObjectRequest(
                        y -> y.bucket(properties.getBucketName())
                            .key(key)
                            .contentMD5(StringUtils.isNotEmpty(md5Digest) ? md5Digest : null)
                            .contentType(contentType)
                            .build())
                    .addTransferListener(LoggingTransferListener.create())
                    .source(filePath).build());

            // 等待上传完成并获取上传结果
            CompletedFileUpload uploadResult = fileUpload.completionFuture().join();
            String eTag = uploadResult.response().eTag();

            // 提取上传结果中的 ETag，并构建一个自定义的 UploadResult 对象
            return UploadResult.builder().url(getUrl() + StringUtils.SLASH + key).filename(key).eTag(eTag).build();
        } catch (Exception e) {
            throw new OssException("上传文件失败，请检查配置信息:[" + e.getMessage() + "]");
        } finally {
            // 无论上传是否成功，最终都会删除临时文件
            FileUtils.del(filePath);
        }
    }

    @Override
    public UploadResult upload(InputStream inputStream, String key, Long length, String contentType) {
        // 如果输入流不是 ByteArrayInputStream，则将其读取为字节数组再创建 ByteArrayInputStream
        if (!(inputStream instanceof ByteArrayInputStream)) {
            inputStream = new ByteArrayInputStream(IoUtil.readBytes(inputStream));
        }
        try {
            // 创建异步请求体（length如果为空会报错）
            BlockingInputStreamAsyncRequestBody body = BlockingInputStreamAsyncRequestBody.builder()
                .contentLength(length)
                .subscribeTimeout(Duration.ofSeconds(120))
                .build();

            // 使用 transferManager 进行上传
            Upload upload = transferManager.upload(
                x -> x.requestBody(body).addTransferListener(LoggingTransferListener.create())
                    .putObjectRequest(
                        y -> y.bucket(properties.getBucketName())
                            .key(key)
                            .contentType(contentType)
                            .build())
                    .build());

            // 将输入流写入请求体
            body.writeInputStream(inputStream);

            // 等待文件上传操作完成
            CompletedUpload uploadResult = upload.completionFuture().join();
            String eTag = uploadResult.response().eTag();

            // 提取上传结果中的 ETag，并构建一个自定义的 UploadResult 对象
            return UploadResult.builder().url(getUrl() + StringUtils.SLASH + key).filename(key).eTag(eTag).build();
        } catch (Exception e) {
            throw new OssException("上传文件失败，请检查配置信息:[" + e.getMessage() + "]");
        }
    }

    @Override
    public Path fileDownload(String path) {
        // 构建临时文件
        Path tempFilePath = FileUtils.createTempFile().toPath();
        // 使用 S3TransferManager 下载文件
        FileDownload downloadFile = transferManager.downloadFile(
            x -> x.getObjectRequest(
                    y -> y.bucket(properties.getBucketName())
                        .key(removeBaseUrl(path))
                        .build())
                .addTransferListener(LoggingTransferListener.create())
                .destination(tempFilePath)
                .build());
        // 等待文件下载操作完成
        downloadFile.completionFuture().join();
        return tempFilePath;
    }

    @Override
    public void download(String key, OutputStream out, Consumer<Long> consumer) {
        try {
            this.download(key, consumer).writeTo(out);
        } catch (Exception e) {
            throw new OssException("文件下载失败，错误信息:[" + e.getMessage() + "]");
        }
    }

    @Override
    public WriteOutSubscriber<OutputStream> download(String key, Consumer<Long> contentLengthConsumer) {
        try {
            // 构建下载请求
            DownloadRequest<ResponsePublisher<GetObjectResponse>> publisherDownloadRequest = DownloadRequest.builder()
                .getObjectRequest(y -> y.bucket(properties.getBucketName())
                    .key(key)
                    .build())
                .addTransferListener(LoggingTransferListener.create())
                .responseTransformer(AsyncResponseTransformer.toPublisher())
                .build();

            // 使用 S3TransferManager 下载文件
            Download<ResponsePublisher<GetObjectResponse>> publisherDownload = transferManager.download(publisherDownloadRequest);
            // 获取下载发布订阅转换器
            ResponsePublisher<GetObjectResponse> publisher = publisherDownload.completionFuture().join().result();
            // 执行文件大小消费者函数
            Optional.ofNullable(contentLengthConsumer)
                .ifPresent(lengthConsumer -> lengthConsumer.accept(publisher.response().contentLength()));

            // 构建写出订阅器对象
            return out -> {
                // 创建可写入的字节通道
                try (WritableByteChannel channel = Channels.newChannel(out)) {
                    // 订阅数据
                    publisher.subscribe(byteBuffer -> {
                        while (byteBuffer.hasRemaining()) {
                            try {
                                channel.write(byteBuffer);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }).join();
                }
            };
        } catch (Exception e) {
            throw new OssException("文件下载失败，错误信息:[" + e.getMessage() + "]");
        }
    }

    @Override
    public void delete(String path) {
        try {
            client.deleteObject(
                x -> x.bucket(properties.getBucketName())
                    .key(removeBaseUrl(path))
                    .build());
        } catch (Exception e) {
            throw new OssException("删除文件失败，请检查配置信息:[" + e.getMessage() + "]");
        }
    }

    @Override
    public String createPresignedGetUrl(String objectKey, Duration expiredTime) {
        URL url = presigner.presignGetObject(
                x -> x.signatureDuration(expiredTime)
                    .getObjectRequest(
                        y -> y.bucket(properties.getBucketName())
                            .key(objectKey)
                            .build())
                    .build())
            .url();
        return url.toExternalForm();
    }

    @Override
    public String createPresignedPutUrl(String objectKey, Duration expiredTime, Map<String, String> metadata) {
        URL url = presigner.presignPutObject(
                x -> x.signatureDuration(expiredTime)
                    .putObjectRequest(
                        y -> y.bucket(properties.getBucketName())
                            .key(objectKey)
                            .metadata(metadata)
                            .build())
                    .build())
            .url();
        return url.toExternalForm();
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
        Path tempFilePath = fileDownload(path);
        InputStream inputStream = Files.newInputStream(tempFilePath);
        FileUtils.del(tempFilePath);
        return inputStream;
    }

    @Override
    public String getEndpoint() {
        String header = getIsHttps();
        return header + properties.getEndpoint();
    }

    @Override
    public String getDomain() {
        String domain = properties.getDomain();
        String endpoint = properties.getEndpoint();
        String header = getIsHttps();

        if (StringUtils.containsAny(endpoint, OssConstant.CLOUD_SERVICE)) {
            return StringUtils.isNotEmpty(domain) ? header + domain : header + endpoint;
        }

        if (StringUtils.isNotEmpty(domain)) {
            return (domain.startsWith(Constants.HTTPS) || domain.startsWith(Constants.HTTP)) ?
                domain : header + domain;
        }

        return header + endpoint;
    }

    public Region of() {
        String region = properties.getRegion();
        return StringUtils.isNotEmpty(region) ? Region.of(region) : Region.US_EAST_1;
    }

    @Override
    public String getUrl() {
        String domain = properties.getDomain();
        String endpoint = properties.getEndpoint();
        String header = getIsHttps();
        if (StringUtils.containsAny(endpoint, OssConstant.CLOUD_SERVICE)) {
            return header + (StringUtils.isNotEmpty(domain) ? domain : properties.getBucketName() + "." + endpoint);
        }
        if (StringUtils.isNotEmpty(domain)) {
            return (domain.startsWith(Constants.HTTPS) || domain.startsWith(Constants.HTTP)) ?
                domain + StringUtils.SLASH + properties.getBucketName() : header + domain + StringUtils.SLASH + properties.getBucketName();
        }
        return header + endpoint + StringUtils.SLASH + properties.getBucketName();
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
        return path.replace(getUrl() + StringUtils.SLASH, "");
    }

    @Override
    public String getConfigKey() {
        return configKey;
    }

    @Override
    public String getIsHttps() {
        return OssConstant.IS_HTTPS.equals(properties.getIsHttps()) ? Constants.HTTPS : Constants.HTTP;
    }

    @Override
    public boolean checkPropertiesSame(OssProperties properties) {
        return this.properties.equals(properties);
    }

    @Override
    public AccessPolicyType getAccessPolicy() {
        return AccessPolicyType.getByType(properties.getAccessPolicy());
    }

}
