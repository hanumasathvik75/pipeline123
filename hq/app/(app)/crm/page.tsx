'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { formatCurrency, leadStageColor, getInitials } from '@/lib/utils';
import type { Lead, LeadStage } from '@/lib/types';
import {
  Building2, DollarSign, Mail, Phone, MapPin, Calendar, Plus,
  Search, ArrowRight, UserCheck, Activity as ActivityIcon, ChevronRight
} from 'lucide-react';

const STAGES: { stage: LeadStage; label: string; color: string }[] = [
  { stage: 'lead', label: 'New Lead', color: '#64748b' },
  { stage: 'contacted', label: 'Contacted', color: '#38bdf8' },
  { stage: 'interested', label: 'Interested', color: '#f59e0b' },
  { stage: 'meeting', label: 'Meeting', color: '#eab308' },
  { stage: 'pilot', label: 'Pilot / PoC', color: '#06b6d4' },
  { stage: 'proposal', label: 'Proposal', color: '#6366f1' },
  { stage: 'negotiation', label: 'Negotiation', color: '#a855f7' },
  { stage: 'customer', label: 'Customer', color: '#10b981' },
];

export default function CRMPage() {
  const { leads, team, updateLeadStage } = useHQStore();
  const [search, setSearch] = useState('');
  const [selectedLeadId, setSelectedLeadId] = useState<string | null>(null);
  const [isAddLeadModalOpen, setIsAddLeadModalOpen] = useState(false);
  const [newLead, setNewLead] = useState({ companyName: '', contactName: '', opportunity: '', potentialValue: '', stage: 'lead' as LeadStage });

  const totalPipelineValue = leads.reduce((sum, l) => sum + (l.stage !== 'customer' ? l.potentialValue : 0), 0);
  const closedValue = leads.reduce((sum, l) => sum + (l.stage === 'customer' ? l.potentialValue : 0), 0);

  const filteredLeads = leads.filter(l =>
    l.companyName.toLowerCase().includes(search.toLowerCase()) ||
    l.contactName.toLowerCase().includes(search.toLowerCase()) ||
    l.opportunity.toLowerCase().includes(search.toLowerCase())
  );

  const selectedLead = leads.find(l => l.id === selectedLeadId);

  return (
    <div className="page animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Business Development & CRM</h1>
          <p className="page-subtitle">
            Track OEM engagements, telematics pilots, fleet contracts, and tier-1 auto supplier pipelines.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setIsAddLeadModalOpen(true)}>
          <Plus size={15} />
          Add Lead
        </button>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid-stats">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(99, 102, 241, 0.12)', color: '#818cf8' }}>
            <Building2 size={20} />
          </div>
          <div>
            <div className="stat-value">{leads.length}</div>
            <div className="stat-label">Total Accounts</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.12)', color: '#38bdf8' }}>
            <DollarSign size={20} />
          </div>
          <div>
            <div className="stat-value">{formatCurrency(totalPipelineValue, 'INR')}</div>
            <div className="stat-label">Active Pipeline Value</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(6, 182, 212, 0.12)', color: '#06b6d4' }}>
            <ActivityIcon size={20} />
          </div>
          <div>
            <div className="stat-value">{leads.filter(l => l.stage === 'pilot').length}</div>
            <div className="stat-label">Active Pilots / PoCs</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.12)', color: '#10b981' }}>
            <UserCheck size={20} />
          </div>
          <div>
            <div className="stat-value">{formatCurrency(closedValue, 'INR')}</div>
            <div className="stat-label">Closed Revenue (ARR)</div>
          </div>
        </div>
      </div>

      {/* Search & Filter */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <div style={{ position: 'relative', flex: '0 0 280px' }}>
          <Search size={13} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            className="input"
            placeholder="Search company, contact, opportunity…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ paddingLeft: '2.25rem', height: 34, fontSize: '0.8rem' }}
          />
        </div>
      </div>

      {/* Pipeline Board */}
      <div className="pipeline-board" style={{ overflowX: 'auto', display: 'flex', gap: '1rem', paddingBottom: '1rem', minHeight: 'calc(100vh - 350px)' }}>
        {STAGES.map(s => {
          const stageLeads = filteredLeads.filter(l => l.stage === s.stage);
          const stageValue = stageLeads.reduce((sum, l) => sum + l.potentialValue, 0);

          return (
            <div key={s.stage} className="pipeline-col" style={{ minWidth: 260, maxWidth: 300, flexShrink: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {/* Stage Header */}
              <div style={{
                background: 'var(--bg-surface)', padding: '0.75rem 1rem', borderRadius: 'var(--radius)',
                borderTop: `3px solid ${s.color}`, border: '1px solid var(--border)', borderTopWidth: 3
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '0.825rem', fontWeight: 700 }}>{s.label}</span>
                  <span className="badge" style={{ background: 'var(--bg-card)', color: 'var(--text-muted)', fontSize: '0.7rem' }}>
                    {stageLeads.length}
                  </span>
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                  {formatCurrency(stageValue, 'INR')}
                </div>
              </div>

              {/* Lead Cards */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem', flex: 1 }}>
                {stageLeads.map(lead => {
                  const assignedMember = team.find(m => m.id === lead.assignedTo);

                  return (
                    <div
                      key={lead.id}
                      onClick={() => setSelectedLeadId(lead.id)}
                      className="card"
                      style={{
                        padding: '1rem',
                        cursor: 'pointer',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '0.625rem',
                        border: selectedLeadId === lead.id ? '1px solid var(--accent)' : '1px solid var(--border)',
                      }}
                    >
                      <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                          <h4 style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-primary)', lineHeight: 1.2 }}>
                            {lead.companyName}
                          </h4>
                          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#10b981' }}>
                            {formatCurrency(lead.potentialValue, lead.currency)}
                          </span>
                        </div>
                        <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                          {lead.type} · {lead.location}
                        </div>
                      </div>

                      <p style={{ fontSize: '0.725rem', color: 'var(--text-secondary)', lineHeight: 1.35, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                        {lead.opportunity}
                      </p>

                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '0.5rem', borderTop: '1px solid var(--border)', fontSize: '0.7rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: 'var(--text-muted)' }}>
                          <span style={{ fontWeight: 500 }}>{lead.contactName}</span>
                        </div>

                        {/* Quick stage selector */}
                        <select
                          value={lead.stage}
                          onClick={e => e.stopPropagation()}
                          onChange={e => updateLeadStage(lead.id, e.target.value as LeadStage)}
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
                  );
                })}

                {stageLeads.length === 0 && (
                  <div style={{ padding: '1rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.725rem', border: '1px dashed var(--border)', borderRadius: 'var(--radius)' }}>
                    Empty stage
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Lead Detail Drawer / Modal */}
      {selectedLead && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 600, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
              <div>
                <span className={`badge ${leadStageColor(selectedLead.stage)}`} style={{ marginBottom: '0.35rem' }}>
                  {STAGES.find(s => s.stage === selectedLead.stage)?.label}
                </span>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>{selectedLead.companyName}</h2>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{selectedLead.type} · {selectedLead.location}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#10b981' }}>
                  {formatCurrency(selectedLead.potentialValue, selectedLead.currency)}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Est. Contract Value</div>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '1.5rem' }}>
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Opportunity Scope</div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-primary)', background: 'var(--bg-elevated)', padding: '0.75rem', borderRadius: 'var(--radius)' }}>
                  {selectedLead.opportunity}
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Contact Person</div>
                  <div style={{ fontSize: '0.825rem' }}>{selectedLead.contactName}</div>
                  {selectedLead.contactEmail && <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{selectedLead.contactEmail}</div>}
                  {selectedLead.contactPhone && <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{selectedLead.contactPhone}</div>}
                </div>

                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Lead Owner</div>
                  <div style={{ fontSize: '0.825rem' }}>{team.find(m => m.id === selectedLead.assignedTo)?.name || 'Unassigned'}</div>
                  {selectedLead.nextFollowUp && (
                    <div style={{ fontSize: '0.75rem', color: '#f59e0b', marginTop: '0.2rem' }}>
                      Next Follow-up: {selectedLead.nextFollowUp}
                    </div>
                  )}
                </div>
              </div>

              {selectedLead.notes && (
                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Strategic Notes</div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', background: 'var(--bg-elevated)', padding: '0.75rem', borderRadius: 'var(--radius)' }}>
                    {selectedLead.notes}
                  </div>
                </div>
              )}

              {/* Activity Timeline */}
              {selectedLead.activities && selectedLead.activities.length > 0 && (
                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.5rem' }}>Recent Engagements</div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {selectedLead.activities.map(a => (
                      <div key={a.id} style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', fontSize: '0.78rem' }}>
                        <span className="badge" style={{ fontSize: '0.65rem', textTransform: 'uppercase' }}>{a.type}</span>
                        <span style={{ color: 'var(--text-muted)' }}>{a.date}:</span>
                        <span style={{ color: 'var(--text-primary)' }}>{a.description}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button className="btn btn-secondary" onClick={() => setSelectedLeadId(null)}>Close</button>
            </div>
          </div>
        </div>
      )}

      {/* Add Lead Modal */}
      {isAddLeadModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 450, padding: '1.5rem', background: 'var(--bg-surface)' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Add New Lead</h3>
            
            <form onSubmit={(e) => { e.preventDefault(); setIsAddLeadModalOpen(false); setNewLead({ companyName: '', contactName: '', opportunity: '', potentialValue: '', stage: 'lead' }); }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Company Name</label>
                <input className="input" placeholder="e.g. Ford Motors" required value={newLead.companyName} onChange={e => setNewLead({...newLead, companyName: e.target.value})} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Contact Person</label>
                <input className="input" placeholder="e.g. Jane Doe" value={newLead.contactName} onChange={e => setNewLead({...newLead, contactName: e.target.value})} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Opportunity Overview</label>
                <input className="input" placeholder="e.g. Telematics Pilot" value={newLead.opportunity} onChange={e => setNewLead({...newLead, opportunity: e.target.value})} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Estimated Value (INR)</label>
                <input type="number" className="input" placeholder="e.g. 5000000" value={newLead.potentialValue} onChange={e => setNewLead({...newLead, potentialValue: e.target.value})} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Initial Stage</label>
                <select className="input" value={newLead.stage} onChange={e => setNewLead({...newLead, stage: e.target.value as LeadStage})}>
                  {STAGES.map(s => <option key={s.stage} value={s.stage}>{s.label}</option>)}
                </select>
              </div>
              
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                <button type="button" className="btn btn-ghost" onClick={() => setIsAddLeadModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add Lead</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
