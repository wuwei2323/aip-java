/**
 * Keycloak AIP 主题 - AJ-Captcha 拼图滑块验证码交互逻辑
 *
 * 验证码接口与登录页同源（/realms/{realm}/aip-captcha），
 * 不再依赖集成平台地址或跨域配置。滑块协议完全遵循 AJ-Captcha：
 * pointJson = AES(JSON.stringify({x: 拖动距离, y: 5}), secretKey)。
 */

let captchaData = null;
let captchaContext = null;
let captchaGeometry = { bgWidth: 310, maxHandleOffset: 1 };
let handleOffset = 0;
let dragStartX = 0;
let dragging = false;
let submitting = false;
let captchaAttempt = 0;
let activeRequest = null;
let retryTimer = null;
let submitTimer = null;
const CAPTCHA_REQUEST_TIMEOUT = 15000;

// 关闭/刷新即使旧请求和定时器失效；AbortController 之外仍需轮次检查以防响应已返回。
function invalidateCaptchaAttempt() {
  captchaAttempt++;
  if (activeRequest) {
    activeRequest.controller.abort();
    clearTimeout(activeRequest.timer);
    activeRequest = null;
  }
  clearTimeout(retryTimer);
  clearTimeout(submitTimer);
  retryTimer = null;
  submitTimer = null;
  captchaData = null;
  submitting = false;
  const input = captchaById('captchaVerification');
  if (input) input.value = '';
  return captchaAttempt;
}

function isCurrentAttempt(attempt) {
  const mask = captchaById('captcha-mask');
  return attempt === captchaAttempt && mask && mask.style.display === 'flex';
}

async function fetchCaptcha(url, options) {
  const request = { controller: new AbortController(), timer: null, timedOut: false };
  activeRequest = request;
  request.timer = setTimeout(() => {
    request.timedOut = true;
    request.controller.abort();
  }, CAPTCHA_REQUEST_TIMEOUT);
  try {
    const response = await fetch(url, { ...options, signal: request.controller.signal });
    return await response.json();
  } catch (error) {
    if (request.timedOut) throw new Error('CAPTCHA request timed out');
    throw error;
  } finally {
    clearTimeout(request.timer);
    if (activeRequest === request) activeRequest = null;
  }
}

function retryCaptcha(attempt, message) {
  if (!isCurrentAttempt(attempt)) return;
  const track = captchaById('captcha-slide-track');
  const tip = captchaById('captcha-tip-text');
  if (track) track.classList.add('is-fail');
  if (tip) {
    tip.className = 'captcha-error-tip';
    tip.textContent = message;
  }
  resetHandle();
  // 保持 submitting，直到自动刷新或用户主动关闭/刷新当前挑战。
  retryTimer = setTimeout(() => {
    if (isCurrentAttempt(attempt)) refreshCaptcha();
  }, 900);
}

/**
 * AES-ECB 加密（对齐后端 anji-captcha AESUtil）
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
 * 从登录表单 action 中解析同源验证码端点、tab_id 与 client_id。
 * action 形如 /realms/{realm}/login-actions/authenticate?...&tab_id=...
 */
function getCaptchaContext() {
  let loginAction = window.AIP_LOGIN_ACTION || '';
  // 兼容 FreeMarker 自动转义产生的 &amp;：回退到 DOM 的 form.action（浏览器已解码）
  if (!loginAction || loginAction.indexOf('&amp;') !== -1) {
    const form = document.getElementById('kc-form-login');
    loginAction = (form && form.action) || '';
  }
  if (!loginAction) {
    return null;
  }
  try {
    const url = new URL(loginAction, window.location.href);
    const marker = '/login-actions/';
    const index = url.pathname.indexOf(marker);
    const realmBase = index >= 0 ? url.pathname.substring(0, index) : url.pathname.replace(/\/+$/, '');
    const tabId = url.searchParams.get('tab_id') || '';
    const clientId = url.searchParams.get('client_id') || '';
    if (!tabId || !clientId) {
      return null;
    }
    return {
      endpoint: `${url.origin}${realmBase}/aip-captcha`,
      tabId: tabId,
      clientId: clientId
    };
  } catch (e) {
    console.error('解析登录 action 失败:', e);
    return null;
  }
}

function captchaApiUrl(path) {
  const context = captchaContext || getCaptchaContext();
  if (!context) {
    return '';
  }
  return `${context.endpoint}/${path}`
    + `?tabId=${encodeURIComponent(context.tabId)}`
    + `&clientId=${encodeURIComponent(context.clientId)}`;
}

function i18nText(key, fallback) {
  const i18n = window.AIP_I18N || {};
  return i18n[key] || fallback || '';
}

function captchaById(id) {
  return document.getElementById(id);
}

/**
 * 打开验证码弹窗
 */
function openCaptcha() {
  const mask = captchaById('captcha-mask');
  if (!mask) {
    return;
  }
  if (mask.style.display === 'flex') {
    return;
  }
  mask.style.display = 'flex';
  refreshCaptcha();
}

