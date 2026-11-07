'use strict';

const shortcutForm = document.getElementById('shortcut-form');
const focusOrigins = new WeakMap();

function isEditingContext(target) {
  if (!(target instanceof Element)) {
    return false;
  }
  return Boolean(target.closest(
    'input, textarea, select, [contenteditable]:not([contenteditable="false"]), [role="textbox"], [role="combobox"], [role="searchbox"], dialog[open]'
  ));
}

document.addEventListener('keydown', (event) => {
  if (!shortcutForm || event.defaultPrevented || event.isComposing || event.repeat
      || !event.altKey || !event.shiftKey || event.ctrlKey || event.metaKey
      || event.code !== 'KeyD' || isEditingContext(event.target)) {
    return;
  }

  event.preventDefault();
  shortcutForm.requestSubmit();
});

document.addEventListener('htmx:beforeRequest', (event) => {
  if (event.detail.target?.id === 'shortcut-result'
      && document.activeElement?.id === 'shortcut-submit') {
    focusOrigins.set(event.detail.xhr, 'shortcut-submit');
  }
});

document.addEventListener('htmx:afterSettle', (event) => {
  if (event.detail.target?.id !== 'shortcut-result') {
    return;
  }
  document.getElementById('request-status').textContent = '案内を取得しました。';
});

document.addEventListener('htmx:afterRequest', (event) => {
  const focusOrigin = focusOrigins.get(event.detail.xhr);
  if (event.detail.target?.id !== 'shortcut-result' || !focusOrigin) {
    return;
  }
  requestAnimationFrame(() => {
    if (document.activeElement === document.body) {
      document.getElementById(focusOrigin)?.focus();
    }
  });
});
