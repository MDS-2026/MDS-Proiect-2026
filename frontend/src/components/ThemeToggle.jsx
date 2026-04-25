import React, { useEffect, useState } from 'react';

export default function ThemeToggle() {
  const [theme, setTheme] = useState(localStorage.getItem('fairpay_theme') || 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('fairpay_theme', theme);
  }, [theme]);

  return (
    <button 
      className="theme-toggle-fab" 
      onClick={() => setTheme(t => t === 'dark' ? 'light' : 'dark')}
      title={theme === 'dark' ? "Switch to Light Mode" : "Switch to Dark Mode"}
    >
      {theme === 'dark' ? '☀️' : '🌙'}
    </button>
  );
}
