'use client';

import { useState } from 'react';
import { DollarSign, TrendingDown, Clock, ShieldCheck, Edit, Plus, Trash2, Loader2 } from 'lucide-react';
import { formatCurrency } from '@/lib/utils';
import { useHQStore } from '@/lib/store';
import { FinanceExpense } from '@/lib/types';

export default function FinancePage() {
  const { financeData, currentUser, updateFinanceData } = useHQStore();
  const [isEditing, setIsEditing] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // Form State
  const [runwayMonths, setRunwayMonths] = useState(14);
  const [monthlyBurn, setMonthlyBurn] = useState(420000);
  const [cashBalance, setCashBalance] = useState(5880000);
  const [committedArr, setCommittedArr] = useState(2600000);
  const [expenses, setExpenses] = useState<FinanceExpense[]>([]);

  // Initialize form when opening edit modal
  const handleOpenEdit = () => {
    if (financeData) {
      setRunwayMonths(financeData.runwayMonths);
      setMonthlyBurn(financeData.monthlyBurn);
      setCashBalance(financeData.cashBalance);
      setCommittedArr(financeData.committedArr);
      setExpenses([...financeData.expenses]);
    }
    setIsEditing(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    await updateFinanceData({ runwayMonths, monthlyBurn, cashBalance, committedArr, expenses });
    setIsSubmitting(false);
    setIsEditing(false);
  };

  if (currentUser?.role === 'intern') {
    return (
      <div className="page" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '50vh', gap: '1rem' }}>
        <ShieldCheck size={48} style={{ color: 'var(--text-muted)' }} />
        <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Access Denied</h2>
        <p style={{ color: 'var(--text-muted)' }}>You do not have clearance to view financial data.</p>
      </div>
    );
  }

  if (!financeData) {
    return (
      <div className="page" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '50vh', gap: '1rem' }}>
        <Loader2 size={24} className="spin" style={{ color: 'var(--text-muted)' }} />
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Loading Finance Data...</p>
        <button 
          className="btn btn-secondary" 
          onClick={async () => {
            try {
              const res = await fetch('/api/finance');
              const text = await res.text();
              alert(`Status: ${res.status}\nBody: ${text}`);
            } catch (e: any) {
              alert('Fetch failed: ' + e.message);
            }
          }}
        >
          Debug Connection
        </button>
      </div>
    );
  }

  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div style={{ flex: 1 }}>
          <h1 className="page-title">Financial Operations & Runway</h1>
          <p className="page-subtitle">
            Cash management, monthly burn analysis, cloud compute costs, and runway projection.
          </p>
        </div>
        {currentUser?.role === 'c_level' && (
          <button className="btn btn-primary" onClick={handleOpenEdit}>
            <Edit size={15} />
            Edit Financials
          </button>
        )}
      </div>

      {/* KPI Stats */}
      <div className="grid-stats">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.12)', color: '#10b981' }}>
            <DollarSign size={20} />
          </div>
          <div>
            <div className="stat-value">{formatCurrency(financeData.cashBalance, 'INR')}</div>
            <div className="stat-label">Total Cash Reserves</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(239, 68, 68, 0.12)', color: '#ef4444' }}>
            <TrendingDown size={20} />
          </div>
          <div>
            <div className="stat-value">{formatCurrency(financeData.monthlyBurn, 'INR')}</div>
            <div className="stat-label">Net Monthly Burn</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.12)', color: '#38bdf8' }}>
            <Clock size={20} />
          </div>
          <div>
            <div className="stat-value">{financeData.runwayMonths} Months</div>
            <div className="stat-label">Projected Runway</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(167, 139, 250, 0.12)', color: '#a78bfa' }}>
            <ShieldCheck size={20} />
          </div>
          <div>
            <div className="stat-value">{formatCurrency(financeData.committedArr, 'INR')}</div>
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
          {financeData.expenses.map((exp, i) => (
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

      {/* Edit Modal */}
      {isEditing && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)' }} onClick={() => setIsEditing(false)} />
          <div className="card" style={{ position: 'relative', width: '100%', maxWidth: 600, maxHeight: '90vh', display: 'flex', flexDirection: 'column', zIndex: 1 }}>
            
            <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border)' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Edit Financial Data</h2>
            </div>
            
            <form onSubmit={handleSave} style={{ padding: '1.5rem', overflow: 'auto', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label className="label">Total Cash Reserves (INR)</label>
                  <input className="input" type="number" value={cashBalance} onChange={e => setCashBalance(Number(e.target.value))} required />
                </div>
                <div>
                  <label className="label">Net Monthly Burn (INR)</label>
                  <input className="input" type="number" value={monthlyBurn} onChange={e => setMonthlyBurn(Number(e.target.value))} required />
                </div>
                <div>
                  <label className="label">Projected Runway (Months)</label>
                  <input className="input" type="number" value={runwayMonths} onChange={e => setRunwayMonths(Number(e.target.value))} required />
                </div>
                <div>
                  <label className="label">Contracted ARR (INR)</label>
                  <input className="input" type="number" value={committedArr} onChange={e => setCommittedArr(Number(e.target.value))} required />
                </div>
              </div>

              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                  <label className="label" style={{ marginBottom: 0 }}>Monthly Expenses Breakdown</label>
                  <button type="button" onClick={() => setExpenses([...expenses, { category: '', amount: 0, percentage: 0 }])} className="btn btn-sm">
                    <Plus size={14} /> Add Line Item
                  </button>
                </div>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {expenses.map((exp, i) => (
                    <div key={i} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <input className="input" placeholder="Category Name" value={exp.category} onChange={e => { const newExp = [...expenses]; newExp[i].category = e.target.value; setExpenses(newExp); }} required style={{ flex: 2 }} />
                      <input className="input" type="number" placeholder="Amount (INR)" value={exp.amount} onChange={e => { const newExp = [...expenses]; newExp[i].amount = Number(e.target.value); setExpenses(newExp); }} required style={{ flex: 1 }} />
                      <div style={{ position: 'relative', flex: '0 0 80px' }}>
                        <input className="input" type="number" placeholder="%" value={exp.percentage} onChange={e => { const newExp = [...expenses]; newExp[i].percentage = Number(e.target.value); setExpenses(newExp); }} required style={{ paddingRight: '1.5rem' }} />
                        <span style={{ position: 'absolute', right: '0.5rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', fontSize: '0.8rem' }}>%</span>
                      </div>
                      <button type="button" onClick={() => setExpenses(expenses.filter((_, idx) => idx !== i))} className="btn-icon" style={{ color: '#ef4444' }}>
                        <Trash2 size={16} />
                      </button>
                    </div>
                  ))}
                  {expenses.length === 0 && <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>No expense items added.</p>}
                </div>
              </div>
              
              <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border)' }}>
                <button type="button" onClick={() => setIsEditing(false)} className="btn btn-ghost" style={{ flex: 1 }}>Cancel</button>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }} disabled={isSubmitting}>
                  {isSubmitting ? 'Saving...' : 'Save Financials'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
