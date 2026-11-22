import React, { useEffect, useState } from 'react';
import {
  Briefcase,
  DollarSign,
  Plus,
  Receipt
} from 'lucide-react';
import {
  EmptyStateCard,
  LearnerScopePicker,
  ModalDialog,
  SectionHeader,
  StatusBadge
} from '../components/UiPrimitives.tsx';
import { RolePolicy } from '../policy/RolePolicy.ts';
import { useGsms } from '../state/GsmsContext.tsx';
import {
  FinanceEntry,
  PORTAL_ROLE_META,
  PortalRole,
  RoleCapability
} from '../types/gsms.ts';

function moduleTypesForSection(section: string): string[] {
  switch (section) {
    case 'Procurement':
      return ['PROCUREMENT'];
    case 'Stores':
      return ['INVENTORY', 'ASSET', 'STOCK_MOVEMENT'];
    case 'Library':
      return ['LIBRARY_BOOK', 'LIBRARY_BORROW'];
    case 'Transport':
      return ['VEHICLE', 'ROUTE', 'TRIP'];
    case 'Welfare':
      return ['WELFARE'];
    case 'Counselling':
      return ['COUNSELLING'];
    case 'Discipline':
      return ['DISCIPLINE'];
    case 'Hostel':
      return ['HOSTEL'];
    case 'Front Desk':
      return ['FRONT_DESK'];
    case 'Admissions':
      return ['ADMISSION'];
    case 'ICT Systems':
      return ['ICT_TICKET'];
    case 'Events':
      return ['EVENT'];
    default:
      return [];
  }
}

function defaultModuleTypeForSection(section: string): string {
  switch (section) {
    case 'Procurement':
      return 'PROCUREMENT';
    case 'Stores':
      return 'INVENTORY';
    case 'Library':
      return 'LIBRARY_BOOK';
    case 'Transport':
      return 'VEHICLE';
    case 'Welfare':
      return 'WELFARE';
    case 'Counselling':
      return 'COUNSELLING';
    case 'Discipline':
      return 'DISCIPLINE';
    case 'Hostel':
      return 'HOSTEL';
    case 'Front Desk':
      return 'FRONT_DESK';
    case 'Admissions':
      return 'ADMISSION';
    case 'ICT Systems':
      return 'ICT_TICKET';
    default:
      return 'EVENT';
  }
}

