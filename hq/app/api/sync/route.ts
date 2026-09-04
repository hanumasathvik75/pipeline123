import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

export const dynamic = 'force-dynamic';

export async function GET() {
  try {
    const state = await serverDb.getFullState();
    return NextResponse.json(state);
  } catch (error) {
    console.error('Failed to sync state:', error);
    return NextResponse.json({ error: 'Failed to read server state' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const updates = await request.json();
    const { 
      tasks, projects, meetings, leads, investors, rdProjects,
      goals, wikiDocs, crmContacts, progressCategories, progressLogs
    } = updates;

    // Use bulk upsert methods where necessary, or just rely on the API routes
    // for individual entity updates. Since the old implementation just replaced
    // the whole arrays, we'll iterate and upsert here for backward compatibility
    // with the store.ts sync pattern, but ideally store.ts should call specific endpoints.

    if (tasks) await Promise.all(tasks.map((t: any) => serverDb.upsertTask(t)));
    if (projects) await Promise.all(projects.map((p: any) => serverDb.upsertProject(p)));
    if (meetings) await Promise.all(meetings.map((m: any) => serverDb.upsertMeeting(m)));
    if (leads) await Promise.all(leads.map((l: any) => serverDb.upsertLead(l)));
    if (investors) await Promise.all(investors.map((i: any) => serverDb.upsertInvestor(i)));
    if (rdProjects) await Promise.all(rdProjects.map((r: any) => serverDb.upsertRDProject(r)));
    if (goals) await Promise.all(goals.map((g: any) => serverDb.upsertGoal(g)));
    if (wikiDocs) await Promise.all(wikiDocs.map((w: any) => serverDb.upsertWikiDoc(w)));
    if (crmContacts) {
        if(crmContacts.length > 50) {
            // Bulk insert for large imports
            await serverDb.bulkInsertCrmContacts(crmContacts);
        } else {
             await Promise.all(crmContacts.map((c: any) => serverDb.upsertCrmContact(c)));
        }
    }
    if (progressCategories) await Promise.all(progressCategories.map((c: any) => serverDb.upsertProgressCategory(c)));
    if (progressLogs) await Promise.all(progressLogs.map((l: any) => serverDb.upsertProgressLog(l)));

    const state = await serverDb.getFullState();

    return NextResponse.json({
      success: true,
      lastUpdated: state.lastUpdated,
      users: state.users,
    });
  } catch (error) {
    console.error('Failed to update state:', error);
    return NextResponse.json({ error: 'Failed to write server state' }, { status: 500 });
  }
}
