'use client';

import { BookOpen, FileText, Search, ExternalLink, Code2, ShieldAlert, Cpu } from 'lucide-react';
import { useState } from 'react';

const DOCS = [
  {
    category: 'Architecture & Engineering',
    items: [
      { title: 'Edge Telematics Agent v2 Protocol', desc: 'Specification for J1939 and OBD-II CAN frame ingestion and virtual sensor inference.', readTime: '6 min read' },
      { title: 'IgnitionAI Schema & Knowledge Graph Spec', desc: 'Ontology of subsystems, DTC error codes, and diagnostic symptoms.', readTime: '10 min read' },
      { title: 'Real-time WebSocket Streaming Pipeline', desc: 'Sub-100ms latency telemetry broadcaster connecting edge vehicles to IgnitionAI Cloud.', readTime: '5 min read' },
    ]
  },
  {
    category: 'R&D & Machine Learning',
    items: [
      { title: 'Battery Degradation PINN Architecture', desc: 'Physics-informed neural networks integrating electrochemical diffusion with telemetry time-series.', readTime: '12 min read' },
      { title: 'Synthetic Telematics Generation Guide', desc: 'Generating anomalous driving cycles using generative models for edge test validation.', readTime: '8 min read' },
    ]
  },
  {
    category: 'Company & Business Operations',
    items: [
      { title: 'IgnitionAI Seed Investor Deck v2.4 Walkthrough', desc: 'Thesis, market TAM, OEM traction slides, and financial model explanations.', readTime: '4 min read' },
      { title: 'OEM Pilot Agreement Template & SLA', desc: 'Standard terms for 60-day hardware telematics pilot deployments.', readTime: '7 min read' },
      { title: 'New Engineer & Intern Onboarding Playbook', desc: 'Local environment setup, monorepo architecture, coding standards, and deployment keys.', readTime: '15 min read' },
    ]
  }
];

export default function WikiPage() {
  const [search, setSearch] = useState('');

  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Documentation & Knowledge Base</h1>
          <p className="page-subtitle">
            Central repository for engineering architecture, R&D papers, OEM deployment SOPs, and company playbooks.
          </p>
        </div>
      </div>

      <div style={{ position: 'relative', maxWidth: 400, marginBottom: '1.5rem' }}>
        <Search size={13} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
        <input
          className="input"
          placeholder="Search technical specs, decks, guides…"
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ paddingLeft: '2.25rem', height: 36, fontSize: '0.825rem' }}
        />
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
        {DOCS.map((cat, idx) => (
          <div key={idx}>
            <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.75rem' }}>
              {cat.category}
            </h3>
            <div className="grid-auto">
              {cat.items
                .filter(item => item.title.toLowerCase().includes(search.toLowerCase()) || item.desc.toLowerCase().includes(search.toLowerCase()))
                .map((doc, i) => (
                  <div
                    key={i}
                    className="card"
                    style={{
                      padding: '1.25rem',
                      cursor: 'pointer',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '0.5rem',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <FileText size={18} style={{ color: '#818cf8' }} />
                      <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>{doc.readTime}</span>
                    </div>

                    <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)', lineHeight: 1.3 }}>
                      {doc.title}
                    </h4>

                    <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                      {doc.desc}
                    </p>
                  </div>
                ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
