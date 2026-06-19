-- ============================================================
-- CANTANHEDE CYCLING HUB - Schema HARDENED (Segurança Corrigida)
-- ============================================================
-- INSTRUÇÕES:
-- 1. Faz backup do teu banco antes de executar
-- 2. Cola este ficheiro COMPLETO no SQL Editor do Supabase
-- 3. Dashboard → SQL Editor → New Query → Cole → Run
-- ============================================================
-- CHANGELOG vs schema original:
--   ✅ Policies role-based (admin/editor/viewer) em vez de USING(true)
--   ✅ Funções auxiliares is_team_member(), get_user_role(), is_admin_or_editor()
--   ✅ CHECK constraints em campos numéricos
--   ✅ Tabela audit_log com trigger automático
--   ✅ Storage buckets com policies por auth.uid()
--   ✅ Coluna created_by em tabelas de conteúdo
-- ============================================================

-- =====================
-- 0. FUNÇÕES AUXILIARES
-- =====================

-- Verifica se o utilizador autenticado tem perfil (é membro da equipa)
CREATE OR REPLACE FUNCTION public.is_team_member()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM profiles WHERE id = (select auth.uid())
  );
$$;

-- Retorna o role do utilizador autenticado
CREATE OR REPLACE FUNCTION public.get_user_role()
RETURNS TEXT
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT COALESCE(
    (SELECT role FROM profiles WHERE id = (select auth.uid())),
    'viewer'
  );
$$;

-- Verifica se é admin
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT (select public.get_user_role()) = 'admin';
$$;

-- Verifica se é admin ou editor
CREATE OR REPLACE FUNCTION public.is_admin_or_editor()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT (select public.get_user_role()) IN ('admin', 'editor');
$$;

-- =====================
-- 1. TABELA: profiles (sem alteração de estrutura, apenas constraints)
-- =====================
-- A tabela profiles já existe. Vamos apenas adicionar constraints.
ALTER TABLE profiles
  ADD CONSTRAINT chk_profiles_role CHECK (role IN ('admin', 'editor', 'viewer'));

-- =====================
-- 2. TABELA: athletes — adicionar created_by e constraints
-- =====================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'athletes' AND column_name = 'created_by') THEN
    ALTER TABLE athletes ADD COLUMN created_by UUID REFERENCES auth.users(id);
  END IF;
END $$;

ALTER TABLE athletes
  DROP CONSTRAINT IF EXISTS chk_athletes_weight,
  DROP CONSTRAINT IF EXISTS chk_athletes_height,
  DROP CONSTRAINT IF EXISTS chk_athletes_ftp,
  DROP CONSTRAINT IF EXISTS chk_athletes_vo2_max,
  DROP CONSTRAINT IF EXISTS chk_athletes_watts_per_kg,
  DROP CONSTRAINT IF EXISTS chk_athletes_hr_max,
  DROP CONSTRAINT IF EXISTS chk_athletes_power_5s,
  DROP CONSTRAINT IF EXISTS chk_athletes_power_1m,
  DROP CONSTRAINT IF EXISTS chk_athletes_power_5m,
  DROP CONSTRAINT IF EXISTS chk_athletes_power_20m,
  DROP CONSTRAINT IF EXISTS chk_athletes_status,
  DROP CONSTRAINT IF EXISTS chk_athletes_name_length;

ALTER TABLE athletes
  ADD CONSTRAINT chk_athletes_weight CHECK (weight IS NULL OR (weight > 0 AND weight < 300)),
  ADD CONSTRAINT chk_athletes_height CHECK (height IS NULL OR (height > 0 AND height < 3)),
  ADD CONSTRAINT chk_athletes_ftp CHECK (ftp IS NULL OR (ftp > 0 AND ftp < 2000)),
  ADD CONSTRAINT chk_athletes_vo2_max CHECK (vo2_max IS NULL OR (vo2_max > 0 AND vo2_max < 120)),
  ADD CONSTRAINT chk_athletes_watts_per_kg CHECK (watts_per_kg IS NULL OR (watts_per_kg > 0 AND watts_per_kg < 15)),
  ADD CONSTRAINT chk_athletes_hr_max CHECK (hr_max IS NULL OR (hr_max > 30 AND hr_max < 250)),
  ADD CONSTRAINT chk_athletes_power_5s CHECK (power_5s IS NULL OR (power_5s > 0 AND power_5s < 3000)),
  ADD CONSTRAINT chk_athletes_power_1m CHECK (power_1m IS NULL OR (power_1m > 0 AND power_1m < 2000)),
  ADD CONSTRAINT chk_athletes_power_5m CHECK (power_5m IS NULL OR (power_5m > 0 AND power_5m < 1500)),
  ADD CONSTRAINT chk_athletes_power_20m CHECK (power_20m IS NULL OR (power_20m > 0 AND power_20m < 1000)),
  ADD CONSTRAINT chk_athletes_status CHECK (status IN ('active', 'developing', 'injured', 'inactive')),
  ADD CONSTRAINT chk_athletes_name_length CHECK (char_length(name) BETWEEN 2 AND 200);

