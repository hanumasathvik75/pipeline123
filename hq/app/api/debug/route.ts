import { NextResponse } from 'next/server';
import { supabaseAdmin } from '@/lib/supabaseAdmin';

export const dynamic = 'force-dynamic';

export async function GET() {
  const results: Record<string, any> = {
    env: {
      hasSupabaseUrl: !!process.env.NEXT_PUBLIC_SUPABASE_URL,
      hasServiceKey: !!process.env.SUPABASE_SERVICE_ROLE_KEY,
      supabaseUrlPreview: process.env.NEXT_PUBLIC_SUPABASE_URL?.slice(0, 30) + '...',
    },
  };

  // Check if supabaseAdmin is properly initialized
  if (!supabaseAdmin || typeof supabaseAdmin.from !== 'function') {
    results.supabaseClient = 'NOT INITIALIZED - env vars missing';
    return NextResponse.json(results);
  }
  results.supabaseClient = 'OK';

  // Check hq_users
  try {
    const { data, error } = await supabaseAdmin
      .from('hq_users')
      .select('id, username, status, role')
      .limit(5);

    if (error) {
      results.usersTable = { error: error.message, code: error.code };
    } else {
      results.usersTable = { rowCount: data?.length ?? 0 };
    }
  } catch (e: any) {
    results.usersTable = { exception: e.message };
  }

  // Check hq_chat_channels
  try {
    const { data, error } = await supabaseAdmin
      .from('hq_chat_channels')
      .select('*')
      .limit(5);

    if (error) {
      results.chatChannels = { error: error.message, code: error.code };
    } else {
      results.chatChannels = { ok: true, count: data?.length ?? 0, rows: data };
    }
  } catch (e: any) {
    results.chatChannels = { exception: e.message };
  }

  return NextResponse.json(results);
}
