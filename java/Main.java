import entities.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO SISTEMA DA HAMBURGUERIA ===\n");

        // 1. ACESSOS -------------------------------------------------------------
        // O gerente é criado pela "criação de acessos", com o perfil escolhido.
        Usuario gerente = new Usuario(null, "Carlos Silva", "111.222.333-44", "carlos@burger.com",
                "(17) 98888-0001", "carlos123", Perfil.GERENTE, false);
        gerente.criarAcesso();

        // Cadastre-se: todo novo cadastro entra como FUNCIONARIO.
        Usuario funcionario = Usuario.cadastrarSe("Ana Souza", "555.666.777-88", "ana@burger.com", "ana12345");

        System.out.println("--- Usuários cadastrados ---");
        for (Usuario u : Usuario.listaUsuarios) {
            System.out.println("- " + u.getNome() + " | CPF " + u.getCpf() + " | " + u.getEmail() + " | " + u.getPerfil());
        }
        // A senha fica guardada como SHA-256, o mesmo valor da coluna usuarios.senha_hash do banco.
        System.out.println("Senha do Carlos guardada como hash: " + gerente.getSenhaHash());

        try {
            Usuario.cadastrarSe("Outra Ana", "999.888.777-66", "ana@burger.com", "outra123");
        } catch (IllegalStateException e) {
            System.out.println("Cadastro recusado: " + e.getMessage());
        }
        try {
            Usuario.cadastrarSe("Ana Clone", "555.666.777-88", "clone@burger.com", "clone123");
        } catch (IllegalStateException e) {
            System.out.println("Cadastro recusado: " + e.getMessage());
        }

        // Login
        System.out.println("\n--- Login ---");
        Usuario logado = Usuario.fazerLogin("ana@burger.com", "ana12345");
        System.out.println("Entrou: " + logado.getNome() + " (" + logado.getPerfil() + ")");
        try {
            Usuario.fazerLogin("ana@burger.com", "senhaErrada");
        } catch (IllegalArgumentException e) {
            System.out.println("Login recusado: " + e.getMessage());
        }

        // Permissões por perfil
        System.out.println("\n--- Permissões por perfil ---");
        String[] acoes = {"CADASTRAR_FORNECEDOR", "FAZER_PEDIDO_COMPRA", "RECEBER_PEDIDO", "REGISTRAR_SAIDA"};
        for (String acao : acoes) {
            System.out.println(acao + " -> Gerente: " + gerente.temPermissao(acao)
                    + " | Funcionário: " + funcionario.temPermissao(acao));
        }

        // Meu perfil: todo usuário altera os próprios dados e a senha, mas não o CPF nem o perfil de acesso.
        System.out.println("\n--- Meu perfil ---");
        Usuario novosDados = new Usuario();
        novosDados.setEmail("ana.souza@burger.com");
        novosDados.setTelefone("(17) 97777-0002");
        funcionario.editarPerfil(novosDados);
        funcionario.alterarSenha("ana12345", "novaSenha1");
        System.out.println("Ana atualizou o e-mail para " + funcionario.getEmail()
                + ", o telefone para " + funcionario.getTelefone() + " e trocou a senha.");

        try {
            funcionario.alterarTipoAcesso(Perfil.GERENTE, funcionario); // Ana tenta virar gerente
        } catch (IllegalStateException e) {
            System.out.println("Alteração recusada: " + e.getMessage());
        }

        // Lembrete de senha (envio de e-mail simulado)
        System.out.println("\n--- Lembrete de senha ---");
        String provisoria = Usuario.solicitarLembreteSenha("ana.souza@burger.com");
        System.out.println("[E-mail simulado para ana.souza@burger.com] Sua senha provisória é: " + provisoria);
        System.out.println("Entrou com a senha provisória? "
                + (Usuario.fazerLogin("ana.souza@burger.com", provisoria) != null));
        System.out.println("Lembrete para e-mail não cadastrado: " + Usuario.solicitarLembreteSenha("ninguem@burger.com"));

        // 2. CADASTRANDO CATEGORIAS E FORNECEDORES --------------------------------
        // Os ids são gerados no cadastro, como o IDENTITY do banco.
        Categoria catCarnes = new Categoria(null, "Carnes", "Blends bovinos e bacon");
        catCarnes.cadastrar();

        Categoria catPaes = new Categoria(null, "Pães", "Pães artesanais");
        catPaes.cadastrar();

        Fornecedor acougue = new Fornecedor(null, "Frigorífico Boi Bom", "12.345.678/0001-99", "(17) 99999-1111", "contato@boibom.com");
        acougue.cadastrar();
        Fornecedor padaria = new Fornecedor(null, "Padaria Pão Quente", "98.765.432/0001-11", "(17) 99999-2222", "vendas@paoquente.com");
        padaria.cadastrar();

        System.out.println("\n--- Pessoas cadastradas (usuários e fornecedores dividem o mesmo id) ---");
        for (Pessoa p : Pessoa.listaPessoas) {
            System.out.println("- id " + p.getId() + " | " + p.getNome() + " | " + p.getClass().getSimpleName());
        }

        // 3. CADASTRANDO INSUMOS E ASSOCIANDO FORNECEDORES ------------------------
        Insumo blendCarne = new Insumo(null, "Blend Bovino 180g", "Carne moída fresca", "kg", 10.0, catCarnes);
        blendCarne.cadastrar();
        blendCarne.associarFornecedor(acougue);

        Insumo paoBrioche = new Insumo(null, "Pão Brioche", "Pão selado na manteiga", "unidade", 30.0, catPaes);
        paoBrioche.cadastrar();
        paoBrioche.associarFornecedor(padaria);

        // 4. PEDIDOS DE COMPRA (feitos pelo gerente) ------------------------------
        System.out.println("\n--- Pedidos de Compra ---");
        PedidoCompra pedidoCarne = new PedidoCompra(null, acougue, gerente, "Reposição semanal de carne");
        pedidoCarne.cadastrar();
        pedidoCarne.adicionarItem(blendCarne, 20.0, 32.90);   // 20 kg a R$ 32,90 o kg

        PedidoCompra pedidoPao = new PedidoCompra(null, padaria, gerente, "Pães da semana");
        pedidoPao.cadastrar();
        pedidoPao.adicionarItem(paoBrioche, 50.0, 1.80);      // 50 pães a R$ 1,80

        try {
            pedidoCarne.adicionarItem(paoBrioche, 10.0, 1.80); // o açougue não vende pão
        } catch (IllegalStateException e) {
            System.out.println("Item recusado: " + e.getMessage());
        }

        for (PedidoCompra p : PedidoCompra.listaPedidos) {
            System.out.printf("Pedido %d | %s | %s | Total R$ %.2f%n",
                    p.getId(), p.getFornecedor().getNome(), p.getStatus(), p.calcularValorTotal());
        }

        // 5. RECEBIMENTO (o pedido sempre chega completo) -------------------------
        // Na conferência, o funcionário informa o lote e a validade de cada item.
        System.out.println("\n--- Recebimento dos Pedidos ---");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 2);
        Date venceEm2Dias = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 13);
        Date venceEm15Dias = cal.getTime();

        pedidoCarne.getItens().get(0).definirLote("LOTE-CARNE-01", venceEm15Dias);
        pedidoCarne.receber(funcionario);

        // Código de lote repetido: o sistema recusa e não recebe nada do pedido
        pedidoPao.getItens().get(0).definirLote("lote-carne-01", venceEm2Dias);
        try {
            pedidoPao.receber(funcionario);
        } catch (IllegalStateException e) {
            System.out.println("Recebimento recusado: " + e.getMessage() + " (status continua " + pedidoPao.getStatus() + ")");
        }
        pedidoPao.getItens().get(0).definirLote("LOTE-PAO-01", venceEm2Dias);
        pedidoPao.receber(funcionario);

        for (PedidoCompra p : PedidoCompra.listaPedidos) {
            System.out.println("Pedido " + p.getId() + " -> " + p.getStatus());
        }

        Lote loteCarne = Lote.buscarPorCodigo("LOTE-CARNE-01");
        Lote lotePao = Lote.buscarPorCodigo("LOTE-PAO-01");

        // 6. MOVIMENTANDO O ESTOQUE (Saídas, Perdas e Ajustes) --------------------
        // As entradas já foram feitas pelo recebimento dos pedidos.
        System.out.println("\n--- Registrando Movimentações ---");
        loteCarne.registrarSaida(12.0, funcionario);   // Usou 12 kg (sobram 8 kg -> abaixo do mínimo de 10 kg!)
        lotePao.registrarSaida(15.0, funcionario);     // Usou 15 pães
        lotePao.registrarPerda(3.0, "Pão amassado na entrega", funcionario); // Perdeu 3 pães (sobram 32)

        loteCarne.registrarAjuste(7.5, gerente);       // Balança deu 7.5 kg em vez de 8.0 kg

        try {
            loteCarne.registrarSaida(100.0, funcionario);
        } catch (IllegalStateException e) {
            System.out.println("Saída recusada: " + e.getMessage());
        }

        // 7. CONSULTAS E RELATÓRIOS -----------------------------------------------
        System.out.println("\n--- Saldos Atuais ---");
        System.out.println("Saldo Blend Bovino: " + blendCarne.consultarSaldo() + " " + blendCarne.getUnidadeMedida());
        System.out.println("Saldo Pão Brioche: " + paoBrioche.consultarSaldo() + " " + paoBrioche.getUnidadeMedida());

        System.out.println("\n--- Rastreabilidade dos Lotes ---");
        SimpleDateFormat data = new SimpleDateFormat("dd/MM/yyyy");
        for (Lote lote : Lote.listaLotes) {
            ItemPedidoCompra item = lote.getItemPedido();
            System.out.println("- " + lote.getCodigo() + " | " + lote.getInsumo().getNome()
                    + " | fornecedor: " + lote.getFornecedor().getNome()
                    + " | pedido " + item.getPedido().getId()
                    + " | validade " + data.format(lote.getValidade()));
        }

        System.out.println("\n--- Alerta: Insumos Abaixo do Estoque Mínimo ---");
        for (Insumo item : Insumo.listarEstoqueBaixo()) {
            System.out.println("- " + item.getNome() + " (Atual: " + item.consultarSaldo() + " | Mínimo: " + item.getEstoqueMinimo() + ")");
        }

        System.out.println("\n--- Alerta: Lotes Vencendo nos Próximos 5 Dias ---");
        for (Lote lote : Lote.listarLotesProximosVencimento(5)) {
            System.out.println("- Código: " + lote.getCodigo() + " | Saldo restante: " + lote.consultarSaldo());
        }

        Calendar calInicio = Calendar.getInstance();
        calInicio.add(Calendar.DAY_OF_MONTH, -1);
        Calendar calFim = Calendar.getInstance();
        calFim.add(Calendar.DAY_OF_MONTH, 1);

        System.out.println("\n--- Histórico de Movimentações ---");
        for (MovimentacaoEstoque mov : MovimentacaoEstoque.listarPorPeriodo(calInicio.getTime(), calFim.getTime())) {
            System.out.println("- " + mov.getTipo() + " | " + mov.getQuantidade() + " | "
                    + mov.getLote().getCodigo() + " | por " + mov.getUsuario().getNome());
        }

        System.out.println("\n--- Relatório de Uso (Saídas no Período) ---");
        Map<Insumo, Double> uso = MovimentacaoEstoque.gerarRelatorioUso(calInicio.getTime(), calFim.getTime());
        for (Map.Entry<Insumo, Double> entry : uso.entrySet()) {
            System.out.println("- " + entry.getKey().getNome() + ": " + entry.getValue() + " " + entry.getKey().getUnidadeMedida());
        }

        System.out.println("\n--- Indicadores Gerais (quantidade de movimentações) ---");
        Map<String, Double> indicadores = Insumo.gerarIndicadores(calInicio.getTime(), calFim.getTime());
        System.out.println(indicadores);
    }
}
