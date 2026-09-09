'use client';

import { use, useEffect, useState } from 'react';
import { useHQStore } from '@/lib/store';
import MessageList from '@/components/chat/MessageList';
import MessageInput from '@/components/chat/MessageInput';
import { Hash, ArrowLeft, Trash2, Users } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

export default function ChannelPage({ params }: { params: Promise<{ channelId: string }> }) {
  const resolvedParams = use(params);
  const { channelId } = resolvedParams;
  const router = useRouter();
  const { chatChannels, fetchChatMessages, fetchChatChannels, currentUser, team, deleteChatChannel } = useHQStore();
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    if (chatChannels.length === 0 && currentUser?.id) {
      fetchChatChannels();
    }
    
    fetchChatMessages(channelId);

    const interval = setInterval(() => {
      fetchChatMessages(channelId);
    }, 3000);

    return () => clearInterval(interval);
  }, [channelId, fetchChatMessages, fetchChatChannels, chatChannels.length, currentUser?.id]);

  const channel = chatChannels.find(c => c.id === channelId);

  let title = 'Chat';
  let subtitle = '';
  const isDirect = channel ? channel.type === 'direct' : channelId.startsWith('dm-');

  if (channel) {
    if (channel.type === 'group') {
      title = `#${channel.name}`;
      subtitle = channel.description || 'Public channel';
    } else {
      const otherMemberId = channel.memberIds.find(id => id !== currentUser?.id) || channel.createdBy;
      const otherMember = team.find(t => t.id === otherMemberId);
      title = otherMember?.name || (otherMemberId === currentUser?.id ? `${currentUser?.name} (You)` : 'Direct Message');
      subtitle = otherMember?.designation || otherMember?.role || 'Direct Message';
    }
  } else {
    title = 'Chat Room';
  }

  const canDeleteChannel = channel && (currentUser?.role === 'c_level' || channel.createdBy === currentUser?.id);

  const handleDeleteChannel = async () => {
    const confirmName = channel?.name || 'this channel';
    if (!window.confirm(`Are you sure you want to delete "${confirmName}"? All messages will be permanently deleted.`)) {
      return;
    }

    setIsDeleting(true);
    const res = await deleteChatChannel(channelId);
    setIsDeleting(false);

    if (res.success) {
      router.push('/chat');
    } else {
      alert(res.error || 'Failed to delete channel');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      {/* Top Header */}
      <div style={{ 
        padding: '0.75rem 1rem', borderBottom: '1px solid var(--border)', 
        background: 'var(--bg-surface)', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        flexShrink: 0
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', overflow: 'hidden' }}>
          {/* Mobile Back button */}
          <Link 
            href="/chat" 
            className="mobile-only"
            style={{ 
              display: 'flex', alignItems: 'center', justifyContent: 'center', 
              width: 32, height: 32, borderRadius: '6px', background: 'var(--bg-hover)', 
              color: 'var(--text-primary)', textDecoration: 'none', flexShrink: 0
            }}
          >
            <ArrowLeft size={18} />
          </Link>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', overflow: 'hidden' }}>
            {isDirect ? (
              <div style={{ width: 10, height: 10, borderRadius: '50%', background: 'var(--success)', flexShrink: 0 }} />
            ) : (
              <Hash size={20} style={{ color: 'var(--primary)', flexShrink: 0 }} />
            )}
            <div style={{ overflow: 'hidden' }}>
              <div style={{ fontWeight: 700, fontSize: '0.95rem', lineHeight: 1.2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {title}
              </div>
              {subtitle && (
                <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {subtitle}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Action buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          {canDeleteChannel && (
            <button
              onClick={handleDeleteChannel}
              disabled={isDeleting}
              title="Delete Channel"
              style={{
                display: 'flex', alignItems: 'center', gap: '0.35rem',
                background: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)',
                border: '1px solid rgba(239, 68, 68, 0.2)', borderRadius: '6px',
                padding: '5px 10px', fontSize: '0.75rem', fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              <Trash2 size={13} />
              <span className="desktop-only">{isDeleting ? 'Deleting...' : 'Delete'}</span>
            </button>
          )}
        </div>
      </div>
      
      <MessageList channelId={channelId} />
      <MessageInput channelId={channelId} />
    </div>
  );
}
