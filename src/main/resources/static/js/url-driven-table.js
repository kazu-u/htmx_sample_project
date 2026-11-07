'use strict';

const tableFocusOrigins = new WeakMap();

document.addEventListener('htmx:beforeRequest', (event) => {
  if (event.detail.target?.id !== 'order-region') {
    return;
  }
  const activeElement = document.activeElement;
  tableFocusOrigins.set(event.detail.xhr,
    activeElement instanceof HTMLElement && activeElement.id
      ? activeElement.id : event.detail.elt.id);
});

document.addEventListener('htmx:afterSettle', (event) => {
  if (event.detail.target?.id !== 'order-region') {
    return;
  }
  const region = document.getElementById('order-region');
  const invalidField = region.querySelector('[aria-invalid="true"]');
  if (invalidField) {
    invalidField.focus();
    document.getElementById('request-status').textContent =
      '入力内容を確認してください。条件は変更されていません。';
    return;
  }

  const focusId = tableFocusOrigins.get(event.detail.xhr);
  const requestedTarget = focusId ? document.getElementById(focusId) : null;
  const focusTarget = requestedTarget && region.contains(requestedTarget)
    ? requestedTarget : region.querySelector('[aria-current="page"]') || region.querySelector('#table-title');
  focusTarget?.focus();
  document.getElementById('request-status').textContent =
    `${document.getElementById('order-count').textContent} 条件とページは URL に反映されました。`;
});
