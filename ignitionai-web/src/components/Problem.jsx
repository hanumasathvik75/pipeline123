import React from 'react';
import { motion } from 'framer-motion';
import './Problem.css';

const problems = [
  {
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
      </svg>
    ),
    title: 'Hours Lost Per Job',
    body: 'Technicians spend 2–4 hours chasing electrical ghosts and intermittent symptoms before a single repair starts. Bays stay occupied, queues pile up.',
    color: '#3b82f6',
  },
  {
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/>
      </svg>
    ),
    title: '"Parts Cannoning"',
    body: 'Without a structured root-cause path, shops replace parts by guesswork. The vehicle comes back. The customer loses trust. The shop eats the cost.',
    color: '#f97316',
    highlight: true,
  },
  {
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <line x1="12" y1="20" x2="12" y2="10"/><line x1="18" y1="20" x2="18" y2="4"/><line x1="6" y1="20" x2="6" y2="16"/>
      </svg>
    ),
    title: 'Margin Erosion',
    body: 'Unbillable diagnostic hours, wasted parts, and repeat visits are silently killing workshop profitability across every mid-range vehicle segment.',
    color: '#10b981',
  },
];

const cardVariants = {
  hidden: { opacity: 0, y: 24 },
  visible: (i) => ({ opacity: 1, y: 0, transition: { duration: 0.6, delay: i * 0.15, ease: [0.25, 0.46, 0.45, 0.94] } }),
};

export default function Problem() {
  return (
    <section className="problem-section" id="problem">
      <div className="container">
        <motion.div
          className="section-header"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6 }}
        >
          <div className="section-label">The Problem</div>
          <h2 className="section-title">Diagnosis is the Bottleneck.<br />Not Repair.</h2>
          <p className="section-subtitle">
            Every no-start and drivability complaint arriving at your bay triggers the same expensive guessing game. IgnitionAI ends it.
          </p>
        </motion.div>

        <div className="problem-grid">
          {problems.map((p, i) => (
            <motion.div
              key={p.title}
              className={`problem-card glass ${p.highlight ? 'problem-card--highlight' : ''}`}
              custom={i}
              variants={cardVariants}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: '-50px' }}
              whileHover={{ y: -6, transition: { duration: 0.2 } }}
            >
              <div className="problem-icon" style={{ color: p.color, background: `${p.color}18`, borderColor: `${p.color}30` }}>
                {p.icon}
              </div>
              <h3>{p.title}</h3>
              <p>{p.body}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
