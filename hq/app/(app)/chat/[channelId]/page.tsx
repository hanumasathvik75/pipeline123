'use client';

import { use, useEffect } from 'react';
import { useHQStore } from '@/lib/store';
import MessageList from '@/components/chat/MessageList';
import MessageInput from '@/components/chat/MessageInput';
import { Hash } from 'lucide-react';

export default function ChannelPage({ params }: { params: Promise<{ channelId: string }> }) {
  const resolvedParams = use(params);
  const { channelId } = resolvedParams;
  const { chatChannels, fetchChatMessages, currentUser, team } = useHQStore();

  useEffect(() => {
    fetchChatMessages(channelId);
    
    // In a real app with Supabase Realtime enabled, you would set up
    // the subscription listener here to listen for new messages on this channel.
    // Example:
    // const sub = supabase.channel(`public:hq_chat_messages:channel_id=eq.${channelId}`)
    //   .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'hq_chat_messages' }, payload => {
    //       addChatMessage(payload.new as ChatMessage);
    //   }).subscribe();
    // return () => { sub.unsubscribe() }
  }, [channelId, fetchChatMessages]);

  const channel = chatChannels.find(c => c.id === channelId);

  let title = 'Loading...';
  if (channel) {
    if (channel.type === 'group') {
      title = channel.name;
    } else {
      const otherMemberId = channel.memberIds.find(id => id !== currentUser?.id);
      const otherMember = team.find(t => t.id === otherMemberId);
      title = otherMember?.name || 'Unknown User';
    }
  }

  return (
    <>
      <div style={{ 
        padding: '1rem', borderBottom: '1px solid var(--border)', 
        background: 'var(--bg-surface)', display: 'flex', alignItems: 'center', gap: '0.5rem',
        fontWeight: 700, fontSize: '1.1rem'
      }}>
        {channel?.type === 'group' ? <Hash size={20} color="var(--text-muted)" /> : <div style={{ width: 10, height: 10, borderRadius: '50%', background: 'var(--success)' }}></div>}
        {title}
      </div>
      
      <MessageList channelId={channelId} />
      <MessageInput channelId={channelId} />
    </>
  );
}
