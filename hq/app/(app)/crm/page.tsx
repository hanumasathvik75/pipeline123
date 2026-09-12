'use client';

import { useState, useRef } from 'react';
import { useHQStore } from '@/lib/store';
import { formatCurrency, leadStageColor, getInitials } from '@/lib/utils';
import type { Lead, LeadStage, CrmContact } from '@/lib/types';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { exportAllDataMaster, downloadCSV } from '@/lib/exportAllData';


const STAGES: { stage: LeadStage; label: string; color: string }[] = [
  { stage: 'lead', label: 'New Lead', color: '#000000' },
  { stage: 'contacted', label: 'Contacted', color: '#000000' },
  { stage: 'interested', label: 'Interested', color: '#000000' },
  { stage: 'meeting', label: 'Meeting', color: '#000000' },
  { stage: 'pilot', label: 'Pilot / PoC', color: '#000000' },
  { stage: 'proposal', label: 'Proposal', color: '#000000' },
  { stage: 'negotiation', label: 'Negotiation', color: '#000000' },
  { stage: 'customer', label: 'Customer', color: '#000000' },
];

export default function CRMPage() {
  const { 
    leads, crmContacts, team, projects, tasks, meetings, wikiDocs, investors, 
    rdProjects, goals, progressLogs, financeData, fundraisingMeta,
    currentUser, updateLeadStage, addLead, addCrmContact, updateCrmContact, 
    deleteCrmContact, importCrmContacts 
  } = useHQStore();

  const [activeTab, setActiveTab] = useState<'pipeline' | 'contacts'>('contacts');
  const [search, setSearch] = useState('');
  
  // Pipeline State
  const [selectedLeadId, setSelectedLeadId] = useState<string | null>(null);
  const [isAddLeadModalOpen, setIsAddLeadModalOpen] = useState(false);
  const [newLead, setNewLead] = useState({ 
    companyName: '', contactName: '', opportunity: '', potentialValue: '', stage: 'lead' as LeadStage 
  });

  // Contacts State
  const [isContactModalOpen, setIsContactModalOpen] = useState(false);
  const [viewNotesContact, setViewNotesContact] = useState<CrmContact | null>(null);
  const [editingContact, setEditingContact] = useState<CrmContact | null>(null);
  const [copiedNotes, setCopiedNotes] = useState(false);
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);

  // Filters & Grouping State
  const [stageFilter, setStageFilter] = useState<LeadStage | 'all'>('all');
  const [groupBy, setGroupBy] = useState<'none' | 'company' | 'title' | 'company_title'>('none');

  const [contactForm, setContactForm] = useState({
    name: '', email: '', phone: '', designation: '', company: '', notes: '', stage: 'lead' as LeadStage
  });
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isAdmin = currentUser?.role === 'c_level' || currentUser?.role === 'founder';

  // Pipeline Data
  const totalPipelineValue = leads.reduce((sum, l) => sum + (l.stage !== 'customer' ? l.potentialValue : 0), 0);
  const closedValue = leads.reduce((sum, l) => sum + (l.stage === 'customer' ? l.potentialValue : 0), 0);
  const filteredLeads = leads.filter(l =>
    l.companyName.toLowerCase().includes(search.toLowerCase()) ||
    l.contactName.toLowerCase().includes(search.toLowerCase()) ||
    l.opportunity.toLowerCase().includes(search.toLowerCase())
  );
  const selectedLead = leads.find(l => l.id === selectedLeadId);

  // Contacts Filtering (searches name, company, designation, email, notes)
  const filteredContacts = crmContacts.filter(c => {
    const matchesCategory = stageFilter === 'all' || c.stage === stageFilter;
    const q = search.toLowerCase().trim();
    if (!q) return matchesCategory;
    const matchesSearch = 
      (c.name || '').toLowerCase().includes(q) ||
      (c.company || '').toLowerCase().includes(q) ||
      (c.designation || '').toLowerCase().includes(q) ||
      (c.email || '').toLowerCase().includes(q) ||
      (c.notes || '').toLowerCase().includes(q);
    return matchesCategory && matchesSearch;
  });

  // Grouping Data
  // 1. By Company
  const contactsByCompany = filteredContacts.reduce((acc, contact) => {
    const company = (contact.company || '').trim() || 'Unknown Company';
    if (!acc[company]) acc[company] = [];
    acc[company].push(contact);
    return acc;
  }, {} as Record<string, CrmContact[]>);

  // 2. By Title / Designation
  const contactsByTitle = filteredContacts.reduce((acc, contact) => {
    const title = (contact.designation || '').trim() || 'No Title Specified';
    if (!acc[title]) acc[title] = [];
    acc[title].push(contact);
    return acc;
  }, {} as Record<string, CrmContact[]>);

  // 3. By Company & Title (Hierarchical: Company -> Title -> Contacts)
  const contactsByCompanyAndTitle = filteredContacts.reduce((acc, contact) => {
    const company = (contact.company || '').trim() || 'Unknown Company';
    const title = (contact.designation || '').trim() || 'General / No Title';
    if (!acc[company]) acc[company] = {};
    if (!acc[company][title]) acc[company][title] = [];
    acc[company][title].push(contact);
    return acc;
  }, {} as Record<string, Record<string, CrmContact[]>>);

  // Contact Handlers
  const openNewContactModal = () => {
    setContactForm({ name: '', email: '', phone: '', designation: '', company: '', notes: '', stage: 'lead' });
    setEditingContact(null);
    setIsContactModalOpen(true);
  };

  const openEditContactModal = (c: CrmContact, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setContactForm({
      name: c.name, email: c.email, phone: c.phone, 
      designation: c.designation, company: c.company, notes: c.notes, stage: c.stage
    });
    setEditingContact(c);
    setIsContactModalOpen(true);
  };

  const handleDeleteContact = (id: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    if (window.confirm('Are you sure you want to delete this contact?')) {
      deleteCrmContact(id);
    }
  };

  const handleContactSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!contactForm.name.trim() || !contactForm.company.trim()) return;

    if (editingContact) {
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
      const contactsToAdd: Omit<CrmContact, 'createdAt' | 'updatedAt'>[] = [];
      
      let startIndex = 0;
      if (lines.length > 0 && lines[0].toLowerCase().includes('name') && lines[0].toLowerCase().includes('email')) {
        startIndex = 1;
      }

      for (let i = startIndex; i < lines.length; i++) {
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
      
      if (fileInputRef.current) fileInputRef.current.value = '';
    };
    reader.readAsText(file);
  };

  // Single CRM Contacts CSV export
  const handleExportContactsOnly = () => {
    const headers = ['Name', 'Email', 'Phone', 'Designation', 'Company', 'Stage', 'Notes', 'Created At'];
    const rows = crmContacts.map(c => [
      `"${(c.name || '').replace(/"/g, '""')}"`,
      `"${(c.email || '').replace(/"/g, '""')}"`,
      `"${(c.phone || '').replace(/"/g, '""')}"`,
      `"${(c.designation || '').replace(/"/g, '""')}"`,
      `"${(c.company || '').replace(/"/g, '""')}"`,
      `"${c.stage}"`,
      `"${(c.notes || '').replace(/"/g, '""')}"`,
      `"${c.createdAt || ''}"`
    ].join(','));
    
    const csvContent = [headers.join(','), ...rows].join('\n');
    downloadCSV(csvContent, `ignitionai_crm_contacts_${new Date().toISOString().split('T')[0]}.csv`);
  };

  // Master Full Website Data CSV Export (Admin only)
  const handleExportAllWebsiteData = () => {
    exportAllDataMaster({
      crmContacts,
      leads,
      team,
      projects,
      tasks,
      meetings,
      wikiDocs,
      investors,
      rdProjects,
      goals,
      progressLogs,
      financeData,
      fundraisingMeta,
    });
    setIsExportModalOpen(false);
  };

  const copyNotesToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedNotes(true);
    setTimeout(() => setCopiedNotes(false), 2000);
  };

  // Reusable Contact Card Component
  const renderContactCard = (c: CrmContact) => (
    <div 
      key={c.id} 
      className="card hover-effect" 
      onClick={() => setViewNotesContact(c)}
      style={{ 
        padding: '1.15rem', 
        border: '1px solid var(--border)', 
        background: 'var(--bg-elevated)',
        display: 'flex',
        flexDirection: 'column',
        gap: '0.65rem',
        cursor: 'pointer',
        transition: 'all 0.15s ease'
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-primary)' }}>{c.name}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.35rem', marginTop: '0.15rem' }}>
            <></>
            <span>{c.designation || 'No Title'}</span>
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.35rem', marginTop: '0.15rem' }}>
            <></>
            <span>{c.company || 'Unknown Company'}</span>
          </div>
        </div>
        <select
          value={c.stage}
          onClick={e => e.stopPropagation()}
          onChange={e => updateCrmContact(c.id, { stage: e.target.value as LeadStage })}
          className={`badge ${leadStageColor(c.stage)}`}
          style={{ border: 'none', cursor: 'pointer', outline: 'none', fontSize: '0.7rem' }}
        >
          {STAGES.map(s => <option key={s.stage} value={s.stage} style={{ background: '#000000', color: '#fff' }}>{s.label}</option>)}
        </select>
      </div>

      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '0.2rem', marginTop: '0.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
          <></> 
          <span>{c.email || '—'}</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
          <></> 
          <span>{c.phone || '—'}</span>
        </div>
      </div>

      {/* Clickable Notes Preview */}
      <div 
        style={{ 
          marginTop: '0.35rem', 
          fontSize: '0.75rem', 
          color: 'var(--text-muted)', 
          overflow: 'hidden', 
          textOverflow: 'ellipsis', 
          whiteSpace: 'nowrap', 
          background: 'var(--bg-surface)', 
          padding: '0.5rem 0.65rem',
          borderRadius: 'var(--radius-sm)',
          border: '1px solid var(--border)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '0.5rem'
        }}
        title="Click anywhere to view full formatted notes"
      >
        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
          <strong>Notes:</strong> {c.notes || '—'}
        </span>
        <></>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.4rem', marginTop: '0.25rem', paddingTop: '0.5rem', borderTop: '1px solid var(--border)' }}>
        <button 
          className="btn btn-icon btn-ghost btn-sm" 
          title="View Full Notes & Profile"
          onClick={(e) => { e.stopPropagation(); setViewNotesContact(c); }}
        >
          <></>
        </button>
        <button 
          className="btn btn-icon btn-ghost btn-sm" 
          title="Edit Contact"
          onClick={(e) => openEditContactModal(c, e)}
        >
          <></>
        </button>
        <button 
          className="btn btn-icon btn-ghost btn-sm" 
          title="Delete Contact"
          onClick={(e) => handleDeleteContact(c.id, e)} 
          style={{ color: '#000000' }}
        >
          <></>
        </button>
      </div>
    </div>
  );

  return (
    <div className="page animate-fade-in">
      {/* Header */}
      <div className="page-header" style={{ flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">CRM & Business Development</h1>
          <p className="page-subtitle">
            Manage contacts, telematics pilots, OEM relationships, and enterprise pipelines.
          </p>
        </div>
        
        <div style={{ display: 'flex', gap: '0.65rem', alignItems: 'center', flexWrap: 'wrap' }}>
          {/* Big Admin-Only Master Data Export Button */}
          {isAdmin && (
            <button 
              className="btn" 
              onClick={() => setIsExportModalOpen(true)}
              style={{ 
                background: 'linear-gradient(135deg, #000000 0%, #000000 100%)',
                color: '#ffffff',
                border: 'none',
                boxShadow: '0 4px 14px rgba(0, 0, 0, 0.35)',
                fontWeight: 600,
                padding: '0.55rem 1.1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                borderRadius: 'var(--radius)',
                cursor: 'pointer'
              }}
              title="Export complete database for all 12 modules across the website"
            >
              <></>
              <span>Export All Website Data (CSV)</span>
              <span style={{ 
                fontSize: '0.65rem', 
                background: 'rgba(255,255,255,0.25)', 
                padding: '0.15rem 0.45rem', 
                borderRadius: 4, 
                textTransform: 'uppercase', 
                letterSpacing: '0.05em',
                fontWeight: 700 
              }}>
                Admin
              </span>
            </button>
          )}

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
                <></> Import CSV
              </button>
              <button className="btn btn-secondary" onClick={handleExportContactsOnly} title="Export only CRM Contacts to CSV">
                <></> Export Contacts CSV
              </button>
              <button className="btn btn-primary" onClick={openNewContactModal}>
                <></> Add Contact
              </button>
            </>
          ) : (
            <button className="btn btn-primary" onClick={() => setIsAddLeadModalOpen(true)}>
              <></> Add Lead
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
          <></> Contacts Directory ({crmContacts.length})
        </button>
        <button 
          className={`btn btn-ghost ${activeTab === 'pipeline' ? 'active' : ''}`}
          style={{ borderBottom: activeTab === 'pipeline' ? '2px solid var(--accent)' : '2px solid transparent', borderRadius: 0, paddingBottom: '0.75rem' }}
          onClick={() => setActiveTab('pipeline')}
        >
          <></> Sales Pipeline ({leads.length})
        </button>
      </div>

      {/* Search & Filter Bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        {/* Search */}
        <div style={{ position: 'relative', flex: '1 1 240px', maxWidth: 360 }}>
          <></>
          <input
            className="input"
            placeholder={activeTab === 'contacts' ? "Search name, company, title, email, notes…" : "Search company, contact, opportunity…"}
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ paddingLeft: '2.25rem', height: 36, fontSize: '0.8rem', width: '100%' }}
          />
        </div>
        
        {activeTab === 'contacts' && (
          <>
            {/* Category / Stage Filter */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <></>
              <select 
                className="input" 
                style={{ height: 36, fontSize: '0.8rem', width: 'auto', minWidth: 160 }}
                value={stageFilter}
                onChange={(e) => setStageFilter(e.target.value as LeadStage | 'all')}
              >
                <option value="all">All Categories ({crmContacts.length})</option>
                {STAGES.map(s => {
                  const count = crmContacts.filter(c => c.stage === s.stage).length;
                  return (
                    <option key={s.stage} value={s.stage}>
                      {s.label} ({count})
                    </option>
                  );
                })}
              </select>
            </div>

            {/* Group By Selector: None | Company | Title | Company & Title */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <></>
              <select
                className="input"
                style={{ height: 36, fontSize: '0.8rem', width: 'auto', minWidth: 190 }}
                value={groupBy}
                onChange={(e) => setGroupBy(e.target.value as 'none' | 'company' | 'title' | 'company_title')}
              >
                <option value="none">Group by: None (Table View)</option>
                <option value="company">Group by: Company</option>
                <option value="title">Group by: Title / Designation</option>
                <option value="company_title">Group by: Company & Title</option>
              </select>
            </div>

            {/* Reset Filter Button if active */}
            {stageFilter !== 'all' && (
              <button 
                className="btn btn-ghost btn-sm" 
                onClick={() => setStageFilter('all')}
                style={{ height: 34, fontSize: '0.75rem', color: 'var(--accent)' }}
              >
                Clear Category Filter 
              </button>
            )}
          </>
        )}
      </div>

      {/* Active Filter Indicator Badge */}
      {activeTab === 'contacts' && stageFilter !== 'all' && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.8rem' }}>
          <span style={{ color: 'var(--text-muted)' }}>Filtering by Category:</span>
          <span className={`badge ${leadStageColor(stageFilter)}`}>
            {STAGES.find(s => s.stage === stageFilter)?.label} ({filteredContacts.length} people)
          </span>
        </div>
      )}

      {/* --- Contacts View --- */}
      {activeTab === 'contacts' && (
        <div className="card" style={{ overflow: 'hidden' }}>
          {/* View Mode 1: Grouped by Company */}
          {groupBy === 'company' && (
            <div style={{ padding: '1.25rem' }}>
              {Object.entries(contactsByCompany).length > 0 ? (
                Object.entries(contactsByCompany).map(([company, contacts]) => (
                  <div key={company} style={{ marginBottom: '2rem' }}>
                    <div style={{ 
                      display: 'flex', 
                      alignItems: 'center', 
                      justifyContent: 'space-between',
                      borderBottom: '1px solid var(--border)', 
                      paddingBottom: '0.6rem',
                      marginBottom: '1rem'
                    }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <></>
                        <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                          {company}
                        </h3>
                      </div>
                      <span className="badge" style={{ fontSize: '0.75rem' }}>
                        {contacts.length} {contacts.length === 1 ? 'contact' : 'contacts'}
                      </span>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(310px, 1fr))', gap: '1rem' }}>
                      {contacts.map(c => renderContactCard(c))}
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                  No contacts found matching current filter or search criteria.
                </div>
              )}
            </div>
          )}

          {/* View Mode 2: Grouped by Title / Designation */}
          {groupBy === 'title' && (
            <div style={{ padding: '1.25rem' }}>
              {Object.entries(contactsByTitle).length > 0 ? (
                Object.entries(contactsByTitle).map(([title, contacts]) => (
                  <div key={title} style={{ marginBottom: '2rem' }}>
                    <div style={{ 
                      display: 'flex', 
                      alignItems: 'center', 
                      justifyContent: 'space-between',
                      borderBottom: '1px solid var(--border)', 
                      paddingBottom: '0.6rem',
                      marginBottom: '1rem'
                    }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <></>
                        <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                          {title}
                        </h3>
                      </div>
                      <span className="badge" style={{ fontSize: '0.75rem' }}>
                        {contacts.length} {contacts.length === 1 ? 'person' : 'people'}
                      </span>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(310px, 1fr))', gap: '1rem' }}>
                      {contacts.map(c => renderContactCard(c))}
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                  No contacts found matching current filter or search criteria.
                </div>
              )}
            </div>
          )}

          {/* View Mode 3: Grouped by Company & Title (Hierarchical) */}
          {groupBy === 'company_title' && (
            <div style={{ padding: '1.25rem' }}>
              {Object.entries(contactsByCompanyAndTitle).length > 0 ? (
                Object.entries(contactsByCompanyAndTitle).map(([company, titleGroups]) => {
                  const companyContactTotal = Object.values(titleGroups).reduce((sum, list) => sum + list.length, 0);
                  return (
                    <div key={company} style={{ marginBottom: '2.5rem', background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius)', padding: '1.25rem' }}>
                      {/* Company Super Header */}
                      <div style={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'space-between',
                        borderBottom: '2px solid var(--border)', 
                        paddingBottom: '0.75rem',
                        marginBottom: '1.25rem'
                      }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                          <></>
                          <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                            {company}
                          </h3>
                        </div>
                        <span className="badge" style={{ fontSize: '0.75rem', background: 'var(--bg-elevated)' }}>
                          {companyContactTotal} Total People
                        </span>
                      </div>

                      {/* Sub-groups by Title */}
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                        {Object.entries(titleGroups).map(([title, contacts]) => (
                          <div key={title} style={{ paddingLeft: '0.5rem' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.65rem' }}>
                              <></>
                              <span style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-secondary)' }}>
                                {title}
                              </span>
                              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>({contacts.length})</span>
                            </div>
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '0.85rem' }}>
                              {contacts.map(c => renderContactCard(c))}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })
              ) : (
                <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                  No contacts found matching current filter or search criteria.
                </div>
              )}
            </div>
          )}

          {/* View Mode 4: None (Standard Table View) */}
          {groupBy === 'none' && (
            <div style={{ overflowX: 'auto' }}>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Company & Title</th>
                    <th>Contact Info</th>
                    <th>Category / Stage</th>
                    <th>Notes & Intelligence</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredContacts.map(c => (
                    <tr 
                      key={c.id} 
                      onClick={() => setViewNotesContact(c)}
                      style={{ cursor: 'pointer' }}
                      className="hover-row"
                    >
                      <td>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{c.name}</div>
                      </td>
                      <td>
                        <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>{c.company}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{c.designation || '—'}</div>
                      </td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                          <></> {c.email || '—'}
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.1rem' }}>
                          <></> {c.phone || '—'}
                        </div>
                      </td>
                      <td onClick={e => e.stopPropagation()}>
                        <select
                          value={c.stage}
                          onChange={e => updateCrmContact(c.id, { stage: e.target.value as LeadStage })}
                          className={`badge ${leadStageColor(c.stage)}`}
                          style={{ border: 'none', cursor: 'pointer', outline: 'none', fontSize: '0.75rem' }}
                        >
                          {STAGES.map(s => <option key={s.stage} value={s.stage} style={{ background: '#000000', color: '#fff' }}>{s.label}</option>)}
                        </select>
                      </td>
                      <td>
                        <div 
                          style={{ 
                            fontSize: '0.75rem', 
                            color: 'var(--text-muted)', 
                            overflow: 'hidden', 
                            textOverflow: 'ellipsis', 
                            whiteSpace: 'nowrap', 
                            maxWidth: 240, 
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '0.35rem'
                          }}
                          onClick={() => setViewNotesContact(c)}
                          title="Click to view full notes in non-edit viewer"
                        >
                          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{c.notes || '—'}</span>
                          <></>
                        </div>
                      </td>
                      <td style={{ textAlign: 'right' }} onClick={e => e.stopPropagation()}>
                        <button 
                          className="btn btn-icon btn-ghost btn-sm" 
                          title="View Full Notes & Profile"
                          onClick={() => setViewNotesContact(c)}
                        >
                          <></>
                        </button>
                        <button 
                          className="btn btn-icon btn-ghost btn-sm" 
                          title="Edit Contact"
                          onClick={(e) => openEditContactModal(c, e)}
                        >
                          <></>
                        </button>
                        <button 
                          className="btn btn-icon btn-ghost btn-sm" 
                          title="Delete Contact"
                          onClick={(e) => handleDeleteContact(c.id, e)} 
                          style={{ color: '#000000' }}
                        >
                          <></>
                        </button>
                      </td>
                    </tr>
                  ))}
                  {filteredContacts.length === 0 && (
                    <tr>
                      <td colSpan={6} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                        No contacts found matching current filter or search criteria.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* --- Pipeline View --- */}
      {activeTab === 'pipeline' && (
        <>
          {/* Summary KPI Cards */}
          <div className="grid-stats" style={{ marginBottom: '1.5rem' }}>
            <div className="stat-card">
              <div className="stat-icon" style={{ background: 'rgba(0, 0, 0, 0.12)', color: '#000000' }}>
                <></>
              </div>
              <div>
                <div className="stat-value">{leads.length}</div>
                <div className="stat-label">Total Accounts</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon" style={{ background: 'rgba(0, 0, 0, 0.12)', color: '#000000' }}>
                <></>
              </div>
              <div>
                <div className="stat-value">{formatCurrency(totalPipelineValue, 'INR')}</div>
                <div className="stat-label">Active Pipeline Value</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon" style={{ background: 'rgba(0, 0, 0, 0.12)', color: '#000000' }}>
                <></>
              </div>
              <div>
                <div className="stat-value">{leads.filter(l => l.stage === 'pilot').length}</div>
                <div className="stat-label">Active Pilots / PoCs</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon" style={{ background: 'rgba(0, 0, 0, 0.12)', color: '#000000' }}>
                <></>
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
                    {stageLeads.map(lead => (
                      <div
                        key={lead.id}
                        onClick={() => setSelectedLeadId(lead.id)}
                        className="card hover-effect"
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
                            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#000000' }}>
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
                              <option key={st.stage} value={st.stage} style={{ background: '#000000', color: '#fff' }}>
                                → {st.label}
                              </option>
                            ))}
                          </select>
                        </div>
                      </div>
                    ))}

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

      {/* --- Full Non-Edit Notes View Modal --- */}
      {viewNotesContact && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 740, padding: '2rem', background: 'var(--bg-surface)', maxHeight: '90vh', overflowY: 'auto' }}>
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid var(--border)', paddingBottom: '1.25rem', marginBottom: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div style={{
                  width: 48, height: 48, borderRadius: '50%',
                  background: 'linear-gradient(135deg, #000000, #000000)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: '1.1rem', fontWeight: 700, color: '#ffffff', flexShrink: 0
                }}>
                  {getInitials(viewNotesContact.name)}
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
                    <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--text-primary)', margin: 0 }}>
                      {viewNotesContact.name}
                    </h2>
                    <span className={`badge ${leadStageColor(viewNotesContact.stage)}`}>
                      {STAGES.find(s => s.stage === viewNotesContact.stage)?.label}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.75rem', marginTop: '0.25rem', flexWrap: 'wrap' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <></> {viewNotesContact.company || 'Unknown Company'}
                    </span>
                    <span>•</span>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <></> {viewNotesContact.designation || 'No Title'}
                    </span>
                  </div>
                </div>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setViewNotesContact(null)}>
                <></>
              </button>
            </div>

            {/* Contact Details Bar */}
            <div style={{ 
              display: 'grid', 
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
              gap: '0.75rem', 
              background: 'var(--bg-elevated)', 
              padding: '0.85rem 1rem', 
              borderRadius: 'var(--radius)', 
              marginBottom: '1.5rem',
              fontSize: '0.8rem'
            }}>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.7rem' }}>Email Address</span>
                {viewNotesContact.email ? (
                  <a href={`mailto:${viewNotesContact.email}`} style={{ color: 'var(--accent)', fontWeight: 600, textDecoration: 'none' }}>
                    {viewNotesContact.email}
                  </a>
                ) : '—'}
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.7rem' }}>Phone</span>
                {viewNotesContact.phone ? (
                  <a href={`tel:${viewNotesContact.phone}`} style={{ color: 'var(--text-primary)', fontWeight: 600, textDecoration: 'none' }}>
                    {viewNotesContact.phone}
                  </a>
                ) : '—'}
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.7rem' }}>Created</span>
                <span style={{ color: 'var(--text-secondary)' }}>
                  {viewNotesContact.createdAt ? new Date(viewNotesContact.createdAt).toLocaleDateString() : 'Recent'}
                </span>
              </div>
            </div>

            {/* Full Non-Edit Notes Section */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <></>
                  <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    Strategic Notes & Engagement History (Read-Only)
                  </h3>
                </div>
                {viewNotesContact.notes && (
                  <button 
                    className="btn btn-ghost btn-sm" 
                    onClick={() => copyNotesToClipboard(viewNotesContact.notes)}
                    style={{ fontSize: '0.75rem', height: 30 }}
                  >
                    {copiedNotes ? <></> : <></>}
                    <span>{copiedNotes ? 'Copied!' : 'Copy Notes'}</span>
                  </button>
                )}
              </div>

              {viewNotesContact.notes ? (
                <div 
                  className="markdown-body" 
                  style={{ 
                    background: 'var(--bg-base)', 
                    border: '1px solid var(--border)', 
                    borderRadius: 'var(--radius)', 
                    padding: '1.25rem 1.5rem',
                    color: 'var(--text-primary)',
                    fontSize: '0.9rem',
                    lineHeight: 1.6,
                    minHeight: 120,
                    maxHeight: '45vh',
                    overflowY: 'auto'
                  }}
                >
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {viewNotesContact.notes}
                  </ReactMarkdown>
                </div>
              ) : (
                <div style={{ 
                  background: 'var(--bg-base)', 
                  border: '1px dashed var(--border)', 
                  borderRadius: 'var(--radius)', 
                  padding: '2rem', 
                  textAlign: 'center', 
                  color: 'var(--text-muted)',
                  fontSize: '0.85rem'
                }}>
                  No notes recorded for this contact yet. Click &quot;Edit Contact &amp; Notes&quot; below to add intelligence.
                </div>
              )}
            </div>

            {/* Footer Action Bar */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.75rem', borderTop: '1px solid var(--border)', paddingTop: '1.25rem' }}>
              <button 
                className="btn btn-secondary" 
                onClick={() => {
                  const target = viewNotesContact;
                  setViewNotesContact(null);
                  openEditContactModal(target);
                }}
              >
                <></> Edit Contact &amp; Notes
              </button>
              <button className="btn btn-primary" onClick={() => setViewNotesContact(null)}>
                Close Viewer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- Admin Master Database Export Modal --- */}
      {isExportModalOpen && isAdmin && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 640, padding: '2rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{ 
                  width: 44, height: 44, borderRadius: 'var(--radius)', 
                  background: 'linear-gradient(135deg, #000000, #000000)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' 
                }}>
                  <></>
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>Export All Website Data</h3>
                    <span className="badge" style={{ background: '#000000', color: '#fff', fontSize: '0.65rem' }}>
                      Admin Access
                    </span>
                  </div>
                  <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                    Export every stored entity across all 12 modules into a comprehensive CSV database.
                  </p>
                </div>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsExportModalOpen(false)}>
                <></>
              </button>
            </div>

            {/* Data Inventory Breakdown */}
            <div style={{ 
              display: 'grid', 
              gridTemplateColumns: 'repeat(3, 1fr)', 
              gap: '0.65rem', 
              background: 'var(--bg-elevated)', 
              padding: '1rem', 
              borderRadius: 'var(--radius)', 
              marginBottom: '1.5rem' 
            }}>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>CRM Contacts</span>
                <strong style={{ fontSize: '0.95rem' }}>{crmContacts.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Sales Leads</span>
                <strong style={{ fontSize: '0.95rem' }}>{leads.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Team Roster</span>
                <strong style={{ fontSize: '0.95rem' }}>{team.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Active Projects</span>
                <strong style={{ fontSize: '0.95rem' }}>{projects.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Tasks &amp; Issues</span>
                <strong style={{ fontSize: '0.95rem' }}>{tasks.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Knowledge Docs</span>
                <strong style={{ fontSize: '0.95rem' }}>{wikiDocs.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Meetings &amp; Minutes</span>
                <strong style={{ fontSize: '0.95rem' }}>{meetings.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Investors</span>
                <strong style={{ fontSize: '0.95rem' }}>{investors.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>R&amp;D Projects</span>
                <strong style={{ fontSize: '0.95rem' }}>{rdProjects.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Goals &amp; OKRs</span>
                <strong style={{ fontSize: '0.95rem' }}>{goals.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Daily Progress Logs</span>
                <strong style={{ fontSize: '0.95rem' }}>{progressLogs.length}</strong>
              </div>
              <div style={{ fontSize: '0.75rem' }}>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Company Financials</span>
                <strong style={{ fontSize: '0.95rem' }}>{financeData ? 'Configured' : 'Default'}</strong>
              </div>
            </div>

            {/* Big Download Master CSV Button */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <button 
                className="btn btn-primary" 
                onClick={handleExportAllWebsiteData}
                style={{ 
                  padding: '0.85rem 1.25rem', 
                  fontSize: '0.95rem', 
                  fontWeight: 700, 
                  background: 'linear-gradient(135deg, #000000 0%, #000000 100%)',
                  justifyContent: 'center',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  border: 'none',
                  boxShadow: '0 4px 14px rgba(0, 0, 0, 0.35)',
                  cursor: 'pointer'
                }}
              >
                <></>
                <span>Download Complete Master CSV (All 12 Modules)</span>
              </button>

              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                <button 
                  className="btn btn-secondary btn-sm" 
                  onClick={() => { handleExportContactsOnly(); setIsExportModalOpen(false); }}
                >
                  <></> Export CRM Contacts Only
                </button>
                <button 
                  className="btn btn-secondary btn-sm" 
                  onClick={() => setIsExportModalOpen(false)}
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* --- Contacts Add/Edit Modal --- */}
      {isContactModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 520, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>{editingContact ? 'Edit Contact & Notes' : 'Add New Contact'}</h3>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsContactModalOpen(false)}>
                <></>
              </button>
            </div>
            
            <form onSubmit={handleContactSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Name *</label>
                  <input className="input" placeholder="e.g. Shashank A" required value={contactForm.name} onChange={e => setContactForm({...contactForm, name: e.target.value})} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Company *</label>
                  <input className="input" placeholder="e.g. Tesla" required value={contactForm.company} onChange={e => setContactForm({...contactForm, company: e.target.value})} />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Email</label>
                  <input type="email" className="input" placeholder="shashank@tesla.com" value={contactForm.email} onChange={e => setContactForm({...contactForm, email: e.target.value})} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Phone</label>
                  <input type="tel" className="input" placeholder="+1 234 567 8900" value={contactForm.phone} onChange={e => setContactForm({...contactForm, phone: e.target.value})} />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Designation / Title</label>
                  <input className="input" placeholder="e.g. CETO or VP Engineering" value={contactForm.designation} onChange={e => setContactForm({...contactForm, designation: e.target.value})} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Category / Stage</label>
                  <select className="input" value={contactForm.stage} onChange={e => setContactForm({...contactForm, stage: e.target.value as LeadStage})}>
                    {STAGES.map(s => <option key={s.stage} value={s.stage}>{s.label}</option>)}
                  </select>
                </div>
              </div>
              
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                  Notes &amp; Strategic Intelligence (Markdown Supported)
                </label>
                <textarea 
                  className="input" 
                  rows={5} 
                  placeholder="# Background&#10;- Discussed Edge Telematics MVP&#10;- Next step: Pilot agreement" 
                  value={contactForm.notes} 
                  onChange={e => setContactForm({...contactForm, notes: e.target.value})} 
                  style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}
                />
              </div>
              
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsContactModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editingContact ? 'Save Changes' : 'Add Contact'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* --- Pipeline Add Lead Modal --- */}
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

      {/* --- Pipeline Lead Detail Drawer / Modal --- */}
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
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#000000' }}>
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
                    <div style={{ fontSize: '0.75rem', color: '#000000', marginTop: '0.2rem' }}>
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
    </div>
  );
}
