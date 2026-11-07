'use strict';

const dialogOpeners = new WeakMap();
const requestOpeners = new WeakMap();

document.addEventListener('htmx:beforeRequest', (event) => {
  if (event.detail.target.id === 'dialog-content') {
    requestOpeners.set(event.detail.xhr, event.detail.elt);
  }
});

document.addEventListener('htmx:afterSwap', (event) => {
  if (event.detail.target.id !== 'dialog-content') {
    return;
  }
  const dialog = document.getElementById('details-dialog');
  if (!dialog.open) {
    dialogOpeners.set(dialog, requestOpeners.get(event.detail.xhr));
    dialog.showModal();
  }
  requestOpeners.delete(event.detail.xhr);
});

document.getElementById('details-dialog').addEventListener('close', (event) => {
  const opener = dialogOpeners.get(event.currentTarget);
  dialogOpeners.delete(event.currentTarget);
  if (opener instanceof HTMLElement && opener.isConnected) {
    opener.focus();
  }
});

document.getElementById('details-dialog').addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    event.preventDefault();
    event.currentTarget.close();
  }
});
