import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import {
  buildRoleScopedSummary,
  generateWorkflowText,
  streamAssistantResponse
} from '../data/aiClient.ts';
import { MultiTenantStore, SchoolTenantData, createInitialMultiTenantStore } from '../data/seedData.ts';
import {
  canSyncSupabase,
  fetchSupabaseStatus,
  getActiveSupabaseSession,
  pullSupabaseCounts,
  pushWorkspaceToSupabase,
  sendSupabasePasswordReset,
  signInSupabase,
  signOutSupabase,
  signUpSupabase,
  testSupabaseConnection
} from '../data/supabaseService.ts';
import { RolePolicy } from '../policy/RolePolicy.ts';
import {
  AiUiState,
  ALL_PORTAL_ROLES,
  AuditLog,
  DEFAULT_ENABLED_MODULES,
  EarlyWarningAlert,
  FinanceEntry,
  GsmsModule,
  GsmsWorkspaceState,
  LeaveRequest,
  LeaveStatus,
  PORTAL_ROLE_META,
  PortalRole,
  ReportCard,
  ReportStatus,
  ReportTemplate,
  RiskLevel,
  RoleCapability,
  School,
  StaffMember,
  Student,
  SupabaseSyncState,
  UserAccessAssignment,
  parseEnumOrDefault,
  parseExtraCapabilities,
  resolveEffectiveRole,
  resolvePrimaryRole
} from '../types/gsms.ts';

export interface AuthSession {
  userId: string;
  displayName: string;
  email: string | null;
  backend: 'LOCAL' | 'SUPABASE';
}

interface GsmsContextValue {
  session: AuthSession | null;
  workspace: GsmsWorkspaceState | null;
  aiState: AiUiState;
  supabaseState: SupabaseSyncState;
  searchQuery: string;
  focusStudentId: string | null;
  bannerMessage: string | null;
  setSearchQuery: (q: string) => void;
  clearBanner: () => void;
  showBanner: (msg: string) => void;
  enterWorkspaceLocal: (displayName?: string, email?: string) => void;
  signInWithSupabaseEmail: (email: string, password: string) => Promise<{ ok: boolean; message: string }>;
  signUpWithSupabaseEmail: (email: string, password: string, fullName: string) => Promise<{ ok: boolean; message: string }>;
  resetSupabasePassword: (email: string) => Promise<{ ok: boolean; message: string }>;
  signOutAll: () => Promise<void>;
  selectFocusStudent: (student: Student) => void;
  switchRole: (role: PortalRole) => void;
  switchSchool: (school: School) => void;
  switchBranch: (branchId: string) => void;
  toggleLowDataMode: (enabled: boolean) => void;
  toggleModule: (school: School, mod: GsmsModule) => void;
  createSchool: (name: string, code: string, city: string, country: string) => void;
  createBranch: (schoolId: string, name: string, code: string, city: string) => void;
  updateAcademicPeriod: (schoolId: string, year: string, term: string) => void;
  delegateUserPermissions: (
    schoolId: string,
    assignment: UserAccessAssignment,
    extraPermissions: string[],
    assignedClasses: string[],
    assignedSubjects: string[],
    linkedStudentIds: string[]
  ) => void;
  createAccessAssignment: (
    schoolId: string,
    fullName: string,
    email: string,
    primaryRole: PortalRole,
    assignedClasses: string[],
    assignedSubjects: string[],
    linkedStudentIds: string[],
    extraPermissions: string[]
  ) => void;
  addStudent: (
    school: School,
    branchId: string,
    admissionNo: string,
    fullName: string,
    className: string,
    parentName: string
  ) => void;
  updateStudent: (
    schoolId: string,
    student: Student,
    patch: {
      fullName: string;
      className: string;
      stream: string;
      parentName: string;
      parentContact: string;
    }
  ) => void;
  removeStudent: (schoolId: string, student: Student) => void;
  addStaffMember: (
    school: School,
    branchId: string,
    input: {
      fullName: string;
      roleTitle: string;
      department: string;
      subjectsSummary: string;
      workloadHours: number;
      leaveBalanceDays: number;
    }
  ) => void;
  updateStaffMember: (
    schoolId: string,
    staff: StaffMember,
    patch: {
      fullName: string;
      roleTitle: string;
      department: string;
      subjectsSummary: string;
      workloadHours: number;
      leaveBalanceDays: number;
    }
  ) => void;
  removeStaffMember: (schoolId: string, staff: StaffMember) => void;
  recordAttendance: (school: School, branchId: string, student: Student, status: string) => void;
  addAcademicRecord: (
    school: School,
    branchId: string,
    recordType: string,
    title: string,
    className: string,
    subjectName: string,
    teacherName: string,
    scheduleSlot: string,
    score: number
  ) => void;
  createReportTemplate: (
    school: School,
    name: string,
    classLevel: string,
    sectionsCsv: string,
    gradingScale: string
  ) => void;
  saveOrSubmitReportCard: (
    school: School,
    branchId: string,
    student: Student,
    template: ReportTemplate,
    gradesSummary: string,
    averageScore: number,
    teacherRemarks: string,
    aiSummary: string | null,
    submitNow: boolean
  ) => void;
  reviewReportCard: (
    schoolId: string,
    report: ReportCard,
    nextStatus: ReportStatus,
    headteacherRemarks: string
  ) => void;
  submitLeaveRequest: (
    school: School,
    branchId: string,
    staff: StaffMember,
    leaveType: string,
    startDate: string,
    endDate: string,
    daysCount: number,
    reason: string,
    aiConflictNote: string | null
  ) => void;
  reviewLeaveRequest: (
    schoolId: string,
    leave: LeaveRequest,
    staffMember: StaffMember | undefined,
    approve: boolean
  ) => void;
  recordFinancePayment: (schoolId: string, entry: FinanceEntry, amountUsd: number) => void;
  sendCommunication: (
    school: School,
    branchId: string,
    channel: string,
    audience: string,
    title: string,
    body: string,
    aiDrafted: boolean
  ) => void;
  addOperationRecord: (
    school: School,
    branchId: string,
    moduleType: string,
    title: string,
    subtitle: string,
    partyName: string,
    status: string,
    metricLabel: string,
    notes: string
  ) => void;
  updateAlertStatus: (schoolId: string, alert: EarlyWarningAlert, status: string) => void;
  askAiCopilot: (prompt: string) => Promise<void>;
  generateQuickAiDraft: (taskPrompt: string, actionType: string) => Promise<string | null>;
  testSupabase: () => Promise<void>;
  pushToSupabase: () => Promise<void>;
  refreshSupabaseCounts: () => Promise<void>;
  toggleSupabaseAutoMirror: (enabled: boolean) => void;
  signInSupabaseInsideGovernance: (email: string, password: string) => Promise<void>;
  signUpSupabaseInsideGovernance: (email: string, password: string) => Promise<void>;
  signOutSupabaseInsideGovernance: () => Promise<void>;
}

const GsmsContext = createContext<GsmsContextValue | null>(null);

const SESSION_STORAGE_KEY = 'gsms_active_session_v1';
const STORE_PREFIX = 'gsms_workspace_store_v1_';
const LOCAL_ACCOUNTS_KEY = 'gsms_local_accounts_v1';

interface LocalAccountRecord {
  userId: string;
  email: string;
  displayName: string;
  passwordHash: string;
}

async function hashPassword(raw: string): Promise<string> {
  if (typeof window !== 'undefined' && window.crypto?.subtle) {
    const encoded = new TextEncoder().encode(`gsms_v1:${raw}`);
    const digest = await window.crypto.subtle.digest('SHA-256', encoded);
    return Array.from(new Uint8Array(digest))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('');
  }
  return btoa(unescape(encodeURIComponent(`gsms_v1:${raw}`)));
}

function readLocalAccounts(): Record<string, LocalAccountRecord> {
  try {
    const raw = localStorage.getItem(LOCAL_ACCOUNTS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Record<string, LocalAccountRecord>;
      if (parsed && typeof parsed === 'object') return parsed;
    }
  } catch {
    // ignore corrupted storage
  }
  return {};
}

function writeLocalAccounts(accounts: Record<string, LocalAccountRecord>): void {
  try {
    localStorage.setItem(LOCAL_ACCOUNTS_KEY, JSON.stringify(accounts));
  } catch {
    // ignore storage quota errors
  }
}

