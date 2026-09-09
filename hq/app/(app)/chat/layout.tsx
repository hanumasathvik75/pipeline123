import ChatSidebar from '@/components/chat/ChatSidebar';

export default function ChatLayout({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', height: '100%', overflow: 'hidden' }}>
      <ChatSidebar />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: 'var(--bg-default)' }}>
        {children}
      </div>
    </div>
  );
}