export const OperationsAndFinanceScreen: React.FC = () => {
  const {
    workspace,
    searchQuery,
    focusStudentId,
    selectFocusStudent,
    recordFinancePayment,
    addOperationRecord
  } = useGsms();

  const [selectedSection, setSelectedSection] = useState<string>('');
  const [payingEntry, setPayingEntry] = useState<FinanceEntry | null>(null);
  const [paymentAmount, setPaymentAmount] = useState('250');

  const [showAddOpModal, setShowAddOpModal] = useState(false);
  const [opModuleType, setOpModuleType] = useState('EVENT');
  const [opTitle, setOpTitle] = useState('');
  const [opSubtitle, setOpSubtitle] = useState('');
  const [opParty, setOpParty] = useState('');
  const [opStatus, setOpStatus] = useState('ACTIVE');
  const [opMetric, setOpMetric] = useState('');
  const [opNotes, setOpNotes] = useState('');

  const activeSchool = workspace?.activeSchool ?? null;
  const activeRole = workspace?.activeRole;
  const extraCaps = workspace?.activeExtraCapabilities ?? [];

  const sections =
    activeRole && activeSchool
      ? RolePolicy.operationSections(activeRole, activeSchool, extraCaps)
      : [];

  useEffect(() => {
    if (sections.length > 0 && !sections.includes(selectedSection)) {
      setSelectedSection(sections[0]);
    }
  }, [sections, selectedSection]);

  if (!workspace || !activeSchool || !activeRole) return null;

  const canRecordPayment = RolePolicy.has(
    activeRole,
    RoleCapability.RECORD_FINANCE_PAYMENT,
    extraCaps
  );
  const canEditOperations =
    RolePolicy.has(activeRole, RoleCapability.EDIT_OPERATIONS, extraCaps) ||
    RolePolicy.has(activeRole, RoleCapability.MANAGE_PROCUREMENT, extraCaps) ||
    RolePolicy.has(activeRole, RoleCapability.MANAGE_STORES, extraCaps) ||
    RolePolicy.has(activeRole, RoleCapability.MANAGE_COUNSELLING, extraCaps) ||
    RolePolicy.has(activeRole, RoleCapability.MANAGE_FRONT_DESK, extraCaps) ||
    RolePolicy.has(activeRole, RoleCapability.MANAGE_ICT_SYSTEMS, extraCaps);

  const requiresLearner = RolePolicy.requiresLearnerScope(activeRole, extraCaps);
  const scopedStudents = RolePolicy.filterStudentsForRole(
    activeRole,
    workspace.students,
    workspace.assignedClasses,
    workspace.linkedStudentIds
  );
  const focusStudent = RolePolicy.resolveFocusStudent(scopedStudents, focusStudentId);

  const q = searchQuery.trim().toLowerCase();

  const scopedFinance = RolePolicy.filterFinanceForRole(
    activeRole,
    workspace.finance,
    focusStudent
  ).filter(
    (f) =>
      !q ||
      f.title.toLowerCase().includes(q) ||
      f.partyName.toLowerCase().includes(q) ||
      f.referenceNo.toLowerCase().includes(q)
  );

  const totalAmountUsd = scopedFinance.reduce((s, f) => s + f.amountUsd, 0);
  const totalPaidUsd = scopedFinance.reduce((s, f) => s + f.paidUsd, 0);

  const sectionTypes = moduleTypesForSection(selectedSection);
  const scopedOps = workspace.operations.filter(
    (op) =>
      sectionTypes.includes(op.moduleType) &&
      (!q ||
        op.title.toLowerCase().includes(q) ||
        (op.subtitle || '').toLowerCase().includes(q) ||
        (op.partyName || '').toLowerCase().includes(q))
  );

  const openAddOperationForSection = () => {
    setOpModuleType(defaultModuleTypeForSection(selectedSection));
    setOpTitle('');
    setOpSubtitle('');
    setOpParty('');
    setOpStatus('ACTIVE');
    setOpMetric('');
    setOpNotes('');
    setShowAddOpModal(true);
  };

  if (sections.length === 0) {
    return (
      <EmptyStateCard
        icon={<Briefcase className="w-5 h-5" />}
        title="Operations Restricted"
        description={`No operations or finance sections are assigned under ${PORTAL_ROLE_META[activeRole].displayName}.`}
      />
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        {sections.map((sec) => {
          const active = sec === selectedSection;
          return (
            <button
              key={sec}
              type="button"
              onClick={() => setSelectedSection(sec)}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold border transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                active
                  ? 'bg-[#0F2537] text-white border-[#0F2537]'
                  : 'bg-white text-[#475569] border-[#D0D9E4] hover:bg-[#E8EEF5]'
              }`}
            >
              {sec}
            </button>
          );
        })}
      </div>

      {selectedSection === 'Finance' ? (
        <div className="space-y-4">
          {requiresLearner ? (
            <LearnerScopePicker
              students={scopedStudents}
              focusStudentId={focusStudentId}
              onSelect={selectFocusStudent}
            />
          ) : null}

          {requiresLearner && !focusStudent ? (
            <EmptyStateCard
              icon={<Receipt className="w-5 h-5" />}
              title="Select a Child to View Invoices"
              description="Choose a linked learner above to view term fee invoices and settlement balances."
            />
          ) : (
            <>
              <div className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex flex-wrap items-center justify-between gap-4">
                <div>
                  <div className="text-xs font-semibold text-[#475569]">
                    {activeRole === PortalRole.HR_MANAGER
                      ? 'Payroll Ledger Summary'
                      : activeRole === PortalRole.PARENT
                        ? `Learner Fee Summary • ${focusStudent?.fullName}`
                        : 'School Finance Ledger'}
                  </div>
                  <div className="text-xl font-bold text-[#0F172A] font-mono-num tabular-nums mt-0.5">
                    ${Math.round(totalPaidUsd).toLocaleString()} / $
                    {Math.round(totalAmountUsd).toLocaleString()} USD
                  </div>
                </div>
                <StatusBadge
                  text={`${totalAmountUsd > 0 ? Math.round((totalPaidUsd / totalAmountUsd) * 100) : 0}% SETTLED`}
                  tone="emerald"
                />
              </div>

              {scopedFinance.length === 0 ? (
                <EmptyStateCard
                  icon={<DollarSign className="w-5 h-5" />}
                  title="No Ledger Entries"
                  description="No matching invoices, expenses, or payroll entries were found for this scope."
                />
              ) : (
                <div className="space-y-3">
                  {scopedFinance.map((entry) => {
                    const remaining = Math.max(0, entry.amountUsd - entry.paidUsd);
                    return (
                      <div
                        key={entry.id}
                        className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex flex-wrap items-center justify-between gap-3"
                      >
                        <div className="min-w-0 space-y-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <StatusBadge
                              text={entry.entryType}
                              tone={
                                entry.entryType === 'INVOICE'
                                  ? 'cobalt'
                                  : entry.entryType === 'PAYROLL'
                                    ? 'violet'
                                    : 'amber'
                              }
                            />
                            <span className="text-xs font-mono-num font-semibold text-[#475569]">
                              {entry.referenceNo}
                            </span>
                            <StatusBadge
                              text={entry.status}
                              tone={
                                entry.status === 'PAID' || entry.status === 'DISBURSED'
                                  ? 'emerald'
                                  : entry.status === 'PARTIAL'
                                    ? 'cobalt'
                                    : 'amber'
                              }
                            />
                          </div>
                          <div className="text-sm font-bold text-[#0F172A]">{entry.title}</div>
                          <div className="text-xs text-[#475569]">
                            {entry.partyName} • {entry.category}
                          </div>
                        </div>

                        <div className="flex items-center gap-3 shrink-0">
                          <div className="text-right font-mono-num tabular-nums">
                            <div className="text-sm font-bold text-[#0F172A]">
                              ${entry.paidUsd.toLocaleString()} / ${entry.amountUsd.toLocaleString()}
                            </div>
                            {remaining > 0 ? (
                              <div className="text-[11px] font-semibold text-[#D97706]">
                                Bal ${remaining.toLocaleString()}
                              </div>
                            ) : (
                              <div className="text-[11px] font-semibold text-[#059669]">Settled</div>
                            )}
                          </div>

                          {canRecordPayment && remaining > 0 ? (
                            <button
                              type="button"
                              onClick={() => {
                                setPayingEntry(entry);
                                setPaymentAmount(String(remaining));
                              }}
                              className="px-3 py-1.5 rounded-lg bg-[#059669] hover:bg-[#047857] text-white text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                            >
                              Record Payment
                            </button>
                          ) : null}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          <SectionHeader
            title={`${selectedSection} Operations`}
            subtitle={`Scoped operational records for ${activeSchool.name}`}
            actionLabel={canEditOperations ? '+ Add Record' : undefined}
            onAction={canEditOperations ? openAddOperationForSection : undefined}
          />
          {scopedOps.length === 0 ? (
            <EmptyStateCard
              icon={<Briefcase className="w-5 h-5" />}
              title={`No ${selectedSection} Records`}
              description={`Add a new ${selectedSection.toLowerCase()} record to start tracking activity.`}
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {scopedOps.map((op) => (
                <div
                  key={op.id}
                  className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-2"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <div className="text-xs font-mono-num font-semibold text-[#1D4ED8]">
                        {op.moduleType}
                      </div>
                      <div className="text-sm font-bold text-[#0F172A] truncate">{op.title}</div>
                      {op.subtitle ? (
                        <div className="text-xs text-[#475569] truncate">{op.subtitle}</div>
                      ) : null}
                    </div>
                    <StatusBadge
                      text={op.status}
                      tone={
                        ['CLEARED', 'ACTIVE', 'DELIVERED', 'IN_STOCK', 'RESOLVED', 'APPROVED'].includes(
                          op.status.toUpperCase()
                        )
                          ? 'emerald'
                          : ['OVERDUE', 'CRITICAL', 'REORDER_DUE', 'OPEN'].includes(
                                op.status.toUpperCase()
                              )
                            ? 'amber'
                            : 'cobalt'
                      }
                    />
                  </div>

                  <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#E8EEF5] text-xs text-[#475569]">
                    <span>{op.partyName || 'Campus Operations'}</span>
                    {op.metricLabel ? (
                      <span className="font-mono-num font-semibold text-[#0F172A]">
                        {op.metricLabel}
                      </span>
                    ) : null}
                  </div>
                  {op.notes ? <p className="text-xs text-[#475569]">{op.notes}</p> : null}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Modal: Record Payment */}
      <ModalDialog
        open={Boolean(payingEntry)}
        title={`Record Payment • ${payingEntry?.referenceNo || ''}`}
        onClose={() => setPayingEntry(null)}
        footer={
          payingEntry ? (
            <>
              <button
                type="button"
                onClick={() => setPayingEntry(null)}
                className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  const amt = Number(paymentAmount) || 0;
                  if (amt <= 0) return;
                  recordFinancePayment(activeSchool.id, payingEntry, amt);
                  setPayingEntry(null);
                }}
                className="px-4 py-2 rounded-xl bg-[#059669] text-white text-xs font-semibold cursor-pointer"
              >
                Confirm Payment
              </button>
            </>
          ) : null
        }
      >
        <div className="space-y-3">
          <div className="text-xs text-[#475569]">
            Party: <span className="font-semibold text-[#0F172A]">{payingEntry?.partyName}</span>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">
              Payment Amount (USD)
            </label>
            <input
              type="number"
              min={1}
              value={paymentAmount}
              onChange={(e) => setPaymentAmount(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] font-mono-num"
            />
          </div>
        </div>
      </ModalDialog>

      {/* Modal: Add Operation Record */}
      <ModalDialog
        open={showAddOpModal}
        title={`Add ${selectedSection} Record`}
        onClose={() => setShowAddOpModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setShowAddOpModal(false)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (!opTitle.trim()) return;
                addOperationRecord(
                  activeSchool,
                  workspace.activeBranchId,
                  opModuleType,
                  opTitle,
                  opSubtitle,
                  opParty,
                  opStatus,
                  opMetric,
                  opNotes
                );
                setShowAddOpModal(false);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              Save Record
            </button>
          </>
        }
      >
        <div className="space-y-3">
          {sectionTypes.length > 1 ? (
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Record Type</label>
              <select
                value={opModuleType}
                onChange={(e) => setOpModuleType(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB]"
              >
                {sectionTypes.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
          ) : null}
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Title</label>
            <input
              type="text"
              value={opTitle}
              onChange={(e) => setOpTitle(e.target.value)}
              placeholder="Record title"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Subtitle</label>
              <input
                type="text"
                value={opSubtitle}
                onChange={(e) => setOpSubtitle(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Party / Owner</label>
              <input
                type="text"
                value={opParty}
                onChange={(e) => setOpParty(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Status</label>
              <input
                type="text"
                value={opStatus}
                onChange={(e) => setOpStatus(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Metric Label</label>
              <input
                type="text"
                value={opMetric}
                onChange={(e) => setOpMetric(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Notes</label>
            <textarea
              rows={2}
              value={opNotes}
              onChange={(e) => setOpNotes(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
        </div>
      </ModalDialog>
    </div>
  );
};