-- =====================
-- 3. TABELA: races — adicionar created_by e constraints
-- =====================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'races' AND column_name = 'created_by') THEN
    ALTER TABLE races ADD COLUMN created_by UUID REFERENCES auth.users(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'races' AND column_name = 'link') THEN
    ALTER TABLE races ADD COLUMN link TEXT;
  END IF;
END $$;

ALTER TABLE races
  DROP CONSTRAINT IF EXISTS chk_races_title_length,
  DROP CONSTRAINT IF EXISTS chk_races_status;

ALTER TABLE races
  ADD CONSTRAINT chk_races_title_length CHECK (char_length(title) BETWEEN 2 AND 500),
  ADD CONSTRAINT chk_races_status CHECK (status IN ('Agendada', 'A decorrer', 'Concluída', 'Cancelada', 'Aberto', 'Pendente', 'Planeado'));

-- =====================
-- 4. TABELA: race_results — constraints
-- =====================
ALTER TABLE race_results
  DROP CONSTRAINT IF EXISTS chk_race_results_position;

ALTER TABLE race_results
  ADD CONSTRAINT chk_race_results_position CHECK (position IS NULL OR (position > 0 AND position < 10000));

-- =====================
-- 5. TABELA: social_posts — adicionar created_by e constraints
-- =====================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'social_posts' AND column_name = 'created_by') THEN
    ALTER TABLE social_posts ADD COLUMN created_by UUID REFERENCES auth.users(id);
  END IF;
END $$;

ALTER TABLE social_posts
  DROP CONSTRAINT IF EXISTS chk_social_posts_title_length,
  DROP CONSTRAINT IF EXISTS chk_social_posts_content_length;

ALTER TABLE social_posts
  ADD CONSTRAINT chk_social_posts_title_length CHECK (char_length(title) BETWEEN 1 AND 500),
  ADD CONSTRAINT chk_social_posts_content_length CHECK (char_length(content) BETWEEN 1 AND 50000);

-- =====================
-- 6. TABELA: sponsors — constraints
-- =====================
ALTER TABLE sponsors
  DROP CONSTRAINT IF EXISTS chk_sponsors_level,
  DROP CONSTRAINT IF EXISTS chk_sponsors_name_length;

ALTER TABLE sponsors
  ADD CONSTRAINT chk_sponsors_level CHECK (level IN ('Gold', 'Silver', 'Bronze', 'Partner')),
  ADD CONSTRAINT chk_sponsors_name_length CHECK (char_length(name) BETWEEN 1 AND 300);

-- =====================
-- 7. TABELA: vault_texts — adicionar created_by e constraints
-- =====================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vault_texts' AND column_name = 'created_by') THEN
    ALTER TABLE vault_texts ADD COLUMN created_by UUID REFERENCES auth.users(id);
  END IF;
END $$;

ALTER TABLE vault_texts
  DROP CONSTRAINT IF EXISTS chk_vault_texts_title_length,
  DROP CONSTRAINT IF EXISTS chk_vault_texts_content_length;

ALTER TABLE vault_texts
  ADD CONSTRAINT chk_vault_texts_title_length CHECK (char_length(title) BETWEEN 1 AND 500),
  ADD CONSTRAINT chk_vault_texts_content_length CHECK (char_length(content) BETWEEN 1 AND 100000);

-- =====================
-- 8. TABELA: app_updates — constraints
-- =====================
ALTER TABLE app_updates
  DROP CONSTRAINT IF EXISTS chk_app_updates_version_code;

