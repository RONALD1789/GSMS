import test from 'node:test';
import assert from 'node:assert/strict';
import { RolePolicy } from './RolePolicy.ts';
import { PortalRole, ReportStatus } from '../types/gsms.ts';
import type { AcademicRecord, ReportCard, Student } from '../types/gsms.ts';

const learner = (id: string, className: string): Student =>
  ({ id, className, fullName: id } as Student);

test('parent and student access fails closed without explicit linked learners', () => {
  const learners = [learner('a', 'P1'), learner('b', 'P2')];
  assert.deepEqual(RolePolicy.filterStudentsForRole(PortalRole.PARENT, learners), []);
  assert.deepEqual(RolePolicy.filterStudentsForRole(PortalRole.STUDENT, learners), []);
});

test('parent and student access is limited to explicitly linked learners', () => {
  const learners = [learner('a', 'P1'), learner('b', 'P2')];
  assert.deepEqual(
    RolePolicy.filterStudentsForRole(PortalRole.PARENT, learners, [], ['b']).map((s) => s.id),
    ['b']
  );
  assert.deepEqual(
    RolePolicy.filterStudentsForRole(PortalRole.STUDENT, learners, [], ['a']).map((s) => s.id),
    ['a']
  );
});

test('teacher and class teacher access fails closed without assigned classes', () => {
  const learners = [learner('a', 'P1'), learner('b', 'P2')];
  assert.deepEqual(RolePolicy.filterStudentsForRole(PortalRole.TEACHER, learners), []);
  assert.deepEqual(RolePolicy.filterStudentsForRole(PortalRole.CLASS_TEACHER, learners), []);
});

test('teacher learner access is limited to assigned classes', () => {
  const learners = [learner('a', 'P1'), learner('b', 'P2')];
  assert.deepEqual(
    RolePolicy.filterStudentsForRole(PortalRole.TEACHER, learners, ['p2']).map((s) => s.id),
    ['b']
  );
});

test('teacher academic access requires an assigned class and assigned subject', () => {
  const records = [
    { className: 'P1', subjectName: 'Math' },
    { className: 'P2', subjectName: 'Math' },
    { className: 'P2', subjectName: 'English' }
  ] as AcademicRecord[];
  assert.deepEqual(RolePolicy.filterAcademicsForRole(PortalRole.TEACHER, records), []);
  assert.deepEqual(
    RolePolicy.filterAcademicsForRole(PortalRole.TEACHER, records, ['P2'], ['Math']),
    [records[1]]
  );
});

test('parent report access includes only published reports for linked children', () => {
  const linked = [learner('child-1', 'P1')];
  const reports = [
    { studentId: 'child-1', status: ReportStatus.PUBLISHED },
    { studentId: 'child-1', status: ReportStatus.DRAFT },
    { studentId: 'child-2', status: ReportStatus.PUBLISHED }
  ] as ReportCard[];
  assert.deepEqual(
    RolePolicy.filterReportCardsForRole(PortalRole.PARENT, reports, linked),
    [reports[0]]
  );
  assert.deepEqual(RolePolicy.filterReportCardsForRole(PortalRole.PARENT, reports), []);
});
