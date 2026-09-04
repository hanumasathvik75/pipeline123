import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';
import type { UserRole } from '@/lib/types';

export async function GET() {
  try {
    const pendingUsers = await serverDb.getPendingUsers();
    return NextResponse.json({ users: pendingUsers });
  } catch (error) {
    console.error('Failed to get pending users:', error);
    return NextResponse.json({ error: 'Failed to retrieve pending users' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const { id, action, role } = await request.json();

    if (!id || !action || !['approve', 'reject'].includes(action)) {
      return NextResponse.json(
        { error: 'Invalid request payload' },
        { status: 400 }
      );
    }

    if (action === 'approve') {
      const approvedRole = (role as UserRole) || 'executive';
      const approvedUser = await serverDb.approveUser(id, approvedRole);
      
      if (!approvedUser) {
        return NextResponse.json({ error: 'Pending user not found' }, { status: 404 });
      }
      return NextResponse.json({ success: true, user: approvedUser });
    } else {
      const ok = await serverDb.rejectUser(id);
      if (!ok) {
        return NextResponse.json({ error: 'Pending user not found' }, { status: 404 });
      }
      return NextResponse.json({ success: true });
    }
  } catch (error) {
    console.error('Failed to process pending user:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
