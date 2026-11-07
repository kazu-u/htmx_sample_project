'use strict';

const requestedTabs = new WeakMap();
const tabs = [...document.querySelectorAll('#tabs-javascript-list [role="tab"]')];
const initiallySelectedTab = tabs.find((tab) => tab.getAttribute('aria-selected') === 'true');
if (initiallySelectedTab) {
  tabs.forEach((tab) => {
    tab.tabIndex = tab === initiallySelectedTab ? 0 : -1;
  });
}

document.addEventListener('htmx:beforeRequest', (event) => {
  if (event.detail.target?.id !== 'tabs-javascript-panel') {
    return;
  }
  const selectedTab = event.detail.elt.closest('#tabs-javascript-list [role="tab"]');
  if (selectedTab) {
    // 差し替え後も要求元のタブを特定できるよう、XHR と対応付ける。
    requestedTabs.set(event.detail.xhr, selectedTab.id);
  }
});

document.addEventListener('keydown', (event) => {
  const currentTab = event.target.closest('#tabs-javascript-list [role="tab"]');
  if (!currentTab) {
    return;
  }
  const tabList = currentTab.closest('[role="tablist"]');
  const tabs = [...tabList.querySelectorAll('[role="tab"]')];
  const currentIndex = tabs.indexOf(currentTab);
  if (currentIndex < 0) {
    return;
  }
  const isRightToLeft = getComputedStyle(tabList).direction === 'rtl';
  let nextIndex;

  if (event.key === 'Home') {
    nextIndex = 0;
  } else if (event.key === 'End') {
    nextIndex = tabs.length - 1;
  } else if (event.key === 'ArrowRight') {
    nextIndex = (currentIndex + (isRightToLeft ? -1 : 1) + tabs.length) % tabs.length;
  } else if (event.key === 'ArrowLeft') {
    nextIndex = (currentIndex + (isRightToLeft ? 1 : -1) + tabs.length) % tabs.length;
  } else if (event.key === ' ') {
    event.preventDefault();
    currentTab.click();
    return;
  } else {
    return;
  }

  event.preventDefault();
  tabs[nextIndex].focus();
});

document.addEventListener('htmx:afterSettle', (event) => {
  if (event.detail.target?.id !== 'tabs-javascript-panel') {
    return;
  }
  const selectedTab = document.getElementById(requestedTabs.get(event.detail.xhr));
  if (!selectedTab) {
    return;
  }

  // 内容の差し替えが完了した後で、選択状態と読み上げ通知を更新する。
  document.querySelectorAll('#tabs-javascript-list [role="tab"]').forEach((tab) => {
    const isSelected = tab === selectedTab;
    tab.setAttribute('aria-selected', String(isSelected));
    tab.tabIndex = isSelected ? 0 : -1;
  });
  document.getElementById('tabs-javascript-panel')?.setAttribute('aria-labelledby', selectedTab.id);
  document.getElementById('request-status').textContent =
    `${selectedTab.textContent.trim()}の内容を表示しました。`;
});
