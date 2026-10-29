package cn.org.aip.keycloak.storage;

import org.keycloak.component.ComponentModel;

/**
 * Keycloak AIP User Storage 配置模型
 *
 * @author Michelle.Chung
 */
public class AipUserStorageConfig {

    public static final String CONFIG_API_URL = "apiUrl";
    public static final String CONFIG_SHARED_SECRET = "sharedSecret";
    public static final String CONFIG_CONNECT_TIMEOUT = "connectTimeout";
    public static final String CONFIG_READ_TIMEOUT = "readTimeout";

    public static final String DEFAULT_API_URL = "http://localhost:8080";
    public static final String DEFAULT_SHARED_SECRET = "aip-sso-internal-secret-token";
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    public static final int DEFAULT_READ_TIMEOUT = 5000;

    private final String apiUrl;
    private final String sharedSecret;
    private final int connectTimeout;
    private final int readTimeout;

    public AipUserStorageConfig(ComponentModel model) {
        String url = model.get(CONFIG_API_URL, DEFAULT_API_URL);
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.apiUrl = url;
        this.sharedSecret = model.get(CONFIG_SHARED_SECRET, DEFAULT_SHARED_SECRET);
        this.connectTimeout = model.get(CONFIG_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
        this.readTimeout = model.get(CONFIG_READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

}
