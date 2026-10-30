<#-- 解析页面标题：优先使用 Client Name，其次 Realm Display Name，最后默认 key -->
<#assign rawTitle = (client.name!(realm.displayNameHtml!realm.displayName!'${loginTitle}'))>
<#assign displayTitle = advancedMsg(rawTitle)>

<!DOCTYPE html>
<html lang="${((locale.currentLanguageTag)!'zh-CN')}">
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${displayTitle}</title>
    <link rel="icon" href="${url.resourcesPath}/icons/login-lock.svg">
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
    <link rel="stylesheet" href="${url.resourcesPath}/css/captcha.css">
</head>
<body>
    <div class="login-page">
        <#-- 国际化多语言切换器 -->
        <#if (realm.internationalizationEnabled?? && realm.internationalizationEnabled && (locale.supported)?? && (locale.supported?size > 1))>
            <div class="login-lang-container">
                <select class="login-lang-select" onchange="window.location.href=this.value">
                    <#list locale.supported as l>
                        <option value="${l.url}" <#if l.languageTag == ((locale.currentLanguageTag)!'')>selected</#if>>${l.label}</option>
                    </#list>
                </select>
            </div>
        </#if>

        <img class="login-page__bg" src="${url.resourcesPath}/img/login-bg.png" alt="" aria-hidden="true">

        <main class="login-main">
            <img class="login-main__illustration" src="${url.resourcesPath}/img/login-illustration.png" alt="" aria-hidden="true">

            <section class="login-card">
                <h1 class="login-card__title">
                    ${displayTitle}
                </h1>

                <#if message?has_content && message.type??>
                    <div class="login-alert login-alert--${message.type}">
                        <span>${message.summary}</span>
                    </div>
                </#if>

                <form id="kc-form-login" class="login-form" action="${url.loginAction}" method="post">
                    <label class="login-field">
                        <span class="login-field__icon">
                            <img src="${url.resourcesPath}/icons/login-user.svg" alt="账号">
                        </span>
                        <input id="username" name="username" value="${(login.username!'')}" type="text" placeholder="${msg('loginAccountPlaceholder')}" autocomplete="username" required autofocus>
                    </label>

                    <label class="login-field">
                        <span class="login-field__icon">
                            <img src="${url.resourcesPath}/icons/login-lock.svg" alt="密码">
                        </span>
                        <input id="password" name="password" type="password" placeholder="${msg('loginPasswordPlaceholder')}" autocomplete="current-password" required>
                    </label>

                    <input type="hidden" id="captchaVerification" name="captchaVerification" value="">

                    <button id="kc-login" class="login-submit" type="submit">
                        ${msg('loginSubmit')}
                    </button>
                </form>

                <div class="login-divider">
                    ${msg('loginOtherWays')}
                </div>

                <div class="login-others">
                    <button class="login-other login-other--wecom" type="button" onclick="handleOtherLogin('wecom')">
                        <span class="login-other__icon">
                            <img src="${url.resourcesPath}/icons/login-wecom.svg" alt="企微">
                        </span>
                        <span>${msg('loginWecom')}</span>
                    </button>
                    <button class="login-other login-other--plain" type="button" onclick="handleOtherLogin('other')">
                        <span class="login-other__icon">
                            <img src="${url.resourcesPath}/icons/login-other.svg" alt="其他">
                        </span>
                        <span>${msg('loginOther')}</span>
                    </button>
                </div>
            </section>
        </main>

        <footer class="login-footer">
            ${msg('loginCopyright')}
        </footer>
    </div>

    <!-- AJ-Captcha 行为验证码弹窗容器 -->
    <div id="captcha-mask" class="captcha-mask" style="display: none;">
        <div class="captcha-popup">
            <div class="captcha-header">
                <span class="captcha-title">${msg('captchaTitle')}</span>
                <span class="captcha-close" onclick="closeCaptcha()">&times;</span>
            </div>
            <div class="captcha-tip-bar">
                <span id="captcha-tip-text">${msg('captchaTipDefault')}</span>
            </div>
            <div class="captcha-img-wrapper">
                <img id="captcha-bg-img" class="captcha-bg-img" alt="验证码底图">
                <div id="captcha-dots-container" class="captcha-dots-container"></div>
                <div id="captcha-loading" class="captcha-loading">
                    <div class="captcha-spinner"></div>
                    <span>${msg('captchaLoading')}</span>
                </div>
            </div>
            <div class="captcha-footer">
                <div class="captcha-actions-left">
                    <button type="button" class="captcha-btn-icon" title="刷新验证码" onclick="refreshCaptcha()">&#8635;</button>
                    <button type="button" class="captcha-btn-icon" title="帮助">&#9432;</button>
                </div>
                <button type="button" id="captcha-btn-confirm" class="captcha-btn-confirm" onclick="submitCaptchaPoints()">${msg('captchaConfirm')}</button>
            </div>
        </div>
    </div>

    <script>
        window.AIP_I18N = {
            captchaTitle: "${msg('captchaTitle')}",
            captchaTipDefault: "${msg('captchaTipDefault')}",
            captchaConfirm: "${msg('captchaConfirm')}",
            captchaLoading: "${msg('captchaLoading')}",
            captchaChecking: "${msg('captchaChecking')}",
            captchaSuccess: "${msg('captchaSuccess')}",
            captchaFail: "${msg('captchaFail')}",
            captchaIncomplete: "${msg('captchaIncomplete')}",
            captchaNetError: "${msg('captchaNetError')}",
            otherWecomTip: "${msg('otherWecomTip')}",
            otherLoginTip: "${msg('otherLoginTip')}"
        };
    </script>
    <script src="${url.resourcesPath}/js/crypto-js.min.js"></script>
    <script src="${url.resourcesPath}/js/captcha.js"></script>
</body>
</html>
