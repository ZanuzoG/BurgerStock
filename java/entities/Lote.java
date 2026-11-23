package entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lote recebido em um pedido de compra. Cada item de pedido gera exatamente um lote,
 * e o código do lote é único no sistema inteiro.
 * O insumo e o fornecedor do lote vêm pelo item do pedido (sem guardar duas vezes).
 */
public class Lote {
    private Integer id;
    private String codigo;
    private Date validade;

    private ItemPedidoCompra itemPedido;

    // Lotes do sistema (equivale à tabela "lotes").
    public static List<Lote> listaLotes = new ArrayList<>();

    public Lote() {
    }

    public Lote(Integer id, String codigo, Date validade, ItemPedidoCompra itemPedido) {
        this.id = id;
        this.codigo = codigo;
        this.validade = validade;
        this.itemPedido = itemPedido;
    }

    /** Valida e guarda o lote. É chamado pelo PedidoCompra.receber(). */
    public void cadastrar() {
        if (this.codigo == null || this.codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("Informe o código do lote.");
        }
        if (this.validade == null) {
            throw new IllegalArgumentException("Informe a validade do lote " + this.codigo.trim() + ".");
        }
        if (this.itemPedido == null) {
            throw new IllegalArgumentException("O lote precisa estar ligado a um item de pedido de compra.");
        }
        if (listaLotes.contains(this)) {
            throw new IllegalStateException("Este lote já foi cadastrado.");
        }
        if (buscarPorCodigo(this.codigo) != null) {
            throw new IllegalStateException("Já existe um lote com o código " + this.codigo.trim() + ".");
        }
        this.codigo = this.codigo.trim();
        if (this.id == null) {
            this.id = listaLotes.size() + 1;
        }
        listaLotes.add(this);
    }

    /** Procura um lote pelo código (sem diferenciar maiúsculas e minúsculas). */
    public static Lote buscarPorCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Lote lote : listaLotes) {
            if (lote.getCodigo() != null && lote.getCodigo().equalsIgnoreCase(codigo.trim())) {
                return lote;
            }
        }
        return null;
    }

    /** Insumo deste lote (vem do item do pedido). */
    public Insumo getInsumo() {
        return this.itemPedido == null ? null : this.itemPedido.getInsumo();
    }

    /** Fornecedor deste lote (vem do pedido de compra). Serve para a rastreabilidade. */
    public Fornecedor getFornecedor() {
        if (this.itemPedido == null || this.itemPedido.getPedido() == null) {
            return null;
        }
        return this.itemPedido.getPedido().getFornecedor();
    }

    /** O saldo não é guardado: é calculado somando as movimentações deste lote. */
    public double consultarSaldo() {
        double saldo = 0.0;
        for (MovimentacaoEstoque mov : MovimentacaoEstoque.listaMovimentacoes) {
            if (mov.getLote() == this && mov.getTipo() != null) {
                switch (mov.getTipo()) {
                    case ENTRADA:
                        saldo += mov.getQuantidade();
                        break;
                    case SAIDA:
                    case PERDA:
                        saldo -= mov.getQuantidade();
                        break;
                    case AJUSTE:
                        saldo += mov.getQuantidade(); // no ajuste a quantidade já vem positiva ou negativa
                        break;
                }
            }
        }
        return saldo;
    }

    public boolean estaProximoDoVencimento(int dias) {
        if (this.validade == null) return false;

        long agora = new Date().getTime();
        long limite = agora + ((long) dias * 24 * 60 * 60 * 1000); // converte dias para milissegundos

        return this.validade.getTime() <= limite;
    }

    public void registrarEntrada(double quantidade, Usuario usuario) {
        validarQuantidade(quantidade);
        registrar(TipoMovimentacao.ENTRADA, "Recebimento de pedido de compra", quantidade, usuario);
    }

    public void registrarSaida(double quantidade, Usuario usuario) {
        validarQuantidade(quantidade);
        validarSaldo(quantidade);
        registrar(TipoMovimentacao.SAIDA, "Consumo na produção", quantidade, usuario);
    }

    public void registrarPerda(double quantidade, String motivo, Usuario usuario) {
        validarQuantidade(quantidade);
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Informe o motivo da perda.");
        }
        validarSaldo(quantidade);
        registrar(TipoMovimentacao.PERDA, motivo, quantidade, usuario);
    }

    public void registrarAjuste(double quantidadeContada, Usuario usuario) {
        double diferenca = quantidadeContada - consultarSaldo();
        if (diferenca != 0) {
            registrar(TipoMovimentacao.AJUSTE, "Inventário físico", diferenca, usuario);
        }
    }

    public static List<Lote> listarLotesProximosVencimento(int dias) {
        List<Lote> proximos = new ArrayList<>();
        for (Lote lote : listaLotes) {
            if (lote.consultarSaldo() > 0 && lote.estaProximoDoVencimento(dias)) {
                proximos.add(lote);
            }
        }
        return proximos;
    }

    private void validarQuantidade(double quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
    }

    private void validarSaldo(double quantidade) {
        double saldo = consultarSaldo();
        if (saldo < quantidade) {
            throw new IllegalStateException("Saldo insuficiente no lote " + codigo
                    + ": disponível " + saldo + ", solicitado " + quantidade + ".");
        }
    }

    private void registrar(TipoMovimentacao tipo, String motivo, double quantidade, Usuario usuario) {
        if (!listaLotes.contains(this)) {
            throw new IllegalStateException("O lote precisa estar cadastrado para ser movimentado.");
        }
        if (usuario == null || !Usuario.listaUsuarios.contains(usuario)) {
            throw new IllegalArgumentException("Informe o usuário responsável pela movimentação.");
        }
        // REGISTRAR_ENTRADA, REGISTRAR_SAIDA, REGISTRAR_PERDA ou REGISTRAR_AJUSTE (este só o gerente)
        if (!usuario.temPermissao("REGISTRAR_" + tipo.name())) {
            throw new IllegalStateException("Seu perfil não permite registrar " + tipo.name() + ".");
        }
        MovimentacaoEstoque mov = new MovimentacaoEstoque(
                null, tipo, motivo, quantidade, new Date(), null, this, usuario
        );
        MovimentacaoEstoque.listaMovimentacoes.add(mov);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Date getValidade() { return validade; }
    public void setValidade(Date validade) { this.validade = validade; }
    public ItemPedidoCompra getItemPedido() { return itemPedido; }
}
