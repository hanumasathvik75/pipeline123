import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_DEMO_SUPABASE_URL || '';
const supabaseServiceKey = process.env.DEMO_SUPABASE_SERVICE_KEY || '';

if (!supabaseUrl || !supabaseServiceKey) {
  console.warn(
    'Missing Demo Supabase environment variables.\n' +
    'Please set NEXT_PUBLIC_DEMO_SUPABASE_URL and DEMO_SUPABASE_SERVICE_KEY in your .env.local file.'
  );
}

// Server-side client with service role key (bypasses RLS)
export const demoSupabaseAdmin = supabaseUrl && supabaseServiceKey 
  ? createClient(supabaseUrl, supabaseServiceKey, { auth: { persistSession: false } })
  : ({} as any);
