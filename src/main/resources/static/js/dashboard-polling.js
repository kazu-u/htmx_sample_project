'use strict';

const dashboardCards = [...document.querySelectorAll('[data-dashboard-poll]')];
const dashboardToggle = document.getElementById('dashboard-auto-toggle');
const dashboardStatus = document.getElementById('dashboard-auto-status');
const dashboardPollRequests = new WeakSet();
const maximumVisibleTime = 5 * 60 * 1000;
let remainingVisibleTime = maximumVisibleTime;
let visibleSince = document.hidden ? null : performance.now();
let userPaused = false;
let timeLimitReached = false;
let expiryTimer;

function currentRemainingTime() {
  if (visibleSince === null) {
    return remainingVisibleTime;
  }
  return Math.max(0, remainingVisibleTime - (performance.now() - visibleSince));
}

function stopVisibleTimer() {
  if (visibleSince !== null) {
    remainingVisibleTime = currentRemainingTime();
    visibleSince = null;
  }
  window.clearTimeout(expiryTimer);
}

function updateDashboardStatus() {
  if (timeLimitReached) {
    dashboardStatus.textContent = '自動更新の上限（表示中の累計5分）に達しました。各カードの手動更新は利用できます。';
    dashboardToggle.disabled = true;
    dashboardToggle.textContent = '自動更新の上限に到達';
  } else if (userPaused) {
    dashboardStatus.textContent = '自動更新を一時停止しています。';
    dashboardToggle.textContent = '自動更新を再開する';
  } else if (document.hidden) {
    dashboardStatus.textContent = 'タブが非表示のため自動取得を停止しています。表示に戻ると再取得します。';
    dashboardToggle.textContent = '自動更新を一時停止する';
  } else {
    dashboardStatus.textContent = '表示中に5秒ごとに取得します。自動更新は表示中の累計5分で終了します。';
    dashboardToggle.textContent = '自動更新を一時停止する';
  }
}

function reachTimeLimit() {
  stopVisibleTimer();
  remainingVisibleTime = 0;
  timeLimitReached = true;
  dashboardCards.forEach((card) => {
    card.dataset.pollPaused = 'true';
  });
  updateDashboardStatus();
}

function startVisibleTimer() {
  if (document.hidden || userPaused || timeLimitReached) {
    return;
  }
  const remainingTime = currentRemainingTime();
  if (remainingTime <= 0) {
    reachTimeLimit();
    return;
  }
  if (visibleSince === null) {
    visibleSince = performance.now();
  }
  window.clearTimeout(expiryTimer);
  expiryTimer = window.setTimeout(reachTimeLimit, currentRemainingTime());
}

function requestCardRefresh(card) {
  if (window.htmx && card.dataset.pollPaused !== 'true') {
    window.htmx.trigger(card, 'dashboard-refresh');
  }
}

dashboardCards.forEach((card) => {
  card.dataset.pollPaused = 'false';
});

dashboardToggle.addEventListener('click', () => {
  if (userPaused) {
    if (currentRemainingTime() <= 0) {
      reachTimeLimit();
      return;
    }
    userPaused = false;
    startVisibleTimer();
    dashboardCards.forEach(requestCardRefresh);
  } else {
    stopVisibleTimer();
    userPaused = true;
  }
  updateDashboardStatus();
});

document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    stopVisibleTimer();
    updateDashboardStatus();
    return;
  }
  if (!userPaused && !timeLimitReached) {
    startVisibleTimer();
    dashboardCards.forEach(requestCardRefresh);
  }
  updateDashboardStatus();
});

window.addEventListener('pageshow', (event) => {
  if (!event.persisted) {
    return;
  }
  if (!document.hidden && !userPaused && !timeLimitReached) {
    startVisibleTimer();
    dashboardCards.forEach(requestCardRefresh);
  }
  updateDashboardStatus();
});

document.addEventListener('htmx:beforeRequest', (event) => {
  const card = event.detail.elt;
  if (!card.matches('[data-dashboard-poll]')) {
    return;
  }
  if (document.hidden || userPaused || timeLimitReached || card.dataset.pollPaused === 'true') {
    event.preventDefault();
    return;
  }
  if (currentRemainingTime() <= 0) {
    reachTimeLimit();
    event.preventDefault();
    return;
  }
  dashboardPollRequests.add(event.detail.xhr);
  card.setAttribute('aria-busy', 'true');
});

document.addEventListener('htmx:afterRequest', (event) => {
  const card = event.detail.elt.closest('[data-dashboard-card]');
  if (!card) {
    return;
  }
  const isAutomatic = dashboardPollRequests.has(event.detail.xhr);
  card.removeAttribute('aria-busy');
  const staleNotice = card.querySelector('[data-dashboard-stale]');
  if (event.detail.xhr.status === 200) {
    staleNotice.hidden = true;
    card.dataset.pollPaused = 'false';
    if (!isAutomatic) {
      document.getElementById('request-status').textContent =
        `${card.dataset.dashboardLabel}を更新しました。`;
    }
  } else {
    staleNotice.hidden = false;
    card.dataset.pollPaused = 'true';
  }
});

document.addEventListener('htmx:beforeCleanupElement', (event) => {
  if (event.detail.elt.matches?.('[data-dashboard-poll]')) {
    event.detail.elt.dataset.pollPaused = 'true';
  }
});

window.addEventListener('pagehide', stopVisibleTimer);

updateDashboardStatus();
startVisibleTimer();
