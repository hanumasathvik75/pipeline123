import React, { useState, useEffect } from 'react';
import './Layout.css';

export default function Layout({ children }) {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const mailGeneral = "mailto:founder@ignitionai.in?subject=Inquiry%20-%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI'd%20like%20to%20learn%20more%20about%20IgnitionAI.%0A%0ARegards%2C";

  return (
    <div className="layout">
      {/* ─── Header ─── */}
      <header className={`header ${scrolled ? 'header--scrolled' : ''}`}>
        <div className="container header-inner">
          <a href="/" className="header-logo">
            <img src="/logo_transparent.png" alt="IgnitionAI" />
          </a>

          <nav className={`header-nav ${menuOpen ? 'header-nav--open' : ''}`}>
            <a href="#problem" onClick={() => setMenuOpen(false)}>The Problem</a>
            <a href="#solution" onClick={() => setMenuOpen(false)}>How It Works</a>
            <a href="#features" onClick={() => setMenuOpen(false)}>Features</a>
            <a href="#contact" onClick={() => setMenuOpen(false)}>Contact</a>
          </nav>

          <div className="header-actions">
            <a href={mailGeneral} className="btn btn-primary" style={{ padding: '0.55rem 1.25rem', fontSize: '0.9rem' }}>
              Request Access
            </a>
            <button
              className={`hamburger ${menuOpen ? 'hamburger--open' : ''}`}
              onClick={() => setMenuOpen(!menuOpen)}
              aria-label="Toggle menu"
            >
              <span></span>
              <span></span>
              <span></span>
            </button>
          </div>
        </div>
      </header>

      {/* Mobile overlay */}
      {menuOpen && <div className="mobile-overlay" onClick={() => setMenuOpen(false)} />}

      <main>{children}</main>

      <div className="divider" />

      {/* ─── Footer ─── */}
      <footer className="footer" id="contact">
        <div className="container">
          <div className="footer-top">
            <div className="footer-brand">
              <img src="/logo_transparent.png" alt="IgnitionAI" className="footer-logo" />
              <p>Building the fastest path from symptom to fix for mid-range passenger vehicles in India.</p>
              <a href={mailGeneral} className="btn btn-primary" style={{ marginTop: '1.5rem', width: 'fit-content' }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
                Get in Touch
              </a>
            </div>

            <div className="footer-links">
              <div className="footer-col">
                <h4>Navigate</h4>
                <a href="#problem">The Problem</a>
                <a href="#solution">How It Works</a>
                <a href="#features">Features</a>
              </div>
              <div className="footer-col">
                <h4>Contact</h4>
                <div className="founder-card">
                  <div className="founder-avatar">S</div>
                  <div>
                    <strong>Shashank</strong>
                    <span>Founder & CEO</span>
                  </div>
                </div>
                <a href="mailto:founder@ignitionai.in" className="founder-email">
                  founder@ignitionai.in
                </a>
              </div>
              <div className="footer-col">
                <h4>Legal</h4>
                <a href="mailto:founder@ignitionai.in?subject=Privacy%20Policy%20Inquiry">Privacy Policy</a>
                <a href="mailto:founder@ignitionai.in?subject=Terms%20of%20Service%20Inquiry">Terms of Service</a>
              </div>
            </div>
          </div>

          <div className="footer-bottom">
            <p>© {new Date().getFullYear()} IgnitionAI. All rights reserved.</p>
            <p>Made in India 🇮🇳</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