function uidGen(prefix: string): string {
  return `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 7)}`;
}

function loadOrCreateStore(session: AuthSession): MultiTenantStore {
  try {
    const raw = localStorage.getItem(`${STORE_PREFIX}${session.userId}`);
    if (raw) {
      const parsed = JSON.parse(raw) as MultiTenantStore;
      if (parsed && parsed.userProfile && Array.isArray(parsed.schools) && parsed.tenants) {
        return parsed;
      }
    }
  } catch {
    // ignore corrupted local storage
  }
  const fresh = createInitialMultiTenantStore(session.userId, session.displayName, session.email);
  try {
    localStorage.setItem(`${STORE_PREFIX}${session.userId}`, JSON.stringify(fresh));
  } catch {
    // ignore storage quota
  }
  return fresh;
}

export const GsmsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [session, setSession] = useState<AuthSession | null>(() => {
    try {
      const raw = localStorage.getItem(SESSION_STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as AuthSession;
        if (parsed?.userId) return parsed;
      }
    } catch {
      // ignore
    }
    return null;
  });

  const [store, setStore] = useState<MultiTenantStore | null>(() =>
    session ? loadOrCreateStore(session) : null
  );

  const [searchQuery, setSearchQuery] = useState('');
  const [focusStudentId, setFocusStudentId] = useState<string | null>(null);
  const [bannerMessage, setBannerMessage] = useState<string | null>(null);
  const [aiState, setAiState] = useState<AiUiState>({ status: 'idle' });
  const [supabaseState, setSupabaseState] = useState<SupabaseSyncState>({
    isConfigured: false,
    configIssue: null,
    endpointLabel: 'Checking...',
    isAuthenticated: false,
    authEmail: null,
    autoMirrorEnabled: false,
    isBusy: false,
    connectionStatus: 'Checking',
    lastLatencyMs: null,
    lastSyncSummary: null,
    lastError: null,
    remoteCounts: {}
  });

  useEffect(() => {
    void fetchSupabaseStatus().then((partial) => {
      setSupabaseState((prev) => ({ ...prev, ...partial }));
    });
  }, []);

  useEffect(() => {
    if (session && store) {
      try {
        localStorage.setItem(`${STORE_PREFIX}${session.userId}`, JSON.stringify(store));
      } catch {
        // ignore quota errors
      }
    }
  }, [session, store]);

  const workspace: GsmsWorkspaceState | null = useMemo(() => {
    if (!store) return null;
    const { userProfile, schools, tenants } = store;
    const activeSchool =
      schools.find((s) => s.id === userProfile.activeSchoolId) ?? schools[0] ?? null;
    const tenant: SchoolTenantData | undefined = activeSchool ? tenants[activeSchool.id] : undefined;
    const activeRole = resolveEffectiveRole(userProfile);
    const branches = tenant?.branches ?? [];
    const activeBranchId =
      branches.find((b) => b.id === userProfile.activeBranchId)?.id ??
      branches[0]?.id ??
      'branch_main';

    const extraCaps = Array.from(parseExtraCapabilities(userProfile.extraPermissions));

    return {
      userProfile,
      schools,
      activeSchool,
      branches,
      activeBranchId,
      activeRole,
      accessAssignments: tenant?.accessAssignments ?? [],
      activeExtraCapabilities: extraCaps,
      assignedClasses: userProfile.assignedClasses,
      assignedSubjects: userProfile.assignedSubjects,
      linkedStudentIds: userProfile.linkedStudentIds,
      students: tenant?.students ?? [],
      staff: tenant?.staff ?? [],
      academics: tenant?.academics ?? [],
      attendance: tenant?.attendance ?? [],
      reportTemplates: tenant?.reportTemplates ?? [],
      reportCards: tenant?.reportCards ?? [],
      finance: tenant?.finance ?? [],
      leaveRequests: tenant?.leaveRequests ?? [],
      communications: tenant?.communications ?? [],
      operations: tenant?.operations ?? [],
      alerts: tenant?.alerts ?? [],
      auditLogs: tenant?.auditLogs ?? []
    };
  }, [store]);

  const workspaceRef = useRef<GsmsWorkspaceState | null>(workspace);
  workspaceRef.current = workspace;

  const autoMirrorIfEnabled = useCallback(
    (nextWorkspace: GsmsWorkspaceState | null) => {
      if (!nextWorkspace) return;
      if (
        supabaseState.isConfigured &&
        supabaseState.isAuthenticated &&
        supabaseState.autoMirrorEnabled &&
        getActiveSupabaseSession() &&
        canSyncSupabase(nextWorkspace.activeRole, nextWorkspace.activeExtraCapabilities)
      ) {
        void pushWorkspaceToSupabase(nextWorkspace)
          .then((res) => {
            setSupabaseState((prev) => ({
              ...prev,
              lastSyncSummary: res.summary,
              lastLatencyMs: res.latencyMs,
              remoteCounts: res.counts,
              lastError: null
            }));
          })
          .catch(() => {
            // non-blocking auto mirror
          });
      }
    },
    [supabaseState.autoMirrorEnabled, supabaseState.isAuthenticated, supabaseState.isConfigured]
  );

  const clearBanner = useCallback(() => setBannerMessage(null), []);
  const showBanner = useCallback((msg: string) => setBannerMessage(msg), []);

  const startSession = useCallback((nextSession: AuthSession) => {
    try {
      localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(nextSession));
    } catch {
      // ignore
    }
    const loaded = loadOrCreateStore(nextSession);
    setSession(nextSession);
    setStore(loaded);
    setFocusStudentId(null);
    setAiState({ status: 'idle' });
  }, []);

  const enterWorkspaceLocal = useCallback(
    (displayName = 'GSMS Executive', email = 'executive@oakridge.edu') => {
      const cleanName = displayName.trim() || 'GSMS Executive';
      const cleanEmail = email.trim() || 'executive@oakridge.edu';
      startSession({
        userId: 'gsms_demo_exec',
        displayName: cleanName,
        email: cleanEmail,
        backend: 'LOCAL'
      });
    },
    [startSession]
  );

  const signInWithSupabaseEmail = useCallback(
    async (email: string, password: string) => {
      const cleanEmail = email.trim();
      const emailKey = cleanEmail.toLowerCase();
      if (!cleanEmail.includes('@')) {
        return { ok: false, message: 'Enter a valid email address.' };
      }
      if (password.length < 6) {
        return { ok: false, message: 'Password must be at least 6 characters.' };
      }

      if (!supabaseState.isConfigured) {
        const accounts = readLocalAccounts();
        const existing = accounts[emailKey];
        const passHash = await hashPassword(password);

        if (existing) {
          if (existing.passwordHash && existing.passwordHash !== passHash) {
            return {
              ok: false,
              message: `Incorrect password for ${cleanEmail}. Click Forgot Password to reset it.`
            };
          }
          if (!existing.passwordHash) {
            accounts[emailKey] = { ...existing, passwordHash: passHash };
            writeLocalAccounts(accounts);
          }
          startSession({
            userId: existing.userId,
            displayName: existing.displayName,
            email: existing.email,
            backend: 'LOCAL'
          });
          return { ok: true, message: `Signed in as ${existing.email}` };
        }

        const inferredName =
          cleanEmail
            .split('@')[0]
            .replace(/[._-]+/g, ' ')
            .replace(/\b\w/g, (c) => c.toUpperCase()) || 'GSMS User';
        const newAccount: LocalAccountRecord = {
          userId: `local_${emailKey.replace(/[^a-z0-9]/g, '_')}`,
          email: cleanEmail,
          displayName: inferredName,
          passwordHash: passHash
        };
        accounts[emailKey] = newAccount;
        writeLocalAccounts(accounts);
        startSession({
          userId: newAccount.userId,
          displayName: newAccount.displayName,
          email: newAccount.email,
          backend: 'LOCAL'
        });
        return { ok: true, message: `Signed in as ${cleanEmail}` };
      }

      setSupabaseState((p) => ({ ...p, isBusy: true, lastError: null }));
      try {
        const res = await signInSupabase(email, password);
        if (res.session) {
          setSupabaseState((p) => ({
            ...p,
            isBusy: false,
            isAuthenticated: true,
            authEmail: res.session!.email,
            connectionStatus: 'Authenticated',
            lastSyncSummary: res.message
          }));
          startSession({
            userId: res.session.userId,
            displayName: res.session.displayName,
            email: res.session.email,
            backend: 'SUPABASE'
          });
          return { ok: true, message: res.message };
        }
        setSupabaseState((p) => ({ ...p, isBusy: false, lastSyncSummary: res.message }));
        return { ok: true, message: res.message };
      } catch (err) {
        const msg = err instanceof Error ? err.message : 'Email authentication failed.';
        setSupabaseState((p) => ({ ...p, isBusy: false, lastError: msg }));
        return { ok: false, message: msg };
      }
    },
    [startSession, supabaseState.isConfigured]
  );

  const signUpWithSupabaseEmail = useCallback(
    async (email: string, password: string, fullName: string) => {
      const cleanEmail = email.trim();
      const emailKey = cleanEmail.toLowerCase();
      const cleanName = fullName.trim();
      if (!cleanEmail.includes('@')) {
        return { ok: false, message: 'Enter a valid email address.' };
      }
      if (password.length < 6) {
        return { ok: false, message: 'Password must be at least 6 characters.' };
      }

      if (!supabaseState.isConfigured) {
        const accounts = readLocalAccounts();
        if (accounts[emailKey]) {
          return {
            ok: false,
            message: `An account for ${cleanEmail} already exists. Switch to Sign In.`
          };
        }
        const passHash = await hashPassword(password);
        const newAccount: LocalAccountRecord = {
          userId: `local_${emailKey.replace(/[^a-z0-9]/g, '_')}`,
          email: cleanEmail,
          displayName: cleanName || cleanEmail.split('@')[0],
          passwordHash: passHash
        };
        accounts[emailKey] = newAccount;
        writeLocalAccounts(accounts);
        startSession({
          userId: newAccount.userId,
          displayName: newAccount.displayName,
          email: newAccount.email,
          backend: 'LOCAL'
        });
        return { ok: true, message: `Account created and signed in as ${cleanEmail}` };
      }

      setSupabaseState((p) => ({ ...p, isBusy: true, lastError: null }));
      try {
        const res = await signUpSupabase(email, password, fullName);
        if (res.session) {
          setSupabaseState((p) => ({
            ...p,
            isBusy: false,
            isAuthenticated: true,
            authEmail: res.session!.email,
            connectionStatus: 'Authenticated',
            lastSyncSummary: res.message
          }));
          startSession({
            userId: res.session.userId,
            displayName: res.session.displayName,
            email: res.session.email,
            backend: 'SUPABASE'
          });
          return { ok: true, message: res.message };
        }
        setSupabaseState((p) => ({ ...p, isBusy: false, lastSyncSummary: res.message }));
        return { ok: true, message: res.message };
      } catch (err) {
        const msg = err instanceof Error ? err.message : 'Registration failed.';
        setSupabaseState((p) => ({ ...p, isBusy: false, lastError: msg }));
        return { ok: false, message: msg };
      }
    },
    [startSession, supabaseState.isConfigured]
  );

  const resetSupabasePassword = useCallback(
    async (email: string) => {
      const cleanEmail = email.trim();
      const emailKey = cleanEmail.toLowerCase();
      if (!cleanEmail.includes('@')) {
        return { ok: false, message: 'Enter your account email address first.' };
      }

      if (!supabaseState.isConfigured) {
        const accounts = readLocalAccounts();
        const existing = accounts[emailKey];
        if (!existing) {
          return {
            ok: true,
            message: `No local password lock found for ${cleanEmail}. Enter a password and click Sign In.`
          };
        }
        accounts[emailKey] = { ...existing, passwordHash: '' };
        writeLocalAccounts(accounts);
        return {
          ok: true,
          message: `Password reset for ${cleanEmail}. Enter your new password and click Sign In.`
        };
      }

      setSupabaseState((p) => ({ ...p, isBusy: true, lastError: null }));
      try {
        const message = await sendSupabasePasswordReset(email);
        setSupabaseState((p) => ({ ...p, isBusy: false, lastSyncSummary: message }));
        return { ok: true, message };
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Password reset failed.';
        setSupabaseState((p) => ({ ...p, isBusy: false, lastError: message }));
        return { ok: false, message };
      }
    },
    [supabaseState.isConfigured]
  );

  const signOutAll = useCallback(async () => {
    await signOutSupabase();
    try {
      localStorage.removeItem(SESSION_STORAGE_KEY);
    } catch {
      // ignore
    }
    setSession(null);
    setStore(null);
    setFocusStudentId(null);
    setAiState({ status: 'idle' });
    setSupabaseState((p) => ({
      ...p,
      isAuthenticated: false,
      authEmail: null,
      autoMirrorEnabled: false,
      remoteCounts: {}
    }));
  }, []);

  const requireCap = useCallback(
    (cap: RoleCapability, actionLabel: string): boolean => {
      const w = workspaceRef.current;
      if (!w) return false;
      if (!RolePolicy.has(w.activeRole, cap, w.activeExtraCapabilities)) {
        const meta = PORTAL_ROLE_META[w.activeRole];
        setBannerMessage(`Access denied: ${meta.shortTitle} cannot ${actionLabel}.`);
        return false;
      }
      return true;
    },
    []
  );

  const appendAudit = (
    tenant: SchoolTenantData,
    schoolId: string,
    ownerId: string,
    actorName: string,
    actorRole: PortalRole,
    actionType: string,
    moduleName: string,
    summary: string,
    aiAction = false
  ): AuditLog[] => {
    const newLog: AuditLog = {
      id: uidGen('log'),
      schoolId,
      ownerId,
      actorName,
      actorRole,
      actionType,
      moduleName,
      summary,
      aiAction,
      createdAtIso: new Date().toISOString()
    };
    return [newLog, ...tenant.auditLogs].slice(0, 100);
  };

  const selectFocusStudent = useCallback((student: Student) => {
    const w = workspaceRef.current;
    if (!w) return;
    if (!RolePolicy.requiresLearnerScope(w.activeRole, w.activeExtraCapabilities)) {
      setBannerMessage(`Access denied: ${PORTAL_ROLE_META[w.activeRole].shortTitle} is not scoped to a single learner.`);
      return;
    }
    setFocusStudentId(student.id);
    setBannerMessage(`Viewing ${student.fullName}`);
  }, []);

  const switchRole = useCallback((role: PortalRole) => {
    setStore((prev) => {
      if (!prev) return prev;
      const primary = resolvePrimaryRole(prev.userProfile);
      if (primary !== PortalRole.SUPER_ADMIN) {
        setBannerMessage(`Locked: ${PORTAL_ROLE_META[primary].displayName} cannot switch portals.`);
        return prev;
      }
      return {
        ...prev,
        userProfile: {
          ...prev.userProfile,
          activeRole: role
        }
      };
    });
    const w = workspaceRef.current;
    if (w && resolvePrimaryRole(w.userProfile) === PortalRole.SUPER_ADMIN) {
      setBannerMessage(`Switched to ${PORTAL_ROLE_META[role].displayName}`);
    }
  }, []);

  const switchSchool = useCallback(
    (school: School) => {
      if (!requireCap(RoleCapability.SWITCH_SCHOOL_TENANT, 'switch school tenants')) return;
      setStore((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          userProfile: {
            ...prev.userProfile,
            activeSchoolId: school.id,
            activeBranchId: 'branch_main'
          }
        };
      });
      setFocusStudentId(null);
      setBannerMessage(`Active tenant: ${school.name}`);
    },
    [requireCap]
  );

  const switchBranch = useCallback(
    (branchId: string) => {
      if (!requireCap(RoleCapability.SWITCH_BRANCH, 'switch school branches')) return;
      setStore((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          userProfile: {
            ...prev.userProfile,
            activeBranchId: branchId
          }
        };
      });
      const br = workspaceRef.current?.branches.find((b) => b.id === branchId);
      setBannerMessage(`Active branch: ${br?.name ?? branchId}`);
    },
    [requireCap]
  );

  const toggleLowDataMode = useCallback((enabled: boolean) => {
    setStore((prev) => {
      if (!prev) return prev;
      return {
        ...prev,
        userProfile: { ...prev.userProfile, lowDataMode: enabled }
      };
    });
    setBannerMessage(enabled ? 'Low-Data Mode enabled' : 'Standard Mode enabled');
  }, []);

  const toggleModule = useCallback(
    (school: School, mod: GsmsModule) => {
      if (!requireCap(RoleCapability.MANAGE_MODULES, 'modify the Module Registry')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const ownerId = prev.userProfile.userId;
        const hasMod = school.enabledModules.includes(mod);
        const nextMods = hasMod
          ? school.enabledModules.filter((m) => m !== mod)
          : [...school.enabledModules, mod];
        const nextSchools = prev.schools.map((s) =>
          s.id === school.id ? { ...s, enabledModules: nextMods } : s
        );
        const tenant = prev.tenants[school.id];
        const nextTenants = tenant
          ? {
              ...prev.tenants,
              [school.id]: {
                ...tenant,
                auditLogs: appendAudit(
                  tenant,
                  school.id,
                  ownerId,
                  actorName,
                  actorRole,
                  'MODULE_TOGGLED',
                  mod,
                  `${hasMod ? 'Disabled' : 'Enabled'} module ${mod} for ${school.name}`
                )
              }
            }
          : prev.tenants;
        return { ...prev, schools: nextSchools, tenants: nextTenants };
      });
      setBannerMessage(`Updated module: ${mod}`);
    },
    [requireCap]
  );

  const createSchool = useCallback(
    (name: string, code: string, city: string, country: string) => {
      if (!requireCap(RoleCapability.CREATE_SCHOOL_TENANT, 'provision new schools')) return;
      const newId = uidGen('school');
      setStore((prev) => {
        if (!prev) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const newSchool: School = {
          id: newId,
          ownerId,
          name: name.trim(),
          code: code.trim().toUpperCase(),
          country: country.trim() || 'Global',
          city: city.trim() || 'Main City',
          plan: 'ENTERPRISE',
          subscriptionStatus: 'ACTIVE',
          brandColorHex: '#1D4ED8',
          activeAcademicYear: '2026/2027',
          activeTerm: 'Term 1',
          enabledModules: [...DEFAULT_ENABLED_MODULES],
          studentCount: 0,
          staffCount: 0,
          monthlyRevenueUsd: 2500,
          churnRiskScore: 5
        };
        const newTenant: SchoolTenantData = {
          branches: [
            {
              id: 'branch_main',
              schoolId: newId,
              ownerId,
              name: 'Main Campus',
              code: 'BR-01',
              city: city.trim() || 'Main City',
              mainCampus: true
            }
          ],
          accessAssignments: [],
          students: [],
          staff: [],
          academics: [],
          attendance: [],
          reportTemplates: [
            {
              id: uidGen('tpl'),
              schoolId: newId,
              ownerId,
              name: `${newSchool.name} Standard Report`,
              classLevel: 'All Classes',
              sections: ['Core Subjects', 'Attendance', 'Teacher Remarks', 'Principal Endorsement'],
              gradingScale: 'A-F Percentage',
              active: true
            }
          ],
          reportCards: [],
          finance: [],
          leaveRequests: [],
          communications: [],
          operations: [],
          alerts: [],
          auditLogs: [
            {
              id: uidGen('log'),
              schoolId: newId,
              ownerId,
              actorName,
              actorRole,
              actionType: 'SCHOOL_CREATED',
              moduleName: 'SYSTEM',
              summary: `Created school tenant ${name.trim()}`,
              aiAction: false,
              createdAtIso: new Date().toISOString()
            }
          ]
        };
        return {
          userProfile: {
            ...prev.userProfile,
            activeSchoolId: newId,
            activeBranchId: 'branch_main'
          },
          schools: [...prev.schools, newSchool],
          tenants: { ...prev.tenants, [newId]: newTenant }
        };
      });
      setBannerMessage(`Provisioned new school tenant: ${name.trim()}`);
    },
    [requireCap]
  );

  const createBranch = useCallback(
    (schoolId: string, name: string, code: string, city: string) => {
      if (!requireCap(RoleCapability.MANAGE_BRANCHES, 'create campus branches')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const branch = {
          id: uidGen('branch'),
          schoolId,
          ownerId,
          name: name.trim(),
          code: code.trim().toUpperCase(),
          city: city.trim() || 'Campus City',
          mainCampus: false
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              branches: [...tenant.branches, branch],
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                'BRANCH_CREATED',
                'SYSTEM',
                `Added campus branch ${branch.name} (${branch.code})`
              )
            }
          }
        };
      });
      setBannerMessage(`Added campus branch: ${name.trim()}`);
    },
    [requireCap]
  );

  const updateAcademicPeriod = useCallback(
    (schoolId: string, year: string, term: string) => {
      if (!requireCap(RoleCapability.UPDATE_ACADEMIC_PERIOD, 'change academic year/term')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const tenant = prev.tenants[schoolId];
        return {
          ...prev,
          schools: prev.schools.map((s) =>
            s.id === schoolId
              ? { ...s, activeAcademicYear: year.trim(), activeTerm: term.trim() }
              : s
          ),
          tenants: tenant
            ? {
                ...prev.tenants,
                [schoolId]: {
                  ...tenant,
                  auditLogs: appendAudit(
                    tenant,
                    schoolId,
                    ownerId,
                    actorName,
                    actorRole,
                    'ACADEMIC_PERIOD_UPDATED',
                    'ACADEMICS',
                    `Set active period to ${year.trim()} (${term.trim()})`
                  )
                }
              }
            : prev.tenants
        };
      });
      setBannerMessage(`Active period updated to ${year} (${term})`);
    },
    [requireCap]
  );

  const delegateUserPermissions = useCallback(
    (
      schoolId: string,
      assignment: UserAccessAssignment,
      extraPermissions: string[],
      assignedClasses: string[],
      assignedSubjects: string[],
      linkedStudentIds: string[]
    ) => {
      if (!requireCap(RoleCapability.MANAGE_USER_PERMISSIONS, 'delegate user permissions')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const updatedAssignments = tenant.accessAssignments.map((a) =>
          a.id === assignment.id
            ? {
                ...a,
                extraPermissions: Array.from(new Set(extraPermissions)),
                assignedClasses: Array.from(new Set(assignedClasses)),
                assignedSubjects: Array.from(new Set(assignedSubjects)),
                linkedStudentIds: Array.from(new Set(linkedStudentIds)),
                updatedBy: actorName
              }
            : a
        );
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              accessAssignments: updatedAssignments,
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                'PERMISSIONS_DELEGATED',
                'ICT_SYSTEMS',
                `Updated delegated permissions (${extraPermissions.length}) for ${assignment.fullName} without altering primaryRole (${assignment.primaryRole})`
              )
            }
          }
        };
      });
      setBannerMessage(`Updated permissions for ${assignment.fullName}`);
    },
    [requireCap]
  );

  const createAccessAssignment = useCallback(
    (
      schoolId: string,
      fullName: string,
      email: string,
      primaryRole: PortalRole,
      assignedClasses: string[],
      assignedSubjects: string[],
      linkedStudentIds: string[],
      extraPermissions: string[]
    ) => {
      if (!requireCap(RoleCapability.MANAGE_USER_PERMISSIONS, 'provision user portal access')) return;
      if (primaryRole === PortalRole.SUPER_ADMIN) {
        setBannerMessage('Cannot assign SUPER_ADMIN at school level.');
        return;
      }
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const newAssign: UserAccessAssignment = {
          id: uidGen('assign'),
          schoolId,
          ownerId,
          fullName: fullName.trim(),
          email: email.trim(),
          primaryRole,
          assignedBranchIds: ['branch_main'],
          assignedClasses,
          assignedSubjects,
          linkedStudentIds,
          extraPermissions,
          active: true,
          updatedBy: actorName
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              accessAssignments: [newAssign, ...tenant.accessAssignments],
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                'USER_ACCESS_PROVISIONED',
                'ICT_SYSTEMS',
                `Provisioned ${PORTAL_ROLE_META[primaryRole].shortTitle} access for ${fullName.trim()}`
              )
            }
          }
        };
      });
      setBannerMessage(`Provisioned portal access for ${fullName.trim()}`);
    },
    [requireCap]
  );

  const addStudent = useCallback(
    (
      school: School,
      branchId: string,
      admissionNo: string,
      fullName: string,
      className: string,
      parentName: string
    ) => {
      if (!requireCap(RoleCapability.ENROLL_STUDENT, 'enroll students')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[school.id];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const student: Student = {
          id: uidGen('stu'),
          schoolId: school.id,
          branchId,
          ownerId,
          admissionNo: admissionNo.trim(),
          fullName: fullName.trim(),
          className: className.trim() || 'Grade 10A',
          stream: 'General',
          parentName: parentName.trim() || 'Guardian',
          parentContact: null,
          attendanceRate: 100,
          averageScore: 85,
          feeBalanceUsd: 1200,
          riskLevel: RiskLevel.LOW,
          welfareStatus: 'Cleared'
        };
        return {
          ...prev,
          schools: prev.schools.map((s) =>
            s.id === school.id ? { ...s, studentCount: s.studentCount + 1 } : s
          ),
          tenants: {
            ...prev.tenants,
            [school.id]: {
              ...tenant,
              students: [student, ...tenant.students],
              auditLogs: appendAudit(
                tenant,
                school.id,
                ownerId,
                actorName,
                actorRole,
                'STUDENT_ADMITTED',
                'ADMISSIONS',
                `Enrolled ${student.fullName} (${student.admissionNo})`
              )
            }
          }
        };
      });
      setBannerMessage(`Enrolled ${fullName.trim()}`);
    },
    [requireCap]
  );

  const updateStudent = useCallback(
    (
      schoolId: string,
      student: Student,
      patch: {
        fullName: string;
        className: string;
        stream: string;
        parentName: string;
        parentContact: string;
      }
    ) => {
      if (!requireCap(RoleCapability.ENROLL_STUDENT, 'amend learner records')) return;
      const nextName = patch.fullName.trim();
      if (!nextName) {
        setBannerMessage('Learner name is required.');
        return;
      }
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const updated: Student = {
          ...student,
          fullName: nextName,
          className: patch.className.trim() || student.className,
          stream: patch.stream.trim() || student.stream,
          parentName: patch.parentName.trim() || student.parentName,
          parentContact: patch.parentContact.trim() || null
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              students: tenant.students.map((s) => (s.id === student.id ? updated : s)),
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                'STUDENT_RECORD_UPDATED',
                'ADMISSIONS',
                `Amended learner record ${updated.fullName} (${updated.admissionNo})`
              )
            }
          }
        };
      });
      setBannerMessage(`Updated ${nextName}`);
    },
    [requireCap]
  );

  const removeStudent = useCallback(
    (schoolId: string, student: Student) => {
      if (!requireCap(RoleCapability.ENROLL_STUDENT, 'remove learner records')) return;
      let blocked = false;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        if (tenant.reportCards.some((rc) => rc.studentId === student.id)) {
          blocked = true;
          return prev;
        }
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        return {
          ...prev,
          schools: prev.schools.map((s) =>
            s.id === schoolId ? { ...s, studentCount: Math.max(0, s.studentCount - 1) } : s
          ),
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              students: tenant.students.filter((s) => s.id !== student.id),
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                'STUDENT_RECORD_REMOVED',
                'ADMISSIONS',
                `Removed learner ${student.fullName} (${student.admissionNo})`
              )
            }
          }
        };
      });
      if (blocked) {
        setBannerMessage(
          `${student.fullName} has report cards on file. Clear those before removing the learner.`
        );
        return;
      }
      setFocusStudentId((prev) => (prev === student.id ? null : prev));
      setBannerMessage(`Removed ${student.fullName}`);
    },
    [requireCap]
  );

  const addStaffMember = useCallback(
    (
      school: School,
      branchId: string,
      input: {
        fullName: string;
        roleTitle: string;
        department: string;
        subjectsSummary: string;
        workloadHours: number;
        leaveBalanceDays: number;
      }
    ) => {
      if (!requireCap(RoleCapability.MANAGE_STAFF, 'manage staff records')) return;
      const name = input.fullName.trim();
      if (!name) {
        setBannerMessage('Staff name is required.');
        return;
      }
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[school.id];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const member: StaffMember = {
          id: uidGen('stf'),
          schoolId: school.id,
          branchId,
          ownerId,
          staffNo: `STF-${String(tenant.staff.length + 1).padStart(3, '0')}`,
          fullName: name,
          roleTitle: input.roleTitle.trim() || 'Faculty',
          department: input.department.trim() || 'Academics',
          subjectsSummary: input.subjectsSummary.trim() || null,
          leaveBalanceDays: Math.max(0, Math.round(input.leaveBalanceDays)),
          leaveUsedDays: 0,
          onLeave: false,
          workloadHours: Math.max(0, Math.round(input.workloadHours))
        };
        return {
          ...prev,
          schools: prev.schools.map((s) =>
            s.id === school.id ? { ...s, staffCount: s.staffCount + 1 } : s
          ),
          tenants: {
            ...prev.tenants,
            [school.id]: {
              ...tenant,
              staff: [member, ...tenant.staff],
              auditLogs: appendAudit(
                tenant,
                school.id,
                ownerId,
                actorName,
                actorRole,
                'STAFF_ADDED',
                'HR_STAFF',
                `Added ${member.fullName} as ${member.roleTitle}`
              )
            }
          }
        };
      });
      setBannerMessage(`Added ${name}`);
    },
    [requireCap]
  );

  const updateStaffMember = useCallback(
    (
      schoolId: string,
      staff: StaffMember,
      patch: {
        fullName: string;
        roleTitle: string;
        department: string;
        subjectsSummary: string;
        workloadHours: number;
        leaveBalanceDays: number;
      }
    ) => {
      if (!requireCap(RoleCapability.MANAGE_STAFF, 'manage staff records')) return;
      const nextName = patch.fullName.trim();
      if (!nextName) {
        setBannerMessage('Staff name is required.');
        return;
      }
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const updated: StaffMember = {
          ...staff,
          fullName: nextName,
          roleTitle: patch.roleTitle.trim() || staff.roleTitle,
          department: patch.department.trim() || staff.department,
          subjectsSummary: patch.subjectsSummary.trim() || null,
          workloadHours: Math.max(0, Math.round(patch.workloadHours)),
          leaveBalanceDays: Math.max(0, Math.round(patch.leaveBalanceDays))
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              staff: tenant.staff.map((sf) => (sf.id === staff.id ? updated : sf)),
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                'STAFF_UPDATED',
                'HR_STAFF',
                `Updated ${updated.fullName} (${updated.roleTitle})`
              )
            }
          }
        };
      });
      setBannerMessage(`Updated ${nextName}`);
    },
    [requireCap]
  );

  const removeStaffMember = useCallback(
    (schoolId: string, staff: StaffMember) => {
      if (!requireCap(RoleCapability.MANAGE_STAFF, 'manage staff records')) return;
      let blocked = false;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const openLeave = tenant.leaveRequests.some(
          (lv) => lv.staffId === staff.id && lv.status === LeaveStatus.PENDING
        );
        if (openLeave) {
          blocked = true;
          return prev;
        }
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        return {
          ...prev,
          schools: prev.schools.map((s) =>
            s.id === schoolId ? { ...s, staffCount: Math.max(0, s.staffCount - 1) } : s
          ),
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              staff: tenant.staff.filter((sf) => sf.id !== staff.id),
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                'STAFF_REMOVED',
                'HR_STAFF',
                `Removed ${staff.fullName} (${staff.staffNo})`
              )
            }
          }
        };
      });
      if (blocked) {
        setBannerMessage(
          `${staff.fullName} has a leave request awaiting review. Resolve it before removing the record.`
        );
        return;
      }
      setBannerMessage(`Removed ${staff.fullName}`);
    },
    [requireCap]
  );

  const recordAttendance = useCallback(
    (school: School, branchId: string, student: Student, status: string) => {
      if (!requireCap(RoleCapability.MARK_ATTENDANCE, 'mark attendance')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[school.id];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const rec = {
          id: uidGen('att'),
          schoolId: school.id,
          branchId,
          academicYear: school.activeAcademicYear,
          term: school.activeTerm,
          ownerId,
          targetType: 'STUDENT',
          targetName: student.fullName,
          className: student.className,
          dateIso: new Date().toISOString().slice(0, 10),
          status
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [school.id]: {
              ...tenant,
              attendance: [rec, ...tenant.attendance],
              auditLogs: appendAudit(
                tenant,
                school.id,
                ownerId,
                actorName,
                actorRole,
                'ATTENDANCE_MARKED',
                'ATTENDANCE',
                `Marked ${student.fullName} as ${status}`
              )
            }
          }
        };
      });
      setBannerMessage(`${student.fullName} marked ${status}`);
    },
    [requireCap]
  );

  const addAcademicRecord = useCallback(
    (
      school: School,
      branchId: string,
      recordType: string,
      title: string,
      className: string,
      subjectName: string,
      teacherName: string,
      scheduleSlot: string,
      score: number
    ) => {
      if (!requireCap(RoleCapability.EDIT_ACADEMICS, 'add academic entries')) return;
      const clampedScore = Math.max(0, Math.min(100, score));
      const grade =
        clampedScore >= 90
          ? 'A'
          : clampedScore >= 80
            ? 'B'
            : clampedScore >= 70
              ? 'C'
              : clampedScore >= 60
                ? 'D'
                : 'F';
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[school.id];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const rec = {
          id: uidGen('acad'),
          schoolId: school.id,
          branchId,
          academicYear: school.activeAcademicYear,
          term: school.activeTerm,
          ownerId,
          recordType,
          title: title.trim(),
          className: className.trim() || 'Grade 10A',
          subjectName: subjectName.trim() || 'General',
          teacherName: teacherName.trim() || 'Faculty',
          scheduleSlot: scheduleSlot.trim() || 'Term 1',
          score: clampedScore,
          maxScore: 100,
          grade
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [school.id]: {
              ...tenant,
              academics: [rec, ...tenant.academics]
            }
          }
        };
      });
      setBannerMessage(`Saved ${recordType}: ${title.trim()}`);
    },
    [requireCap]
  );

  const createReportTemplate = useCallback(
    (
      school: School,
      name: string,
      classLevel: string,
      sectionsCsv: string,
      gradingScale: string
    ) => {
      if (!requireCap(RoleCapability.MANAGE_REPORT_TEMPLATES, 'create report templates')) return;
      const sections = sectionsCsv
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      const resolvedSections =
        sections.length > 0
          ? sections
          : ['Core Subjects', 'Attendance', 'Teacher Remarks', 'Principal Sign-Off'];
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[school.id];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const tpl: ReportTemplate = {
          id: uidGen('tpl'),
          schoolId: school.id,
          ownerId,
          name: name.trim(),
          classLevel: classLevel.trim() || 'All Classes',
          sections: resolvedSections,
          gradingScale: gradingScale.trim() || 'A-F Percentage',
          active: true
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [school.id]: {
              ...tenant,
              reportTemplates: [tpl, ...tenant.reportTemplates],
              auditLogs: appendAudit(
                tenant,
                school.id,
                ownerId,
                actorName,
                actorRole,
                'TEMPLATE_CREATED',
                'REPORT_CARDS',
                `Uploaded template ${tpl.name}`
              )
            }
          }
        };
      });
      setBannerMessage(`Created report template: ${name.trim()}`);
    },
    [requireCap]
  );

  const saveOrSubmitReportCard = useCallback(
    (
      school: School,
      branchId: string,
      student: Student,
      template: ReportTemplate,
      gradesSummary: string,
      averageScore: number,
      teacherRemarks: string,
      aiSummary: string | null,
      submitNow: boolean
    ) => {
      if (!requireCap(RoleCapability.FILL_SUBMIT_REPORT_CARDS, 'fill or submit report cards')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[school.id];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const report: ReportCard = {
          id: uidGen('rc'),
          schoolId: school.id,
          branchId,
          academicYear: school.activeAcademicYear,
          term: school.activeTerm,
          ownerId,
          studentId: student.id,
          studentName: student.fullName,
          className: student.className,
          templateId: template.id,
          templateName: template.name,
          subjectGradesSummary: gradesSummary.trim(),
          averageScore: Math.max(0, Math.min(100, averageScore)),
          teacherRemarks: teacherRemarks.trim(),
          aiSummary: aiSummary?.trim() || null,
          headteacherRemarks: null,
          status: submitNow ? ReportStatus.SUBMITTED : ReportStatus.DRAFT,
          version: 1,
          submittedBy: actorName,
          reviewedBy: null
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [school.id]: {
              ...tenant,
              reportCards: [report, ...tenant.reportCards],
              auditLogs: appendAudit(
                tenant,
                school.id,
                ownerId,
                actorName,
                actorRole,
                submitNow ? 'REPORT_SUBMITTED' : 'REPORT_SAVED',
                'REPORT_CARDS',
                `${submitNow ? 'Submitted' : 'Saved draft'} report card for ${student.fullName} (v1)`,
                Boolean(aiSummary)
              )
            }
          }
        };
      });
      setBannerMessage(submitNow ? 'Report submitted for Headteacher review' : 'Draft saved');
    },
    [requireCap]
  );

  const reviewReportCard = useCallback(
    (schoolId: string, report: ReportCard, nextStatus: ReportStatus, headteacherRemarks: string) => {
      const required =
        nextStatus === ReportStatus.SUBMITTED
          ? RoleCapability.FILL_SUBMIT_REPORT_CARDS
          : RoleCapability.APPROVE_PUBLISH_REPORT_CARDS;
      if (!requireCap(required, `move report cards to ${nextStatus}`)) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const nextVersion = report.version + 1;
        const updatedReports = tenant.reportCards.map((r) =>
          r.id === report.id
            ? {
                ...r,
                status: nextStatus,
                version: nextVersion,
                reviewedBy: actorName,
                headteacherRemarks: headteacherRemarks.trim() || r.headteacherRemarks
              }
            : r
        );
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              reportCards: updatedReports,
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                `REPORT_${nextStatus}`,
                'REPORT_CARDS',
                `Moved ${report.studentName} report card to ${nextStatus} (v${nextVersion})`
              )
            }
          }
        };
      });
      setBannerMessage(`Report card marked ${nextStatus}`);
    },
    [requireCap]
  );

  const submitLeaveRequest = useCallback(
    (
      school: School,
      branchId: string,
      staff: StaffMember,
      leaveType: string,
      startDate: string,
      endDate: string,
      daysCount: number,
      reason: string,
      aiConflictNote: string | null
    ) => {
      if (!requireCap(RoleCapability.APPLY_LEAVE, 'submit leave requests')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[school.id];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const lv: LeaveRequest = {
          id: uidGen('lv'),
          schoolId: school.id,
          branchId,
          ownerId,
          staffId: staff.id,
          staffName: staff.fullName,
          roleTitle: staff.roleTitle,
          leaveType,
          startDate: startDate.trim(),
          endDate: endDate.trim(),
          daysCount: Math.max(1, daysCount),
          reason: reason.trim(),
          attachmentNote: null,
          status: LeaveStatus.PENDING,
          reviewedBy: null,
          aiConflictNote: aiConflictNote?.trim() || null
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [school.id]: {
              ...tenant,
              leaveRequests: [lv, ...tenant.leaveRequests],
              auditLogs: appendAudit(
                tenant,
                school.id,
                ownerId,
                actorName,
                actorRole,
                'LEAVE_APPLIED',
                'LEAVE_MANAGEMENT',
                `${staff.fullName} requested ${lv.daysCount} day(s) ${leaveType} leave`,
                Boolean(aiConflictNote)
              )
            }
          }
        };
      });
      setBannerMessage(`Leave request submitted for ${staff.fullName}`);
    },
    [requireCap]
  );

  const reviewLeaveRequest = useCallback(
    (
      schoolId: string,
      leave: LeaveRequest,
      staffMember: StaffMember | undefined,
      approve: boolean
    ) => {
      if (!requireCap(RoleCapability.APPROVE_LEAVE, 'approve or reject leave requests')) return;
      const nextStatus = approve ? LeaveStatus.APPROVED : LeaveStatus.REJECTED;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const nextLeaves = tenant.leaveRequests.map((l) =>
          l.id === leave.id ? { ...l, status: nextStatus, reviewedBy: actorName } : l
        );
        const nextStaff =
          approve && staffMember
            ? tenant.staff.map((st) =>
                st.id === staffMember.id
                  ? {
                      ...st,
                      onLeave: true,
                      leaveBalanceDays: Math.max(0, st.leaveBalanceDays - leave.daysCount),
                      leaveUsedDays: st.leaveUsedDays + leave.daysCount
                    }
                  : st
              )
            : tenant.staff;
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              leaveRequests: nextLeaves,
              staff: nextStaff,
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                `LEAVE_${nextStatus}`,
                'LEAVE_MANAGEMENT',
                `${nextStatus} ${leave.daysCount} day(s) leave for ${leave.staffName}`
              )
            }
          }
        };
      });
      setBannerMessage(
        approve
          ? `Approved leave: ${leave.staffName} is now marked On Leave`
          : `Rejected leave request for ${leave.staffName}`
      );
    },
    [requireCap]
  );

  const recordFinancePayment = useCallback(
    (schoolId: string, entry: FinanceEntry, amountUsd: number) => {
      if (!requireCap(RoleCapability.RECORD_FINANCE_PAYMENT, 'record fee payments')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const newPaid = Math.min(entry.amountUsd, entry.paidUsd + amountUsd);
        const nextStatus = newPaid >= entry.amountUsd ? 'PAID' : 'PARTIAL';
        const nextFinance = tenant.finance.map((f) =>
          f.id === entry.id ? { ...f, paidUsd: newPaid, status: nextStatus } : f
        );
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              finance: nextFinance,
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                'PAYMENT_RECORDED',
                'FINANCE',
                `Recorded USD ${amountUsd.toFixed(2)} for ${entry.referenceNo}`
              )
            }
          }
        };
      });
      setBannerMessage(`Payment recorded for ${entry.referenceNo}`);
    },
    [requireCap]
  );

  const sendCommunication = useCallback(
    (
      school: School,
      branchId: string,
      channel: string,
      audience: string,
      title: string,
      body: string,
      aiDrafted: boolean
    ) => {
      if (!requireCap(RoleCapability.COMPOSE_COMMUNICATIONS, 'send school notices')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[school.id];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const msg = {
          id: uidGen('comm'),
          schoolId: school.id,
          branchId,
          ownerId,
          channel,
          audience: audience.trim() || 'All Users',
          title: title.trim(),
          body: body.trim(),
          senderName: actorName,
          senderRole: actorRole,
          aiDrafted
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [school.id]: {
              ...tenant,
              communications: [msg, ...tenant.communications],
              auditLogs: appendAudit(
                tenant,
                school.id,
                ownerId,
                actorName,
                actorRole,
                'MESSAGE_DISPATCHED',
                'COMMUNICATION',
                `Sent ${channel} notice: ${title.trim()}`,
                aiDrafted
              )
            }
          }
        };
      });
      setBannerMessage(`Dispatched ${channel} message`);
    },
    [requireCap]
  );

  const addOperationRecord = useCallback(
    (
      school: School,
      branchId: string,
      moduleType: string,
      title: string,
      subtitle: string,
      partyName: string,
      status: string,
      metricLabel: string,
      notes: string
    ) => {
      const w = workspaceRef.current;
      if (!w) return;
      const sectionName = (() => {
        switch (moduleType) {
          case 'LIBRARY_BOOK':
          case 'LIBRARY_BORROW':
            return 'Library';
          case 'VEHICLE':
          case 'ROUTE':
          case 'TRIP':
            return 'Transport';
          case 'WELFARE':
            return 'Welfare';
          case 'COUNSELLING':
            return 'Counselling';
          case 'DISCIPLINE':
            return 'Discipline';
          case 'PROCUREMENT':
            return 'Procurement';
          case 'INVENTORY':
          case 'ASSET':
          case 'STOCK_MOVEMENT':
            return 'Stores';
          case 'HOSTEL':
            return 'Hostel';
          case 'FRONT_DESK':
            return 'Front Desk';
          case 'ADMISSION':
            return 'Admissions';
          case 'ICT_TICKET':
            return 'ICT Systems';
          default:
            return 'Events';
        }
      })();
      const allowedSections = RolePolicy.operationSections(
        w.activeRole,
        school,
        w.activeExtraCapabilities
      );
      const hasEditCap =
        RolePolicy.has(w.activeRole, RoleCapability.EDIT_OPERATIONS, w.activeExtraCapabilities) ||
        RolePolicy.has(w.activeRole, RoleCapability.MANAGE_PROCUREMENT, w.activeExtraCapabilities) ||
        RolePolicy.has(w.activeRole, RoleCapability.MANAGE_STORES, w.activeExtraCapabilities) ||
        RolePolicy.has(w.activeRole, RoleCapability.MANAGE_COUNSELLING, w.activeExtraCapabilities) ||
        RolePolicy.has(w.activeRole, RoleCapability.MANAGE_FRONT_DESK, w.activeExtraCapabilities) ||
        RolePolicy.has(w.activeRole, RoleCapability.MANAGE_ICT_SYSTEMS, w.activeExtraCapabilities);

      if (!allowedSections.includes(sectionName) || !hasEditCap) {
        setBannerMessage(
          `Access denied: ${PORTAL_ROLE_META[w.activeRole].shortTitle} cannot add records in ${sectionName}.`
        );
        return;
      }

      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[school.id];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        const rec = {
          id: uidGen('op'),
          schoolId: school.id,
          branchId,
          ownerId,
          moduleType,
          title: title.trim(),
          subtitle: subtitle.trim(),
          partyName: partyName.trim(),
          status: status.trim() || 'ACTIVE',
          metricLabel: metricLabel.trim(),
          notes: notes.trim()
        };
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [school.id]: {
              ...tenant,
              operations: [rec, ...tenant.operations],
              auditLogs: appendAudit(
                tenant,
                school.id,
                ownerId,
                actorName,
                actorRole,
                'OPERATION_LOGGED',
                moduleType,
                `Added ${moduleType} record: ${title.trim()}`
              )
            }
          }
        };
      });
      setBannerMessage(`Saved ${moduleType} record`);
    },
    []
  );

  const updateAlertStatus = useCallback(
    (schoolId: string, alert: EarlyWarningAlert, status: string) => {
      if (!requireCap(RoleCapability.MANAGE_ALERTS, 'update early warning alerts')) return;
      setStore((prev) => {
        if (!prev) return prev;
        const tenant = prev.tenants[schoolId];
        if (!tenant) return prev;
        const ownerId = prev.userProfile.userId;
        const actorName = prev.userProfile.displayName;
        const actorRole = resolveEffectiveRole(prev.userProfile);
        return {
          ...prev,
          tenants: {
            ...prev.tenants,
            [schoolId]: {
              ...tenant,
              alerts: tenant.alerts.map((a) => (a.id === alert.id ? { ...a, status } : a)),
              auditLogs: appendAudit(
                tenant,
                schoolId,
                ownerId,
                actorName,
                actorRole,
                'ALERT_UPDATED',
                'ANALYTICS',
                `Updated alert for ${alert.targetName} -> ${status}`
              )
            }
          }
        };
      });
      setBannerMessage(`Alert updated to ${status}`);
    },
    [requireCap]
  );

  const logAiAuditLocal = useCallback((schoolId: string, actionType: string, summary: string) => {
    setStore((prev) => {
      if (!prev) return prev;
      const tenant = prev.tenants[schoolId];
      if (!tenant) return prev;
      const ownerId = prev.userProfile.userId;
      const actorName = prev.userProfile.displayName;
      const actorRole = resolveEffectiveRole(prev.userProfile);
      return {
        ...prev,
        tenants: {
          ...prev.tenants,
          [schoolId]: {
            ...tenant,
            auditLogs: appendAudit(
              tenant,
              schoolId,
              ownerId,
              actorName,
              actorRole,
              actionType,
              'AI_TOOLS',
              summary,
              true
            )
          }
        }
      };
    });
  }, []);

  const askAiCopilot = useCallback(
    async (prompt: string) => {
      const w = workspaceRef.current;
      if (!w) return;
      if (w.activeSchool && !w.activeSchool.enabledModules.includes(GsmsModule.AI_TOOLS)) {
        setAiState({
          status: 'error',
          message: `AI Tools module is currently disabled for ${w.activeSchool.name}.`
        });
        return;
      }
      const dataSummary = buildRoleScopedSummary(w, focusStudentId);
      setAiState({ status: 'loading' });
      try {
        const finalText = await streamAssistantResponse(
          w.activeRole,
          w.activeSchool,
          dataSummary,
          prompt,
          (partial) => {
            setAiState({ status: 'streaming', text: partial });
          }
        );
        setAiState({ status: 'success', text: finalText });
        if (w.activeSchool) {
          logAiAuditLocal(
            w.activeSchool.id,
            'AI_COPILOT_QUERY',
            `AI prompt in ${PORTAL_ROLE_META[w.activeRole].shortTitle}: ${prompt.slice(0, 120)}`
          );
        }
      } catch (err) {
        setAiState({
          status: 'error',
          message: err instanceof Error ? err.message : 'AI request failed.'
        });
      }
    },
    [focusStudentId, logAiAuditLocal]
  );

  const generateQuickAiDraft = useCallback(
    async (taskPrompt: string, actionType: string): Promise<string | null> => {
      const w = workspaceRef.current;
      if (!w) return null;
      if (w.activeSchool && !w.activeSchool.enabledModules.includes(GsmsModule.AI_TOOLS)) {
        setBannerMessage('Enable AI Tools in Module Registry to run AI generation.');
        return null;
      }
      setBannerMessage('Gemini AI generating...');
      try {
        const text = await generateWorkflowText(
          w.activeRole,
          w.activeSchool,
          buildRoleScopedSummary(w, focusStudentId),
          taskPrompt
        );
        setBannerMessage('AI draft ready');
        if (w.activeSchool) {
          logAiAuditLocal(
            w.activeSchool.id,
            actionType,
            `Generated AI workflow content (${actionType})`
          );
        }
        return text;
      } catch (err) {
        setBannerMessage(
          `AI Error: ${err instanceof Error ? err.message : 'Generation failed'}`
        );
        return null;
      }
    },
    [focusStudentId, logAiAuditLocal]
  );

  // Supabase governance helpers
  const testSupabase = useCallback(async () => {
    setSupabaseState((p) => ({ ...p, isBusy: true, lastError: null }));
    try {
      const res = await testSupabaseConnection();
      setSupabaseState((p) => ({
        ...p,
        isBusy: false,
        connectionStatus: 'Connected',
        lastLatencyMs: res.latencyMs,
        lastError: null
      }));
      setBannerMessage(res.message);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Supabase ping failed.';
      setSupabaseState((p) => ({ ...p, isBusy: false, connectionStatus: 'Error', lastError: msg }));
      setBannerMessage(msg);
    }
  }, []);

  const refreshSupabaseCounts = useCallback(async () => {
    const schoolId = workspaceRef.current?.activeSchool?.id;
    if (!schoolId) return;
    setSupabaseState((p) => ({ ...p, isBusy: true, lastError: null }));
    try {
      const counts = await pullSupabaseCounts(schoolId);
      const total = Object.values(counts).reduce((a, b) => a + b, 0);
      setSupabaseState((p) => ({
        ...p,
        isBusy: false,
        connectionStatus: 'Connected',
        remoteCounts: counts,
        lastSyncSummary: `Verified ${total} PostgreSQL rows across ${Object.keys(counts).length} tables`
      }));
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to refresh Supabase counts.';
      setSupabaseState((p) => ({ ...p, isBusy: false, lastError: msg }));
    }
  }, []);

  const pushToSupabase = useCallback(async () => {
    const w = workspaceRef.current;
    if (!w) return;
    setSupabaseState((p) => ({ ...p, isBusy: true, lastError: null }));
    try {
      const res = await pushWorkspaceToSupabase(w);
      setSupabaseState((p) => ({
        ...p,
        isBusy: false,
        connectionStatus: 'Connected',
        lastLatencyMs: res.latencyMs,
        lastSyncSummary: res.summary,
        remoteCounts: res.counts,
        lastError: null
      }));
      setBannerMessage(res.summary);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Supabase sync failed.';
      setSupabaseState((p) => ({ ...p, isBusy: false, lastError: msg }));
      setBannerMessage(msg);
    }
  }, []);

  const toggleSupabaseAutoMirror = useCallback((enabled: boolean) => {
    setSupabaseState((p) => ({ ...p, autoMirrorEnabled: enabled, lastError: null }));
    setBannerMessage(enabled ? 'Supabase Auto-Mirror enabled' : 'Supabase Auto-Mirror paused');
  }, []);

  const signInSupabaseInsideGovernance = useCallback(async (email: string, password: string) => {
    setSupabaseState((p) => ({ ...p, isBusy: true, lastError: null }));
    try {
      const res = await signInSupabase(email, password);
      setSupabaseState((p) => ({
        ...p,
        isBusy: false,
        isAuthenticated: Boolean(res.session),
        authEmail: res.session?.email ?? null,
        connectionStatus: res.session ? 'Authenticated' : p.connectionStatus,
        lastSyncSummary: res.message
      }));
      setBannerMessage(res.message);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Supabase sign-in failed.';
      setSupabaseState((p) => ({ ...p, isBusy: false, lastError: msg }));
      setBannerMessage(msg);
    }
  }, []);

  const signUpSupabaseInsideGovernance = useCallback(async (email: string, password: string) => {
    setSupabaseState((p) => ({ ...p, isBusy: true, lastError: null }));
    try {
      const res = await signUpSupabase(email, password);
      setSupabaseState((p) => ({
        ...p,
        isBusy: false,
        isAuthenticated: Boolean(res.session),
        authEmail: res.session?.email ?? null,
        connectionStatus: res.session ? 'Authenticated' : p.connectionStatus,
        lastSyncSummary: res.message
      }));
      setBannerMessage(res.message);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Supabase registration failed.';
      setSupabaseState((p) => ({ ...p, isBusy: false, lastError: msg }));
      setBannerMessage(msg);
    }
  }, []);

  const signOutSupabaseInsideGovernance = useCallback(async () => {
    await signOutSupabase();
    setSupabaseState((p) => ({
      ...p,
      isAuthenticated: false,
      authEmail: null,
      autoMirrorEnabled: false,
      remoteCounts: {},
      lastError: null
    }));
    setBannerMessage('Signed out of Supabase Cloud sync');
  }, []);

  // Trigger optional auto-mirroring when store updates
  useEffect(() => {
    if (store && workspace) {
      autoMirrorIfEnabled(workspace);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [store]);

  // Keep unused enum helper exports tree-shaken cleanly
  void ALL_PORTAL_ROLES;
  void parseEnumOrDefault;

  return (
    <GsmsContext.Provider
      value={{
        session,
        workspace,
        aiState,
        supabaseState,
        searchQuery,
        focusStudentId,
        bannerMessage,
        setSearchQuery,
        clearBanner,
        showBanner,
        enterWorkspaceLocal,
        signInWithSupabaseEmail,
        signUpWithSupabaseEmail,
        resetSupabasePassword,
        signOutAll,
        selectFocusStudent,
        switchRole,
        switchSchool,
        switchBranch,
        toggleLowDataMode,
        toggleModule,
        createSchool,
        createBranch,
        updateAcademicPeriod,
        delegateUserPermissions,
        createAccessAssignment,
        addStudent,
        updateStudent,
        removeStudent,
        addStaffMember,
        updateStaffMember,
        removeStaffMember,
        recordAttendance,
        addAcademicRecord,
        createReportTemplate,
        saveOrSubmitReportCard,
        reviewReportCard,
        submitLeaveRequest,
        reviewLeaveRequest,
        recordFinancePayment,
        sendCommunication,
        addOperationRecord,
        updateAlertStatus,
        askAiCopilot,
        generateQuickAiDraft,
        testSupabase,
        pushToSupabase,
        refreshSupabaseCounts,
        toggleSupabaseAutoMirror,
        signInSupabaseInsideGovernance,
        signUpSupabaseInsideGovernance,
        signOutSupabaseInsideGovernance
      }}
    >
      {children}
    </GsmsContext.Provider>
  );
};

export function useGsms(): GsmsContextValue {
  const ctx = useContext(GsmsContext);
  if (!ctx) throw new Error('useGsms must be used inside GsmsProvider');
  return ctx;
}
