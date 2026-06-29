# Instruções de Configuração da Base de Dados (Supabase)

Para que a aplicação Android funcione perfeitamente com todas as novas regras de formação, exames médicos desportivos (EMD), validadores de bicicletas e staff técnico, deves aplicar as seguintes alterações na tua base de dados do Supabase.

---

## 🛠️ Passo a Passo para Aplicação:

1. Acede ao painel do teu projeto no [Supabase Console](https://supabase.com/).
2. No menu lateral esquerdo, clica em **SQL Editor** (o ícone com `SQL`).
3. Clica em **New Query** (Nova Consulta).
4. Copia o código SQL abaixo por completo e cola-o na caixa de texto.
5. Clica no botão **Run** (Executar) no canto inferior direito.
6. Verifica se é exibida a mensagem de sucesso `"Success. No rows returned."`.

---

## 📝 Código SQL para Executar:

```sql
-- 1. Alterações na Tabela athletes (Menores e Saúde)
ALTER TABLE athletes 
ADD COLUMN IF NOT EXISTS encarregado_educacao_nome TEXT,
ADD COLUMN IF NOT EXISTS encarregado_educacao_contacto TEXT,
ADD COLUMN IF NOT EXISTS termo_responsabilidade_assinado BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS termo_responsabilidade_url TEXT,
ADD COLUMN IF NOT EXISTS emd_validade DATE;

COMMENT ON COLUMN athletes.encarregado_educacao_nome IS 'Nome do encarregado de educação (opcional)';
COMMENT ON COLUMN athletes.encarregado_educacao_contacto IS 'Contacto telefónico/email do encarregado de educação';
COMMENT ON COLUMN athletes.termo_responsabilidade_assinado IS 'Indica se os pais assinaram o termo de responsabilidade para provas';
COMMENT ON COLUMN athletes.emd_validade IS 'Data de validade do Exame Médico-Desportivo (EMD)';

-- 2. Criação da Tabela athlete_equipment (Validador de Bicicletas)
CREATE TABLE IF NOT EXISTS athlete_equipment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athletes(id) ON DELETE CASCADE,
    discipline TEXT NOT NULL, -- 'Estrada', 'Pista', 'BTT'
    wheel_size TEXT, -- '20', '24', '26', '27.5', '29', '700c'
    front_chainring INTEGER NOT NULL, -- Número de dentes do prato (ex: 46)
    rear_cog INTEGER NOT NULL, -- Número de dentes do carreto (ex: 14)
    carbon_wheels BOOLEAN DEFAULT false,
    rim_profile_over_65 BOOLEAN DEFAULT false,
    disc_wheels BOOLEAN DEFAULT false, -- Rodas lenticulares
    tt_handlebars BOOLEAN DEFAULT false, -- Extensores contrarrelógio
    is_validated BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Ativar RLS
ALTER TABLE athlete_equipment ENABLE ROW LEVEL SECURITY;

-- Criar Políticas RLS para athlete_equipment
DROP POLICY IF EXISTS "Authenticated users can CRUD equipment" ON athlete_equipment;
CREATE POLICY "Authenticated users can CRUD equipment" 
ON athlete_equipment FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- 3. Alterações na Tabela races (Tipos de Prova) e Nova Tabela race_sub_stages (Exercícios Escolas)
ALTER TABLE races
ADD COLUMN IF NOT EXISTS race_format TEXT DEFAULT 'Estrada', -- 'Encontro de Escolas', 'Estrada', 'BTT XCO', 'BTT XCC'
ADD COLUMN IF NOT EXISTS distance_km DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS duration_minutes INTEGER;

COMMENT ON COLUMN races.race_format IS 'Formato ou tipo específico de corrida';
COMMENT ON COLUMN races.distance_km IS 'Distância total planeada em quilómetros (para provas normais)';
COMMENT ON COLUMN races.duration_minutes IS 'Duração máxima planeada em minutos (para provas normais)';

CREATE TABLE IF NOT EXISTS race_sub_stages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    race_id UUID NOT NULL REFERENCES races(id) ON DELETE CASCADE,
    name TEXT NOT NULL, -- Ex: "Destreza/Gincana", "Prova em Linha"
    stage_type TEXT NOT NULL, -- Ex: "gincana", "linha", "xcc", "cronometro"
    distance_km DOUBLE PRECISION,
    duration_minutes INTEGER,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Ativar RLS para sub-provas
ALTER TABLE race_sub_stages ENABLE ROW LEVEL SECURITY;

-- Políticas RLS para race_sub_stages
DROP POLICY IF EXISTS "Authenticated users can CRUD race_sub_stages" ON race_sub_stages;
CREATE POLICY "Authenticated users can CRUD race_sub_stages" 
ON race_sub_stages FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- 4. Alterações na Tabela race_results (Classificações de Escolas e Dorsais)
ALTER TABLE race_results
ADD COLUMN IF NOT EXISTS penalty_seconds INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS pedagogical_status TEXT, -- 'Completou com êxito' ou 'Continua o seu processo de formação'
ADD COLUMN IF NOT EXISTS team_points INTEGER, -- 1 ou 2 pontos
ADD COLUMN IF NOT EXISTS bib_number TEXT, -- Número de dorsal/frontal atribuído
ADD COLUMN IF NOT EXISTS race_sub_stage_id UUID REFERENCES race_sub_stages(id) ON DELETE CASCADE; -- Referência opcional à sub-prova

COMMENT ON COLUMN race_results.penalty_seconds IS 'Segundos de penalização acumulados (especialmente para provas de Destreza)';
COMMENT ON COLUMN race_results.pedagogical_status IS 'Classificação pedagógica para Sub-7/Sub-9 baseada nas penalizações';
COMMENT ON COLUMN race_results.team_points IS 'Pontos atribuídos à equipa com base na classificação pedagógica';
COMMENT ON COLUMN race_results.bib_number IS 'Número do dorsal do atleta na prova';
COMMENT ON COLUMN race_results.race_sub_stage_id IS 'Sub-prova associada a este resultado (se aplicável)';

-- 5. Criação da Tabela team_staff (Foco nos Treinadores e Secretariado)
CREATE TABLE IF NOT EXISTS team_staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    role TEXT NOT NULL, -- 'Treinador', 'Diretor Desportivo', 'Mecânico', 'Massagista'
    coach_level TEXT, -- 'Grau I', 'Grau II', 'Grau III', NULL
    license_number TEXT,
    is_federated BOOLEAN DEFAULT false,
    phone TEXT,
    email TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Ativar RLS
ALTER TABLE team_staff ENABLE ROW LEVEL SECURITY;

-- Criar Políticas RLS para team_staff
DROP POLICY IF EXISTS "Authenticated users can CRUD team_staff" ON team_staff;
CREATE POLICY "Authenticated users can CRUD team_staff" 
ON team_staff FOR ALL TO authenticated USING (true) WITH CHECK (true);
```
