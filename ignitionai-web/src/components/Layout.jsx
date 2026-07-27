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

  const mailGeneral = "mailto:founder@ignitionai.in?subject=Inquiry%20%E2%80%94%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI'd%20like%20to%20learn%20more%20about%20IgnitionAI.%0A%0ARegards%2C";
  const mailPricing = "mailto:founder@ignitionai.in?subject=Pricing%20Enquiry%20%E2%80%94%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI%27m%20interested%20in%20learning%20more%20about%20IgnitionAI%20pricing%20for%20my%20workshop.%0A%0AWorkshop%20Name%3A%0ACity%3A%0ANumber%20of%20Bays%3A%0APhone%2FWhatsApp%3A%0A%0ALooking%20forward%20to%20hearing%20from%20you.";

  return (
    <div className="layout">
      {/* ─── Header ─── */}
      <header className={`header ${scrolled ? 'header--scrolled' : ''}`}>
        <div className="container header-inner">
          <a href="/" className="header-logo">
            <img src="/logo_transparent_white.png" alt="IgnitionAI" style={{ height: '34px', width: 'auto', filter: 'brightness(0) invert(1)', opacity: 0.9 }} />
          </a>

          <nav className={`header-nav ${menuOpen ? 'header-nav--open' : ''}`}>
            <a href="#problem" onClick={() => setMenuOpen(false)}>The Problem</a>
            <a href="#solution" onClick={() => setMenuOpen(false)}>How It Works</a>
            <a href="#features" onClick={() => setMenuOpen(false)}>Features</a>
            <a href="#contact" onClick={() => setMenuOpen(false)}>Contact</a>
          </nav>

          <div className="header-actions">
            <a href={mailPricing} className="btn btn-secondary" style={{ padding: '0.55rem 1.25rem', fontSize: '0.85rem', marginRight: '8px' }}>
              Enquire Pricing
            </a>
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
              <img src="/logo_transparent_white.png" alt="IgnitionAI" className="footer-logo" style={{ filter: 'brightness(0) invert(1)', opacity: 0.85 }} />
              <p>Building the fastest path from symptom to fix for mid-range passenger vehicles in India.</p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '1.2rem', fontSize: '13px' }}>
                <a href="tel:+918610815472" style={{ color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.62 3.38a2 2 0 0 1 2-2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                  +91 86108 15472
                </a>
                <a href="mailto:founder@ignitionai.in" style={{ color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                  founder@ignitionai.in
                </a>
              </div>
              <div style={{ display: 'flex', gap: '12px', marginTop: '1.2rem', flexWrap: 'wrap' }}>
                <a href={mailGeneral} className="btn btn-primary" style={{ width: 'fit-content', fontSize: '0.85rem', padding: '0.5rem 1.1rem' }}>Get in Touch</a>
                <a href={mailPricing} className="btn btn-secondary" style={{ width: 'fit-content', fontSize: '0.85rem', padding: '0.5rem 1.1rem' }}>Enquire Pricing</a>
              </div>
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
            <p>© {new Date().getFullYear()} IgnitionAI. All rights reserved. Made in India 🇮🇳</p>
            <span style={{ fontFamily: 'monospace', fontSize: '11px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', padding: '2px 8px', borderRadius: '3px', color: 'var(--text-muted)' }}>UDYAM-TN-02-0444115</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
