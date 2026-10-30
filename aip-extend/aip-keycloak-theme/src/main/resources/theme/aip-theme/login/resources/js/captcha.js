/**
 * Keycloak AIP 主题 - AJ-Captcha 汉字点选验证码与交互逻辑
 */

let captchaData = null;
let clickedPoints = [];

/**
 * 智能解析平台后端服务基地址 (aip-admin)
 */
function getBackendBaseUrl() {
  // 1. 优先从当前页面地址栏中的 redirect_uri 提取 (即平台后端回调地址)
  const urlParams = new URLSearchParams(window.location.search);
  const redirectUri = urlParams.get('redirect_uri');
  if (redirectUri) {
    try {
      const u = new URL(redirectUri);
      return u.origin;
    } catch (e) {
      console.warn('解析 redirect_uri 异常:', e);
    }
  }

  // 2. 局域网访问默认直连后端 IP
  if (window.location.hostname === '192.168.43.100') {
    return 'http://192.168.3.82:8080';
  }

  return window.location.origin;
}

/**
 * AES-ECB 加密 (对齐后端 anji-captcha AESUtil)
 */
function aesEncrypt(word, key) {
  if (typeof CryptoJS === 'undefined') {
    console.error('CryptoJS 未加载');
    return word;
  }
  const keyHex = CryptoJS.enc.Utf8.parse(key);
  const encrypted = CryptoJS.AES.encrypt(CryptoJS.enc.Utf8.parse(word), keyHex, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  });
  return encrypted.toString();
}

/**
 * 打开验证码弹窗
 */
function openCaptcha() {
  const mask = document.getElementById('captcha-mask');
  if (mask) {
    mask.style.display = 'flex';
    refreshCaptcha();
  }
}

/**
 * 关闭验证码弹窗
 */
function closeCaptcha() {
  const mask = document.getElementById('captcha-mask');
  if (mask) {
    mask.style.display = 'none';
  }
  clickedPoints = [];
  const dotsContainer = document.getElementById('captcha-dots-container');
  if (dotsContainer) {
    dotsContainer.innerHTML = '';
  }
}

/**
 * 刷新并获取汉字点选验证码
 */
async function refreshCaptcha() {
  clickedPoints = [];
  const dotsContainer = document.getElementById('captcha-dots-container');
  if (dotsContainer) {
    dotsContainer.innerHTML = '';
  }

  const tipText = document.getElementById('captcha-tip-text');
  const i18n = window.AIP_I18N || {};
  if (tipText) {
    tipText.className = '';
    tipText.innerHTML = i18n.captchaLoading || '正在加载安全验证...';
  }

  const loadingEl = document.getElementById('captcha-loading');
  if (loadingEl) {
    loadingEl.style.display = 'flex';
  }

  const backendUrl = getBackendBaseUrl();

  try {
    const response = await fetch(`${backendUrl}/captcha/get?captchaType=clickWord`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    });

    const res = await response.json();
    if (res && res.code === 200 && res.data) {
      captchaData = res.data;
      const bgImg = document.getElementById('captcha-bg-img');
      if (bgImg) {
        bgImg.src = 'data:image/png;base64,' + res.data.originalImageBase64;
      }

      if (tipText) {
        const words = (res.data.wordList || []).join(' ');
        tipText.innerHTML = `请依次点击: <span class="highlight-words">${words}</span>`;
      }
    } else {
      if (tipText) {
        tipText.innerHTML = `<span class="captcha-error-tip">获取验证码失败: ${res?.msg || '服务未就绪'}</span>`;
      }
    }
  } catch (err) {
    console.error('获取验证码网络失败:', err);
    if (tipText) {
      tipText.innerHTML = `<span class="captcha-error-tip">${i18n.captchaNetError || '网络连接失败，请检查平台后端'}</span>`;
    }
  } finally {
    if (loadingEl) {
      loadingEl.style.display = 'none';
    }
  }
}

/**
 * 提交点选轨迹校验
 */
