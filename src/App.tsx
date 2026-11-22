import React, { useEffect, useState } from 'react';
import {
  BarChart3,
  Bell,
  BookOpen,
  Briefcase,
  LayoutDashboard,
  Lock,
  Menu,
  Search,
  ShieldCheck,
  Sparkles,
  Users,
  X
} from 'lucide-react';
import { RolePolicy } from './policy/RolePolicy.ts';
import { AcademicsAndReportsScreen } from './screens/AcademicsAndReportsScreen.tsx';
import { AiAndGovernanceScreen } from './screens/AiAndGovernanceScreen.tsx';
import { AnalyticsAndCommsScreen } from './screens/AnalyticsAndCommsScreen.tsx';
import { AuthScreen } from './screens/AuthScreen.tsx';
import { OperationsAndFinanceScreen } from './screens/OperationsAndFinanceScreen.tsx';
import { PeopleAndLeaveScreen } from './screens/PeopleAndLeaveScreen.tsx';
import { PortalDashboards } from './screens/PortalDashboards.tsx';
import { GsmsProvider, useGsms } from './state/GsmsContext.tsx';
import {
  ALL_PORTAL_ROLES,
  GsmsTab,
  PORTAL_ROLE_META,
  PortalRole,
  resolvePrimaryRole
} from './types/gsms.ts';

function tabIcon(tab: GsmsTab) {
  switch (tab) {
    case GsmsTab.DASHBOARD:
      return <LayoutDashboard className="w-4 h-4 shrink-0" />;
    case GsmsTab.ACADEMICS:
      return <BookOpen className="w-4 h-4 shrink-0" />;
    case GsmsTab.PEOPLE:
      return <Users className="w-4 h-4 shrink-0" />;
    case GsmsTab.OPERATIONS:
      return <Briefcase className="w-4 h-4 shrink-0" />;
    case GsmsTab.ANALYTICS:
      return <BarChart3 className="w-4 h-4 shrink-0" />;
    case GsmsTab.AI_GOVERNANCE:
      return <Sparkles className="w-4 h-4 shrink-0" />;
  }
}

