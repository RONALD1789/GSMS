import { jsPDF } from 'jspdf';
import { ReportCard, School } from '../types/gsms.ts';

export function exportReportCardPdf(school: School, report: ReportCard): string {
  const doc = new jsPDF({ unit: 'pt', format: 'a4' });
  const pageWidth = doc.internal.pageSize.getWidth();

  // Header band (Oxford Navy #0F2537)
  doc.setFillColor(15, 37, 55);
  doc.rect(0, 0, pageWidth, 112, 'F');

  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(18);
  doc.text(school.name.toUpperCase(), 36, 44);

  doc.setTextColor(209, 250, 229);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(10.5);
  doc.text(`${school.city}, ${school.country}  •  ${report.academicYear} (${report.term})`, 36, 68);
  doc.text(`OFFICIAL STUDENT REPORT CARD  •  v${report.version} [${report.status}]`, 36, 88);

  let y = 148;
  doc.setTextColor(71, 85, 105);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10);
  doc.text('STUDENT INFORMATION', 36, y);

  y += 20;
  doc.setTextColor(15, 23, 42);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(11.5);
  doc.text(`Student Name: ${report.studentName}`, 36, y);
  doc.text(`Class: ${report.className}`, 340, y);

  y += 18;
  doc.text(`Template: ${report.templateName}`, 36, y);
  doc.text(`Overall Average: ${report.averageScore.toFixed(1)}%`, 340, y);

  y += 16;
  doc.setDrawColor(203, 213, 225);
  doc.line(36, y, pageWidth - 36, y);

  y += 26;
  doc.setTextColor(71, 85, 105);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10);
  doc.text('SUBJECT PERFORMANCE SUMMARY', 36, y);

  y += 18;
  doc.setTextColor(15, 23, 42);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(11.5);
  const subjects = report.subjectGradesSummary
    .split(/[,;\n]/)
    .map((s) => s.trim())
    .filter(Boolean);
  for (const subj of subjects) {
    if (y > 720) break;
    doc.text(`• ${subj}`, 44, y);
    y += 18;
  }

  y += 10;
  doc.line(36, y, pageWidth - 36, y);

  y += 24;
  doc.setTextColor(71, 85, 105);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10);
  doc.text('TEACHER REMARKS', 36, y);
  y += 16;
  doc.setTextColor(15, 23, 42);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(11);
  const teacherLines = doc.splitTextToSize(report.teacherRemarks?.trim() || 'No remarks recorded.', pageWidth - 72);
  doc.text(teacherLines, 36, y);
  y += teacherLines.length * 15 + 14;

  if (report.aiSummary?.trim()) {
    doc.setTextColor(124, 58, 237);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.text('AI ACADEMIC INSIGHT', 36, y);
    y += 16;
    doc.setTextColor(15, 23, 42);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(11);
    const aiLines = doc.splitTextToSize(report.aiSummary.trim(), pageWidth - 72);
    doc.text(aiLines, 36, y);
    y += aiLines.length * 15 + 14;
  }

  doc.setTextColor(71, 85, 105);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10);
  doc.text('HEADTEACHER / PRINCIPAL APPROVAL', 36, y);
  y += 16;
  doc.setTextColor(15, 23, 42);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(11);
  const headLines = doc.splitTextToSize(
    report.headteacherRemarks?.trim() || 'Pending principal remarks.',
    pageWidth - 72
  );
  doc.text(headLines, 36, y);
  y += headLines.length * 15 + 24;

  doc.setTextColor(71, 85, 105);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9.5);
  doc.text(
    `Submitted by: ${report.submittedBy || 'Class Teacher'}   |   Reviewed by: ${report.reviewedBy || 'Pending'}`,
    36,
    y
  );

  const safeName = report.studentName.replace(/[^a-zA-Z0-9_-]/g, '_');
  const filename = `GSMS_Report_${safeName}_v${report.version}.pdf`;
  doc.save(filename);
  return filename;
}
