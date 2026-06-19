-- ============================================================
-- CANTANHEDE CYCLING HUB - Schema Completo para Supabase
-- Copia e cola isto no SQL Editor da tua nova conta Supabase
-- (Dashboard → SQL Editor → New Query → Cole → Run)
-- ============================================================

-- =====================
-- 1. TABELA: profiles
-- =====================
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name TEXT,
    avatar_url TEXT,
    role TEXT DEFAULT 'editor',
    phone TEXT,
    location TEXT,  
    bio TEXT,
    birth_date TEXT,
    gender TEXT,
    language TEXT,
    theme TEXT,
    updated_at TIMESTAMPTZ DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- =====================
-- 2. TABELA: athletes
-- =====================
CREATE TABLE IF NOT EXISTS athletes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    photo_url TEXT,
    birth_date TEXT,
    instagram_handle TEXT,
    license_number TEXT,
    status TEXT DEFAULT 'active',
    weight DOUBLE PRECISION,
    height DOUBLE PRECISION,
    role TEXT,
    ftp INTEGER,
    vo2_max DOUBLE PRECISION,
    watts_per_kg DOUBLE PRECISION,
    hr_max INTEGER,
    power_5s INTEGER,
    power_1m INTEGER,
    power_5m INTEGER,
    power_20m INTEGER
);

-- =====================
-- 3. TABELA: races
-- =====================
CREATE TABLE IF NOT EXISTS races (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    date TEXT NOT NULL,
    category TEXT NOT NULL,
    status TEXT DEFAULT 'Aberto',
    location TEXT,
    description TEXT,
    gender TEXT DEFAULT 'Misto',
    sub_categories JSONB DEFAULT '[]'::jsonb,
    link TEXT
);

-- =====================
-- 4. TABELA: race_results
-- =====================
CREATE TABLE IF NOT EXISTS race_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    race_id UUID NOT NULL REFERENCES races(id) ON DELETE CASCADE,
    athlete_id UUID NOT NULL REFERENCES athletes(id) ON DELETE CASCADE,
    position INTEGER,
    time TEXT,
    category_at_time TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- =====================
-- 5. TABELA: social_posts
-- =====================
CREATE TABLE IF NOT EXISTS social_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    image_url TEXT,
    tags JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- =====================
-- 6. TABELA: sponsors
-- =====================
CREATE TABLE IF NOT EXISTS sponsors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    logo_url TEXT,
    website TEXT,
    level TEXT DEFAULT 'Gold'
);

-- =====================
-- 7. TABELA: vault_texts
-- =====================
CREATE TABLE IF NOT EXISTS vault_texts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- =====================
-- 8. TABELA: app_updates (Para o sistema de atualizações)
-- =====================
CREATE TABLE IF NOT EXISTS app_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_code INTEGER NOT NULL UNIQUE,
    version_name TEXT NOT NULL,
    apk_url TEXT NOT NULL,
    release_notes TEXT,
    is_mandatory BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================
-- RLS (Row Level Security) - Políticas de Acesso
-- Permite que utilizadores autenticados façam CRUD em tudo
-- ============================================================

-- Ativar RLS em todas as tabelas
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE athletes ENABLE ROW LEVEL SECURITY;
ALTER TABLE races ENABLE ROW LEVEL SECURITY;
ALTER TABLE race_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE social_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE sponsors ENABLE ROW LEVEL SECURITY;
ALTER TABLE vault_texts ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_updates ENABLE ROW LEVEL SECURITY;

-- Limpar políticas existentes para evitar erros de duplicados
DROP POLICY IF EXISTS "Authenticated users can read profiles" ON profiles;
DROP POLICY IF EXISTS "Authenticated users can read athletes" ON athletes;
DROP POLICY IF EXISTS "Authenticated users can read races" ON races;
DROP POLICY IF EXISTS "Authenticated users can read race_results" ON race_results;
DROP POLICY IF EXISTS "Authenticated users can read social_posts" ON social_posts;
DROP POLICY IF EXISTS "Authenticated users can read sponsors" ON sponsors;
DROP POLICY IF EXISTS "Authenticated users can read vault_texts" ON vault_texts;

DROP POLICY IF EXISTS "Anyone can read app_updates" ON app_updates;

DROP POLICY IF EXISTS "Authenticated users can insert profiles" ON profiles;
DROP POLICY IF EXISTS "Authenticated users can insert athletes" ON athletes;
DROP POLICY IF EXISTS "Authenticated users can insert races" ON races;
DROP POLICY IF EXISTS "Authenticated users can insert race_results" ON race_results;
DROP POLICY IF EXISTS "Authenticated users can insert social_posts" ON social_posts;
DROP POLICY IF EXISTS "Authenticated users can insert sponsors" ON sponsors;
DROP POLICY IF EXISTS "Authenticated users can insert vault_texts" ON vault_texts;
DROP POLICY IF EXISTS "Authenticated users can insert app_updates" ON app_updates;

DROP POLICY IF EXISTS "Authenticated users can update profiles" ON profiles;
DROP POLICY IF EXISTS "Authenticated users can update athletes" ON athletes;
DROP POLICY IF EXISTS "Authenticated users can update races" ON races;
DROP POLICY IF EXISTS "Authenticated users can update race_results" ON race_results;
DROP POLICY IF EXISTS "Authenticated users can update social_posts" ON social_posts;
DROP POLICY IF EXISTS "Authenticated users can update sponsors" ON sponsors;
DROP POLICY IF EXISTS "Authenticated users can update vault_texts" ON vault_texts;
DROP POLICY IF EXISTS "Authenticated users can update app_updates" ON app_updates;

