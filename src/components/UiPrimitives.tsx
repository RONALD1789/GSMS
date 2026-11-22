import React from 'react';
import { Sparkles, Zap, X } from 'lucide-react';
import { PORTAL_ROLE_META, PortalRole, RiskLevel, Student } from '../types/gsms.ts';

export type AccentTone = 'cobalt' | 'emerald' | 'amber' | 'crimson' | 'violet' | 'slate';

const TONE_MAP: Record<AccentTone, { bg: string; text: string; bar: string }> = {
  cobalt: { bg: 'bg-[#DBEAFE] text-[#1D4ED8]', text: 'text-[#1D4ED8]', bar: '#1D4ED8' },
  emerald: { bg: 'bg-[#D1FAE5] text-[#059669]', text: 'text-[#059669]', bar: '#059669' },
  amber: { bg: 'bg-[#FEF3C7] text-[#D97706]', text: 'text-[#D97706]', bar: '#D97706' },
  crimson: { bg: 'bg-[#FEE2E2] text-[#DC2626]', text: 'text-[#DC2626]', bar: '#DC2626' },
  violet: { bg: 'bg-[#EDE9FE] text-[#7C3AED]', text: 'text-[#7C3AED]', bar: '#7C3AED' },
  slate: { bg: 'bg-[#E8EEF5] text-[#475569]', text: 'text-[#475569]', bar: '#475569' }
};

export function riskTone(riskLevel: string): AccentTone {
  switch (riskLevel.toUpperCase()) {
    case RiskLevel.CRITICAL:
      return 'crimson';
    case RiskLevel.HIGH:
      return 'amber';
    case RiskLevel.MEDIUM:
      return 'cobalt';
    default:
      return 'emerald';
  }
}

export const StatusBadge: React.FC<{ text: string; tone?: AccentTone }> = ({
  text,
  tone = 'cobalt'
}) => {
  const cls = TONE_MAP[tone].bg;
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-md text-[11px] font-semibold tracking-wide whitespace-nowrap shrink-0 ${cls}`}
    >
      {text}
    </span>
  );
};

export const SectionHeader: React.FC<{
  title: string;
  subtitle?: string;
  actionLabel?: string;
  onAction?: () => void;
}> = ({ title, subtitle, actionLabel, onAction }) => (
  <div className="flex items-center justify-between gap-3 py-1">
    <div className="min-w-0">
      <h2 className="text-lg font-bold text-[#0F172A] tracking-tight truncate">{title}</h2>
      {subtitle ? <p className="text-xs text-[#475569] mt-0.5">{subtitle}</p> : null}
    </div>
    {actionLabel && onAction ? (
      <button
        type="button"
        onClick={onAction}
        className="text-xs font-semibold text-[#1D4ED8] hover:text-[#1E40AF] px-3 py-1.5 rounded-lg hover:bg-[#DBEAFE]/60 transition-colors whitespace-nowrap shrink-0 cursor-pointer"
      >
        {actionLabel}
      </button>
    ) : null}
  </div>
);

export const KpiMetricCard: React.FC<{
  label: string;
  value: string;
  delta: string;
  icon: React.ReactNode;
  tone: AccentTone;
}> = ({ label, value, delta, icon, tone }) => {
  const style = TONE_MAP[tone];
  return (
    <div className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex flex-col gap-2.5 min-w-0">
      <div className="flex items-center justify-between gap-2">
        <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${style.bg}`}>
          {icon}
        </div>
        <span
          className={`px-2 py-0.5 rounded-md text-[11px] font-semibold whitespace-nowrap shrink-0 ${style.bg}`}
        >
          {delta}
        </span>
      </div>
      <div className="text-2xl font-bold text-[#0F172A] font-mono-num tabular-nums truncate">
        {value}
      </div>
      <div className="text-xs font-medium text-[#475569] truncate">{label}</div>
    </div>
  );
};

