package cn.org.aip.common.sso.core;

import cn.org.aip.common.core.utils.MessageUtils;
import lombok.Getter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Safe classification of Keycloak failures, without response bodies, tokens or unsafe causes.
 */
@Getter
public class KeycloakRequestException extends RuntimeException {

    private final Reason reason;

    /**
     * Creates a localized error from a fixed reason.
     *
     * @param reason safe failure classification
     */
    public KeycloakRequestException(Reason reason) {
        super(MessageUtils.message(reason.messageKey));
        this.reason = reason;
    }

    /**
     * Discards unsafe transport exception details while retaining their failure category.
     *
     * @param exception transport or parsing failure
     * @return safe exception, with no original cause attached
     */
    public static KeycloakRequestException from(RuntimeException exception) {
        if (exception instanceof KeycloakRequestException known) {
            return known;
        }
        if (exception instanceof ResourceAccessException) {
            return new KeycloakRequestException(Reason.UNAVAILABLE);
        }
        if (exception instanceof RestClientResponseException response) {
            return new KeycloakRequestException(switch (response.getStatusCode().value()) {
                case 401, 403 -> Reason.UNAUTHORIZED;
                case 404 -> Reason.NOT_FOUND;
                case 409 -> Reason.CONFLICT;
                default -> Reason.HTTP_ERROR;
            });
        }
        return new KeycloakRequestException(Reason.REQUEST_FAILED);
    }

    /** Fixed categories that are safe to persist and display to administrators. */
    public enum Reason {
        DISABLED("sso.keycloak.disabled"),
        ADMIN_UNCONFIGURED("sso.keycloak.admin.unconfigured"),
        UNAVAILABLE("sso.keycloak.unavailable"),
        UNAUTHORIZED("sso.keycloak.unauthorized"),
        NOT_FOUND("sso.keycloak.not.found"),
        CONFLICT("sso.keycloak.conflict"),
        HTTP_ERROR("sso.keycloak.http.error"),
        INVALID_RESPONSE("sso.keycloak.response.invalid"),
        STORED_SECRET_UNAVAILABLE("application.sync.secret.unavailable"),
        REQUEST_FAILED("sso.keycloak.request.failed");

        private final String messageKey;

        Reason(String messageKey) {
            this.messageKey = messageKey;
        }
    }
}
