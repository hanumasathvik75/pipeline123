'use client';

import { BookOpen, FileText, Search, Plus, Edit2, Trash2, X, Eye, UploadCloud, ExternalLink, Loader2 } from 'lucide-react';
import { useState, useRef } from 'react';
import { useHQStore } from '@/lib/store';
import type { WikiDoc } from '@/lib/types';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export default function WikiPage() {
  const { wikiDocs, currentUser, addWikiDoc, updateWikiDoc, deleteWikiDoc } = useHQStore();
  const [search, setSearch] = useState('');
  
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [editingDoc, setEditingDoc] = useState<WikiDoc | null>(null);
  const [viewingDoc, setViewingDoc] = useState<WikiDoc | null>(null);

  // Form state
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [content, setContent] = useState('');
  const [readTime, setReadTime] = useState('');
  const [googleDriveUrl, setGoogleDriveUrl] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isAdmin = currentUser?.role === 'c_level';

  const openNewModal = () => {
    setTitle('');
    setDescription('');
    setCategory('Engineering');
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
    setCategory(doc.category);
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
      if(window.confirm('Are you sure you want to delete this document?')) {
          deleteWikiDoc(id);
      }
  };

  const handleSubmit = (e: React.FormEvent) => {
      e.preventDefault();
      if(!title.trim() || !category.trim()) return;

      if(editingDoc) {
          updateWikiDoc(editingDoc.id, {
              title: title.trim(),
              description: description.trim(),
              category: category.trim(),
              content: content.trim(),
              readTime: readTime.trim(),
              googleDriveUrl: googleDriveUrl.trim()
          });
      } else {
          addWikiDoc({
              title: title.trim(),
              description: description.trim(),
              category: category.trim(),
              content: content.trim(),
              readTime: readTime.trim() || '5 min read',
              googleDriveUrl: googleDriveUrl.trim(),
              createdBy: currentUser?.id || 'sys'
          });
      }
      setIsModalOpen(false);
  };

  // Group by category
  const docsByCategory = wikiDocs.reduce((acc, doc) => {
      if (!acc[doc.category]) {
          acc[doc.category] = [];
      }
      acc[doc.category].push(doc);
      return acc;
  }, {} as Record<string, WikiDoc[]>);

  const categories = Object.keys(docsByCategory).sort();

  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Documentation & Knowledge Base</h1>
          <p className="page-subtitle">
            Central repository for engineering architecture, R&D papers, OEM deployment SOPs, and company playbooks.
          </p>
        </div>
        <button className="btn btn-primary" onClick={openNewModal}>
          <Plus size={15} />
          New Document
        </button>
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

      {wikiDocs.length === 0 ? (
          <div className="card" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
              <BookOpen size={48} style={{ opacity: 0.2, margin: '0 auto 1rem' }} />
              <h3>No Documents Found</h3>
              <p>Create the first document for the knowledge base.</p>
          </div>
      ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
            {categories.map((cat, idx) => {
              const filteredItems = docsByCategory[cat].filter(item => 
                  item.title.toLowerCase().includes(search.toLowerCase()) || 
                  item.description.toLowerCase().includes(search.toLowerCase())
              );

              if (filteredItems.length === 0) return null;

              return (
              <div key={idx}>
                <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.75rem' }}>
                  {cat}
                </h3>
                <div className="grid-auto">
                  {filteredItems.map((doc, i) => (
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
                          position: 'relative'
                        }}
                      >
                        <div style={{ position: 'absolute', top: '1rem', right: '1rem', display: 'flex', gap: '0.25rem' }}>
                           {(isAdmin || doc.createdBy === currentUser?.id) && (
                               <>
                                <button className="btn btn-icon btn-ghost btn-sm" onClick={(e) => openEditModal(doc, e)}>
                                    <Edit2 size={14} />
                                </button>
                                <button className="btn btn-icon btn-ghost btn-sm" onClick={(e) => handleDelete(doc.id, e)} style={{ color: '#ef4444' }}>
                                    <Trash2 size={14} />
                                </button>
                               </>
                           )}
                        </div>
                        
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', paddingRight: '3rem' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <FileText size={18} style={{ color: doc.googleDriveUrl ? '#34d399' : '#818cf8' }} />
                            {doc.googleDriveUrl && <span style={{ fontSize: '0.65rem', fontWeight: 600, color: '#34d399', background: 'rgba(52,211,153,0.1)', padding: '0.15rem 0.4rem', borderRadius: 4 }}>Drive</span>}
                          </div>
                          <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>{doc.readTime}</span>
                        </div>

                        <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)', lineHeight: 1.3 }}>
                          {doc.title}
                        </h4>

                        <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', lineHeight: 1.4, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical' }}>
                          {doc.description}
                        </p>
                      </div>
                    ))}
                </div>
              </div>
            )})}
          </div>
      )}

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
                  <span className="badge" style={{ marginBottom: '0.5rem', background: 'rgba(99,102,241,0.15)', color: '#818cf8' }}>{viewingDoc.category}</span>
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
                 <p style={{ fontSize: '1.1rem', color: 'var(--text-secondary)', fontStyle: 'italic', marginBottom: '1.5rem' }}>{viewingDoc.description}</p>
                 
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

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                        Title *
                    </label>
                    <input
                        className="input"
                        placeholder="e.g. Edge Telematics Agent v2 Protocol"
                        value={title}
                        onChange={e => setTitle(e.target.value)}
                        required
                    />
                </div>

                <div className="grid-2">
                     <div>
                        <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                            Category *
                        </label>
                        <input
                            className="input"
                            placeholder="e.g. Architecture & Engineering"
                            value={category}
                            onChange={e => setCategory(e.target.value)}
                            required
                        />
                        <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>Or type a new category to create it.</div>
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
                </div>

                <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                        Short Description *
                    </label>
                    <textarea
                        className="input"
                        rows={2}
                        placeholder="Brief summary of the document..."
                        value={description}
                        onChange={e => setDescription(e.target.value)}
                        style={{ resize: 'vertical' }}
                        required
                    />
                </div>

                <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                        Google Drive Integration
                    </label>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                        <input
                            className="input"
                            placeholder="Paste Google Drive URL or upload file..."
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
                                    // Simulate network upload to drive
                                    setTimeout(() => {
                                        setGoogleDriveUrl('https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf');
                                        setIsUploading(false);
                                    }, 2000);
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
                            rows={10}
                            placeholder="Full document content goes here (if not using Google Drive)..."
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
