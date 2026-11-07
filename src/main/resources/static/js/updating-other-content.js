'use strict';

const contactFocusOrigins = new WeakMap();
const contactRequestForms = new WeakMap();

function isContactsForm(element) {
  return element?.id === 'contact-form' || element?.id === 'contacts-reset-form';
}

document.addEventListener('htmx:beforeSwap', (event) => {
  const target = event.detail.target;
  if (target?.id !== 'contacts-panel' || ![200, 422].includes(event.detail.xhr.status)) {
    return;
  }
  const responseDocument = new DOMParser().parseFromString(event.detail.xhr.responseText, 'text/html');
  const responsePanel = responseDocument.getElementById('contacts-panel');
  const currentRevision = Number(target.dataset.revision);
  const responseRevision = Number(responsePanel?.dataset.revision);
  if (responsePanel && Number.isSafeInteger(currentRevision) && Number.isSafeInteger(responseRevision)
      && responseRevision < currentRevision) {
    event.detail.shouldSwap = false;
    event.detail.isError = false;
  }
});

document.addEventListener('htmx:beforeRequest', (event) => {
  const source = event.detail.elt;
  const form = source?.matches('form') ? source : source?.closest('form');
  if (!isContactsForm(form)) {
    return;
  }
  contactRequestForms.set(event.detail.xhr, form.id);
  if (form.contains(document.activeElement)) {
    contactFocusOrigins.set(event.detail.xhr, document.activeElement.id);
  }
});

document.addEventListener('htmx:afterSettle', (event) => {
  if (event.detail.target?.id === 'contacts-panel' && event.detail.xhr.status === 422) {
    focusContactError();
  }
});

function restoreContactFocus(xhr) {
  const focusOriginId = contactFocusOrigins.get(xhr);
  const focusTarget = focusOriginId ? document.getElementById(focusOriginId) : null;
  if (document.activeElement === document.body && focusTarget && !focusTarget.matches(':disabled')) {
    focusTarget.focus();
  }
}

function focusContactError() {
  const invalidInput = document.querySelector('#contacts-panel [aria-invalid="true"]');
  const reloadLink = document.querySelector(
    '#revision-conflict-reload, #revision-reload, #reset-revision-reload');
  (invalidInput || reloadLink || document.getElementById('contact-name'))?.focus();
}

document.addEventListener('htmx:afterRequest', (event) => {
  const formId = contactRequestForms.get(event.detail.xhr);
  if (!formId) {
    return;
  }

  const status = event.detail.xhr.status;
  if (status >= 200 && status < 300) {
    const contactForm = document.getElementById('contact-form');
    contactForm?.reset();
    contactForm?.querySelectorAll('[aria-invalid="true"]').forEach((input) => {
      input.removeAttribute('aria-invalid');
    });
    contactForm?.querySelectorAll('.field-error').forEach((error) => {
      error.textContent = '';
    });
    requestAnimationFrame(() => restoreContactFocus(event.detail.xhr));
  } else if (status !== 422) {
    requestAnimationFrame(() => restoreContactFocus(event.detail.xhr));
  }
});

document.addEventListener('input', (event) => {
  if (!['contact-name', 'contact-email'].includes(event.target.id)) {
    return;
  }
  event.target.removeAttribute('aria-invalid');
  const error = document.getElementById(`${event.target.id}-error`);
  if (error) {
    error.textContent = '';
  }
});

document.addEventListener('DOMContentLoaded', () => {
  const validationError = document.querySelector(
    '#contact-form [aria-invalid="true"], #revision-conflict-error, #revision-error, #reset-revision-error');
  if (validationError) {
    focusContactError();
  }
});
