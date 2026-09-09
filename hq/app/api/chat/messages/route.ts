import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

export const dynamic = 'force-dynamic';

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const channelId = searchParams.get('channelId');
    const limit = parseInt(searchParams.get('limit') || '50');
    
    if (!channelId) {
      return NextResponse.json({ error: 'Channel ID is required' }, { status: 400 });
    }

    const messages = await serverDb.getChatMessages(channelId, limit);
    return NextResponse.json({ messages });
  } catch (error: any) {
    console.error('Failed to get messages:', error);
    return NextResponse.json({ error: error?.message || 'Failed to read messages' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    
    if (!body.channelId || !body.senderId || !body.content) {
      return NextResponse.json({ error: 'Missing required fields' }, { status: 400 });
    }

    const newMessage = await serverDb.insertChatMessage(body);
    return NextResponse.json({ message: newMessage });
  } catch (error: any) {
    console.error('Failed to insert message:', error);
    return NextResponse.json({ error: error?.message || 'Failed to insert message' }, { status: 500 });
  }
}
