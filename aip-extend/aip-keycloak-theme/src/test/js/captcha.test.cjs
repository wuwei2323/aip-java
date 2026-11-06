const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const crypto = require('node:crypto');

const resources = path.resolve(__dirname, '../../main/resources/theme/aip-theme/login/resources/js');
const source = fs.readFileSync(path.join(resources, 'captcha.js'), 'utf8');
const cryptoSource = fs.readFileSync(path.join(resources, 'crypto-js.min.js'), 'utf8');

// 用独立 VM 和可控 fetch/时钟验证真实主题脚本，不安装 DOM 或浏览器依赖。
function harness({ ignoreAbort = false } = {}) {
  const elements = new Map();
  const requests = [];
  const timers = new Map();
  const submittedTickets = [];
  const ready = [];
  let timerId = 0;
  function element(id) {
    if (!elements.has(id)) {
      const classes = new Set();
      const handlers = new Map();
      elements.set(id, {
        style: { display: id === 'captcha-mask' ? 'flex' : '' },
        classList: { add: (...xs) => xs.forEach(x => classes.add(x)), remove: (...xs) => xs.forEach(x => classes.delete(x)), contains: x => classes.has(x) },
        setAttribute() {}, focus() {}, value: '', textContent: '', innerHTML: '',
        clientWidth: 310, offsetWidth: id === 'captcha-slide-handle' ? 40 : 310,
        action: 'https://login.test/realms/aip/login-actions/authenticate?client_id=client-1&tab_id=tab-1',
        addEventListener(name, handler) { handlers.set(name, handler); },
        dispatch(name, event) { handlers.get(name)?.(event); },
        submit() { submittedTickets.push(element('captchaVerification').value); }
      });
    }
    return elements.get(id);
  }
  const sandbox = {
    URL, AbortController,
    console: { error() {}, log() {} },
    window: { location: { href: 'https://login.test/' }, addEventListener() {} },
    document: { getElementById: element, addEventListener(name, fn) { if (name === 'DOMContentLoaded') ready.push(fn); } },
    setTimeout(fn, ms) { const id = ++timerId; timers.set(id, { fn, ms }); return id; },
    clearTimeout(id) { timers.delete(id); },
    requestAnimationFrame(fn) { fn(); },
    fetch(url, options) {
      return new Promise((resolve, reject) => {
        const request = { url, options, reply(data) { resolve({ json: async () => data }); }, reject };
        requests.push(request);
        options.signal.addEventListener('abort', () => {
          if (!ignoreAbort) reject(Object.assign(new Error('aborted'), { name: 'AbortError' }));
        });
      });
    }
  };
  vm.createContext(sandbox);
  vm.runInContext(cryptoSource, sandbox);
  vm.runInContext(source, sandbox);
  ready.forEach(fn => fn());
  return {
    element, requests, submittedTickets,
    eval(code) { return vm.runInContext(code, sandbox); },
    runTimers(ms) {
      for (const [id, timer] of [...timers]) {
        if (timer.ms === ms && timers.has(id)) { timers.delete(id); timer.fn(); }
      }
    },
    timerCount(ms) { return [...timers.values()].filter(timer => timer.ms === ms).length; }
  };
}

const image = token => ({ code: 200, data: {
  token, secretKey: '1234567890123456', originalImageBase64: 'background', jigsawImageBase64: 'piece'
} });
const success = ticket => ({ code: 200, data: { captchaVerification: ticket, result: true } });

async function load(h, token = 'challenge') {
  const loading = h.eval('refreshCaptcha()');
  h.requests.at(-1).reply(image(token));
  await loading;
  h.eval('handleOffset = 130;');
}

test('normal success submits exactly once and preserves AES protocol', async () => {
  const h = harness();
  await load(h);
  const checking = h.eval('submitCaptcha()');
  const request = h.requests.at(-1);
  const body = JSON.parse(request.options.body);
  assert.equal(body.token, 'challenge');
  const decipher = crypto.createDecipheriv('aes-128-ecb', Buffer.from('1234567890123456'), null);
  const point = JSON.parse(Buffer.concat([decipher.update(Buffer.from(body.pointJson, 'base64')), decipher.final()]).toString());
  assert.equal(point.y, 5);
  assert.equal(point.x, h.eval('nativeOffset()'));
  request.reply(success('one-time-ticket'));
  await checking;
  assert.equal(h.eval('submitting'), true);
  await h.eval('submitCaptcha()');
  assert.equal(h.requests.length, 2);
  h.runTimers(400);
  h.runTimers(400);
  assert.deepEqual(h.submittedTickets, ['one-time-ticket']);
});