const GsmsShell: React.FC = () => {
  const {
    session,
    workspace,
    searchQuery,
    setSearchQuery,
    bannerMessage,
    clearBanner,
    switchRole
  } = useGsms();

  const [activeTab, setActiveTab] = useState<GsmsTab>(GsmsTab.DASHBOARD);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  useEffect(() => {
    if (!bannerMessage) return;
    const t = setTimeout(() => clearBanner(), 4000);
    return () => clearTimeout(t);
  }, [bannerMessage, clearBanner]);

  const activeRole = workspace?.activeRole;
  const activeSchool = workspace?.activeSchool ?? null;
  const extraCaps = workspace?.activeExtraCapabilities ?? [];

  const visibleTabs =
    activeRole && activeSchool
      ? RolePolicy.visibleTabs(activeRole, activeSchool, extraCaps)
      : [GsmsTab.DASHBOARD, GsmsTab.AI_GOVERNANCE];

  useEffect(() => {
    if (!visibleTabs.includes(activeTab)) {
      setActiveTab(GsmsTab.DASHBOARD);
    }
  }, [visibleTabs, activeTab]);

  if (!session || !workspace || !activeRole) {
    return <AuthScreen />;
  }

  const primaryRole = resolvePrimaryRole(workspace.userProfile);
  const isSuperAdmin = primaryRole === PortalRole.SUPER_ADMIN;
  const roleMeta = PORTAL_ROLE_META[activeRole];
  const activeBranch = workspace.branches.find((b) => b.id === workspace.activeBranchId);
  const openAlertCount = RolePolicy.filterAlertsForRole(activeRole, workspace.alerts).filter(
    (a) => a.status !== 'RESOLVED'
  ).length;

  return (
    <div className="min-h-screen bg-[#F5F7FB] text-[#0F172A] flex flex-col">
      {/* Top Bar */}
      <header className="sticky top-0 z-30 bg-[#0F2537] text-white border-b border-[#1B304A]">
        <div className="px-4 sm:px-6 h-16 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <button
              type="button"
              onClick={() => setDrawerOpen(true)}
              aria-label="Open role portals drawer"
              className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-colors cursor-pointer shrink-0"
            >
              <Menu className="w-5 h-5" />
            </button>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-sm sm:text-base font-extrabold tracking-tight truncate">
                  {activeSchool?.name || 'GSMS Platform'}
                </span>
                {activeBranch ? (
                  <span className="hidden sm:inline-block px-2 py-0.5 rounded bg-white/15 text-[10px] font-semibold whitespace-nowrap">
                    {activeBranch.name}
                  </span>
                ) : null}
              </div>
              <div className="text-[11px] text-[#D1FAE5] truncate">
                {roleMeta.shortTitle}
                {extraCaps.length > 0 ? ` (+${extraCaps.length})` : ''}
                {activeSchool
                  ? ` • ${activeSchool.activeAcademicYear} (${activeSchool.activeTerm})`
                  : ''}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {searchOpen ? (
              <div className="flex items-center bg-white/10 border border-white/20 rounded-xl px-2.5 py-1">
                <Search className="w-3.5 h-3.5 text-white/70 mr-1.5" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Filter tenant records..."
                  className="bg-transparent text-xs text-white placeholder:text-white/60 focus:outline-none w-36 sm:w-48"
                />
                <button
                  type="button"
                  onClick={() => {
                    setSearchQuery('');
                    setSearchOpen(false);
                  }}
                  aria-label="Clear search"
                  className="text-white/70 hover:text-white ml-1 cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setSearchOpen(true)}
                aria-label="Open search"
                className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-colors cursor-pointer"
              >
                <Search className="w-4 h-4" />
              </button>
            )}

            {visibleTabs.includes(GsmsTab.ANALYTICS) ? (
              <button
                type="button"
                onClick={() => setActiveTab(GsmsTab.ANALYTICS)}
                aria-label="Open alerts and notices"
                className="relative p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-colors cursor-pointer"
              >
                <Bell className="w-4 h-4" />
                {openAlertCount > 0 ? (
                  <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-[#DC2626] text-[10px] font-bold flex items-center justify-center font-mono-num">
                    {openAlertCount}
                  </span>
                ) : null}
              </button>
            ) : null}
          </div>
        </div>
      </header>

      {/* Main Layout */}
      <div className="flex-1 flex max-w-7xl w-full mx-auto">
        {/* Desktop Sidebar Navigation Rail */}
        <aside className="hidden md:flex flex-col w-56 shrink-0 border-r border-[#D0D9E4] bg-white p-3 gap-1.5">
          {visibleTabs.map((tab) => {
            const active = tab === activeTab;
            return (
              <button
                key={tab}
                type="button"
                onClick={() => setActiveTab(tab)}
                className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-colors whitespace-nowrap cursor-pointer ${
                  active
                    ? 'bg-[#0F2537] text-white'
                    : 'text-[#475569] hover:bg-[#F5F7FB] hover:text-[#0F172A]'
                }`}
              >
                {tabIcon(tab)}
                <span>{RolePolicy.tabDisplayLabel(activeRole, tab)}</span>
              </button>
            );
          })}
        </aside>

        {/* Active Screen Viewport */}
        <main className="flex-1 min-w-0 p-4 sm:p-6 pb-24 md:pb-8">
          {activeTab === GsmsTab.DASHBOARD ? (
            <PortalDashboards onNavigateTab={setActiveTab} />
          ) : null}
          {activeTab === GsmsTab.ACADEMICS ? <AcademicsAndReportsScreen /> : null}
          {activeTab === GsmsTab.PEOPLE ? <PeopleAndLeaveScreen /> : null}
          {activeTab === GsmsTab.OPERATIONS ? <OperationsAndFinanceScreen /> : null}
          {activeTab === GsmsTab.ANALYTICS ? <AnalyticsAndCommsScreen /> : null}
          {activeTab === GsmsTab.AI_GOVERNANCE ? <AiAndGovernanceScreen /> : null}
        </main>
      </div>

      {/* Mobile Bottom Navigation Bar */}
      <nav className="md:hidden fixed bottom-0 inset-x-0 z-30 bg-white border-t border-[#D0D9E4] px-2 py-1.5 flex items-center justify-around">
        {visibleTabs.map((tab) => {
          const active = tab === activeTab;
          return (
            <button
              key={tab}
              type="button"
              onClick={() => setActiveTab(tab)}
              className={`flex flex-col items-center gap-0.5 px-2.5 py-1 rounded-lg text-[10px] font-semibold whitespace-nowrap cursor-pointer ${
                active ? 'text-[#1D4ED8]' : 'text-[#475569]'
              }`}
            >
              {tabIcon(tab)}
              <span>{RolePolicy.tabDisplayLabel(activeRole, tab)}</span>
            </button>
          );
        })}
      </nav>

      {/* Collapsible Role Portals Drawer */}
      {drawerOpen ? (
        <div className="fixed inset-0 z-50 flex">
          <div
            className="fixed inset-0 bg-[#091826]/60 backdrop-blur-[2px]"
            onClick={() => setDrawerOpen(false)}
          />
          <div className="relative z-10 w-80 max-w-[85vw] bg-white h-full shadow-2xl flex flex-col border-r border-[#D0D9E4]">
            <div className="bg-[#0F2537] text-white p-4 flex items-center justify-between gap-2">
              <div className="min-w-0">
                <div className="text-sm font-extrabold">19 Role Portals</div>
                <div className="text-[11px] text-white/75 truncate">
                  {workspace.userProfile?.displayName} •{' '}
                  {isSuperAdmin ? 'SWITCHABLE' : 'LOCKED'}
                </div>
              </div>
              <button
                type="button"
                onClick={() => setDrawerOpen(false)}
                aria-label="Close portals drawer"
                className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {extraCaps.length > 0 ? (
              <div className="px-4 py-2.5 bg-[#EDE9FE] border-b border-[#7C3AED]/20 text-xs font-semibold text-[#5B21B6]">
                +{extraCaps.length} Delegated Permissions Active
              </div>
            ) : null}

            <div className="flex-1 overflow-y-auto p-3 space-y-1.5">
              {ALL_PORTAL_ROLES.map((role) => {
                const meta = PORTAL_ROLE_META[role];
                const isSelected = role === activeRole;
                const locked = !isSuperAdmin && !isSelected;
                return (
                  <button
                    key={role}
                    type="button"
                    onClick={() => {
                      switchRole(role);
                      setDrawerOpen(false);
                    }}
                    className={`w-full text-left rounded-xl px-3 py-2.5 border transition-colors flex items-start justify-between gap-2 cursor-pointer ${
                      isSelected
                        ? 'bg-[#0F2537] text-white border-[#0F2537]'
                        : locked
                          ? 'bg-[#F5F7FB] text-[#475569] border-[#E8EEF5] opacity-75'
                          : 'bg-white text-[#0F172A] border-[#D0D9E4] hover:bg-[#F5F7FB]'
                    }`}
                  >
                    <div className="min-w-0">
                      <div className="text-xs font-bold truncate">{meta.displayName}</div>
                      <div
                        className={`text-[11px] line-clamp-1 mt-0.5 ${
                          isSelected ? 'text-white/75' : 'text-[#475569]'
                        }`}
                      >
                        {meta.scopeDescription}
                      </div>
                    </div>
                    {isSelected ? (
                      <ShieldCheck className="w-4 h-4 text-[#34D399] shrink-0 mt-0.5" />
                    ) : locked ? (
                      <Lock className="w-3.5 h-3.5 text-[#475569] shrink-0 mt-0.5" />
                    ) : null}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      ) : null}

      {/* Floating Banner Toast */}
      {bannerMessage ? (
        <div className="fixed bottom-20 md:bottom-6 right-4 sm:right-6 z-40 max-w-sm bg-[#0F2537] text-white px-4 py-3 rounded-xl shadow-lg border border-[#1D4ED8]/40 flex items-center justify-between gap-3">
          <span className="text-xs font-medium">{bannerMessage}</span>
          <button
            type="button"
            onClick={clearBanner}
            aria-label="Dismiss notification"
            className="text-white/70 hover:text-white cursor-pointer shrink-0"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      ) : null}
    </div>
  );
};

export function App() {
  return (
    <GsmsProvider>
      <GsmsShell />
    </GsmsProvider>
  );
}

export default App;
