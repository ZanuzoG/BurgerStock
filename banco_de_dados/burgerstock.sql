/* =====================================================================
   PROJETO BURGERSTOCK - Banco de dados (SQL Server 2016 ou mais novo)
   Script único: cria o banco, as tabelas, as views, os dados de exemplo
   (os mesmos do Main.java) e as consultas das funcionalidades.
   ===================================================================== */

---------------------------------------------------------------------
-- 1. Banco de dados
---------------------------------------------------------------------
USE master;
GO

IF DB_ID('burgerstock') IS NOT NULL
BEGIN
    ALTER DATABASE burgerstock SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE burgerstock;
END
GO

-- CI = não diferencia maiúsculas: e-mail e código de lote são únicos sem olhar maiúsculas, como no Java
CREATE DATABASE burgerstock COLLATE Latin1_General_CI_AS;
GO

USE burgerstock;
GO

-- Necessário para o índice filtrado do e-mail (o SSMS já usa assim por padrão)
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

---------------------------------------------------------------------
-- 2. Pessoas, usuários e fornecedores
--    Usuário e fornecedor são pessoas: usam o mesmo id da pessoa
--    (chave primária e estrangeira ao mesmo tempo).
---------------------------------------------------------------------
CREATE TABLE pessoas (
    id        INT IDENTITY(1,1) NOT NULL,
    nome      VARCHAR(150)      NOT NULL,
    email     VARCHAR(120)      NULL,
    telefone  VARCHAR(20)       NULL,
    CONSTRAINT pk_pessoas PRIMARY KEY (id),
    CONSTRAINT ck_pessoas_nome CHECK (LEN(nome) > 0)
);
GO

-- E-mail único entre todas as pessoas, mas aceitando várias pessoas sem e-mail
CREATE UNIQUE INDEX uq_pessoas_email ON pessoas (email) WHERE email IS NOT NULL;
GO

-- Regras garantidas pelo sistema Java, e não pelo banco:
-- usuário sempre tem e-mail, e uma pessoa não é usuário e fornecedor ao mesmo tempo.
CREATE TABLE usuarios (
    pessoa_id   INT          NOT NULL,
    cpf         CHAR(11)     NOT NULL,   -- só números
    senha_hash  VARCHAR(64)  NOT NULL,   -- SHA-256 em hexadecimal
    perfil      VARCHAR(20)  NOT NULL CONSTRAINT df_usuarios_perfil DEFAULT 'FUNCIONARIO',
    ativo       BIT          NOT NULL CONSTRAINT df_usuarios_ativo DEFAULT 1,
    CONSTRAINT pk_usuarios PRIMARY KEY (pessoa_id),
    CONSTRAINT fk_usuarios_pessoas FOREIGN KEY (pessoa_id) REFERENCES pessoas (id),
    CONSTRAINT uq_usuarios_cpf UNIQUE (cpf),
    CONSTRAINT ck_usuarios_cpf CHECK (LEN(cpf) = 11 AND cpf NOT LIKE '%[^0-9]%'),
    CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('GERENTE', 'FUNCIONARIO'))
);
GO

CREATE TABLE fornecedores (
    pessoa_id  INT       NOT NULL,
    cnpj       CHAR(14)  NOT NULL,       -- só números
    ativo      BIT       NOT NULL CONSTRAINT df_fornecedores_ativo DEFAULT 1,
    CONSTRAINT pk_fornecedores PRIMARY KEY (pessoa_id),
    CONSTRAINT fk_fornecedores_pessoas FOREIGN KEY (pessoa_id) REFERENCES pessoas (id),
    CONSTRAINT uq_fornecedores_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_fornecedores_cnpj CHECK (LEN(cnpj) = 14 AND cnpj NOT LIKE '%[^0-9]%')
);
GO

---------------------------------------------------------------------
-- 3. Categorias, insumos e a associação insumo x fornecedor (N:N)
---------------------------------------------------------------------
CREATE TABLE categorias (
    id         INT IDENTITY(1,1) NOT NULL,
    nome       VARCHAR(100)      NOT NULL,
    descricao  VARCHAR(255)      NULL,
    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uq_categorias_nome UNIQUE (nome),
    CONSTRAINT ck_categorias_nome CHECK (LEN(nome) > 0)
);
GO

