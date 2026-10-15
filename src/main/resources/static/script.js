(() => {
    // ==========================================
    // 1. Tool Directory & Metadata
    // ==========================================
    const TOOLS = [
        // --- Popular & Compress ---
        {
            id: 'compress',
            name: 'Compress PDF',
            category: 'popular',
            subCategory: 'compress',
            desc: 'Reduce file size while optimizing quality for web and email.',
            endpoint: '/api/compress',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 14h6m-6 4h6m8-10l-4-4-4 4m4-4v12"></path></svg>`,
            motionType: 'compress',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Compression Level</label>
                    <div class="opt-chips">
                        <label class="opt-chip">
                            <input type="radio" name="level" value="LOW" />
                            <span>Low</span>
                            <small>Highest quality</small>
                        </label>
                        <label class="opt-chip">
                            <input type="radio" name="level" value="MEDIUM" checked />
                            <span>Medium</span>
                            <small>Recommended</small>
                        </label>
                        <label class="opt-chip">
                            <input type="radio" name="level" value="HIGH" />
                            <span>High</span>
                            <small>Smallest file</small>
                        </label>
                    </div>
                </div>
            `,
            buildPayload: (formData) => {
                const level = document.querySelector('input[name="level"]:checked')?.value || 'MEDIUM';
                formData.append('level', level);
            }
        },
        {
            id: 'merge',
            name: 'Merge PDF',
            category: 'popular',
            subCategory: 'organize',
            desc: 'Combine multiple PDF files into one ordered document.',
            endpoint: '/api/tools/merge',
            accept: '.pdf,application/pdf',
            multiple: true,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 7h12m-12 5h12m-12 5h6M4 4v16"></path></svg>`,
            motionType: 'merge'
        },
        {
            id: 'split',
            name: 'Split PDF',
            category: 'organize',
            desc: 'Extract specific page ranges or split each page into a ZIP archive.',
            endpoint: '/api/tools/split',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="6" cy="6" r="3"></circle><circle cx="6" cy="18" r="3"></circle><line x1="20" y1="4" x2="8.12" y2="15.88"></line><line x1="14.47" y1="14.48" x2="20" y2="20"></line><line x1="8.12" y1="8.12" x2="12" y2="12"></line></svg>`,
            motionType: 'split',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Split Mode</label>
                    <select id="splitMode" class="opt-select">
                        <option value="range" selected>Extract specified page range</option>
                        <option value="all">Split every page into ZIP archive</option>
                    </select>
                </div>
                <div class="opt-field" id="splitRangeField">
                    <label class="opt-label">Page Selection</label>
                    <input type="text" id="splitRange" class="opt-input" placeholder="e.g. 1-2, 4" value="1-2" />
                    <span class="opt-hint">Enter comma-separated pages or ranges (e.g. 1-3, 5).</span>
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('mode', document.getElementById('splitMode')?.value || 'range');
                formData.append('range', document.getElementById('splitRange')?.value || '');
            }
        },
        {
            id: 'organize',
            name: 'Organize PDF',
            category: 'organize',
            desc: 'Reorder, rotate, or delete pages to restructure your document.',
            endpoint: '/api/tools/organize',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>`,
            motionType: 'organize',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">New Page Order</label>
                    <input type="text" id="orgOrder" class="opt-input" placeholder="e.g. 2, 1, 3" value="default" />
                    <span class="opt-hint">Specify sequence in comma-separated order, or 'default'.</span>
                </div>
                <div class="opt-field">
                    <label class="opt-label">Delete Pages (Optional)</label>
                    <input type="text" id="orgRemove" class="opt-input" placeholder="e.g. 3, 5" />
                </div>
                <div class="opt-field">
                    <label class="opt-label">Rotate Document</label>
                    <select id="orgRotate" class="opt-select">
                        <option value="0" selected>No rotation (0°)</option>
                        <option value="90">90° Clockwise</option>
                        <option value="180">180°</option>
                        <option value="270">270°</option>
                    </select>
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('order', document.getElementById('orgOrder')?.value || 'default');
                formData.append('remove', document.getElementById('orgRemove')?.value || '');
                formData.append('rotate', document.getElementById('orgRotate')?.value || '0');
            }
        },
        {
            id: 'rotate',
            name: 'Rotate PDF',
            category: 'organize',
            desc: 'Permanently rotate pages in 90-degree increments.',
            endpoint: '/api/tools/rotate',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"></polyline><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path></svg>`,
            motionType: 'rotate',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Orientation</label>
                    <div class="opt-chips">
                        <label class="opt-chip">
                            <input type="radio" name="rotDeg" value="90" checked />
                            <span>90°</span>
                            <small>Clockwise</small>
                        </label>
                        <label class="opt-chip">
                            <input type="radio" name="rotDeg" value="180" />
                            <span>180°</span>
                            <small>Invert</small>
                        </label>
                        <label class="opt-chip">
                            <input type="radio" name="rotDeg" value="270" />
                            <span>270°</span>
                            <small>Counter-CW</small>
                        </label>
                    </div>
                </div>
            `,
            buildPayload: (formData) => {
                const deg = document.querySelector('input[name="rotDeg"]:checked')?.value || '90';
                formData.append('degrees', deg);
                formData.append('range', 'all');
            }
        },
        {
            id: 'remove-pages',
            name: 'Remove Pages',
            category: 'organize',
            desc: 'Delete unwanted or blank pages from your document.',
            endpoint: '/api/tools/remove-pages',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>`,
            motionType: 'organize',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Pages to Delete</label>
                    <input type="text" id="delPages" class="opt-input" placeholder="e.g. 2, 4-6" />
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('pages', document.getElementById('delPages')?.value || '');
            }
        },
        {
            id: 'crop',
            name: 'Crop Margins',
            category: 'organize',
            desc: 'Trim page margins and whitespace for cleaner viewing.',
            endpoint: '/api/tools/crop',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6.13 1L6 16a2 2 0 0 0 2 2h15"></path><path d="M1 6.13L16 6a2 2 0 0 1 2 2v15"></path></svg>`,
            motionType: 'compress',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Margin Trim (Points)</label>
                    <input type="number" id="cropMargin" class="opt-input" value="20" min="5" max="72" />
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('margin', document.getElementById('cropMargin')?.value || '20');
            }
        },

        // --- Convert ---
        {
            id: 'pdf-to-word',
            name: 'PDF to Word',
            category: 'convert',
            desc: 'Extract text paragraphs and generate an editable Word (DOCX) document.',
            endpoint: '/api/tools/pdf-to-word',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>`,
            motionType: 'pdf-to-word'
        },
        {
            id: 'pdf-to-excel',
            name: 'PDF to Excel',
            category: 'convert',
            desc: 'Extract tabular lines and data into a structured Excel (XLSX) workbook.',
            endpoint: '/api/tools/pdf-to-excel',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><line x1="3" y1="9" x2="21" y2="9"></line><line x1="3" y1="15" x2="21" y2="15"></line><line x1="9" y1="3" x2="9" y2="21"></line><line x1="15" y1="3" x2="15" y2="21"></line></svg>`,
            motionType: 'pdf-to-word'
        },
        {
            id: 'pdf-to-img',
            name: 'PDF to JPG',
            category: 'convert',
            desc: 'Render document pages into clean JPG image files.',
            endpoint: '/api/tools/pdf-to-img',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>`,
            motionType: 'pdf-to-img',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Image Format</label>
                    <div class="opt-chips">
                        <label class="opt-chip">
                            <input type="radio" name="imgFmt" value="jpg" checked />
                            <span>JPG</span>
                            <small>Compact</small>
                        </label>
                        <label class="opt-chip">
                            <input type="radio" name="imgFmt" value="png" />
                            <span>PNG</span>
                            <small>Lossless</small>
                        </label>
                    </div>
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('format', document.querySelector('input[name="imgFmt"]:checked')?.value || 'jpg');
                formData.append('dpi', '150');
            }
        },
        {
            id: 'img-to-pdf',
            name: 'JPG to PDF',
            category: 'convert',
            desc: 'Convert one or multiple JPG or PNG images into a PDF document.',
            endpoint: '/api/tools/img-to-pdf',
            accept: 'image/*,.jpg,.jpeg,.png,.webp',
            multiple: true,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><circle cx="10" cy="13" r="2"></circle><path d="M20 17l-4-4-5 5"></path></svg>`,
            motionType: 'img-to-pdf'
        },
        {
            id: 'pdf-to-text',
            name: 'PDF to Text',
            category: 'convert',
            desc: 'Extract clean plain text from your PDF into a downloadable .txt file.',
            endpoint: '/api/tools/pdf-to-text',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="17" y1="10" x2="3" y2="10"></line><line x1="21" y1="6" x2="3" y2="6"></line><line x1="21" y1="14" x2="3" y2="14"></line><line x1="17" y1="18" x2="3" y2="18"></line></svg>`,
            motionType: 'pdf-to-word'
        },
        {
            id: 'text-to-pdf',
            name: 'Text to PDF',
            category: 'convert',
            desc: 'Format plain text or uploaded notes into a clean paginated PDF.',
            endpoint: '/api/tools/text-to-pdf',
            accept: '.txt,text/plain',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>`,
            motionType: 'img-to-pdf',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Document Title</label>
                    <input type="text" id="txtTitle" class="opt-input" placeholder="e.g. Document" value="Document" />
                </div>
                <div class="opt-field">
                    <label class="opt-label">Or Type Text Content</label>
                    <textarea id="txtContent" class="opt-textarea" rows="3" placeholder="Enter text here if not uploading a .txt file..."></textarea>
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('title', document.getElementById('txtTitle')?.value || 'Document');
                const t = document.getElementById('txtContent')?.value;
                if (t && t.trim()) formData.append('text', t.trim());
            }
        },
        {
            id: 'markdown-to-pdf',
            name: 'Markdown to PDF',
            category: 'convert',
            desc: 'Transform Markdown (.md) documents with headings and lists into PDF.',
            endpoint: '/api/tools/markdown-to-pdf',
            accept: '.md,text/markdown',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 7 4 4 20 4 20 7"></polyline><line x1="9" y1="20" x2="15" y2="20"></line><line x1="12" y1="4" x2="12" y2="20"></line></svg>`,
            motionType: 'img-to-pdf',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Document Title</label>
                    <input type="text" id="mdTitle" class="opt-input" placeholder="e.g. Markdown Document" value="Markdown Document" />
                </div>
                <div class="opt-field">
                    <label class="opt-label">Or Paste Markdown</label>
                    <textarea id="mdContent" class="opt-textarea" rows="3" placeholder="# Title\n\n- Key point\n- Description"></textarea>
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('title', document.getElementById('mdTitle')?.value || 'Markdown Document');
                const m = document.getElementById('mdContent')?.value;
                if (m && m.trim()) formData.append('markdown', m.trim());
            }
        },

        // --- Edit & Security ---
        {
            id: 'watermark',
            name: 'Watermark PDF',
            category: 'edit',
            desc: 'Apply a transparent text watermark stamp across every page.',
            endpoint: '/api/tools/watermark',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path></svg>`,
            motionType: 'watermark',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Watermark Text</label>
                    <input type="text" id="wmText" class="opt-input" value="CONFIDENTIAL" />
                </div>
                <div class="opt-field">
                    <label class="opt-label">Opacity (10% to 80%)</label>
                    <input type="range" id="wmOpacity" min="0.1" max="0.8" step="0.05" value="0.25" style="width:100%;" />
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('text', document.getElementById('wmText')?.value || 'CONFIDENTIAL');
                formData.append('opacity', document.getElementById('wmOpacity')?.value || '0.25');
                formData.append('rotation', '45');
            }
        },
        {
            id: 'page-numbers',
            name: 'Add Page Numbers',
            category: 'edit',
            desc: 'Stamp clean page numbers with custom position and format.',
            endpoint: '/api/tools/page-numbers',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="9" x2="20" y2="9"></line><line x1="4" y1="15" x2="20" y2="15"></line><line x1="10" y1="3" x2="8" y2="21"></line><line x1="16" y1="3" x2="14" y2="21"></line></svg>`,
            motionType: 'page-numbers',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Position</label>
                    <select id="pnPos" class="opt-select">
                        <option value="bottom-center" selected>Bottom Center</option>
                        <option value="bottom-right">Bottom Right</option>
                        <option value="top-center">Top Center</option>
                    </select>
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('position', document.getElementById('pnPos')?.value || 'bottom-center');
                formData.append('format', 'Page {n} of {total}');
            }
        },
        {
            id: 'protect',
            name: 'Protect PDF',
            category: 'security',
            desc: 'Encrypt your document with 128-bit AES password protection.',
            endpoint: '/api/tools/protect',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>`,
            motionType: 'protect',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Set Document Password</label>
                    <input type="password" id="protectPwd" class="opt-input" placeholder="Enter password to encrypt file..." />
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('password', document.getElementById('protectPwd')?.value || '');
            }
        },
        {
            id: 'unlock',
            name: 'Unlock PDF',
            category: 'security',
            desc: 'Remove password encryption using your authorized password.',
            endpoint: '/api/tools/unlock',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 9.9-1"></path></svg>`,
            motionType: 'unlock',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Enter Password</label>
                    <input type="password" id="unlockPwd" class="opt-input" placeholder="Current file password..." />
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('password', document.getElementById('unlockPwd')?.value || '');
            }
        },
        {
            id: 'redact',
            name: 'Redact PDF',
            category: 'security',
            desc: 'Permanently blackout confidential keywords or sensitive zones.',
            endpoint: '/api/tools/redact',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg>`,
            motionType: 'watermark',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Sensitive Term / Confidential Notice</label>
                    <input type="text" id="redactTerm" class="opt-input" placeholder="e.g. Confidential, SSN" />
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('text', document.getElementById('redactTerm')?.value || '');
            }
        },
        {
            id: 'repair',
            name: 'Repair PDF',
            category: 'security',
            desc: 'Analyze, rebuild, and recover content from corrupted PDF files.',
            endpoint: '/api/tools/repair',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path></svg>`,
            motionType: 'ocr'
        },
        {
            id: 'flatten',
            name: 'Flatten PDF',
            category: 'security',
            desc: 'Flatten fillable forms and annotations directly into page content.',
            endpoint: '/api/tools/flatten',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 14 10 14 10 20"></polyline><polyline points="20 10 14 10 14 4"></polyline><line x1="14" y1="10" x2="21" y2="3"></line><line x1="3" y1="21" x2="10" y2="14"></line></svg>`,
            motionType: 'compress'
        },

        // --- OCR & Scan ---
        {
            id: 'ocr',
            name: 'OCR Searchable PDF',
            category: 'ocr',
            desc: 'Recognize scanned text in document images to make content searchable.',
            endpoint: '/api/tools/ocr',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7V4h3"></path><path d="M20 7V4h-3"></path><path d="M4 17v3h3"></path><path d="M20 17v3h-3"></path><circle cx="12" cy="12" r="3"></circle></svg>`,
            motionType: 'ocr',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Document Language</label>
                    <select id="ocrLang" class="opt-select">
                        <option value="eng" selected>English</option>
                        <option value="spa">Spanish</option>
                        <option value="fra">French</option>
                        <option value="deu">German</option>
                    </select>
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('lang', document.getElementById('ocrLang')?.value || 'eng');
            }
        },
        {
            id: 'inspect',
            name: 'Inspect Metadata',
            category: 'ocr',
            desc: 'Display technical properties, author, page count, and encryption status.',
            endpoint: '/api/tools/inspect',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`,
            motionType: 'ocr'
        },
        {
            id: 'remove-metadata',
            name: 'Remove Metadata',
            category: 'ocr',
            desc: 'Sanitize document by removing author names, producer tags, and dates.',
            endpoint: '/api/tools/remove-metadata',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"></path></svg>`,
            motionType: 'ocr'
        },
        {
            id: 'extract-images',
            name: 'Extract Images',
            category: 'ocr',
            desc: 'Extract all embedded raster images into a downloadable ZIP archive.',
            endpoint: '/api/tools/extract-images',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path></svg>`,
            motionType: 'pdf-to-img'
        },

        // --- AI Tools ---
        {
            id: 'ai-summarize',
            name: 'AI Summarizer',
            category: 'ai',
            desc: 'Generate an executive summary and core bulleted takeaways.',
            endpoint: '/api/tools/ai-summarize',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>`,
            motionType: 'ocr'
        },
        {
            id: 'ai-chat',
            name: 'Chat with PDF',
            category: 'ai',
            desc: 'Ask questions about your document and receive answers grounded in content.',
            endpoint: '/api/tools/ai-chat',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>`,
            motionType: 'ocr',
            renderOptions: () => `
                <div class="opt-field">
                    <label class="opt-label">Your Question</label>
                    <input type="text" id="aiQuestion" class="opt-input" placeholder="e.g. What are the key contract dates?" value="What is the primary topic of this document?" />
                </div>
            `,
            buildPayload: (formData) => {
                formData.append('question', document.getElementById('aiQuestion')?.value || 'Summarize key points');
            }
        },
        {
            id: 'ai-keypoints',
            name: 'Extract Key Points',
            category: 'ai',
            desc: 'Identify important deadlines, obligations, statistics, and action items.',
            endpoint: '/api/tools/ai-keypoints',
            accept: '.pdf,application/pdf',
            multiple: false,
            svgIcon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 14 14"></polyline></svg>`,
            motionType: 'ocr'
        }
    ];

    // ==========================================
    // 2. DOM Elements
    // ==========================================
    const dashboardView = document.getElementById('dashboardView');
    const toolWorkspace = document.getElementById('toolWorkspace');
    const toolsGrid = document.getElementById('toolsGrid');
    const toolSearchInput = document.getElementById('toolSearchInput');
    const categoryTabs = document.getElementById('categoryTabs');
    const noResultsFound = document.getElementById('noResultsFound');
    const clearSearchBtn = document.getElementById('clearSearchBtn');

    // Workspace elements
    const backToDashboardBtn = document.getElementById('backToDashboardBtn');
    const returnHomeBtn = document.getElementById('returnHomeBtn');
    const processAnotherBtn = document.getElementById('processAnotherBtn');
    const workspaceHeading = document.getElementById('workspaceHeading');
    const workspaceDescription = document.getElementById('workspaceDescription');
    const workspaceIconWrap = document.getElementById('workspaceIconWrap');
    const workspaceCategoryLabel = document.getElementById('workspaceCategoryLabel');
    const workspaceToolLabel = document.getElementById('workspaceToolLabel');

    const dropzone = document.getElementById('dropzone');
    const fileInput = document.getElementById('fileInput');
    const dropzonePrompt = document.getElementById('dropzonePrompt');
    const fileQueue = document.getElementById('fileQueue');
    const fileListContainer = document.getElementById('fileListContainer');
    const addMoreFilesBtn = document.getElementById('addMoreFilesBtn');
    const toolConfigForm = document.getElementById('toolConfigForm');
    const actionBtn = document.getElementById('actionBtn');
    const actionBtnText = document.getElementById('actionBtnText');

    // Motion stage
    const animationStage = document.getElementById('animationStage');
    const stageMotionBox = document.getElementById('stageMotionBox');
    const animationStatusText = document.getElementById('animationStatusText');

    // Error banner
    const errorBanner = document.getElementById('errorBanner');
    const errorMessage = document.getElementById('errorMessage');
    const dismissErrorBtn = document.getElementById('dismissErrorBtn');

    // Result panel
    const resultState = document.getElementById('resultState');
    const resultMetrics = document.getElementById('resultMetrics');
    const statOriginal = document.getElementById('statOriginal');
    const statResult = document.getElementById('statResult');
    const statSavings = document.getElementById('statSavings');
    const statSavingsCard = document.getElementById('statSavingsCard');
    const richDataViewer = document.getElementById('richDataViewer');
    const viewerContent = document.getElementById('viewerContent');
    const viewerTitle = document.getElementById('viewerTitle');
    const copyDataBtn = document.getElementById('copyDataBtn');
    const downloadBtn = document.getElementById('downloadBtn');

    // Privacy modal
    const privacyModal = document.getElementById('privacyModal');
    const openPrivacyModalBtn = document.getElementById('openPrivacyModalBtn');
    const closePrivacyModalBtn = document.getElementById('closePrivacyModalBtn');
    const dismissPrivacyModalBtn = document.getElementById('dismissPrivacyModalBtn');
    const footerPrivacyLink = document.getElementById('footerPrivacyLink');

    // State
    let activeTool = null;
    let selectedFiles = [];
    let currentCategory = 'all';

    // ==========================================
    // 3. Helper Functions
    // ==========================================
    function formatBytes(bytes) {
        if (!bytes || bytes === 0) return '0 B';
        const units = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(1024));
        const val = bytes / Math.pow(1024, i);
        return `${val.toFixed(val >= 10 || i === 0 ? 0 : 1)} ${units[i]}`;
    }

    function renderToolsGrid(query = '', category = 'all') {
        const q = query.toLowerCase().trim();
        const filtered = TOOLS.filter(t => {
            const matchesCategory = (category === 'all')
                || (category === 'popular' && (t.category === 'popular' || t.subCategory === 'organize' || t.id === 'pdf-to-word' || t.id === 'img-to-pdf'))
                || (t.category === category);
            const matchesQuery = !q
                || t.name.toLowerCase().includes(q)
                || t.desc.toLowerCase().includes(q)
                || t.id.includes(q);
            return matchesCategory && matchesQuery;
        });

        toolsGrid.innerHTML = '';
        if (filtered.length === 0) {
            noResultsFound.classList.remove('hidden');
        } else {
            noResultsFound.classList.add('hidden');
            filtered.forEach(tool => {
                const card = document.createElement('a');
                card.className = 'tool-card';
                card.href = `#${tool.id}`;
                card.innerHTML = `
                    <div>
                        <div class="card-icon-line">${tool.svgIcon}</div>
                        <h3 class="card-title">${tool.name}</h3>
                        <p class="card-desc">${tool.desc}</p>
                    </div>
                    <div class="card-foot">
                        <span>Open tool</span>
                        <span>→</span>
                    </div>
                `;
                card.addEventListener('click', (e) => {
                    e.preventDefault();
                    window.location.hash = tool.id;
                });
                toolsGrid.appendChild(card);
            });
        }
    }

    // ==========================================
    // 4. Purposeful Motion Generator per Tool
    // ==========================================
    function getMotionMarkup(tool) {
        const type = tool.motionType || 'compress';
        switch (type) {
            case 'compress':
                return `
                    <div class="motion-compress-frame">
                        <div class="motion-compress-layer"></div>
                        <div class="motion-compress-layer" style="width: 34px;"></div>
                        <div class="motion-compress-layer" style="width: 26px;"></div>
                    </div>
                `;
            case 'merge':
                return `
                    <div class="motion-merge-wrap">
                        <div class="motion-doc-card motion-doc-left"></div>
                        <div class="motion-doc-card motion-doc-right"></div>
                    </div>
                `;
            case 'split':
                return `
                    <div class="motion-split-wrap">
                        <div class="motion-split-half-left"></div>
                        <div class="motion-laser-line"></div>
                        <div class="motion-split-half-right"></div>
                    </div>
                `;
            case 'rotate':
                return `
                    <div class="motion-rotate-card">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#4F46E5" stroke-width="2"><polyline points="23 4 23 10 17 10"></polyline><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path></svg>
                    </div>
                `;
            case 'organize':
                return `
                    <div class="motion-merge-wrap">
                        <div class="motion-doc-card" style="transform: translateX(-24px); border-color:#CBD5E1;"></div>
                        <div class="motion-doc-card" style="transform: scale(1.08); z-index:2; border-color:var(--accent);"></div>
                        <div class="motion-doc-card" style="transform: translateX(24px); border-color:#CBD5E1;"></div>
                    </div>
                `;
            case 'pdf-to-word':
                return `
                    <div class="motion-convert-wrap">
                        <div class="motion-box-source">PDF</div>
                        <span class="motion-arrow-pulse">➔</span>
                        <div class="motion-box-target">DOCX</div>
                    </div>
                `;
            case 'pdf-to-img':
                return `
                    <div class="motion-convert-wrap">
                        <div class="motion-box-source">PDF</div>
                        <span class="motion-arrow-pulse">➔</span>
                        <div class="motion-box-target">JPG</div>
                    </div>
                `;
            case 'img-to-pdf':
                return `
                    <div class="motion-convert-wrap">
                        <div class="motion-box-source">IMG</div>
                        <span class="motion-arrow-pulse">➔</span>
                        <div class="motion-box-target">PDF</div>
                    </div>
                `;
            case 'protect':
                return `
                    <div class="motion-lock-wrap">
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                    </div>
                `;
            case 'unlock':
                return `
                    <div class="motion-lock-wrap">
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#10B981" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 9.9-1"></path></svg>
                    </div>
                `;
            case 'watermark':
                return `
                    <div class="motion-compress-frame" style="animation:none;">
                        <span style="font-size:9px; font-weight:800; color:#94A3B8; transform:rotate(-30deg);">STAMP</span>
                    </div>
                `;
            case 'page-numbers':
                return `
                    <div class="motion-compress-frame" style="animation:none; justify-content: flex-end; padding-bottom:8px;">
                        <span style="font-size:10px; font-family:monospace; font-weight:700; color:var(--accent);">01 / 03</span>
                    </div>
                `;
            case 'ocr':
            default:
                return `
                    <div class="motion-ocr-card">
                        <div class="motion-ocr-beam"></div>
                        <div class="motion-ocr-line"></div>
                        <div class="motion-ocr-line" style="width:75%;"></div>
                        <div class="motion-ocr-line" style="width:90%;"></div>
                        <div class="motion-ocr-line" style="width:60%;"></div>
                    </div>
                `;
        }
    }

    // ==========================================
    // 5. Open Tool Workspace
    // ==========================================
    function openTool(toolId) {
        const tool = TOOLS.find(t => t.id === toolId);
        if (!tool) {
            showDashboard();
            return;
        }

        activeTool = tool;
        selectedFiles = [];

        workspaceHeading.textContent = tool.name;
        workspaceDescription.textContent = tool.desc;
        workspaceIconWrap.innerHTML = tool.svgIcon;
        workspaceCategoryLabel.textContent = tool.category.toUpperCase();
        workspaceToolLabel.textContent = tool.name;
        actionBtnText.textContent = tool.name;

        dropzonePrompt.textContent = tool.multiple
            ? `Choose ${tool.name} files or drag them here`
            : `Choose a file or drag it here`;
        fileInput.accept = tool.accept || '*/*';
        fileInput.multiple = !!tool.multiple;
        addMoreFilesBtn.classList.toggle('hidden', !tool.multiple);

        // Render configuration form
        if (typeof tool.renderOptions === 'function') {
            toolConfigForm.innerHTML = tool.renderOptions();
            toolConfigForm.classList.remove('hidden');
        } else {
            toolConfigForm.innerHTML = '';
            toolConfigForm.classList.add('hidden');
        }

        resetWorkspace();
        dashboardView.classList.add('hidden');
        toolWorkspace.classList.remove('hidden');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function showDashboard() {
        activeTool = null;
        selectedFiles = [];
        toolWorkspace.classList.add('hidden');
        dashboardView.classList.remove('hidden');
        window.location.hash = 'dashboard';
        renderToolsGrid(toolSearchInput.value, currentCategory);
    }

    function resetWorkspace() {
        selectedFiles = [];
        fileInput.value = '';
        fileQueue.classList.add('hidden');
        fileListContainer.innerHTML = '';
        actionBtn.disabled = true;
        animationStage.classList.add('hidden');
        errorBanner.classList.add('hidden');
        resultState.classList.add('hidden');
        dropzone.classList.remove('hidden');
    }

    function updateFileQueueUI() {
        fileListContainer.innerHTML = '';
        if (selectedFiles.length === 0) {
            fileQueue.classList.add('hidden');
            actionBtn.disabled = true;
            return;
        }

        fileQueue.classList.remove('hidden');
        actionBtn.disabled = false;

        selectedFiles.forEach((file, index) => {
            const item = document.createElement('div');
            item.className = 'file-pill-item';
            item.innerHTML = `
                <span class="file-pill-name">📄 ${file.name}</span>
                <div class="file-pill-right">
                    <span class="file-pill-size">${formatBytes(file.size)}</span>
                    <button class="file-remove-x" type="button" data-index="${index}" title="Remove file">✕</button>
                </div>
            `;
            fileListContainer.appendChild(item);
        });

        fileListContainer.querySelectorAll('.file-remove-x').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const idx = parseInt(btn.dataset.index, 10);
                selectedFiles.splice(idx, 1);
                updateFileQueueUI();
            });
        });
    }

    function showError(heading, message) {
        document.getElementById('errorHeading').textContent = heading || 'Error';
        errorMessage.textContent = message || 'An unexpected error occurred.';
        errorBanner.classList.remove('hidden');
        animationStage.classList.add('hidden');
        actionBtn.disabled = false;
    }

    // ==========================================
    // 6. File Input & Drag and Drop
    // ==========================================
    dropzone.addEventListener('click', () => fileInput.click());
    dropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropzone.classList.add('dragover');
    });
    dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
    dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropzone.classList.remove('dragover');
        handleFiles(e.dataTransfer.files);
    });
    fileInput.addEventListener('change', (e) => {
        handleFiles(e.target.files);
    });
    addMoreFilesBtn.addEventListener('click', () => fileInput.click());

    function handleFiles(files) {
        if (!files || files.length === 0) return;
        errorBanner.classList.add('hidden');

        if (activeTool && activeTool.multiple) {
            for (let i = 0; i < files.length; i++) {
                selectedFiles.push(files[i]);
            }
        } else {
            selectedFiles = [files[0]];
        }
        updateFileQueueUI();
    }

    // ==========================================
    // 7. Action Execution & Real Processing
    // ==========================================
    actionBtn.addEventListener('click', async () => {
        if (!activeTool) return;
        const allowsTextOnly = (activeTool.id === 'text-to-pdf' || activeTool.id === 'markdown-to-pdf');

        if (selectedFiles.length === 0 && !allowsTextOnly) {
            showError('File required', 'Please select a document to proceed.');
            return;
        }

        errorBanner.classList.add('hidden');
        resultState.classList.add('hidden');

        // Activate purposeful animation stage
        stageMotionBox.innerHTML = getMotionMarkup(activeTool);
        animationStatusText.textContent = `Processing with ${activeTool.name}...`;
        animationStage.classList.remove('hidden');
        actionBtn.disabled = true;

        const formData = new FormData();

        if (activeTool.multiple) {
            selectedFiles.forEach(f => formData.append('files', f));
        } else if (selectedFiles.length > 0) {
            formData.append('file', selectedFiles[0]);
        }

        if (typeof activeTool.buildPayload === 'function') {
            activeTool.buildPayload(formData);
        }

        try {
            const response = await fetch(activeTool.endpoint, {
                method: 'POST',
                body: formData
            });

            const data = await response.json();

            if (!response.ok || !data.success) {
                showError('Process Error', data.message || 'Operation could not be completed.');
                return;
            }

            // Success state
            animationStage.classList.add('hidden');
            resultState.classList.remove('hidden');

            if (data.id) {
                downloadBtn.href = `/api/download/${data.id}`;
                downloadBtn.download = data.originalFilename || 'document';
                downloadBtn.classList.remove('hidden');
            } else {
                downloadBtn.classList.add('hidden');
            }

            // Comparison Metrics
            if (data.originalSize && data.compressedSize) {
                resultMetrics.classList.remove('hidden');
                statOriginal.textContent = formatBytes(data.originalSize);
                statResult.textContent = formatBytes(data.compressedSize);
                if (data.savingsPercent !== undefined && data.savingsPercent > 0) {
                    statSavingsCard.classList.remove('hidden');
                    statSavings.textContent = `${data.savingsPercent.toFixed(1)}%`;
                } else {
                    statSavingsCard.classList.add('hidden');
                }
            } else {
                resultMetrics.classList.add('hidden');
            }

            // Rich data viewer (metadata, text, AI)
            if (data.data) {
                richDataViewer.classList.remove('hidden');
                if (activeTool.id === 'inspect') {
                    viewerTitle.textContent = 'Document Metadata';
                    viewerContent.textContent = Object.entries(data.data)
                        .map(([k, v]) => `${k.padEnd(16, ' ')}: ${v}`)
                        .join('\n');
                } else if (activeTool.id === 'ai-summarize') {
                    viewerTitle.textContent = 'AI Summary';
                    viewerContent.textContent = data.data.summary || JSON.stringify(data.data, null, 2);
                } else if (activeTool.id === 'ai-chat') {
                    viewerTitle.textContent = 'Document Answer';
                    viewerContent.textContent = data.data.answer || JSON.stringify(data.data, null, 2);
                } else if (activeTool.id === 'ai-keypoints') {
                    viewerTitle.textContent = 'Key Points & Actions';
                    viewerContent.textContent = JSON.stringify(data.data, null, 2);
                } else if (data.data.text) {
                    viewerTitle.textContent = 'Extracted Text';
                    viewerContent.textContent = data.data.text;
                } else {
                    viewerContent.textContent = JSON.stringify(data.data, null, 2);
                }
            } else {
                richDataViewer.classList.add('hidden');
            }

        } catch (err) {
            showError('Connection Error', 'Could not reach server. Please check local connection.');
        }
    });

    // Copy to clipboard
    copyDataBtn.addEventListener('click', () => {
        navigator.clipboard.writeText(viewerContent.textContent)
            .then(() => {
                copyDataBtn.textContent = 'Copied!';
                setTimeout(() => copyDataBtn.textContent = 'Copy to clipboard', 2000);
            })
            .catch(() => alert('Could not copy text.'));
    });

    // Reset & Navigation listeners
    backToDashboardBtn.addEventListener('click', showDashboard);
    returnHomeBtn.addEventListener('click', showDashboard);
    processAnotherBtn.addEventListener('click', resetWorkspace);
    dismissErrorBtn.addEventListener('click', () => errorBanner.classList.add('hidden'));

    // Category tabs
    categoryTabs.querySelectorAll('.cat-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            categoryTabs.querySelectorAll('.cat-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentCategory = tab.dataset.category;
            renderToolsGrid(toolSearchInput.value, currentCategory);
        });
    });

    // Quick action chips in Hero
    document.querySelectorAll('.quick-chip').forEach(chip => {
        chip.addEventListener('click', (e) => {
            e.preventDefault();
            const tid = chip.dataset.tool;
            window.location.hash = tid;
        });
    });

    // Search input
    toolSearchInput.addEventListener('input', (e) => {
        renderToolsGrid(e.target.value, currentCategory);
    });

    clearSearchBtn.addEventListener('click', () => {
        toolSearchInput.value = '';
        renderToolsGrid('', currentCategory);
    });

    window.addEventListener('keydown', (e) => {
        if (e.key === '/' && document.activeElement !== toolSearchInput) {
            e.preventDefault();
            toolSearchInput.focus();
        }
    });

    // Privacy Modal Listeners
    function openPrivacyModal() {
        privacyModal.classList.remove('hidden');
    }
    function closePrivacyModal() {
        privacyModal.classList.add('hidden');
    }

    openPrivacyModalBtn.addEventListener('click', openPrivacyModal);
    footerPrivacyLink.addEventListener('click', (e) => {
        e.preventDefault();
        openPrivacyModal();
    });
    closePrivacyModalBtn.addEventListener('click', closePrivacyModal);
    dismissPrivacyModalBtn.addEventListener('click', closePrivacyModal);
    privacyModal.addEventListener('click', (e) => {
        if (e.target === privacyModal) closePrivacyModal();
    });

    // URL Hash Routing
    function handleHashChange() {
        const hash = window.location.hash.replace('#', '').trim();
        if (!hash || hash === 'dashboard') {
            showDashboard();
        } else {
            openTool(hash);
        }
    }

    window.addEventListener('hashchange', handleHashChange);

    // Initial render
    renderToolsGrid();
    if (window.location.hash && window.location.hash !== '#dashboard') {
        handleHashChange();
    }
})();
