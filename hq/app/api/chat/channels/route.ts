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
  } catch (error: any) {
    console.error('Failed to get channels:', error);
    return NextResponse.json({ error: error?.message || 'Failed to read channels' }, { status: 500 });
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
  } catch (error: any) {
    console.error('Failed to create channel:', error);
    return NextResponse.json({ error: error?.message || 'Failed to create channel' }, { status: 500 });
  }
}

export async function DELETE(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const channelId = searchParams.get('channelId');
    const userId = searchParams.get('userId');
    const userRole = searchParams.get('userRole') || 'member';

    if (!channelId || !userId) {
      return NextResponse.json({ error: 'channelId and userId are required' }, { status: 400 });
    }

    await serverDb.deleteChatChannel(channelId, userId, userRole);
    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Failed to delete channel:', error);
    return NextResponse.json({ error: error?.message || 'Failed to delete channel' }, { status: 500 });
  }
}
