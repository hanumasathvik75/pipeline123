'use client';

import { useState, useRef, useEffect } from 'react';
import { useHQStore } from '@/lib/store';
import { Send, Hash, AtSign, Search } from 'lucide-react';
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
    
    // Simple slash command detection
    if (val.endsWith('/') || val.endsWith('@')) {
      setShowRefMenu(true);
      setRefSearchTerm('');
    } else if (showRefMenu) {
      const parts = val.split(/[\/@]/);
      setRefSearchTerm(parts[parts.length - 1].toLowerCase());
    }
    
    if (val === '' || (!val.includes('/') && !val.includes('@'))) {
      setShowRefMenu(false);
    }
  };

  const addReference = (ref: ChatReference) => {
    setReferences([...references, ref]);
    setContent(content.replace(/[\/@][\w\s]*$/, '') + ' '); // remove the search text
    setShowRefMenu(false);
    inputRef.current?.focus();
  };

  // Filter items for the reference menu
  const filteredTasks = tasks.filter(t => t.title.toLowerCase().includes(refSearchTerm)).slice(0, 3);
  const filteredProjects = projects.filter(p => p.name.toLowerCase().includes(refSearchTerm)).slice(0, 3);
  const filteredWiki = wikiDocs.filter(w => w.title.toLowerCase().includes(refSearchTerm)).slice(0, 3);

  return (
    <div style={{ padding: '1rem', borderTop: '1px solid var(--border)', background: 'var(--bg-card)' }}>
      {references.length > 0 && (
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem', flexWrap: 'wrap' }}>
          {references.map((ref, idx) => (
            <div key={idx} style={{ 
              display: 'flex', alignItems: 'center', gap: '0.25rem', 
              padding: '0.25rem 0.5rem', background: 'var(--surface-hover)', 
              borderRadius: '16px', fontSize: '0.7rem', fontWeight: 600 
            }}>
              {ref.title}
              <button onClick={() => setReferences(references.filter((_, i) => i !== idx))} style={{ border: 'none', background: 'none', cursor: 'pointer', padding: 0, marginLeft: '0.25rem' }}>✕</button>
            </div>
          ))}
        </div>
      )}
      
      <div style={{ position: 'relative' }}>
        {showRefMenu && (
          <div style={{ 
            position: 'absolute', bottom: '100%', left: 0, width: '300px', maxHeight: '300px', 
            overflowY: 'auto', background: 'var(--bg-surface)', border: '1px solid var(--border)', 
            borderRadius: '8px', boxShadow: 'var(--shadow-md)', zIndex: 10, marginBottom: '0.5rem' 
          }}>
            <div style={{ padding: '0.5rem', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}>
              Attach Reference
            </div>
            {filteredTasks.length > 0 && (
              <div style={{ padding: '0.25rem 0' }}>
                <div style={{ padding: '0.25rem 0.75rem', fontSize: '0.65rem', textTransform: 'uppercase', color: 'var(--text-muted)' }}>Tasks</div>
                {filteredTasks.map(t => (
                  <div key={t.id} onClick={() => addReference({ type: 'task', id: t.id, title: t.title })} style={{ padding: '0.5rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem' }} className="hover-bg">
                    {t.title}
                  </div>
                ))}
              </div>
            )}
             {filteredProjects.length > 0 && (
              <div style={{ padding: '0.25rem 0' }}>
                <div style={{ padding: '0.25rem 0.75rem', fontSize: '0.65rem', textTransform: 'uppercase', color: 'var(--text-muted)' }}>Projects</div>
                {filteredProjects.map(p => (
                  <div key={p.id} onClick={() => addReference({ type: 'project', id: p.id, title: p.name })} style={{ padding: '0.5rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem' }} className="hover-bg">
                    {p.name}
                  </div>
                ))}
              </div>
            )}
             {filteredWiki.length > 0 && (
              <div style={{ padding: '0.25rem 0' }}>
                <div style={{ padding: '0.25rem 0.75rem', fontSize: '0.65rem', textTransform: 'uppercase', color: 'var(--text-muted)' }}>Wiki</div>
                {filteredWiki.map(w => (
                  <div key={w.id} onClick={() => addReference({ type: 'wiki', id: w.id, title: w.title })} style={{ padding: '0.5rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem' }} className="hover-bg">
                    {w.title}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: '0.5rem', border: '1px solid var(--border)', borderRadius: '8px', padding: '0.5rem', background: 'var(--bg-surface)' }}>
          <textarea
            ref={inputRef}
            value={content}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder="Type a message or '/' to attach tasks..."
            style={{ 
              flex: 1, border: 'none', background: 'none', resize: 'none', 
              maxHeight: '150px', minHeight: '40px', outline: 'none', fontSize: '0.9rem',
              color: 'var(--text-primary)', fontFamily: 'inherit'
            }}
            rows={1}
          />
          <button 
            onClick={handleSend}
            disabled={!content.trim() && references.length === 0}
            className="btn btn-primary btn-icon"
            style={{ flexShrink: 0 }}
          >
            <Send size={16} />
          </button>
        </div>
      </div>
    </div>
  );
}
