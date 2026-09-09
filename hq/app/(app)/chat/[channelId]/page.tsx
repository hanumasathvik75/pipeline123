'use client';

import { use, useEffect } from 'react';
import { useHQStore } from '@/lib/store';
import MessageList from '@/components/chat/MessageList';
import MessageInput from '@/components/chat/MessageInput';
import { Hash } from 'lucide-react';

export default function ChannelPage({ params }: { params: Promise<{ channelId: string }> }) {
  const resolvedParams = use(params);
  const { channelId } = resolvedParams;
  const { chatChannels, fetchChatMessages, fetchChatChannels, currentUser, team } = useHQStore();

  useEffect(() => {
    // Ensure channels are fetched if not yet loaded
    if (chatChannels.length === 0 && currentUser?.id) {
      fetchChatChannels();
    }
    
    // Initial fetch of messages
    fetchChatMessages(channelId);

    // Poll for new messages every 3 seconds
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
      subtitle = channel.description || `${channel.memberIds?.length || 0} members`;
    } else {
      const otherMemberId = channel.memberIds.find(id => id !== currentUser?.id) || channel.createdBy;
      const otherMember = team.find(t => t.id === otherMemberId);
      title = otherMember?.name || (otherMemberId === currentUser?.id ? `${currentUser?.name} (You)` : 'Direct Message');
      subtitle = otherMember?.designation || otherMember?.role || 'Direct Message';
    }
  } else {
    title = channelId.startsWith('chan-') ? 'Loading Channel...' : 'Chat Channel';
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      <div style={{ 
        padding: '0.875rem 1.25rem', borderBottom: '1px solid var(--border)', 
        background: 'var(--bg-surface)', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        flexShrink: 0
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          {isDirect ? (
            <div style={{ width: 10, height: 10, borderRadius: '50%', background: 'var(--success)' }} />
          ) : (
            <Hash size={20} style={{ color: 'var(--primary)' }} />
          )}
          <div>
            <div style={{ fontWeight: 700, fontSize: '1rem', lineHeight: 1.2 }}>{title}</div>
            {subtitle && (
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{subtitle}</div>
            )}
          </div>
        </div>
      </div>
      
      <MessageList channelId={channelId} />
      <MessageInput channelId={channelId} />
    </div>
  );
}