async function submitCaptchaPoints() {
  const targetCount = captchaData?.wordList ? captchaData.wordList.length : 4;
  const tipText = document.getElementById('captcha-tip-text');
  const i18n = window.AIP_I18N || {};

  if (clickedPoints.length !== targetCount) {
    if (tipText) {
      tipText.innerHTML = `<span class="captcha-error-tip">${i18n.captchaIncomplete || '请依次点选全部汉字后再提交'}</span>`;
    }
    return;
  }

  if (tipText) {
    tipText.innerHTML = i18n.captchaChecking || '正在校验点选位置...';
  }

  const confirmBtn = document.getElementById('captcha-btn-confirm');
  if (confirmBtn) {
    confirmBtn.disabled = true;
  }

  try {
    const encryptedPoints = aesEncrypt(JSON.stringify(clickedPoints), captchaData.secretKey);
    const backendUrl = getBackendBaseUrl();

    const response = await fetch(`${backendUrl}/captcha/check`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({
        captchaType: 'clickWord',
        pointJson: encryptedPoints,
        token: captchaData.token
      })
    });

    const res = await response.json();
    if (res && res.code === 200) {
      if (tipText) {
        tipText.innerHTML = `<span style="color: #67c23a; font-weight: 600;">${i18n.captchaSuccess || '验证通过，正在登录...'}</span>`;
      }

      // 验证通过，回填验证凭证并真正提交登录表单
      const verificationInput = document.getElementById('captchaVerification');
      if (verificationInput) {
        verificationInput.value = res.data?.captchaVerification || 'verified';
      }

      setTimeout(() => {
        closeCaptcha();
        const form = document.getElementById('kc-form-login');
        if (form) {
          form.submit();
        }
      }, 400);
    } else {
      if (tipText) {
        tipText.innerHTML = `<span class="captcha-error-tip">${res?.msg || i18n.captchaFail || '验证未通过，请重新点选'}</span>`;
      }
      setTimeout(() => {
        refreshCaptcha();
      }, 800);
    }
  } catch (err) {
    console.error('点选校验失败:', err);
    if (tipText) {
      tipText.innerHTML = `<span class="captcha-error-tip">${i18n.captchaNetError || '校验异常，请刷新重试'}</span>`;
    }
    setTimeout(() => {
      refreshCaptcha();
    }, 800);
  } finally {
    if (confirmBtn) {
      confirmBtn.disabled = false;
    }
  }
}

/**
 * 监听图片点击并记录坐标点
 */
document.addEventListener('DOMContentLoaded', function () {
  const bgImg = document.getElementById('captcha-bg-img');
  const dotsContainer = document.getElementById('captcha-dots-container');

  if (bgImg) {
    bgImg.addEventListener('click', function (e) {
      if (!captchaData) return;
      const targetCount = captchaData.wordList ? captchaData.wordList.length : 4;
      if (clickedPoints.length >= targetCount) return;

      const rect = bgImg.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const clickY = e.clientY - rect.top;

      // 按 AJ-Captcha 310 x 155 原始设计分辨率换算逻辑坐标
      const scaleX = 310 / rect.width;
      const scaleY = 155 / rect.height;
      const originX = Math.round(clickX * scaleX);
      const originY = Math.round(clickY * scaleY);

      clickedPoints.push({ x: originX, y: originY });

      // 在页面上放置带编号的圆点
      const dot = document.createElement('div');
      dot.className = 'captcha-dot';
      dot.style.left = clickX + 'px';
      dot.style.top = clickY + 'px';
      dot.innerText = clickedPoints.length;
      dotsContainer.appendChild(dot);
    });
  }

  // 拦截登录表单提交
  const form = document.getElementById('kc-form-login');
  if (form) {
    form.addEventListener('submit', function (e) {
      const usernameInput = document.getElementById('username');
      const passwordInput = document.getElementById('password');
      const verificationInput = document.getElementById('captchaVerification');

      if (!usernameInput || !usernameInput.value || !usernameInput.value.trim()) {
        if (usernameInput) usernameInput.focus();
        e.preventDefault();
        return;
      }

      if (!passwordInput || !passwordInput.value || !passwordInput.value.trim()) {
        if (passwordInput) passwordInput.focus();
        e.preventDefault();
        return;
      }

      // 如果未完成验证码，拦截提交并弹出验证码
      if (!verificationInput || !verificationInput.value) {
        e.preventDefault();
        openCaptcha();
      }
    });
  }
});

/**
 * 其他登录方式点击提示
 */
function handleOtherLogin(type) {
  const i18n = window.AIP_I18N || {};
  if (type === 'wecom') {
    alert(i18n.otherWecomTip || '企业微信扫码登录已纳入集成规划，当前环境请使用平台账号密码统一认证。');
  } else {
    alert(i18n.otherLoginTip || '暂未开放其他登录方式，请使用平台账号密码统一认证。');
  }
}
