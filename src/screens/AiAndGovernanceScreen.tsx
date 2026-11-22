import React, { useState } from 'react';
import {
  Building2,
  Cloud,
  Database,
  KeyRound,
  LogOut,
  Plus,
  RefreshCw,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  UserPlus,
  Zap
} from 'lucide-react';
import {
  ModalDialog,
  SectionHeader,
  StatusBadge
} from '../components/UiPrimitives.tsx';
import { canSyncSupabase } from '../data/supabaseService.ts';
import { RolePolicy } from '../policy/RolePolicy.ts';
import { useGsms } from '../state/GsmsContext.tsx';
import {
  ALL_GSMS_MODULES,
  ALL_PORTAL_ROLES,
  GSMS_MODULE_META,
  GsmsModule,
  PORTAL_ROLE_META,
  PortalRole,
  ROLE_CAPABILITY_META,
  RoleCapability,
  UserAccessAssignment
} from '../types/gsms.ts';

const NON_DELEGATABLE_TENANT_CAPS = new Set<RoleCapability>([
  RoleCapability.CREATE_SCHOOL_TENANT,
  RoleCapability.SWITCH_SCHOOL_TENANT
]);

export const AiAndGovernanceScreen: React.FC = () => {
  const {
    workspace,
    aiState,
    supabaseState,
    askAiCopilot,
    switchSchool,
    switchBranch,
    createSchool,
    createBranch,
    updateAcademicPeriod,
    toggleModule,
    toggleLowDataMode,
    delegateUserPermissions,
    createAccessAssignment,
    testSupabase,
    pushToSupabase,
    refreshSupabaseCounts,
    toggleSupabaseAutoMirror,
    signInSupabaseInsideGovernance,
    signUpSupabaseInsideGovernance,
    signOutSupabaseInsideGovernance,
    signOutAll
  } = useGsms();

  const [subTab, setSubTab] = useState<'AI' | 'GOV'>('AI');
  const [prompt, setPrompt] = useState('');

  // Modals: New School & Branch
  const [showNewSchoolModal, setShowNewSchoolModal] = useState(false);
  const [schoolName, setSchoolName] = useState('');
  const [schoolCode, setSchoolCode] = useState('');
  const [schoolCity, setSchoolCity] = useState('');
  const [schoolCountry, setSchoolCountry] = useState('');

  const [showNewBranchModal, setShowNewBranchModal] = useState(false);
  const [branchName, setBranchName] = useState('');
  const [branchCode, setBranchCode] = useState('');
  const [branchCity, setBranchCity] = useState('');

  // Modals: Add User & Delegate Permissions
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [newUserName, setNewUserName] = useState('');
  const [newUserEmail, setNewUserEmail] = useState('');
  const [newUserRole, setNewUserRole] = useState<PortalRole>(PortalRole.TEACHER);
  const [newUserClasses, setNewUserClasses] = useState('Grade 10A');
  const [newUserSubjects, setNewUserSubjects] = useState('Mathematics');
  const [newUserStudents, setNewUserStudents] = useState('');

  const [delegatingUser, setDelegatingUser] = useState<UserAccessAssignment | null>(null);
  const [delExtras, setDelExtras] = useState<Set<string>>(new Set());
  const [delClasses, setDelClasses] = useState('');
  const [delSubjects, setDelSubjects] = useState('');
  const [delStudents, setDelStudents] = useState('');

  // Supabase inline auth modal
  const [showSbAuthModal, setShowSbAuthModal] = useState(false);
  const [sbEmail, setSbEmail] = useState('');
  const [sbPassword, setSbPassword] = useState('');

  if (!workspace) return null;

  const { activeRole, activeSchool, activeExtraCapabilities } = workspace;
  const roleMeta = PORTAL_ROLE_META[activeRole];
  const aiEnabled = activeSchool?.enabledModules.includes(GsmsModule.AI_TOOLS) ?? true;

  const canCreateSchool = RolePolicy.has(
    activeRole,
    RoleCapability.CREATE_SCHOOL_TENANT,
    activeExtraCapabilities
  );
  const canSwitchSchool = RolePolicy.has(
    activeRole,
    RoleCapability.SWITCH_SCHOOL_TENANT,
    activeExtraCapabilities
  );
  const canSwitchBranch = RolePolicy.has(
    activeRole,
    RoleCapability.SWITCH_BRANCH,
    activeExtraCapabilities
  );
  const canManageBranches = RolePolicy.has(
    activeRole,
    RoleCapability.MANAGE_BRANCHES,
    activeExtraCapabilities
  );
  const canManageModules = RolePolicy.has(
    activeRole,
    RoleCapability.MANAGE_MODULES,
    activeExtraCapabilities
  );
  const canUpdatePeriod = RolePolicy.has(
    activeRole,
    RoleCapability.UPDATE_ACADEMIC_PERIOD,
    activeExtraCapabilities
  );
  const canManageUsers = RolePolicy.has(
    activeRole,
    RoleCapability.MANAGE_USER_PERMISSIONS,
    activeExtraCapabilities
  );
  const canSupabase = canSyncSupabase(activeRole, activeExtraCapabilities);

  const govLabel =
    activeRole === PortalRole.SUPER_ADMIN ||
    activeRole === PortalRole.SCHOOL_ADMIN ||
    activeRole === PortalRole.ICT_ADMIN
      ? 'Access & Modules'
      : canSwitchBranch
        ? 'Branch & Profile'
        : 'Role & Profile';

  const openDelegateModal = (assignment: UserAccessAssignment) => {
    setDelegatingUser(assignment);
    setDelExtras(new Set(assignment.extraPermissions));
    setDelClasses(assignment.assignedClasses.join(', '));
    setDelSubjects(assignment.assignedSubjects.join(', '));
    setDelStudents(assignment.linkedStudentIds.join(', '));
  };

  const baseRoleOfDelegating = delegatingUser
    ? ALL_PORTAL_ROLES.find((r) => r === delegatingUser.primaryRole) || PortalRole.TEACHER
    : PortalRole.TEACHER;
  const baseCapsOfDelegating = RolePolicy.baseCapabilities(baseRoleOfDelegating);

  const delegatableCapabilities = Object.values(RoleCapability).filter(
    (c) => !baseCapsOfDelegating.has(c) && !NON_DELEGATABLE_TENANT_CAPS.has(c)
  );

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setSubTab('AI')}
          className={`px-3.5 py-2 rounded-xl text-xs font-semibold border transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
            subTab === 'AI'
              ? 'bg-[#0F2537] text-white border-[#0F2537]'
              : 'bg-white text-[#475569] border-[#D0D9E4] hover:bg-[#E8EEF5]'
          }`}
        >
          Gemini AI Copilot
        </button>
        <button
          type="button"
          onClick={() => setSubTab('GOV')}
          className={`px-3.5 py-2 rounded-xl text-xs font-semibold border transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
            subTab === 'GOV'
              ? 'bg-[#0F2537] text-white border-[#0F2537]'
              : 'bg-white text-[#475569] border-[#D0D9E4] hover:bg-[#E8EEF5]'
          }`}
        >
          {govLabel}
        </button>
      </div>

      {subTab === 'AI' ? (
        <div className="space-y-4">
          <div className="bg-white rounded-2xl border border-[#D0D9E4] p-5 shadow-xs space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-[#EDE9FE] text-[#7C3AED] flex items-center justify-center">
                  <Sparkles className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-[#0F172A]">
                    Role-Scoped Gemini AI • {roleMeta.shortTitle}
                  </h3>
                  <p className="text-xs text-[#475569]">{roleMeta.scopeDescription}</p>
                </div>
              </div>
              <StatusBadge
                text={aiEnabled ? 'AI READY' : 'MODULE DISABLED'}
                tone={aiEnabled ? 'violet' : 'amber'}
              />
            </div>

            <div className="flex flex-wrap gap-2">
              {roleMeta.aiCapabilities.map((cap) => (
                <button
                  key={cap}
                  type="button"
                  disabled={!aiEnabled || aiState.status === 'loading' || aiState.status === 'streaming'}
                  onClick={() => {
                    setPrompt(cap);
                    void askAiCopilot(cap);
                  }}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#EDE9FE]/60 hover:bg-[#EDE9FE] border border-[#7C3AED]/25 text-xs font-semibold text-[#5B21B6] transition-colors whitespace-nowrap shrink-0 cursor-pointer disabled:opacity-45"
                >
                  <Zap className="w-3.5 h-3.5" />
                  {cap}
                </button>
              ))}
            </div>

            <div className="space-y-2">
              <textarea
                rows={3}
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder={`Ask Gemini AI inside ${roleMeta.displayName} (strictly scoped to ${activeSchool?.name || 'your tenant'})...`}
                className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB] focus:bg-white focus:outline-none focus:border-[#7C3AED]"
              />
              <div className="flex justify-end">
                <button
                  type="button"
                  disabled={
                    !aiEnabled ||
                    !prompt.trim() ||
                    aiState.status === 'loading' ||
                    aiState.status === 'streaming'
                  }
                  onClick={() => void askAiCopilot(prompt)}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-[#7C3AED] hover:bg-[#6D28D9] disabled:opacity-45 text-white text-xs font-bold whitespace-nowrap shrink-0 cursor-pointer"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  {aiState.status === 'loading' || aiState.status === 'streaming'
                    ? 'Generating...'
                    : 'Run Context-Aware AI'}
                </button>
              </div>
            </div>
          </div>

          {aiState.status === 'loading' ? (
            <div className="bg-white rounded-2xl border border-[#D0D9E4] p-5 text-xs text-[#475569]">
              Analyzing tenant context and preparing {roleMeta.shortTitle} briefing...
            </div>
          ) : null}

          {aiState.status === 'streaming' || aiState.status === 'success' ? (
            <div className="bg-white rounded-2xl border border-[#7C3AED]/35 p-5 shadow-xs space-y-2">
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs font-bold text-[#7C3AED]">
                  {aiState.status === 'streaming' ? 'Streaming Gemini Response...' : 'Gemini AI Response'}
                </span>
                <StatusBadge text={roleMeta.shortTitle} tone="violet" />
              </div>
              <div className="text-sm text-[#0F172A] whitespace-pre-wrap leading-relaxed">
                {aiState.text}
              </div>
            </div>
          ) : null}

          {aiState.status === 'error' ? (
            <div className="rounded-2xl bg-[#FEE2E2] border border-[#DC2626]/30 p-4 text-xs text-[#991B1B]">
              {aiState.message}
            </div>
          ) : null}
        </div>
      ) : (
        <div className="space-y-5">
          {/* Active Role RBAC Policy Summary */}
          <div className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-2">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-[#059669]" />
                <span className="text-sm font-bold text-[#0F172A]">{roleMeta.displayName}</span>
              </div>
              <StatusBadge
                text={
                  activeExtraCapabilities.length > 0
                    ? `+${activeExtraCapabilities.length} DELEGATED`
                    : 'BASE RBAC'
                }
                tone={activeExtraCapabilities.length > 0 ? 'violet' : 'emerald'}
              />
            </div>
            <p className="text-xs text-[#475569]">{roleMeta.scopeDescription}</p>
          </div>

          {/* User Access & Delegated Permissions */}
          {canManageUsers ? (
            <div className="space-y-3">
              <SectionHeader
                title="User Access & Delegated Permissions"
                subtitle="Assign extra capabilities without altering a user's primary role"
                actionLabel="+ Add User"
                onAction={() => setShowAddUserModal(true)}
              />
              <div className="space-y-2.5">
                {workspace.accessAssignments.map((u) => (
                  <div
                    key={u.id}
                    className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex flex-wrap items-center justify-between gap-3"
                  >
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-bold text-[#0F172A]">{u.fullName}</span>
                        <StatusBadge text={u.primaryRole} tone="cobalt" />
                        {u.extraPermissions.length > 0 ? (
                          <StatusBadge text={`+${u.extraPermissions.length} Extra`} tone="violet" />
                        ) : null}
                      </div>
                      <div className="text-xs text-[#475569] mt-0.5">
                        {u.email}
                        {u.assignedClasses.length > 0
                          ? ` • Classes: ${u.assignedClasses.join(', ')}`
                          : ''}
                        {u.assignedSubjects.length > 0
                          ? ` • Subjects: ${u.assignedSubjects.join(', ')}`
                          : ''}
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => openDelegateModal(u)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#0F2537] hover:bg-[#091826] text-white text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                    >
                      <KeyRound className="w-3.5 h-3.5" />
                      Delegate Permissions
                    </button>
                  </div>
                ))}
              </div>
            </div>
          ) : null}

          {/* School Tenants */}
          {canSwitchSchool || canCreateSchool ? (
            <div className="space-y-3">
              <SectionHeader
                title="School Tenants"
                subtitle="Switch active institution or provision a new tenant"
                actionLabel={canCreateSchool ? '+ New School' : undefined}
                onAction={canCreateSchool ? () => setShowNewSchoolModal(true) : undefined}
              />
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                {workspace.schools.map((sc) => {
                  const isCurrent = sc.id === activeSchool?.id;
                  return (
                    <button
                      key={sc.id}
                      type="button"
                      disabled={!canSwitchSchool}
                      onClick={() => switchSchool(sc)}
                      className={`text-left rounded-2xl border p-4 transition-colors cursor-pointer ${
                        isCurrent
                          ? 'bg-[#0F2537] text-white border-[#0F2537]'
                          : 'bg-white text-[#0F172A] border-[#D0D9E4] hover:bg-[#E8EEF5]'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-bold truncate">{sc.name}</span>
                        <span
                          className={`text-[11px] font-mono-num px-2 py-0.5 rounded ${
                            isCurrent ? 'bg-white/20 text-white' : 'bg-[#F5F7FB] text-[#475569]'
                          }`}
                        >
                          {sc.code}
                        </span>
                      </div>
                      <div
                        className={`text-xs mt-1 ${isCurrent ? 'text-white/75' : 'text-[#475569]'}`}
                      >
                        {sc.city}, {sc.country} • {sc.activeAcademicYear} ({sc.activeTerm})
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          ) : null}

          {/* Campuses & Branches */}
          {canSwitchBranch || canManageBranches ? (
            <div className="space-y-3">
              <SectionHeader
                title="Campuses & Branches"
                subtitle="Switch active campus or register a new branch"
                actionLabel={canManageBranches ? '+ Add Branch' : undefined}
                onAction={canManageBranches ? () => setShowNewBranchModal(true) : undefined}
              />
              <div className="flex flex-wrap gap-2">
                {workspace.branches.map((br) => {
                  const isCurrent = br.id === workspace.activeBranchId;
                  return (
                    <button
                      key={br.id}
                      type="button"
                      disabled={!canSwitchBranch}
                      onClick={() => switchBranch(br.id)}
                      className={`px-3.5 py-2 rounded-xl text-xs font-semibold border transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                        isCurrent
                          ? 'bg-[#1D4ED8] text-white border-[#1D4ED8]'
                          : 'bg-white text-[#0F172A] border-[#D0D9E4] hover:bg-[#E8EEF5]'
                      }`}
                    >
                      {br.name} ({br.code})
                    </button>
                  );
                })}
              </div>
            </div>
          ) : null}

          {/* Academic Term Selector */}
          {canUpdatePeriod && activeSchool ? (
            <div className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="text-sm font-bold text-[#0F172A]">Active Academic Period</div>
                <div className="text-xs text-[#475569]">
                  {activeSchool.activeAcademicYear} • Currently {activeSchool.activeTerm}
                </div>
              </div>
              <div className="flex gap-2">
                {['Term 1', 'Term 2', 'Term 3'].map((t) => {
                  const active = activeSchool.activeTerm === t;
                  return (
                    <button
                      key={t}
                      type="button"
                      onClick={() =>
                        updateAcademicPeriod(activeSchool.id, activeSchool.activeAcademicYear, t)
                      }
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors whitespace-nowrap cursor-pointer ${
                        active
                          ? 'bg-[#059669] text-white border-[#059669]'
                          : 'bg-[#F5F7FB] text-[#475569] border-[#D0D9E4] hover:bg-[#E8EEF5]'
                      }`}
                    >
                      {t}
                    </button>
                  );
                })}
              </div>
            </div>
          ) : null}

          {/* 21-Module Registry */}
          {canManageModules && activeSchool ? (
            <div className="space-y-3">
              <SectionHeader
                title={`21-Module Registry (${activeSchool.enabledModules.length}/21 Enabled)`}
                subtitle="Toggle school modules dynamically to govern available portals and features"
              />
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
                {ALL_GSMS_MODULES.map((mod) => {
                  const enabled = activeSchool.enabledModules.includes(mod);
                  const meta = GSMS_MODULE_META[mod];
                  return (
                    <label
                      key={mod}
                      className={`flex items-center justify-between gap-3 rounded-xl border p-3 cursor-pointer transition-colors ${
                        enabled
                          ? 'bg-white border-[#1D4ED8]/40'
                          : 'bg-[#F5F7FB] border-[#D0D9E4] opacity-75'
                      }`}
                    >
                      <div className="min-w-0">
                        <div className="text-xs font-bold text-[#0F172A] truncate">{meta.label}</div>
                        <div className="text-[11px] text-[#475569]">{meta.category}</div>
                      </div>
                      <input
                        type="checkbox"
                        checked={enabled}
                        onChange={() => toggleModule(activeSchool, mod)}
                        className="w-4 h-4 accent-[#1D4ED8]"
                      />
                    </label>
                  );
                })}
              </div>
            </div>
          ) : null}

          {/* Supabase PostgreSQL Cloud Sync */}
          {canSupabase ? (
            <div className="bg-white rounded-2xl border border-[#D0D9E4] p-5 shadow-xs space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-xl bg-[#D1FAE5] text-[#059669] flex items-center justify-center">
                    <Database className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-sm font-bold text-[#0F172A]">
                      Supabase PostgreSQL Cloud
                    </div>
                    <div className="text-xs text-[#475569]">
                      Endpoint: {supabaseState.endpointLabel} •{' '}
                      {supabaseState.authEmail || 'Not signed in to Supabase Auth'}
                    </div>
                  </div>
                </div>
                <StatusBadge
                  text={supabaseState.connectionStatus.toUpperCase()}
                  tone={supabaseState.isAuthenticated ? 'emerald' : 'amber'}
                />
              </div>

              <div className="flex flex-wrap gap-2">
                {!supabaseState.isAuthenticated ? (
                  <button
                    type="button"
                    disabled={!supabaseState.isConfigured || supabaseState.isBusy}
                    onClick={() => setShowSbAuthModal(true)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#0F2537] text-white text-xs font-semibold whitespace-nowrap cursor-pointer disabled:opacity-45"
                  >
                    <Cloud className="w-3.5 h-3.5" />
                    Supabase Sign In / Up
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => void signOutSupabaseInsideGovernance()}
                    className="px-3 py-1.5 rounded-lg border border-[#D0D9E4] text-xs font-semibold text-[#475569] whitespace-nowrap cursor-pointer"
                  >
                    Supabase Sign Out
                  </button>
                )}
                <button
                  type="button"
                  disabled={!supabaseState.isConfigured || supabaseState.isBusy}
                  onClick={() => void testSupabase()}
                  className="px-3 py-1.5 rounded-lg bg-[#DBEAFE] text-[#1D4ED8] text-xs font-semibold whitespace-nowrap cursor-pointer disabled:opacity-45"
                >
                  Test Ping
                </button>
                <button
                  type="button"
                  disabled={!supabaseState.isAuthenticated || supabaseState.isBusy}
                  onClick={() => void pushToSupabase()}
                  className="px-3 py-1.5 rounded-lg bg-[#059669] text-white text-xs font-semibold whitespace-nowrap cursor-pointer disabled:opacity-45"
                >
                  Push {activeSchool?.code || 'Tenant'}
                </button>
                <button
                  type="button"
                  disabled={!supabaseState.isAuthenticated || supabaseState.isBusy}
                  onClick={() => void refreshSupabaseCounts()}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-[#D0D9E4] text-xs font-semibold text-[#0F172A] whitespace-nowrap cursor-pointer disabled:opacity-45"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Refresh Counts
                </button>
              </div>

              <label className="flex items-center justify-between gap-3 rounded-xl bg-[#F5F7FB] border border-[#D0D9E4] p-3 cursor-pointer">
                <span className="text-xs font-semibold text-[#0F172A]">
                  Auto-Mirror Tenant Writes to Supabase PostgreSQL
                </span>
                <input
                  type="checkbox"
                  checked={supabaseState.autoMirrorEnabled}
                  disabled={!supabaseState.isAuthenticated}
                  onChange={(e) => toggleSupabaseAutoMirror(e.target.checked)}
                  className="w-4 h-4 accent-[#059669]"
                />
              </label>

              {supabaseState.lastSyncSummary ? (
                <div className="text-xs text-[#059669] font-medium">
                  {supabaseState.lastSyncSummary}
                </div>
              ) : null}
              {supabaseState.lastError ? (
                <div className="text-xs text-[#DC2626]">{supabaseState.lastError}</div>
              ) : null}

              {Object.keys(supabaseState.remoteCounts).length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {Object.entries(supabaseState.remoteCounts).map(([tbl, count]) => (
                    <span
                      key={tbl}
                      className="px-2.5 py-1 rounded-lg bg-[#F5F7FB] border border-[#D0D9E4] text-[11px] font-mono-num text-[#0F172A]"
                    >
                      {tbl}: {count}
                    </span>
                  ))}
                </div>
              ) : null}
            </div>
          ) : null}

          {/* Low-Data & Sign Out */}
          <div className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex flex-wrap items-center justify-between gap-4">
            <label className="flex items-center gap-3 cursor-pointer">
              <SlidersHorizontal className="w-4 h-4 text-[#475569]" />
              <div>
                <div className="text-xs font-bold text-[#0F172A]">
                  Low-Data &amp; Offline Sync Mode
                </div>
                <div className="text-[11px] text-[#475569]">
                  Omits decorative banner photography for constrained school networks
                </div>
              </div>
              <input
                type="checkbox"
                checked={workspace.userProfile?.lowDataMode ?? false}
                onChange={(e) => toggleLowDataMode(e.target.checked)}
                className="w-4 h-4 accent-[#1D4ED8]"
              />
            </label>

            <button
              type="button"
              onClick={() => void signOutAll()}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#FEE2E2] hover:bg-[#FECACA] text-[#DC2626] text-xs font-bold whitespace-nowrap shrink-0 cursor-pointer"
            >
              <LogOut className="w-3.5 h-3.5" />
              Sign Out
            </button>
          </div>
        </div>
      )}

      {/* Modal: Provision School */}
      <ModalDialog
        open={showNewSchoolModal}
        title="Provision New School Tenant"
        onClose={() => setShowNewSchoolModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setShowNewSchoolModal(false)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (!schoolName.trim() || !schoolCode.trim()) return;
                createSchool(schoolName, schoolCode, schoolCity, schoolCountry);
                setSchoolName('');
                setSchoolCode('');
                setSchoolCity('');
                setSchoolCountry('');
                setShowNewSchoolModal(false);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              <Building2 className="w-3.5 h-3.5" />
              Create Tenant
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">School Name</label>
            <input
              type="text"
              value={schoolName}
              onChange={(e) => setSchoolName(e.target.value)}
              placeholder="e.g. Summit Ridge Academy"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Code</label>
              <input
                type="text"
                value={schoolCode}
                onChange={(e) => setSchoolCode(e.target.value)}
                placeholder="SRA-01"
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">City</label>
              <input
                type="text"
                value={schoolCity}
                onChange={(e) => setSchoolCity(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Country</label>
              <input
                type="text"
                value={schoolCountry}
                onChange={(e) => setSchoolCountry(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
        </div>
      </ModalDialog>

      {/* Modal: Add Branch */}
      <ModalDialog
        open={showNewBranchModal}
        title="Add Campus Branch"
        onClose={() => setShowNewBranchModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setShowNewBranchModal(false)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (!activeSchool || !branchName.trim() || !branchCode.trim()) return;
                createBranch(activeSchool.id, branchName, branchCode, branchCity);
                setBranchName('');
                setBranchCode('');
                setBranchCity('');
                setShowNewBranchModal(false);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              Save Branch
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Branch Name</label>
            <input
              type="text"
              value={branchName}
              onChange={(e) => setBranchName(e.target.value)}
              placeholder="e.g. West Science Annex"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Code</label>
              <input
                type="text"
                value={branchCode}
                onChange={(e) => setBranchCode(e.target.value)}
                placeholder="BR-03"
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">City</label>
              <input
                type="text"
                value={branchCity}
                onChange={(e) => setBranchCity(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
        </div>
      </ModalDialog>

      {/* Modal: Add User Access */}
      <ModalDialog
        open={showAddUserModal}
        title="Provision School User Portal Access"
        onClose={() => setShowAddUserModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setShowAddUserModal(false)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (!activeSchool || !newUserName.trim() || !newUserEmail.trim()) return;
                createAccessAssignment(
                  activeSchool.id,
                  newUserName,
                  newUserEmail,
                  newUserRole,
                  newUserClasses.split(',').map((s) => s.trim()).filter(Boolean),
                  newUserSubjects.split(',').map((s) => s.trim()).filter(Boolean),
                  newUserStudents.split(',').map((s) => s.trim()).filter(Boolean),
                  []
                );
                setNewUserName('');
                setNewUserEmail('');
                setShowAddUserModal(false);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              <UserPlus className="w-3.5 h-3.5" />
              Provision User
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Full Name</label>
            <input
              type="text"
              value={newUserName}
              onChange={(e) => setNewUserName(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Email</label>
            <input
              type="email"
              value={newUserEmail}
              onChange={(e) => setNewUserEmail(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Primary Role</label>
            <select
              value={newUserRole}
              onChange={(e) => setNewUserRole(e.target.value as PortalRole)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB]"
            >
              {ALL_PORTAL_ROLES.filter((r) => r !== PortalRole.SUPER_ADMIN).map((r) => (
                <option key={r} value={r}>
                  {PORTAL_ROLE_META[r].displayName}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">
                Assigned Classes (CSV)
              </label>
              <input
                type="text"
                value={newUserClasses}
                onChange={(e) => setNewUserClasses(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">
                Assigned Subjects (CSV)
              </label>
              <input
                type="text"
                value={newUserSubjects}
                onChange={(e) => setNewUserSubjects(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">
              Linked Student IDs (CSV)
            </label>
            <input
              type="text"
              value={newUserStudents}
              onChange={(e) => setNewUserStudents(e.target.value)}
              placeholder="e.g. stu_1, stu_2"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
        </div>
      </ModalDialog>

      {/* Modal: Delegate Permissions */}
      <ModalDialog
        open={Boolean(delegatingUser)}
        title={`Delegate Permissions • ${delegatingUser?.fullName || ''}`}
        onClose={() => setDelegatingUser(null)}
        footer={
          delegatingUser && activeSchool ? (
            <>
              <button
                type="button"
                onClick={() => setDelegatingUser(null)}
                className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  delegateUserPermissions(
                    activeSchool.id,
                    delegatingUser,
                    Array.from(delExtras),
                    delClasses.split(',').map((s) => s.trim()).filter(Boolean),
                    delSubjects.split(',').map((s) => s.trim()).filter(Boolean),
                    delStudents.split(',').map((s) => s.trim()).filter(Boolean)
                  );
                  setDelegatingUser(null);
                }}
                className="px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
              >
                Save Delegations
              </button>
            </>
          ) : null
        }
      >
        <div className="space-y-3">
          <div className="rounded-xl bg-[#F5F7FB] border border-[#D0D9E4] p-3 text-xs text-[#475569]">
            Primary role <span className="font-bold text-[#0F172A]">{delegatingUser?.primaryRole}</span> is
            strictly locked. Grant additional capabilities below:
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
            <div>
              <label className="block text-[11px] font-semibold text-[#475569] mb-1">Classes CSV</label>
              <input
                type="text"
                value={delClasses}
                onChange={(e) => setDelClasses(e.target.value)}
                className="w-full px-2.5 py-1.5 text-xs rounded-lg border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-[#475569] mb-1">Subjects CSV</label>
              <input
                type="text"
                value={delSubjects}
                onChange={(e) => setDelSubjects(e.target.value)}
                className="w-full px-2.5 py-1.5 text-xs rounded-lg border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-[#475569] mb-1">Students CSV</label>
              <input
                type="text"
                value={delStudents}
                onChange={(e) => setDelStudents(e.target.value)}
                className="w-full px-2.5 py-1.5 text-xs rounded-lg border border-[#D0D9E4]"
              />
            </div>
          </div>

          <div className="max-h-60 overflow-y-auto space-y-1.5 pr-1">
            {delegatableCapabilities.map((cap) => {
              const checked = delExtras.has(cap);
              const meta = ROLE_CAPABILITY_META[cap];
              return (
                <label
                  key={cap}
                  className="flex items-center justify-between gap-2 rounded-xl border border-[#D0D9E4] px-3 py-2 text-xs cursor-pointer hover:bg-[#F5F7FB]"
                >
                  <div>
                    <div className="font-semibold text-[#0F172A]">{meta.label}</div>
                    <div className="text-[10px] text-[#475569]">{meta.category}</div>
                  </div>
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={(e) => {
                      const next = new Set(delExtras);
                      if (e.target.checked) next.add(cap);
                      else next.delete(cap);
                      setDelExtras(next);
                    }}
                    className="w-4 h-4 accent-[#7C3AED]"
                  />
                </label>
              );
            })}
          </div>
        </div>
      </ModalDialog>

      {/* Modal: Supabase Auth inside Governance */}
      <ModalDialog
        open={showSbAuthModal}
        title="Supabase PostgreSQL Authentication"
        onClose={() => setShowSbAuthModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => {
                void signUpSupabaseInsideGovernance(sbEmail, sbPassword);
                setShowSbAuthModal(false);
              }}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#0F172A] cursor-pointer"
            >
              Sign Up
            </button>
            <button
              type="button"
              onClick={() => {
                void signInSupabaseInsideGovernance(sbEmail, sbPassword);
                setShowSbAuthModal(false);
              }}
              className="px-4 py-2 rounded-xl bg-[#059669] text-white text-xs font-semibold cursor-pointer"
            >
              Sign In
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Email</label>
            <input
              type="email"
              value={sbEmail}
              onChange={(e) => setSbEmail(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Password</label>
            <input
              type="password"
              value={sbPassword}
              onChange={(e) => setSbPassword(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
        </div>
      </ModalDialog>
    </div>
  );
};
