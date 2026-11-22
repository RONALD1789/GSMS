import React, { useEffect, useState } from 'react';
import {
  CalendarClock,
  CheckCircle2,
  GraduationCap,
  Pencil,
  Plus,
  Sparkles,
  Trash2,
  UserPlus,
  Users,
  XCircle
} from 'lucide-react';
import {
  EmptyStateCard,
  ModalDialog,
  SectionHeader,
  StatusBadge,
  riskTone
} from '../components/UiPrimitives.tsx';
import { RolePolicy } from '../policy/RolePolicy.ts';
import { useGsms } from '../state/GsmsContext.tsx';
import {
  GsmsModule,
  LeaveStatus,
  PORTAL_ROLE_META,
  RoleCapability,
  StaffMember,
  Student,
  schoolHasModule
} from '../types/gsms.ts';

export const PeopleAndLeaveScreen: React.FC = () => {
  const {
    workspace,
    searchQuery,
    addStudent,
    updateStudent,
    removeStudent,
    addStaffMember,
    updateStaffMember,
    removeStaffMember,
    submitLeaveRequest,
    reviewLeaveRequest,
    generateQuickAiDraft
  } = useGsms();

  const [selectedSubTab, setSelectedSubTab] = useState<string>('');

  // Enroll student modal
  const [showEnrollModal, setShowEnrollModal] = useState(false);
  const [admissionNo, setAdmissionNo] = useState('ADM-2026-099');
  const [studentName, setStudentName] = useState('');
  const [studentClass, setStudentClass] = useState('Grade 10A');
  const [parentName, setParentName] = useState('');
  const [editingStudent, setEditingStudent] = useState<Student | null>(null);
  const [removingStudent, setRemovingStudent] = useState<Student | null>(null);

  // Staff directory modal
  const [showStaffModal, setShowStaffModal] = useState(false);
  const [editingStaff, setEditingStaff] = useState<StaffMember | null>(null);
  const [staffName, setStaffName] = useState('');
  const [staffRoleTitle, setStaffRoleTitle] = useState('');
  const [staffDepartment, setStaffDepartment] = useState('');
  const [staffSubjects, setStaffSubjects] = useState('');
  const [staffWorkload, setStaffWorkload] = useState('20');
  const [staffLeaveBalance, setStaffLeaveBalance] = useState('21');
  const [removingStaff, setRemovingStaff] = useState<StaffMember | null>(null);

  // Apply leave modal
  const [showLeaveModal, setShowLeaveModal] = useState(false);
  const [leaveStaffId, setLeaveStaffId] = useState('');
  const [leaveType, setLeaveType] = useState('Annual');
  const [startDate, setStartDate] = useState('2026-04-12');
  const [endDate, setEndDate] = useState('2026-04-14');
  const [daysCount, setDaysCount] = useState('3');
  const [leaveReason, setLeaveReason] = useState('');
  const [aiConflictNote, setAiConflictNote] = useState('');
  const [aiCheckingLeave, setAiCheckingLeave] = useState(false);

  const activeSchool = workspace?.activeSchool ?? null;
  const activeRole = workspace?.activeRole;
  const extraCaps = workspace?.activeExtraCapabilities ?? [];

  const subTabs =
    activeRole && activeSchool
      ? RolePolicy.peopleSubTabs(activeRole, activeSchool, extraCaps)
      : [];

  useEffect(() => {
    if (subTabs.length > 0 && !subTabs.includes(selectedSubTab)) {
      setSelectedSubTab(subTabs[0]);
    }
  }, [subTabs, selectedSubTab]);

  if (!workspace || !activeSchool || !activeRole) return null;

  const canEnroll =
    RolePolicy.has(activeRole, RoleCapability.ENROLL_STUDENT, extraCaps) &&
    schoolHasModule(activeSchool, GsmsModule.ADMISSIONS);
  const canViewFees = RolePolicy.canViewLearnerFeeBalances(activeRole, extraCaps);
  const canViewLeaveBalances = RolePolicy.canViewStaffLeaveBalances(activeRole, extraCaps);
  const canManageStaff = RolePolicy.has(activeRole, RoleCapability.MANAGE_STAFF, extraCaps);
  const canApplyLeave = RolePolicy.has(activeRole, RoleCapability.APPLY_LEAVE, extraCaps);
  const canApproveLeave = RolePolicy.has(activeRole, RoleCapability.APPROVE_LEAVE, extraCaps);

  const q = searchQuery.trim().toLowerCase();

  const visibleStudents = RolePolicy.filterStudentsForRole(
    activeRole,
    workspace.students,
    workspace.assignedClasses,
    workspace.linkedStudentIds
  ).filter(
    (s) =>
      !q ||
      s.fullName.toLowerCase().includes(q) ||
      s.className.toLowerCase().includes(q) ||
      s.admissionNo.toLowerCase().includes(q)
  );

  const visibleStaff = workspace.staff.filter(
    (sf) =>
      !q ||
      sf.fullName.toLowerCase().includes(q) ||
      sf.roleTitle.toLowerCase().includes(q) ||
      sf.department.toLowerCase().includes(q)
  );

  const visibleLeaves = workspace.leaveRequests.filter(
    (lv) =>
      !q ||
      lv.staffName.toLowerCase().includes(q) ||
      lv.leaveType.toLowerCase().includes(q) ||
      lv.status.toLowerCase().includes(q)
  );

  const handleAiConflictCheck = async () => {
    const sf =
      workspace.staff.find((s) => s.id === (leaveStaffId || workspace.staff[0]?.id)) ||
      workspace.staff[0];
    setAiCheckingLeave(true);
    const note = await generateQuickAiDraft(
      `Analyze timetable cover impact and suggest a substitute teacher for ${sf?.fullName || 'Faculty'} (${sf?.roleTitle || 'Teacher'}, ${sf?.department || 'Department'}) requesting ${daysCount} day(s) of ${leaveType} leave (${startDate} to ${endDate}).`,
      'AI_LEAVE_CONFLICT_CHECK'
    );
    if (note) setAiConflictNote(note);
    setAiCheckingLeave(false);
  };

  if (subTabs.length === 0) {
    return (
      <EmptyStateCard
        icon={<Users className="w-5 h-5" />}
        title="Directory Restricted"
        description={`No People or HR sections are visible under ${PORTAL_ROLE_META[activeRole].displayName}.`}
      />
    );
  }

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

      {/* TAB 1: Students */}
      {selectedSubTab === 'Students' ? (
        <div className="space-y-3">
          <SectionHeader
            title="Student Directory"
            subtitle={`${visibleStudents.length} learners in current scope`}
            actionLabel={canEnroll ? '+ Enroll' : undefined}
            onAction={
              canEnroll
                ? () => {
                    setEditingStudent(null);
                    setStudentName('');
                    setParentName('');
                    setShowEnrollModal(true);
                  }
                : undefined
            }
          />
          {visibleStudents.length === 0 ? (
            <EmptyStateCard
              icon={<GraduationCap className="w-5 h-5" />}
              title="No Matching Students"
              description="Adjust your search filter or enroll a new student."
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {visibleStudents.map((stu) => (
                <div
                  key={stu.id}
                  className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-2"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="text-sm font-bold text-[#0F172A] truncate">
                        {stu.fullName}
                      </div>
                      <div className="text-xs text-[#475569]">
                        {stu.admissionNo} • {stu.className} {stu.stream ? `(${stu.stream})` : ''}
                      </div>
                    </div>
                    <StatusBadge text={stu.riskLevel} tone={riskTone(stu.riskLevel)} />
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#E8EEF5] text-xs">
                    <span className="text-[#475569]">Guardian: {stu.parentName}</span>
                    <div className="flex items-center gap-3 font-mono-num tabular-nums">
                      <span>Att {stu.attendanceRate}%</span>
                      <span>Avg {stu.averageScore}%</span>
                      {canViewFees ? (
                        <span
                          className={`font-bold ${
                            stu.feeBalanceUsd > 0 ? 'text-[#D97706]' : 'text-[#059669]'
                          }`}
                        >
                          Bal ${stu.feeBalanceUsd.toLocaleString()}
                        </span>
                      ) : null}
                    </div>
                  </div>
                  {canEnroll ? (
                    <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#E8EEF5]">
                      <button
                        type="button"
                        onClick={() => {
                          setEditingStudent(stu);
                          setAdmissionNo(stu.admissionNo);
                          setStudentName(stu.fullName);
                          setStudentClass(stu.className);
                          setParentName(stu.parentName);
                          setShowEnrollModal(true);
                        }}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-[#D0D9E4] text-[#1D4ED8] text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                      >
                        <Pencil className="w-3.5 h-3.5" />
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => setRemovingStudent(stu)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#FEE2E2] hover:bg-[#FECACA] text-[#DC2626] text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        Remove
                      </button>
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}

      {/* TAB 2: Staff & Faculty */}
      {selectedSubTab === 'Staff & Faculty' ? (
        <div className="space-y-3">
          <SectionHeader
            title="Staff & Faculty Directory"
            subtitle="Teaching workloads and operational assignments"
            actionLabel={canManageStaff ? '+ Add Staff' : undefined}
            onAction={
              canManageStaff
                ? () => {
                    setEditingStaff(null);
                    setStaffName('');
                    setStaffRoleTitle('');
                    setStaffDepartment('');
                    setStaffSubjects('');
                    setStaffWorkload('20');
                    setStaffLeaveBalance('21');
                    setShowStaffModal(true);
                  }
                : undefined
            }
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {visibleStaff.map((st) => (
              <div
                key={st.id}
                className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-2"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <div className="text-sm font-bold text-[#0F172A] truncate">{st.fullName}</div>
                    <div className="text-xs text-[#475569]">
                      {st.roleTitle} • {st.department}
                    </div>
                  </div>
                  <StatusBadge
                    text={st.onLeave ? 'ON LEAVE' : 'ACTIVE'}
                    tone={st.onLeave ? 'amber' : 'emerald'}
                  />
                </div>
                <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#E8EEF5] text-xs text-[#475569]">
                  <span>{st.subjectsSummary || st.staffNo}</span>
                  <div className="flex items-center gap-3 font-mono-num tabular-nums">
                    <span>{st.workloadHours}h/wk</span>
                    {canViewLeaveBalances ? (
                      <span className="font-semibold text-[#1D4ED8]">
                        Leave Bal: {st.leaveBalanceDays}d
                      </span>
                    ) : null}
                  </div>
                  {canManageStaff ? (
                    <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#E8EEF5]">
                      <button
                        type="button"
                        onClick={() => {
                          setEditingStaff(st);
                          setStaffName(st.fullName);
                          setStaffRoleTitle(st.roleTitle);
                          setStaffDepartment(st.department);
                          setStaffSubjects(st.subjectsSummary || '');
                          setStaffWorkload(String(st.workloadHours));
                          setStaffLeaveBalance(String(st.leaveBalanceDays));
                          setShowStaffModal(true);
                        }}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-[#D0D9E4] text-[#1D4ED8] text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                      >
                        <Pencil className="w-3.5 h-3.5" />
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => setRemovingStaff(st)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#FEE2E2] hover:bg-[#FECACA] text-[#DC2626] text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        Remove
                      </button>
                    </div>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : null}

      {/* TAB 3: Leave Workflow */}
      {selectedSubTab === 'Leave Workflow' ? (
        <div className="space-y-3">
          <SectionHeader
            title="Staff Leave Requests & Cover Approvals"
            subtitle="AI substitute planning & HR leave balance deductions"
            actionLabel={canApplyLeave ? '+ Apply Leave' : undefined}
            onAction={
              canApplyLeave
                ? () => {
                    setLeaveStaffId(workspace.staff[0]?.id || '');
                    setShowLeaveModal(true);
                  }
                : undefined
            }
          />
          {visibleLeaves.length === 0 ? (
            <EmptyStateCard
              icon={<CalendarClock className="w-5 h-5" />}
              title="No Leave Requests"
              description="Submitted staff leave applications appear here for review."
            />
          ) : (
            <div className="space-y-3">
              {visibleLeaves.map((lv) => {
                const staffMember = workspace.staff.find((s) => s.id === lv.staffId);
                return (
                  <div
                    key={lv.id}
                    className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-2.5"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="text-sm font-bold text-[#0F172A]">
                          {lv.staffName} • {lv.leaveType} Leave ({lv.daysCount}d)
                        </div>
                        <div className="text-xs text-[#475569]">
                          {lv.roleTitle} •{' '}
                          <span className="font-mono-num">
                            {lv.startDate} → {lv.endDate}
                          </span>
                        </div>
                      </div>
                      <StatusBadge
                        text={lv.status}
                        tone={
                          lv.status === LeaveStatus.APPROVED
                            ? 'emerald'
                            : lv.status === LeaveStatus.REJECTED
                              ? 'crimson'
                              : 'amber'
                        }
                      />
                    </div>
                    <p className="text-xs text-[#0F172A]">{lv.reason}</p>
                    {lv.aiConflictNote ? (
                      <div className="rounded-xl bg-[#EDE9FE] px-3 py-2 text-xs text-[#5B21B6]">
                        <span className="font-bold">AI Cover Check: </span>
                        {lv.aiConflictNote}
                      </div>
                    ) : null}
                    {canApproveLeave && lv.status === LeaveStatus.PENDING ? (
                      <div className="flex flex-wrap items-center gap-2 pt-1">
                        <button
                          type="button"
                          onClick={() =>
                            reviewLeaveRequest(activeSchool.id, lv, staffMember, true)
                          }
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#059669] hover:bg-[#047857] text-white text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Approve &amp; Mark Leave
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            reviewLeaveRequest(activeSchool.id, lv, staffMember, false)
                          }
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#FEE2E2] hover:bg-[#FECACA] text-[#DC2626] text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                        >
                          <XCircle className="w-3.5 h-3.5" />
                          Reject
                        </button>
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ) : null}

      {/* Modal: Enroll / Edit Student */}
      <ModalDialog
        open={showEnrollModal}
        title={editingStudent ? 'Edit Learner Record' : 'Enroll New Student'}
        onClose={() => {
          setShowEnrollModal(false);
          setEditingStudent(null);
        }}
        footer={
          <>
            <button
              type="button"
              onClick={() => {
                setShowEnrollModal(false);
                setEditingStudent(null);
              }}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (!studentName.trim()) return;
                if (editingStudent) {
                  updateStudent(activeSchool.id, editingStudent, {
                    fullName: studentName,
                    className: studentClass,
                    stream: editingStudent.stream || '',
                    parentName,
                    parentContact: editingStudent.parentContact || ''
                  });
                  setEditingStudent(null);
                } else {
                  addStudent(
                    activeSchool,
                    workspace.activeBranchId,
                    admissionNo,
                    studentName,
                    studentClass,
                    parentName
                  );
                  setStudentName('');
                  setParentName('');
                }
                setShowEnrollModal(false);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              {editingStudent ? 'Save Changes' : 'Enroll Student'}
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Admission No</label>
            <input
              type="text"
              value={admissionNo}
              disabled={Boolean(editingStudent)}
              onChange={(e) => setAdmissionNo(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] font-mono-num disabled:bg-[#F5F7FB] disabled:text-[#94A3B8]"
            />
            {editingStudent ? (
              <p className="text-[11px] text-[#94A3B8] mt-1">
                Admission numbers are fixed once a learner is enrolled.
              </p>
            ) : null}
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Full Name</label>
            <input
              type="text"
              value={studentName}
              onChange={(e) => setStudentName(e.target.value)}
              placeholder="e.g. Nadia Al-Mansoor"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Class</label>
              <input
                type="text"
                value={studentClass}
                onChange={(e) => setStudentClass(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">
                Parent / Guardian
              </label>
              <input
                type="text"
                value={parentName}
                onChange={(e) => setParentName(e.target.value)}
                placeholder="Guardian name"
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
        </div>
      </ModalDialog>

      {/* Modal: Remove Student */}
      <ModalDialog
        open={Boolean(removingStudent)}
        title="Remove Learner Record"
        onClose={() => setRemovingStudent(null)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setRemovingStudent(null)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (removingStudent) removeStudent(activeSchool.id, removingStudent);
                setRemovingStudent(null);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#DC2626] text-white text-xs font-semibold cursor-pointer"
            >
              <Trash2 className="w-3.5 h-3.5" />
              Remove Learner
            </button>
          </>
        }
      >
        <p className="text-xs text-[#475569] leading-relaxed">
          {removingStudent?.fullName} ({removingStudent?.admissionNo}) will be removed from the
          learner directory for {activeSchool.name}. Attendance history and the audit trail stay
          intact. Learners who already have report cards on file cannot be removed.
        </p>
      </ModalDialog>

      {/* Modal: Add / Edit Staff */}
      <ModalDialog
        open={showStaffModal}
        title={editingStaff ? 'Edit Staff Record' : 'Add Staff Member'}
        onClose={() => {
          setShowStaffModal(false);
          setEditingStaff(null);
        }}
        footer={
          <>
            <button
              type="button"
              onClick={() => {
                setShowStaffModal(false);
                setEditingStaff(null);
              }}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (!staffName.trim()) return;
                const input = {
                  fullName: staffName,
                  roleTitle: staffRoleTitle,
                  department: staffDepartment,
                  subjectsSummary: staffSubjects,
                  workloadHours: Number(staffWorkload) || 0,
                  leaveBalanceDays: Number(staffLeaveBalance) || 0
                };
                if (editingStaff) {
                  updateStaffMember(activeSchool.id, editingStaff, input);
                } else {
                  addStaffMember(activeSchool, workspace.activeBranchId, input);
                }
                setShowStaffModal(false);
                setEditingStaff(null);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              <UserPlus className="w-3.5 h-3.5" />
              {editingStaff ? 'Save Changes' : 'Add Staff'}
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Full Name</label>
            <input
              type="text"
              value={staffName}
              onChange={(e) => setStaffName(e.target.value)}
              placeholder="e.g. Grace Nakato"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Role / Title</label>
              <input
                type="text"
                value={staffRoleTitle}
                onChange={(e) => setStaffRoleTitle(e.target.value)}
                placeholder="e.g. Mathematics Teacher"
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Department</label>
              <input
                type="text"
                value={staffDepartment}
                onChange={(e) => setStaffDepartment(e.target.value)}
                placeholder="e.g. Sciences"
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">
              Subjects / Scope
            </label>
            <input
              type="text"
              value={staffSubjects}
              onChange={(e) => setStaffSubjects(e.target.value)}
              placeholder="e.g. Grade 9-11 Mathematics"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">
                Weekly Workload (h)
              </label>
              <input
                type="number"
                min="0"
                value={staffWorkload}
                onChange={(e) => setStaffWorkload(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] font-mono-num"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">
                Leave Balance (days)
              </label>
              <input
                type="number"
                min="0"
                value={staffLeaveBalance}
                onChange={(e) => setStaffLeaveBalance(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] font-mono-num"
              />
            </div>
          </div>
        </div>
      </ModalDialog>

      {/* Modal: Remove Staff */}
      <ModalDialog
        open={Boolean(removingStaff)}
        title="Remove Staff Record"
        onClose={() => setRemovingStaff(null)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setRemovingStaff(null)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (removingStaff) removeStaffMember(activeSchool.id, removingStaff);
                setRemovingStaff(null);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#DC2626] text-white text-xs font-semibold cursor-pointer"
            >
              <Trash2 className="w-3.5 h-3.5" />
              Remove Staff
            </button>
          </>
        }
      >
        <p className="text-xs text-[#475569] leading-relaxed">
          {removingStaff?.fullName} ({removingStaff?.staffNo}) will be removed from the staff
          directory for {activeSchool.name}. Staff with a leave request awaiting review must be
          resolved first.
        </p>
      </ModalDialog>

      {/* Modal: Apply Leave */}
      <ModalDialog
        open={showLeaveModal}
        title="Submit Staff Leave Application"
        onClose={() => setShowLeaveModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setShowLeaveModal(false)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                const sf =
                  workspace.staff.find((s) => s.id === leaveStaffId) || workspace.staff[0];
                if (!sf || !leaveReason.trim()) return;
                submitLeaveRequest(
                  activeSchool,
                  workspace.activeBranchId,
                  sf,
                  leaveType,
                  startDate,
                  endDate,
                  Number(daysCount) || 1,
                  leaveReason,
                  aiConflictNote || null
                );
                setLeaveReason('');
                setAiConflictNote('');
                setShowLeaveModal(false);
              }}
              className="px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              Submit Request
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Staff Member</label>
            <select
              value={leaveStaffId}
              onChange={(e) => setLeaveStaffId(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB]"
            >
              {workspace.staff.map((st) => (
                <option key={st.id} value={st.id}>
                  {st.fullName} ({st.roleTitle})
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Leave Type</label>
              <select
                value={leaveType}
                onChange={(e) => setLeaveType(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB]"
              >
                <option value="Annual">Annual</option>
                <option value="Sick">Sick</option>
                <option value="Maternity">Maternity</option>
                <option value="Emergency">Emergency</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Days</label>
              <input
                type="number"
                min={1}
                value={daysCount}
                onChange={(e) => setDaysCount(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] font-mono-num"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Start Date</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] font-mono-num"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">End Date</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] font-mono-num"
              />
            </div>
          </div>
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-xs font-semibold text-[#475569]">Reason &amp; Details</label>
              <button
                type="button"
                disabled={aiCheckingLeave}
                onClick={handleAiConflictCheck}
                className="inline-flex items-center gap-1 text-xs font-semibold text-[#7C3AED] hover:underline cursor-pointer"
              >
                <Sparkles className="w-3.5 h-3.5" />
                {aiCheckingLeave ? 'Analyzing...' : 'AI Conflict & Cover Check'}
              </button>
            </div>
            <textarea
              rows={2}
              value={leaveReason}
              onChange={(e) => setLeaveReason(e.target.value)}
              placeholder="Reason for leave request..."
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          {aiConflictNote ? (
            <div className="rounded-xl bg-[#EDE9FE] px-3 py-2 text-xs text-[#5B21B6]">
              {aiConflictNote}
            </div>
          ) : null}
        </div>
      </ModalDialog>
    </div>
  );
};
