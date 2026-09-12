import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_EXTERNAL_SUPABASE_URL || '';
const supabaseServiceKey = process.env.EXTERNAL_SUPABASE_SERVICE_ROLE_KEY || '';

if (!supabaseUrl || !supabaseServiceKey) {
  console.warn(
    'Missing External Supabase environment variables.\n' +
    'Please set NEXT_PUBLIC_EXTERNAL_SUPABASE_URL and EXTERNAL_SUPABASE_SERVICE_ROLE_KEY in your .env.local file.'
  );
}

// Server-side client with service role key (bypasses RLS)
export const externalSupabaseAdmin = supabaseUrl && supabaseServiceKey 
  ? createClient(supabaseUrl, supabaseServiceKey, { auth: { persistSession: false } })
  : ({} as any);
