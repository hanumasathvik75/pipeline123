'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { rdStageLabel, getInitials } from '@/lib/utils';
import type { RDProject, Experiment, RDStage } from '@/lib/types';
import {
  FlaskConical, Cpu, Trophy, Sparkles, Plus, ArrowRight, BarChart3,
  FileCheck, Database, Sliders, CheckCircle2, ChevronRight, Star
} from 'lucide-react';

const STAGES: { stage: RDStage; label: string }[] = [
  { stage: 'hypothesis', label: 'Hypothesis' },
  { stage: 'methodology', label: 'Methodology' },
  { stage: 'dataset', label: 'Dataset' },
  { stage: 'model_dev', label: 'Model Dev' },
  { stage: 'results', label: 'Results' },
  { stage: 'published', label: 'Published' },
];

export default function RDWorkspacePage() {
  const { rdProjects, team, addExperiment } = useHQStore();
  const [selectedProjectId, setSelectedProjectId] = useState<string>(rdProjects[0]?.id || '');
  const [selectedExp, setSelectedExp] = useState<Experiment | null>(null);
  const [isLogModalOpen, setIsLogModalOpen] = useState(false);
  const [newExp, setNewExp] = useState({ model: '', dataset: '', result: '' });

  const currentProject = rdProjects.find(p => p.id === selectedProjectId) || rdProjects[0];
  const allExperiments = rdProjects.flatMap(p => p.experiments);
  const bestExperiments = allExperiments.filter(e => e.isBest);

  return (
    <div className="page animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">R&D & ML Workspace</h1>
          <p className="page-subtitle">
            Pioneering physical-AI algorithms, virtual sensors, synthetic driving telematics, and battery health degradation models.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setIsLogModalOpen(true)}>
          <Plus size={15} />
          Log Experiment
        </button>
      </div>

      {/* KPI Stats */}
      <div className="grid-stats">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(167, 139, 250, 0.12)', color: '#a78bfa' }}>
            <FlaskConical size={20} />
          </div>
          <div>
            <div className="stat-value">{rdProjects.length}</div>
            <div className="stat-label">Active Research Tracks</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.12)', color: '#38bdf8' }}>
            <Cpu size={20} />
          </div>
          <div>
            <div className="stat-value">{allExperiments.length}</div>
            <div className="stat-label">Total Model Experiments</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.12)', color: '#10b981' }}>
            <Trophy size={20} />
          </div>
          <div>
            <div className="stat-value">{bestExperiments.length}</div>
            <div className="stat-label">State-of-the-Art Benchmarks</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(245, 158, 11, 0.12)', color: '#f59e0b' }}>
            <BarChart3 size={20} />
          </div>
          <div>
            <div className="stat-value">R² 0.962</div>
            <div className="stat-label">Peak Virtual Sensor Fit</div>
          </div>
        </div>
      </div>

      {/* Project Selector Tabs */}
      <div style={{ display: 'flex', gap: '0.75rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>
        {rdProjects.map(proj => {
          const isActive = proj.id === selectedProjectId;
          return (
            <button
              key={proj.id}
              onClick={() => setSelectedProjectId(proj.id)}
              className="btn btn-sm"
              style={{
                background: isActive ? 'var(--accent-muted)' : 'transparent',
                color: isActive ? '#818cf8' : 'var(--text-secondary)',
                border: `1px solid ${isActive ? 'rgba(99,102,241,0.3)' : 'transparent'}`,
                fontWeight: isActive ? 600 : 500,
                fontSize: '0.85rem'
              }}
            >
              {proj.title}
            </button>
          );
        })}
      </div>

      {currentProject && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Research Track Overview Card */}
          <div className="card" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem', flexWrap: 'wrap', gap: '1rem' }}>
              <div>
                <span className="badge" style={{ background: 'rgba(167, 139, 250, 0.15)', color: '#a78bfa', marginBottom: '0.5rem' }}>
                  Track: {rdStageLabel(currentProject.stage)}
                </span>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {currentProject.title}
                </h2>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.35rem' }}>
                  <strong>Research Question:</strong> {currentProject.researchQuestion}
                </p>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Lead Scientist</div>
                  <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>
                    {team.find(m => m.id === currentProject.leadId)?.name || 'Lead'}
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Supervisor</div>
                  <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>
                    {team.find(m => m.id === currentProject.supervisorId)?.name || 'Supervisor'}
                  </div>
                </div>
              </div>
            </div>

            {/* Stage Progress Stepper */}
            <div style={{ marginTop: '1.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                <span>Scientific Pipeline Maturity</span>
                <span>{currentProject.progress}% complete</span>
              </div>
              <div className="progress-bar" style={{ height: 6, marginBottom: '1rem' }}>
                <div className="progress-fill" style={{ width: `${currentProject.progress}%`, background: 'linear-gradient(90deg, #6366f1, #a855f7)' }} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(6, 1fr)', gap: '0.5rem' }}>
                {STAGES.map((s, idx) => {
                  const currentIdx = STAGES.findIndex(x => x.stage === currentProject.stage);
                  const isCompleted = idx < currentIdx;
                  const isCurrent = idx === currentIdx;

                  return (
                    <div
                      key={s.stage}
                      style={{
                        padding: '0.5rem',
                        borderRadius: 'var(--radius)',
                        background: isCurrent ? 'rgba(99, 102, 241, 0.15)' : isCompleted ? 'var(--bg-elevated)' : 'transparent',
                        border: `1px solid ${isCurrent ? 'var(--accent)' : isCompleted ? 'rgba(16, 185, 129, 0.3)' : 'var(--border)'}`,
                        textAlign: 'center',
                      }}
                    >
                      <div style={{ fontSize: '0.65rem', color: isCompleted ? '#10b981' : isCurrent ? '#818cf8' : 'var(--text-muted)', fontWeight: 600 }}>
                        Step {idx + 1}
                      </div>
                      <div style={{ fontSize: '0.72rem', fontWeight: isCurrent ? 700 : 500, color: isCurrent ? '#fff' : 'var(--text-secondary)' }}>
                        {s.label}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Context details: hypothesis, dataset, methodology */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem', marginTop: '1.5rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border)' }}>
              {currentProject.dataset && (
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>
                    <Database size={13} />
                    <span>Dataset Reference</span>
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-primary)' }}>{currentProject.dataset}</div>
                </div>
              )}
              {currentProject.methodology && (
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>
                    <Sliders size={13} />
                    <span>Methodology</span>
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-primary)' }}>{currentProject.methodology}</div>
                </div>
              )}
            </div>
          </div>

          {/* Experiment Tracker Table */}
          <div className="card" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  Model Benchmarks & Experiment Runs ({currentProject.experiments.length})
                </h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Sorted chronological experiment evaluations with RMSE, R² scores, and hyperparameters.
                </p>
              </div>
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table className="data-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    <th style={{ textAlign: 'left', padding: '0.75rem' }}>Run #</th>
                    <th style={{ textAlign: 'left', padding: '0.75rem' }}>Model Architecture</th>
                    <th style={{ textAlign: 'left', padding: '0.75rem' }}>Features / Signals</th>
                    <th style={{ textAlign: 'right', padding: '0.75rem' }}>RMSE</th>
                    <th style={{ textAlign: 'right', padding: '0.75rem' }}>R² Score</th>
                    <th style={{ textAlign: 'left', padding: '0.75rem' }}>Status / Verdict</th>
                    <th style={{ textAlign: 'right', padding: '0.75rem' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {currentProject.experiments.map(exp => (
                    <tr
                      key={exp.id}
                      style={{
                        borderBottom: '1px solid var(--border)',
                        background: exp.isBest ? 'rgba(16, 185, 129, 0.06)' : 'transparent',
                      }}
                    >
                      <td style={{ padding: '0.75rem', fontWeight: 700, fontSize: '0.85rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <span>#{exp.number}</span>
                          {exp.isBest && (
                            <span className="badge" style={{ background: 'rgba(16, 185, 129, 0.2)', color: '#10b981', fontSize: '0.65rem', border: '1px solid rgba(16, 185, 129, 0.4)', display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                              <Star size={10} fill="#10b981" /> Best SOTA
                            </span>
                          )}
                        </div>
                      </td>
                      <td style={{ padding: '0.75rem', fontWeight: 600, fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                        {exp.model}
                      </td>
                      <td style={{ padding: '0.75rem', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.25rem', maxWidth: 280 }}>
                          {exp.features.map((f, i) => (
                            <span key={i} className="badge" style={{ fontSize: '0.65rem' }}>{f}</span>
                          ))}
                        </div>
                      </td>
                      <td style={{ padding: '0.75rem', textAlign: 'right', fontFamily: 'monospace', fontSize: '0.85rem', fontWeight: 600, color: exp.isBest ? '#10b981' : 'var(--text-secondary)' }}>
                        {exp.rmse !== undefined ? exp.rmse.toFixed(4) : '—'}
                      </td>
                      <td style={{ padding: '0.75rem', textAlign: 'right', fontFamily: 'monospace', fontSize: '0.85rem', fontWeight: 600, color: exp.isBest ? '#10b981' : 'var(--text-secondary)' }}>
                        {exp.r2 !== undefined ? exp.r2.toFixed(4) : '—'}
                      </td>
                      <td style={{ padding: '0.75rem', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                        {exp.result}
                      </td>
                      <td style={{ padding: '0.75rem', textAlign: 'right' }}>
                        <button
                          onClick={() => setSelectedExp(exp)}
                          className="btn btn-sm btn-secondary"
                          style={{ fontSize: '0.7rem', padding: '0.25rem 0.5rem' }}
                        >
                          Details
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Parameter Detail Modal */}
      {selectedExp && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 500, padding: '1.5rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>
                Run #{selectedExp.number}: {selectedExp.model}
              </h3>
              {selectedExp.isBest && (
                <span className="badge" style={{ background: 'rgba(16, 185, 129, 0.2)', color: '#10b981' }}>
                  Best Model
                </span>
              )}
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.25rem', fontSize: '0.8rem' }}>
              <div>
                <strong>Dataset:</strong> {selectedExp.dataset}
              </div>
              <div>
                <strong>Verdict:</strong> {selectedExp.result}
              </div>
              <div>
                <strong>Hyperparameters:</strong>
                <pre style={{ background: 'var(--bg-elevated)', padding: '0.75rem', borderRadius: 'var(--radius)', marginTop: '0.35rem', overflowX: 'auto', fontSize: '0.75rem' }}>
                  {JSON.stringify(selectedExp.parameters, null, 2)}
                </pre>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button className="btn btn-secondary" onClick={() => setSelectedExp(null)}>Close</button>
            </div>
          </div>
        </div>
      )}

      {/* Log Experiment Modal */}
      {isLogModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 450, padding: '1.5rem', background: 'var(--bg-surface)' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Log New Experiment</h3>
            
            <form onSubmit={(e) => { 
              e.preventDefault(); 
              if (selectedProjectId) {
                addExperiment(selectedProjectId, {
                  model: newExp.model,
                  dataset: newExp.dataset,
                  result: newExp.result,
                  features: ['Custom'],
                  parameters: { epochs: 100, batch_size: 32 },
                  isBest: false
                });
              }
              setIsLogModalOpen(false); 
              setNewExp({ model: '', dataset: '', result: '' }); 
            }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Model Architecture</label>
                <input className="input" placeholder="e.g. LSTM-Transformer v4" required value={newExp.model} onChange={e => setNewExp({...newExp, model: e.target.value})} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Dataset Used</label>
                <input className="input" placeholder="e.g. Battery Cycle 400x" required value={newExp.dataset} onChange={e => setNewExp({...newExp, dataset: e.target.value})} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Early Results / Verdict</label>
                <input className="input" placeholder="e.g. Shows 4% better RMSE" required value={newExp.result} onChange={e => setNewExp({...newExp, result: e.target.value})} />
              </div>
              
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                <button type="button" className="btn btn-ghost" onClick={() => setIsLogModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Experiment</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
