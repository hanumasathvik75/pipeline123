import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { Priority, ProjectStatus, TaskStatus, LeadStage } from './types';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function priorityColor(priority: Priority): string {
  switch (priority) {
    case 'critical': return 'text-red-400 bg-red-400/10 border-red-400/20';
    case 'high': return 'text-orange-400 bg-orange-400/10 border-orange-400/20';
    case 'medium': return 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20';
    case 'low': return 'text-emerald-400 bg-emerald-400/10 border-emerald-400/20';
  }
}

export function priorityDot(priority: Priority): string {
  switch (priority) {
    case 'critical': return 'bg-red-400';
    case 'high': return 'bg-orange-400';
    case 'medium': return 'bg-yellow-400';
    case 'low': return 'bg-emerald-400';
  }
}

export function statusColor(status: ProjectStatus | TaskStatus): string {
  switch (status) {
    case 'done': return 'text-emerald-400 bg-emerald-400/10 border-emerald-400/20';
    case 'in_progress': return 'text-blue-400 bg-blue-400/10 border-blue-400/20';
    case 'review': return 'text-purple-400 bg-purple-400/10 border-purple-400/20';
    case 'testing': return 'text-cyan-400 bg-cyan-400/10 border-cyan-400/20';
    case 'blocked': return 'text-red-400 bg-red-400/10 border-red-400/20';
    case 'planned': return 'text-blue-300 bg-blue-300/10 border-blue-300/20';
    case 'backlog': return 'text-slate-400 bg-slate-400/10 border-slate-400/20';
    case 'on_hold': return 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20';
    default: return 'text-slate-400 bg-slate-400/10 border-slate-400/20';
  }
}

export function statusLabel(status: ProjectStatus | TaskStatus): string {
  const labels: Record<string, string> = {
    backlog: 'Backlog',
    planned: 'Planned',
    in_progress: 'In Progress',
    review: 'Review',
    testing: 'Testing',
    done: 'Done',
    blocked: 'Blocked',
    on_hold: 'On Hold',
  };
  return labels[status] || status;
}

export function leadStageColor(stage: LeadStage): string {
  switch (stage) {
    case 'customer': return 'text-emerald-400 bg-emerald-400/10 border-emerald-400/20';
    case 'negotiation': return 'text-purple-400 bg-purple-400/10 border-purple-400/20';
    case 'proposal': return 'text-blue-400 bg-blue-400/10 border-blue-400/20';
    case 'pilot': return 'text-cyan-400 bg-cyan-400/10 border-cyan-400/20';
    case 'meeting': return 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20';
    case 'interested': return 'text-orange-400 bg-orange-400/10 border-orange-400/20';
    case 'contacted': return 'text-blue-300 bg-blue-300/10 border-blue-300/20';
    case 'lead': return 'text-slate-400 bg-slate-400/10 border-slate-400/20';
  }
}

export function formatCurrency(value: number, currency: 'INR' | 'USD' = 'INR'): string {
  if (currency === 'INR') {
    if (value >= 100000) return `₹${(value / 100000).toFixed(1)}L`;
    if (value >= 1000) return `₹${(value / 1000).toFixed(0)}K`;
    return `₹${value}`;
  }
  return `$${value.toLocaleString()}`;
}

export function formatDate(dateStr: string): string {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export function daysUntil(dateStr: string): number {
  const now = new Date();
  const target = new Date(dateStr);
  const diff = target.getTime() - now.getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

export function getInitials(name: string): string {
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
}

export function avatarColor(name: string): string {
  const colors = [
    'from-violet-500 to-purple-600',
    'from-blue-500 to-cyan-600',
    'from-emerald-500 to-teal-600',
    'from-orange-500 to-red-600',
    'from-pink-500 to-rose-600',
    'from-indigo-500 to-blue-600',
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash += name.charCodeAt(i);
  return colors[hash % colors.length];
}

export function rdStageLabel(stage: string): string {
  const labels: Record<string, string> = {
    hypothesis: 'Hypothesis',
    methodology: 'Methodology',
    dataset: 'Dataset',
    model_dev: 'Model Development',
    results: 'Results',
    published: 'Published',
  };
  return labels[stage] || stage;
}
