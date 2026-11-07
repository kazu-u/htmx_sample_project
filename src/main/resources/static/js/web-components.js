'use strict';

const summaryTopics = Object.freeze({
  inventory: {
    label: '在庫',
    url: '/examples/web-components/summary/inventory'
  },
  orders: {
    label: '注文',
    url: '/examples/web-components/summary/orders'
  }
});

class HtmxSummaryCard extends HTMLElement {
  connectedCallback() {
    if (this.shadowRoot) {
      return;
    }

    const template = document.getElementById('summary-card-template');
    const shadowRoot = this.attachShadow({ mode: 'open' });
    shadowRoot.append(template.content.cloneNode(true));

    const topicName = this.getAttribute('topic');
    const topic = Object.hasOwn(summaryTopics, topicName) ? summaryTopics[topicName] : null;
    const title = shadowRoot.querySelector('[data-title]');
    const button = shadowRoot.querySelector('[data-load]');
    const result = shadowRoot.querySelector('.summary-result');
    const status = shadowRoot.querySelector('[data-status]');
    const error = shadowRoot.querySelector('[data-error]');

    if (!topic) {
      this.setAttribute('role', 'group');
      this.setAttribute('aria-label', '設定エラー');
      title.textContent = 'カードを表示できません';
      button.disabled = true;
      error.hidden = false;
      error.textContent = 'このカードの種類は定義されていません。';
      result.textContent = '';
      return;
    }

    this.setAttribute('role', 'group');
    this.setAttribute('aria-label', `${topic.label}の概要`);
    title.textContent = `${topic.label}の概要`;
    button.setAttribute('aria-label', `${topic.label}の情報を取得`);
    button.setAttribute('hx-get', topic.url);

    for (const eventName of ['htmx:beforeRequest', 'htmx:afterRequest',
      'htmx:responseError', 'htmx:sendError', 'htmx:timeout']) {
      shadowRoot.addEventListener(eventName, (event) => {
        if (event.detail.elt !== button) {
          return;
        }

        event.stopPropagation();
        if (eventName === 'htmx:beforeRequest') {
          error.hidden = true;
          status.textContent = '情報を取得しています。';
          result.setAttribute('aria-busy', 'true');
        } else if (eventName === 'htmx:afterRequest') {
          result.removeAttribute('aria-busy');
          if (event.detail.xhr.status === 200) {
            status.textContent = '情報を更新しました。';
          }
        } else if (eventName === 'htmx:responseError') {
          error.hidden = false;
          error.textContent = `情報を取得できませんでした（HTTP ${event.detail.xhr.status}）。直前の表示を残しています。`;
          status.textContent = '';
        } else if (eventName === 'htmx:sendError') {
          error.hidden = false;
          error.textContent = '通信できませんでした。直前の表示を残しています。接続を確認して再試行してください。';
          status.textContent = '';
        } else if (eventName === 'htmx:timeout') {
          error.hidden = false;
          error.textContent = '通信がタイムアウトしました。直前の表示を残しています。時間を置いて再試行してください。';
          status.textContent = '';
        }
      });
    }

    htmx.process(shadowRoot);
  }
}

if (!customElements.get('htmx-summary-card')) {
  customElements.define('htmx-summary-card', HtmxSummaryCard);
}
