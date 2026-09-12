import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { Priority, ProjectStatus, TaskStatus, LeadStage } from './types';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function priorityColor(priority: Priority): string {
  if (priority === 'critical') return 'text-white bg-black border-black';
  return 'text-[#073988] bg-[#073988]/10 border-[#073988]/20';
}

export function priorityDot(priority: Priority): string {
  return priority === 'critical' ? 'bg-black' : 'bg-[#073988]';
}

export function statusColor(status: ProjectStatus | TaskStatus): string {
  if (status === 'done') return 'text-white bg-[#073988] border-[#073988]';
  if (status === 'blocked') return 'text-white bg-black border-black';
  return 'text-[#073988] bg-[#073988]/10 border-[#073988]/20';
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
  if (stage === 'customer') return 'text-white bg-[#073988] border-[#073988]';
  return 'text-[#073988] bg-[#073988]/10 border-[#073988]/20';
}

export function formatCurrency(value: number, currency: string = 'INR'): string {
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
  return 'bg-[#073988] text-white';
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