CREATE TABLE insumos (
    id              INT IDENTITY(1,1) NOT NULL,
    nome            VARCHAR(120)      NOT NULL,
    descricao       VARCHAR(255)      NULL,
    unidade_medida  VARCHAR(30)       NOT NULL,   -- a mesma na compra e no estoque
    estoque_minimo  DECIMAL(10,3)     NOT NULL CONSTRAINT df_insumos_minimo DEFAULT 0,
    ativo           BIT               NOT NULL CONSTRAINT df_insumos_ativo DEFAULT 1,
    categoria_id    INT               NOT NULL,
    CONSTRAINT pk_insumos PRIMARY KEY (id),
    CONSTRAINT fk_insumos_categorias FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT ck_insumos_nome CHECK (LEN(nome) > 0),
    CONSTRAINT ck_insumos_unidade CHECK (LEN(unidade_medida) > 0),
    CONSTRAINT ck_insumos_minimo CHECK (estoque_minimo >= 0)
);
GO

CREATE TABLE insumo_fornecedor (
    id             INT IDENTITY(1,1) NOT NULL,
    insumo_id      INT NOT NULL,
    fornecedor_id  INT NOT NULL,
    CONSTRAINT pk_insumo_fornecedor PRIMARY KEY (id),
    CONSTRAINT fk_insumo_fornecedor_insumos FOREIGN KEY (insumo_id) REFERENCES insumos (id),
    CONSTRAINT fk_insumo_fornecedor_fornecedores FOREIGN KEY (fornecedor_id) REFERENCES fornecedores (pessoa_id),
    CONSTRAINT uq_insumo_fornecedor UNIQUE (insumo_id, fornecedor_id)
);
GO

---------------------------------------------------------------------
-- 4. Pedidos de compra e itens (N:N entre pedido e insumo)
--    O pedido sempre chega completo.
---------------------------------------------------------------------
CREATE TABLE pedidos_compra (
    id                INT IDENTITY(1,1) NOT NULL,
    fornecedor_id     INT               NOT NULL,
    usuario_id        INT               NOT NULL,   -- quem fez o pedido
    data_pedido       DATETIME          NOT NULL CONSTRAINT df_pedidos_compra_data DEFAULT GETDATE(),
    data_recebimento  DATETIME          NULL,
    status            VARCHAR(20)       NOT NULL CONSTRAINT df_pedidos_compra_status DEFAULT 'ABERTO',
    observacao        VARCHAR(255)      NULL,
    CONSTRAINT pk_pedidos_compra PRIMARY KEY (id),
    CONSTRAINT fk_pedidos_compra_fornecedores FOREIGN KEY (fornecedor_id) REFERENCES fornecedores (pessoa_id),
    CONSTRAINT fk_pedidos_compra_usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios (pessoa_id),
    CONSTRAINT ck_pedidos_compra_status CHECK (status IN ('ABERTO', 'RECEBIDO', 'CANCELADO')),
    -- Só o pedido RECEBIDO tem data de recebimento, e ela não é anterior ao pedido
    CONSTRAINT ck_pedidos_compra_recebimento CHECK (
        (status = 'RECEBIDO' AND data_recebimento IS NOT NULL AND data_recebimento >= data_pedido)
        OR (status <> 'RECEBIDO' AND data_recebimento IS NULL)
    )
);
GO

-- Regra garantida pelo sistema Java: o insumo do item precisa estar associado
-- (insumo_fornecedor) ao fornecedor do pedido.
CREATE TABLE itens_pedido_compra (
    id                INT IDENTITY(1,1) NOT NULL,
    pedido_compra_id  INT               NOT NULL,
    insumo_id         INT               NOT NULL,
    quantidade        DECIMAL(10,3)     NOT NULL,
    valor_unitario    DECIMAL(10,2)     NOT NULL,
    CONSTRAINT pk_itens_pedido_compra PRIMARY KEY (id),
    CONSTRAINT fk_itens_pedido_compra_pedidos FOREIGN KEY (pedido_compra_id) REFERENCES pedidos_compra (id),
    CONSTRAINT fk_itens_pedido_compra_insumos FOREIGN KEY (insumo_id) REFERENCES insumos (id),
    CONSTRAINT uq_itens_pedido_compra UNIQUE (pedido_compra_id, insumo_id),
    CONSTRAINT ck_itens_pedido_compra_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_itens_pedido_compra_valor CHECK (valor_unitario > 0)
);
GO