export const AiCopilotBannerCard: React.FC<{
  role: PortalRole;
  lowDataMode: boolean;
  aiEnabled: boolean;
  onPromptClick: (capability: string) => void;
  onOpenFullAi: () => void;
}> = ({ role, lowDataMode, aiEnabled, onPromptClick, onOpenFullAi }) => {
  const meta = PORTAL_ROLE_META[role];
  return (
    <div className="relative overflow-hidden rounded-2xl bg-[#0F2537] text-white border border-[#1B304A] shadow-sm">
      {!lowDataMode ? (
        <img
          src="/assets/img_ai_prism.jpg"
          alt="GSMS Gemini AI Intelligence Layer"
          className="absolute inset-0 w-full h-full object-cover opacity-25 pointer-events-none"
        />
      ) : null}
      <div
        className="absolute inset-0 pointer-events-none"
        style={{
          background:
            'linear-gradient(90deg, rgba(9,24,38,0.95) 0%, rgba(15,37,55,0.82) 100%)'
        }}
      />
      <div className="relative p-5 flex flex-col gap-3">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-full bg-[#34D399]/20 flex items-center justify-center shrink-0">
              <Sparkles className="w-4 h-4 text-[#34D399]" />
            </div>
            <div className="min-w-0">
              <div className="text-sm font-bold text-white truncate">
                Gemini AI • {meta.shortTitle}
              </div>
              <div className="text-xs text-white/75 truncate">
                {aiEnabled ? 'Role-scoped intelligence ready' : 'AI Tools module disabled'}
              </div>
            </div>
          </div>
          <button
            type="button"
            disabled={!aiEnabled}
            onClick={onOpenFullAi}
            className="px-3.5 py-2 rounded-xl bg-[#1D4ED8] hover:bg-[#1E40AF] disabled:opacity-40 text-xs font-semibold text-white transition-colors whitespace-nowrap shrink-0 cursor-pointer"
          >
            Open AI
          </button>
        </div>

        {aiEnabled ? (
          <div className="flex flex-wrap gap-2">
            {meta.aiCapabilities.map((cap) => (
              <button
                key={cap}
                type="button"
                onClick={() => onPromptClick(cap)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 border border-white/20 text-xs font-medium text-white transition-colors whitespace-nowrap shrink-0 cursor-pointer"
              >
                <Zap className="w-3.5 h-3.5 text-[#34D399] shrink-0" />
                {cap}
              </button>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
};

export const BarChartCard: React.FC<{
  title: string;
  subtitle: string;
  bars: Array<{ label: string; value: number }>;
  tone?: AccentTone;
}> = ({ title, subtitle, bars, tone = 'cobalt' }) => {
  const maxVal = Math.max(1, ...bars.map((b) => b.value));
  const barHex = TONE_MAP[tone].bar;

  return (
    <div className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs">
      <div className="text-sm font-bold text-[#0F172A]">{title}</div>
      <div className="text-xs text-[#475569] mt-0.5 mb-4">{subtitle}</div>

      {bars.length === 0 ? (
        <div className="text-xs text-[#475569] py-6 text-center">No chart metrics available yet.</div>
      ) : (
        <div className="grid gap-3" style={{ gridTemplateColumns: `repeat(${bars.length}, minmax(0, 1fr))` }}>
          {bars.map((bar, idx) => {
            const pct = Math.max(8, Math.min(100, Math.round((bar.value / maxVal) * 100)));
            return (
              <div key={`${bar.label}-${idx}`} className="flex flex-col items-center gap-1.5 min-w-0">
                <div className="w-full max-w-[44px] h-28 rounded-xl bg-[#E8EEF5] flex items-end overflow-hidden p-0.5">
                  <div
                    className="w-full rounded-lg transition-all duration-300"
                    style={{ height: `${pct}%`, backgroundColor: barHex }}
                  />
                </div>
                <span className="text-[11px] font-bold font-mono-num tabular-nums" style={{ color: barHex }}>
                  {Math.round(bar.value)}%
                </span>
                <span className="text-[11px] text-[#475569] truncate max-w-full">{bar.label}</span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export const TrendSparklineCard: React.FC<{
  title: string;
  headlineValue: string;
  points: number[];
  lineColor?: string;
}> = ({ title, headlineValue, points, lineColor = '#059669' }) => {
  const safePoints = points.length >= 2 ? points : [85, 88, 90, 92];
  const minP = Math.min(...safePoints);
  const maxP = Math.max(...safePoints) === minP ? minP + 1 : Math.max(...safePoints);
  const width = 320;
  const height = 74;

  const coords = safePoints.map((v, i) => {
    const x = (i / (safePoints.length - 1)) * (width - 16) + 8;
    const norm = (v - minP) / (maxP - minP);
    const y = height - (norm * (height - 20) + 10);
    return { x, y };
  });

  const polyline = coords.map((c) => `${c.x},${c.y}`).join(' ');

  return (
    <div className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs">
      <div className="flex items-center justify-between gap-2 mb-3">
        <span className="text-sm font-bold text-[#0F172A]">{title}</span>
        <span className="text-lg font-bold font-mono-num tabular-nums" style={{ color: lineColor }}>
          {headlineValue}
        </span>
      </div>
      <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-20 overflow-visible">
        <polyline
          fill="none"
          stroke={lineColor}
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
          points={polyline}
        />
        {coords.map((c, idx) => (
          <circle key={idx} cx={c.x} cy={c.y} r="4" fill={lineColor} />
        ))}
      </svg>
    </div>
  );
};

export const EmptyStateCard: React.FC<{
  icon: React.ReactNode;
  title: string;
  description: string;
}> = ({ icon, title, description }) => (
  <div className="bg-white rounded-2xl border border-[#D0D9E4] p-6 text-center flex flex-col items-center gap-2">
    <div className="w-10 h-10 rounded-xl bg-[#DBEAFE] text-[#1D4ED8] flex items-center justify-center">
      {icon}
    </div>
    <div className="text-sm font-bold text-[#0F172A]">{title}</div>
    <p className="text-xs text-[#475569] max-w-md">{description}</p>
  </div>
);

export const LearnerScopePicker: React.FC<{
  students: Student[];
  focusStudentId: string | null;
  onSelect: (student: Student) => void;
}> = ({ students, focusStudentId, onSelect }) => {
  if (students.length === 0) return null;
  return (
    <div className="bg-white rounded-2xl border border-[#D0D9E4] p-3.5 shadow-xs">
      <div className="text-xs font-semibold text-[#475569] mb-2">Viewing learner</div>
      <div className="flex flex-wrap gap-2">
        {students.map((learner) => {
          const active = learner.id === focusStudentId;
          return (
            <button
              key={learner.id}
              type="button"
              onClick={() => onSelect(learner)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                active
                  ? 'bg-[#1D4ED8] text-white border-[#1D4ED8]'
                  : 'bg-[#F5F7FB] text-[#0F172A] border-[#D0D9E4] hover:bg-[#E8EEF5]'
              }`}
            >
              {learner.fullName} • {learner.className}
            </button>
          );
        })}
      </div>
    </div>
  );
};

export const ModalDialog: React.FC<{
  open: boolean;
  title: string;
  onClose: () => void;
  children: React.ReactNode;
  footer?: React.ReactNode;
}> = ({ open, title, onClose, children, footer }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#091826]/60 backdrop-blur-[2px] p-4">
      <div className="bg-white rounded-2xl border border-[#D0D9E4] shadow-xl w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden">
        <div className="px-5 py-4 border-b border-[#D0D9E4] flex items-center justify-between gap-3">
          <h3 className="text-base font-bold text-[#0F172A] truncate">{title}</h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal"
            className="p-1.5 rounded-lg text-[#475569] hover:bg-[#E8EEF5] cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
        <div className="p-5 overflow-y-auto flex-1 space-y-3">{children}</div>
        {footer ? (
          <div className="px-5 py-3.5 border-t border-[#D0D9E4] bg-[#F5F7FB] flex items-center justify-end gap-2">
            {footer}
          </div>
        ) : null}
      </div>
    </div>
  );
};
