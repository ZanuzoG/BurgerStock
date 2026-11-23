package entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Insumo {
    private Integer id;
    private String nome;
    private String descricao;
    private String unidadeMedida;
    private Double estoqueMinimo;
    private boolean ativo;
    private Categoria categoria;

    public static List<Insumo> listaInsumos = new ArrayList<>();

    public Insumo() {
        this.id = null;
        this.nome = null;
        this.descricao = null;
        this.unidadeMedida = null;
        this.estoqueMinimo = null;
        this.ativo = false;
        this.categoria = new Categoria();
    }

    public Insumo(Integer id, String nome, String descricao, String unidadeMedida, Double estoqueMinimo, Categoria categoria) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.unidadeMedida = unidadeMedida;
        this.estoqueMinimo = estoqueMinimo;
        this.ativo = false;
        this.categoria = categoria;
    }

    // Métodos de negócio do UML
    public void cadastrar() {
        if (this.nome != null && !this.nome.trim().isEmpty()) {
            this.ativo = true;
            listaInsumos.add(this);
        }
    }

    /** Vincula este insumo a um fornecedor. Não permite repetir a mesma combinação. */
    public void associarFornecedor(Fornecedor fornecedor) {
        if (fornecedor != null) {
            if (InsumoFornecedor.existeVinculo(this, fornecedor)) {
                throw new IllegalStateException("Este insumo já está associado a este fornecedor.");
            }
            InsumoFornecedor vinculo = new InsumoFornecedor(null, this, fornecedor);
            InsumoFornecedor.listaVinculos.add(vinculo);
        }
    }

    public double consultarSaldo() {
        double total = 0.0;
        for (Lote lote : Lote.listaLotes) {
            if (lote.getInsumo() == this) {
                total += lote.consultarSaldo();
            }
        }
        return total;
    }

    public boolean estaAbaixoDoMinimo() {
        return this.estoqueMinimo != null && this.consultarSaldo() < this.estoqueMinimo;
    }

    public static List<Insumo> listarEstoqueBaixo() {
        List<Insumo> baixos = new ArrayList<>();
        for (Insumo insumo : listaInsumos) {
            if (insumo.isAtivo() && insumo.estaAbaixoDoMinimo()) {
                baixos.add(insumo);
            }
        }
        return baixos;
    }

    /**
     * Conta quantas movimentações de cada tipo houve no período.
     * Conta registros em vez de somar quantidades, porque somar misturaria kg com unidades.
     */
    public static Map<String, Double> gerarIndicadores(Date inicio, Date fim) {
        Map<String, Double> indicadores = new HashMap<>();
        double entradas = 0, saidas = 0, perdas = 0;

        for (MovimentacaoEstoque mov : MovimentacaoEstoque.listarPorPeriodo(inicio, fim)) {
            if (mov.getTipo() == TipoMovimentacao.ENTRADA) entradas++;
            else if (mov.getTipo() == TipoMovimentacao.SAIDA) saidas++;
            else if (mov.getTipo() == TipoMovimentacao.PERDA) perdas++;
        }

        indicadores.put("Entradas", entradas);
        indicadores.put("Saidas", saidas);
        indicadores.put("Perdas", perdas);
        return indicadores;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(String unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public Double getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(Double estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public boolean isAtivo() {
        return this.ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
}
