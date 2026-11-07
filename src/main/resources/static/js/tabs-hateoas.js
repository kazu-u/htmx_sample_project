'use strict';

const tabFocusOrigins = new WeakMap();

document.addEventListener('keydown', (event) => {
  const currentTab = event.target.closest('#tab-interface [role="tab"]');
  if (!currentTab) {
    return;
  }
  const tabList = currentTab.closest('[role="tablist"]');
  const tabs = [...tabList.querySelectorAll('[role="tab"]')];
  const currentIndex = tabs.indexOf(currentTab);
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

document.addEventListener('htmx:beforeRequest', (event) => {
  if (event.detail.target?.id === 'tab-interface') {
    tabFocusOrigins.set(event.detail.xhr, document.activeElement?.id);
  }
});

document.addEventListener('htmx:afterSettle', (event) => {
  if (event.detail.target?.id !== 'tab-interface') {
    return;
  }
  const focusOrigin = tabFocusOrigins.get(event.detail.xhr);
  if (focusOrigin?.startsWith('tab-link-')) {
    document.getElementById(focusOrigin)?.focus();
  }
});
