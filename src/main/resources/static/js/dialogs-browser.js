'use strict';

// プロンプトの値を通常フォームにも残し、通信失敗後に再利用できるようにする。
document.addEventListener('htmx:prompt', (event) => {
  if (typeof event.detail.prompt === 'string') {
    document.getElementById('message').value = event.detail.prompt;
  }
});

const promptInputVersions = new WeakMap();
let promptInputVersion = 0;

document.addEventListener('input', (event) => {
  if (event.target.id === 'message') {
    promptInputVersion += 1;
  }
});

document.addEventListener('htmx:beforeRequest', (event) => {
  if (event.detail.elt.id === 'dialog-submit') {
    promptInputVersions.set(event.detail.xhr, promptInputVersion);
  }
});

// htmx:prompt の値変更は要求ヘッダーに反映されないため、要求設定時に変換する。
document.addEventListener('htmx:configRequest', (event) => {
  const prompt = event.detail.headers['HX-Prompt'];
  if (prompt === undefined) {
    return;
  }
  try {
    event.detail.headers['HX-Prompt'] = encodeURIComponent(prompt);
  } catch (error) {
    if (!(error instanceof URIError)) {
      throw error;
    }
    event.preventDefault();
    const notification = document.getElementById('request-error');
    notification.textContent = '入力に送信できない文字があります。別の文字に置き換えてください。';
    notification.hidden = false;
    document.getElementById('request-status').textContent = '';
  }
});

document.addEventListener('htmx:afterRequest', (event) => {
  if (event.detail.elt?.id !== 'dialog-submit') {
    return;
  }
  const inputVersion = promptInputVersions.get(event.detail.xhr);
  promptInputVersions.delete(event.detail.xhr);
  if (inputVersion === promptInputVersion
      && event.detail.xhr.status >= 200 && event.detail.xhr.status < 300) {
    document.getElementById('message').value = '';
  }
});