ALTER TABLE app_updates
  ADD CONSTRAINT chk_app_updates_version_code CHECK (version_code > 0 AND version_code < 100000);


-- ============================================================
-- 9. TABELA: audit_log (NOVA)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_name TEXT NOT NULL,
    operation TEXT NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    record_id TEXT,
    user_id UUID,
    user_role TEXT,
    old_data JSONB,
    new_data JSONB,
    ip_address TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Índices para consultas de auditoria
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_table_name ON audit_log(table_name);

-- RLS no audit_log: apenas admins podem ler, ninguém pode apagar
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Admins can read audit_log" ON audit_log;
DROP POLICY IF EXISTS "System can insert audit_log" ON audit_log;

CREATE POLICY "Admins can read audit_log" ON audit_log
  FOR SELECT TO authenticated
  USING ((select public.is_admin()));

-- Permitir insert via trigger (service role)
CREATE POLICY "System can insert audit_log" ON audit_log
  FOR INSERT
  WITH CHECK (true);

-- =====================
-- 10. TRIGGER FUNCTION DE AUDITORIA
-- =====================
CREATE OR REPLACE FUNCTION public.audit_trigger_func()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO audit_log (table_name, operation, record_id, user_id, user_role, new_data)
    VALUES (TG_TABLE_NAME, TG_OP, NEW.id::TEXT, auth.uid(), public.get_user_role(), to_jsonb(NEW));
    RETURN NEW;
  ELSIF TG_OP = 'UPDATE' THEN
    INSERT INTO audit_log (table_name, operation, record_id, user_id, user_role, old_data, new_data)
    VALUES (TG_TABLE_NAME, TG_OP, NEW.id::TEXT, auth.uid(), public.get_user_role(), to_jsonb(OLD), to_jsonb(NEW));
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    INSERT INTO audit_log (table_name, operation, record_id, user_id, user_role, old_data)
    VALUES (TG_TABLE_NAME, TG_OP, OLD.id::TEXT, auth.uid(), public.get_user_role(), to_jsonb(OLD));
    RETURN OLD;
  END IF;
END;
$$;

-- Aplicar trigger a todas as tabelas de dados
DROP TRIGGER IF EXISTS audit_trigger ON profiles;
DROP TRIGGER IF EXISTS audit_trigger ON athletes;
DROP TRIGGER IF EXISTS audit_trigger ON races;
DROP TRIGGER IF EXISTS audit_trigger ON race_results;
DROP TRIGGER IF EXISTS audit_trigger ON social_posts;
DROP TRIGGER IF EXISTS audit_trigger ON sponsors;
DROP TRIGGER IF EXISTS audit_trigger ON vault_texts;
DROP TRIGGER IF EXISTS audit_trigger ON app_updates;

CREATE TRIGGER audit_trigger AFTER INSERT OR UPDATE OR DELETE ON profiles
  FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();
CREATE TRIGGER audit_trigger AFTER INSERT OR UPDATE OR DELETE ON athletes
  FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();
CREATE TRIGGER audit_trigger AFTER INSERT OR UPDATE OR DELETE ON races
  FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();
CREATE TRIGGER audit_trigger AFTER INSERT OR UPDATE OR DELETE ON race_results
  FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();
CREATE TRIGGER audit_trigger AFTER INSERT OR UPDATE OR DELETE ON social_posts
  FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();
CREATE TRIGGER audit_trigger AFTER INSERT OR UPDATE OR DELETE ON sponsors
  FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();
CREATE TRIGGER audit_trigger AFTER INSERT OR UPDATE OR DELETE ON vault_texts
  FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();
CREATE TRIGGER audit_trigger AFTER INSERT OR UPDATE OR DELETE ON app_updates
  FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();


-- ============================================================
-- 11. NOVAS POLÍTICAS RLS (ROLE-BASED)
-- ============================================================

