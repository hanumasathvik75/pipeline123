import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';
import { cookies } from 'next/headers';
import { verifyJwt } from '@/lib/auth';

export async function POST(request: Request) {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get('hq_token')?.value;
    
    if (!token) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const session = await verifyJwt(token);
    if (!session || session.role !== 'c_level') {
      return NextResponse.json({ error: 'Forbidden. Only C-Level can directly provision accounts.' }, { status: 403 });
    }

    const { name, username, password, designation, department, skills, role } = await request.json();

    if (!name || !username || !password || !designation) {
      return NextResponse.json({ error: 'All fields are required' }, { status: 400 });
    }

    const isAvailable = await serverDb.checkUsernameAvailable(username);
    if (!isAvailable) {
      return NextResponse.json({ error: 'Username is already taken or pending approval' }, { status: 409 });
    }

    const passwordHash = await serverDb.hashPassword(password);
    
    // Insert directly into hq_users since an admin is provisioning it
    const id = `user-${Date.now()}`;
    const { error } = await serverDb.supabaseAdmin.from('hq_users').insert({
      id,
      name: name.trim(),
      username: username.trim(),
      email: username.trim(),
      password_hash: passwordHash,
      role: role || 'intern',
      designation: designation.trim(),
      department: department?.trim() || 'General',
      skills: Array.isArray(skills) ? skills : [],
      status: 'active'
    });

    if (error) throw new Error(error.message);

    return NextResponse.json({ success: true, userId: id });
  } catch (error: any) {
    console.error('Provision user error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
