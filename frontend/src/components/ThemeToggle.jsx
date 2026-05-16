import React, { useEffect, useState } from 'react';

export default function ThemeToggle({ variant }) {
  const [theme, setTheme] = useState(localStorage.getItem('fairpay_theme') || 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('fairpay_theme', theme);
  }, [theme]);

  const toggleTheme = () => setTheme(t => t === 'dark' ? 'light' : 'dark');

  if (variant === 'sidebar') {
    return (
      <div className="theme-switch-wrapper">
        <span className="theme-switch-label">Theme</span>
        <label className="theme-switch" title={theme === 'dark' ? "Switch to Light Mode" : "Switch to Dark Mode"}>
          <input type="checkbox" checked={theme === 'dark'} onChange={toggleTheme} />
          <span className="theme-switch-slider">
            <span className="theme-switch-icon">☀️</span>
            <span className="theme-switch-icon">🌙</span>
          </span>
        </label>
      </div>
    );
  }

  return (
    <div style={{ position: 'absolute', top: 24, right: 32 }}>
      <label className="theme-switch" title={theme === 'dark' ? "Switch to Light Mode" : "Switch to Dark Mode"}>
        <input type="checkbox" checked={theme === 'dark'} onChange={toggleTheme} />
        <span className="theme-switch-slider">
          <span className="theme-switch-icon">☀️</span>
          <span className="theme-switch-icon">🌙</span>
        </span>
      </label>
    </div>
  );
}
