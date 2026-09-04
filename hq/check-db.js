import { createClient } from '@supabase/supabase-js';
import dotenv from 'dotenv';
dotenv.config({ path: '.env.local' });

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || '';
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY || '';

const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey, { auth: { persistSession: false } });

async function run() {
  console.log("Checking Finance Data...");
  const { data: finance, error: financeError } = await supabaseAdmin
    .from('hq_finance')
    .select('*')
    .eq('id', 'hq-finance-1')
    .single();
  console.log("Finance Data:", finance, "Error:", financeError);

  console.log("\nChecking Fundraising Meta Data...");
  const { data: fundraising, error: fundError } = await supabaseAdmin
    .from('hq_fundraising_meta')
    .select('*')
    .eq('id', 'hq-fundraising-1')
    .single();
  console.log("Fundraising Data:", fundraising, "Error:", fundError);
}

run();
