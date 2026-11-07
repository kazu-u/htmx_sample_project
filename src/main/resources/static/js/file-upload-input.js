'use strict';

document.addEventListener('htmx:afterSettle', (event) => {
  if (event.detail.target.id === 'file-upload-input-form' && event.detail.xhr.status === 200) {
    document.getElementById('upload-input-file').value = '';
  }
});