-- Limpar TODAS as políticas existentes primeiro
-- profiles
DROP POLICY IF EXISTS "Authenticated users can read profiles" ON profiles;
DROP POLICY IF EXISTS "Authenticated users can insert profiles" ON profiles;
DROP POLICY IF EXISTS "Authenticated users can update profiles" ON profiles;
DROP POLICY IF EXISTS "Authenticated users can delete profiles" ON profiles;
-- athletes
DROP POLICY IF EXISTS "Authenticated users can read athletes" ON athletes;
DROP POLICY IF EXISTS "Authenticated users can insert athletes" ON athletes;
DROP POLICY IF EXISTS "Authenticated users can update athletes" ON athletes;
DROP POLICY IF EXISTS "Authenticated users can delete athletes" ON athletes;
-- races
DROP POLICY IF EXISTS "Authenticated users can read races" ON races;
DROP POLICY IF EXISTS "Authenticated users can insert races" ON races;
DROP POLICY IF EXISTS "Authenticated users can update races" ON races;
DROP POLICY IF EXISTS "Authenticated users can delete races" ON races;
-- race_results
DROP POLICY IF EXISTS "Authenticated users can read race_results" ON race_results;
DROP POLICY IF EXISTS "Authenticated users can insert race_results" ON race_results;
DROP POLICY IF EXISTS "Authenticated users can update race_results" ON race_results;
DROP POLICY IF EXISTS "Authenticated users can delete race_results" ON race_results;
-- social_posts
DROP POLICY IF EXISTS "Authenticated users can read social_posts" ON social_posts;
DROP POLICY IF EXISTS "Authenticated users can insert social_posts" ON social_posts;
DROP POLICY IF EXISTS "Authenticated users can update social_posts" ON social_posts;
DROP POLICY IF EXISTS "Authenticated users can delete social_posts" ON social_posts;
-- sponsors
DROP POLICY IF EXISTS "Authenticated users can read sponsors" ON sponsors;
DROP POLICY IF EXISTS "Authenticated users can insert sponsors" ON sponsors;
DROP POLICY IF EXISTS "Authenticated users can update sponsors" ON sponsors;
DROP POLICY IF EXISTS "Authenticated users can delete sponsors" ON sponsors;
-- vault_texts
DROP POLICY IF EXISTS "Authenticated users can read vault_texts" ON vault_texts;
DROP POLICY IF EXISTS "Authenticated users can insert vault_texts" ON vault_texts;
DROP POLICY IF EXISTS "Authenticated users can update vault_texts" ON vault_texts;
DROP POLICY IF EXISTS "Authenticated users can delete vault_texts" ON vault_texts;
-- app_updates
DROP POLICY IF EXISTS "Anyone can read app_updates" ON app_updates;
DROP POLICY IF EXISTS "Authenticated users can insert app_updates" ON app_updates;
DROP POLICY IF EXISTS "Authenticated users can update app_updates" ON app_updates;
DROP POLICY IF EXISTS "Authenticated users can delete app_updates" ON app_updates;

-- ═══════════════════════════════
-- PROFILES: Cada user edita o seu, membros podem ler todos
-- ═══════════════════════════════
CREATE POLICY "Team members can read all profiles" ON profiles
  FOR SELECT TO authenticated
  USING ((select public.is_team_member()));

CREATE POLICY "Users can insert own profile" ON profiles
  FOR INSERT TO authenticated
  WITH CHECK ((select auth.uid()) = id);

CREATE POLICY "Users can update own profile" ON profiles
  FOR UPDATE TO authenticated
  USING ((select auth.uid()) = id)
  WITH CHECK ((select auth.uid()) = id);

CREATE POLICY "Only admins can delete profiles" ON profiles
  FOR DELETE TO authenticated
  USING ((select public.is_admin()));

-- ═══════════════════════════════
-- ATHLETES: Membros leem, editores+admins escrevem, admins apagam
-- ═══════════════════════════════
CREATE POLICY "Team members can read athletes" ON athletes
  FOR SELECT TO authenticated
  USING ((select public.is_team_member()));

CREATE POLICY "Editors and admins can insert athletes" ON athletes
  FOR INSERT TO authenticated
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Editors and admins can update athletes" ON athletes
  FOR UPDATE TO authenticated
  USING ((select public.is_admin_or_editor()))
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Only admins can delete athletes" ON athletes
  FOR DELETE TO authenticated
  USING ((select public.is_admin()));

-- ═══════════════════════════════
-- RACES: Membros leem, editores+admins escrevem, admins apagam
-- ═══════════════════════════════
CREATE POLICY "Team members can read races" ON races
  FOR SELECT TO authenticated
  USING ((select public.is_team_member()));