DROP POLICY IF EXISTS "Authenticated users can delete profiles" ON profiles;
DROP POLICY IF EXISTS "Authenticated users can delete athletes" ON athletes;
DROP POLICY IF EXISTS "Authenticated users can delete races" ON races;
DROP POLICY IF EXISTS "Authenticated users can delete race_results" ON race_results;
DROP POLICY IF EXISTS "Authenticated users can delete social_posts" ON social_posts;
DROP POLICY IF EXISTS "Authenticated users can delete sponsors" ON sponsors;
DROP POLICY IF EXISTS "Authenticated users can delete vault_texts" ON vault_texts;
DROP POLICY IF EXISTS "Authenticated users can delete app_updates" ON app_updates;

-- Criar Políticas: SELECT (leitura)
CREATE POLICY "Authenticated users can read profiles" ON profiles FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated users can read athletes" ON athletes FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated users can read races" ON races FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated users can read race_results" ON race_results FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated users can read social_posts" ON social_posts FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated users can read sponsors" ON sponsors FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated users can read vault_texts" ON vault_texts FOR SELECT TO authenticated USING (true);

-- IMPORTANTE: app_updates tem de ser legível publicamente para a app verificar atualizações
CREATE POLICY "Anyone can read app_updates" ON app_updates FOR SELECT USING (true);

-- Criar Políticas: INSERT
CREATE POLICY "Authenticated users can insert profiles" ON profiles FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Authenticated users can insert athletes" ON athletes FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Authenticated users can insert races" ON races FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Authenticated users can insert race_results" ON race_results FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Authenticated users can insert social_posts" ON social_posts FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Authenticated users can insert sponsors" ON sponsors FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Authenticated users can insert vault_texts" ON vault_texts FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Authenticated users can insert app_updates" ON app_updates FOR INSERT TO authenticated WITH CHECK (true);

-- Criar Políticas: UPDATE
CREATE POLICY "Authenticated users can update profiles" ON profiles FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Authenticated users can update athletes" ON athletes FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Authenticated users can update races" ON races FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Authenticated users can update race_results" ON race_results FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Authenticated users can update social_posts" ON social_posts FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Authenticated users can update sponsors" ON sponsors FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Authenticated users can update vault_texts" ON vault_texts FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Authenticated users can update app_updates" ON app_updates FOR UPDATE TO authenticated USING (true) WITH CHECK (true);

-- Criar Políticas: DELETE
CREATE POLICY "Authenticated users can delete profiles" ON profiles FOR DELETE TO authenticated USING (true);
CREATE POLICY "Authenticated users can delete athletes" ON athletes FOR DELETE TO authenticated USING (true);
CREATE POLICY "Authenticated users can delete races" ON races FOR DELETE TO authenticated USING (true);
CREATE POLICY "Authenticated users can delete race_results" ON race_results FOR DELETE TO authenticated USING (true);
CREATE POLICY "Authenticated users can delete social_posts" ON social_posts FOR DELETE TO authenticated USING (true);
CREATE POLICY "Authenticated users can delete sponsors" ON sponsors FOR DELETE TO authenticated USING (true);
CREATE POLICY "Authenticated users can delete vault_texts" ON vault_texts FOR DELETE TO authenticated USING (true);
CREATE POLICY "Authenticated users can delete app_updates" ON app_updates FOR DELETE TO authenticated USING (true);

-- ============================================================
-- STORAGE BUCKETS (avatars e app-releases)
-- ============================================================
INSERT INTO storage.buckets (id, name, public)
VALUES ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO storage.buckets (id, name, public)
VALUES ('app-releases', 'app-releases', true)
ON CONFLICT (id) DO NOTHING;

-- Limpar políticas de storage existentes
DROP POLICY IF EXISTS "Public read access for avatars" ON storage.objects;
DROP POLICY IF EXISTS "Public read access for app-releases" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can upload avatars" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can upload app-releases" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can update avatars" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can update app-releases" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can delete avatars" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can delete app-releases" ON storage.objects;

-- Criar Políticas de Storage
CREATE POLICY "Public read access for avatars" ON storage.objects FOR SELECT USING (bucket_id = 'avatars');
CREATE POLICY "Public read access for app-releases" ON storage.objects FOR SELECT USING (bucket_id = 'app-releases');

CREATE POLICY "Authenticated users can upload avatars" ON storage.objects FOR INSERT TO authenticated WITH CHECK (bucket_id = 'avatars');
CREATE POLICY "Authenticated users can upload app-releases" ON storage.objects FOR INSERT TO authenticated WITH CHECK (bucket_id = 'app-releases');

CREATE POLICY "Authenticated users can update avatars" ON storage.objects FOR UPDATE TO authenticated USING (bucket_id = 'avatars') WITH CHECK (bucket_id = 'avatars');
CREATE POLICY "Authenticated users can update app-releases" ON storage.objects FOR UPDATE TO authenticated USING (bucket_id = 'app-releases') WITH CHECK (bucket_id = 'app-releases');

CREATE POLICY "Authenticated users can delete avatars" ON storage.objects FOR DELETE TO authenticated USING (bucket_id = 'avatars');
CREATE POLICY "Authenticated users can delete app-releases" ON storage.objects FOR DELETE TO authenticated USING (bucket_id = 'app-releases');

-- ============================================================
-- ✅ PRONTO! Schema criado com sucesso.
-- ============================================================