test('closing while check is pending invalidates even a late successful response', async () => {
  const h = harness({ ignoreAbort: true });
  await load(h);
  const checking = h.eval('submitCaptcha()');
  const old = h.requests.at(-1);
  h.eval('closeCaptcha()');
  assert.equal(old.options.signal.aborted, true);
  old.reply(success('stale-ticket'));
  await checking;
  h.runTimers(400);
  assert.deepEqual(h.submittedTickets, []);
  assert.equal(h.element('captchaVerification').value, '');
});

test('closing after success cancels delayed form submission', async () => {
  const h = harness();
  await load(h);
  const checking = h.eval('submitCaptcha()');
  h.requests.at(-1).reply(success('ticket'));
  await checking;
  h.eval('closeCaptcha()');
  assert.equal(h.timerCount(400), 0);
  h.runTimers(400);
  assert.deepEqual(h.submittedTickets, []);
});

test('older GET cannot overwrite a newer refresh', async () => {
  const h = harness({ ignoreAbort: true });
  const first = h.eval('refreshCaptcha()');
  const second = h.eval('refreshCaptcha()');
  h.requests[1].reply(image('newer'));
  await second;
  h.requests[0].reply(image('older'));
  await first;
  assert.equal(h.eval('captchaData.token'), 'newer');
  assert.equal(h.requests[0].options.signal.aborted, true);
});

test('old finally cannot hide loading or clear the active request of a new refresh', async () => {
  const h = harness({ ignoreAbort: true });
  const first = h.eval('refreshCaptcha()');
  const second = h.eval('refreshCaptcha()');
  h.requests[0].reply(image('older'));
  await first;
  assert.equal(h.element('captcha-loading').style.display, 'flex');
  assert.equal(h.eval('activeRequest !== null'), true);
  h.requests[1].reply(image('newer'));
  await second;
  assert.equal(h.element('captcha-loading').style.display, 'none');
});

test('manual refresh cancels the old failed-check retry timer', async () => {
  const h = harness();
  await load(h);
  const checking = h.eval('submitCaptcha()');
  h.requests.at(-1).reply({ code: 400, msg: 'captchaFail' });
  await checking;
  assert.equal(h.timerCount(900), 1);
  const refreshing = h.eval('refreshCaptcha()');
  assert.equal(h.timerCount(900), 0);
  h.runTimers(900);
  assert.equal(h.requests.length, 3);
  h.requests.at(-1).reply(image('new'));
  await refreshing;
});

test('late failure cannot unlock a newer in-flight check or schedule a retry', async () => {
  const h = harness({ ignoreAbort: true });
  await load(h);
  const oldCheck = h.eval('submitCaptcha()');
  const oldRequest = h.requests.at(-1);
  await load(h, 'new');
  const newCheck = h.eval('submitCaptcha()');
  const newRequest = h.requests.at(-1);
  oldRequest.reply({ code: 400, msg: 'captchaFail' });
  await oldCheck;
  assert.equal(h.eval('submitting'), true);
  assert.equal(h.eval('activeRequest !== null'), true);
  assert.equal(h.timerCount(900), 0);
  newRequest.reply(success('new-ticket'));
  await newCheck;
  h.runTimers(400);
  assert.deepEqual(h.submittedTickets, ['new-ticket']);
});

test('GET timeout clears loading and permits manual retry', async () => {
  const h = harness();
  const loading = h.eval('refreshCaptcha()');
  h.runTimers(15000);
  await loading;
  assert.equal(h.eval('activeRequest'), null);
  assert.equal(h.element('captcha-loading').style.display, 'none');
  await load(h, 'recovered');
  assert.equal(h.eval('captchaData.token'), 'recovered');
});

test('CHECK timeout schedules recovery and close cancels it', async () => {
  const h = harness();
  await load(h);
  const checking = h.eval('submitCaptcha()');
  h.runTimers(15000);
  await checking;
  assert.equal(h.timerCount(900), 1);
  h.eval('closeCaptcha()');
  h.runTimers(900);
  assert.equal(h.requests.length, 2);
  assert.equal(h.eval('submitting'), false);
});

test('normal request cancellation does not display a network error', async () => {
  const h = harness();
  const loading = h.eval('refreshCaptcha()');
  h.eval('closeCaptcha()');
  await loading;
  assert.equal(h.element('captcha-tip-text').innerHTML, '');
  assert.equal(h.timerCount(15000), 0);
});

test('native slider coordinates preserve integer boundaries at any display width', () => {
  const h = harness();
  for (const width of [240, 310]) {
    h.element('captcha-bg-img').clientWidth = width;
    h.element('captcha-slide-track').clientWidth = width;
    h.eval('measureGeometry(); handleOffset = 0;');
    assert.equal(h.eval('nativeOffset()'), 0);
    h.eval('handleOffset = captchaGeometry.maxHandleOffset;');
    assert.equal(h.eval('nativeOffset()'), 263);
    h.eval('handleOffset = captchaGeometry.maxHandleOffset / 2;');
    assert.equal(h.eval('nativeOffset()'), 132);
  }
});
