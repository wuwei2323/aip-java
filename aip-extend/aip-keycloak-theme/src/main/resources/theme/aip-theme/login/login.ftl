<!DOCTYPE html>
<html lang="${((locale.currentLanguageTag)!'zh-CN')}">
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${msg('loginTitle')}</title>
    <link rel="icon" href="${url.resourcesPath}/icons/login-lock.svg">
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
    <link rel="stylesheet" href="${url.resourcesPath}/css/captcha.css">
</head>
<body>
    <div class="login-page">
        <#-- 国际化多语言切换器 (Element Plus 下拉菜单风格) -->
        <#if (realm.internationalizationEnabled?? && realm.internationalizationEnabled && (locale.supported)?? && (locale.supported?size > 1))>
            <div class="login-lang-container" id="lang-dropdown">
                <div class="el-dropdown-trigger" onclick="toggleLangDropdown(event)">
                    <svg class="el-dropdown-icon" viewBox="0 0 1024 1024" width="16" height="16">
                        <path fill="currentColor" d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896m0 82.688A365.312 365.312 0 0 0 146.688 512c0 23.68 2.304 46.848 6.656 69.312h110.272c-2.432-22.656-3.776-45.696-3.776-69.312 0-23.616 1.344-46.656 3.776-69.312H153.344A365.312 365.312 0 0 0 512 146.688m-64.832 2.688c-42.368 47.68-75.008 117.824-91.712 204.624h156.544V149.376zm129.664 0v204.624h156.544c-16.704-86.8-49.344-156.944-91.712-204.624m-129.664 274.624v176h176v-176zm-70.016 0H270.08c-2.432 28.672-3.84 58.112-3.84 88s1.408 59.328 3.84 88h107.072v-176zm316.032 0v176h107.072c2.432-28.672 3.84-58.112 3.84-88s-1.408-59.328-3.84-88H693.184zm-246.016 246v204.624c42.368-47.68 75.008-117.824 91.712-204.624zm129.664 0c16.704 86.8 49.344 156.944 91.712 204.624V669.952zm-221.376 0c16.704 86.8 49.344 156.944 91.712 204.624A365.312 365.312 0 0 0 153.344 597.312h110.272c2.432 23.616 5.824 46.656 10.112 69.312m313.088 204.624c42.368-47.68 75.008-117.824 91.712-204.624h110.272a365.312 365.312 0 0 0-201.984 204.624"/>
                    </svg>
                    <span class="el-dropdown-label">
                        <#assign currentLangName = "简体中文">
                        <#list locale.supported as l>
                            <#if l.languageTag == ((locale.currentLanguageTag)!'zh-CN')>
                                <#if l.languageTag == "zh-CN"><#assign currentLangName = "简体中文">
                                <#elseif l.languageTag == "en"><#assign currentLangName = "English">
                                <#else><#assign currentLangName = l.label>
                                </#if>
                            </#if>
                        </#list>
                        ${currentLangName}
                    </span>
                    <svg class="el-dropdown-arrow" viewBox="0 0 1024 1024" width="12" height="12">
                        <path fill="currentColor" d="M831.872 340.777a32 32 0 0 1 45.248 45.248L534.656 728.49a32 32 0 0 1-45.312 0L146.88 386.025a32 32 0 0 1 45.248-45.248L512 660.872l319.872-320.095z"/>
                    </svg>
                </div>
                <div class="el-dropdown-menu" id="lang-dropdown-menu">
                    <#list locale.supported as l>
                        <#assign itemText = l.label>
                        <#if l.languageTag == "zh-CN"><#assign itemText = "简体中文">
                        <#elseif l.languageTag == "en"><#assign itemText = "English">
                        </#if>
                        <a href="${l.url}" class="el-dropdown-item <#if l.languageTag == ((locale.currentLanguageTag)!'zh-CN')>is-active</#if>">
                            <span>${itemText}</span>
                            <#if l.languageTag == ((locale.currentLanguageTag)!'zh-CN')>
                                <svg class="el-dropdown-check" viewBox="0 0 1024 1024" width="14" height="14">
                                    <path fill="currentColor" d="M384 704a32 32 0 0 1-22.624-9.376L169.376 502.624a32 32 0 0 1 45.248-45.248L384 626.752l425.376-425.376a32 32 0 0 1 45.248 45.248L406.624 694.624A32 32 0 0 1 384 704z"/>
                                </svg>
                            </#if>
                        </a>
                    </#list>
                </div>
            </div>
        </#if>

        <img class="login-page__bg" src="${url.resourcesPath}/img/login-bg.png" alt="" aria-hidden="true">

        <main class="login-main">
            <img class="login-main__illustration" src="${url.resourcesPath}/img/login-illustration.png" alt="" aria-hidden="true">

            <section class="login-card">
                <h1 class="login-card__title">
                    ${msg('loginTitle')}
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

        function toggleLangDropdown(e) {
            e.stopPropagation();
            var container = document.getElementById('lang-dropdown');
            if (container) {
                container.classList.toggle('is-open');
            }
        }

        document.addEventListener('click', function(e) {
            var container = document.getElementById('lang-dropdown');
            if (container && !container.contains(e.target)) {
                container.classList.remove('is-open');
            }
        });
    </script>
    <script src="${url.resourcesPath}/js/crypto-js.min.js"></script>
    <script src="${url.resourcesPath}/js/captcha.js"></script>
</body>
</html>
