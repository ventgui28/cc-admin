-- ============================================================
-- TABELA: user_logs
-- ============================================================
CREATE TABLE IF NOT EXISTS public.user_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    user_name TEXT NOT NULL,
    action TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

-- Habilitar Row Level Security (RLS)
ALTER TABLE public.user_logs ENABLE ROW LEVEL SECURITY;

-- Limpar políticas anteriores se existirem
DROP POLICY IF EXISTS "Authenticated users can select user_logs" ON public.user_logs;
DROP POLICY IF EXISTS "Authenticated users can insert user_logs" ON public.user_logs;

-- Políticas RLS:
-- 1. Qualquer utilizador autenticado pode ler os logs de atividade
CREATE POLICY "Authenticated users can select user_logs" ON public.user_logs
    FOR SELECT TO authenticated
    USING (true);

-- 2. Qualquer utilizador autenticado pode registar os seus próprios logs
CREATE POLICY "Authenticated users can insert user_logs" ON public.user_logs
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = user_id OR user_id IS NULL);

-- Criar índices de performance para consultas por data de criação (ex: Atividade Recente)
CREATE INDEX IF NOT EXISTS idx_user_logs_created_at ON public.user_logs(created_at DESC);
