'use client';

import { ChatMessage, ChatReference, TeamMember } from '@/lib/types';
import { useHQStore } from '@/lib/store';
import { getInitials } from '@/lib/utils';
import { format } from 'date-fns';
import { CheckSquare, FolderKanban, BookOpen } from 'lucide-react';
import Link from 'next/link';

function ReferenceCard({ reference }: { reference: ChatReference }) {
  let icon = <CheckSquare size={16} />;
  let href = `/tasks`;
  if (reference.type === 'project') {
    icon = <FolderKanban size={16} />;
    href = `/projects`;
  } else if (reference.type === 'wiki') {
    icon = <BookOpen size={16} />;
    href = `/wiki`;
  }

  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center', gap: '0.5rem', 
      padding: '0.5rem 0.75rem', marginTop: '0.25rem', marginBottom: '0.25rem',
      background: 'var(--bg-surface)', border: '1px solid var(--border)', 
      borderRadius: '8px', textDecoration: 'none', color: 'var(--text-primary)'
    }}>
      {icon}
      <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>{reference.title}</span>
    </div>
  );
}

export default function MessageList({ channelId }: { channelId: string }) {
  const { chatMessages, team } = useHQStore();
  const messages = chatMessages[channelId] || [];

  if (messages.length === 0) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
        No messages yet. Send a message to start the conversation!
      </div>
    );
  }

  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: '1rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
      {messages.map((msg, index) => {
        const sender = team.find(t => t.id === msg.senderId);
        const prevMsg = index > 0 ? messages[index - 1] : null;
        const isConsecutive = prevMsg && prevMsg.senderId === msg.senderId && 
            (new Date(msg.createdAt).getTime() - new Date(prevMsg.createdAt).getTime() < 5 * 60000);

        return (
          <div key={msg.id} style={{ display: 'flex', gap: '0.75rem', marginTop: isConsecutive ? '-0.5rem' : '0' }}>
            {!isConsecutive ? (
              <div className="avatar" style={{ background: 'var(--primary)', color: 'white', width: 32, height: 32, flexShrink: 0, fontSize: '0.8rem' }}>
                {getInitials(sender?.name || 'Unknown User')}
              </div>
            ) : (
              <div style={{ width: 32, flexShrink: 0 }}></div>
            )}
            
            <div style={{ flex: 1 }}>
              {!isConsecutive && (
                <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem', marginBottom: '0.25rem' }}>
                  <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>{sender?.name || 'Unknown User'}</span>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                    {format(new Date(msg.createdAt), 'MMM d, h:mm a')}
                  </span>
                </div>
              )}
              <div style={{ fontSize: '0.9rem', color: 'var(--text-primary)', whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>
                {msg.content}
              </div>
              {msg.references && msg.references.length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', marginTop: '0.25rem' }}>
                  {msg.references.map(ref => (
                    <ReferenceCard key={ref.id} reference={ref} />
                  ))}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
