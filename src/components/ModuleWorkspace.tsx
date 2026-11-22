import { useEffect, useMemo, useState } from "react";
import { AlertCircle, ArrowLeft, CheckCircle2, ChevronLeft, ChevronRight, CirclePlus, Download, LoaderCircle, RefreshCw, Search, ShieldCheck } from "lucide-react";
import { supabase } from "../lib/supabase";

type Row = Record<string, unknown>;
type Column = { key: string; label: string };
type Config = { table: string; columns: Column[]; filter?: { key: string; value: string } | { key: string; values: string[] }; schoolKey?: string; description: string };

const configs: Record<string,Config> = {
  "Students": { table:"gsms_students", columns:[{key:"admission_no",label:"Admission No."},{key:"full_name",label:"Learner"},{key:"class_name",label:"Class"},{key:"stream",label:"Stream"},{key:"attendance_rate",label:"Attendance"},{key:"average_score",label:"Average"}], description:"Learner directory. Records returned here are limited by your school, branch, class and linked-student permissions." },
  "Admissions": { table:"gsms_admissions_student_summaries", columns:[{key:"admission_no",label:"Admission No."},{key:"full_name",label:"Learner"},{key:"class_name",label:"Class"},{key:"stream",label:"Stream"},{key:"parent_name",label:"Parent / Guardian"},{key:"parent_contact",label:"Contact"}], description:"Admission-safe learner directory for authorised school admissions users." },
  "Classes & subjects": { table:"gsms_academic_records", columns:[{key:"title",label:"Record"},{key:"record_type",label:"Type"},{key:"class_name",label:"Class"},{key:"subject_name",label:"Subject"},{key:"teacher_name",label:"Teacher"},{key:"academic_year",label:"Academic year"},{key:"term",label:"Term"}], description:"Class and subject records available to your assigned academic scope." },
  "Attendance": { table:"gsms_attendance", columns:[{key:"target_name",label:"Learner / Staff"},{key:"target_type",label:"Type"},{key:"class_name",label:"Class"},{key:"date_iso",label:"Date"},{key:"status",label:"Status"}], description:"Attendance records visible within your assigned branch and class, or for your linked learners." },
  "Assessments & reports": { table:"gsms_report_cards", columns:[{key:"student_name",label:"Learner"},{key:"class_name",label:"Class"},{key:"template_name",label:"Report template"},{key:"average_score",label:"Average"},{key:"status",label:"Workflow status"},{key:"academic_year",label:"Year"},{key:"term",label:"Term"}], description:"Report cards follow the draft, submission, review and publication permissions in Supabase." },
  "Timetable": { table:"gsms_academic_records", columns:[{key:"title",label:"Lesson / Activity"},{key:"class_name",label:"Class"},{key:"subject_name",label:"Subject"},{key:"teacher_name",label:"Teacher"},{key:"schedule_slot",label:"Time"},{key:"academic_year",label:"Year"},{key:"term",label:"Term"}], filter:{key:"record_type",value:"TIMETABLE"}, description:"Timetable entries for classes and branches you are authorised to access." },
  "Staff": { table:"gsms_staff", columns:[{key:"staff_no",label:"Staff No."},{key:"full_name",label:"Staff member"},{key:"role_title",label:"Position"},{key:"department",label:"Department"},{key:"leave_balance_days",label:"Leave balance"},{key:"on_leave",label:"On leave"}], description:"Staff records are restricted to leadership, HR and each staff member's own permitted profile." },
  "Fees & payments": { table:"gsms_finance_entries", columns:[{key:"reference_no",label:"Reference"},{key:"title",label:"Description"},{key:"party_name",label:"Payer"},{key:"amount_usd",label:"Amount"},{key:"paid_usd",label:"Paid"},{key:"status",label:"Status"}], filter:{key:"entry_type",value:"INVOICE"}, description:"Invoice and payment information is restricted to authorised finance users and linked parent accounts." },
  "Payroll": { table:"gsms_finance_entries", columns:[{key:"reference_no",label:"Reference"},{key:"title",label:"Payroll item"},{key:"party_name",label:"Staff member"},{key:"amount_usd",label:"Amount"},{key:"paid_usd",label:"Paid"},{key:"status",label:"Status"}], filter:{key:"entry_type",value:"PAYROLL"}, description:"Payroll entries available to authorised school finance and HR roles." },
  "Library": { table:"gsms_operations", columns:[{key:"title",label:"Item / Activity"},{key:"subtitle",label:"Details"},{key:"party_name",label:"Assigned to"},{key:"status",label:"Status"},{key:"notes",label:"Notes"}], filter:{key:"module_type",values:["LIBRARY","LIBRARY_ITEM","LIBRARY_LOAN","BOOK","LIBRARY_CATALOG"]}, description:"Library operations visible under your assigned library permissions." },
  "Transport": { table:"gsms_operations", columns:[{key:"title",label:"Vehicle / Route"},{key:"module_type",label:"Type"},{key:"subtitle",label:"Details"},{key:"party_name",label:"Assigned to"},{key:"status",label:"Status"}], filter:{key:"module_type",values:["VEHICLE","ROUTE","TRIP"]}, description:"Transport operations visible to the transport team and authorised school leadership." },
  "Messages": { table:"gsms_communications", columns:[{key:"title",label:"Message"},{key:"channel",label:"Channel"},{key:"audience",label:"Audience"},{key:"sender_name",label:"Sent by"},{key:"sender_role",label:"Role"},{key:"updated_at",label:"Date"}], description:"School communications. Supabase policies determine which audiences and class messages you can access." },
  "School calendar": { table:"gsms_academic_records", columns:[{key:"title",label:"Event"},{key:"class_name",label:"Class / Audience"},{key:"schedule_slot",label:"Date / Time"},{key:"teacher_name",label:"Coordinator"},{key:"term",label:"Term"}], filter:{key:"record_type",value:"EVENT"}, description:"School events and calendar entries in your authorised academic scope." },
  "Reports & analytics": { table:"gsms_early_warning_alerts", columns:[{key:"category",label:"Category"},{key:"risk_level",label:"Risk"},{key:"target_name",label:"Learner / Target"},{key:"reason",label:"Reason"},{key:"assigned_role",label:"Assigned to"},{key:"status",label:"Status"}], description:"Early-warning alerts and follow-up items available to authorised school leadership." },
  "User access": { table:"gsms_user_access_assignments", columns:[{key:"full_name",label:"User"},{key:"email",label:"Email"},{key:"primary_role",label:"Portal role"},{key:"assigned_classes",label:"Classes"},{key:"active",label:"Active"}], description:"Role assignments. You can view only records granted by the user-access RLS policy." },
  "Settings": { table:"gsms_school_summaries", schoolKey:"id", columns:[{key:"name",label:"School name"},{key:"code",label:"School code"},{key:"country",label:"Country"},{key:"city",label:"City"},{key:"active_academic_year",label:"Academic year"},{key:"active_term",label:"Active term"},{key:"plan",label:"Plan"}], description:"School tenant profile. Sensitive owner and subscription data is masked for non-owners." },
};

