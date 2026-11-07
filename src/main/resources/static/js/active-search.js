'use strict';

// 安全設定で禁止したイベント条件式を使わず、IMEと待ち時間を外部JSで扱う。
const searchForm = document.getElementById('search-form');
const searchInput = document.getElementById('search-query');
const searchHint = document.getElementById('search-hint');
const searchRequests = new WeakMap();
let searchTimer;
let searchVersion = 0;
let composingSearch = false;

function scheduleSearch() {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    searchForm.dispatchEvent(new CustomEvent('active-search', {bubbles: true}));
  }, 500);
}

function markSearchChanged() {
  searchVersion += 1;
  clearTimeout(searchTimer);
  searchInput.removeAttribute('aria-invalid');
  searchHint.textContent = '検索条件が変わりました。表示中の結果は前の条件のものです。';
}

searchInput.addEventListener('input', (event) => {
  markSearchChanged();
  if (!composingSearch && !event.isComposing) {
    scheduleSearch();
  }
});
searchInput.addEventListener('compositionstart', () => {
  composingSearch = true;
  markSearchChanged();
});
searchInput.addEventListener('compositionend', () => {
  composingSearch = false;
  scheduleSearch();
});
searchForm.addEventListener('submit', (event) => {
  clearTimeout(searchTimer);
  if (composingSearch) {
    event.preventDefault();
    event.stopImmediatePropagation();
  }
}, true);

document.addEventListener('htmx:beforeRequest', (event) => {
  if (event.detail.target.id !== 'search-results') {
    return;
  }
  clearTimeout(searchTimer);
  searchRequests.set(event.detail.xhr, {
    version: searchVersion,
    query: event.detail.requestConfig.parameters.query
  });
  searchHint.textContent = '検索中です。表示中の結果は前回のものです。';
});

// 新しい要求の送信前でも入力は変わり得る。同じ値へ戻す操作も版で区別する。
document.addEventListener('htmx:beforeSwap', (event) => {
  if (event.detail.target.id !== 'search-results') {
    return;
  }
  const request = searchRequests.get(event.detail.xhr);
  const contentType = event.detail.xhr.getResponseHeader('Content-Type') || '';
  if ([200, 422].includes(event.detail.xhr.status) && contentType.startsWith('text/html')
      && (request?.version !== searchVersion || request?.query !== searchInput.value)) {
    event.detail.shouldSwap = false;
    event.detail.isError = false;
  }
});

document.addEventListener('htmx:afterSwap', (event) => {
  if (event.detail.target.id === 'search-results') {
    const invalid = !!document.querySelector('#search-results [data-search-error]');
    searchInput.setAttribute('aria-invalid', invalid ? 'true' : 'false');
    searchHint.textContent = invalid ? '検索語を修正して再検索してください。' : '現在の検索条件の結果です。';
  }
});

function showSearchFailure(event) {
  if (event.detail.target.id === 'search-results') {
    searchHint.textContent = '検索が完了しませんでした。表示中の結果は前回のものです。「検索する」で再試行できます。';
  }
}

document.addEventListener('htmx:afterRequest', (event) => {
  if (event.detail.failed) {
    showSearchFailure(event);
  }
});
document.addEventListener('htmx:sendError', showSearchFailure);
document.addEventListener('htmx:timeout', showSearchFailure);
