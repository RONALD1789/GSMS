-- FIRST SCHOOL PROVISIONING TEMPLATE
-- Run once in Supabase SQL Editor as the project database administrator.
-- Replace the four values below before running. This is a trusted manual bootstrap
-- because a brand-new sign-up must not be able to create its own privileged tenant.

do $$
declare
  v_owner_uid uuid := 'REPLACE_WITH_SUPABASE_AUTH_USER_UUID'::uuid;
  v_school_name text := 'REPLACE_WITH_SCHOOL_NAME';
  v_school_code text := 'REPLACE_WITH_UNIQUE_SCHOOL_CODE';
  v_city text := 'Kampala';
  v_country text := 'Uganda';
  v_school_id text := 'school_' || replace(gen_random_uuid()::text, '-', '');
  v_branch_id text := 'branch_' || replace(gen_random_uuid()::text, '-', '');
begin
  if not exists (select 1 from auth.users where id = v_owner_uid) then
    raise exception 'No Supabase Auth user exists with the supplied UUID.';
  end if;

  if v_school_name like 'REPLACE_%' or v_school_code like 'REPLACE_%' then
    raise exception 'Replace the school name and school code placeholders before running.';
  end if;

  insert into public.gsms_schools (
    id,
    owner_id,
    supabase_owner_uid,
    name,
    code,
    country,
    city,
    plan,
    subscription_status,
    brand_color_hex,
    active_academic_year,
    active_term,
    enabled_modules
  ) values (
    v_school_id,
    v_owner_uid::text,
    v_owner_uid,
    v_school_name,
    v_school_code,
    v_country,
    v_city,
    'ENTERPRISE',
    'ACTIVE',
    '#1D4ED8',
    '2026/2027',
    'Term 1',
    array[]::text[]
  );

  insert into public.gsms_branches (
    id,
    school_id,
    owner_id,
    name,
    code,
    city,
    main_campus
  ) values (
    v_branch_id,
    v_school_id,
    v_owner_uid::text,
    'Main Campus',
    'MAIN',
    v_city,
    true
  );

  raise notice 'GSMS school provisioned. School ID: %, Main branch ID: %', v_school_id, v_branch_id;
end;
$$;

-- Verify the new school and main branch:
-- select s.id, s.name, s.code, b.id as main_branch_id
-- from public.gsms_schools s
-- join public.gsms_branches b on b.school_id = s.id and b.main_campus = true
-- where s.supabase_owner_uid = 'REPLACE_WITH_SUPABASE_AUTH_USER_UUID'::uuid;
