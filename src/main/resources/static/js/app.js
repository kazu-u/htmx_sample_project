'use strict';

// 断片の中では登録せず、ページ全体で一度だけ CSRF・通知・フォーカスを扱う。
document.addEventListener('htmx:configRequest', (event) => {
  const url = new URL(event.detail.path, document.baseURI);
  if (url.origin !== location.origin) {
    event.preventDefault();
    return;
  }
  if (!['get', 'head', 'options'].includes(event.detail.verb.toLowerCase())) {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;
    event.detail.headers[header] = token;
  }
});

function showError(message) {
  const notification = document.getElementById('request-error');
  notification.textContent = message;
  notification.hidden = false;
  document.getElementById('request-status').textContent = '';
}

const rowNeighbors = new WeakMap();
const scrollFocus = new WeakMap();
const emailChecks = new WeakMap();
const progressFocusOrigins = new WeakMap();
const animationFocusOrigins = new WeakMap();
const undoableFocusOrigins = new WeakMap();
const undoableListFocusOrigins = new WeakMap();
const progressMutations = new WeakSet();
let emailInputVersion = 0;
let pendingRequests = 0;
let progressMutationPending = false;
const progressPolls = new WeakSet();
const dashboardPolls = new WeakSet();
let lastProgressStatus = document.getElementById('progress-state')?.dataset.jobStatus;

function isProgressRead(event) {
  return progressPolls.has(event.detail.xhr) || event.detail.elt?.id === 'progress-refresh';
}

function pauseProgressPolling() {
  const progressState = document.querySelector('#progress-state[data-progress-poll]');
  if (progressState) {
    progressState.dataset.pollPaused = 'true';
  }
}

// 入力を元に戻した場合も、入力前の要求結果を新しい検証結果として扱わない。
document.addEventListener('input', (event) => {
  if (event.target.closest('#bulk-form')) {
    document.getElementById('bulk-notice')?.remove();
    if (pendingRequests === 0) {
      document.getElementById('request-status').textContent = '選択を変更しました。まだ保存していません。';
    }
  }
  if (event.target.closest('#validation-form')) {
    document.querySelector('#validation-panel .notice')?.remove();
    if (pendingRequests === 0) {
      document.getElementById('request-status').textContent = '';
    }
  }
  if (event.target.id !== 'validation-email') {
    return;
  }
  emailInputVersion += 1;
  event.target.removeAttribute('aria-invalid');
  document.getElementById('email-feedback').textContent = '入力が変わりました。欄を離れるか、全項目を検証してください。';
});

// abort 同期は次の単独要求を捨てるため、change の処理前に旧要求を中断する。
// 全体送信中の入力は disabled なので、その要求を中断しない。
document.addEventListener('change', (event) => {
  if (event.target.id === 'prefecture-select' && !event.target.disabled) {
    event.target.setAttribute('aria-invalid', 'false');
    document.getElementById('prefecture-error').textContent = '';
  }
  if (event.target.id === 'validation-email' && !event.target.disabled) {
    const form = event.target.form;
    form.dispatchEvent(new CustomEvent('htmx:abort', {bubbles: true, detail: {elt: form}}));
  }
}, true);

function finishRowDeletion(row) {
  const neighbors = rowNeighbors.get(row);
  const adjacentTargets = [
    neighbors?.next?.querySelector('button:not(:disabled)'),
    neighbors?.previous?.querySelector('button:not(:disabled)')
  ];
  const focusTarget = adjacentTargets.find((target) => target?.isConnected)
    || document.querySelector('#tasks tr button:not(:disabled)')
    || document.getElementById('task-list-title');
  focusTarget?.focus();
  if (!document.querySelector('#tasks tr[id^="task-row-"]')) {
    document.getElementById('empty-list-message').hidden = false;
  }
  document.getElementById('request-status').textContent = pendingRequests > 0
    ? '行を削除しました。他の要求を処理中です。'
    : '行を削除しました。';
}

