import React, { useEffect, useState } from 'react';
import {
  BellRing,
  CheckCircle2,
  ClipboardList,
  MessageSquare,
  ShieldCheck,
  Sparkles
} from 'lucide-react';
import {
  BarChartCard,
  EmptyStateCard,
  ModalDialog,
  SectionHeader,
  StatusBadge,
  TrendSparklineCard,
  riskTone
} from '../components/UiPrimitives.tsx';
import { RolePolicy } from '../policy/RolePolicy.ts';
import { useGsms } from '../state/GsmsContext.tsx';
import { PORTAL_ROLE_META, RoleCapability } from '../types/gsms.ts';

export const AnalyticsAndCommsScreen: React.FC = () => {
  const {
    workspace,
    searchQuery,
    updateAlertStatus,
    sendCommunication,
    generateQuickAiDraft
  } = useGsms();

  const [selectedSubTab, setSelectedSubTab] = useState<string>('');
  const [showComposeModal, setShowComposeModal] = useState(false);
  const [channel, setChannel] = useState('IN_APP');
  const [audience, setAudience] = useState('All Parents & Staff');
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [aiDrafted, setAiDrafted] = useState(false);
  const [aiDrafting, setAiDrafting] = useState(false);

  const activeSchool = workspace?.activeSchool ?? null;
  const activeRole = workspace?.activeRole;
  const extraCaps = workspace?.activeExtraCapabilities ?? [];

  const subTabs =
    activeRole && activeSchool
      ? RolePolicy.analyticsSubTabs(activeRole, activeSchool, extraCaps)
      : [];

  useEffect(() => {
    if (subTabs.length > 0 && !subTabs.includes(selectedSubTab)) {
      setSelectedSubTab(subTabs[0]);
    }
  }, [subTabs, selectedSubTab]);

  if (!workspace || !activeSchool || !activeRole) return null;

  const canViewCharts = RolePolicy.has(activeRole, RoleCapability.VIEW_ANALYTICS_CHARTS, extraCaps);
  const canManageAlerts = RolePolicy.has(activeRole, RoleCapability.MANAGE_ALERTS, extraCaps);
  const canCompose = RolePolicy.has(activeRole, RoleCapability.COMPOSE_COMMUNICATIONS, extraCaps);

  const q = searchQuery.trim().toLowerCase();

  const visibleAlerts = RolePolicy.filterAlertsForRole(activeRole, workspace.alerts).filter(
    (al) =>
      !q ||
      al.targetName.toLowerCase().includes(q) ||
      al.category.toLowerCase().includes(q) ||
      al.reason.toLowerCase().includes(q)
  );

  const visibleComms = RolePolicy.filterCommunicationsForRole(
    activeRole,
    workspace.communications
  ).filter(
    (c) =>
      !q ||
      c.title.toLowerCase().includes(q) ||
      c.body.toLowerCase().includes(q) ||
      c.audience.toLowerCase().includes(q)
  );

  const visibleLogs = workspace.auditLogs.filter(
    (lg) =>
      !q ||
      lg.summary.toLowerCase().includes(q) ||
      lg.actorName.toLowerCase().includes(q) ||
      lg.actionType.toLowerCase().includes(q)
  );

  const handleAiNoticeDraft = async () => {
    setAiDrafting(true);
    const text = await generateQuickAiDraft(
      `Draft a polished, concise ${channel} school announcement titled "${title || 'Term Update'}" for audience "${audience}" at ${activeSchool.name}.`,
      'AI_NOTICE_DRAFT'
    );
    if (text) {
      setBody(text);
      setAiDrafted(true);
    }
    setAiDrafting(false);
  };

  if (subTabs.length === 0) {
    return (
      <EmptyStateCard
        icon={<BellRing className="w-5 h-5" />}
        title="Intelligence & Notices Restricted"
        description={`No analytics, communications, or audit sections are enabled under ${PORTAL_ROLE_META[activeRole].displayName}.`}
      />
    );
  }

  const avgAttendance =
    workspace.students.length > 0
      ? Math.round(
          workspace.students.reduce((s, st) => s + st.attendanceRate, 0) / workspace.students.length
        )
      : 92;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        {subTabs.map((tab) => {
          const active = tab === selectedSubTab;
          return (
            <button
              key={tab}
              type="button"
              onClick={() => setSelectedSubTab(tab)}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold border transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                active
                  ? 'bg-[#0F2537] text-white border-[#0F2537]'
                  : 'bg-white text-[#475569] border-[#D0D9E4] hover:bg-[#E8EEF5]'
              }`}
            >
              {tab}
            </button>
          );
        })}
      </div>

      {/* TAB 1: Analytics & Alerts / Role Alerts */}
      {selectedSubTab === 'Analytics & Alerts' || selectedSubTab === 'Role Alerts' ? (
        <div className="space-y-4">
          {canViewCharts ? (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <BarChartCard
                title="Cohort Attendance & Academic Standing"
                subtitle="Live per-student attendance metrics"
                tone="cobalt"
                bars={workspace.students.map((st) => ({
                  label: st.fullName.split(' ')[0],
                  value: st.attendanceRate
                }))}
              />
              <TrendSparklineCard
                title="Term Attendance Stability"
                headlineValue={`${avgAttendance}%`}
                points={[87, 90, 91, 93, avgAttendance]}
                lineColor="#059669"
              />
            </div>
          ) : null}

          <SectionHeader
            title="Early-Warning Risk Alerts"
            subtitle="Automated academic, attendance, finance, staffing & ICT flags"
          />
          {visibleAlerts.length === 0 ? (
            <EmptyStateCard
              icon={<CheckCircle2 className="w-5 h-5" />}
              title="No Active Risk Alerts"
              description="All alerts assigned to your role have been triaged or resolved."
            />
          ) : (
            <div className="space-y-3">
              {visibleAlerts.map((al) => (
                <div
                  key={al.id}
                  className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-2"
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <StatusBadge text={al.category} tone="cobalt" />
                        <StatusBadge text={al.riskLevel} tone={riskTone(al.riskLevel)} />
                        <StatusBadge
                          text={al.status}
                          tone={
                            al.status === 'RESOLVED'
                              ? 'emerald'
                              : al.status === 'IN_PROGRESS'
                                ? 'violet'
                                : 'amber'
                          }
                        />
                      </div>
                      <div className="text-sm font-bold text-[#0F172A] mt-1.5">{al.targetName}</div>
                    </div>

                    {canManageAlerts && al.status !== 'RESOLVED' ? (
                      <div className="flex items-center gap-2 shrink-0">
                        {al.status !== 'IN_PROGRESS' ? (
                          <button
                            type="button"
                            onClick={() => updateAlertStatus(activeSchool.id, al, 'IN_PROGRESS')}
                            className="px-3 py-1.5 rounded-lg bg-[#EDE9FE] hover:bg-[#DDD6FE] text-[#6D28D9] text-xs font-semibold whitespace-nowrap cursor-pointer"
                          >
                            Assign
                          </button>
                        ) : null}
                        <button
                          type="button"
                          onClick={() => updateAlertStatus(activeSchool.id, al, 'RESOLVED')}
                          className="px-3 py-1.5 rounded-lg bg-[#059669] hover:bg-[#047857] text-white text-xs font-semibold whitespace-nowrap cursor-pointer"
                        >
                          Resolve
                        </button>
                      </div>
                    ) : null}
                  </div>
                  <p className="text-xs text-[#475569]">{al.reason}</p>
                  <div className="text-xs font-medium text-[#1D4ED8]">
                    Recommended Action: {al.recommendedAction}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}

      {/* TAB 2: Communications */}
      {selectedSubTab === 'Communications' ? (
        <div className="space-y-3">
          <SectionHeader
            title="School Communications & Notices"
            subtitle="Audience-targeted circulars across SMS, Email, WhatsApp & Portal"
            actionLabel={canCompose ? '+ Compose' : undefined}
            onAction={canCompose ? () => setShowComposeModal(true) : undefined}
          />
          {visibleComms.length === 0 ? (
            <EmptyStateCard
              icon={<MessageSquare className="w-5 h-5" />}
              title="No Notices Posted"
              description="Dispatch a notice to parents, students, or staff."
            />
          ) : (
            <div className="space-y-3">
              {visibleComms.map((cm) => (
                <div
                  key={cm.id}
                  className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-2"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <StatusBadge text={cm.channel} tone="cobalt" />
                      <StatusBadge text={cm.audience} tone="slate" />
                      {cm.aiDrafted ? <StatusBadge text="AI DRAFTED" tone="violet" /> : null}
                    </div>
                    <span className="text-[11px] text-[#475569]">
                      By {cm.senderName} ({cm.senderRole})
                    </span>
                  </div>
                  <div className="text-sm font-bold text-[#0F172A]">{cm.title}</div>
                  <p className="text-xs text-[#475569] leading-relaxed">{cm.body}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}

      {/* TAB 3: Audit Logs */}
      {selectedSubTab === 'Audit Logs' ? (
        <div className="space-y-3">
          <SectionHeader
            title="Immutable RBAC & AI Audit Trail"
            subtitle="Cryptographic event log of mutations, approvals, and Gemini AI invocations"
          />
          {visibleLogs.length === 0 ? (
            <EmptyStateCard
              icon={<ClipboardList className="w-5 h-5" />}
              title="No Audit Entries"
              description="Actions performed in this tenant appear here chronologically."
            />
          ) : (
            <div className="space-y-2">
              {visibleLogs.map((lg) => (
                <div
                  key={lg.id}
                  className="bg-white rounded-2xl border border-[#D0D9E4] p-3.5 shadow-xs flex flex-wrap items-start justify-between gap-3"
                >
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <StatusBadge text={lg.actionType} tone={lg.aiAction ? 'violet' : 'cobalt'} />
                      <StatusBadge text={lg.aiAction ? 'GEMINI AI' : lg.moduleName} tone="slate" />
                      <span className="text-xs font-semibold text-[#0F172A]">
                        {lg.actorName} ({lg.actorRole})
                      </span>
                    </div>
                    <div className="text-xs text-[#475569]">{lg.summary}</div>
                  </div>
                  {lg.createdAtIso ? (
                    <span className="text-[11px] font-mono-num text-[#475569] shrink-0">
                      {lg.createdAtIso.replace('T', ' ').slice(0, 19)}
                    </span>
                  ) : (
                    <ShieldCheck className="w-4 h-4 text-[#059669] shrink-0" />
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}

      {/* Modal: Compose Communication */}
      <ModalDialog
        open={showComposeModal}
        title="Dispatch School Communication"
        onClose={() => setShowComposeModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setShowComposeModal(false)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (!title.trim() || !body.trim()) return;
                sendCommunication(
                  activeSchool,
                  workspace.activeBranchId,
                  channel,
                  audience,
                  title,
                  body,
                  aiDrafted
                );
                setTitle('');
                setBody('');
                setAiDrafted(false);
                setShowComposeModal(false);
              }}
              className="px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              Send Notice
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Channel</label>
              <select
                value={channel}
                onChange={(e) => setChannel(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB]"
              >
                <option value="IN_APP">IN_APP</option>
                <option value="EMAIL">EMAIL</option>
                <option value="SMS">SMS</option>
                <option value="WHATSAPP">WHATSAPP</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Audience</label>
              <input
                type="text"
                value={audience}
                onChange={(e) => setAudience(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Subject / Title</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Midterm Parent-Teacher Conference"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-xs font-semibold text-[#475569]">Message Body</label>
              <button
                type="button"
                disabled={aiDrafting}
                onClick={handleAiNoticeDraft}
                className="inline-flex items-center gap-1 text-xs font-semibold text-[#7C3AED] hover:underline cursor-pointer"
              >
                <Sparkles className="w-3.5 h-3.5" />
                {aiDrafting ? 'Drafting...' : 'AI Draft Notice'}
              </button>
            </div>
            <textarea
              rows={4}
              value={body}
              onChange={(e) => {
                setBody(e.target.value);
                setAiDrafted(false);
              }}
              placeholder="Compose message..."
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
        </div>
      </ModalDialog>
    </div>
  );
};
