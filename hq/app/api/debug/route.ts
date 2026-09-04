import { NextResponse } from 'next/server';
import { supabaseAdmin } from '@/lib/supabaseAdmin';

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

  // Try to list tables
  try {
    const { data, error } = await supabaseAdmin
      .from('hq_users')
      .select('id, username, status, role')
      .limit(5);

    if (error) {
      results.tableQuery = { error: error.message, code: error.code };
    } else {
      results.tableQuery = { rowCount: data?.length ?? 0, rows: data };
    }
  } catch (e: any) {
    results.tableQuery = { exception: e.message };
  }

  return NextResponse.json(results);
}
