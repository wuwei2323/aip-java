package cn.org.aip.keycloak.captcha;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;

import static cn.org.aip.keycloak.captcha.CaptchaTestSupport.CONTEXT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证验证码失败先于密码校验，以及审计事件的发送顺序。 */
@Tag("dev")
class AipCaptchaUsernamePasswordFormTest {
    private CaptchaTestSupport.Fixture fixture;
    private AuthenticationFlowContext context;
    private EventBuilder event;
    private LoginFormsProvider form;
    private Response challenge;
    private MultivaluedMap<String, String> formData;
    private PasswordProbe authenticator;

    @BeforeEach
    void setUp() {
        fixture = new CaptchaTestSupport.Fixture();
        context = mock(AuthenticationFlowContext.class);
        event = mock(EventBuilder.class);
        form = mock(LoginFormsProvider.class, RETURNS_SELF);
        challenge = mock(Response.class);
        formData = new MultivaluedHashMap<>();
        formData.add("username", "test-user");
        formData.add("password", "test-password");
        HttpRequest request = mock(HttpRequest.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(request.getDecodedFormParameters()).thenReturn(formData);
        when(context.getAuthenticationSession()).thenReturn(fixture.auth);
        when(context.getSession()).thenReturn(fixture.session);
        when(context.getEvent()).thenReturn(event);
        when(context.form()).thenReturn(form);
        when(form.createLoginUsernamePassword()).thenReturn(challenge);
        authenticator = new PasswordProbe();
    }

    @Test
    void missingTicketRejectsBeforePasswordAndAddsAuditDetailBeforeError() {
        authenticator.action(context);
        assertEquals(0, authenticator.passwordChecks);
        verify(context).failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
        verify(context, never()).success();
        verify(form).setFormData(formData);
        verify(form).setError("captchaVerifyFail");
        var order = inOrder(event);
        order.verify(event).detail("captcha", "invalid");
        order.verify(event).error(Errors.INVALID_USER_CREDENTIALS);
    }

    @Test
    void forgedOrCrossRootTicketRejectsBeforePassword() {
        formData.putSingle(AipCaptchaConstants.FORM_FIELD, "not-a-ticket");
        authenticator.action(context);
        String ticket = AipCaptchaTicketService.issue(fixture.session, CONTEXT);
        formData.putSingle(AipCaptchaConstants.FORM_FIELD, ticket);
        when(fixture.root.getId()).thenReturn("other-root");
        authenticator.action(context);
        assertEquals(0, authenticator.passwordChecks);
        verify(context, never()).success();
    }

    @Test
    void validTicketRunsStandardActionAndCannotBeReused() {
        String ticket = AipCaptchaTicketService.issue(fixture.session, CONTEXT);
        formData.putSingle(AipCaptchaConstants.FORM_FIELD, ticket);
        authenticator.action(context);
        assertEquals(1, authenticator.passwordChecks);
        verify(context).success();
        authenticator.action(context);
        assertEquals(1, authenticator.passwordChecks);
        verify(context).failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
    }

    @Test
    void passwordFailureStillConsumesTicket() {
        formData.putSingle(AipCaptchaConstants.FORM_FIELD, AipCaptchaTicketService.issue(fixture.session, CONTEXT));
        authenticator.validPassword = false;
        authenticator.action(context);
        authenticator.action(context);
        assertEquals(1, authenticator.passwordChecks);
        verify(context, never()).success();
        verify(context).failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
    }

    @Test
    void cancelDoesNotRequireCaptcha() {
        formData.putSingle("cancel", "true");
        authenticator.action(context);
        verify(context).cancelLogin();
        verifyNoInteractions(event, form);
        assertEquals(0, authenticator.passwordChecks);
    }

    /** 仅替代标准 action 中的密码校验，以观察是否进入该步骤。 */
    private static final class PasswordProbe extends AipCaptchaUsernamePasswordForm {
        private int passwordChecks;
        private boolean validPassword = true;

        @Override
        protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
            passwordChecks++;
            return validPassword;
        }
    }
}
