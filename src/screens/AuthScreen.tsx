import React, { useState } from 'react';
import {
  Building2,
  CheckCircle2,
  FileText,
  KeyRound,
  Lock,
  Mail,
  ShieldCheck,
  Sparkles,
  User
} from 'lucide-react';
import { useGsms } from '../state/GsmsContext.tsx';
import { StatusBadge } from '../components/UiPrimitives.tsx';

export const AuthScreen: React.FC = () => {
  const {
    supabaseState,
    enterWorkspaceLocal,
    signInWithSupabaseEmail,
    signUpWithSupabaseEmail,
    resetSupabasePassword
  } = useGsms();

  const [authMode, setAuthMode] = useState<'SIGN_IN' | 'SIGN_UP'>('SIGN_IN');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [localName, setLocalName] = useState('GSMS Executive');
  const [localEmail, setLocalEmail] = useState('executive@oakridge.edu');
  const [feedback, setFeedback] = useState<{ ok: boolean; text: string } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSupabaseSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setFeedback(null);
    const res =
      authMode === 'SIGN_IN'
        ? await signInWithSupabaseEmail(email, password)
        : await signUpWithSupabaseEmail(email, password, fullName);
    setFeedback({ ok: res.ok, text: res.message });
    setSubmitting(false);
  };

  const handlePasswordReset = async () => {
    setSubmitting(true);
    setFeedback(null);
    const res = await resetSupabasePassword(email);
    setFeedback({ ok: res.ok, text: res.message });
    setSubmitting(false);
  };

  return (
    <div className="min-h-screen bg-[#F5F7FB] text-[#0F172A] flex flex-col">
      {/* Hero Header */}
      <div className="relative overflow-hidden bg-[#0F2537] text-white border-b border-[#1B304A]">
        <img
          src="/assets/img_campus_hero.jpg"
          alt="Global School Campus"
          className="absolute inset-0 w-full h-full object-cover opacity-30 pointer-events-none"
        />
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background:
              'linear-gradient(110deg, rgba(9,24,38,0.96) 0%, rgba(15,37,55,0.88) 55%, rgba(29,78,216,0.68) 100%)'
          }}
        />
        <div className="relative max-w-6xl mx-auto px-4 sm:px-6 py-10 sm:py-14">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-md bg-white/15 border border-white/20 text-[11px] font-bold tracking-wider uppercase text-[#D1FAE5] mb-4">
            <ShieldCheck className="w-3.5 h-3.5 text-[#34D399]" />
            Multi-Tenant AI School ERP
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-white max-w-3xl">
            Global School Management System (GSMS)
          </h1>
          <p className="text-sm sm:text-base text-white/80 mt-2.5 max-w-2xl leading-relaxed">
            19 independent role-based institutional portals, strict multi-layer RBAC with granular permission
            delegation, 21 modular school engines, end-to-end report approvals with PDF export, and role-scoped
            Gemini AI assistance.
          </p>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-6xl w-full mx-auto px-4 sm:px-6 py-8 flex-1 space-y-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Card 1: Supabase Email & Password Authentication */}
          <div className="lg:col-span-7 bg-white rounded-2xl border border-[#D0D9E4] p-6 shadow-xs">
            <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
              <div>
                <h2 className="text-lg font-bold text-[#0F172A]">Institutional Sign-In</h2>
                <p className="text-xs text-[#475569] mt-0.5">
                  {supabaseState.isConfigured
                    ? 'Authenticate via Supabase GoTrue for multi-device PostgreSQL tenant sync'
                    : 'Sign in or create an account with persistent local workspace storage'}
                </p>
              </div>
              <StatusBadge
                text={supabaseState.isConfigured ? 'CLOUD AUTH READY' : 'LOCAL AUTH READY'}
                tone={supabaseState.isConfigured ? 'emerald' : 'cobalt'}
              />
            </div>

            <div className="flex gap-2 mb-5 p-1 bg-[#F5F7FB] rounded-xl border border-[#D0D9E4]">
              <button
                type="button"
                onClick={() => {
                  setAuthMode('SIGN_IN');
                  setFeedback(null);
                }}
                className={`flex-1 py-2 px-3 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                  authMode === 'SIGN_IN'
                    ? 'bg-[#0F2537] text-white'
                    : 'text-[#475569] hover:text-[#0F172A]'
                }`}
              >
                Sign In
              </button>
              <button
                type="button"
                onClick={() => {
                  setAuthMode('SIGN_UP');
                  setFeedback(null);
                }}
                className={`flex-1 py-2 px-3 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                  authMode === 'SIGN_UP'
                    ? 'bg-[#0F2537] text-white'
                    : 'text-[#475569] hover:text-[#0F172A]'
                }`}
              >
                Create Account
              </button>
            </div>

            <form onSubmit={handleSupabaseSubmit} className="space-y-3.5">
              {authMode === 'SIGN_UP' ? (
                <div>
                  <label className="block text-xs font-semibold text-[#475569] mb-1">Full Name</label>
                  <div className="relative">
                    <User className="w-4 h-4 text-[#475569] absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      placeholder="Dr. Evelyn Vance"
                      required
                      className="w-full pl-9 pr-3 py-2.5 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB] focus:bg-white focus:outline-none focus:border-[#1D4ED8]"
                    />
                  </div>
                </div>
              ) : null}

              <div>
                <label className="block text-xs font-semibold text-[#475569] mb-1">Institutional Email</label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-[#475569] absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="principal@oakridge.edu"
                    required
                    className="w-full pl-9 pr-3 py-2.5 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB] focus:bg-white focus:outline-none focus:border-[#1D4ED8]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#475569] mb-1">Password</label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-[#475569] absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Minimum 6 characters"
                    required
                    minLength={6}
                    className="w-full pl-9 pr-3 py-2.5 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB] focus:bg-white focus:outline-none focus:border-[#1D4ED8]"
                  />
                </div>
              </div>

              {!supabaseState.isConfigured ? (
                <div className="rounded-xl bg-[#DBEAFE]/50 border border-[#1D4ED8]/25 px-3.5 py-2.5 text-xs text-[#1E3A8A]">
                  Local Account Mode active: enter any valid email and 6+ character password to sign in
                  immediately, or configure real Supabase secrets later for cloud sync.
                </div>
              ) : null}

              {feedback ? (
                <div
                  className={`rounded-xl px-3.5 py-2.5 text-xs border ${
                    feedback.ok
                      ? 'bg-[#D1FAE5] border-[#059669]/30 text-[#065F46]'
                      : 'bg-[#FEE2E2] border-[#DC2626]/30 text-[#991B1B]'
                  }`}
                >
                  {feedback.text}
                </div>
              ) : null}

              <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2.5 rounded-xl bg-[#1D4ED8] hover:bg-[#1E40AF] disabled:opacity-45 text-white text-sm font-semibold transition-colors whitespace-nowrap shrink-0 cursor-pointer"
                >
                  {submitting
                    ? 'Authenticating...'
                    : authMode === 'SIGN_IN'
                      ? 'Sign In with Email'
                      : 'Create Account'}
                </button>
                {authMode === 'SIGN_IN' ? (
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={handlePasswordReset}
                    className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-semibold text-[#1D4ED8] hover:bg-[#DBEAFE]/60 disabled:opacity-45 transition-colors whitespace-nowrap shrink-0 cursor-pointer"
                  >
                    <KeyRound className="w-3.5 h-3.5" />
                    Forgot Password
                  </button>
                ) : null}
              </div>
            </form>
          </div>

          {/* Card 2: Instant Multi-Tenant Workspace Launch */}
          <div className="lg:col-span-5 bg-white rounded-2xl border border-[#D0D9E4] p-6 shadow-xs flex flex-col gap-4">
            <div className="flex items-center justify-between gap-2">
              <div>
                <h2 className="text-lg font-bold text-[#0F172A]">Workspace Sandbox</h2>
                <p className="text-xs text-[#475569] mt-0.5">
                  3 pre-seeded school tenants, 19 RBAC portals &amp; local persistence
                </p>
              </div>
              <StatusBadge text="READY" tone="cobalt" />
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-[#475569] mb-1">Operator Name</label>
                <input
                  type="text"
                  value={localName}
                  onChange={(e) => setLocalName(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB] focus:bg-white focus:outline-none focus:border-[#1D4ED8]"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-[#475569] mb-1">Operator Email</label>
                <input
                  type="email"
                  value={localEmail}
                  onChange={(e) => setLocalEmail(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB] focus:bg-white focus:outline-none focus:border-[#1D4ED8]"
                />
              </div>
            </div>

            <div className="rounded-xl bg-[#F5F7FB] border border-[#D0D9E4] p-3.5 space-y-1.5 text-xs text-[#475569]">
              <div className="flex items-center gap-2 font-semibold text-[#0F172A]">
                <CheckCircle2 className="w-4 h-4 text-[#059669] shrink-0" />
                Oakridge International, Horizon STEM &amp; Pacific Crest
              </div>
              <p>
                Explore all 19 specialized portals, delegate granular permissions, run Gemini AI workflows, and
                export PDF report cards with instant persistence.
              </p>
            </div>

            <button
              type="button"
              onClick={() => enterWorkspaceLocal(localName, localEmail)}
              className="w-full py-3 px-4 rounded-xl bg-[#0F2537] hover:bg-[#091826] text-white text-sm font-bold transition-colors whitespace-nowrap shrink-0 cursor-pointer"
            >
              Launch Multi-Tenant GSMS Workspace
            </button>
          </div>
        </div>

        {/* 3 Institutional Feature Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white rounded-2xl border border-[#D0D9E4] p-5 shadow-xs space-y-2">
            <div className="w-10 h-10 rounded-xl bg-[#DBEAFE] text-[#1D4ED8] flex items-center justify-center">
              <Building2 className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-[#0F172A]">Multi-School &amp; Branch Tenancy</h3>
            <p className="text-xs text-[#475569] leading-relaxed">
              Manage multiple institutions, campuses, academic years, terms, and 21 dynamically toggled ERP
              modules under isolated tenant governance.
            </p>
          </div>

          <div className="bg-white rounded-2xl border border-[#D0D9E4] p-5 shadow-xs space-y-2">
            <div className="w-10 h-10 rounded-xl bg-[#EDE9FE] text-[#7C3AED] flex items-center justify-center">
              <Sparkles className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-[#0F172A]">Role-Scoped Gemini AI Copilot</h3>
            <p className="text-xs text-[#475569] leading-relaxed">
              Every portal receives context-aware AI capabilities—from timetable generation and leave conflict
              checks to learner study plans and finance forecasts.
            </p>
          </div>

          <div className="bg-white rounded-2xl border border-[#D0D9E4] p-5 shadow-xs space-y-2">
            <div className="w-10 h-10 rounded-xl bg-[#D1FAE5] text-[#059669] flex items-center justify-center">
              <FileText className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-[#0F172A]">End-to-End Approvals &amp; PDF Reports</h3>
            <p className="text-xs text-[#475569] leading-relaxed">
              Draft student report cards with AI comments, route through Principal endorsement, publish to parent
              portals, and export official A4 PDFs.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
