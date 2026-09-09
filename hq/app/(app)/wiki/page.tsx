'use client';

import { BookOpen, FileText, Search, Plus, Edit2, Trash2, X, UploadCloud, ExternalLink, Loader2, Microscope, Cpu } from 'lucide-react';
import { useState, useRef } from 'react';
import { useHQStore } from '@/lib/store';
import type { WikiDoc } from '@/lib/types';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

const PRIMARY_CATEGORIES = ['Engineering', 'Research'];

export default function WikiPage() {
  const { wikiDocs, currentUser, addWikiDoc, updateWikiDoc, deleteWikiDoc } = useHQStore();
  const [search, setSearch] = useState('');
  const [selectedCategoryTab, setSelectedCategoryTab] = useState<string>('All');
  
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [editingDoc, setEditingDoc] = useState<WikiDoc | null>(null);
  const [viewingDoc, setViewingDoc] = useState<WikiDoc | null>(null);

  // Form state
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('Engineering');
  const [isCustomCategory, setIsCustomCategory] = useState(false);
  const [customCategoryName, setCustomCategoryName] = useState('');
  const [content, setContent] = useState('');
  const [readTime, setReadTime] = useState('');
  const [googleDriveUrl, setGoogleDriveUrl] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isAdmin = currentUser?.role === 'c_level';

  const openNewModal = (defaultCat = 'Engineering') => {
    setTitle('');
    setDescription('');
    setCategory(defaultCat);
    setIsCustomCategory(false);
    setCustomCategoryName('');
    setContent('');
    setReadTime('5 min read');
    setGoogleDriveUrl('');
    setEditingDoc(null);
    setIsModalOpen(true);
  };

  const openEditModal = (doc: WikiDoc, e: React.MouseEvent) => {
    e.stopPropagation();
    setTitle(doc.title);
    setDescription(doc.description);
    
    if (PRIMARY_CATEGORIES.includes(doc.category)) {
      setCategory(doc.category);
      setIsCustomCategory(false);
      setCustomCategoryName('');
    } else {
      setCategory(doc.category);
      setIsCustomCategory(true);
      setCustomCategoryName(doc.category);
    }

    setContent(doc.content);
    setReadTime(doc.readTime || '');
    setGoogleDriveUrl(doc.googleDriveUrl || '');
    setEditingDoc(doc);
    setIsModalOpen(true);
  };

  const openViewModal = (doc: WikiDoc) => {
    setViewingDoc(doc);
    setIsViewModalOpen(true);
  };

  const handleDelete = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (window.confirm('Are you sure you want to delete this document?')) {
      deleteWikiDoc(id);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const finalCategory = isCustomCategory ? customCategoryName.trim() : category.trim();
    if (!title.trim() || !finalCategory) return;

    if (editingDoc) {
      updateWikiDoc(editingDoc.id, {
        title: title.trim(),
        description: description.trim(),
        category: finalCategory,
        content: content.trim(),
        readTime: readTime.trim(),
        googleDriveUrl: googleDriveUrl.trim()
      });
    } else {
      addWikiDoc({
        title: title.trim(),
        description: description.trim(),
        category: finalCategory,
        content: content.trim(),
        readTime: readTime.trim() || '5 min read',
        googleDriveUrl: googleDriveUrl.trim(),
        createdBy: currentUser?.id || 'sys'
      });
    }
    setIsModalOpen(false);
  };

  // Group all documents by category
  const docsByCategory: Record<string, WikiDoc[]> = {
    Engineering: [],
    Research: [],
  };

  // Include custom categories if any exist
  wikiDocs.forEach(doc => {
    if (!docsByCategory[doc.category]) {
      docsByCategory[doc.category] = [];
    }
    docsByCategory[doc.category].push(doc);
  });

  const allCategories = Object.keys(docsByCategory);
  const displayedCategories = selectedCategoryTab === 'All' 
    ? allCategories 
    : allCategories.filter(c => c === selectedCategoryTab);

  return (
    <div className="page animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Documentation & Knowledge Base</h1>
          <p className="page-subtitle">
            Central repository for Engineering architecture, Research & AI/ML whitepapers, OEM deployment SOPs, and company playbooks.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="btn btn-primary" onClick={() => openNewModal('Engineering')}>
            <Plus size={15} />
            New Document
          </button>
        </div>
      </div>

      {/* Search & Category Filter Tabs */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '1.5rem' }}>
        <div style={{ position: 'relative', maxWidth: 420 }}>
          <Search size={14} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            className="input"
            placeholder="Search specs, research papers, guides…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ paddingLeft: '2.25rem', height: 38, fontSize: '0.85rem' }}
          />
        </div>

        {/* Category Tabs */}
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <button
            onClick={() => setSelectedCategoryTab('All')}
            className="btn btn-sm"
            style={{
              background: selectedCategoryTab === 'All' ? 'var(--primary)' : 'var(--bg-surface)',
              color: selectedCategoryTab === 'All' ? 'white' : 'var(--text-secondary)',
              border: '1px solid var(--border)',
              borderRadius: '20px',
              fontSize: '0.8rem',
              fontWeight: 600,
              padding: '4px 14px'
            }}
          >
            All Categories ({wikiDocs.length})
          </button>

          <button
            onClick={() => setSelectedCategoryTab('Engineering')}
            className="btn btn-sm"
            style={{
              display: 'flex', alignItems: 'center', gap: '5px',
              background: selectedCategoryTab === 'Engineering' ? 'var(--primary)' : 'var(--bg-surface)',
              color: selectedCategoryTab === 'Engineering' ? 'white' : 'var(--text-secondary)',
              border: '1px solid var(--border)',
              borderRadius: '20px',
              fontSize: '0.8rem',
              fontWeight: 600,
              padding: '4px 14px'
            }}
          >
            <Cpu size={13} /> Engineering ({docsByCategory['Engineering']?.length || 0})
          </button>

          <button
            onClick={() => setSelectedCategoryTab('Research')}
            className="btn btn-sm"
            style={{
              display: 'flex', alignItems: 'center', gap: '5px',
              background: selectedCategoryTab === 'Research' ? 'var(--primary)' : 'var(--bg-surface)',
              color: selectedCategoryTab === 'Research' ? 'white' : 'var(--text-secondary)',
              border: '1px solid var(--border)',
              borderRadius: '20px',
              fontSize: '0.8rem',
              fontWeight: 600,
              padding: '4px 14px'
            }}
          >
            <Microscope size={13} /> Research ({docsByCategory['Research']?.length || 0})
          </button>

          {allCategories.filter(c => !PRIMARY_CATEGORIES.includes(c)).map(cat => (
            <button
              key={cat}
              onClick={() => setSelectedCategoryTab(cat)}
              className="btn btn-sm"
              style={{
                background: selectedCategoryTab === cat ? 'var(--primary)' : 'var(--bg-surface)',
                color: selectedCategoryTab === cat ? 'white' : 'var(--text-secondary)',
                border: '1px solid var(--border)',
                borderRadius: '20px',
                fontSize: '0.8rem',
                fontWeight: 600,
                padding: '4px 14px'
              }}
            >
              {cat} ({docsByCategory[cat]?.length || 0})
            </button>
          ))}
        </div>
      </div>

      {/* Category Sections */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        {displayedCategories.map((cat) => {
          const items = docsByCategory[cat] || [];
          const filteredItems = items.filter(item => 
            item.title.toLowerCase().includes(search.toLowerCase()) || 
            item.description.toLowerCase().includes(search.toLowerCase())
          );

          if (search && filteredItems.length === 0) return null;

          const isEngineering = cat === 'Engineering';
          const isResearch = cat === 'Research';

          return (
            <div key={cat} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  {isEngineering && <Cpu size={18} style={{ color: 'var(--primary)' }} />}
                  {isResearch && <Microscope size={18} style={{ color: '#3b82f6' }} />}
                  <span>{cat}</span>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                    ({items.length} {items.length === 1 ? 'doc' : 'docs'})
                  </span>
                </h3>

                <button 
                  onClick={() => openNewModal(cat)}
                  className="btn btn-sm btn-ghost"
                  style={{ fontSize: '0.75rem', padding: '2px 8px' }}
                >
                  + Add to {cat}
                </button>
              </div>

              {filteredItems.length === 0 ? (
                <div 
                  className="card" 
                  style={{ 
                    padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)',
                    background: 'var(--bg-surface)', border: '1px dashed var(--border)' 
                  }}
                >
                  <p style={{ fontSize: '0.85rem', marginBottom: '0.75rem' }}>
                    {isResearch ? 'No Research papers or whitepapers added yet.' : `No documents under ${cat} yet.`}
                  </p>
                  <button 
                    onClick={() => openNewModal(cat)}
                    className="btn btn-sm btn-primary"
                    style={{ fontSize: '0.75rem' }}
                  >
                    + Create First {cat} Document
                  </button>
                </div>
              ) : (
                <div className="grid-auto" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1rem' }}>
                  {filteredItems.map((doc) => (
                    <div
                      key={doc.id}
                      className="card hover-effect"
                      onClick={() => openViewModal(doc)}
                      style={{
                        padding: '1.25rem',
                        cursor: 'pointer',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '0.5rem',
                        position: 'relative',
                        background: 'var(--bg-surface)',
                        border: '1px solid var(--border)',
                        borderRadius: '12px'
                      }}
                    >
                      <div style={{ position: 'absolute', top: '1rem', right: '1rem', display: 'flex', gap: '0.25rem' }}>
                        {(isAdmin || doc.createdBy === currentUser?.id) && (
                          <>
                            <button 
                              className="btn btn-icon btn-ghost btn-sm" 
                              onClick={(e) => openEditModal(doc, e)}
                              title="Edit"
                            >
                              <Edit2 size={14} />
                            </button>
                            <button 
                              className="btn btn-icon btn-ghost btn-sm" 
                              onClick={(e) => handleDelete(doc.id, e)} 
                              style={{ color: 'var(--danger)' }}
                              title="Delete"
                            >
                              <Trash2 size={14} />
                            </button>
                          </>
                        )}
                      </div>
                      
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', paddingRight: '3rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <FileText size={18} style={{ color: doc.googleDriveUrl ? '#34d399' : (isResearch ? '#3b82f6' : 'var(--primary)') }} />
                          {doc.googleDriveUrl && (
                            <span style={{ fontSize: '0.65rem', fontWeight: 600, color: '#34d399', background: 'rgba(52,211,153,0.1)', padding: '0.15rem 0.4rem', borderRadius: 4 }}>
                              Drive
                            </span>
                          )}
                        </div>
                        <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>{doc.readTime}</span>
                      </div>

                      <h4 style={{ fontSize: '0.925rem', fontWeight: 700, color: 'var(--text-primary)', lineHeight: 1.3 }}>
                        {doc.title}
                      </h4>

                      <p style={{ fontSize: '0.775rem', color: 'var(--text-secondary)', lineHeight: 1.4, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical' }}>
                        {doc.description}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* View Document Modal */}
      {isViewModalOpen && viewingDoc && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 800, padding: '2rem', background: 'var(--bg-surface)', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.25rem' }}>
              <div>
                <span className="badge" style={{ marginBottom: '0.5rem', background: viewingDoc.category === 'Research' ? 'rgba(59,130,246,0.15)' : 'rgba(234,88,12,0.15)', color: viewingDoc.category === 'Research' ? '#3b82f6' : 'var(--primary)' }}>
                  {viewingDoc.category}
                </span>
                <h3 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)', lineHeight: 1.2 }}>{viewingDoc.title}</h3>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
                  {viewingDoc.readTime} · Last updated {new Date(viewingDoc.updatedAt).toLocaleDateString()}
                </div>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsViewModalOpen(false)}>
                <X size={20} />
              </button>
            </div>
            <div style={{ padding: '1.5rem 0', borderTop: '1px solid var(--border)', borderBottom: '1px solid var(--border)', marginBottom: '1.5rem' }}>
              <p style={{ fontSize: '1.05rem', color: 'var(--text-secondary)', fontStyle: 'italic', marginBottom: '1.5rem' }}>{viewingDoc.description}</p>
              
              {viewingDoc.googleDriveUrl ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <a href={viewingDoc.googleDriveUrl} target="_blank" rel="noreferrer" className="btn btn-sm btn-secondary">
                      <ExternalLink size={14} /> Open in Google Drive
                    </a>
                  </div>
                  <div style={{ width: '100%', height: '65vh', minHeight: '500px', borderRadius: 'var(--radius)', overflow: 'hidden', border: '1px solid var(--border)', background: '#f8fafc' }}>
                    <iframe 
                      src={viewingDoc.googleDriveUrl.includes('drive.google.com') && !viewingDoc.googleDriveUrl.includes('/preview') 
                        ? viewingDoc.googleDriveUrl.replace(/\/view.*$/, '/preview') 
                        : viewingDoc.googleDriveUrl} 
                      style={{ border: 'none', display: 'block', width: '100%', height: '100%' }}
                      title="Google Drive Viewer"
                    />
                  </div>
                </div>
              ) : (
                <div className="markdown-body" style={{ color: 'var(--text-primary)', fontSize: '0.95rem' }}>
                  {viewingDoc.content ? (
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>
                      {viewingDoc.content}
                    </ReactMarkdown>
                  ) : 'No detailed content provided.'}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Add/Edit Modal */}
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
                  {editingDoc ? 'Edit Document' : 'New Knowledge Base Document'}
                </h3>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsModalOpen(false)}>
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Document Title *
                </label>
                <input
                  className="input"
                  placeholder="e.g. Telematics Edge Agent Architecture or CAN-bus ML Anomaly Research"
                  value={title}
                  onChange={e => setTitle(e.target.value)}
                  required
                  autoFocus
                />
              </div>

              {/* Category Selector: Choose Engineering or Research */}
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                  Choose Category *
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.625rem', marginBottom: '0.5rem' }}>
                  <button
                    type="button"
                    onClick={() => { setCategory('Engineering'); setIsCustomCategory(false); }}
                    style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
                      padding: '0.75rem', borderRadius: '10px', fontSize: '0.875rem', fontWeight: 700,
                      border: !isCustomCategory && category === 'Engineering' ? '2px solid var(--primary)' : '1px solid var(--border)',
                      background: !isCustomCategory && category === 'Engineering' ? 'rgba(234, 88, 12, 0.12)' : 'var(--bg-default)',
                      color: !isCustomCategory && category === 'Engineering' ? 'var(--primary)' : 'var(--text-primary)',
                      cursor: 'pointer', transition: 'all 0.15s ease'
                    }}
                  >
                    <Cpu size={16} /> Engineering
                  </button>

                  <button
                    type="button"
                    onClick={() => { setCategory('Research'); setIsCustomCategory(false); }}
                    style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
                      padding: '0.75rem', borderRadius: '10px', fontSize: '0.875rem', fontWeight: 700,
                      border: !isCustomCategory && category === 'Research' ? '2px solid #3b82f6' : '1px solid var(--border)',
                      background: !isCustomCategory && category === 'Research' ? 'rgba(59, 130, 246, 0.12)' : 'var(--bg-default)',
                      color: !isCustomCategory && category === 'Research' ? '#3b82f6' : 'var(--text-primary)',
                      cursor: 'pointer', transition: 'all 0.15s ease'
                    }}
                  >
                    <Microscope size={16} /> Research
                  </button>
                </div>

                {/* Or Custom Category toggle */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.25rem' }}>
                  <button
                    type="button"
                    onClick={() => setIsCustomCategory(!isCustomCategory)}
                    style={{
                      background: 'none', border: 'none', padding: 0,
                      fontSize: '0.725rem', color: 'var(--text-muted)', textDecoration: 'underline',
                      cursor: 'pointer'
                    }}
                  >
                    {isCustomCategory ? '← Back to Engineering / Research' : '+ Need a custom category?'}
                  </button>
                </div>

                {isCustomCategory && (
                  <div style={{ marginTop: '0.5rem' }}>
                    <input
                      className="input"
                      placeholder="Enter custom category name (e.g. Operations, Legal, SOPs)"
                      value={customCategoryName}
                      onChange={e => setCustomCategoryName(e.target.value)}
                      required={isCustomCategory}
                    />
                  </div>
                )}
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Estimated Read Time
                </label>
                <input
                  className="input"
                  placeholder="e.g. 5 min read"
                  value={readTime}
                  onChange={e => setReadTime(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Short Description *
                </label>
                <textarea
                  className="input"
                  rows={2}
                  placeholder="Brief summary of what this document covers..."
                  value={description}
                  onChange={e => setDescription(e.target.value)}
                  style={{ resize: 'vertical' }}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Google Drive / Cloud URL (optional)
                </label>
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                  <input
                    className="input"
                    placeholder="Paste Google Drive URL or PDF link..."
                    value={googleDriveUrl}
                    onChange={e => setGoogleDriveUrl(e.target.value)}
                    style={{ flex: 1 }}
                  />
                  <input 
                    type="file" 
                    ref={fileInputRef} 
                    style={{ display: 'none' }} 
                    onChange={(e) => {
                      if (e.target.files?.[0]) {
                        setIsUploading(true);
                        setTimeout(() => {
                          setGoogleDriveUrl('https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf');
                          setIsUploading(false);
                        }, 1500);
                      }
                    }}
                  />
                  <button 
                    type="button" 
                    className="btn btn-secondary"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={isUploading}
                  >
                    {isUploading ? <Loader2 size={15} className="animate-spin" /> : <UploadCloud size={15} />}
                    {isUploading ? 'Uploading...' : 'Upload File'}
                  </button>
                </div>
              </div>

              {!googleDriveUrl && (
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Detailed Content (Markdown Supported)
                  </label>
                  <textarea
                    className="input"
                    rows={8}
                    placeholder="Write detailed documentation, research methodology, findings, or architecture notes in Markdown..."
                    value={content}
                    onChange={e => setContent(e.target.value)}
                    style={{ resize: 'vertical', fontFamily: 'monospace' }}
                  />
                </div>
              )}
              
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  {editingDoc ? 'Save Changes' : 'Publish Document'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
