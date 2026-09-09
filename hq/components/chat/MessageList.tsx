'use client';

import { useRef, useEffect, useState } from 'react';
import { ChatMessage, ChatReference } from '@/lib/types';
import { useHQStore } from '@/lib/store';
import { getInitials } from '@/lib/utils';
import { format } from 'date-fns';
import { CheckSquare, FolderKanban, BookOpen, Trash2, MessageSquare, ExternalLink } from 'lucide-react';
import Link from 'next/link';

function ReferenceCard({ reference }: { reference: ChatReference }) {
  let icon = <CheckSquare size={14} className="text-primary" />;
  let href = `/tasks`;
  let badgeText = 'Task';
  let badgeColor = 'rgba(234, 88, 12, 0.1)';
  let textColor = 'var(--primary)';

  if (reference.type === 'project') {
    icon = <FolderKanban size={14} style={{ color: '#3b82f6' }} />;
    href = `/projects`;
    badgeText = 'Project';
    badgeColor = 'rgba(59, 130, 246, 0.1)';
    textColor = '#3b82f6';
  } else if (reference.type === 'wiki') {
    icon = <BookOpen size={14} style={{ color: '#10b981' }} />;
    href = `/wiki`;
    badgeText = 'Wiki';
    badgeColor = 'rgba(16, 185, 129, 0.1)';
    textColor = '#10b981';
  }

  return (
    <Link 
      href={href}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: '0.5rem', 
        padding: '0.4rem 0.75rem', marginTop: '0.25rem', marginBottom: '0.25rem',
        background: 'var(--bg-surface)', border: '1px solid var(--border)', 
        borderRadius: '8px', textDecoration: 'none', color: 'var(--text-primary)',
        fontSize: '0.825rem', maxWidth: '100%', overflow: 'hidden'
      }}
      className="hover-bg"
    >
      <span style={{ 
        display: 'inline-flex', alignItems: 'center', gap: '3px',
        padding: '2px 6px', borderRadius: '4px', background: badgeColor, 
        color: textColor, fontSize: '0.675rem', fontWeight: 700 
      }}>
        {icon} {badgeText}
      </span>
      <span style={{ fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {reference.title}
      </span>
      <ExternalLink size={12} style={{ color: 'var(--text-muted)', marginLeft: 'auto', flexShrink: 0 }} />
    </Link>
  );
}

export default function MessageList({ channelId }: { channelId: string }) {
  const { chatMessages, team, currentUser, deleteChatMessage } = useHQStore();
  const messages = chatMessages[channelId] || [];
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  const handleDelete = async (messageId: string) => {
    if (!window.confirm('Delete this message?')) return;
    setDeletingId(messageId);
    await deleteChatMessage(messageId, channelId);
    setDeletingId(null);
  };

  if (messages.length === 0) {
    return (
      <div style={{ 
        flex: 1, display: 'flex', flexDirection: 'column', 
        alignItems: 'center', justifyContent: 'center', padding: '2rem',
        textAlign: 'center', color: 'var(--text-muted)' 
      }}>
        <div style={{ 
          width: 54, height: 54, borderRadius: '16px', background: 'var(--surface-hover)', 
          display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1rem',
          color: 'var(--primary)' 
        }}>
          <MessageSquare size={26} />
        </div>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.375rem' }}>
          No messages here yet
        </h3>
        <p style={{ fontSize: '0.85rem', maxWidth: '340px', lineHeight: 1.5 }}>
          Send a message to kick off the conversation! Use <code style={{ background: 'var(--bg-hover)', padding: '2px 4px', borderRadius: '4px' }}>/</code> to link tasks or documents.
        </p>
      </div>
    );
  }

  return (
    <div style={{ 
      flex: 1, overflowY: 'auto', padding: '1rem', display: 'flex', 
      flexDirection: 'column', gap: '0.25rem' 
    }}>
      {messages.map((msg, index) => {
        const sender = team.find(t => t.id === msg.senderId);
        const isCurrentUser = msg.senderId === currentUser?.id;
        const prevMsg = index > 0 ? messages[index - 1] : null;
        
        // Consecutive messages within 4 minutes
        const isConsecutive = prevMsg && 
          prevMsg.senderId === msg.senderId && 
          (new Date(msg.createdAt).getTime() - new Date(prevMsg.createdAt).getTime() < 4 * 60000);

        const canDelete = isCurrentUser;

        return (
          <div 
            key={msg.id} 
            className="chat-msg-row"
            style={{ 
              paddingTop: isConsecutive ? '2px' : '8px',
              paddingBottom: '4px',
            }}
          >
            {/* Avatar (only on non-consecutive) */}
            {!isConsecutive ? (
              <div 
                style={{ 
                  width: 36, height: 36, borderRadius: '10px', 
                  background: isCurrentUser ? 'var(--primary)' : 'var(--bg-card)', 
                  color: isCurrentUser ? 'white' : 'var(--text-primary)',
                  border: isCurrentUser ? 'none' : '1px solid var(--border)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: '0.8rem', fontWeight: 700, flexShrink: 0 
                }}
              >
                {getInitials(sender?.name || (isCurrentUser ? currentUser?.name || 'Me' : 'User'))}
              </div>
            ) : (
              <div style={{ width: 36, flexShrink: 0 }} />
            )}

            {/* Message Body */}
            <div style={{ flex: 1, minWidth: 0 }}>
              {!isConsecutive && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem' }}>
                  <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-primary)' }}>
                    {sender?.name || (isCurrentUser ? `${currentUser?.name} (You)` : 'Team Member')}
                  </span>
                  {sender?.role === 'c_level' && (
                    <span style={{ 
                      fontSize: '0.625rem', padding: '1px 5px', borderRadius: '4px', 
                      background: 'rgba(234, 88, 12, 0.1)', color: 'var(--primary)', fontWeight: 700 
                    }}>
                      ADMIN
                    </span>
                  )}
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                    {format(new Date(msg.createdAt), 'MMM d, h:mm a')}
                  </span>
                </div>
              )}

              <div style={{ 
                fontSize: '0.9rem', color: 'var(--text-primary)', 
                whiteSpace: 'pre-wrap', wordBreak: 'break-word', lineHeight: 1.5 
              }}>
                {msg.content}
              </div>

              {msg.references && msg.references.length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', marginTop: '0.375rem' }}>
                  {msg.references.map(ref => (
                    <ReferenceCard key={ref.id} reference={ref} />
                  ))}
                </div>
              )}
            </div>

            {/* Action toolbar */}
            {canDelete && (
              <div className="chat-msg-actions">
                <button
                  onClick={() => handleDelete(msg.id)}
                  disabled={deletingId === msg.id}
                  title="Delete message"
                  style={{
                    background: 'none', border: 'none', cursor: 'pointer',
                    color: 'var(--text-muted)', padding: '4px', borderRadius: '4px',
                    display: 'flex', alignItems: 'center'
                  }}
                  className="hover-bg"
                >
                  <Trash2 size={13} style={{ color: 'var(--danger)' }} />
                </button>
              </div>
            )}
          </div>
        );
      })}
      <div ref={messagesEndRef} />
    </div>
  );
}