document.addEventListener('htmx:beforeRequest', (event) => {
  const isProgressPoll = event.detail.elt.matches('[data-progress-poll]');
  const isDashboardPoll = event.detail.elt.matches('[data-dashboard-poll]');
  const isProgressRefresh = event.detail.elt.id === 'progress-refresh';
  if (isDashboardPoll) {
    dashboardPolls.add(event.detail.xhr);
  }
  if (event.detail.target.id === 'animation-card') {
    animationFocusOrigins.set(event.detail.xhr, document.activeElement?.id);
  }
  const progressForm = event.detail.elt.closest('#progress-panel form[hx-post]');
  if (progressMutationPending && (isProgressPoll || isProgressRefresh)) {
    event.preventDefault();
    return;
  }
  if (isProgressPoll && event.detail.elt.dataset.pollPaused === 'true') {
    event.preventDefault();
    return;
  }
  if (progressForm) {
    progressMutationPending = true;
    progressMutations.add(event.detail.xhr);
    document.getElementById('progress-refresh')?.setAttribute('aria-disabled', 'true');
  }
  if (isProgressPoll) {
    progressPolls.add(event.detail.xhr);
  } else if (!isDashboardPoll) {
    pendingRequests += 1;
  }
  if (event.detail.target.id === 'scroll-more') {
    scrollFocus.set(event.detail.xhr, event.detail.target.contains(document.activeElement));
  }
  if (['progress-panel', 'progress-state'].includes(event.detail.target.id)) {
    progressFocusOrigins.set(event.detail.xhr, document.activeElement?.id);
  }
  if (event.detail.target.id === 'email-feedback') {
    emailChecks.set(event.detail.xhr, emailInputVersion);
  } else if (event.detail.target.id === 'validation-panel') {
    emailInputVersion += 1;
  }
  const row = event.detail.target;
  if (row?.id.startsWith('task-row-')) {
    rowNeighbors.set(row, {
      next: row.nextElementSibling,
      previous: row.previousElementSibling
    });
  }
  if (row?.matches('tr[data-undoable-row]')) {
    undoableFocusOrigins.set(event.detail.xhr, row.contains(document.activeElement));
  }
  if (event.detail.target?.id === 'undoable-list') {
    undoableListFocusOrigins.set(event.detail.xhr, event.detail.target.contains(document.activeElement));
  }
  if (!isProgressPoll && !isDashboardPoll) {
    document.getElementById('request-error').hidden = true;
    document.getElementById('request-status').textContent = '通信中です。';
  }
  document.querySelector('#task-list .notice')?.remove();
  document.getElementById('bulk-notice')?.remove();
  if (!isProgressPoll && !isDashboardPoll) {
    document.querySelector('[data-request-region]')?.setAttribute('aria-busy', 'true');
  }
});

// 入力エラーだけを、想定した更新先・HTML応答の場合に明示的に差し替える。
document.addEventListener('htmx:beforeSwap', (event) => {
  if (event.detail.target.id === 'progress-state'
      && event.detail.shouldSwap && event.detail.xhr.status === 200) {
    progressFocusOrigins.set(event.detail.xhr, document.activeElement?.id);
  }
  const contentType = event.detail.xhr.getResponseHeader('Content-Type') || '';
  if (event.detail.target.id === 'email-feedback'
      && [200, 422].includes(event.detail.xhr.status) && contentType.startsWith('text/html')
      && emailChecks.get(event.detail.xhr) !== emailInputVersion) {
    event.detail.shouldSwap = false;
    event.detail.isError = false;
    return;
  }
  if (event.detail.xhr.status === 422
      && (['contact', 'contacts-panel', 'email-feedback', 'validation-panel', 'search-results', 'bulk-panel',
        'city-field', 'selection-result', 'upload-result', 'file-upload-input-form',
        'note-list', 'dialog-result', 'candidate-region', 'order-region', 'application-wizard']
        .includes(event.detail.target.id)
        || event.detail.target.matches('[data-edit-row]'))
      && contentType.startsWith('text/html')) {
    event.detail.shouldSwap = true;
    event.detail.isError = false;
  }
});

