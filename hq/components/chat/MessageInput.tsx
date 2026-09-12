'use client';

import { useState, useRef } from 'react';
import { useHQStore } from '@/lib/store';

import { ChatReference } from '@/lib/types';

export default function MessageInput({ channelId }: { channelId: string }) {
  const [content, setContent] = useState('');
  const [references, setReferences] = useState<ChatReference[]>([]);
  const { sendChatMessage, tasks, projects, wikiDocs } = useHQStore();
  
  const [showRefMenu, setShowRefMenu] = useState(false);
  const [refSearchTerm, setRefSearchTerm] = useState('');
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const handleSend = async () => {
    if (!content.trim() && references.length === 0) return;
    
    const text = content;
    const refs = references;
    setContent('');
    setReferences([]);
    setShowRefMenu(false);

    const res = await sendChatMessage(text, refs, channelId);
    if (res && !res.success) {
      alert(`Message failed to send: ${res.error || 'Please check connection'}`);
      setContent(text);
      setReferences(refs);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const val = e.target.value;
    setContent(val);
    
    // Slash command detection
    if (val.endsWith('/') || val.endsWith('@')) {
      setShowRefMenu(true);
      setRefSearchTerm('');
    } else if (showRefMenu) {
      const parts = val.split(/[\/@]/);
      setRefSearchTerm(parts[parts.length - 1].toLowerCase());
    }
    
    if (val === '' || (!val.includes('/') && !val.includes('@') && showRefMenu && !refSearchTerm)) {
      setShowRefMenu(false);
    }
  };

  const addReference = (ref: ChatReference) => {
    if (!references.some(r => r.id === ref.id)) {
      setReferences([...references, ref]);
    }
    setContent(content.replace(/[\/@][\w\s]*$/, '').trim() + ' ');
    setShowRefMenu(false);
    inputRef.current?.focus();
  };

  const filteredTasks = tasks.filter(t => t.title.toLowerCase().includes(refSearchTerm)).slice(0, 4);
  const filteredProjects = projects.filter(p => p.name.toLowerCase().includes(refSearchTerm)).slice(0, 4);
  const filteredWiki = wikiDocs.filter(w => w.title.toLowerCase().includes(refSearchTerm)).slice(0, 4);

  return (
    <div style={{ 
      padding: '0.75rem 1rem', borderTop: '1px solid var(--border)', 
      background: 'var(--bg-surface)', flexShrink: 0 
    }}>
      {/* Attached Reference Chips */}
      {references.length > 0 && (
        <div style={{ display: 'flex', gap: '0.375rem', marginBottom: '0.5rem', flexWrap: 'wrap' }}>
          {references.map((ref, idx) => (
            <div 
              key={idx} 
              style={{ 
                display: 'flex', alignItems: 'center', gap: '0.35rem', 
                padding: '0.2rem 0.6rem', background: 'var(--surface-hover)', 
                border: '1px solid var(--border)', borderRadius: '16px', 
                fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-primary)' 
              }}
            >
              <span>{ref.title}</span>
              <button 
                onClick={() => setReferences(references.filter((_, i) => i !== idx))} 
                style={{ border: 'none', background: 'none', cursor: 'pointer', padding: 0, display: 'flex', color: 'var(--text-muted)' }}
              >
                <></>
              </button>
            </div>
          ))}
        </div>
      )}
      
      <div style={{ position: 'relative' }}>
        {/* References Menu Popover */}
        {showRefMenu && (
          <div style={{ 
            position: 'absolute', bottom: 'calc(100% + 8px)', left: 0, 
            width: '100%', maxWidth: '340px', maxHeight: '280px', 
            overflowY: 'auto', background: 'var(--bg-card)', border: '1px solid var(--border)', 
            borderRadius: '12px', boxShadow: '0 8px 24px rgba(0,0,0,0.18)', zIndex: 100 
          }}>
            <div style={{ 
              padding: '0.5rem 0.75rem', fontSize: '0.75rem', fontWeight: 700, 
              color: 'var(--text-secondary)', borderBottom: '1px solid var(--border)',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center'
            }}>
              <span>Attach Context</span>
              <button onClick={() => setShowRefMenu(false)} style={{ border: 'none', background: 'none', cursor: 'pointer', color: 'var(--text-muted)' }}>
                <></>
              </button>
            </div>

            {filteredTasks.length > 0 && (
              <div style={{ padding: '0.25rem 0' }}>
                <div style={{ padding: '0.25rem 0.75rem', fontSize: '0.65rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 700 }}>
                  Tasks
                </div>
                {filteredTasks.map(t => (
                  <div 
                    key={t.id} 
                    onClick={() => addReference({ type: 'task', id: t.id, title: t.title })} 
                    style={{ padding: '0.4rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }} 
                    className="hover-bg"
                  >
                    <></>
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</span>
                  </div>
                ))}
              </div>
            )}

            {filteredProjects.length > 0 && (
              <div style={{ padding: '0.25rem 0' }}>
                <div style={{ padding: '0.25rem 0.75rem', fontSize: '0.65rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 700 }}>
                  Projects
                </div>
                {filteredProjects.map(p => (
                  <div 
                    key={p.id} 
                    onClick={() => addReference({ type: 'project', id: p.id, title: p.name })} 
                    style={{ padding: '0.4rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }} 
                    className="hover-bg"
                  >
                    <></>
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.name}</span>
                  </div>
                ))}
              </div>
            )}

            {filteredWiki.length > 0 && (
              <div style={{ padding: '0.25rem 0' }}>
                <div style={{ padding: '0.25rem 0.75rem', fontSize: '0.65rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 700 }}>
                  Knowledge Base
                </div>
                {filteredWiki.map(w => (
                  <div 
                    key={w.id} 
                    onClick={() => addReference({ type: 'wiki', id: w.id, title: w.title })} 
                    style={{ padding: '0.4rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }} 
                    className="hover-bg"
                  >
                    <></>
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{w.title}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        
        {/* Main Input Box */}
        <div style={{ 
          display: 'flex', alignItems: 'flex-end', gap: '0.5rem', 
          border: '1px solid var(--border)', borderRadius: '12px', 
          padding: '0.4rem 0.6rem', background: 'var(--bg-default)',
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
        }}>
          <button
            type="button"
            onClick={() => setShowRefMenu(!showRefMenu)}
            title="Attach task, project, or doc"
            style={{ 
              background: 'none', border: 'none', cursor: 'pointer', 
              color: showRefMenu ? 'var(--primary)' : 'var(--text-muted)', 
              padding: '6px', borderRadius: '6px', display: 'flex', alignItems: 'center', flexShrink: 0 
            }}
            className="hover-bg"
          >
            <></>
          </button>

          <textarea
            ref={inputRef}
            value={content}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder="Type a message... (or '/' to attach)"
            style={{ 
              flex: 1, border: 'none', background: 'none', resize: 'none', 
              maxHeight: '120px', minHeight: '36px', outline: 'none', fontSize: '0.9rem',
              color: 'var(--text-primary)', fontFamily: 'inherit', padding: '6px 2px'
            }}
            rows={1}
          />

          <button 
            onClick={handleSend}
            disabled={!content.trim() && references.length === 0}
            className="btn btn-primary"
            style={{ 
              padding: '6px 12px', borderRadius: '8px', 
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0, minWidth: '40px', minHeight: '36px' 
            }}
          >
            <></>
          </button>
        </div>
      </div>
    </div>
  );
}