/**
 * 关闭验证码弹窗并重置滑块
 */
function closeCaptcha() {
  invalidateCaptchaAttempt();
  const mask = captchaById('captcha-mask');
  if (mask) {
    mask.style.display = 'none';
  }
  resetSlider();
}

/**
 * 刷新并获取拼图滑块验证码
 */
async function refreshCaptcha() {
  const attempt = invalidateCaptchaAttempt();
  resetSlider();
  if (!isCurrentAttempt(attempt)) return;
  captchaContext = getCaptchaContext();

  const tipText = captchaById('captcha-tip-text');
  const loadingEl = captchaById('captcha-loading');
  const track = captchaById('captcha-slide-track');
  if (track) {
    track.classList.remove('is-success', 'is-fail');
  }
  if (tipText) {
    tipText.className = '';
    tipText.textContent = i18nText('captchaLoading', '正在加载安全验证...');
  }
  if (loadingEl) {
    loadingEl.style.display = 'flex';
  }

  const url = captchaApiUrl('get');
  if (!url) {
    if (tipText) {
      tipText.innerHTML = `<span class="captcha-error-tip">${i18nText('captchaNetError', '网络连接失败，请刷新重试')}</span>`;
    }
    if (loadingEl) {
      loadingEl.style.display = 'none';
    }
    return;
  }

  try {
    const res = await fetchCaptcha(`${url}&captchaType=blockPuzzle`, {
      method: 'GET',
      credentials: 'same-origin',
      headers: { 'Accept': 'application/json' }
    });
    if (!isCurrentAttempt(attempt)) return;
    if (res && res.code === 200 && res.data) {
      captchaData = res.data;
      const bgImg = captchaById('captcha-bg-img');
      const jigsawImg = captchaById('captcha-jigsaw-img');
      if (bgImg) {
        bgImg.src = 'data:image/png;base64,' + res.data.originalImageBase64;
      }
      if (jigsawImg) {
        jigsawImg.src = 'data:image/png;base64,' + res.data.jigsawImageBase64;
      }
      if (tipText) {
        tipText.textContent = i18nText('captchaSlideTip', '向右拖动滑块完成拼图');
      }
      requestAnimationFrame(() => {
        if (isCurrentAttempt(attempt)) measureGeometry();
      });
    } else {
      const message = res && res.msg ? i18nText(res.msg, '') : '';
      if (tipText) {
        tipText.innerHTML = `<span class="captcha-error-tip">${message || i18nText('captchaFail', '验证码加载失败，请刷新重试')}</span>`;
      }
    }
  } catch (err) {
    if (!isCurrentAttempt(attempt)) return;
    console.error('获取验证码网络失败:', err);
    if (tipText) {
      tipText.innerHTML = `<span class="captcha-error-tip">${i18nText('captchaNetError', '网络连接失败，请刷新重试')}</span>`;
    }
  } finally {
    if (isCurrentAttempt(attempt) && loadingEl) {
      loadingEl.style.display = 'none';
    }
  }
}

/**
 * 测量底图与轨道的显示尺寸，建立像素到原生坐标的映射
 */
function measureGeometry() {
  const bgImg = captchaById('captcha-bg-img');
  const track = captchaById('captcha-slide-track');
  const handle = captchaById('captcha-slide-handle');
  if (!bgImg || !track || !handle) {
    return;
  }
  const bgWidth = bgImg.clientWidth > 0 ? bgImg.clientWidth : 310;
  const maxHandleOffset = Math.max(1, track.clientWidth - handle.offsetWidth);
  captchaGeometry = { bgWidth, maxHandleOffset };
  if (handleOffset > maxHandleOffset) {
    handleOffset = maxHandleOffset;
    renderSlider();
  }
}

/**
 * 当前显示位移对应的 AJ-Captcha 原生 x 坐标（310 坐标系）
 */
function nativeOffset() {
  if (handleOffset <= 0) {
    return 0;
  }
  const ratio = handleOffset / captchaGeometry.maxHandleOffset;
  return Math.min(263, Math.round(ratio * 263));
}

/**
 * 同步滑块手柄与拼图块位置
 */
function renderSlider() {
  const handle = captchaById('captcha-slide-handle');
  const jigsawImg = captchaById('captcha-jigsaw-img');
  if (handle) {
    handle.style.transform = `translateX(${handleOffset}px)`;
    handle.setAttribute('aria-valuenow', String(nativeOffset()));
  }
  if (jigsawImg) {
    const ratio = nativeOffset() / 263;
    const pieceTranslate = ratio * Math.max(0, captchaGeometry.bgWidth - 47 * captchaGeometry.bgWidth / 310);
    jigsawImg.style.transform = `translateX(${pieceTranslate}px)`;
  }
}

function resetHandle() {
  handleOffset = 0;
  dragging = false;
  const handle = captchaById('captcha-slide-handle');
  if (handle) {
    handle.classList.remove('is-dragging');
  }
  renderSlider();
}

function resetSlider() {
  resetHandle();
  const track = captchaById('captcha-slide-track');
  if (track) {
    track.classList.remove('is-success', 'is-fail');
  }
}

