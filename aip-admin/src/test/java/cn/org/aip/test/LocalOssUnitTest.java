package cn.org.aip.test;

import cn.hutool.core.io.FileUtil;
import cn.org.aip.common.core.config.properties.FileProperties;
import cn.org.aip.common.core.constant.FileConstants;
import cn.org.aip.common.oss.core.LocalOssClient;
import cn.org.aip.common.oss.entity.UploadResult;
import cn.org.aip.common.oss.properties.OssProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地存储 LocalOssClient 单元测试
 *
 * @author AIP
 */
@Tag("local")
@DisplayName("本地存储单元测试")
public class LocalOssUnitTest {

    private LocalOssClient localOssClient;
    private File tempTestDir;

    @BeforeEach
    public void setUp() {
        tempTestDir = FileUtil.createTempFile("oss_test_", "", null, true);
        FileUtil.del(tempTestDir);
        FileUtil.mkdir(tempTestDir);

        System.setProperty(FileConstants.FILE_PATH, tempTestDir.getAbsolutePath());

        OssProperties ossProperties = new OssProperties();
        ossProperties.setEndpoint("local");
        ossProperties.setPrefix("upload");

        FileProperties fileProperties = new FileProperties();
        fileProperties.setPath(tempTestDir.getAbsolutePath());
        fileProperties.setPrefix(FileConstants.DEFAULT_RESOURCE_PREFIX);

        localOssClient = new LocalOssClient("local", ossProperties, fileProperties);
    }

    @AfterEach
    public void tearDown() {
        if (tempTestDir != null && tempTestDir.exists()) {
            FileUtil.del(tempTestDir);
        }
        System.clearProperty(FileConstants.FILE_PATH);
    }

    @Test
    @DisplayName("测试本地文件上传、下载与删除")
    public void testLocalUploadDownloadDelete() {
        String content = "Hello Local OSS Storage Test!";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        // 1. 测试上传字节流
        UploadResult uploadResult = localOssClient.uploadSuffix(bytes, ".txt", "text/plain");
        Assertions.assertNotNull(uploadResult);
        Assertions.assertNotNull(uploadResult.getUrl());
        Assertions.assertTrue(uploadResult.getUrl().contains("/profile/upload/"));
        Assertions.assertTrue(uploadResult.getFilename().endsWith(".txt"));

        // 2. 验证本地物理文件已真实生成
        String key = uploadResult.getFilename();
        File savedFile = new File(tempTestDir, key);
        Assertions.assertTrue(savedFile.exists(), "物理磁盘中未生成文件: " + savedFile.getAbsolutePath());
        Assertions.assertEquals(content, FileUtil.readUtf8String(savedFile));

        // 3. 测试通过 download 方法读取内容
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AtomicLong fileLength = new AtomicLong();
        localOssClient.download(key, out, fileLength::set);
        Assertions.assertEquals(bytes.length, fileLength.get());
        Assertions.assertEquals(content, out.toString(StandardCharsets.UTF_8));

        // 4. 测试 fileDownload 临时文件
        Path downloadedPath = localOssClient.fileDownload(uploadResult.getUrl());
        Assertions.assertTrue(downloadedPath.toFile().exists());

        // 5. 测试 delete 删除
        localOssClient.delete(uploadResult.getUrl());
        Assertions.assertFalse(savedFile.exists(), "删除后本地物理文件仍存在: " + savedFile.getAbsolutePath());
    }

    @Test
    @DisplayName("测试相对路径支持能力")
    public void testRelativePathSupport() {
        // 配置相对路径，如 ./target/temp_rel_upload
        String relPath = "./target/temp_rel_upload";
        FileProperties relProperties = new FileProperties();
        relProperties.setPath(relPath);

        // 验证 getPath() 自动转换为规范绝对路径
        String canonicalPath = relProperties.getPath();
        Assertions.assertNotNull(canonicalPath);
        Assertions.assertTrue(new File(canonicalPath).isAbsolute(), "相对路径未被自动转换为绝对路径: " + canonicalPath);

        // 测试相对路径下的上传能力
        OssProperties ossProperties = new OssProperties();
        ossProperties.setEndpoint("local");
        ossProperties.setPrefix("upload");

        LocalOssClient relClient = new LocalOssClient("local", ossProperties, relProperties);
        String testContent = "Relative Path Storage Content";
        UploadResult uploadResult = relClient.uploadSuffix(testContent.getBytes(StandardCharsets.UTF_8), ".txt", "text/plain");
        Assertions.assertNotNull(uploadResult);

        File savedRelFile = new File(canonicalPath, uploadResult.getFilename());
        Assertions.assertTrue(savedRelFile.exists(), "相对路径物理文件生成失败: " + savedRelFile.getAbsolutePath());
        Assertions.assertEquals(testContent, FileUtil.readUtf8String(savedRelFile));

        // 清理相对路径临时生成文件
        relClient.delete(uploadResult.getUrl());
        Assertions.assertFalse(savedRelFile.exists(), "删除文件失败");
        FileUtil.del(canonicalPath);
    }
}