document.addEventListener('htmx:afterSwap', (event) => {
  if (!event.detail.target) {
    return;
  }
  if (event.detail.target.id === 'email-feedback') {
    const result = document.querySelector('#email-feedback [data-email-valid]');
    if (result) {
      document.getElementById('validation-email').setAttribute('aria-invalid',
        result.dataset.emailValid === 'false' ? 'true' : 'false');
    }
  }
});

document.addEventListener('htmx:afterSettle', (event) => {
  if (!event.detail.target) {
    return;
  }
  if (event.detail.target.id === 'bulk-panel') {
    const panel = document.getElementById('bulk-panel');
    const errors = panel.querySelector('#bulk-errors');
    (errors || panel.querySelector('#bulk-save'))?.focus();
    document.getElementById('request-status').textContent =
      errors ? '入力内容を確認してください。保存は行っていません。'
        : panel.querySelector('#bulk-notice')?.textContent || '一覧を取得しました。';
  } else if (event.detail.target.id === 'validation-panel') {
    const panel = document.getElementById('validation-panel');
    const invalid = panel.querySelector('[aria-invalid="true"]');
    (invalid || panel.querySelector('.notice'))?.focus();
    document.getElementById('request-status').textContent =
      invalid ? '入力内容を確認してください。' : '全項目の検証が完了しました。';
  } else if (event.detail.target.id === 'contact' || event.detail.target.matches('[data-edit-row]')) {
    const contact = document.getElementById(event.detail.target.id);
    const invalid = contact.querySelector('[aria-invalid="true"]');
    const formError = [...contact.querySelectorAll('[role="alert"]')]
      .find((element) => element.textContent.trim());
    const focusTarget = invalid || formError || contact.querySelector('input[name="name"]')
      || contact.querySelector('#edit-button, [data-edit-button]');
    focusTarget?.focus();
    document.getElementById('request-status').textContent =
      invalid ? '入力内容を確認してください。'
        : formError ? formError.textContent.trim()
          : '連絡先の表示を更新しました。';
    if (pendingRequests > 0) {
      document.getElementById('request-status').textContent += '他の要求を処理中です。';
    }
  } else if (event.detail.target.id === 'application-wizard') {
    const wizard = document.getElementById('application-wizard');
    const invalid = wizard.querySelector('[aria-invalid="true"]');
    const alert = [...wizard.querySelectorAll('[role="alert"]')]
      .find((element) => element.textContent.trim());
    (invalid || alert || wizard.querySelector('[data-step-heading]'))?.focus();
    document.getElementById('request-status').textContent = invalid || alert
      ? '入力内容を確認してください。'
      : wizard.querySelector('.notice')?.textContent.trim() || '申請フォームを更新しました。';
  } else if (event.detail.target.id.startsWith('undoable-row-')) {
    const row = document.getElementById(event.detail.target.id);
    if (undoableFocusOrigins.get(event.detail.xhr)) {
      row?.querySelector('button')?.focus();
    }
    document.getElementById('request-status').textContent =
      row?.querySelector('[data-action-notice]')?.textContent.trim()
      || '一覧の状態を更新しました。';
  } else if (event.detail.target.id === 'undoable-list') {
    if (undoableListFocusOrigins.get(event.detail.xhr)) {
      document.getElementById('task-list-title')?.focus();
    }
    document.getElementById('request-status').textContent = '一覧を再取得しました。';
  } else if (event.detail.target.id === 'load-more') {
    document.getElementById('request-status').textContent = '一覧に次のデータを追加しました。';
  } else if (event.detail.target.id === 'lazy-content') {
    document.getElementById('request-status').textContent = '集計を取得しました。';
  } else if (event.detail.target.id === 'scroll-more') {
    document.getElementById('request-status').textContent = document.getElementById('scroll-end')
      ? '全9件を表示しました。' : '一覧に3件を追加しました。';
  } else if (event.detail.target.id === 'city-field') {
    const error = document.querySelector('#city-error:not(:empty)');
    if (error) {
      document.querySelector('#prefecture-select[aria-invalid="true"]')?.focus();
    }
    document.getElementById('request-status').textContent = error?.textContent.trim()
      || '市区町村の候補を更新しました。選択結果をリセットしました。';
  } else if (event.detail.target.id === 'selection-result') {
    const result = document.getElementById('selection-result');
    const error = result?.querySelector('[data-selection-error]');
    if (error) {
      const prefecture = document.getElementById('prefecture-select');
      const focusTarget = prefecture?.value
        ? document.getElementById('city-select') : prefecture;
      focusTarget?.focus();
    }
    document.getElementById('request-status').textContent =
      error?.textContent.trim() || '選択結果を確認しました。';
  } else if (event.detail.target.id === 'tab-interface') {
    const selectedTab = document.querySelector('#tab-interface [role="tab"][aria-selected="true"]');
    document.getElementById('request-status').textContent =
      selectedTab ? `${selectedTab.textContent.trim()}を表示しました。` : 'タブを更新しました。';
  } else if (event.detail.target.id === 'animation-card') {
    document.getElementById('request-status').textContent = 'カードの表示を切り替えました。';
  } else if (event.detail.target.id === 'upload-result') {
    const result = document.getElementById('upload-result');
    const error = result.querySelector('[data-upload-error]');
    const input = document.getElementById('upload-file');
    if (error) {
      input.setAttribute('aria-invalid', 'true');
      input.focus();
      document.getElementById('request-status').textContent = error.textContent.trim();
    } else {
      input.removeAttribute('aria-invalid');
      document.getElementById('request-status').textContent =
        result.querySelector('[data-upload-success]')?.textContent.trim()
          || 'ファイルを受信して検証しました。';
    }
  } else if (event.detail.target.id === 'file-upload-input-form') {
    const form = document.getElementById('file-upload-input-form');
    const fileInput = form.querySelector('#upload-input-file');
    const fileError = form.querySelector('#upload-input-file-error')?.textContent.trim();
    if (fileError) {
      fileInput.setAttribute('aria-invalid', 'true');
    } else {
      fileInput.removeAttribute('aria-invalid');
    }
    const invalid = form.querySelector('[aria-invalid="true"]');
    const success = form.querySelector('[data-upload-input-success]');
    (invalid || success)?.focus();
    document.getElementById('request-status').textContent = invalid
      ? '入力内容を確認してください。'
      : success?.textContent.trim() || '入力内容を確認しました。';
  } else if (event.detail.target.id === 'dialog-result') {
    const result = document.getElementById('dialog-result');
    const error = result.matches('[role="alert"]');
    if (error) {
      result.focus();
    }
    document.getElementById('request-status').textContent = result.textContent.trim();
  } else if (['progress-panel', 'progress-state'].includes(event.detail.target.id)) {
    const status = document.getElementById('progress-state')?.dataset.jobStatus;
    const previousStatus = lastProgressStatus;
    if (status && status !== lastProgressStatus) {
      const messages = {
        IDLE: '模擬ジョブは開始されていません。',
        RUNNING: '模擬ジョブを実行中です。',
        COMPLETED: '模擬ジョブが完了しました。',
        FAILED: '模擬ジョブは失敗しました。',
        CANCELLED: '模擬ジョブをキャンセルしました。'
      };
      document.getElementById('request-status').textContent = messages[status];
      lastProgressStatus = status;
    } else if (!progressPolls.has(event.detail.xhr)
        && event.detail.elt?.id === 'progress-refresh') {
      document.getElementById('request-status').textContent = '進捗を再取得しました。';
    }
    const focusOrigin = progressFocusOrigins.get(event.detail.xhr);
    if (event.detail.target.id === 'progress-panel' && status === 'RUNNING') {
      document.getElementById('cancel-job-button')?.focus();
    } else if (event.detail.target.id === 'progress-panel' && status === 'CANCELLED') {
      document.getElementById('start-success-button')?.focus();
    } else if (event.detail.target.id === 'progress-state'
        && previousStatus === 'RUNNING' && ['COMPLETED', 'FAILED'].includes(status)
        && focusOrigin === 'cancel-job-button' && document.activeElement === document.body) {
      document.getElementById('start-success-button')?.focus();
    }
  }
});

