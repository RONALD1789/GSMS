import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import type { Session, User } from "@supabase/supabase-js";
import {
  Activity, AlertCircle, ArrowLeft, Bell, BookOpen, Bus, CalendarDays,
  CheckCircle2, ChevronDown, ChevronLeft, ChevronRight, ClipboardCheck, CreditCard, GraduationCap,
  LayoutDashboard, Library, LifeBuoy, LoaderCircle, LogOut, Menu, MessageSquare, RefreshCw, Search,
  Settings, ShieldCheck, Sparkles, Users, Wallet, X
} from "lucide-react";
import { supabase, supabaseConfigured } from "./lib/supabase";

type Assignment = Record<string, unknown> & {
  id?: string; school_id?: string; primary_role?: string; email?: string;
  active?: boolean; assigned_branch_ids?: string[]; linked_student_ids?: string[];
};
type School = Record<string, unknown> & { id?: string; name?: string; school_name?: string; logo_url?: string };
type ModuleItem = { label: string; icon: typeof LayoutDashboard; group: string; roles: string[] };

const roles = ["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","TEACHER","CLASS_TEACHER","PARENT","STUDENT","FINANCE_OFFICER","HR_MANAGER","LIBRARIAN","TRANSPORT_MANAGER","PROCUREMENT_OFFICER","STOREKEEPER","WELFARE_NURSE","ADMISSIONS_RECEPTION","SCHOOL_COUNSELLOR","EXAMINATION_OFFICER","ICT_ADMIN"];
const modules: ModuleItem[] = [
  {label:"Overview",icon:LayoutDashboard,group:"Workspace",roles},
  {label:"Students",icon:GraduationCap,group:"Academics",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","TEACHER","CLASS_TEACHER","PARENT","STUDENT"]},
  {label:"Admissions",icon:Users,group:"Academics",roles:["SUPER_ADMIN","SCHOOL_ADMIN","ADMISSIONS_RECEPTION"]},
  {label:"Classes & subjects",icon:BookOpen,group:"Academics",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","TEACHER","CLASS_TEACHER","EXAMINATION_OFFICER"]},
  {label:"Attendance",icon:ClipboardCheck,group:"Academics",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","TEACHER","CLASS_TEACHER","PARENT","STUDENT","EXAMINATION_OFFICER"]},
  {label:"Assessments & reports",icon:CheckCircle2,group:"Academics",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","TEACHER","CLASS_TEACHER","PARENT","STUDENT","EXAMINATION_OFFICER"]},
  {label:"Timetable",icon:CalendarDays,group:"Academics",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","TEACHER","CLASS_TEACHER","PARENT","STUDENT","EXAMINATION_OFFICER"]},
  {label:"Report templates",icon:BookOpen,group:"Academics",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","TEACHER","CLASS_TEACHER","EXAMINATION_OFFICER"]},
  {label:"Examinations",icon:ClipboardCheck,group:"Academics",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","EXAMINATION_OFFICER"]},
  {label:"Staff",icon:Users,group:"People",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","HR_MANAGER"]},
  {label:"Leave requests",icon:CalendarDays,group:"People",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","TEACHER","CLASS_TEACHER","FINANCE_OFFICER","HR_MANAGER","LIBRARIAN","TRANSPORT_MANAGER","PROCUREMENT_OFFICER","STOREKEEPER","WELFARE_NURSE","SCHOOL_COUNSELLOR","EXAMINATION_OFFICER","ICT_ADMIN"]},
  {label:"Fees & payments",icon:Wallet,group:"Finance",roles:["SUPER_ADMIN","SCHOOL_ADMIN","FINANCE_OFFICER","PARENT"]},
  {label:"Payroll",icon:CreditCard,group:"Finance",roles:["SUPER_ADMIN","SCHOOL_ADMIN","FINANCE_OFFICER","HR_MANAGER"]},
  {label:"Library",icon:Library,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","LIBRARIAN","TEACHER","CLASS_TEACHER","STUDENT"]},
  {label:"Transport",icon:Bus,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","TRANSPORT_MANAGER","PARENT"]},
  {label:"Procurement",icon:Wallet,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","FINANCE_OFFICER","PROCUREMENT_OFFICER","STOREKEEPER"]},
  {label:"Inventory & stores",icon:Library,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","PROCUREMENT_OFFICER","STOREKEEPER","ICT_ADMIN"]},
  {label:"Welfare & hostel",icon:Users,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","SCHOOL_COUNSELLOR","WELFARE_NURSE"]},
  {label:"Counselling & discipline",icon:ShieldCheck,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","SCHOOL_COUNSELLOR","WELFARE_NURSE"]},
  {label:"Front desk",icon:Users,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","ADMISSIONS_RECEPTION"]},
  {label:"ICT support",icon:Settings,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","ICT_ADMIN"]},
  {label:"Messages",icon:MessageSquare,group:"Communication",roles},
  {label:"School calendar",icon:CalendarDays,group:"Communication",roles},
  {label:"Reports & analytics",icon:Activity,group:"Insights",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","FINANCE_OFFICER","HR_MANAGER"]},
  {label:"User access",icon:ShieldCheck,group:"Administration",roles:["SUPER_ADMIN","SCHOOL_ADMIN","ICT_ADMIN"]},
  {label:"Branches & campuses",icon:GraduationCap,group:"Administration",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER"]},
  {label:"Settings",icon:Settings,group:"Administration",roles:["SUPER_ADMIN","SCHOOL_ADMIN"]},
];

function roleLabel(role: string) {
  return role.toLowerCase().replaceAll("_"," ").replace(/\b\w/g, c => c.toUpperCase());
}
function initials(value: string) {
  return value.split(/[\s@._-]+/).filter(Boolean).slice(0,2).map(v=>v[0].toUpperCase()).join("") || "U";
}
function displayName(user: User) {
  return String(user.user_metadata?.full_name || user.user_metadata?.name || user.email?.split("@")[0] || "User");
}

export default function App() {
  const [session,setSession] = useState<Session|null>(null);
  const [loading,setLoading] = useState(true);
  const [email,setEmail] = useState("");
  const [password,setPassword] = useState("");
  const [authError,setAuthError] = useState("");
  const [authBusy,setAuthBusy] = useState(false);
  const [assignments,setAssignments] = useState<Assignment[]>([]);
  const [school,setSchool] = useState<School|null>(null);
  const [activeRole,setActiveRole] = useState("");
  const [activePage,setActivePage] = useState("Overview");
  const [mobileOpen,setMobileOpen] = useState(false);
  const [search,setSearch] = useState("");
  const [profileOpen,setProfileOpen] = useState(false);
  const [dataMessage,setDataMessage] = useState("");

  useEffect(()=>{
    if(!supabase){setLoading(false);return;}
    supabase.auth.getSession().then(({data})=>{setSession(data.session);setLoading(false);});
    const {data:{subscription}} = supabase.auth.onAuthStateChange((_event,next)=>setSession(next));
    return ()=>subscription.unsubscribe();
  },[]);

  useEffect(()=>{
    let cancelled=false;
    async function loadAccess(){
      if(!supabase || !session?.user){setAssignments([]);setSchool(null);return;}
      const {data,error}=await supabase.from("gsms_user_access_assignments")
        .select("*").eq("auth_user_id",session.user.id).eq("active",true);
      if(cancelled)return;
      if(error){setDataMessage("Your account signed in, but its school access could not be loaded. Check that the GSMS access migration has been applied.");setAssignments([]);return;}
      const list=(data||[]) as Assignment[];
      setAssignments(list);
      const metadataRole=String(session.user.app_metadata?.role || session.user.user_metadata?.role || "").toUpperCase();
      const rawRole=list[0]?.primary_role ? String(list[0].primary_role).toUpperCase() : metadataRole;
      const nextRole=rawRole==="ACCOUNTANT"?"FINANCE_OFFICER":rawRole;
      setActiveRole(roles.includes(nextRole)?nextRole:(nextRole || ""));
      const schoolResult=await supabase.from("gsms_school_summaries").select("*");
      if(cancelled)return;
      if(schoolResult.error){
        setDataMessage("Your account signed in, but school context could not be loaded. Confirm the GSMS role-scoped migration and access grants.");
        setSchool(null);
        return;
      }
      const schoolRows=(schoolResult.data||[]) as School[];
      const assignedSchoolId=list[0]?.school_id;
      const currentSchool=assignedSchoolId
        ? schoolRows.find(row=>row.id===assignedSchoolId) || null
        : schoolRows[0] || null;
      setSchool(currentSchool);
      if(!nextRole && currentSchool) setActiveRole("SUPER_ADMIN");
    }
    loadAccess();
    return ()=>{cancelled=true;};
  },[session]);

  const allowedModules=useMemo(()=>modules.filter(m=>m.roles.includes(activeRole)),[activeRole]);
  const filteredModules=useMemo(()=>allowedModules.filter(m=>m.label.toLowerCase().includes(search.toLowerCase())),[allowedModules,search]);
  const groups=[...new Set(filteredModules.map(m=>m.group))];
  const schoolName=String(school?.school_name || school?.name || "Your school workspace");
  const userName=session?.user?displayName(session.user):"";

  async function signIn(e:FormEvent){
    e.preventDefault();setAuthError("");setAuthBusy(true);
    if(!supabase){setAuthError("Supabase is not configured. Add the web environment variables in Vercel.");setAuthBusy(false);return;}
    const {error}=await supabase.auth.signInWithPassword({email:email.trim(),password});
    if(error)setAuthError(error.message);
    setAuthBusy(false);
  }
  async function signOut(){await supabase?.auth.signOut();setProfileOpen(false);setActivePage("Overview");}

  if(loading) return <div className="loading-screen"><div className="brand-mark"><GraduationCap size={26}/></div><span>Loading GSMS workspace…</span></div>;
  if(!session) return <main className="auth-shell">
    <section className="auth-visual">
      <div className="auth-brand"><div className="brand-mark"><GraduationCap size={25}/></div><span>GSMS</span></div>
      <div className="visual-copy"><span className="eyebrow">GLOBAL SCHOOL MANAGEMENT SYSTEM</span><h1>One connected platform.<br/><em>Every school, moving forward.</em></h1><p>Bring learning, people, operations and school leadership together in one secure workspace.</p></div>
      <div className="visual-foot"><span><ShieldCheck size={16}/> Secure school operations</span><span>Built for modern education</span></div>
      <div className="orb orb-one"/><div className="orb orb-two"/>
    </section>
    <section className="auth-panel">
      <div className="auth-form-wrap">
        <div className="mobile-brand"><div className="brand-mark"><GraduationCap size={23}/></div><b>GSMS</b></div>
        <span className="eyebrow dark">WELCOME BACK</span><h2>Sign in to your portal</h2><p className="muted">Use the account provided by your school administrator.</p>
        {!supabaseConfigured&&<div className="notice warning">Supabase environment variables are not configured. Set <code>VITE_SUPABASE_URL</code> and <code>VITE_SUPABASE_PUBLISHABLE_KEY</code> in Vercel.</div>}
        <form onSubmit={signIn} className="login-form">
          <label>Email address<input type="email" autoComplete="username" required placeholder="you@school.com" value={email} onChange={e=>setEmail(e.target.value)}/></label>
          <label>Password<input type="password" autoComplete="current-password" required placeholder="Enter your password" value={password} onChange={e=>setPassword(e.target.value)}/></label>
          {authError&&<div className="form-error">{authError}</div>}
          <button className="primary-btn" disabled={authBusy||!supabaseConfigured}>{authBusy?"Signing in…":"Sign in securely"}<span>→</span></button>
        </form>
        <div className="auth-help"><ShieldCheck size={16}/> Your access is controlled by your school role and permissions.</div>
        <div className="auth-bottom">© {new Date().getFullYear()} GSMS · Global School Management System</div>
      </div>
    </section>
  </main>;

  const role=activeRole || "UNASSIGNED";
  return <div className="app-shell">
    {mobileOpen&&<button className="mobile-scrim" aria-label="Close menu" onClick={()=>setMobileOpen(false)}/>}
    <aside className={`sidebar ${mobileOpen?"sidebar-open":""}`}>
      <div className="side-brand"><div className="brand-mark"><GraduationCap size={23}/></div><div><strong>GSMS</strong><small>GLOBAL SCHOOL SYSTEM</small></div><button className="icon-btn close-side" onClick={()=>setMobileOpen(false)}><X size={19}/></button></div>
      <div className="school-switch"><div className="school-avatar">{initials(schoolName)}</div><div className="school-info"><b>{schoolName}</b><small>{assignments.length?roleLabel(role):"No school linked"}</small></div><ChevronDown size={15}/></div>
      <div className="side-search"><Search size={15}/><input placeholder="Find a module…" value={search} onChange={e=>setSearch(e.target.value)}/><kbd>⌘ K</kbd></div>
      <nav className="side-nav">
        {groups.map(group=><div className="nav-group" key={group}><span className="nav-heading">{group}</span>{filteredModules.filter(m=>m.group===group).map(item=><button key={item.label} className={`nav-link ${activePage===item.label?"active":""}`} onClick={()=>{setActivePage(item.label);setMobileOpen(false);}}><item.icon size={17}/><span>{item.label}</span>{item.label==="Messages"&&<i className="nav-dot"/>}</button>)}</div>)}
        {filteredModules.length===0&&<p className="empty-search">No matching modules.</p>}
      </nav>
      <div className="sidebar-bottom"><div className="help-card"><div className="help-icon"><LifeBuoy size={17}/></div><b>Need a hand?</b><p>Find help with your school workspace.</p><button onClick={()=>setActivePage("Help & support")}>Visit help centre <span>→</span></button></div><div className="side-version">GSMS Web · Supabase edition</div></div>
    </aside>
    <main className="main-area">
      <header className="topbar"><div className="top-left"><button className="icon-btn menu-btn" onClick={()=>setMobileOpen(true)}><Menu size={20}/></button><div className="breadcrumb">Workspace <span>/</span> <b>{activePage}</b></div></div><div className="top-actions"><div className="academic-pill"><CalendarDays size={15}/><span>Academic workspace</span></div><button className="icon-btn notification-btn" aria-label="Notifications"><Bell size={19}/><i/></button><div className="profile-wrap"><button className="profile-trigger" onClick={()=>setProfileOpen(!profileOpen)}><div className="avatar">{initials(userName)}</div><span><b>{userName}</b><small>{roleLabel(role)}</small></span><ChevronDown size={15}/></button>{profileOpen&&<div className="profile-menu"><div className="profile-menu-head"><b>{userName}</b><small>{session.user.email}</small></div><button onClick={()=>setActivePage("Settings")}><Settings size={15}/> Account settings</button><button onClick={signOut}><LogOut size={15}/> Sign out</button></div>}</div></div></header>
      <div className="page-content">
        {dataMessage&&<div className="notice warning"><span>{dataMessage}</span><button onClick={()=>setDataMessage("")}><X size={16}/></button></div>}
        {activePage==="Overview"?<><div className="welcome-row"><div><span className="eyebrow dark">YOUR SCHOOL AT A GLANCE</span><h1>Good day, {userName.split(" ")[0]} <span className="wave">✦</span></h1><p>Here’s your workspace overview and the tools available to your role.</p></div><div className="date-chip"><CalendarDays size={17}/>{new Date().toLocaleDateString(undefined,{weekday:"short",day:"numeric",month:"short",year:"numeric"})}</div></div>
          <section className="hero-card"><div className="hero-text"><span className="hero-tag"><Sparkles size={13}/> YOUR SCHOOL WORKSPACE</span><h2>{schoolName}</h2><p>Everything your school needs to organise learning, support people and make informed decisions.</p><div className="hero-meta"><span><ShieldCheck size={15}/> Role-secured access</span><span><Activity size={15}/> Live Supabase connection</span></div></div><div className="hero-art"><div className="art-ring ring-a"/><div className="art-ring ring-b"/><div className="art-icon"><GraduationCap size={48}/></div><div className="floating-card float-top"><Users size={16}/><span>People</span></div><div className="floating-card float-bottom"><BookOpen size={16}/><span>Learning</span></div></div></section>
          <div className="section-heading"><div><h3>Workspace summary</h3><p>Account and access information for this session.</p></div><span className="live-status"><i/> Connected</span></div>
          <div className="stat-grid"><article className="stat-card"><div className="stat-top"><span>Assigned school</span><span className="stat-icon blue"><GraduationCap size={18}/></span></div><strong className="stat-value stat-text">{schoolName}</strong><small>Current tenant workspace</small></article><article className="stat-card"><div className="stat-top"><span>Active role</span><span className="stat-icon purple"><ShieldCheck size={18}/></span></div><strong className="stat-value stat-text">{roleLabel(role)}</strong><small>Permission-scoped portal</small></article><article className="stat-card"><div className="stat-top"><span>Branch access</span><span className="stat-icon green"><Activity size={18}/></span></div><strong className="stat-value">{assignments[0]?.assigned_branch_ids?.length ?? "—"}</strong><small>Assigned branch scope</small></article><article className="stat-card"><div className="stat-top"><span>Linked learners</span><span className="stat-icon amber"><Users size={18}/></span></div><strong className="stat-value">{assignments[0]?.linked_student_ids?.length ?? "—"}</strong><small>Linked learner records</small></article></div>
          <div className="section-heading module-heading"><div><h3>Your modules</h3><p>Choose a workspace to continue.</p></div><span className="module-count">{allowedModules.length} available</span></div>
          <div className="module-grid">{allowedModules.filter(m=>m.label!=="Overview").slice(0,8).map(m=><button className="module-card" key={m.label} onClick={()=>setActivePage(m.label)}><span className="module-icon"><m.icon size={19}/></span><span><b>{m.label}</b><small>{m.group}</small></span><span className="module-arrow">↗</span></button>)}</div>
        </>:<ModuleWorkspace page={activePage} schoolId={String(school?.id||"")} role={role} onBack={()=>setActivePage("Overview")}/>}
        <footer className="page-footer"><span>© {new Date().getFullYear()} GSMS · Global School Management System</span><span>Secure by design <ShieldCheck size={13}/></span></footer>
      </div>
    </main>
  </div>;
}


type ModuleRow = Record<string, unknown>;
type ModuleColumn = {key:string;label:string};
type ModuleConfig = {table:string;columns:ModuleColumn[];filter?:{key:string;value:string}|{key:string;values:string[]};schoolKey?:string;description:string};
const moduleConfigs:Record<string,ModuleConfig>={
  "Students":{table:"gsms_students",columns:[{key:"admission_no",label:"Admission No."},{key:"full_name",label:"Learner"},{key:"class_name",label:"Class"},{key:"stream",label:"Stream"},{key:"attendance_rate",label:"Attendance"},{key:"average_score",label:"Average"}],description:"Learner directory, restricted by your school, branch, class and linked-student permissions."},
  "Admissions":{table:"gsms_admissions_student_summaries",columns:[{key:"admission_no",label:"Admission No."},{key:"full_name",label:"Learner"},{key:"class_name",label:"Class"},{key:"stream",label:"Stream"},{key:"parent_name",label:"Parent / Guardian"},{key:"parent_contact",label:"Contact"}],description:"Admission-safe learner directory for authorised admissions users."},
  "Classes & subjects":{table:"gsms_academic_records",columns:[{key:"title",label:"Record"},{key:"record_type",label:"Type"},{key:"class_name",label:"Class"},{key:"subject_name",label:"Subject"},{key:"teacher_name",label:"Teacher"},{key:"academic_year",label:"Year"},{key:"term",label:"Term"}],description:"Academic records available to your assigned class and branch scope."},
  "Attendance":{table:"gsms_attendance",columns:[{key:"target_name",label:"Learner / Staff"},{key:"target_type",label:"Type"},{key:"class_name",label:"Class"},{key:"date_iso",label:"Date"},{key:"status",label:"Status"}],description:"Attendance records visible within your assigned branch and class, or for your linked learners."},
  "Assessments & reports":{table:"gsms_report_cards",columns:[{key:"student_name",label:"Learner"},{key:"class_name",label:"Class"},{key:"template_name",label:"Report template"},{key:"average_score",label:"Average"},{key:"status",label:"Workflow status"},{key:"academic_year",label:"Year"},{key:"term",label:"Term"}],description:"Report cards follow the draft, submission, review and publication permissions in Supabase."},
  "Timetable":{table:"gsms_academic_records",columns:[{key:"title",label:"Lesson / Activity"},{key:"class_name",label:"Class"},{key:"subject_name",label:"Subject"},{key:"teacher_name",label:"Teacher"},{key:"schedule_slot",label:"Time"},{key:"academic_year",label:"Year"},{key:"term",label:"Term"}],filter:{key:"record_type",value:"TIMETABLE"},description:"Timetable entries for classes and branches you are authorised to access."},
  "Staff":{table:"gsms_staff",columns:[{key:"staff_no",label:"Staff No."},{key:"full_name",label:"Staff member"},{key:"role_title",label:"Position"},{key:"department",label:"Department"},{key:"leave_balance_days",label:"Leave balance"},{key:"on_leave",label:"On leave"}],description:"Staff records are restricted to leadership, HR and each staff member's permitted profile."},
  "Leave requests":{table:"gsms_leave_requests",columns:[{key:"staff_name",label:"Staff member"},{key:"role_title",label:"Role"},{key:"leave_type",label:"Leave type"},{key:"start_date",label:"Start"},{key:"end_date",label:"End"},{key:"days_count",label:"Days"},{key:"status",label:"Status"}],description:"Leave requests are scoped to your own staff record, branch and authorised HR workflow."},
  "Report templates":{table:"gsms_report_templates",columns:[{key:"name",label:"Template"},{key:"class_level",label:"Class level"},{key:"grading_scale",label:"Grading scale"},{key:"active",label:"Active"}],description:"Report templates available to academic leadership and examination roles."},
  "Examinations":{table:"gsms_academic_records",columns:[{key:"title",label:"Exam / Activity"},{key:"record_type",label:"Type"},{key:"class_name",label:"Class"},{key:"subject_name",label:"Subject"},{key:"schedule_slot",label:"Schedule"},{key:"term",label:"Term"}],filter:{key:"record_type",values:["EXAM","EXAM_TIMETABLE","INVIGILATION","MARK_MODERATION"]},description:"Examination timetables, invigilation and mark moderation within your assigned academic scope."},
  "Branches & campuses":{table:"gsms_branches",columns:[{key:"name",label:"Campus"},{key:"code",label:"Code"},{key:"city",label:"City"},{key:"main_campus",label:"Main campus"}],description:"Branch and campus records for your school tenant."},
  "Procurement":{table:"gsms_operations",columns:[{key:"title",label:"Request / Order"},{key:"module_type",label:"Type"},{key:"party_name",label:"Vendor / Requester"},{key:"status",label:"Status"},{key:"notes",label:"Notes"}],filter:{key:"module_type",values:["PROCUREMENT","REQUISITION","PURCHASE_ORDER","VENDOR","RFQ"]},description:"Procurement requests and purchase-order records, restricted by procurement and finance permissions."},
  "Inventory & stores":{table:"gsms_operations",columns:[{key:"title",label:"Stock / Asset"},{key:"module_type",label:"Type"},{key:"subtitle",label:"Details"},{key:"status",label:"Status"},{key:"notes",label:"Notes"}],filter:{key:"module_type",values:["STOCK","STOCK_ISSUE","STOCK_RECEIPT","INVENTORY","ASSET"]},description:"Inventory and stores operations, restricted by storekeeper and delegated permissions."},
  "Welfare & hostel":{table:"gsms_operations",columns:[{key:"title",label:"Case / Record"},{key:"module_type",label:"Type"},{key:"party_name",label:"Learner / Person"},{key:"status",label:"Status"},{key:"notes",label:"Notes"}],filter:{key:"module_type",values:["WELFARE","HOSTEL"]},description:"Welfare and hostel records visible only to authorised welfare roles and school leadership."},
  "Counselling & discipline":{table:"gsms_operations",columns:[{key:"title",label:"Case"},{key:"module_type",label:"Type"},{key:"party_name",label:"Learner / Person"},{key:"status",label:"Status"},{key:"notes",label:"Notes"}],filter:{key:"module_type",values:["DISCIPLINE","COUNSELLING","INTERVENTION","SAFEGUARDING"]},description:"Counselling and discipline records, restricted by the school's safeguarding permissions."},
  "Front desk":{table:"gsms_operations",columns:[{key:"title",label:"Visit / Request"},{key:"module_type",label:"Type"},{key:"party_name",label:"Visitor / Contact"},{key:"status",label:"Status"},{key:"notes",label:"Notes"}],filter:{key:"module_type",values:["ADMISSION","FRONT_DESK","EVENT"]},description:"Admissions reception and front-desk operations for your assigned campus."},
  "ICT support":{table:"gsms_operations",columns:[{key:"title",label:"Ticket / Asset"},{key:"module_type",label:"Type"},{key:"party_name",label:"Requester"},{key:"status",label:"Status"},{key:"notes",label:"Notes"}],filter:{key:"module_type",values:["ICT_TICKET","ICT_ASSET","SYSTEM_INCIDENT","ACCESS_REQUEST"]},description:"ICT tickets, assets and incidents available to authorised ICT administrators."},
  "Fees & payments":{table:"gsms_finance_entries",columns:[{key:"reference_no",label:"Reference"},{key:"title",label:"Description"},{key:"party_name",label:"Payer"},{key:"amount_usd",label:"Amount"},{key:"paid_usd",label:"Paid"},{key:"status",label:"Status"}],filter:{key:"entry_type",value:"INVOICE"},description:"Invoice and payment information is restricted to authorised finance users and linked parent accounts."},
  "Payroll":{table:"gsms_finance_entries",columns:[{key:"reference_no",label:"Reference"},{key:"title",label:"Payroll item"},{key:"party_name",label:"Staff member"},{key:"amount_usd",label:"Amount"},{key:"paid_usd",label:"Paid"},{key:"status",label:"Status"}],filter:{key:"entry_type",value:"PAYROLL"},description:"Payroll entries available to authorised finance and HR roles."},
  "Library":{table:"gsms_operations",columns:[{key:"title",label:"Item / Activity"},{key:"subtitle",label:"Details"},{key:"party_name",label:"Assigned to"},{key:"status",label:"Status"},{key:"notes",label:"Notes"}],filter:{key:"module_type",values:["LIBRARY","LIBRARY_ITEM","LIBRARY_LOAN","BOOK","LIBRARY_CATALOG"]},description:"Library operations visible under your assigned library permissions."},
  "Transport":{table:"gsms_operations",columns:[{key:"title",label:"Vehicle / Route"},{key:"module_type",label:"Type"},{key:"subtitle",label:"Details"},{key:"party_name",label:"Assigned to"},{key:"status",label:"Status"}],filter:{key:"module_type",values:["VEHICLE","ROUTE","TRIP"]},description:"Transport operations visible to the transport team and authorised school leadership."},
  "Messages":{table:"gsms_communications",columns:[{key:"title",label:"Message"},{key:"channel",label:"Channel"},{key:"audience",label:"Audience"},{key:"sender_name",label:"Sent by"},{key:"sender_role",label:"Role"},{key:"updated_at",label:"Date"}],description:"School communications, restricted by your role and the message audience."},
  "School calendar":{table:"gsms_academic_records",columns:[{key:"title",label:"Event"},{key:"class_name",label:"Class / Audience"},{key:"schedule_slot",label:"Date / Time"},{key:"teacher_name",label:"Coordinator"},{key:"term",label:"Term"}],filter:{key:"record_type",value:"EVENT"},description:"School events and calendar entries in your authorised academic scope."},
  "Reports & analytics":{table:"gsms_early_warning_alerts",columns:[{key:"category",label:"Category"},{key:"risk_level",label:"Risk"},{key:"target_name",label:"Learner / Target"},{key:"reason",label:"Reason"},{key:"assigned_role",label:"Assigned to"},{key:"status",label:"Status"}],description:"Early-warning alerts and follow-up items available to authorised school leadership."},
  "User access":{table:"gsms_user_access_assignments",columns:[{key:"full_name",label:"User"},{key:"email",label:"Email"},{key:"primary_role",label:"Portal role"},{key:"assigned_classes",label:"Classes"},{key:"active",label:"Active"}],description:"Role assignments available to school managers and authorised ICT administrators."},
  "Settings":{table:"gsms_school_summaries",schoolKey:"id",columns:[{key:"name",label:"School name"},{key:"code",label:"School code"},{key:"country",label:"Country"},{key:"city",label:"City"},{key:"active_academic_year",label:"Academic year"},{key:"active_term",label:"Term"},{key:"plan",label:"Plan"}],description:"School profile. Sensitive owner and subscription information is masked for non-owners."}
};
const parentStudents:ModuleConfig={table:"gsms_parent_student_summaries",columns:[{key:"admission_no",label:"Admission No."},{key:"full_name",label:"Learner"},{key:"class_name",label:"Class"},{key:"stream",label:"Stream"},{key:"attendance_rate",label:"Attendance"},{key:"average_score",label:"Average"}],description:"Only the learners linked to your parent account are shown."};
function ModuleWorkspace({page,schoolId,role,onBack}:{page:string;schoolId:string;role:string;onBack:()=>void}){
  const base=moduleConfigs[page];
  const config=page==="Students"&&["PARENT","STUDENT"].includes(role)?parentStudents:base;
  const [rows,setRows]=useState<ModuleRow[]>([]);const [loading,setLoading]=useState(true);const [error,setError]=useState("");const [actionError,setActionError]=useState("");const [updatingId,setUpdatingId]=useState("");const [search,setSearch]=useState("");const [refresh,setRefresh]=useState(0);const [pageNo,setPageNo]=useState(0);const pageSize=12;
  useEffect(()=>{let live=true;async function load(){if(!supabase||!config||!schoolId){setRows([]);setLoading(false);return;}setLoading(true);setError("");let query=supabase.from(config.table).select(["id",...config.columns.map(col=>col.key)].join(",")).eq(config.schoolKey||"school_id",schoolId).limit(100);if(config.filter){if("values" in config.filter)query=query.in(config.filter.key,config.filter.values);else query=query.eq(config.filter.key,config.filter.value);}const result=await query;if(!live)return;if(result.error){setError(result.error.message);setRows([]);}else setRows((result.data||[]) as unknown as ModuleRow[]);setLoading(false);}load();return()=>{live=false;};},[page,schoolId,refresh,role]);
  const filtered=useMemo(()=>{const q=search.trim().toLowerCase();return q?rows.filter(row=>Object.values(row).some(value=>String(value??"").toLowerCase().includes(q))):rows;},[rows,search]);const pages=Math.max(1,Math.ceil(filtered.length/pageSize));const shown=filtered.slice(pageNo*pageSize,(pageNo+1)*pageSize);
  useEffect(()=>setPageNo(0),[search,page]);
  async function changeReportStatus(row:ModuleRow,nextStatus:string){
    if(!supabase||!row.id)return;
    setUpdatingId(String(row.id));setActionError("");
    const result=await supabase.from("gsms_report_cards").update({status:nextStatus}).eq("id",String(row.id)).eq("school_id",schoolId);
    if(result.error)setActionError(result.error.message);else setRefresh(v=>v+1);
    setUpdatingId("");
  }
  function reportAction(row:ModuleRow){
    if(page!=="Assessments & reports")return null;
    const status=String(row.status||"").toUpperCase();
    const leadership=["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER"].includes(role);
    if(["TEACHER","CLASS_TEACHER"].includes(role)&&["DRAFT","RETURNED"].includes(status))return <button className="table-action" disabled={updatingId===String(row.id)} onClick={()=>changeReportStatus(row,"SUBMITTED")}>Submit</button>;
    if(leadership&&status==="SUBMITTED")return <button className="table-action approve" disabled={updatingId===String(row.id)} onClick={()=>changeReportStatus(row,"APPROVED")}>Approve</button>;
    if(leadership&&status==="APPROVED")return <button className="table-action" disabled={updatingId===String(row.id)} onClick={()=>changeReportStatus(row,"PUBLISHED")}>Publish</button>;
    return <span className="no-action">—</span>;
  }
  if(!config)return <section className="module-page"><div className="module-page-icon"><ShieldCheck size={25}/></div><span className="eyebrow dark">WORKSPACE</span><h1>{page}</h1><p className="module-intro">This area is being prepared for the next GSMS feature release.</p><button className="secondary-btn" onClick={onBack}>← Back to overview</button></section>;
  return <section className="data-workspace"><div className="data-heading"><div><button className="back-link" onClick={onBack}><ArrowLeft size={14}/> Overview</button><span className="eyebrow dark">{config.table.replace("gsms_","").replaceAll("_"," ").toUpperCase()}</span><h1>{page}</h1><p>{config.description}</p></div><button className="refresh-btn" onClick={()=>setRefresh(v=>v+1)} disabled={loading}><RefreshCw size={15}/> Refresh</button></div>
    {actionError&&<div className="notice warning">{actionError}<button onClick={()=>setActionError("")}><X size={15}/></button></div>}<div className="data-toolbar"><div className="data-search"><Search size={16}/><input value={search} onChange={e=>setSearch(e.target.value)} placeholder={`Search ${page.toLowerCase()}…`}/></div><span className="row-count">{filtered.length} {filtered.length===1?"record":"records"}</span></div>
    {loading?<div className="data-state"><LoaderCircle className="spin" size={24}/><span>Loading records securely…</span></div>:error?<div className="data-state error-state"><AlertCircle size={24}/><b>Unable to load this module</b><span>{error}</span><small>Confirm that the Supabase schema and role-scoped RLS migration are applied and that this role can read these records.</small><button className="secondary-btn" onClick={()=>setRefresh(v=>v+1)}>Try again</button></div>:shown.length===0?<div className="data-state"><div className="empty-data-icon"><CheckCircle2 size={23}/></div><b>{search?"No matching records":"No records to display"}</b><span>{search?"Try another search term.":"No records are currently visible to this account in this workspace."}</span></div>:<><div className="records-table-wrap"><table className="records-table"><thead><tr>{config.columns.map(col=><th key={col.key}>{col.label}</th>)}{page==="Assessments & reports"&&<th>Action</th>}</tr></thead><tbody>{shown.map((row,i)=><tr key={String(row.id||i)}>{config.columns.map(col=><td key={col.key}>{["status","risk_level","active"].includes(col.key)?<span className={`status-pill ${["approved","published","active","present","paid"].includes(String(row[col.key]).toLowerCase())?"success":["pending","draft","submitted"].includes(String(row[col.key]).toLowerCase())?"pending":"neutral"}`}>{String(row[col.key]??"—")}</span>:<span className={["full_name","student_name","title"].includes(col.key)?"primary-cell":""}>{String(row[col.key]??"—")}</span>}</td>)}{page==="Assessments & reports"&&<td>{reportAction(row)}</td>}</tr>)}</tbody></table></div><div className="table-pagination"><span>Showing {pageNo*pageSize+1}–{Math.min((pageNo+1)*pageSize,filtered.length)} of {filtered.length}</span><div><button disabled={pageNo===0} onClick={()=>setPageNo(v=>Math.max(0,v-1))}><ChevronLeft size={15}/></button><b>{pageNo+1} / {pages}</b><button disabled={pageNo>=pages-1} onClick={()=>setPageNo(v=>Math.min(pages-1,v+1))}><ChevronRight size={15}/></button></div></div></>}
    <div className="data-security-note"><ShieldCheck size={15}/><span>Supabase Row Level Security enforces access to every returned record.</span></div></section>;
}
