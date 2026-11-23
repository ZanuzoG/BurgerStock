package entities;

/** Situação de um pedido de compra. O pedido sempre chega completo: ABERTO -> RECEBIDO. */
public enum StatusPedido {
    ABERTO,
    RECEBIDO,
    CANCELADO
}
