'use client';

import ChatSidebar from '@/components/chat/ChatSidebar';
import { usePathname } from 'next/navigation';

export default function ChatLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const isChannelOpen = pathname !== '/chat';

  return (
    <div className="chat-container">
      {/* Sidebar column: On mobile, hidden when inside a channel room */}
      <div className={`chat-sidebar-col ${isChannelOpen ? 'hide-mobile' : ''}`}>
        <ChatSidebar />
      </div>

      {/* Main message/content column: On mobile, hidden when on chat home */}
      <div className={`chat-main-col ${!isChannelOpen ? 'hide-mobile' : ''}`}>
        {children}
      </div>
    </div>
  );
}
