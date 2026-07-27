import React from 'react';
import { motion } from 'framer-motion';
import './Hero.css';

const mailPilot = "mailto:founder@ignitionai.in?subject=Request%20for%20Early%20Access%20%E2%80%94%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI%20came%20across%20IgnitionAI%20and%20I%27m%20very%20interested%20in%20the%20early%20access%20pilot.%0A%0AHere%27s%20a%20bit%20about%20our%20workshop%3A%0A%0AWorkshop%20Name%3A%20%0ALocation%3A%20%0ANumber%20of%20Bays%3A%20%0A%0APlease%20let%20me%20know%20the%20next%20steps.%0A%0ARegards%2C";
const mailDemo  = "mailto:founder@ignitionai.in?subject=Demo%20Request%20%E2%80%94%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI%27d%20love%20to%20see%20a%20live%20demo%20of%20the%20IgnitionAI%20diagnostic%20platform.%0A%0AAvailability%3A%20%0APreferred%20mode%20(call%2Fin-person)%3A%20%0A%0ARegards%2C";
const mailPricing = "mailto:founder@ignitionai.in?subject=Pricing%20Enquiry%20%E2%80%94%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI%27m%20interested%20in%20learning%20more%20about%20IgnitionAI%20pricing%20for%20my%20workshop.%0A%0AWorkshop%20Name%3A%0ACity%3A%0ANumber%20of%20Bays%3A%0APhone%2FWhatsApp%3A%0A%0ALooking%20forward%20to%20hearing%20from%20you.";

const steps = [
  { label: 'Vehicle',    value: '2020 Hyundai Creta 1.5 CRDi', color: '#3b82f6' },
  { label: 'Symptom',   value: 'Crank No Start',                color: '#f97316' },
  { label: 'Confidence',value: '92% Match',                     color: '#10b981' },
];

const diagnosis = [
  { step: 1, active: true,  title: 'Check Fuel Rail Pressure', desc: 'Crank and observe PID — min. 250 Bar required.' },
  { step: 2, active: false, title: 'Verify CKP Sensor Signal',  desc: 'Check waveform at ECU pin 24 using oscilloscope.' },
  { step: 3, active: false, title: 'Inspect Injector Pulse',    desc: 'Use noid light on injector #1 harness connector.' },
];

export default function Hero() {
  return (
    <section className="hero">
      {/* Background glows */}
      <div className="hero-glow hero-glow--left" />
      <div className="hero-glow hero-glow--right" />

      {/* Big transparent logo watermark */}
      <div className="hero-logo-bg-wrap">
        <div className="hero-logo-bg-glow" />
        <img
          src="/logo_transparent_white.png"
          alt=""
          aria-hidden="true"
          className="hero-logo-bg"
        />
      </div>

      <div className="container hero-inner">
        {/* ─── Left copy ─── */}
        <motion.div
          className="hero-copy"
          initial={{ opacity: 0, y: 32 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.25, 0.46, 0.45, 0.94] }}
        >
          <motion.div
            className="hero-badge"
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.5, delay: 0.1 }}
          >
            <span className="badge-dot" />
            Now onboarding pilot workshops in India
          </motion.div>

          <h1 className="hero-title">
            The AI Diagnostic Assistant for{' '}
            <span className="text-gradient">No-Start &amp; Drivability</span>
          </h1>

          <p className="hero-subtitle">
            IgnitionAI gives technicians a structured, AI-powered path from symptom to root cause — built exclusively for mid-range passenger vehicles on Indian roads.
          </p>

          <div className="hero-ctas">
            <a href={mailPilot} className="btn btn-primary btn-lg">
              Request Early Access
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
            </a>
            <a href={mailDemo} className="btn btn-secondary btn-lg">
              Watch a Demo
            </a>
            <a href={mailPricing} className="btn btn-ghost btn-lg">
              Enquire Pricing
            </a>
          </div>

          <div className="hero-social-proof">
            <div className="sp-item">
              <strong>Cross-brand</strong>
              <span>Maruti, Hyundai, Tata &amp; more</span>
            </div>
            <div className="sp-divider" />
            <div className="sp-item">
              <strong>Bilingual</strong>
              <span>Hindi &amp; English</span>
            </div>
            <div className="sp-divider" />
            <div className="sp-item">
              <strong>Offline Ready</strong>
              <span>Works without internet</span>
            </div>
          </div>
        </motion.div>

        {/* ─── Right visual ─── */}
        <motion.div
          className="hero-visual animate-float"
          initial={{ opacity: 0, y: 32, scale: 0.96 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.9, delay: 0.25, ease: [0.25, 0.46, 0.45, 0.94] }}
        >
          <div className="app-card glass">
            <div className="app-card-header">
              <div className="app-dots">
                <span style={{ background: '#ff5f56' }}></span>
                <span style={{ background: '#ffbd2e' }}></span>
                <span style={{ background: '#27c93f' }}></span>
              </div>
              <span className="app-title-bar">IgnitionAI Diagnostic</span>
            </div>

            <div className="app-meta">
              {steps.map(s => (
                <div key={s.label} className="app-meta-item">
                  <span className="meta-label">{s.label}</span>
                  <span className="meta-value" style={{ color: s.color }}>{s.value}</span>
                </div>
              ))}
            </div>

            <div className="app-steps">
              {diagnosis.map((d) => (
                <motion.div
                  key={d.step}
                  className={`app-step ${d.active ? 'app-step--active' : ''}`}
                  initial={{ opacity: 0, x: -12 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ duration: 0.5, delay: 0.5 + d.step * 0.15 }}
                >
                  <div className={`step-num ${d.active ? 'step-num--active' : ''}`}>{d.step}</div>
                  <div className="step-body">
                    <p className="step-title">{d.title}</p>
                    <p className="step-desc">{d.desc}</p>
                  </div>
                  {d.active && <div className="step-badge">In Progress</div>}
                </motion.div>
              ))}
            </div>

            <div className="app-footer-bar">
              <div className="progress-bar">
                <motion.div
                  className="progress-fill"
                  initial={{ width: 0 }}
                  animate={{ width: '33%' }}
                  transition={{ duration: 1.2, delay: 1, ease: 'easeOut' }}
                />
              </div>
              <span>Step 1 of 3</span>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
