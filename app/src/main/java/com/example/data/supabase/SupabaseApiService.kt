package com.example.data.supabase

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body body: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body body: SupabaseSignUpRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Body body: SupabaseRecoverRequest
    ): Response<ResponseBody>

    @POST("auth/v1/logout")
    suspend fun signOut(): Response<ResponseBody>

    @GET("rest/v1/")
    suspend fun pingPostgrest(): Response<ResponseBody>

    // --- Upserts (Merge Duplicates) ---

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_schools")
    suspend fun upsertSchools(@Body rows: List<SbSchoolRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_branches")
    suspend fun upsertBranches(@Body rows: List<SbBranchRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_user_access_assignments")
    suspend fun upsertAccessAssignments(@Body rows: List<SbAccessAssignmentRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_students")
    suspend fun upsertStudents(@Body rows: List<SbStudentRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_staff")
    suspend fun upsertStaff(@Body rows: List<SbStaffRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_academic_records")
    suspend fun upsertAcademicRecords(@Body rows: List<SbAcademicRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_attendance")
    suspend fun upsertAttendance(@Body rows: List<SbAttendanceRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_report_templates")
    suspend fun upsertReportTemplates(@Body rows: List<SbReportTemplateRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_report_cards")
    suspend fun upsertReportCards(@Body rows: List<SbReportCardRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_finance_entries")
    suspend fun upsertFinanceEntries(@Body rows: List<SbFinanceRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_leave_requests")
    suspend fun upsertLeaveRequests(@Body rows: List<SbLeaveRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_communications")
    suspend fun upsertCommunications(@Body rows: List<SbCommRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_operations")
    suspend fun upsertOperations(@Body rows: List<SbOperationRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/gsms_early_warning_alerts")
    suspend fun upsertAlerts(@Body rows: List<SbAlertRow>): Response<ResponseBody>

    @Headers("Prefer: resolution=ignore-duplicates,return=minimal")
    @POST("rest/v1/gsms_audit_logs")
    suspend fun insertAuditLogs(@Body rows: List<SbAuditLogRow>): Response<ResponseBody>

    // --- Full Multi-Tenant Reads ---

    @GET("rest/v1/gsms_schools")
    suspend fun fetchSchools(
        @Query("select") select: String = "*"
    ): Response<List<SbSchoolRow>>

    @GET("rest/v1/gsms_branches")
    suspend fun fetchBranches(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbBranchRow>>

    @GET("rest/v1/gsms_user_access_assignments")
    suspend fun fetchAccessAssignments(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbAccessAssignmentRow>>

    @GET("rest/v1/gsms_students")
    suspend fun fetchStudents(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbStudentRow>>

    @GET("rest/v1/gsms_staff")
    suspend fun fetchStaff(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbStaffRow>>

    @GET("rest/v1/gsms_academic_records")
    suspend fun fetchAcademicRecords(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbAcademicRow>>

    @GET("rest/v1/gsms_attendance")
    suspend fun fetchAttendance(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbAttendanceRow>>

    @GET("rest/v1/gsms_report_templates")
    suspend fun fetchReportTemplates(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbReportTemplateRow>>

    @GET("rest/v1/gsms_report_cards")
    suspend fun fetchReportCards(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbReportCardRow>>

    @GET("rest/v1/gsms_finance_entries")
    suspend fun fetchFinanceEntries(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbFinanceRow>>

    @GET("rest/v1/gsms_leave_requests")
    suspend fun fetchLeaveRequests(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbLeaveRow>>

    @GET("rest/v1/gsms_communications")
    suspend fun fetchCommunications(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbCommRow>>

    @GET("rest/v1/gsms_operations")
    suspend fun fetchOperations(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbOperationRow>>

    @GET("rest/v1/gsms_early_warning_alerts")
    suspend fun fetchAlerts(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbAlertRow>>

    @GET("rest/v1/gsms_audit_logs")
    suspend fun fetchAuditLogs(
        @Query("select") select: String = "*",
        @Query("school_id") schoolIdFilter: String
    ): Response<List<SbAuditLogRow>>

    // --- Scoped Row Counts (select=id only) ---

    @GET("rest/v1/gsms_schools")
    suspend fun fetchSchoolIds(@Query("select") select: String = "id"): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_branches")
    suspend fun fetchBranchIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_user_access_assignments")
    suspend fun fetchAccessAssignmentIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_students")
    suspend fun fetchStudentIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_staff")
    suspend fun fetchStaffIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_academic_records")
    suspend fun fetchAcademicIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_attendance")
    suspend fun fetchAttendanceIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_report_templates")
    suspend fun fetchReportTemplateIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_report_cards")
    suspend fun fetchReportCardIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_finance_entries")
    suspend fun fetchFinanceIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_leave_requests")
    suspend fun fetchLeaveIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_communications")
    suspend fun fetchCommunicationIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_operations")
    suspend fun fetchOperationIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_early_warning_alerts")
    suspend fun fetchAlertIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>

    @GET("rest/v1/gsms_audit_logs")
    suspend fun fetchAuditLogIds(@Query("select") select: String = "id", @Query("school_id") schoolIdFilter: String): Response<List<SbIdOnlyRow>>
}
