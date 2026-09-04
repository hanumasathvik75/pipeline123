import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';
import { FinanceData } from '@/lib/types';

export const dynamic = 'force-dynamic';

export async function GET() {
  try {
    const data = await serverDb.getFinanceData();
    if (!data) {
      return NextResponse.json({ error: 'Finance data not found' }, { status: 404 });
    }
    return NextResponse.json({ financeData: data });
  } catch (error: any) {
    console.error('Finance GET error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

export async function PUT(request: Request) {
  try {
    // In a real app we'd verify session/JWT here
    // For this prototype, we'll assume the frontend only allows this if c_level
    const updates: Partial<FinanceData> = await request.json();

    const success = await serverDb.updateFinanceData(updates);
    if (!success) {
      return NextResponse.json({ error: 'Failed to update finance data' }, { status: 500 });
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Finance PUT error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
