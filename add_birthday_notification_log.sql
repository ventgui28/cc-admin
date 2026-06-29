-- ============================================================
-- CANTANHEDE CYCLING HUB - Tabela de Histórico de Notificações
-- ============================================================

CREATE TABLE IF NOT EXISTS birthday_notification_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athletes(id) ON DELETE CASCADE,
    notification_year INTEGER NOT NULL,   -- ex: 2026
    days_before INTEGER NOT NULL,          -- 1 ou 2
    sent_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (athlete_id, notification_year, days_before)
);

-- Ativar RLS
ALTER TABLE birthday_notification_log ENABLE ROW LEVEL SECURITY;

-- Limpar políticas existentes se existirem
DROP POLICY IF EXISTS "Authenticated users can read birthday logs" ON birthday_notification_log;
DROP POLICY IF EXISTS "Authenticated users can insert birthday logs" ON birthday_notification_log;

-- Criar Políticas
CREATE POLICY "Authenticated users can read birthday logs" ON birthday_notification_log FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated users can insert birthday logs" ON birthday_notification_log FOR INSERT TO authenticated WITH CHECK (true);

-- Criar Índice
CREATE INDEX IF NOT EXISTS idx_birthday_notif_log_lookup
    ON birthday_notification_log (athlete_id, notification_year, days_before);
