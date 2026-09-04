'use client';

import { Target, CheckCircle2, TrendingUp, Award, Calendar } from 'lucide-react';

const OKRS = [
  {
    quarter: 'Q3 2026 (Current)',
    objective: 'Production Validation & Fleet Telematics Pilot Deployments',
    progress: 68,
    keyResults: [
      { kr: 'Deploy physical virtual sensor edge agent on 2 commercial EV bus testbeds', progress: 80, target: '2 fleets', current: '1 fleet active, 1 in config' },
      { kr: 'Achieve battery state-of-health estimation R² > 0.95 on real-world duty cycles', progress: 92, target: 'R² 0.95', current: 'R² 0.962 SOTA' },
      { kr: 'Sign 3 paid PoC / LOI agreements with Indian tier-1 automotive suppliers', progress: 66, target: '3 LOIs', current: '2 signed (Ashok Leyland ecosystem, Tata AutoComp)' },
      { kr: 'Close $750K Pre-Seed equity / grant funding round', progress: 40, target: '$750K', current: '$200K soft committed' },
    ]
  },
  {
    quarter: 'Q4 2026 (Upcoming)',
    objective: 'Scalable Telematics Cloud Platform & Enterprise OEM Integration',
    progress: 15,
    keyResults: [
      { kr: 'Complete multi-tenant high-throughput CAN bus streaming pipeline (100k msg/s)', progress: 20, target: '100k msg/s', current: 'Benchmarked at 25k msg/s' },
      { kr: 'Publish 1 peer-reviewed research paper on Physics-Informed Battery Degradation', progress: 30, target: '1 paper', current: 'Draft in review' },
      { kr: 'Expand engineering & ML team to 10 full-time members', progress: 0, target: '10 team', current: 'Hiring pipeline open' },
    ]
  }
];

export default function GoalsPage() {
  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Company Goals & OKRs</h1>
          <p className="page-subtitle">
            Strategic North Star metrics, quarterly objectives, and key results aligning the entire IgnitionAI team.
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {OKRS.map((group, idx) => (
          <div key={idx} className="card" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
              <div>
                <span className="badge" style={{ background: 'rgba(99,102,241,0.15)', color: '#818cf8', marginBottom: '0.4rem' }}>
                  {group.quarter}
                </span>
                <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {group.objective}
                </h2>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--accent)' }}>
                  {group.progress}%
                </span>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Aggregate Progress</div>
              </div>
            </div>

            <div className="progress-bar" style={{ height: 6, marginBottom: '1.25rem' }}>
              <div className="progress-fill" style={{ width: `${group.progress}%` }} />
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {group.keyResults.map((kr, i) => (
                <div
                  key={i}
                  style={{
                    padding: '1rem',
                    borderRadius: 'var(--radius)',
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                    <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)', maxWidth: '80%' }}>
                      {kr.kr}
                    </div>
                    <span style={{ fontSize: '0.8rem', fontWeight: 700, color: kr.progress >= 80 ? '#10b981' : '#f59e0b' }}>
                      {kr.progress}%
                    </span>
                  </div>

                  <div className="progress-bar" style={{ height: 4, marginBottom: '0.5rem' }}>
                    <div
                      className="progress-fill"
                      style={{
                        width: `${kr.progress}%`,
                        background: kr.progress >= 80 ? '#10b981' : '#f59e0b',
                      }}
                    />
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                    <span>Target: {kr.target}</span>
                    <span>Status: {kr.current}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
