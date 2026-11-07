'use strict';

const confirmationDialog = document.getElementById('confirmation-dialog');
const confirmationQuestion = document.getElementById('confirmation-question');
const confirmRequestButton = document.getElementById('confirm-request');
let pendingIssueRequest = null;
let confirmationOpener = null;
let approvedRequestOpener = null;

document.addEventListener('htmx:confirm', (event) => {
  const trigger = event.detail.elt;
  const question = event.detail.question;
  if (!trigger.matches('[data-custom-confirm]') || !question) {
    return;
  }

  event.preventDefault();
  if (pendingIssueRequest || confirmationDialog.open) {
    return;
  }

  pendingIssueRequest = event.detail.issueRequest;
  confirmationOpener = trigger;
  confirmationQuestion.textContent = question;
  confirmationDialog.showModal();
});

document.addEventListener('htmx:afterRequest', (event) => {
  const opener = approvedRequestOpener;
  if (!opener || event.detail.elt !== opener) {
    return;
  }

  approvedRequestOpener = null;
  confirmationOpener = null;
  requestAnimationFrame(() => {
    if (document.activeElement === document.body && opener.isConnected && !opener.disabled) {
      opener.focus();
    }
  });
});

confirmRequestButton.addEventListener('click', () => {
  const issueRequest = pendingIssueRequest;
  if (!issueRequest) {
    return;
  }

  pendingIssueRequest = null;
  approvedRequestOpener = confirmationOpener;
  confirmationDialog.close();
  issueRequest(true);
});

confirmationDialog.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    event.preventDefault();
    confirmationDialog.close();
  }
});

confirmationDialog.addEventListener('close', () => {
  pendingIssueRequest = null;
  const opener = confirmationOpener;
  confirmationOpener = null;
  if (!approvedRequestOpener && opener instanceof HTMLElement && opener.isConnected) {
    opener.focus();
  }
});
