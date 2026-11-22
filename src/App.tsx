import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import type { Session, User } from "@supabase/supabase-js";
import {
  Activity, Bell, BookOpen, Bus, CalendarDays,
  CheckCircle2, ChevronDown, ClipboardCheck, CreditCard, GraduationCap,
  LayoutDashboard, Library, LifeBuoy, LogOut, Menu, MessageSquare, Search,
  Settings, ShieldCheck, Sparkles, Users, Wallet, X
} from "lucide-react";
import { supabase, supabaseConfigured } from "./lib/supabase";
import ModuleWorkspace from "./components/ModuleWorkspace";

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
  {label:"Staff",icon:Users,group:"People",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","HR_MANAGER"]},
  {label:"Fees & payments",icon:Wallet,group:"Finance",roles:["SUPER_ADMIN","SCHOOL_ADMIN","FINANCE_OFFICER","PARENT"]},
  {label:"Payroll",icon:CreditCard,group:"Finance",roles:["SUPER_ADMIN","SCHOOL_ADMIN","FINANCE_OFFICER","HR_MANAGER"]},
  {label:"Library",icon:Library,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","LIBRARIAN","TEACHER","CLASS_TEACHER","STUDENT"]},
  {label:"Transport",icon:Bus,group:"Operations",roles:["SUPER_ADMIN","SCHOOL_ADMIN","TRANSPORT_MANAGER","PARENT"]},
  {label:"Messages",icon:MessageSquare,group:"Communication",roles},
  {label:"School calendar",icon:CalendarDays,group:"Communication",roles},
  {label:"Reports & analytics",icon:Activity,group:"Insights",roles:["SUPER_ADMIN","SCHOOL_ADMIN","HEADTEACHER","DEPUTY_HEADTEACHER","FINANCE_OFFICER","HR_MANAGER"]},
  {label:"User access",icon:ShieldCheck,group:"Administration",roles:["SUPER_ADMIN","SCHOOL_ADMIN","ICT_ADMIN"]},
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
      const rawRole=list[0]?.primary_role ? String(list[0].primary_role).toUpperCase() : metadataRole;\n      const nextRole=rawRole==="ACCOUNTANT"?"FINANCE_OFFICER":rawRole;
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
  const pageModule=modules.find(m=>m.label===activePage);
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
        </>:<ModuleWorkspace page={activePage} schoolId={String(school?.id||"")} onBack={()=>setActivePage("Overview")}/>}
        <footer className="page-footer"><span>© {new Date().getFullYear()} GSMS · Global School Management System</span><span>Secure by design <ShieldCheck size={13}/></span></footer>
      </div>
    </main>
  </div>;
}
