import React, { useEffect, useState } from 'react';
import {
  Award,
  BookOpen,
  CalendarClock,
  CheckCircle2,
  Download,
  FileText,
  Plus,
  Sparkles,
  UserCheck
} from 'lucide-react';
import {
  EmptyStateCard,
  LearnerScopePicker,
  ModalDialog,
  SectionHeader,
  StatusBadge
} from '../components/UiPrimitives.tsx';
import { exportReportCardPdf } from '../data/pdfExport.ts';
import { RolePolicy } from '../policy/RolePolicy.ts';
import { useGsms } from '../state/GsmsContext.tsx';
import {
  PORTAL_ROLE_META,
  ReportCard,
  ReportStatus,
  RoleCapability
} from '../types/gsms.ts';

export const AcademicsAndReportsScreen: React.FC = () => {
  const {
    workspace,
    searchQuery,
    focusStudentId,
    selectFocusStudent,
    addAcademicRecord,
    recordAttendance,
    createReportTemplate,
    saveOrSubmitReportCard,
    reviewReportCard,
    generateQuickAiDraft,
    showBanner
  } = useGsms();

  const [selectedSubTab, setSelectedSubTab] = useState<string>('');

  // Modal states
  const [showAddAcademicModal, setShowAddAcademicModal] = useState(false);
  const [acadType, setAcadType] = useState('EXAM');
  const [acadTitle, setAcadTitle] = useState('');
  const [acadClass, setAcadClass] = useState('Grade 10A');
  const [acadSubject, setAcadSubject] = useState('Mathematics');
  const [acadTeacher, setAcadTeacher] = useState('');
  const [acadSlot, setAcadSlot] = useState('Term 1');
  const [acadScore, setAcadScore] = useState('86');

  const [showFillReportModal, setShowFillReportModal] = useState(false);
  const [fillStudentId, setFillStudentId] = useState('');
  const [fillTemplateId, setFillTemplateId] = useState('');
  const [fillGrades, setFillGrades] = useState('Math 88% (A), Science 84% (B), English 89% (A)');
  const [fillAverage, setFillAverage] = useState('87');
  const [fillTeacherRemarks, setFillTeacherRemarks] = useState('');
  const [fillAiSummary, setFillAiSummary] = useState('');
  const [aiDraftingReport, setAiDraftingReport] = useState(false);

  const [reviewingReport, setReviewingReport] = useState<ReportCard | null>(null);
  const [headRemarks, setHeadRemarks] = useState('');
  const [aiDraftingReview, setAiDraftingReview] = useState(false);

  const [showNewTemplateModal, setShowNewTemplateModal] = useState(false);
  const [tplName, setTplName] = useState('');
  const [tplClassLevel, setTplClassLevel] = useState('All Classes');
  const [tplSections, setTplSections] = useState(
    'Core Subjects, Attendance Rate, Teacher Remarks, Principal Endorsement'
  );
  const [tplScale, setTplScale] = useState('A-F Percentage');

  const activeSchool = workspace?.activeSchool ?? null;
  const activeRole = workspace?.activeRole;
  const extraCaps = workspace?.activeExtraCapabilities ?? [];

  const subTabs =
    activeRole && activeSchool
      ? RolePolicy.academicSubTabs(activeRole, activeSchool, extraCaps)
      : [];

  useEffect(() => {
    if (subTabs.length > 0 && !subTabs.includes(selectedSubTab)) {
      setSelectedSubTab(subTabs[0]);
    }
  }, [subTabs, selectedSubTab]);

  if (!workspace || !activeSchool || !activeRole) return null;

  const canEditAcademics = RolePolicy.has(activeRole, RoleCapability.EDIT_ACADEMICS, extraCaps);
  const canMarkAttendance = RolePolicy.has(activeRole, RoleCapability.MARK_ATTENDANCE, extraCaps);
  const canFillReport = RolePolicy.has(activeRole, RoleCapability.FILL_SUBMIT_REPORT_CARDS, extraCaps);
  const canApproveReport = RolePolicy.has(
    activeRole,
    RoleCapability.APPROVE_PUBLISH_REPORT_CARDS,
    extraCaps
  );
  const canManageTemplates = RolePolicy.has(
    activeRole,
    RoleCapability.MANAGE_REPORT_TEMPLATES,
    extraCaps
  );

  const requiresLearner = RolePolicy.requiresLearnerScope(activeRole, extraCaps);
  const scopedStudents = RolePolicy.filterStudentsForRole(
    activeRole,
    workspace.students,
    workspace.assignedClasses,
    workspace.linkedStudentIds
  );
  const focusStudent = RolePolicy.resolveFocusStudent(scopedStudents, focusStudentId);

  const q = searchQuery.trim().toLowerCase();

  const scopedAcademics = RolePolicy.filterAcademicsForRole(
    activeRole,
    workspace.academics,
    workspace.assignedClasses,
    workspace.assignedSubjects
  ).filter(
    (rec) =>
      (!requiresLearner || !focusStudent || rec.className === focusStudent.className) &&
      (!q ||
        rec.title.toLowerCase().includes(q) ||
        rec.className.toLowerCase().includes(q) ||
        (rec.subjectName || '').toLowerCase().includes(q))
  );

  const scopedReports = RolePolicy.filterReportCardsForRole(
    activeRole,
    workspace.reportCards,
    scopedStudents,
    focusStudent
  ).filter(
    (rc) =>
      !q ||
      rc.studentName.toLowerCase().includes(q) ||
      rc.className.toLowerCase().includes(q) ||
      rc.status.toLowerCase().includes(q)
  );

  const scopedAttendance = workspace.attendance.filter(
    (att) =>
      (!requiresLearner || !focusStudent || att.targetName === focusStudent.fullName) &&
      (!q ||
        att.targetName.toLowerCase().includes(q) ||
        (att.className || '').toLowerCase().includes(q))
  );

  const reportStatusTone = (status: string) => {
    switch (status) {
      case ReportStatus.PUBLISHED:
        return 'emerald' as const;
      case ReportStatus.APPROVED:
        return 'cobalt' as const;
      case ReportStatus.SUBMITTED:
        return 'amber' as const;
      case ReportStatus.RETURNED:
        return 'crimson' as const;
      default:
        return 'slate' as const;
    }
  };

  const handleAiReportComment = async () => {
    const stu = scopedStudents.find((s) => s.id === (fillStudentId || scopedStudents[0]?.id));
    setAiDraftingReport(true);
    const text = await generateQuickAiDraft(
      `Write a concise, constructive term report card teacher comment and a 2-sentence AI academic summary for ${stu?.fullName || 'the learner'} (${stu?.className || 'Class'}) with grades "${fillGrades}" and overall average ${fillAverage}%.`,
      'AI_REPORT_COMMENT'
    );
    if (text) {
      setFillTeacherRemarks(text);
      setFillAiSummary(text);
    }
    setAiDraftingReport(false);
  };

  const handleAiReviewSummary = async () => {
    if (!reviewingReport) return;
    setAiDraftingReview(true);
    const text = await generateQuickAiDraft(
      `Draft a concise Headteacher endorsement note for ${reviewingReport.studentName} (${reviewingReport.className}), average ${reviewingReport.averageScore}%, grades: ${reviewingReport.subjectGradesSummary}.`,
      'AI_PRINCIPAL_REVIEW'
    );
    if (text) setHeadRemarks(text);
    setAiDraftingReview(false);
  };

  const handleExportPdf = (report: ReportCard) => {
    const filename = exportReportCardPdf(activeSchool, report);
    showBanner(`Downloaded ${filename}`);
  };

  if (subTabs.length === 0) {
    return (
      <EmptyStateCard
        icon={<BookOpen className="w-5 h-5" />}
        title="Academics Unavailable for This Portal"
        description={`No academic modules are enabled or accessible under ${PORTAL_ROLE_META[activeRole].displayName}.`}
      />
    );
  }

  return (
    <div className="space-y-4">
      {/* Sub-navigation bar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
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
      </div>

      {requiresLearner ? (
        <LearnerScopePicker
          students={scopedStudents}
          focusStudentId={focusStudentId}
          onSelect={selectFocusStudent}
        />
      ) : null}

      {requiresLearner && !focusStudent ? (
        <EmptyStateCard
          icon={<BookOpen className="w-5 h-5" />}
          title="Select a Learner First"
          description="Pick a learner above to inspect class timetables, attendance logs, and published term report cards."
        />
      ) : (
        <>
          {/* TAB 1: Academics & Exams */}
          {selectedSubTab === 'Academics & Exams' ? (
            <div className="space-y-3">
              <SectionHeader
                title="Timetables, Exams & Gradebook"
                subtitle={`${activeSchool.activeAcademicYear} • ${activeSchool.activeTerm}`}
                actionLabel={canEditAcademics ? '+ Add Entry' : undefined}
                onAction={
                  canEditAcademics
                    ? () => {
                        setAcadTeacher(workspace.userProfile?.displayName || 'Faculty');
                        setShowAddAcademicModal(true);
                      }
                    : undefined
                }
              />
              {scopedAcademics.length === 0 ? (
                <EmptyStateCard
                  icon={<CalendarClock className="w-5 h-5" />}
                  title="No Academic Entries Found"
                  description="Add a timetable slot, assessment, or exam entry to populate this view."
                />
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {scopedAcademics.map((rec) => (
                    <div
                      key={rec.id}
                      className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex items-start justify-between gap-3"
                    >
                      <div className="min-w-0 space-y-1">
                        <div className="flex items-center gap-2">
                          <StatusBadge
                            text={rec.recordType}
                            tone={rec.recordType === 'EXAM' ? 'violet' : 'cobalt'}
                          />
                          <span className="text-xs font-semibold text-[#475569]">{rec.className}</span>
                        </div>
                        <div className="text-sm font-bold text-[#0F172A] truncate">{rec.title}</div>
                        <div className="text-xs text-[#475569]">
                          {rec.subjectName || 'General'} • {rec.teacherName || 'Faculty'} •{' '}
                          {rec.scheduleSlot || activeSchool.activeTerm}
                        </div>
                      </div>
                      <div className="text-right shrink-0">
                        <div className="text-sm font-bold font-mono-num tabular-nums text-[#0F172A]">
                          {rec.score}/{rec.maxScore}
                        </div>
                        <div className="text-xs font-bold text-[#059669]">Grade {rec.grade || 'A'}</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : null}

          {/* TAB 2: Attendance */}
          {selectedSubTab === 'Attendance' ? (
            <div className="space-y-4">
              {canMarkAttendance ? (
                <div className="space-y-2.5">
                  <SectionHeader
                    title="Daily Roll Call"
                    subtitle="Mark today's attendance across visible learners"
                  />
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {scopedStudents.map((stu) => (
                      <div
                        key={stu.id}
                        className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex flex-wrap items-center justify-between gap-3"
                      >
                        <div className="min-w-0">
                          <div className="text-sm font-bold text-[#0F172A] truncate">
                            {stu.fullName}
                          </div>
                          <div className="text-xs text-[#475569]">
                            {stu.className} • Rate{' '}
                            <span className="font-mono-num font-semibold">{stu.attendanceRate}%</span>
                          </div>
                        </div>
                        <div className="flex items-center gap-1.5 shrink-0">
                          <button
                            type="button"
                            onClick={() =>
                              recordAttendance(
                                activeSchool,
                                workspace.activeBranchId,
                                stu,
                                'PRESENT'
                              )
                            }
                            className="px-2.5 py-1.5 rounded-lg bg-[#D1FAE5] hover:bg-[#A7F3D0] text-[#065F46] text-xs font-bold whitespace-nowrap cursor-pointer"
                          >
                            PRESENT
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              recordAttendance(
                                activeSchool,
                                workspace.activeBranchId,
                                stu,
                                'LATE'
                              )
                            }
                            className="px-2.5 py-1.5 rounded-lg bg-[#FEF3C7] hover:bg-[#FDE68A] text-[#92400E] text-xs font-bold whitespace-nowrap cursor-pointer"
                          >
                            LATE
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              recordAttendance(
                                activeSchool,
                                workspace.activeBranchId,
                                stu,
                                'ABSENT'
                              )
                            }
                            className="px-2.5 py-1.5 rounded-lg bg-[#FEE2E2] hover:bg-[#FECACA] text-[#991B1B] text-xs font-bold whitespace-nowrap cursor-pointer"
                          >
                            ABSENT
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}

              <SectionHeader
                title="Attendance Log"
                subtitle="Recorded learner & staff check-in history"
              />
              {scopedAttendance.length === 0 ? (
                <EmptyStateCard
                  icon={<UserCheck className="w-5 h-5" />}
                  title="No Attendance Records"
                  description="Attendance entries appear here once roll call is recorded."
                />
              ) : (
                <div className="space-y-2">
                  {scopedAttendance.map((att) => (
                    <div
                      key={att.id}
                      className="bg-white rounded-2xl border border-[#D0D9E4] px-4 py-3 shadow-xs flex items-center justify-between gap-3"
                    >
                      <div className="min-w-0">
                        <div className="text-sm font-bold text-[#0F172A] truncate">
                          {att.targetName}
                        </div>
                        <div className="text-xs text-[#475569]">
                          {att.className || att.targetType} •{' '}
                          <span className="font-mono-num">{att.dateIso}</span>
                        </div>
                      </div>
                      <StatusBadge
                        text={att.status}
                        tone={
                          att.status === 'PRESENT'
                            ? 'emerald'
                            : att.status === 'LATE'
                              ? 'amber'
                              : 'crimson'
                        }
                      />
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : null}

          {/* TAB 3: Report Cards */}
          {selectedSubTab === 'Report Cards' ? (
            <div className="space-y-3">
              <SectionHeader
                title="Term Report Cards & Principal Approvals"
                subtitle="Draft, AI-comment, submit, endorse, publish, and export PDF"
                actionLabel={canFillReport ? '+ Fill Report' : undefined}
                onAction={
                  canFillReport
                    ? () => {
                        setFillStudentId(scopedStudents[0]?.id || '');
                        setFillTemplateId(workspace.reportTemplates[0]?.id || '');
                        setShowFillReportModal(true);
                      }
                    : undefined
                }
              />

              {scopedReports.length === 0 ? (
                <EmptyStateCard
                  icon={<FileText className="w-5 h-5" />}
                  title="No Report Cards Available"
                  description={
                    requiresLearner
                      ? 'Published report cards for this learner will appear here once endorsed by the Headteacher.'
                      : 'Use "+ Fill Report" to draft and submit a learner report card.'
                  }
                />
              ) : (
                <div className="space-y-3">
                  {scopedReports.map((rc) => (
                    <div
                      key={rc.id}
                      className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-3"
                    >
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-base font-bold text-[#0F172A]">
                              {rc.studentName}
                            </span>
                            <StatusBadge text={rc.status} tone={reportStatusTone(rc.status)} />
                            <span className="text-xs font-mono-num text-[#475569]">
                              v{rc.version}
                            </span>
                          </div>
                          <div className="text-xs text-[#475569] mt-0.5">
                            {rc.className} • {rc.templateName} • {rc.academicYear} ({rc.term})
                          </div>
                        </div>
                        <div className="text-right shrink-0">
                          <div className="text-lg font-bold font-mono-num tabular-nums text-[#1D4ED8]">
                            {rc.averageScore.toFixed(1)}%
                          </div>
                          <div className="text-[11px] text-[#475569]">Overall Avg</div>
                        </div>
                      </div>

                      <div className="rounded-xl bg-[#F5F7FB] border border-[#E8EEF5] p-3 text-xs space-y-1.5">
                        <div>
                          <span className="font-semibold text-[#0F172A]">Subjects: </span>
                          <span className="text-[#475569]">{rc.subjectGradesSummary}</span>
                        </div>
                        {rc.teacherRemarks ? (
                          <div>
                            <span className="font-semibold text-[#0F172A]">Teacher: </span>
                            <span className="text-[#475569]">{rc.teacherRemarks}</span>
                          </div>
                        ) : null}
                        {rc.aiSummary ? (
                          <div>
                            <span className="font-semibold text-[#7C3AED]">AI Summary: </span>
                            <span className="text-[#475569]">{rc.aiSummary}</span>
                          </div>
                        ) : null}
                        {rc.headteacherRemarks ? (
                          <div>
                            <span className="font-semibold text-[#059669]">Principal: </span>
                            <span className="text-[#475569]">{rc.headteacherRemarks}</span>
                          </div>
                        ) : null}
                      </div>

                      <div className="flex flex-wrap items-center gap-2 pt-1">
                        {canFillReport &&
                        (rc.status === ReportStatus.DRAFT ||
                          rc.status === ReportStatus.RETURNED) ? (
                          <button
                            type="button"
                            onClick={() =>
                              reviewReportCard(
                                activeSchool.id,
                                rc,
                                ReportStatus.SUBMITTED,
                                rc.headteacherRemarks || ''
                              )
                            }
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] text-white text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            Submit
                          </button>
                        ) : null}

                        {canApproveReport && rc.status === ReportStatus.SUBMITTED ? (
                          <button
                            type="button"
                            onClick={() => {
                              setReviewingReport(rc);
                              setHeadRemarks(rc.headteacherRemarks || '');
                            }}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#059669] hover:bg-[#047857] text-white text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                          >
                            <Award className="w-3.5 h-3.5" />
                            Review &amp; Approve
                          </button>
                        ) : null}

                        {canApproveReport && rc.status === ReportStatus.APPROVED ? (
                          <button
                            type="button"
                            onClick={() =>
                              reviewReportCard(
                                activeSchool.id,
                                rc,
                                ReportStatus.PUBLISHED,
                                rc.headteacherRemarks || 'Approved and published to Parent Portal.'
                              )
                            }
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#7C3AED] hover:bg-[#6D28D9] text-white text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                          >
                            Publish to Parent
                          </button>
                        ) : null}

                        <button
                          type="button"
                          onClick={() => handleExportPdf(rc)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#0F2537] hover:bg-[#091826] text-white text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                        >
                          <Download className="w-3.5 h-3.5" />
                          PDF
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : null}

          {/* TAB 4: Templates */}
          {selectedSubTab === 'Templates' ? (
            <div className="space-y-3">
              <SectionHeader
                title="Report Card Templates"
                subtitle="Configurable school-specific grading scales and sections"
                actionLabel={canManageTemplates ? '+ New Template' : undefined}
                onAction={canManageTemplates ? () => setShowNewTemplateModal(true) : undefined}
              />
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {workspace.reportTemplates.map((tpl) => (
                  <div
                    key={tpl.id}
                    className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-2"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-sm font-bold text-[#0F172A]">{tpl.name}</span>
                      <StatusBadge text={tpl.active ? 'ACTIVE' : 'ARCHIVED'} tone="emerald" />
                    </div>
                    <div className="text-xs text-[#475569]">
                      Level: {tpl.classLevel} • Scale: {tpl.gradingScale}
                    </div>
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {tpl.sections.map((sec) => (
                        <span
                          key={sec}
                          className="px-2 py-0.5 rounded-md bg-[#F5F7FB] border border-[#D0D9E4] text-[11px] font-medium text-[#475569]"
                        >
                          {sec}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </>
      )}

      {/* Modal: Add Academic Entry */}
      <ModalDialog
        open={showAddAcademicModal}
        title="Add Timetable / Exam Entry"
        onClose={() => setShowAddAcademicModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setShowAddAcademicModal(false)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] hover:bg-[#E8EEF5] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (!acadTitle.trim()) return;
                addAcademicRecord(
                  activeSchool,
                  workspace.activeBranchId,
                  acadType,
                  acadTitle,
                  acadClass,
                  acadSubject,
                  acadTeacher,
                  acadSlot,
                  Number(acadScore) || 85
                );
                setAcadTitle('');
                setShowAddAcademicModal(false);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              Save Entry
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Record Type</label>
            <select
              value={acadType}
              onChange={(e) => setAcadType(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB]"
            >
              <option value="EXAM">EXAM</option>
              <option value="TIMETABLE">TIMETABLE</option>
              <option value="GRADEBOOK">GRADEBOOK</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Title</label>
            <input
              type="text"
              value={acadTitle}
              onChange={(e) => setAcadTitle(e.target.value)}
              placeholder="e.g. Term 1 Calculus Midterm"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Class</label>
              <input
                type="text"
                value={acadClass}
                onChange={(e) => setAcadClass(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Subject</label>
              <input
                type="text"
                value={acadSubject}
                onChange={(e) => setAcadSubject(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Schedule Slot</label>
              <input
                type="text"
                value={acadSlot}
                onChange={(e) => setAcadSlot(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Score (0-100)</label>
              <input
                type="number"
                value={acadScore}
                onChange={(e) => setAcadScore(e.target.value)}
                min={0}
                max={100}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] font-mono-num"
              />
            </div>
          </div>
        </div>
      </ModalDialog>

      {/* Modal: Fill & Submit Report Card */}
      <ModalDialog
        open={showFillReportModal}
        title="Fill & Submit Report Card"
        onClose={() => setShowFillReportModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => {
                const stu = scopedStudents.find((s) => s.id === fillStudentId) || scopedStudents[0];
                const tpl =
                  workspace.reportTemplates.find((t) => t.id === fillTemplateId) ||
                  workspace.reportTemplates[0];
                if (!stu || !tpl) return;
                saveOrSubmitReportCard(
                  activeSchool,
                  workspace.activeBranchId,
                  stu,
                  tpl,
                  fillGrades,
                  Number(fillAverage) || 85,
                  fillTeacherRemarks,
                  fillAiSummary || null,
                  false
                );
                setShowFillReportModal(false);
              }}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] bg-white text-xs font-semibold text-[#0F172A] hover:bg-[#E8EEF5] cursor-pointer"
            >
              Save Draft
            </button>
            <button
              type="button"
              onClick={() => {
                const stu = scopedStudents.find((s) => s.id === fillStudentId) || scopedStudents[0];
                const tpl =
                  workspace.reportTemplates.find((t) => t.id === fillTemplateId) ||
                  workspace.reportTemplates[0];
                if (!stu || !tpl) return;
                saveOrSubmitReportCard(
                  activeSchool,
                  workspace.activeBranchId,
                  stu,
                  tpl,
                  fillGrades,
                  Number(fillAverage) || 85,
                  fillTeacherRemarks,
                  fillAiSummary || null,
                  true
                );
                setShowFillReportModal(false);
              }}
              className="px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              Submit for Review
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Student</label>
            <select
              value={fillStudentId}
              onChange={(e) => setFillStudentId(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB]"
            >
              {scopedStudents.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.fullName} ({s.className})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Template</label>
            <select
              value={fillTemplateId}
              onChange={(e) => setFillTemplateId(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] bg-[#F5F7FB]"
            >
              {workspace.reportTemplates.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Subject Grades</label>
            <input
              type="text"
              value={fillGrades}
              onChange={(e) => setFillGrades(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">
              Average Score (%)
            </label>
            <input
              type="number"
              value={fillAverage}
              onChange={(e) => setFillAverage(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4] font-mono-num"
            />
          </div>
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-xs font-semibold text-[#475569]">Teacher Remarks</label>
              <button
                type="button"
                disabled={aiDraftingReport}
                onClick={handleAiReportComment}
                className="inline-flex items-center gap-1 text-xs font-semibold text-[#7C3AED] hover:underline cursor-pointer"
              >
                <Sparkles className="w-3.5 h-3.5" />
                {aiDraftingReport ? 'Generating...' : 'AI Generate Comment'}
              </button>
            </div>
            <textarea
              rows={3}
              value={fillTeacherRemarks}
              onChange={(e) => setFillTeacherRemarks(e.target.value)}
              placeholder="Term academic progress comment..."
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
        </div>
      </ModalDialog>

      {/* Modal: Review & Approve Report Card */}
      <ModalDialog
        open={Boolean(reviewingReport)}
        title={`Review Report • ${reviewingReport?.studentName || ''}`}
        onClose={() => setReviewingReport(null)}
        footer={
          reviewingReport ? (
            <>
              <button
                type="button"
                onClick={() => {
                  reviewReportCard(
                    activeSchool.id,
                    reviewingReport,
                    ReportStatus.RETURNED,
                    headRemarks || 'Please revise remarks before final approval.'
                  );
                  setReviewingReport(null);
                }}
                className="px-3.5 py-2 rounded-xl bg-[#FEE2E2] text-[#DC2626] text-xs font-semibold cursor-pointer"
              >
                Return to Teacher
              </button>
              <button
                type="button"
                onClick={() => {
                  reviewReportCard(
                    activeSchool.id,
                    reviewingReport,
                    ReportStatus.APPROVED,
                    headRemarks || 'Endorsed by Principal.'
                  );
                  setReviewingReport(null);
                }}
                className="px-4 py-2 rounded-xl bg-[#059669] text-white text-xs font-semibold cursor-pointer"
              >
                Approve
              </button>
            </>
          ) : null
        }
      >
        <div className="space-y-3">
          <div className="text-xs text-[#475569]">
            Grades: <span className="font-semibold text-[#0F172A]">{reviewingReport?.subjectGradesSummary}</span>
          </div>
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-xs font-semibold text-[#475569]">Headteacher Remarks</label>
              <button
                type="button"
                disabled={aiDraftingReview}
                onClick={handleAiReviewSummary}
                className="inline-flex items-center gap-1 text-xs font-semibold text-[#7C3AED] hover:underline cursor-pointer"
              >
                <Sparkles className="w-3.5 h-3.5" />
                {aiDraftingReview ? 'Summarizing...' : 'AI Review Summary'}
              </button>
            </div>
            <textarea
              rows={3}
              value={headRemarks}
              onChange={(e) => setHeadRemarks(e.target.value)}
              placeholder="Principal endorsement note..."
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
        </div>
      </ModalDialog>

      {/* Modal: New Template */}
      <ModalDialog
        open={showNewTemplateModal}
        title="Create Report Template"
        onClose={() => setShowNewTemplateModal(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setShowNewTemplateModal(false)}
              className="px-3.5 py-2 rounded-xl border border-[#D0D9E4] text-xs font-semibold text-[#475569] cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => {
                if (!tplName.trim()) return;
                createReportTemplate(activeSchool, tplName, tplClassLevel, tplSections, tplScale);
                setTplName('');
                setShowNewTemplateModal(false);
              }}
              className="px-4 py-2 rounded-xl bg-[#1D4ED8] text-white text-xs font-semibold cursor-pointer"
            >
              Create Template
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">Template Name</label>
            <input
              type="text"
              value={tplName}
              onChange={(e) => setTplName(e.target.value)}
              placeholder="e.g. IB Senior Academic Report"
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Class Level</label>
              <input
                type="text"
                value={tplClassLevel}
                onChange={(e) => setTplClassLevel(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-[#475569] mb-1">Grading Scale</label>
              <input
                type="text"
                value={tplScale}
                onChange={(e) => setTplScale(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1">
              Sections (comma-separated)
            </label>
            <input
              type="text"
              value={tplSections}
              onChange={(e) => setTplSections(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-[#D0D9E4]"
            />
          </div>
        </div>
      </ModalDialog>
    </div>
  );
};
