'use strict';

function isUploadRequest(event) {
  return event.detail.elt?.id === 'file-upload-form'
    && event.detail.target?.id === 'upload-result';
}

document.addEventListener('htmx:beforeRequest', (event) => {
  if (!isUploadRequest(event)) {
    return;
  }
  const progress = document.getElementById('upload-progress');
  progress.value = 0;
  document.getElementById('upload-progress-value').textContent = '0%';
  event.detail.xhr.upload.addEventListener('progress', (progressEvent) => {
    if (!progressEvent.lengthComputable || progressEvent.total <= 0) {
      return;
    }
    const percent = Math.min(100, Math.floor(progressEvent.loaded / progressEvent.total * 100));
    progress.value = percent;
    document.getElementById('upload-progress-value').textContent = `${percent}%`;
  });
});

document.addEventListener('htmx:afterRequest', (event) => {
  if (!isUploadRequest(event)) {
    return;
  }
  if ([200, 422].includes(event.detail.xhr.status)) {
    document.getElementById('upload-progress').value = 100;
    document.getElementById('upload-progress-value').textContent = '送信完了';
  } else {
    document.getElementById('upload-progress').removeAttribute('value');
    document.getElementById('upload-progress-value').textContent = '送信を完了できませんでした';
  }
});

document.addEventListener('htmx:afterSettle', (event) => {
  if (event.detail.target.id === 'upload-result' && event.detail.xhr.status === 200) {
    document.getElementById('upload-file').value = '';
  }
});
