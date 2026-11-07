'use strict';

const noteFocusOrigins = new WeakMap();

document.addEventListener('htmx:beforeRequest', (event) => {
  const form = event.detail.elt;
  if (form.id === 'note-form' && form.contains(document.activeElement)) {
    noteFocusOrigins.set(event.detail.xhr, document.activeElement);
  }
});

function restoreNoteFocus(xhr) {
  const focusOrigin = noteFocusOrigins.get(xhr);
  if (document.activeElement === document.body && focusOrigin?.isConnected
      && !focusOrigin.matches(':disabled')) {
    focusOrigin.focus();
  }
}

function handleNoteFormResponse(event) {
  const form = event.detail.elt;
  if (form.id !== 'note-form') {
    return;
  }

  const input = document.getElementById('note-text');
  const error = document.getElementById('note-error');
  const status = event.detail.xhr.status;
  if (status >= 200 && status < 300) {
    form.reset();
    input.removeAttribute('aria-invalid');
    error.textContent = '';
    document.getElementById('request-status').textContent = 'メモを追加し、入力欄を初期化しました。';
    requestAnimationFrame(() => restoreNoteFocus(event.detail.xhr));
  } else if (status === 422) {
    input.setAttribute('aria-invalid', 'true');
    input.focus();
    document.getElementById('request-status').textContent = error.textContent.trim();
  } else {
    requestAnimationFrame(() => restoreNoteFocus(event.detail.xhr));
  }
}

document.addEventListener('htmx:afterRequest', handleNoteFormResponse);

document.addEventListener('input', (event) => {
  if (event.target.id !== 'note-text') {
    return;
  }
  event.target.removeAttribute('aria-invalid');
  document.getElementById('note-error').textContent = '';
});

document.addEventListener('DOMContentLoaded', () => {
  const error = document.getElementById('note-error');
  if (error.textContent.trim()) {
    document.getElementById('note-text').focus();
  }
});