document.addEventListener('htmx:afterRequest', (event) => {
  if (event.detail.target.id === 'animation-card' && document.activeElement === document.body) {
    document.getElementById(animationFocusOrigins.get(event.detail.xhr))?.focus();
  }
  if (progressMutations.has(event.detail.xhr)) {
    progressMutationPending = false;
    progressMutations.delete(event.detail.xhr);
    document.querySelector('#progress-refresh[aria-disabled="true"]')?.removeAttribute('aria-disabled');
  }
  if (progressPolls.has(event.detail.xhr)) {
    if (event.detail.xhr.status !== 200) {
      event.detail.elt.dataset.pollPaused = 'true';
    }
    return;
  }
  if (dashboardPolls.has(event.detail.xhr)) {
    return;
  }
  if (event.detail.target.id === 'scroll-more' && event.detail.xhr.status === 200
      && scrollFocus.get(event.detail.xhr)
      && (document.activeElement === document.body
        || event.detail.target.contains(document.activeElement))) {
    (document.querySelector('#scroll-more button') || document.getElementById('scroll-end'))?.focus();
  }
  const row = event.detail.target;
  pendingRequests -= 1;
  const requestStatus = document.getElementById('request-status');
  const deletedRow = event.detail.xhr.status === 200
    && row?.id.startsWith('task-row-') && !row.isConnected;
  if (deletedRow) {
    finishRowDeletion(row);
  }
  if (pendingRequests > 0) {
    document.querySelector('[data-request-region]')?.setAttribute('aria-busy', 'true');
    if (!deletedRow) {
      requestStatus.textContent = '他の要求を処理中です。';
    }
  } else {
    document.querySelector('[data-request-region][aria-busy="true"]')?.removeAttribute('aria-busy');
    if (requestStatus.textContent === '通信中です。'
        || requestStatus.textContent === '他の要求を処理中です。') {
      requestStatus.textContent = '';
    }
  }
});

