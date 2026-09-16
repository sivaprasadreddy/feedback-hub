(() => {
    const storageKey = 'feedback-hub-theme';
    const root = document.documentElement;
    const systemTheme = window.matchMedia('(prefers-color-scheme: dark)');

    function storedTheme() {
        try {
            return localStorage.getItem(storageKey);
        } catch {
            return null;
        }
    }

    function applyTheme(dark) {
        root.classList.toggle('dark', dark);
        root.dataset.theme = dark ? 'dark' : 'light';
        root.style.colorScheme = dark ? 'dark' : 'light';

        document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
            const nextTheme = dark ? 'light' : 'dark';
            const label = `Switch to ${nextTheme} mode`;
            button.setAttribute('aria-label', label);
            button.setAttribute('title', label);
        });
    }

    window.toggleTheme = () => {
        const dark = !root.classList.contains('dark');
        try {
            localStorage.setItem(storageKey, dark ? 'dark' : 'light');
        } catch {
            // The selected theme still applies for this page when storage is unavailable.
        }
        applyTheme(dark);
    };

    applyTheme(storedTheme() === 'dark' || (storedTheme() === null && systemTheme.matches));

    document.addEventListener('DOMContentLoaded', () => {
        applyTheme(root.classList.contains('dark'));
    });

    systemTheme.addEventListener('change', (event) => {
        if (storedTheme() === null) {
            applyTheme(event.matches);
        }
    });
})();
