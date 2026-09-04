'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useHQStore } from '@/lib/store';
import { Zap, Mail, Lock, AlertCircle } from 'lucide-react';

const DEMO_USERS = [
  { email: 'shashank@ignitionai.in', role: 'Founder / CEO', name: 'Shashank' },
  { email: 'arjun@ignitionai.in', role: 'Engineering', name: 'Arjun K' },
  { email: 'priya@ignitionai.in', role: 'R&D / ML', name: 'Priya S' },
  { email: 'dushyanth@ignitionai.in', role: 'Intern', name: 'Dushyanth B' },
];

export default function LoginPage() {
  const router = useRouter();
  const login = useHQStore(s => s.login);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    
    try {
      const ok = await login(email, password);
      if (ok) {
        router.replace('/dashboard');
      } else {
        setError('Invalid credentials. Check email and password.');
      }
    } catch {
      setError('Authentication failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100dvh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'var(--bg-base)',
      padding: '2rem',
    }}>
      {/* Background grid */}
      <div style={{
        position: 'fixed', inset: 0, zIndex: 0,
        backgroundImage: 'linear-gradient(rgba(0,0,0,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(0,0,0,0.03) 1px, transparent 1px)',
        backgroundSize: '40px 40px',
      }} />

      <div style={{ position: 'relative', zIndex: 1, width: '100%', maxWidth: 420 }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
          <div style={{
            width: 220,
            margin: '0 auto 1rem',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/logo.png" alt="IgnitionAI Logo" style={{ width: '100%', height: 'auto', objectFit: 'contain' }} />
          </div>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', fontWeight: 500 }}>
            HQ Operating System
          </p>
        </div>

        {/* Login Card */}
        <div className="card" style={{ padding: '2rem' }}>
          <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>
                Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <Mail size={15} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  className="input"
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="you@ignitionai.in"
                  style={{ paddingLeft: '2.25rem' }}
                  required
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>
                Password
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={15} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  className="input"
                  type="password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="••••••••"
                  style={{ paddingLeft: '2.25rem' }}
                  required
                />
              </div>
            </div>

            {error && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.75rem', background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.2)', borderRadius: 8 }}>
                <AlertCircle size={14} color="#ef4444" />
                <span style={{ fontSize: '0.8rem', color: '#f87171' }}>{error}</span>
              </div>
            )}

            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{ width: '100%', padding: '0.75rem', fontSize: '0.9rem', opacity: loading ? 0.7 : 1 }}
            >
              {loading ? (
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <span style={{ width: 16, height: 16, border: '2px solid rgba(255,255,255,0.3)', borderTopColor: 'white', borderRadius: '50%', animation: 'spin 0.7s linear infinite' }} />
                  Signing in…
                </span>
              ) : 'Sign In to HQ'}
            </button>
          </form>
        </div>

        {/* Quick Demo Login Credentials */}
        <div style={{ marginTop: '1.5rem' }}>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center', marginBottom: '0.75rem' }}>
            Registered Team Accounts (Default password: <code>ignition2026</code>)
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.5rem' }}>
            {DEMO_USERS.map(u => (
              <button
                key={u.email}
                onClick={() => setEmail(u.email)}
                style={{
                  background: email === u.email ? 'var(--bg-elevated)' : 'var(--bg-surface)',
                  border: `1px solid ${email === u.email ? 'var(--text-primary)' : 'var(--border)'}`,
                  borderRadius: 8,
                  padding: '0.625rem 0.75rem',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'all 0.15s',
                }}
              >
                <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-primary)' }}>{u.name}</div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 1 }}>{u.role}</div>
              </button>
            ))}
          </div>
        </div>
      </div>

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