---------------------------------------------------------------------
-- 5. Lotes (um por item recebido) e movimentações de estoque
---------------------------------------------------------------------
CREATE TABLE lotes (
    id                     INT IDENTITY(1,1) NOT NULL,
    codigo                 VARCHAR(50)       NOT NULL,
    validade               DATE              NOT NULL,
    item_pedido_compra_id  INT               NOT NULL,
    CONSTRAINT pk_lotes PRIMARY KEY (id),
    CONSTRAINT fk_lotes_itens_pedido_compra FOREIGN KEY (item_pedido_compra_id) REFERENCES itens_pedido_compra (id),
    CONSTRAINT uq_lotes_codigo UNIQUE (codigo),                 -- único no sistema inteiro
    CONSTRAINT uq_lotes_item UNIQUE (item_pedido_compra_id),    -- um lote por item de pedido
    CONSTRAINT ck_lotes_codigo CHECK (LEN(codigo) > 0)
);
GO

CREATE TABLE movimentacoes_estoque (
    id          INT IDENTITY(1,1) NOT NULL,
    tipo        VARCHAR(20)       NOT NULL,
    motivo      VARCHAR(150)      NOT NULL,
    quantidade  DECIMAL(10,3)     NOT NULL,   -- no AJUSTE vem com sinal (+ ou -)
    data_hora   DATETIME          NOT NULL CONSTRAINT df_movimentacoes_data DEFAULT GETDATE(),
    observacao  VARCHAR(255)      NULL,
    lote_id     INT               NOT NULL,
    usuario_id  INT               NOT NULL,   -- responsável pela movimentação
    CONSTRAINT pk_movimentacoes_estoque PRIMARY KEY (id),
    CONSTRAINT fk_movimentacoes_lotes FOREIGN KEY (lote_id) REFERENCES lotes (id),
    CONSTRAINT fk_movimentacoes_usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios (pessoa_id),
    CONSTRAINT ck_movimentacoes_tipo CHECK (tipo IN ('ENTRADA', 'SAIDA', 'PERDA', 'AJUSTE')),
    CONSTRAINT ck_movimentacoes_quantidade CHECK (quantidade > 0 OR (tipo = 'AJUSTE' AND quantidade <> 0)),
    CONSTRAINT ck_movimentacoes_motivo CHECK (LEN(motivo) > 0)
);
GO

-- Índices para as consultas mais usadas (saldo por lote e histórico por período)
CREATE INDEX ix_movimentacoes_lote ON movimentacoes_estoque (lote_id);
CREATE INDEX ix_movimentacoes_data_hora ON movimentacoes_estoque (data_hora);
GO

---------------------------------------------------------------------
-- 6. Views: o que é calculado e não fica guardado em coluna
---------------------------------------------------------------------

-- Saldo de cada lote = entradas + ajustes - saídas - perdas
CREATE VIEW vw_saldo_lote AS
SELECT l.id               AS lote_id,
       l.codigo,
       l.validade,
       i.id               AS insumo_id,
       i.nome             AS insumo,
       i.unidade_medida,
       p.fornecedor_id,
       f.nome             AS fornecedor,
       p.id               AS pedido_compra_id,
       COALESCE(SUM(CASE m.tipo
                      WHEN 'ENTRADA' THEN m.quantidade
                      WHEN 'AJUSTE'  THEN m.quantidade
                      WHEN 'SAIDA'   THEN -m.quantidade
                      WHEN 'PERDA'   THEN -m.quantidade
                    END), 0) AS saldo
FROM lotes l
JOIN itens_pedido_compra ip ON ip.id = l.item_pedido_compra_id
JOIN pedidos_compra p       ON p.id = ip.pedido_compra_id
JOIN pessoas f              ON f.id = p.fornecedor_id
JOIN insumos i              ON i.id = ip.insumo_id
LEFT JOIN movimentacoes_estoque m ON m.lote_id = l.id
GROUP BY l.id, l.codigo, l.validade, i.id, i.nome, i.unidade_medida, p.fornecedor_id, f.nome, p.id;
GO

