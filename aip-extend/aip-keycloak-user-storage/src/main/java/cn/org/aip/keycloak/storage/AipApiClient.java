package cn.org.aip.keycloak.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Keycloak SPI 访问平台内部接口客户端
 * 使用 Java 标准 HttpClient，无需第三方 HTTP 依赖
 *
 * @author Michelle.Chung
 */
public class AipApiClient {

    private static final Logger logger = Logger.getLogger(AipApiClient.class);

    private final AipUserStorageConfig config;
    private final HttpClient httpClient;

    public AipApiClient(AipUserStorageConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(config.getConnectTimeout()))
            .build();
    }

    /**
     * 根据用户名/工号获取用户信息
     */
    public UserDto getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String url = config.getApiUrl() + "/internal/keycloak/users/" + encodedUsername;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .header("X-Internal-Secret", config.getSharedSecret())
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                logger.warnf("Failed to get user by username %s, status: %d", username, response.statusCode());
                return null;
            }

            JsonNode root = JsonSerialization.readValue(response.body(), JsonNode.class);
            if (root == null || root.get("code") == null || root.get("code").asInt() != 200) {
                return null;
            }

            JsonNode dataNode = root.get("data");
            if (dataNode == null || dataNode.isNull()) {
                return null;
            }

            return JsonSerialization.readValue(dataNode.toString(), UserDto.class);
        } catch (Exception e) {
            logger.errorf("Error calling getUserByUsername %s: %s", username, e.getMessage());
            return null;
        }
    }

    /**
     * 根据平台用户ID获取用户信息
     */
    public UserDto getUserById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        try {
            String url = config.getApiUrl() + "/internal/keycloak/users/by-id/" + id;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .header("X-Internal-Secret", config.getSharedSecret())
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                logger.warnf("Failed to get user by id %s, status: %d", id, response.statusCode());
                return null;
            }

            JsonNode root = JsonSerialization.readValue(response.body(), JsonNode.class);
            if (root == null || root.get("code") == null || root.get("code").asInt() != 200) {
                return null;
            }

            JsonNode dataNode = root.get("data");
            if (dataNode == null || dataNode.isNull()) {
                return null;
            }

            return JsonSerialization.readValue(dataNode.toString(), UserDto.class);
        } catch (Exception e) {
            logger.errorf("Error calling getUserById %s: %s", id, e.getMessage());
            return null;
        }
    }

    /**
     * 搜索用户
     */
    public List<UserDto> searchUsers(String search, int first, int max) {
        try {
            StringBuilder sb = new StringBuilder(config.getApiUrl() + "/internal/keycloak/users?");
            if (search != null && !search.trim().isEmpty()) {
                sb.append("search=").append(URLEncoder.encode(search, StandardCharsets.UTF_8)).append("&");
            }
            sb.append("first=").append(first).append("&max=").append(max);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sb.toString()))
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .header("X-Internal-Secret", config.getSharedSecret())
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return Collections.emptyList();
            }

            JsonNode root = JsonSerialization.readValue(response.body(), JsonNode.class);
            if (root == null || root.get("code") == null || root.get("code").asInt() != 200) {
                return Collections.emptyList();
            }

            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.has("rows")) {
                return Collections.emptyList();
            }

            JsonNode rowsNode = dataNode.get("rows");
            if (rowsNode == null || !rowsNode.isArray()) {
                return Collections.emptyList();
            }

            List<UserDto> list = new ArrayList<>();
            for (JsonNode row : rowsNode) {
                list.add(JsonSerialization.readValue(row.toString(), UserDto.class));
            }
            return list;
        } catch (Exception e) {
            logger.errorf("Error calling searchUsers: %s", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 校验用户密码
     */
    public VerifyPasswordResult verifyPassword(String username, String password) {
        try {
            String url = config.getApiUrl() + "/internal/keycloak/verify-password";
            Map<String, String> bodyMap = new HashMap<>(2);
            bodyMap.put("username", username);
            bodyMap.put("password", password);
            String jsonBody = JsonSerialization.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .header("X-Internal-Secret", config.getSharedSecret())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                logger.warnf("verifyPassword HTTP error status %d", response.statusCode());
                return VerifyPasswordResult.fail("内部认证服务不可用: HTTP " + response.statusCode());
            }

            JsonNode root = JsonSerialization.readValue(response.body(), JsonNode.class);
            if (root == null || root.get("code") == null || root.get("code").asInt() != 200) {
                String msg = (root != null && root.has("msg")) ? root.get("msg").asText() : "认证失败";
                return VerifyPasswordResult.fail(msg);
            }

            JsonNode dataNode = root.get("data");
            if (dataNode == null) {
                return VerifyPasswordResult.fail("认证响应为空");
            }

            return JsonSerialization.readValue(dataNode.toString(), VerifyPasswordResult.class);
        } catch (Exception e) {
            logger.errorf("Error calling verifyPassword for %s: %s", username, e.getMessage());
            return VerifyPasswordResult.fail("校验密码异常: " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserDto {
        private String id;
        private String username;
        private String userName;
        private String employeeNo;
        private String email;
        private String nickName;
        private String phonenumber;
        private boolean enabled;
        private Long deptId;
        private String deptName;
        private Boolean passwordResetRequired;
        private Map<String, List<String>> attributes = new HashMap<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getEmployeeNo() {
            return employeeNo;
        }

        public void setEmployeeNo(String employeeNo) {
            this.employeeNo = employeeNo;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public String getPhonenumber() {
            return phonenumber;
        }

        public void setPhonenumber(String phonenumber) {
            this.phonenumber = phonenumber;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }

        public String getDeptName() {
            return deptName;
        }

        public void setDeptName(String deptName) {
            this.deptName = deptName;
        }

        public Boolean getPasswordResetRequired() {
            return passwordResetRequired;
        }

        public void setPasswordResetRequired(Boolean passwordResetRequired) {
            this.passwordResetRequired = passwordResetRequired;
        }

        public Map<String, List<String>> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, List<String>> attributes) {
            this.attributes = attributes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifyPasswordResult {
        private boolean valid;
        private Long userId;
        private String username;
        private String employeeNo;
        private String status;
        private Boolean passwordResetRequired;
        private String message;

        public static VerifyPasswordResult fail(String message) {
            VerifyPasswordResult result = new VerifyPasswordResult();
            result.setValid(false);
            result.setMessage(message);
            return result;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmployeeNo() {
            return employeeNo;
        }

        public void setEmployeeNo(String employeeNo) {
            this.employeeNo = employeeNo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Boolean getPasswordResetRequired() {
            return passwordResetRequired;
        }

        public void setPasswordResetRequired(Boolean passwordResetRequired) {
            this.passwordResetRequired = passwordResetRequired;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

}
