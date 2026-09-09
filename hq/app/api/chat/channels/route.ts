import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

export const dynamic = 'force-dynamic';

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const userId = searchParams.get('userId');
    
    if (!userId) {
      return NextResponse.json({ error: 'User ID is required' }, { status: 400 });
    }

    const channels = await serverDb.getChatChannelsForUser(userId);
    return NextResponse.json({ channels });
  } catch (error) {
    console.error('Failed to get channels:', error);
    return NextResponse.json({ error: 'Failed to read channels' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { channel, memberIds } = body;
    
    if (!channel || !memberIds) {
      return NextResponse.json({ error: 'Channel and memberIds are required' }, { status: 400 });
    }

    const newChannel = await serverDb.createChatChannel(channel, memberIds);
    return NextResponse.json({ channel: newChannel });
  } catch (error) {
    console.error('Failed to create channel:', error);
    return NextResponse.json({ error: 'Failed to create channel' }, { status: 500 });
  }
}
