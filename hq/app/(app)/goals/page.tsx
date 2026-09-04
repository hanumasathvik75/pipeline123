'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { Target, Plus, CheckCircle2, TrendingUp, Award, Calendar, Edit2, Trash2, X, PlusCircle, MinusCircle } from 'lucide-react';
import type { Goal, KeyResult } from '@/lib/types';

export default function GoalsPage() {
  const { goals, currentUser, addGoal, updateGoal, deleteGoal } = useHQStore();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingGoal, setEditingGoal] = useState<Goal | null>(null);

  // Form state
  const [quarter, setQuarter] = useState('');
  const [objective, setObjective] = useState('');
  const [progress, setProgress] = useState(0);
  const [keyResults, setKeyResults] = useState<KeyResult[]>([
      { id: `kr-${Date.now()}`, title: '', target: '', current: '', progress: 0 }
  ]);

  const isAdmin = currentUser?.role === 'c_level';

  const openNewModal = () => {
    setQuarter('');
    setObjective('');
    setProgress(0);
    setKeyResults([{ id: `kr-${Date.now()}`, title: '', target: '', current: '', progress: 0 }]);
    setEditingGoal(null);
    setIsModalOpen(true);
  };

  const openEditModal = (g: Goal) => {
    setQuarter(g.quarter);
    setObjective(g.objective);
    setProgress(g.progress);
    setKeyResults(g.keyResults && g.keyResults.length > 0 ? [...g.keyResults] : [{ id: `kr-${Date.now()}`, title: '', target: '', current: '', progress: 0 }]);
    setEditingGoal(g);
    setIsModalOpen(true);
  };

  const handleDelete = (id: string) => {
      if(window.confirm('Are you sure you want to delete this Goal/OKR?')) {
          deleteGoal(id);
      }
  };

  const handleAddKR = () => {
      setKeyResults([...keyResults, { id: `kr-${Date.now()}`, title: '', target: '', current: '', progress: 0 }]);
  };

  const handleRemoveKR = (index: number) => {
      if(keyResults.length > 1) {
          setKeyResults(keyResults.filter((_, i) => i !== index));
      }
  };

  const updateKR = (index: number, field: keyof KeyResult, value: string | number) => {
      const updated = [...keyResults];
      updated[index] = { ...updated[index], [field]: value };
      setKeyResults(updated);
  };

  const handleSubmit = (e: React.FormEvent) => {
      e.preventDefault();
      if(!quarter.trim() || !objective.trim()) return;

      const validKRs = keyResults.filter(kr => kr.title.trim() !== '');

      if(editingGoal) {
          updateGoal(editingGoal.id, {
              quarter: quarter.trim(),
              objective: objective.trim(),
              progress,
              keyResults: validKRs
          });
      } else {
          addGoal({
              quarter: quarter.trim(),
              objective: objective.trim(),
              progress,
              keyResults: validKRs,
              createdBy: currentUser?.id || 'sys'
          });
      }
      setIsModalOpen(false);
  };

  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Company Goals & OKRs</h1>
          <p className="page-subtitle">
            Strategic North Star metrics, quarterly objectives, and key results aligning the entire IgnitionAI team.
          </p>
        </div>
        {isAdmin && (
            <button className="btn btn-primary" onClick={openNewModal}>
            <Plus size={15} />
            New Goal
            </button>
        )}
      </div>

      {goals.length === 0 ? (
          <div className="card" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
              <Target size={48} style={{ opacity: 0.2, margin: '0 auto 1rem' }} />
              <h3>No Goals Set</h3>
              <p>Add your first OKR to start tracking progress.</p>
          </div>
      ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {goals.map((group) => (
              <div key={group.id} className="card hover-effect" style={{ padding: '1.5rem', position: 'relative' }}>
                {isAdmin && (
                    <div style={{ position: 'absolute', top: '1.25rem', right: '1.25rem', display: 'flex', gap: '0.25rem' }}>
                        <button className="btn btn-icon btn-ghost btn-sm" onClick={() => openEditModal(group)}>
                            <Edit2 size={16} />
                        </button>
                        <button className="btn btn-icon btn-ghost btn-sm" onClick={() => handleDelete(group.id)} style={{ color: '#ef4444' }}>
                            <Trash2 size={16} />
                        </button>
                    </div>
                )}
                
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem', paddingRight: '4rem' }}>
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
                  <div className="progress-fill" style={{ width: `${group.progress}%`, background: group.progress >= 80 ? '#10b981' : 'var(--accent)' }} />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {group.keyResults && group.keyResults.map((kr, i) => (
                    <div
                      key={kr.id || i}
                      style={{
                        padding: '1rem',
                        borderRadius: 'var(--radius)',
                        background: 'var(--bg-elevated)',
                        border: '1px solid var(--border)',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                        <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)', maxWidth: '80%' }}>
                          {kr.title}
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
                  {(!group.keyResults || group.keyResults.length === 0) && (
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textAlign: 'center' }}>No key results defined for this objective.</div>
                  )}
                </div>
              </div>
            ))}
          </div>
      )}

      {/* Goal Modal */}
      {isModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 700, padding: '1.75rem', background: 'var(--bg-surface)', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {editingGoal ? 'Edit Goal' : 'New Goal'}
                </h3>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsModalOpen(false)}>
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '1rem' }}>
                    <div>
                        <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                            Quarter/Timeline *
                        </label>
                        <input
                            className="input"
                            placeholder="e.g. Q3 2026"
                            value={quarter}
                            onChange={e => setQuarter(e.target.value)}
                            required
                        />
                    </div>
                    <div>
                         <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                            Overall Progress (%)
                        </label>
                        <input
                            type="number"
                            min={0}
                            max={100}
                            className="input"
                            value={progress}
                            onChange={e => setProgress(Number(e.target.value))}
                        />
                    </div>
                </div>

                <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                        Objective *
                    </label>
                    <input
                        className="input"
                        placeholder="e.g. Expand platform capabilities"
                        value={objective}
                        onChange={e => setObjective(e.target.value)}
                        required
                    />
                </div>
                
                <div style={{ marginTop: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                        <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                            Key Results
                        </label>
                        <button type="button" className="btn btn-sm btn-ghost" onClick={handleAddKR}>
                            <PlusCircle size={14} /> Add KR
                        </button>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        {keyResults.map((kr, index) => (
                            <div key={kr.id} style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '0.5rem', background: 'var(--bg-elevated)', padding: '1rem', borderRadius: 'var(--radius)', border: '1px solid var(--border)' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>KR #{index + 1}</span>
                                    <button type="button" className="btn btn-icon btn-ghost btn-sm" onClick={() => handleRemoveKR(index)} style={{ color: '#ef4444' }}>
                                        <MinusCircle size={14} />
                                    </button>
                                </div>
                                <input
                                    className="input"
                                    placeholder="Title"
                                    value={kr.title}
                                    onChange={e => updateKR(index, 'title', e.target.value)}
                                    required
                                />
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 80px', gap: '0.5rem' }}>
                                     <input
                                        className="input"
                                        placeholder="Target (e.g. $1M)"
                                        value={kr.target}
                                        onChange={e => updateKR(index, 'target', e.target.value)}
                                    />
                                    <input
                                        className="input"
                                        placeholder="Current Status"
                                        value={kr.current}
                                        onChange={e => updateKR(index, 'current', e.target.value)}
                                    />
                                     <input
                                        type="number"
                                        min={0}
                                        max={100}
                                        className="input"
                                        placeholder="%"
                                        value={kr.progress}
                                        onChange={e => updateKR(index, 'progress', Number(e.target.value))}
                                    />
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
                
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  {editingGoal ? 'Save Changes' : 'Create Goal'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
