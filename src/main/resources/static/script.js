(() => {
    const dropzone = document.getElementById('dropzone');
    const fileInput = document.getElementById('fileInput');
    const fileInfo = document.getElementById('fileInfo');
    const fileNameEl = document.getElementById('fileName');
    const fileSizeEl = document.getElementById('fileSize');
    const removeFileBtn = document.getElementById('removeFile');
    const compressBtn = document.getElementById('compressBtn');
    const progressEl = document.getElementById('progress');
    const resultEl = document.getElementById('result');
    const errorBox = document.getElementById('errorBox');
    const statOriginal = document.getElementById('statOriginal');
    const statCompressed = document.getElementById('statCompressed');
    const statSavings = document.getElementById('statSavings');
    const downloadLink = document.getElementById('downloadLink');
    const startOverBtn = document.getElementById('startOver');

    let selectedFile = null;

    function formatBytes(bytes) {
        if (bytes === 0) return '0 B';
        const units = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(1024));
        const value = bytes / Math.pow(1024, i);
        return `${value.toFixed(value >= 10 || i === 0 ? 0 : 1)} ${units[i]}`;
    }

    function resetUI() {
        selectedFile = null;
        fileInput.value = '';
        fileInfo.classList.add('hidden');
        compressBtn.disabled = true;
        resultEl.classList.add('hidden');
        errorBox.classList.add('hidden');
        progressEl.classList.add('hidden');
        dropzone.classList.remove('hidden');
    }

    function showError(message) {
        errorBox.textContent = message;
        errorBox.classList.remove('hidden');
        progressEl.classList.add('hidden');
        compressBtn.disabled = false;
    }

    function handleFileSelected(file) {
        if (!file) return;

        const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
        if (!isPdf) {
            showError('Please select a PDF file.');
            return;
        }

        selectedFile = file;
        fileNameEl.textContent = file.name;
        fileSizeEl.textContent = formatBytes(file.size);
        fileInfo.classList.remove('hidden');
        compressBtn.disabled = false;
        errorBox.classList.add('hidden');
        resultEl.classList.add('hidden');
    }

    dropzone.addEventListener('click', () => fileInput.click());

    dropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropzone.classList.add('dragover');
    });

    dropzone.addEventListener('dragleave', () => {
        dropzone.classList.remove('dragover');
    });

    dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropzone.classList.remove('dragover');
        const file = e.dataTransfer.files && e.dataTransfer.files[0];
        handleFileSelected(file);
    });

    fileInput.addEventListener('change', (e) => {
        const file = e.target.files && e.target.files[0];
        handleFileSelected(file);
    });

    removeFileBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        resetUI();
    });

    startOverBtn.addEventListener('click', resetUI);

    compressBtn.addEventListener('click', async () => {
        if (!selectedFile) return;

        const level = document.querySelector('input[name="level"]:checked').value;

        errorBox.classList.add('hidden');
        resultEl.classList.add('hidden');
        progressEl.classList.remove('hidden');
        compressBtn.disabled = true;

        const formData = new FormData();
        formData.append('file', selectedFile);
        formData.append('level', level);

        try {
            const response = await fetch('/api/compress', {
                method: 'POST',
                body: formData
            });

            const data = await response.json();

            if (!response.ok || !data.success) {
                showError(data.message || 'Something went wrong while compressing your PDF.');
                return;
            }

            statOriginal.textContent = formatBytes(data.originalSize);
            statCompressed.textContent = formatBytes(data.compressedSize);
            const savings = Math.max(0, data.savingsPercent);
            statSavings.textContent = `${savings.toFixed(1)}%`;

            downloadLink.href = `/api/download/${data.id}`;

            progressEl.classList.add('hidden');
            dropzone.classList.add('hidden');
            resultEl.classList.remove('hidden');

        } catch (err) {
            showError('Could not reach the server. Please try again.');
        }
    });
})();
