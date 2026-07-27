import React from 'react';
import { motion } from 'framer-motion';
import './Solution.css';

const mailDemo = "mailto:founder@ignitionai.in?subject=Demo%20Request%20-%20IgnitionAI&body=Hi%20Shashank%2C%0A%0AI'd%20love%20to%20see%20a%20live%20demo%20of%20the%20IgnitionAI%20diagnostic%20platform.%0A%0AAvailability%3A%20%0APreferred%20mode%20(call%2Fin-person)%3A%20%0A%0ARegards%2C";

const steps = [
  { num: '01', title: 'Input the Symptom', body: 'Technician describes the complaint — in English or Hindi — via text or voice. The AI immediately narrows the diagnostic scope.' },
  { num: '02', title: 'Follow the Guided Path', body: 'IgnitionAI presents a ranked, vehicle-specific decision tree. No guessing. Each step is grounded in real-world diagnostic logic for Indian market vehicles.' },
  { num: '03', title: 'Confirm Root Cause', body: 'The system confirms a precise fault with confidence scoring, part numbers, and labor time estimates before a single part is ordered.' },
];

export default function Solution() {
  return (
    <section className="solution-section" id="solution">
      <div className="container">
        <div className="solution-inner">
          {/* Left */}
          <motion.div
            className="solution-copy"
            initial={{ opacity: 0, x: -30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true, margin: '-80px' }}
            transition={{ duration: 0.7, ease: [0.25, 0.46, 0.45, 0.94] }}
          >
            <div className="section-label">How It Works</div>
            <h2 className="section-title" style={{ textAlign: 'left' }}>From Symptom to Root Cause in Minutes</h2>
            <p className="solution-desc">
              IgnitionAI doesn't replace your technician — it makes them dramatically faster. A structured diagnostic co-pilot that runs on any device, anywhere in the bay.
            </p>

            <div className="solution-steps">
              {steps.map((s, i) => (
                <motion.div
                  key={s.num}
                  className="solution-step"
                  initial={{ opacity: 0, x: -20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ duration: 0.5, delay: i * 0.15 }}
                >
                  <div className="step-number-badge">{s.num}</div>
                  <div>
                    <h4>{s.title}</h4>
                    <p>{s.body}</p>
                  </div>
                </motion.div>
              ))}
            </div>

            <a href={mailDemo} className="btn btn-primary" style={{ marginTop: '2.5rem' }}>
              Request a Demo
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </a>
          </motion.div>

          {/* Right visual */}
          <motion.div
            className="solution-visual"
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true, margin: '-80px' }}
            transition={{ duration: 0.7, delay: 0.2, ease: [0.25, 0.46, 0.45, 0.94] }}
          >
            <div className="glass solution-card">
              <div className="solution-card-label">Live Diagnostic Session</div>
              <div className="vehicle-tag">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
                2019 Maruti Suzuki Swift 1.2 VXi
              </div>

              <div className="flow-container">
                <div className="flow-node flow-node--input">
                  <div className="flow-icon">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="22"/></svg>
                  </div>
                  <div>
                    <span className="flow-tag">Voice Input</span>
                    <p>"Gaadi start nahi ho rahi subah se"</p>
                  </div>
                </div>

                <div className="flow-arrow">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 19 5 12"/></svg>
                </div>

                <div className="flow-node flow-node--active">
                  <div className="flow-icon flow-icon--blue">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M4.93 4.93a10 10 0 0 0 0 14.14"/></svg>
                  </div>
                  <div>
                    <span className="flow-tag flow-tag--blue">AI Analysis</span>
                    <p>Mapped to: <strong>No Crank / Weak Crank</strong></p>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>14 possible causes identified</p>
                  </div>
                </div>

                <div className="flow-arrow">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 19 5 12"/></svg>
                </div>

                <div className="flow-node flow-node--result">
                  <div className="flow-icon flow-icon--green">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                  <div>
                    <span className="flow-tag flow-tag--green">Root Cause Found</span>
                    <p><strong>Battery terminal corrosion</strong></p>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>Confidence: 87% · Est. fix: 25 min</p>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
