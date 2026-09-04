import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

export async function GET() {
  try {
    const finance = await serverDb.getFinanceData();
    const fundraising = await serverDb.getFundraisingMeta();
    
    return NextResponse.json({
      success: true,
      finance,
      fundraising
    });
  } catch (error: any) {
    return NextResponse.json({
      success: false,
      error: error.message
    });
  }
}