-- Saldo de cada insumo (soma dos lotes). Insumo sem lote aparece com saldo 0.
CREATE VIEW vw_saldo_insumo AS
SELECT i.id    AS insumo_id,
       i.nome  AS insumo,
       c.nome  AS categoria,
       i.unidade_medida,
       i.estoque_minimo,
       i.ativo,
       COALESCE(SUM(s.saldo), 0) AS saldo,
       CASE WHEN COALESCE(SUM(s.saldo), 0) < i.estoque_minimo THEN 1 ELSE 0 END AS abaixo_do_minimo
FROM insumos i
JOIN categorias c ON c.id = i.categoria_id
LEFT JOIN vw_saldo_lote s ON s.insumo_id = i.id
GROUP BY i.id, i.nome, c.nome, i.unidade_medida, i.estoque_minimo, i.ativo;
GO

-- Valor total de cada pedido = soma de quantidade x valor unitário
CREATE VIEW vw_pedido_total AS
SELECT p.id    AS pedido_compra_id,
       p.data_pedido,
       p.data_recebimento,
       p.status,
       f.nome  AS fornecedor,
       CAST(COALESCE(SUM(ip.quantidade * ip.valor_unitario), 0) AS DECIMAL(12,2)) AS valor_total
FROM pedidos_compra p
JOIN pessoas f ON f.id = p.fornecedor_id
LEFT JOIN itens_pedido_compra ip ON ip.pedido_compra_id = p.id
GROUP BY p.id, p.data_pedido, p.data_recebimento, p.status, f.nome;
GO

---------------------------------------------------------------------
-- 7. Dados de exemplo (os mesmos do Main.java)
--    Tudo numa transação: se um INSERT falhar, nada é gravado.
--    Os ids gerados pelo banco ficam em variáveis (SCOPE_IDENTITY).
---------------------------------------------------------------------
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @ontem DATETIME = DATEADD(DAY, -1, CAST(CAST(GETDATE() AS DATE) AS DATETIME));
DECLARE @hoje  DATE     = CAST(GETDATE() AS DATE);

DECLARE @carlos INT, @ana INT, @boibom INT, @paoquente INT;
DECLARE @carnes INT, @paes INT, @blend INT, @brioche INT;
DECLARE @pedidoCarne INT, @pedidoPao INT, @itemCarne INT, @itemPao INT;
DECLARE @loteCarne INT, @lotePao INT;