function onPointerDown(event) {
  if (!captchaData || submitting) {
    return;
  }
  dragging = true;
  dragStartX = event.clientX - handleOffset;
  const handle = captchaById('captcha-slide-handle');
  if (handle) {
    handle.classList.add('is-dragging');
  }
  measureGeometry();
  event.preventDefault();
}

function onPointerMove(event) {
  if (!dragging || submitting) {
    return;
  }
  const next = Math.max(0, Math.min(captchaGeometry.maxHandleOffset, event.clientX - dragStartX));
  handleOffset = next;
  renderSlider();
}

function onPointerUp() {
  if (!dragging || submitting) {
    return;
  }
  dragging = false;
  const handle = captchaById('captcha-slide-handle');
  if (handle) {
    handle.classList.remove('is-dragging');
  }
  if (handleOffset <= 2) {
    resetSlider();
    return;
  }
  submitCaptcha();
}

function onHandleKeyDown(event) {
  if (!captchaData || submitting) {
    return;
  }
  const step = Math.max(4, Math.round(captchaGeometry.maxHandleOffset / 20));
  if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
    handleOffset = Math.min(captchaGeometry.maxHandleOffset, handleOffset + step);
    renderSlider();
    event.preventDefault();
  } else if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
    handleOffset = Math.max(0, handleOffset - step);
    renderSlider();
    event.preventDefault();
  } else if (event.key === 'Enter' || event.key === ' ') {
    if (handleOffset > 0) {
      submitCaptcha();
      event.preventDefault();
    }
  }
}

/**
 * 提交滑块坐标校验
 */
async function submitCaptcha() {
  if (!captchaData || !captchaData.secretKey || !captchaData.token || submitting || activeRequest) {
    return;
  }
  const attempt = captchaAttempt;
  const challenge = captchaData;
  if (!isCurrentAttempt(attempt)) return;
  submitting = true;

  const tipText = captchaById('captcha-tip-text');
  const track = captchaById('captcha-slide-track');
  if (tipText) {
    tipText.className = '';
    tipText.textContent = i18nText('captchaChecking', '正在校验滑块位置...');
  }

  try {
    const pointJson = aesEncrypt(JSON.stringify({ x: nativeOffset(), y: 5 }), challenge.secretKey);
    const res = await fetchCaptcha(captchaApiUrl('check'), {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify({ captchaType: 'blockPuzzle', token: challenge.token, pointJson })
    });
    if (!isCurrentAttempt(attempt)) return;
    const ticket = res && res.code === 200 && res.data && res.data.captchaVerification;
    if (typeof ticket === 'string' && ticket) {
      if (track) track.classList.add('is-success');
      if (tipText) tipText.textContent = i18nText('captchaSuccess', '验证通过，正在登录...');
      submitTimer = setTimeout(() => {
        if (!isCurrentAttempt(attempt)) return;
        // 先关闭并清理本轮状态，再写入票据立即提交；取消时不会遗留可提交票据。
        closeCaptcha();
        const verificationInput = captchaById('captchaVerification');
        const form = captchaById('kc-form-login');
        if (form && verificationInput) {
          verificationInput.value = ticket;
          form.submit();
        }
      }, 400);
    } else {
      const message = res && res.msg ? i18nText(res.msg, '') : '';
      retryCaptcha(attempt, message || i18nText('captchaFail', '验证未通过，请重新拖动滑块'));
    }
  } catch (err) {
    if (!isCurrentAttempt(attempt)) return;
    console.error('滑块校验失败:', err);
    retryCaptcha(attempt, i18nText('captchaNetError', '网络连接失败，请刷新重试'));
  }
}

/**
 * 其他登录方式点击提示
 */
function handleOtherLogin(type) {
  if (type === 'wecom') {
    alert(i18nText('otherWecomTip', '企业微信扫码登录已纳入集成规划，当前环境请使用平台账号密码统一认证。'));
  } else {
    alert(i18nText('otherLoginTip', '暂未开放其他登录方式，请使用平台账号密码统一认证。'));
  }
}

document.addEventListener('DOMContentLoaded', function () {
  const handle = captchaById('captcha-slide-handle');
  if (handle) {
    handle.addEventListener('pointerdown', onPointerDown);
    handle.addEventListener('keydown', onHandleKeyDown);
  }
  window.addEventListener('pointermove', onPointerMove);
  window.addEventListener('pointerup', onPointerUp);
  window.addEventListener('pointercancel', onPointerUp);
  window.addEventListener('resize', measureGeometry);

  const form = captchaById('kc-form-login');
  if (form) {
    form.addEventListener('submit', function (e) {
      const usernameInput = captchaById('username');
      const passwordInput = captchaById('password');
      const verificationInput = captchaById('captchaVerification');

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

      // 未完成滑块验证时拦截提交并弹出验证码
      if (!verificationInput || !verificationInput.value) {
        e.preventDefault();
        openCaptcha();
      }
    });
  }
});
