'use client';

import { DollarSign, TrendingDown, Clock, ShieldCheck, CreditCard, PieChart } from 'lucide-react';
import { formatCurrency } from '@/lib/utils';

export default function FinancePage() {
  const runwayMonths = 14;
  const monthlyBurn = 420000; // INR
  const cashBalance = 5880000; // INR ~ $70K
  const committedArr = 2600000; // INR

  const EXPENSES = [
    { category: 'Engineering & ML Payroll', amount: 280000, percentage: 67 },
    { category: 'Cloud Infrastructure & GPU Compute (AWS / GCP)', amount: 65000, percentage: 15 },
    { category: 'Hardware Testbeds & OBD-II Simulators', amount: 35000, percentage: 8 },
    { category: 'Legal, Compliance & Patents', amount: 25000, percentage: 6 },
    { category: 'SaaS Tooling & Admin', amount: 15000, percentage: 4 },
  ];

  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Financial Operations & Runway</h1>
          <p className="page-subtitle">
            Cash management, monthly burn analysis, cloud compute costs, and runway projection.
          </p>
        </div>
      </div>

      {/* KPI Stats */}
      <div className="grid-stats">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.12)', color: '#10b981' }}>
            <DollarSign size={20} />
          </div>
          <div>
            <div className="stat-value">{formatCurrency(cashBalance, 'INR')}</div>
            <div className="stat-label">Total Cash Reserves</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(239, 68, 68, 0.12)', color: '#ef4444' }}>
            <TrendingDown size={20} />
          </div>
          <div>
            <div className="stat-value">{formatCurrency(monthlyBurn, 'INR')}</div>
            <div className="stat-label">Net Monthly Burn</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.12)', color: '#38bdf8' }}>
            <Clock size={20} />
          </div>
          <div>
            <div className="stat-value">{runwayMonths} Months</div>
            <div className="stat-label">Projected Runway</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(167, 139, 250, 0.12)', color: '#a78bfa' }}>
            <ShieldCheck size={20} />
          </div>
          <div>
            <div className="stat-value">{formatCurrency(committedArr, 'INR')}</div>
            <div className="stat-label">Contracted ARR</div>
          </div>
        </div>
      </div>

      {/* Breakdown Card */}
      <div className="card" style={{ padding: '1.5rem' }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '1rem' }}>
          Monthly Expense Allocation Breakdown
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {EXPENSES.map((exp, i) => (
            <div key={i}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '0.35rem' }}>
                <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{exp.category}</span>
                <span style={{ fontWeight: 700, color: 'var(--text-secondary)' }}>
                  {formatCurrency(exp.amount, 'INR')} ({exp.percentage}%)
                </span>
              </div>
              <div className="progress-bar" style={{ height: 6 }}>
                <div
                  className="progress-fill"
                  style={{
                    width: `${exp.percentage}%`,
                    background: i === 0 ? 'var(--accent)' : i === 1 ? '#38bdf8' : i === 2 ? '#f59e0b' : '#a855f7'
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
