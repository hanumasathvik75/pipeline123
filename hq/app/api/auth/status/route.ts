import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const username = searchParams.get('username');

    if (!username) {
      return NextResponse.json({ error: 'Username is required' }, { status: 400 });
    }

    const status = await serverDb.getPendingUserStatus(username);
    return NextResponse.json({ status });
  } catch (error) {
    console.error('Status check error:', error);
    return NextResponse.json(
      { error: 'Internal server error checking status' },
      { status: 500 }
    );
  }
}
