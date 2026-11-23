package entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo de quem fornece o quê: resolve o N:N entre Insumo e Fornecedor.
 * É usado para validar os itens de um pedido de compra.
 */
public class InsumoFornecedor {
    private Integer id;
    private Insumo insumo;
    private Fornecedor fornecedor;

    // Vínculos do sistema (equivale à tabela "insumo_fornecedor").
    public static List<InsumoFornecedor> listaVinculos = new ArrayList<>();

    public InsumoFornecedor() {}

    public InsumoFornecedor(Integer id, Insumo insumo, Fornecedor fornecedor) {
        this.id = id;
        this.insumo = insumo;
        this.fornecedor = fornecedor;
    }

    /** true se o insumo já está associado ao fornecedor. */
    public static boolean existeVinculo(Insumo insumo, Fornecedor fornecedor) {
        for (InsumoFornecedor vinculo : listaVinculos) {
            if (vinculo.getInsumo() == insumo && vinculo.getFornecedor() == fornecedor) {
                return true;
            }
        }
        return false;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Insumo getInsumo() { return insumo; }
    public Fornecedor getFornecedor() { return fornecedor; }
}
