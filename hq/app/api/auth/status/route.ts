import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

const SUPABASE_CONFIGURED =
  !!process.env.NEXT_PUBLIC_SUPABASE_URL && !!process.env.SUPABASE_SERVICE_ROLE_KEY;

// Shared in-memory store — must match the one in register/route.ts
// In demo mode: status is always "pending" after registration (admin must approve in prod)
const demoPendingUsernames = new Set<string>();

export function addDemoPending(username: string) {
  demoPendingUsernames.add(username.toLowerCase());
}

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const username = searchParams.get('username');

    if (!username) {
      return NextResponse.json({ error: 'Username is required' }, { status: 400 });
    }

    // --- LOCAL DEMO MODE -------------------------------------------------
    if (!SUPABASE_CONFIGURED) {
      // shashank is always "active", any recently registered user is "pending"
      if (username.toLowerCase() === 'shashank') {
        return NextResponse.json({ status: 'active' });
      }
      // We can't reliably share in-memory state between route modules in Next.js,
      // so in demo mode return "pending" for any submitted username
      return NextResponse.json({ status: 'pending' });
    }

    // --- PRODUCTION MODE -------------------------------------------------
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
