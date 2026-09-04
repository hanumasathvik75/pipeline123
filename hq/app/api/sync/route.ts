import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

export async function GET() {
  try {
    const state = serverDb.getState();
    // Return sanitized users
    const safeUsers = state.users.map(({ passwordHash, ...u }) => u);
    return NextResponse.json({
      ...state,
      users: safeUsers,
    });
  } catch (error) {
    console.error('Failed to sync state:', error);
    return NextResponse.json({ error: 'Failed to read server state' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const updates = await request.json();
    const { tasks, projects, meetings, leads, investors, rdProjects } = updates;

    if (tasks) serverDb.updateTasks(tasks);
    if (projects) serverDb.updateProjects(projects);
    if (meetings) serverDb.updateMeetings(meetings);
    if (leads) serverDb.updateLeads(leads);
    if (investors) serverDb.updateInvestors(investors);
    if (rdProjects) serverDb.updateRDProjects(rdProjects);

    const state = serverDb.getState();
    const safeUsers = state.users.map(({ passwordHash, ...u }) => u);

    return NextResponse.json({
      success: true,
      lastUpdated: state.lastUpdated,
      users: safeUsers,
    });
  } catch (error) {
    console.error('Failed to update state:', error);
    return NextResponse.json({ error: 'Failed to write server state' }, { status: 500 });
  }
}
