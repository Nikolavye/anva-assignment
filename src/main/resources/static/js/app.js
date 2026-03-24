const BASE_URL = '/api/v1/word-frequency';

document.addEventListener('DOMContentLoaded', () => {

    // UI Elements
    const tabs = document.querySelectorAll('.tab');
    const specificWordInput = document.getElementById('specificWordInput');
    const topNInput = document.getElementById('topNInput');
    const analyzeBtn = document.getElementById('analyzeBtn');
    const textInput = document.getElementById('textInput');

    // Result Elements
    const resultSection = document.getElementById('resultSection');
    const operationLabel = document.getElementById('operationLabel');
    const singleResultBox = document.getElementById('singleResultBox');
    const singleResultValue = document.getElementById('singleResultValue');
    const listResultBox = document.getElementById('listResultBox');
    const resultList = document.getElementById('resultList');

    // History Elements
    const historyList = document.getElementById('historyList');
    const refreshHistoryBtn = document.getElementById('refreshHistoryBtn');
    const deleteAllHistoryBtn = document.getElementById('deleteAllHistoryBtn');

    let currentOperation = 'highest';

    // Tab Switching Logic
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentOperation = tab.getAttribute('data-op');

            // Hide all conditional inputs
            specificWordInput.classList.add('hidden');
            topNInput.classList.add('hidden');

            // Show appropriate input
            if (currentOperation === 'specific') {
                specificWordInput.classList.remove('hidden');
            } else if (currentOperation === 'top-n') {
                topNInput.classList.remove('hidden');
            }
        });
    });

    // Run Analysis action
    analyzeBtn.addEventListener('click', async () => {
        const text = textInput.value;
        if (!text.trim()) {
            alert("Please enter some text!");
            return;
        }

        // Add loading state
        const originalBtnText = analyzeBtn.innerHTML;
        analyzeBtn.innerHTML = '<i class="lucide-loader"></i> Processing...';
        analyzeBtn.style.opacity = '0.7';

        try {
            let endpoint = '';
            let payload = { text: text };

            if (currentOperation === 'highest') {
                endpoint = '/highest';
                operationLabel.textContent = "Highest Frequency Found";
            } else if (currentOperation === 'specific') {
                endpoint = '/frequency-for-word';
                const word = document.getElementById('wordInput').value;
                if (!word) { alert("Please enter a target word"); return; }
                payload.word = word;
                operationLabel.textContent = `Frequency for "${word}"`;
            } else if (currentOperation === 'top-n') {
                endpoint = '/most-frequent-n';
                const n = document.getElementById('nInput').value;
                payload.n = parseInt(n) || 3;
                operationLabel.textContent = `Top ${payload.n} Words`;
            }

            const response = await fetch(BASE_URL + endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) throw new Error("Server Error");

            const data = await response.json();
            displayResult(data, currentOperation);
            loadHistory(); // Refresh history automatically

        } catch (error) {
            console.error(error);
            alert("An error occurred while analyzing the text.");
        } finally {
            // Restore button state
            analyzeBtn.innerHTML = originalBtnText;
            analyzeBtn.style.opacity = '1';
            lucide.createIcons();
        }
    });

    // Display Logic
    function displayResult(data, operation) {
        resultSection.classList.remove('hidden');
        singleResultBox.classList.add('hidden');
        listResultBox.classList.add('hidden');

        if (operation === 'highest' || operation === 'specific') {
            singleResultBox.classList.remove('hidden');

            // Check if top word is in operation text
            const wordBox = document.getElementById('singleResultWordBox');
            const wordElem = document.getElementById('singleResultWord');

            if (operation === 'highest' && data.operation && data.operation.includes('Top word:')) {
                let match = data.operation.match(/\(Top word: (.*)\)/);
                if (match && match[1]) {
                    wordElem.textContent = match[1];
                    wordBox.classList.remove('hidden');
                } else {
                    wordBox.classList.add('hidden');
                }
            } else if (wordBox) {
                wordBox.classList.add('hidden');
            }

            // Animate number counting up
            animateValue(singleResultValue, 0, data.singleResult, 1000);
        } else if (operation === 'top-n') {
            listResultBox.classList.remove('hidden');
            resultList.innerHTML = '';

            data.results.forEach(item => {
                const li = document.createElement('li');
                li.className = 'word-item slide-in';
                li.innerHTML = `
                    <span class="word-text">${item.word}</span>
                    <span class="word-count">${item.frequency}</span>
                `;
                resultList.appendChild(li);
            });
        }
    }

    // Load History
    async function loadHistory() {
        try {
            const response = await fetch(BASE_URL + '/history');
            if (!response.ok) return;
            const history = await response.json();

            historyList.innerHTML = '';
            // Sort by newest first
            history.reverse().forEach(record => {
                const date = new Date(record.timestamp).toLocaleString();
                const li = document.createElement('li');
                li.className = 'history-item';

                let resultText = "";
                if (record.results && record.results.length > 0) {
                    resultText = "Found " + record.results.length + " words";
                } else {
                    resultText = "Result: " + record.singleResult;
                }

                li.innerHTML = `
                    <div class="history-content">
                        <div class="history-op">${record.operation} &bull; <span style="color:var(--text-main)">${resultText}</span></div>
                        <div class="history-text">"${record.text}"</div>
                        <div class="history-time"><i data-lucide="clock" style="width:12px;height:12px"></i> ${date}</div>
                    </div>
                    <button class="delete-btn" data-id="${record.id}">
                        <i data-lucide="trash-2"></i>
                    </button>
                `;
                historyList.appendChild(li);
            });
            lucide.createIcons();

            // Re-bind delete buttons after rendering
            document.querySelectorAll('.delete-btn').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    // Prevent event from bubbling up to parents
                    e.stopPropagation();
                    const id = btn.getAttribute('data-id');
                    await deleteHistory(id);
                });
            });

        } catch (error) {
            console.error("Failed to load history", error);
        }
    }

    // Refresh Btn
    refreshHistoryBtn.addEventListener('click', () => {
        refreshHistoryBtn.classList.add('rotating');
        loadHistory().then(() => {
            setTimeout(() => refreshHistoryBtn.classList.remove('rotating'), 500);
        });
    });

    // Delete All History Btn
    if (deleteAllHistoryBtn) {
        deleteAllHistoryBtn.addEventListener('click', async () => {
            if (!confirm("Are you sure you want to delete ALL records?")) return;
            try {
                const response = await fetch(BASE_URL + '/history', { method: 'DELETE' });
                if (response.ok) {
                    loadHistory();
                } else {
                    alert("Failed to delete all records. Server returned: " + response.status);
                }
            } catch (error) {
                console.error(error);
                alert("Network error: Could not connect to the server.");
            }
        });
    }

    // Make delete globally available
    window.deleteHistory = async function (id) {
        if (!confirm("Delete this record?")) return;
        try {
            const response = await fetch(BASE_URL + `/history/${id}`, { method: 'DELETE' });
            if (response.ok) {
                loadHistory();
            } else {
                alert("Failed to delete record. Server returned: " + response.status);
            }
        } catch (e) {
            console.error(e);
            alert("Network error: Could not connect to the server.");
        }
    }


    function animateValue(obj, start, end, duration) {
        let startTimestamp = null;
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            obj.innerHTML = Math.floor(progress * (end - start) + start);
            if (progress < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    }

    // Initial load
    loadHistory();
});
