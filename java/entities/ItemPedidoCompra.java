package entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Cada insumo de um pedido de compra, com quantidade e valor unitário.
 * Resolve o N:N entre PedidoCompra e Insumo. Quando o pedido chega, o item gera um lote.
 */
public class ItemPedidoCompra {
    private Integer id;
    private double quantidade;      // na mesma unidade de medida do estoque
    private double valorUnitario;

    private PedidoCompra pedido;
    private Insumo insumo;
    private Lote lote; // fica vazio até a conferência da entrega

    // Itens de todos os pedidos (equivale à tabela "itens_pedido_compra").
    public static List<ItemPedidoCompra> listaItens = new ArrayList<>();

    public ItemPedidoCompra() {
    }

    public ItemPedidoCompra(Integer id, PedidoCompra pedido, Insumo insumo, double quantidade, double valorUnitario) {
        this.id = id;
        this.pedido = pedido;
        this.insumo = insumo;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
    }

    /** Conferência da entrega: informa o código e a validade do lote deste item. */
    public void definirLote(String codigo, Date validade) {
        if (this.pedido == null || this.pedido.getStatus() != StatusPedido.ABERTO) {
            throw new IllegalStateException("Só é possível informar o lote de um pedido em aberto.");
        }
        this.lote = new Lote(null, codigo, validade, this);
    }

    public double calcularSubtotal() {
        return this.quantidade * this.valorUnitario;
    }

    public Integer getId() { return id; }
    public double getQuantidade() { return quantidade; }
    public double getValorUnitario() { return valorUnitario; }
    public PedidoCompra getPedido() { return pedido; }
    public Insumo getInsumo() { return insumo; }
    public Lote getLote() { return lote; }
}
