package entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pedido de compra feito a um fornecedor. Os itens ficam em ItemPedidoCompra.
 * O pedido sempre chega completo: ao receber, cada item vira um lote e gera uma ENTRADA.
 */
public class PedidoCompra {
    private Integer id;
    private Date dataPedido;
    private Date dataRecebimento;
    private StatusPedido status;
    private String observacao;

    private Fornecedor fornecedor;
    private Usuario usuario; // quem fez o pedido
    private List<ItemPedidoCompra> itens = new ArrayList<>();

    // Pedidos do sistema (equivale à tabela "pedidos_compra").
    public static List<PedidoCompra> listaPedidos = new ArrayList<>();

    public PedidoCompra() {
    }

    public PedidoCompra(Integer id, Fornecedor fornecedor, Usuario usuario, String observacao) {
        this.id = id;
        this.fornecedor = fornecedor;
        this.usuario = usuario;
        this.observacao = observacao;
    }

    /** Registra o pedido em aberto, com a data de hoje. */
    public void cadastrar() {
        if (listaPedidos.contains(this)) {
            throw new IllegalStateException("Este pedido já foi cadastrado.");
        }
        if (this.fornecedor == null || !this.fornecedor.isAtivo()
                || !Fornecedor.listaFornecedores.contains(this.fornecedor)) {
            throw new IllegalArgumentException("Informe um fornecedor ativo.");
        }
        if (this.usuario == null || !Usuario.listaUsuarios.contains(this.usuario)) {
            throw new IllegalArgumentException("Informe o usuário que fez o pedido.");
        }
        if (!this.usuario.temPermissao("FAZER_PEDIDO_COMPRA")) {
            throw new IllegalStateException("Somente um gerente pode fazer pedido de compra.");
        }
        if (this.id == null) {
            this.id = listaPedidos.size() + 1;
        }
        this.dataPedido = new Date();
        this.status = StatusPedido.ABERTO;
        listaPedidos.add(this);
    }

    /** Adiciona um insumo ao pedido. O insumo precisa ser fornecido por este fornecedor. */
    public void adicionarItem(Insumo insumo, double quantidade, double valorUnitario) {
        if (this.status != StatusPedido.ABERTO) {
            throw new IllegalStateException("Só é possível adicionar itens a um pedido em aberto.");
        }
        if (insumo == null) {
            throw new IllegalArgumentException("Informe o insumo.");
        }
        if (!InsumoFornecedor.existeVinculo(insumo, this.fornecedor)) {
            throw new IllegalStateException("O insumo " + insumo.getNome()
                    + " não está associado ao fornecedor " + this.fornecedor.getNome() + ".");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (valorUnitario <= 0) {
            throw new IllegalArgumentException("O valor unitário deve ser maior que zero.");
        }
        for (ItemPedidoCompra item : this.itens) {
            if (item.getInsumo() == insumo) {
                throw new IllegalStateException("O insumo " + insumo.getNome() + " já está neste pedido.");
            }
        }
        ItemPedidoCompra item = new ItemPedidoCompra(
                ItemPedidoCompra.listaItens.size() + 1, this, insumo, quantidade, valorUnitario);
        this.itens.add(item);
        ItemPedidoCompra.listaItens.add(item);
    }

    /**
     * Recebe o pedido completo. Antes, cada item precisa ter o lote informado (definirLote).
     * Confere tudo primeiro e só depois cadastra os lotes e registra as entradas,
     * para não deixar o pedido recebido pela metade se algo estiver errado.
     */
    public void receber(Usuario recebedor) {
        if (this.status != StatusPedido.ABERTO) {
            throw new IllegalStateException("Só é possível receber um pedido em aberto.");
        }
        if (recebedor == null) {
            throw new IllegalArgumentException("Informe quem recebeu o pedido.");
        }
        if (this.itens.isEmpty()) {
            throw new IllegalStateException("O pedido não tem itens.");
        }

        // 1) Confere todos os itens antes de mudar qualquer coisa
        Set<String> codigosDoPedido = new HashSet<>();
        for (ItemPedidoCompra item : this.itens) {
            Lote lote = item.getLote();
            String nomeInsumo = item.getInsumo().getNome();
            if (lote == null) {
                throw new IllegalStateException("Informe o lote e a validade do item " + nomeInsumo + ".");
            }
            if (lote.getCodigo() == null || lote.getCodigo().trim().isEmpty()) {
                throw new IllegalArgumentException("Informe o código do lote do item " + nomeInsumo + ".");
            }
            if (lote.getValidade() == null) {
                throw new IllegalArgumentException("Informe a validade do lote do item " + nomeInsumo + ".");
            }
            String codigo = lote.getCodigo().trim().toUpperCase();
            if (Lote.buscarPorCodigo(codigo) != null) {
                throw new IllegalStateException("Já existe um lote com o código " + lote.getCodigo().trim() + ".");
            }
            if (!codigosDoPedido.add(codigo)) {
                throw new IllegalStateException("O código " + lote.getCodigo().trim() + " aparece em dois itens deste pedido.");
            }
        }

        // 2) Cadastra os lotes e registra a ENTRADA de cada item
        for (ItemPedidoCompra item : this.itens) {
            item.getLote().cadastrar();
            item.getLote().registrarEntrada(item.getQuantidade(), recebedor);
        }
        this.status = StatusPedido.RECEBIDO;
        this.dataRecebimento = new Date();
    }

    /** Cancela um pedido que ainda não chegou. */
    public void cancelar() {
        if (this.status != StatusPedido.ABERTO) {
            throw new IllegalStateException("Só é possível cancelar um pedido em aberto.");
        }
        this.status = StatusPedido.CANCELADO;
    }

    /** Valor total = soma dos subtotais dos itens (é calculado, não é guardado). */
    public double calcularValorTotal() {
        double total = 0.0;
        for (ItemPedidoCompra item : this.itens) {
            total += item.calcularSubtotal();
        }
        return total;
    }

    public Integer getId() { return id; }
    public Date getDataPedido() { return dataPedido; }
    public Date getDataRecebimento() { return dataRecebimento; }
    public StatusPedido getStatus() { return status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Fornecedor getFornecedor() { return fornecedor; }
    public Usuario getUsuario() { return usuario; }
    public List<ItemPedidoCompra> getItens() { return Collections.unmodifiableList(itens); }
}