CREATE POLICY "Editors and admins can insert races" ON races
  FOR INSERT TO authenticated
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Editors and admins can update races" ON races
  FOR UPDATE TO authenticated
  USING ((select public.is_admin_or_editor()))
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Only admins can delete races" ON races
  FOR DELETE TO authenticated
  USING ((select public.is_admin()));

-- ═══════════════════════════════
-- RACE_RESULTS: Membros leem, editores+admins escrevem, admins apagam
-- ═══════════════════════════════
CREATE POLICY "Team members can read race_results" ON race_results
  FOR SELECT TO authenticated
  USING ((select public.is_team_member()));

CREATE POLICY "Editors and admins can insert race_results" ON race_results
  FOR INSERT TO authenticated
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Editors and admins can update race_results" ON race_results
  FOR UPDATE TO authenticated
  USING ((select public.is_admin_or_editor()))
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Only admins can delete race_results" ON race_results
  FOR DELETE TO authenticated
  USING ((select public.is_admin()));

-- ═══════════════════════════════
-- SOCIAL_POSTS: Membros leem, editores+admins escrevem, admins apagam
-- ═══════════════════════════════
CREATE POLICY "Team members can read social_posts" ON social_posts
  FOR SELECT TO authenticated
  USING ((select public.is_team_member()));

CREATE POLICY "Editors and admins can insert social_posts" ON social_posts
  FOR INSERT TO authenticated
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Editors and admins can update social_posts" ON social_posts
  FOR UPDATE TO authenticated
  USING ((select public.is_admin_or_editor()))
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Only admins can delete social_posts" ON social_posts
  FOR DELETE TO authenticated
  USING ((select public.is_admin()));

-- ═══════════════════════════════
-- SPONSORS: Membros leem, editores+admins escrevem, admins apagam
-- ═══════════════════════════════
CREATE POLICY "Team members can read sponsors" ON sponsors
  FOR SELECT TO authenticated
  USING ((select public.is_team_member()));

CREATE POLICY "Editors and admins can insert sponsors" ON sponsors
  FOR INSERT TO authenticated
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Editors and admins can update sponsors" ON sponsors
  FOR UPDATE TO authenticated
  USING ((select public.is_admin_or_editor()))
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Only admins can delete sponsors" ON sponsors
  FOR DELETE TO authenticated
  USING ((select public.is_admin()));

-- ═══════════════════════════════
-- VAULT_TEXTS: Membros leem, editores+admins escrevem, admins apagam
-- ═══════════════════════════════
CREATE POLICY "Team members can read vault_texts" ON vault_texts
  FOR SELECT TO authenticated
  USING ((select public.is_team_member()));

CREATE POLICY "Editors and admins can insert vault_texts" ON vault_texts
  FOR INSERT TO authenticated
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Editors and admins can update vault_texts" ON vault_texts
  FOR UPDATE TO authenticated
  USING ((select public.is_admin_or_editor()))
  WITH CHECK ((select public.is_admin_or_editor()));

CREATE POLICY "Only admins can delete vault_texts" ON vault_texts
  FOR DELETE TO authenticated
  USING ((select public.is_admin()));

-- ═══════════════════════════════
-- APP_UPDATES: Público para leitura (app verifica updates sem login),
--              apenas admin pode inserir/atualizar/apagar
-- ═══════════════════════════════
CREATE POLICY "Anyone can read app_updates" ON app_updates
  FOR SELECT
  USING (true);

CREATE POLICY "Only admins can insert app_updates" ON app_updates
  FOR INSERT TO authenticated
  WITH CHECK ((select public.is_admin()));

CREATE POLICY "Only admins can update app_updates" ON app_updates
  FOR UPDATE TO authenticated
  USING ((select public.is_admin()))
  WITH CHECK ((select public.is_admin()));

CREATE POLICY "Only admins can delete app_updates" ON app_updates
  FOR DELETE TO authenticated
  USING ((select public.is_admin()));


-- ============================================================
-- 12. STORAGE POLICIES (CORRIGIDAS)
-- ============================================================

