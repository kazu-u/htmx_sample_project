'use strict';

const comboboxInput = document.getElementById('candidate-query');
const comboboxForm = document.getElementById('combobox-form');
const selectedContactId = document.getElementById('selected-contact-id');
const comboboxStatus = document.getElementById('combobox-status');
const queryError = document.getElementById('query-error');
const candidateRequests = new WeakMap();
let candidateTimer;
let candidateVersion = 0;
let composingCandidate = false;
let activeCandidateIndex = -1;
let candidateListFresh = false;

function candidateRegion() {
  return document.getElementById('candidate-region');
}

comboboxInput.setAttribute('role', 'combobox');
comboboxInput.setAttribute('aria-autocomplete', 'list');
comboboxInput.setAttribute('aria-controls', 'candidate-list');
comboboxInput.setAttribute('aria-expanded', 'false');
if (comboboxInput.dataset.autosearch === 'true') {
  scheduleCandidates();
}

function candidateOptions() {
  return [...document.querySelectorAll('#candidate-list [role="option"]')];
}

function closeCandidates() {
  candidateRegion().hidden = true;
  comboboxInput.setAttribute('aria-expanded', 'false');
  comboboxInput.removeAttribute('aria-activedescendant');
  activeCandidateIndex = -1;
  candidateOptions().forEach((option) => option.setAttribute('aria-selected', 'false'));
}

function scheduleCandidates() {
  clearTimeout(candidateTimer);
  candidateTimer = setTimeout(() => {
    comboboxInput.dispatchEvent(new CustomEvent('combobox-search', {bubbles: true}));
  }, 300);
}

function markCandidateQueryChanged() {
  candidateVersion += 1;
  clearTimeout(candidateTimer);
  selectedContactId.value = '';
  candidateListFresh = false;
  queryError.textContent = '';
  comboboxInput.removeAttribute('aria-invalid');
  closeCandidates();
  comboboxStatus.textContent = '入力が変わりました。候補を取得します。';
}

function activateCandidate(index) {
  const options = candidateOptions();
  if (options.length === 0) {
    return;
  }
  activeCandidateIndex = (index + options.length) % options.length;
  options.forEach((option, optionIndex) => {
    option.setAttribute('aria-selected', String(optionIndex === activeCandidateIndex));
  });
  comboboxInput.setAttribute('aria-activedescendant', options[activeCandidateIndex].id);
  options[activeCandidateIndex].scrollIntoView({block: 'nearest'});
}

function chooseCandidate(option) {
  comboboxInput.value = option.dataset.label;
  selectedContactId.value = option.dataset.contactId;
  candidateVersion += 1;
  candidateListFresh = false;
  closeCandidates();
  comboboxStatus.textContent = `${option.dataset.label}を選択しました。確定ボタンで送信してください。`;
}

comboboxInput.addEventListener('input', (event) => {
  markCandidateQueryChanged();
  if (!composingCandidate && !event.isComposing) {
    scheduleCandidates();
  }
});
comboboxInput.addEventListener('compositionstart', () => {
  composingCandidate = true;
  markCandidateQueryChanged();
});
comboboxInput.addEventListener('compositionend', () => {
  composingCandidate = false;
  scheduleCandidates();
});

