package dev.health;

public class IndexHtml {
    public static final String CONTENT = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Blood Work Analyzer</title>
    <!-- Google Fonts: Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <!-- Marked.js for markdown parsing -->
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <style>
        :root {
            --bg-base: #0f172a;       /* slate-900 */
            --bg-surface: #1e293b;    /* slate-800 */
            --bg-control: #334155;    /* slate-700 */
            --border-color: #475569;  /* slate-600 */
            --text-primary: #f8fafc;  /* slate-50 */
            --text-secondary: #cbd5e1;/* slate-300 */
            --text-muted: #94a3b8;     /* slate-400 */
            --primary: #6366f1;       /* indigo-500 */
            --primary-hover: #4f46e5; /* indigo-600 */
            --primary-glow: rgba(99, 102, 241, 0.4);
            --danger: #ef4444;        /* red-500 */
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', sans-serif;
            background-color: var(--bg-base);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            line-height: 1.5;
        }

        header {
            border-bottom: 1px solid var(--border-color);
            background-color: rgba(30, 41, 59, 0.7);
            backdrop-filter: blur(12px);
            position: sticky;
            top: 0;
            z-index: 50;
            padding: 1.25rem 2rem;
            display: flex;
            align-items: center;
        }

        header h1 {
            font-size: 1.5rem;
            font-weight: 700;
            background: linear-gradient(135deg, #a5b4fc, #818cf8);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        header h1::before {
            content: '🧬';
            font-size: 1.5rem;
        }

        main {
            flex: 1;
            max-width: 1400px;
            width: 100%;
            margin: 0 auto;
            padding: 2rem;
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 2rem;
        }

        @media (max-width: 900px) {
            main {
                grid-template-columns: 1fr;
            }
        }

        .panel {
            background-color: var(--bg-surface);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 2rem;
            display: flex;
            flex-direction: column;
            gap: 1.5rem;
            box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.3);
        }

        .panel-title {
            font-size: 1.25rem;
            font-weight: 600;
            color: var(--text-primary);
            border-bottom: 2px solid var(--border-color);
            padding-bottom: 0.5rem;
        }

        textarea {
            width: 100%;
            height: 400px;
            background-color: var(--bg-base);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            color: var(--text-primary);
            padding: 1rem;
            font-family: inherit;
            font-size: 0.95rem;
            resize: none;
            outline: none;
            transition: border-color 0.2s, box-shadow 0.2s;
        }

        textarea:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px var(--primary-glow);
        }

        button {
            width: 100%;
            background-color: var(--primary);
            color: white;
            border: none;
            border-radius: 8px;
            padding: 0.85rem;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s, transform 0.1s, box-shadow 0.2s;
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 0.5rem;
            outline: none;
        }

        button:hover {
            background-color: var(--primary-hover);
            box-shadow: 0 4px 12px var(--primary-glow);
        }

        button:active {
            transform: scale(0.98);
        }

        button:disabled {
            background-color: var(--bg-control);
            color: var(--text-muted);
            cursor: not-allowed;
            transform: none;
            box-shadow: none;
        }

        .scroll-box {
            height: 230px;
            overflow-y: auto;
            padding: 1rem 1.25rem;
            border: 1px solid var(--border-color);
            border-radius: 8px;
            background-color: var(--bg-base);
            font-size: 0.92rem;
            line-height: 1.6;
            color: var(--text-secondary);
        }

        .scroll-box h1, .scroll-box h2, .scroll-box h3 {
            color: var(--text-primary);
            margin-top: 1rem;
            margin-bottom: 0.5rem;
        }
        .scroll-box h1:first-child, .scroll-box h2:first-child, .scroll-box h3:first-child {
            margin-top: 0;
        }
        .scroll-box ul, .scroll-box ol {
            margin-left: 1.5rem;
            margin-bottom: 0.75rem;
        }
        .scroll-box p {
            margin-bottom: 0.75rem;
        }

        .spinner {
            border: 3px solid rgba(255, 255, 255, 0.1);
            width: 20px;
            height: 20px;
            border-radius: 50%;
            border-left-color: #fff;
            animation: spin 1s linear infinite;
            display: none;
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }

        .alert-error {
            background-color: rgba(239, 68, 68, 0.15);
            border: 1px solid var(--danger);
            color: #fca5a5;
            padding: 0.75rem 1rem;
            border-radius: 6px;
            font-size: 0.9rem;
            display: none;
        }
    </style>
</head>
<body>
    <header>
        <h1>Blood Work Analyzer</h1>
    </header>

    <main>
        <!-- Left Column: Input Panel -->
        <div class="panel">
            <h2 class="panel-title">Blood Work Report</h2>
            <div id="error-message" class="alert-error"></div>
            <textarea id="blood-report" placeholder="Paste your blood work report here..."></textarea>
            <button id="analyze-btn">
                <span class="spinner" id="analyze-spinner"></span>
                <span id="btn-text">Analyze</span>
            </button>
        </div>

        <!-- Right Column: Output Panel -->
        <div class="panel">
            <div>
                <h2 class="panel-title" style="margin-bottom: 0.75rem;">Health Summary</h2>
                <div class="scroll-box" id="health-summary"></div>
            </div>
            <div>
                <h2 class="panel-title" style="margin-bottom: 0.75rem;">Suggested Diet Plan</h2>
                <div class="scroll-box" id="diet-plan"></div>
            </div>
        </div>
    </main>

    <script>
        document.getElementById('analyze-btn').addEventListener('click', async () => {
            const reportText = document.getElementById('blood-report').value.trim();
            const errorDiv = document.getElementById('error-message');
            const analyzeBtn = document.getElementById('analyze-btn');
            const spinner = document.getElementById('analyze-spinner');
            const btnText = document.getElementById('btn-text');
            const summaryDiv = document.getElementById('health-summary');
            const dietDiv = document.getElementById('diet-plan');

            // Reset UI
            errorDiv.style.display = 'none';
            summaryDiv.innerHTML = '';
            dietDiv.innerHTML = '';

            if (!reportText) {
                errorDiv.innerText = 'Please paste a blood work report before analyzing.';
                errorDiv.style.display = 'block';
                return;
            }

            // Show loading state
            analyzeBtn.disabled = true;
            spinner.style.display = 'inline-block';
            btnText.innerText = 'Analyzing...';
            summaryDiv.innerHTML = '<p style="color: var(--text-muted);">Analyzing report data...</p>';
            dietDiv.innerHTML = '<p style="color: var(--text-muted);">Preparing custom diet recommendations...</p>';

            try {
                const response = await fetch('/analyze', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ 'blood_report': reportText })
                });

                if (!response.ok) {
                    throw new Error(`Server returned status: ${response.status}`);
                }

                const data = await response.json();
                
                if (data.error) {
                    throw new Error(data.error);
                }

                // Render response markdown to HTML using marked.js
                summaryDiv.innerHTML = marked.parse(data.healthSummary || '');
                dietDiv.innerHTML = marked.parse(data.dietPlan || '');

            } catch (err) {
                console.error(err);
                errorDiv.innerText = `An error occurred: ${err.message}`;
                errorDiv.style.display = 'block';
                summaryDiv.innerHTML = '';
                dietDiv.innerHTML = '';
            } finally {
                // Restore button state
                analyzeBtn.disabled = false;
                spinner.style.display = 'none';
                btnText.innerText = 'Analyze';
            }
        });
    </script>
</body>
</html>
""";
}