-- 7.1 Usuários: um GERENTE e uma FUNCIONARIO (senha guardada como SHA-256)
INSERT INTO pessoas (nome, email, telefone)
VALUES ('Carlos Silva', 'carlos@burger.com', '(17) 98888-0001');
SET @carlos = SCOPE_IDENTITY();
INSERT INTO usuarios (pessoa_id, cpf, senha_hash, perfil)
VALUES (@carlos, '11122233344', CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', 'carlos123'), 2), 'GERENTE');

INSERT INTO pessoas (nome, email, telefone)
VALUES ('Ana Souza', 'ana@burger.com', '(17) 97777-0002');
SET @ana = SCOPE_IDENTITY();
INSERT INTO usuarios (pessoa_id, cpf, senha_hash, perfil)
VALUES (@ana, '55566677788', CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', 'ana12345'), 2), 'FUNCIONARIO');

-- 7.2 Fornecedores (também são pessoas)
INSERT INTO pessoas (nome, email, telefone)
VALUES ('Frigorífico Boi Bom', 'contato@boibom.com', '(17) 99999-1111');
SET @boibom = SCOPE_IDENTITY();
INSERT INTO fornecedores (pessoa_id, cnpj) VALUES (@boibom, '12345678000199');

INSERT INTO pessoas (nome, email, telefone)
VALUES ('Padaria Pão Quente', 'vendas@paoquente.com', '(17) 99999-2222');
SET @paoquente = SCOPE_IDENTITY();
INSERT INTO fornecedores (pessoa_id, cnpj) VALUES (@paoquente, '98765432000111');

-- 7.3 Categorias e insumos
INSERT INTO categorias (nome, descricao) VALUES ('Carnes', 'Blends bovinos e bacon');
SET @carnes = SCOPE_IDENTITY();
INSERT INTO categorias (nome, descricao) VALUES ('Pães', 'Pães artesanais');
SET @paes = SCOPE_IDENTITY();

INSERT INTO insumos (nome, descricao, unidade_medida, estoque_minimo, categoria_id)
VALUES ('Blend Bovino 180g', 'Carne moída fresca', 'kg', 10, @carnes);
SET @blend = SCOPE_IDENTITY();
INSERT INTO insumos (nome, descricao, unidade_medida, estoque_minimo, categoria_id)
VALUES ('Pão Brioche', 'Pão selado na manteiga', 'unidade', 30, @paes);
SET @brioche = SCOPE_IDENTITY();

-- 7.4 Quem fornece o quê
INSERT INTO insumo_fornecedor (insumo_id, fornecedor_id)
VALUES (@blend, @boibom),
       (@brioche, @paoquente);

-- 7.5 Pedidos de compra feitos pelo Gerente (entram como ABERTO)
INSERT INTO pedidos_compra (fornecedor_id, usuario_id, data_pedido, observacao)
VALUES (@boibom, @carlos, DATEADD(MINUTE, 450, @ontem), 'Reposição semanal de carne');      -- 07:30
SET @pedidoCarne = SCOPE_IDENTITY();
INSERT INTO itens_pedido_compra (pedido_compra_id, insumo_id, quantidade, valor_unitario)
VALUES (@pedidoCarne, @blend, 20, 32.90);           -- 20 kg a R$ 32,90
SET @itemCarne = SCOPE_IDENTITY();

INSERT INTO pedidos_compra (fornecedor_id, usuario_id, data_pedido, observacao)
VALUES (@paoquente, @carlos, DATEADD(MINUTE, 455, @ontem), 'Pães da semana');              -- 07:35
SET @pedidoPao = SCOPE_IDENTITY();
INSERT INTO itens_pedido_compra (pedido_compra_id, insumo_id, quantidade, valor_unitario)
VALUES (@pedidoPao, @brioche, 50, 1.80);            -- 50 pães a R$ 1,80
SET @itemPao = SCOPE_IDENTITY();

-- 7.6 Recebimento (como o PedidoCompra.receber do Java):
--     cria o lote de cada item, registra a ENTRADA e marca o pedido como RECEBIDO
INSERT INTO lotes (codigo, validade, item_pedido_compra_id)
VALUES ('LOTE-CARNE-01', DATEADD(DAY, 15, @hoje), @itemCarne);
SET @loteCarne = SCOPE_IDENTITY();
INSERT INTO movimentacoes_estoque (tipo, motivo, quantidade, data_hora, lote_id, usuario_id)
VALUES ('ENTRADA', 'Recebimento de pedido de compra', 20, DATEADD(MINUTE, 490, @ontem), @loteCarne, @ana);   -- 08:10
UPDATE pedidos_compra
SET status = 'RECEBIDO', data_recebimento = DATEADD(MINUTE, 490, @ontem)
WHERE id = @pedidoCarne;

INSERT INTO lotes (codigo, validade, item_pedido_compra_id)
VALUES ('LOTE-PAO-01', DATEADD(DAY, 2, @hoje), @itemPao);
SET @lotePao = SCOPE_IDENTITY();
INSERT INTO movimentacoes_estoque (tipo, motivo, quantidade, data_hora, lote_id, usuario_id)
VALUES ('ENTRADA', 'Recebimento de pedido de compra', 50, DATEADD(MINUTE, 492, @ontem), @lotePao, @ana);     -- 08:12
UPDATE pedidos_compra
SET status = 'RECEBIDO', data_recebimento = DATEADD(MINUTE, 492, @ontem)
WHERE id = @pedidoPao;

-- 7.7 Saídas, perda e ajuste de inventário
INSERT INTO movimentacoes_estoque (tipo, motivo, quantidade, data_hora, lote_id, usuario_id)
VALUES ('SAIDA',  'Consumo na produção',      12,   DATEADD(MINUTE, 700, @ontem), @loteCarne, @ana),      -- 11:40
       ('SAIDA',  'Consumo na produção',      15,   DATEADD(MINUTE, 705, @ontem), @lotePao,   @ana),      -- 11:45
       ('PERDA',  'Pão amassado na entrega',   3,   DATEADD(MINUTE, 740, @ontem), @lotePao,   @ana),      -- 12:20
       ('AJUSTE', 'Inventário físico',        -0.5, DATEADD(MINUTE, 965, @ontem), @loteCarne, @carlos);  -- 16:05

COMMIT;
GO

---------------------------------------------------------------------
-- 8. Consultas das funcionalidades
--    Período dos relatórios: últimos 7 dias, incluindo hoje.
---------------------------------------------------------------------
DECLARE @inicio DATETIME = DATEADD(DAY, -7, CAST(CAST(GETDATE() AS DATE) AS DATETIME));
DECLARE @fim    DATETIME = DATEADD(DAY,  1, CAST(CAST(GETDATE() AS DATE) AS DATETIME));

-- 8.1 Login (func. 04): devolve o usuário se e-mail e senha conferem e ele está ativo
DECLARE @email VARCHAR(120) = 'ana@burger.com';
DECLARE @senha VARCHAR(100) = 'ana12345';
SELECT p.id, p.nome, u.perfil
FROM usuarios u
JOIN pessoas p ON p.id = u.pessoa_id
WHERE p.email = @email
  AND u.senha_hash = CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', @senha), 2)
  AND u.ativo = 1;

-- 8.2 Saldo por lote (func. 17): LOTE-CARNE-01 = 7,5 e LOTE-PAO-01 = 32
SELECT codigo, insumo, fornecedor, unidade_medida, validade, saldo
FROM vw_saldo_lote
ORDER BY codigo;

-- 8.3 Saldo por insumo, com categoria (func. 17)
SELECT insumo, categoria, unidade_medida, saldo, estoque_minimo
FROM vw_saldo_insumo
WHERE ativo = 1
ORDER BY insumo;

-- 8.4 Alerta: insumos ativos abaixo do estoque mínimo (func. 19): Blend Bovino 180g
SELECT insumo, saldo, estoque_minimo
FROM vw_saldo_insumo
WHERE ativo = 1 AND abaixo_do_minimo = 1;

-- 8.5 Alerta: lotes com saldo que vencem nos próximos 5 dias (func. 19): LOTE-PAO-01
SELECT codigo, insumo, validade, saldo
FROM vw_saldo_lote
WHERE saldo > 0
  AND validade <= DATEADD(DAY, 5, CAST(GETDATE() AS DATE))
ORDER BY validade;

-- 8.6 Histórico de movimentações no período, com o responsável (func. 18)
SELECT m.data_hora, m.tipo, l.codigo AS lote, m.quantidade, m.motivo, p.nome AS responsavel
FROM movimentacoes_estoque m
JOIN lotes l   ON l.id = m.lote_id
JOIN pessoas p ON p.id = m.usuario_id
WHERE m.data_hora >= @inicio AND m.data_hora < @fim
ORDER BY m.data_hora;

-- 8.7 Relatório de uso: saídas por insumo no período, do mais usado ao menos usado (func. 21)
SELECT s.insumo, s.unidade_medida, SUM(m.quantidade) AS total_usado
FROM movimentacoes_estoque m
JOIN vw_saldo_lote s ON s.lote_id = m.lote_id
WHERE m.tipo = 'SAIDA'
  AND m.data_hora >= @inicio AND m.data_hora < @fim
GROUP BY s.insumo, s.unidade_medida
ORDER BY total_usado DESC;

-- 8.8 Indicadores: quantidade de movimentações de cada tipo no período (func. 20)
SELECT tipo, COUNT(*) AS quantidade_de_movimentacoes
FROM movimentacoes_estoque
WHERE data_hora >= @inicio AND data_hora < @fim
GROUP BY tipo;

-- 8.9 Rastreabilidade: de qual pedido e fornecedor veio cada lote (func. 13)
SELECT s.codigo AS lote, s.insumo, s.fornecedor, s.pedido_compra_id, p.data_recebimento, s.validade
FROM vw_saldo_lote s
JOIN pedidos_compra p ON p.id = s.pedido_compra_id
ORDER BY s.codigo;

-- 8.10 Pedidos de compra com o valor total (func. 11): R$ 658,00 e R$ 90,00
SELECT pedido_compra_id, fornecedor, status, data_pedido, valor_total
FROM vw_pedido_total
ORDER BY pedido_compra_id;
GO
