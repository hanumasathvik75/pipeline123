'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import type { Investor, InvestorStage } from '@/lib/types';
import {
  TrendingUp, DollarSign, Users, Briefcase, Plus, Search, Calendar,
  ArrowRight, CheckCircle2, FileText, ChevronRight
} from 'lucide-react';

const STAGES: { stage: InvestorStage; label: string; color: string }[] = [
  { stage: 'research', label: 'Research', color: '#64748b' },
  { stage: 'intro', label: 'Warm Intro', color: '#38bdf8' },
  { stage: 'first_meeting', label: '1st Pitch', color: '#f59e0b' },
  { stage: 'partner_meeting', label: 'Partner Mtg', color: '#eab308' },
  { stage: 'due_diligence', label: 'Due Diligence', color: '#6366f1' },
  { stage: 'term_sheet', label: 'Term Sheet', color: '#a855f7' },
  { stage: 'closed', label: 'Closed / Committed', color: '#10b981' },
  { stage: 'pass', label: 'Passed', color: '#ef4444' },
];

export default function FundraisingPage() {
  const { investors, updateInvestorStage } = useHQStore();
  const [search, setSearch] = useState('');
  const [selectedInvestor, setSelectedInvestor] = useState<Investor | null>(null);
  const [isAddInvestorModalOpen, setIsAddInvestorModalOpen] = useState(false);
  const [newInvestor, setNewInvestor] = useState({ name: '', firm: '', ticketSize: '', stage: 'research' as InvestorStage });

  const activeInvestors = investors.filter(i => i.stage !== 'closed' && i.stage !== 'pass');
  const committedInvestors = investors.filter(i => i.stage === 'closed');

  const filtered = investors.filter(i =>
    i.name.toLowerCase().includes(search.toLowerCase()) ||
    (i.firm && i.firm.toLowerCase().includes(search.toLowerCase())) ||
    (i.investmentThesis && i.investmentThesis.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="page animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Fundraising & Investor CRM</h1>
          <p className="page-subtitle">
            Manage institutional deep-tech venture capital, angel networks, data rooms, and cap table pipeline.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setIsAddInvestorModalOpen(true)}>
          <Plus size={15} />
          Add Investor
        </button>
      </div>

      {/* KPI Stats */}
      <div className="grid-stats">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(99, 102, 241, 0.12)', color: '#818cf8' }}>
            <TrendingUp size={20} />
          </div>
          <div>
            <div className="stat-value">$750K</div>
            <div className="stat-label">Round Target (Seed)</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.12)', color: '#38bdf8' }}>
            <Users size={20} />
          </div>
          <div>
            <div className="stat-value">{activeInvestors.length}</div>
            <div className="stat-label">Active Discussions</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.12)', color: '#10b981' }}>
            <DollarSign size={20} />
          </div>
          <div>
            <div className="stat-value">{committedInvestors.length}</div>
            <div className="stat-label">Committed / Closed</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(245, 158, 11, 0.12)', color: '#f59e0b' }}>
            <Briefcase size={20} />
          </div>
          <div>
            <div className="stat-value">v2.4</div>
            <div className="stat-label">Current Pitch Deck</div>
          </div>
        </div>
      </div>

      {/* Search */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <div style={{ position: 'relative', flex: '0 0 280px' }}>
          <Search size={13} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            className="input"
            placeholder="Search VC firm, partner, thesis…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ paddingLeft: '2.25rem', height: 34, fontSize: '0.8rem' }}
          />
        </div>
      </div>

      {/* Pipeline Columns */}
      <div className="pipeline-board" style={{ overflowX: 'auto', display: 'flex', gap: '1rem', paddingBottom: '1rem', minHeight: 'calc(100vh - 350px)' }}>
        {STAGES.map(s => {
          const stageInvestors = filtered.filter(i => i.stage === s.stage);

          return (
            <div key={s.stage} className="pipeline-col" style={{ minWidth: 260, maxWidth: 300, flexShrink: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {/* Header */}
              <div style={{
                background: 'var(--bg-surface)', padding: '0.75rem 1rem', borderRadius: 'var(--radius)',
                borderTop: `3px solid ${s.color}`, border: '1px solid var(--border)', borderTopWidth: 3
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '0.825rem', fontWeight: 700 }}>{s.label}</span>
                  <span className="badge" style={{ background: 'var(--bg-card)', color: 'var(--text-muted)', fontSize: '0.7rem' }}>
                    {stageInvestors.length}
                  </span>
                </div>
              </div>

              {/* Cards */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem', flex: 1 }}>
                {stageInvestors.map(inv => (
                  <div
                    key={inv.id}
                    onClick={() => setSelectedInvestor(inv)}
                    className="card"
                    style={{
                      padding: '1rem',
                      cursor: 'pointer',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '0.625rem',
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <h4 style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-primary)', lineHeight: 1.2 }}>
                          {inv.name}
                        </h4>
                        {inv.ticketSize && (
                          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#10b981' }}>
                            {inv.ticketSize}
                          </span>
                        )}
                      </div>
                      {inv.firm && (
                        <div style={{ fontSize: '0.7rem', color: 'var(--accent)', marginTop: '0.2rem' }}>
                          {inv.firm}
                        </div>
                      )}
                    </div>

                    {inv.investmentThesis && (
                      <p style={{ fontSize: '0.725rem', color: 'var(--text-secondary)', lineHeight: 1.35, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                        {inv.investmentThesis}
                      </p>
                    )}

                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '0.5rem', borderTop: '1px solid var(--border)', fontSize: '0.7rem' }}>
                      <span style={{ color: 'var(--text-muted)' }}>Round: {inv.round}</span>

                      {/* Quick stage selector */}
                      <select
                        value={inv.stage}
                        onClick={e => e.stopPropagation()}
                        onChange={e => updateInvestorStage(inv.id, e.target.value as InvestorStage)}
                        style={{
                          background: 'transparent',
                          border: 'none',
                          color: 'var(--text-muted)',
                          fontSize: '0.65rem',
                          cursor: 'pointer',
                          outline: 'none',
                        }}
                      >
                        {STAGES.map(st => (
                          <option key={st.stage} value={st.stage} style={{ background: '#1e2230', color: '#fff' }}>
                            → {st.label}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                ))}

                {stageInvestors.length === 0 && (
                  <div style={{ padding: '1rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.725rem', border: '1px dashed var(--border)', borderRadius: 'var(--radius)' }}>
                    No investors
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Investor Detail Drawer */}
      {selectedInvestor && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 580, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
              <div>
                <span className="badge" style={{ marginBottom: '0.35rem', background: 'rgba(99,102,241,0.15)', color: '#818cf8' }}>
                  {selectedInvestor.round} · {selectedInvestor.stage}
                </span>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>{selectedInvestor.name}</h2>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{selectedInvestor.firm}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#10b981' }}>
                  {selectedInvestor.ticketSize || 'Undisclosed'}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Ticket Size</div>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '1.5rem', fontSize: '0.825rem' }}>
              {selectedInvestor.investmentThesis && (
                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Investment Thesis & Focus</div>
                  <div style={{ background: 'var(--bg-elevated)', padding: '0.75rem', borderRadius: 'var(--radius)', color: 'var(--text-secondary)' }}>
                    {selectedInvestor.investmentThesis}
                  </div>
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Next Action</div>
                  <div>{selectedInvestor.nextAction}</div>
                  {selectedInvestor.nextActionDate && (
                    <div style={{ fontSize: '0.75rem', color: '#f59e0b', marginTop: '0.2rem' }}>
                      Target: {selectedInvestor.nextActionDate}
                    </div>
                  )}
                </div>

                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Deck Version Sent</div>
                  <div>{selectedInvestor.deckVersionSent || 'v2.4 (Seed Pitch)'}</div>
                </div>
              </div>

              {selectedInvestor.notes && (
                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Confidential Notes</div>
                  <div style={{ background: 'var(--bg-elevated)', padding: '0.75rem', borderRadius: 'var(--radius)', color: 'var(--text-secondary)' }}>
                    {selectedInvestor.notes}
                  </div>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button className="btn btn-secondary" onClick={() => setSelectedInvestor(null)}>Close</button>
            </div>
          </div>
        </div>
      )}

      {/* Add Investor Modal */}
      {isAddInvestorModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 450, padding: '1.5rem', background: 'var(--bg-surface)' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Add New Investor</h3>
            
            <form onSubmit={(e) => { e.preventDefault(); setIsAddInvestorModalOpen(false); setNewInvestor({ name: '', firm: '', ticketSize: '', stage: 'research' }); }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Partner / Investor Name</label>
                <input className="input" placeholder="e.g. Marc Andreessen" required value={newInvestor.name} onChange={e => setNewInvestor({...newInvestor, name: e.target.value})} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>VC Firm or Network</label>
                <input className="input" placeholder="e.g. a16z" value={newInvestor.firm} onChange={e => setNewInvestor({...newInvestor, firm: e.target.value})} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Target Ticket Size</label>
                <input className="input" placeholder="e.g. $2.5M" value={newInvestor.ticketSize} onChange={e => setNewInvestor({...newInvestor, ticketSize: e.target.value})} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Initial Pipeline Stage</label>
                <select className="input" value={newInvestor.stage} onChange={e => setNewInvestor({...newInvestor, stage: e.target.value as InvestorStage})}>
                  {STAGES.map(s => <option key={s.stage} value={s.stage}>{s.label}</option>)}
                </select>
              </div>
              
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                <button type="button" className="btn btn-ghost" onClick={() => setIsAddInvestorModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add to Pipeline</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
