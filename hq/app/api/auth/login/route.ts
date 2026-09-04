import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

export async function POST(request: Request) {
  try {
    const { username, password } = await request.json();

    if (!username || !password) {
      return NextResponse.json(
        { error: 'Username and password are required' },
        { status: 400 }
      );
    }

    const user = await serverDb.findUserByUsername(username);

    if (!user) {
      return NextResponse.json(
        { error: 'Invalid credentials' },
        { status: 401 }
      );
    }

    if (user.status !== 'active') {
       return NextResponse.json(
        { error: `Account is ${user.status}. Please contact an administrator.` },
        { status: 403 }
      );
    }

    const isValid = await serverDb.verifyPassword(password, user.passwordHash);

    if (!isValid) {
      return NextResponse.json(
        { error: 'Invalid credentials' },
        { status: 401 }
      );
    }

    const { passwordHash, ...safeUser } = user;

    return NextResponse.json({
      success: true,
      user: safeUser,
    });
  } catch (error) {
    console.error('Login error:', error);
    return NextResponse.json(
      { error: 'Internal server error during login' },
      { status: 500 }
    );
  }
}