function valueText(value: unknown) {
  if(value===null || value===undefined || value==="") return "—";
  if(typeof value==="boolean") return value ? "Yes" : "No";
  if(Array.isArray(value)) return value.join(", ") || "—";
  if(typeof value==="number") return Number.isInteger(value)?String(value):value.toFixed(1);
  return String(value);
}
function statusClass(value: unknown) {
  const s=String(value||"").toLowerCase();
  if(["approved","published","active","present","paid","complete","completed","cleared"].includes(s)) return "success";
  if(["submitted","pending","draft","open","in progress","partial"].includes(s)) return "pending";
  if(["rejected","overdue","absent","high","critical","suspended","inactive"].includes(s)) return "danger";
  return "neutral";
}

export default function ModuleWorkspace({page,schoolId,onBack}:{page:string;schoolId:string;onBack:()=>void}) {
  const config=configs[page];
  const [rows,setRows]=useState<Row[]>([]);
  const [loading,setLoading]=useState(true);
  const [error,setError]=useState("");
  const [search,setSearch]=useState("");
  const [refreshKey,setRefreshKey]=useState(0);
  const [pageIndex,setPageIndex]=useState(0);
  const pageSize=12;

  useEffect(()=>{
    let active=true;
    async function load(){
      if(!supabase || !config || !schoolId){setRows([]);setLoading(false);return;}
      setLoading(true);setError("");
      let query=supabase.from(config.table).select(config.columns.map(c=>c.key).join(","))
        .eq(config.schoolKey||"school_id",schoolId).limit(100);
      if(config.filter){
        if("values" in config.filter) query=query.in(config.filter.key,config.filter.values);
        else query=query.eq(config.filter.key,config.filter.value);
      }
      const result=await query;
      if(!active)return;
      if(result.error){setError(result.error.message);setRows([]);}
      else setRows((result.data||[]) as Row[]);
      setLoading(false);
    }
    load();
    return ()=>{active=false;};
  },[page,schoolId,refreshKey]);

  const visibleRows=useMemo(()=>{
    const q=search.trim().toLowerCase();
    if(!q)return rows;
    return rows.filter(row=>Object.values(row).some(value=>valueText(value).toLowerCase().includes(q)));
  },[rows,search]);
  const pageCount=Math.max(1,Math.ceil(visibleRows.length/pageSize));
  const pageRows=visibleRows.slice(pageIndex*pageSize,(pageIndex+1)*pageSize);

  useEffect(()=>setPageIndex(0),[search,page]);
  if(!config) return <section className="module-page"><div className="module-page-icon"><ShieldCheck size={25}/></div><span className="eyebrow dark">WORKSPACE</span><h1>{page}</h1><p className="module-intro">This area is being prepared for the next GSMS feature release.</p><button className="secondary-btn" onClick={onBack}>← Back to overview</button></section>;

  return <section className="data-workspace">
    <div className="data-heading"><div><button className="back-link" onClick={onBack}><ArrowLeft size={14}/> Overview</button><span className="eyebrow dark">{config.table.replace("gsms_","").replaceAll("_"," ").toUpperCase()}</span><h1>{page}</h1><p>{config.description}</p></div><button className="refresh-btn" onClick={()=>setRefreshKey(v=>v+1)} disabled={loading}><RefreshCw size={15}/> Refresh</button></div>
    <div className="data-toolbar"><div className="data-search"><Search size={16}/><input value={search} onChange={e=>setSearch(e.target.value)} placeholder={`Search ${page.toLowerCase()}…`}/></div><div className="data-toolbar-right"><span className="row-count">{visibleRows.length} {visibleRows.length===1?"record":"records"}</span></div></div>
    {loading?<div className="data-state"><LoaderCircle className="spin" size={24}/><span>Loading records securely…</span></div>
    :error?<div className="data-state error-state"><AlertCircle size={24}/><b>Unable to load this module</b><span>{error}</span><small>Confirm that the Supabase schema and role-scoped RLS migration are applied and that this role is allowed to read the selected records.</small><button className="secondary-btn" onClick={()=>setRefreshKey(v=>v+1)}>Try again</button></div>
    :pageRows.length===0?<div className="data-state"><div className="empty-data-icon"><CheckCircle2 size={23}/></div><b>{search?"No matching records":"No records to display"}</b><span>{search?"Try another search term.":"No records are currently visible to this account in this workspace."}</span></div>
    :<><div className="records-table-wrap"><table className="records-table"><thead><tr>{config.columns.map(col=><th key={col.key}>{col.label}</th>)}</tr></thead><tbody>{pageRows.map((row,index)=><tr key={String(row.id||index)}>{config.columns.map(col=><td key={col.key}>{col.key==="status"||col.key==="risk_level"||col.key==="active"?<span className={`status-pill ${statusClass(row[col.key])}`}>{valueText(row[col.key])}</span>:<span className={col.key==="full_name"||col.key==="student_name"||col.key==="title"?"primary-cell":""}>{valueText(row[col.key])}</span>}</td>)}</tr>)}</tbody></table></div><div className="table-pagination"><span>Showing {pageIndex*pageSize+1}–{Math.min((pageIndex+1)*pageSize,visibleRows.length)} of {visibleRows.length}</span><div><button disabled={pageIndex===0} onClick={()=>setPageIndex(v=>Math.max(0,v-1))}><ChevronLeft size={15}/></button><b>{pageIndex+1} / {pageCount}</b><button disabled={pageIndex>=pageCount-1} onClick={()=>setPageIndex(v=>Math.min(pageCount-1,v+1))}><ChevronRight size={15}/></button></div></div></>}
    <div className="data-security-note"><ShieldCheck size={15}/><span>Data is fetched from Supabase. Row-level access is enforced by your database policies.</span></div>
  </section>;
}
