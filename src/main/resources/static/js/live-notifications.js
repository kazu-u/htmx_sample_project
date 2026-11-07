'use strict';

let lastNotificationEventId = null;

htmx.createEventSource = (url) => {
  const streamUrl = new URL(url, window.location.href);
  if (lastNotificationEventId !== null) {
    streamUrl.searchParams.set('lastEventId', lastNotificationEventId);
  }
  const eventSource = new EventSource(streamUrl.toString(), {withCredentials: true});
  eventSource.addEventListener('notification', (event) => {
    if (event.lastEventId) {
      lastNotificationEventId = event.lastEventId;
    }
  });
  return eventSource;
};

const notificationStream = document.getElementById('notification-stream');
const notificationStatus = document.getElementById('stream-status');
const notificationRetry = document.getElementById('stream-retry');
const maximumConnectionFailures = 5;
let consecutiveConnectionFailures = 0;

document.addEventListener('htmx:sseOpen', (event) => {
  if (event.target !== notificationStream) {
    return;
  }
  consecutiveConnectionFailures = 0;
  notificationStatus.textContent = 'サーバーに接続しました。通知を受信しています。';
});

document.addEventListener('htmx:sseMessage', (event) => {
  if (event.target === document.getElementById('notification-log')) {
    document.getElementById('notification-placeholder')?.remove();
  }
});

document.addEventListener('htmx:sseError', (event) => {
  if (event.target !== notificationStream) {
    return;
  }
  consecutiveConnectionFailures += 1;
  if (consecutiveConnectionFailures >= maximumConnectionFailures) {
    notificationStream.removeAttribute('sse-connect');
    event.detail.source.close();
    notificationStatus.textContent = '接続に連続して失敗したため、再試行を停止しました。';
    notificationRetry.hidden = false;
    return;
  }
  notificationStatus.textContent =
    `接続が切れました。再接続しています（${consecutiveConnectionFailures}/${maximumConnectionFailures}回目の失敗）。`;
});

document.addEventListener('htmx:sseClose', (event) => {
  if (event.target === notificationStream && event.detail.type === 'message') {
    notificationStatus.textContent = '5件の通知を配信し、接続を終了しました。';
  }
});
