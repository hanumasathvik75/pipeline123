import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';
import { FundraisingMeta } from '@/lib/types';

export async function GET() {
  try {
    const data = await serverDb.getFundraisingMeta();
    if (!data) {
      return NextResponse.json({ error: 'Fundraising meta not found' }, { status: 404 });
    }
    return NextResponse.json({ fundraisingMeta: data });
  } catch (error: any) {
    console.error('Fundraising Meta GET error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

export async function PUT(request: Request) {
  try {
    const updates: Partial<FundraisingMeta> = await request.json();

    const success = await serverDb.updateFundraisingMeta(updates);
    if (!success) {
      return NextResponse.json({ error: 'Failed to update fundraising meta' }, { status: 500 });
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Fundraising Meta PUT error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