document.addEventListener('htmx:responseError', (event) => {
  if (isProgressRead(event)) {
    pauseProgressPolling();
    showError('進捗を取得できませんでした。表示中の値を残しました。自動取得を停止しています。「進捗を手動で取得・再試行する」で再開してください。');
    return;
  }
  if (['orders-content', 'inventory-content'].includes(event.detail.target?.id)) {
    showError('ダッシュボードのカードを更新できませんでした。前回の値を残し、そのカードの自動取得を停止しています。手動更新で再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'dialog-content') {
    showError(`案内を取得できませんでした（HTTP ${event.detail.xhr.status}）。ダイアログは開いていません。時間を置いて起動ボタンから再試行してください。`);
    return;
  }
  if (event.detail.target?.id === 'tab-interface') {
    showError(`タブを読み込めませんでした（HTTP ${event.detail.xhr.status}）。表示中のタブは維持しています。時間を置いて再試行してください。`);
    return;
  }
  if (event.detail.target?.id === 'tabs-javascript-panel') {
    showError(`タブの内容を読み込めませんでした（HTTP ${event.detail.xhr.status}）。選択中のタブと表示を維持しています。時間を置いて再試行してください。`);
    return;
  }
  const status = event.detail.xhr.status;
  if (status === 409 && (event.detail.target?.matches('tr[data-undoable-row]')
      || event.detail.target?.id === 'undoable-list')) {
    showError('別の操作で状態が変わったため更新できませんでした。一覧を再取得してから操作してください。');
    return;
  }
  const messages = {
    400: '要求が不正です。入力や操作を確認してください。',
    403: '要求を許可できませんでした。セッション失効や CSRF トークン不一致の可能性があります。ページを再読み込みして再試行してください。',
    404: '操作対象が見つかりません。ページを再読み込みしてください。',
    409: '別の操作で更新されています。入力を必要に応じて控え、再読み込みして編集し直してください。',
    413: '送信内容が上限を超えました。ファイルサイズや入力内容を確認して再試行してください。',
    503: '一時的に処理できません。新しい画面の取得で発生した場合は、既存の画面を使うか、時間を置いて再試行してください。'
  };
  showError(messages[status] || `処理を完了できませんでした（HTTP ${status}）。入力を残しています。時間を置いて再試行してください。`);
});

document.addEventListener('htmx:sendError', (event) => {
  if (isProgressRead(event)) {
    pauseProgressPolling();
    showError('進捗を取得できませんでした。表示中の値を残し、自動取得を停止しています。接続を確認して手動で再試行してください。');
    return;
  }
  if (['orders-content', 'inventory-content'].includes(event.detail.target?.id)) {
    showError('ダッシュボードのカードを取得できませんでした。前回の値を残し、そのカードの自動取得を停止しています。接続を確認して手動更新で再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'animation-card') {
    showError('表示を取得できませんでした。現在のカードを残しています。接続を確認して再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'dialog-content') {
    showError('案内を取得できませんでした。ダイアログは開いていません。接続を確認して起動ボタンから再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'tab-interface') {
    showError('タブを読み込めませんでした。表示中のタブは維持しています。接続を確認して再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'tabs-javascript-panel') {
    showError('タブの内容を読み込めませんでした。選択中のタブと表示を維持しています。接続を確認して再試行してください。');
    return;
  }
  showError('通信できませんでした。入力を残しています。接続を確認して再試行してください。');
});
document.addEventListener('htmx:timeout', (event) => {
  if (isProgressRead(event)) {
    pauseProgressPolling();
    showError('進捗の取得がタイムアウトしました。表示中の値を残し、自動取得を停止しています。接続を確認して手動で再試行してください。');
    return;
  }
  if (['orders-content', 'inventory-content'].includes(event.detail.target?.id)) {
    showError('ダッシュボードのカード取得がタイムアウトしました。前回の値を残し、そのカードの自動取得を停止しています。接続を確認して手動更新で再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'scroll-more') {
    showError('続きの取得がタイムアウトしました。一覧を残しています。「続きを読み込む・再試行する」で再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'lazy-content') {
    showError('集計の取得がタイムアウトしました。表示を残しています。「集計を取得・再取得する」で再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'search-results') {
    showError('検索がタイムアウトしました。入力と直前の結果を残しています。接続を確認し、「検索する」で再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'animation-card') {
    showError('表示の取得がタイムアウトしました。現在のカードを残しています。接続を確認して再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'dialog-content') {
    showError('案内の取得がタイムアウトしました。ダイアログは開いていません。接続を確認して起動ボタンから再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'tab-interface') {
    showError('タブの取得がタイムアウトしました。表示中のタブは維持しています。接続を確認して再試行してください。');
    return;
  }
  if (event.detail.target?.id === 'tabs-javascript-panel') {
    showError('タブの内容取得がタイムアウトしました。選択中のタブと表示を維持しています。接続を確認して再試行してください。');
    return;
  }
  const validation = ['email-feedback', 'validation-panel'].includes(event.detail.target?.id);
  showError(validation
    ? '検証がタイムアウトしました。入力を残しています。接続を確認し、欄を変更するか全項目を検証して再試行してください。'
    : '通信がタイムアウトしました。保存結果が不明なため、入力を控えて表示を再取得してから再試行してください。');
});