comboboxInput.addEventListener('keydown', (event) => {
  const options = candidateOptions();
  if (event.key === 'ArrowDown' && candidateListFresh && options.length > 0) {
    event.preventDefault();
    candidateRegion().hidden = false;
    comboboxInput.setAttribute('aria-expanded', 'true');
    activateCandidate(activeCandidateIndex + 1);
  } else if (event.key === 'ArrowUp' && candidateListFresh && options.length > 0) {
    event.preventDefault();
    candidateRegion().hidden = false;
    comboboxInput.setAttribute('aria-expanded', 'true');
    activateCandidate(activeCandidateIndex < 0 ? options.length - 1 : activeCandidateIndex - 1);
  } else if (event.key === 'Home' && !candidateRegion().hidden && options.length > 0) {
    event.preventDefault();
    activateCandidate(0);
  } else if (event.key === 'End' && !candidateRegion().hidden && options.length > 0) {
    event.preventDefault();
    activateCandidate(options.length - 1);
  } else if (event.key === 'Enter' && !candidateRegion().hidden && activeCandidateIndex >= 0) {
    event.preventDefault();
    chooseCandidate(options[activeCandidateIndex]);
  } else if (event.key === 'Escape' && !candidateRegion().hidden) {
    event.preventDefault();
    closeCandidates();
    comboboxStatus.textContent = '候補を閉じました。';
  }
});

document.addEventListener('mousedown', (event) => {
  if (event.target.closest('#candidate-region [role="option"]')) {
    event.preventDefault();
  }
});
document.addEventListener('click', (event) => {
  const option = event.target.closest('#candidate-region [role="option"]');
  if (option) {
    chooseCandidate(option);
  }
});
comboboxInput.addEventListener('blur', () => {
  setTimeout(() => {
    if (!candidateRegion().contains(document.activeElement)) {
      closeCandidates();
    }
  }, 0);
});
document.addEventListener('click', (event) => {
  if (!comboboxForm.contains(event.target)) {
    closeCandidates();
  }
});
comboboxForm.addEventListener('submit', (event) => {
  clearTimeout(candidateTimer);
  if (composingCandidate) {
    event.preventDefault();
    event.stopImmediatePropagation();
  }
});

document.addEventListener('htmx:beforeRequest', (event) => {
  if (event.detail.target.id !== 'candidate-region') {
    return;
  }
  candidateRequests.set(event.detail.xhr, {
    version: candidateVersion,
    query: event.detail.requestConfig.parameters.query
  });
  comboboxStatus.textContent = '候補を取得しています。';
});
document.addEventListener('htmx:beforeSwap', (event) => {
  if (event.detail.target.id !== 'candidate-region') {
    return;
  }
  const request = candidateRequests.get(event.detail.xhr);
  const contentType = event.detail.xhr.getResponseHeader('Content-Type') || '';
  if ([200, 422].includes(event.detail.xhr.status) && contentType.startsWith('text/html')
      && (request?.version !== candidateVersion || request?.query !== comboboxInput.value)) {
    event.detail.shouldSwap = false;
    event.detail.isError = false;
  }
});
document.addEventListener('htmx:afterSwap', (event) => {
  if (event.detail.target.id !== 'candidate-region') {
    return;
  }
  const error = candidateRegion().querySelector('#candidate-message')?.textContent.trim();
  const options = candidateOptions();
  candidateListFresh = true;
  const invalid = !!candidateRegion().querySelector('[data-candidate-error]');
  queryError.textContent = invalid ? error : '';
  comboboxInput.setAttribute('aria-invalid', String(invalid));
  candidateRegion().hidden = options.length === 0;
  comboboxInput.setAttribute('aria-expanded', String(options.length > 0));
  comboboxInput.removeAttribute('aria-activedescendant');
  activeCandidateIndex = -1;
  comboboxStatus.textContent = invalid ? error
    : options.length > 0 ? `${options.length}件の候補があります。上下矢印キーで選べます。`
      : error || '一致する候補はありません。検索語を変更してください。';
});

function showCandidateFailure(event) {
  if (event.detail.target.id === 'candidate-region') {
    closeCandidates();
    comboboxStatus.textContent =
      '候補を取得できませんでした。入力を保っています。時間をおいて再入力してください。';
  }
}

document.addEventListener('htmx:afterRequest', (event) => {
  if (event.detail.failed && event.detail.xhr.statusText !== 'abort') {
    showCandidateFailure(event);
  }
});
document.addEventListener('htmx:sendError', showCandidateFailure);
document.addEventListener('htmx:timeout', showCandidateFailure);