-- Limpar policies de storage existentes
DROP POLICY IF EXISTS "Public read access for avatars" ON storage.objects;
DROP POLICY IF EXISTS "Public read access for app-releases" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can upload avatars" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can upload app-releases" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can update avatars" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can update app-releases" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can delete avatars" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can delete app-releases" ON storage.objects;

-- Novas policies hardened
DROP POLICY IF EXISTS "Team members can read avatars" ON storage.objects;
DROP POLICY IF EXISTS "Users can upload own avatar" ON storage.objects;
DROP POLICY IF EXISTS "Users can update own avatar" ON storage.objects;
DROP POLICY IF EXISTS "Users can delete own avatar" ON storage.objects;
DROP POLICY IF EXISTS "Anyone can download app releases" ON storage.objects;
DROP POLICY IF EXISTS "Only admins can upload app releases" ON storage.objects;
DROP POLICY IF EXISTS "Only admins can update app releases" ON storage.objects;
DROP POLICY IF EXISTS "Only admins can delete app releases" ON storage.objects;

-- AVATARS: membros podem ler todos, cada user só faz upload/update do seu
CREATE POLICY "Team members can read avatars" ON storage.objects
  FOR SELECT TO authenticated
  USING (bucket_id = 'avatars' AND (select public.is_team_member()));

CREATE POLICY "Users can upload own avatar" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (
    bucket_id = 'avatars'
    AND (storage.foldername(name))[1] IS NOT DISTINCT FROM ''
    AND name LIKE (select auth.uid())::text || '%'
  );

CREATE POLICY "Users can update own avatar" ON storage.objects
  FOR UPDATE TO authenticated
  USING (
    bucket_id = 'avatars'
    AND name LIKE (select auth.uid())::text || '%'
  )
  WITH CHECK (
    bucket_id = 'avatars'
    AND name LIKE (select auth.uid())::text || '%'
  );

CREATE POLICY "Users can delete own avatar" ON storage.objects
  FOR DELETE TO authenticated
  USING (
    bucket_id = 'avatars'
    AND name LIKE (select auth.uid())::text || '%'
  );

-- APP-RELEASES: público para download (necessário para auto-update),
-- apenas admin pode fazer upload
CREATE POLICY "Anyone can download app releases" ON storage.objects
  FOR SELECT
  USING (bucket_id = 'app-releases');

CREATE POLICY "Only admins can upload app releases" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (
    bucket_id = 'app-releases'
    AND (select public.is_admin())
  );

CREATE POLICY "Only admins can update app releases" ON storage.objects
  FOR UPDATE TO authenticated
  USING (bucket_id = 'app-releases' AND (select public.is_admin()))
  WITH CHECK (bucket_id = 'app-releases' AND (select public.is_admin()));

CREATE POLICY "Only admins can delete app releases" ON storage.objects
  FOR DELETE TO authenticated
  USING (bucket_id = 'app-releases' AND (select public.is_admin()));

-- Tornar bucket avatars privado (manter app-releases público para auto-update download)
UPDATE storage.buckets SET public = false WHERE id = 'avatars';
-- app-releases mantém public = true porque o download de APK precisa funcionar
-- sem autenticação (o UpdateManager pode verificar antes do login)


-- ============================================================
-- 13. CONFIGURAR PRIMEIRO ADMIN
-- ============================================================
-- IMPORTANTE: Depois de executar este schema, tens de definir o teu perfil como admin.
-- Substitui 'TEU_USER_ID_AQUI' pelo teu UUID de utilizador do Supabase Auth.
--
-- Para encontrar o teu user ID:
-- Dashboard → Authentication → Users → Copia o UUID do teu user
--
-- UPDATE profiles SET role = 'admin' WHERE id = 'TEU_USER_ID_AQUI';
--
-- ALTERNATIVA: Se o teu perfil ainda não existe, cria-o:
-- INSERT INTO profiles (id, full_name, role)
-- VALUES ('TEU_USER_ID_AQUI', 'Teu Nome', 'admin');

-- ============================================================
-- ✅ SCHEMA HARDENED APLICADO COM SUCESSO!
-- ============================================================
-- Próximos passos manuais:
-- 1. Define o teu perfil como 'admin' (instrução acima)
-- 2. No painel Supabase, ativa "Confirm email" em Authentication → Settings
-- 3. Configura hCaptcha em Authentication → Settings → Security
-- ============================================================
