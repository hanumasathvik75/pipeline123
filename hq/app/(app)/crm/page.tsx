'use client';

import { useState, useRef } from 'react';
import { useHQStore } from '@/lib/store';
import { formatCurrency, leadStageColor, getInitials } from '@/lib/utils';
import type { Lead, LeadStage, CrmContact } from '@/lib/types';
import {
  Building2, DollarSign, Mail, Phone, MapPin, Calendar, Plus,
  Search, ArrowRight, UserCheck, Activity as ActivityIcon, ChevronRight, Upload, Edit2, Trash2, X
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
  const { leads, crmContacts, team, updateLeadStage, addLead, addCrmContact, updateCrmContact, deleteCrmContact, importCrmContacts } = useHQStore();
  const [activeTab, setActiveTab] = useState<'pipeline' | 'contacts'>('contacts');
  const [search, setSearch] = useState('');
  
  // Pipeline State
  const [selectedLeadId, setSelectedLeadId] = useState<string | null>(null);
  const [isAddLeadModalOpen, setIsAddLeadModalOpen] = useState(false);
  const [newLead, setNewLead] = useState({ companyName: '', contactName: '', opportunity: '', potentialValue: '', stage: 'lead' as LeadStage });

  // Contacts State
  const [isContactModalOpen, setIsContactModalOpen] = useState(false);
  const [editingContact, setEditingContact] = useState<CrmContact | null>(null);
  const [contactForm, setContactForm] = useState({
      name: '', email: '', phone: '', designation: '', company: '', notes: '', stage: 'lead' as LeadStage
  });
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Pipeline Data
  const totalPipelineValue = leads.reduce((sum, l) => sum + (l.stage !== 'customer' ? l.potentialValue : 0), 0);
  const closedValue = leads.reduce((sum, l) => sum + (l.stage === 'customer' ? l.potentialValue : 0), 0);
  const filteredLeads = leads.filter(l =>
    l.companyName.toLowerCase().includes(search.toLowerCase()) ||
    l.contactName.toLowerCase().includes(search.toLowerCase()) ||
    l.opportunity.toLowerCase().includes(search.toLowerCase())
  );
  const selectedLead = leads.find(l => l.id === selectedLeadId);

  // Contacts Data
  const filteredContacts = crmContacts.filter(c => 
      c.name.toLowerCase().includes(search.toLowerCase()) ||
      c.company.toLowerCase().includes(search.toLowerCase()) ||
      c.email.toLowerCase().includes(search.toLowerCase())
  );

  // Contact Handlers
  const openNewContactModal = () => {
      setContactForm({ name: '', email: '', phone: '', designation: '', company: '', notes: '', stage: 'lead' });
      setEditingContact(null);
      setIsContactModalOpen(true);
  };

  const openEditContactModal = (c: CrmContact) => {
      setContactForm({
          name: c.name, email: c.email, phone: c.phone, 
          designation: c.designation, company: c.company, notes: c.notes, stage: c.stage
      });
      setEditingContact(c);
      setIsContactModalOpen(true);
  };

  const handleDeleteContact = (id: string) => {
      if(window.confirm('Are you sure you want to delete this contact?')) {
          deleteCrmContact(id);
      }
  };

  const handleContactSubmit = (e: React.FormEvent) => {
      e.preventDefault();
      if(!contactForm.name.trim() || !contactForm.company.trim()) return;

      if(editingContact) {
          updateCrmContact(editingContact.id, contactForm);
      } else {
          addCrmContact(contactForm);
      }
      setIsContactModalOpen(false);
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (!file) return;

      const reader = new FileReader();
      reader.onload = (event) => {
          const text = event.target?.result as string;
          if (!text) return;
          
          const lines = text.split('\n').map(l => l.trim()).filter(Boolean);
          // Expects CSV header: Name,Email,Phone,Designation,Company,Notes
          // We will just skip the header line if it contains 'name'
          const contactsToAdd: Omit<CrmContact, 'createdAt' | 'updatedAt'>[] = [];
          
          let startIndex = 0;
          if (lines.length > 0 && lines[0].toLowerCase().includes('name') && lines[0].toLowerCase().includes('email')) {
              startIndex = 1;
          }

          for(let i = startIndex; i < lines.length; i++) {
              // Handle quotes in CSV naive approach
              const row = lines[i].split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/);
              if (row.length >= 5) {
                  contactsToAdd.push({
                      id: `crm-${Date.now()}-${i}`,
                      name: row[0].replace(/"/g, '').trim(),
                      email: row[1].replace(/"/g, '').trim(),
                      phone: row[2].replace(/"/g, '').trim(),
                      designation: row[3].replace(/"/g, '').trim(),
                      company: row[4].replace(/"/g, '').trim(),
                      notes: row[5] ? row[5].replace(/"/g, '').trim() : '',
                      stage: 'lead'
                  });
              }
          }

          if (contactsToAdd.length > 0) {
              importCrmContacts(contactsToAdd);
              alert(`Successfully imported ${contactsToAdd.length} contacts.`);
          } else {
              alert('No valid contacts found. Please ensure the CSV has columns: Name, Email, Phone, Designation, Company, Notes.');
          }
          
          if(fileInputRef.current) fileInputRef.current.value = '';
      };
      reader.readAsText(file);
  };

  return (
    <div className="page animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">CRM & Business Development</h1>
          <p className="page-subtitle">
            Manage contacts, telematics pilots, and enterprise opportunities.
          </p>
        </div>
        
        <div style={{ display: 'flex', gap: '0.5rem' }}>
             {activeTab === 'contacts' ? (
                <>
                    <input 
                        type="file" 
                        accept=".csv" 
                        ref={fileInputRef} 
                        style={{ display: 'none' }} 
                        onChange={handleFileUpload}
                    />
                    <button className="btn btn-secondary" onClick={() => fileInputRef.current?.click()}>
                        <Upload size={15} /> Import CSV
                    </button>
                    <button className="btn btn-primary" onClick={openNewContactModal}>
                        <Plus size={15} /> Add Contact
                    </button>
                </>
             ) : (
                <button className="btn btn-primary" onClick={() => setIsAddLeadModalOpen(true)}>
                    <Plus size={15} /> Add Lead
                </button>
             )}
        </div>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid var(--border)', marginBottom: '1.5rem' }}>
          <button 
              className={`btn btn-ghost ${activeTab === 'contacts' ? 'active' : ''}`}
              style={{ borderBottom: activeTab === 'contacts' ? '2px solid var(--accent)' : '2px solid transparent', borderRadius: 0, paddingBottom: '0.75rem' }}
              onClick={() => setActiveTab('contacts')}
          >
              <UserCheck size={16} /> Contacts Directory
          </button>
          <button 
              className={`btn btn-ghost ${activeTab === 'pipeline' ? 'active' : ''}`}
              style={{ borderBottom: activeTab === 'pipeline' ? '2px solid var(--accent)' : '2px solid transparent', borderRadius: 0, paddingBottom: '0.75rem' }}
              onClick={() => setActiveTab('pipeline')}
          >
              <ActivityIcon size={16} /> Sales Pipeline
          </button>
      </div>

      {/* Search & Filter */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <div style={{ position: 'relative', flex: '0 0 280px' }}>
          <Search size={13} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            className="input"
            placeholder={activeTab === 'contacts' ? "Search contacts, company, email…" : "Search company, contact, opportunity…"}
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ paddingLeft: '2.25rem', height: 34, fontSize: '0.8rem' }}
          />
        </div>
      </div>

      {/* --- Contacts View --- */}
      {activeTab === 'contacts' && (
          <div className="card" style={{ overflow: 'hidden' }}>
              <table className="data-table">
                  <thead>
                      <tr>
                          <th>Name</th>
                          <th>Company & Title</th>
                          <th>Contact Info</th>
                          <th>Stage</th>
                          <th>Notes</th>
                          <th style={{ textAlign: 'right' }}>Actions</th>
                      </tr>
                  </thead>
                  <tbody>
                      {filteredContacts.map(c => (
                          <tr key={c.id}>
                              <td>
                                  <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{c.name}</div>
                              </td>
                              <td>
                                  <div style={{ fontSize: '0.85rem' }}>{c.company}</div>
                                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{c.designation}</div>
                              </td>
                              <td>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                                      <Mail size={12} /> {c.email || '—'}
                                  </div>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.1rem' }}>
                                      <Phone size={12} /> {c.phone || '—'}
                                  </div>
                              </td>
                              <td>
                                   <select
                                        value={c.stage}
                                        onChange={e => updateCrmContact(c.id, { stage: e.target.value as LeadStage })}
                                        className={`badge ${leadStageColor(c.stage)}`}
                                        style={{ border: 'none', cursor: 'pointer', outline: 'none' }}
                                    >
                                        {STAGES.map(s => <option key={s.stage} value={s.stage} style={{ background: '#1e2230', color: '#fff' }}>{s.label}</option>)}
                                    </select>
                              </td>
                              <td>
                                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 200 }}>
                                      {c.notes || '—'}
                                  </div>
                              </td>
                              <td style={{ textAlign: 'right' }}>
                                  <button className="btn btn-icon btn-ghost btn-sm" onClick={() => openEditContactModal(c)}>
                                      <Edit2 size={14} />
                                  </button>
                                  <button className="btn btn-icon btn-ghost btn-sm" onClick={() => handleDeleteContact(c.id)} style={{ color: '#ef4444' }}>
                                      <Trash2 size={14} />
                                  </button>
                              </td>
                          </tr>
                      ))}
                      {filteredContacts.length === 0 && (
                          <tr>
                              <td colSpan={6} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                                  No contacts found. Use the "Import CSV" button to bulk upload.
                              </td>
                          </tr>
                      )}
                  </tbody>
              </table>
          </div>
      )}

      {/* --- Pipeline View --- */}
      {activeTab === 'pipeline' && (
        <>
            {/* Summary KPI Cards */}
            <div className="grid-stats" style={{ marginBottom: '1.5rem' }}>
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

            {/* Pipeline Board */}
            <div className="pipeline-board" style={{ overflowX: 'auto', display: 'flex', gap: '1rem', paddingBottom: '1rem', minHeight: 'calc(100vh - 400px)' }}>
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
        </>
      )}

      {/* Contacts Add/Edit Modal */}
      {isContactModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 500, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>{editingContact ? 'Edit Contact' : 'Add New Contact'}</h3>
                <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsContactModalOpen(false)}>
                    <X size={16} />
                </button>
            </div>
            
            <form onSubmit={handleContactSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Name *</label>
                    <input className="input" placeholder="e.g. Jane Doe" required value={contactForm.name} onChange={e => setContactForm({...contactForm, name: e.target.value})} />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Company *</label>
                    <input className="input" placeholder="e.g. Ford Motors" required value={contactForm.company} onChange={e => setContactForm({...contactForm, company: e.target.value})} />
                  </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Email</label>
                    <input type="email" className="input" placeholder="jane@example.com" value={contactForm.email} onChange={e => setContactForm({...contactForm, email: e.target.value})} />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Phone</label>
                    <input type="tel" className="input" placeholder="+1 234 567 8900" value={contactForm.phone} onChange={e => setContactForm({...contactForm, phone: e.target.value})} />
                  </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Designation</label>
                    <input className="input" placeholder="e.g. CTO" value={contactForm.designation} onChange={e => setContactForm({...contactForm, designation: e.target.value})} />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Status / Stage</label>
                    <select className="input" value={contactForm.stage} onChange={e => setContactForm({...contactForm, stage: e.target.value as LeadStage})}>
                      {STAGES.map(s => <option key={s.stage} value={s.stage}>{s.label}</option>)}
                    </select>
                  </div>
              </div>
              
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Notes</label>
                <textarea className="input" rows={3} placeholder="Background, requirements, next steps..." value={contactForm.notes} onChange={e => setContactForm({...contactForm, notes: e.target.value})} />
              </div>
              
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsContactModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editingContact ? 'Save Changes' : 'Add Contact'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

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
            
            <form onSubmit={(e) => { 
              e.preventDefault(); 
              addLead({
                companyName: newLead.companyName,
                contactName: newLead.contactName,
                opportunity: newLead.opportunity,
                potentialValue: Number(newLead.potentialValue),
                stage: newLead.stage,
                type: 'Enterprise',
                location: 'Global',
                currency: 'INR',
                contactEmail: '',
                contactPhone: '',
                assignedTo: team[0]?.id || 'u-1',
                notes: '',
                nextFollowUp: '',
                activities: []
              });
              setIsAddLeadModalOpen(false); 
              setNewLead({ companyName: '', contactName: '', opportunity: '', potentialValue: '', stage: 'lead' }); 
            }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
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
                <button type="button" className="btn btn-secondary" onClick={() => setIsAddLeadModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add Lead</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
