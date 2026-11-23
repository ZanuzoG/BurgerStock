package entities;

import java.util.*;

public class MovimentacaoEstoque {
    private Integer id;
    private TipoMovimentacao tipo;
    private String motivo;
    private Double quantidade;
    private Date dataHora;
    private String observacao;

    private Lote lote;
    private Usuario usuario;

    // Movimentações do sistema (equivale à tabela "movimentacoes_estoque").
    public static List<MovimentacaoEstoque> listaMovimentacoes = new ArrayList<>();

    public MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(Integer id, TipoMovimentacao tipo, String motivo, Double quantidade, Date dataHora, String observacao, Lote lote, Usuario usuario) {
        this.id = id;
        this.tipo = tipo;
        this.motivo = motivo;
        this.quantidade = quantidade;
        this.dataHora = dataHora;
        this.observacao = observacao;
        this.lote = lote;
        this.usuario = usuario;
    }

    public static List<MovimentacaoEstoque> listarPorPeriodo(Date inicio, Date fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Informe a data inicial e a data final do período.");
        }
        List<MovimentacaoEstoque> resultado = new ArrayList<>();
        for (MovimentacaoEstoque mov : listaMovimentacoes) {
            if (mov.getDataHora() != null
                    && !mov.getDataHora().before(inicio)
                    && !mov.getDataHora().after(fim)) {
                resultado.add(mov);
            }
        }
        return resultado;
    }

    public static Map<Insumo, Double> gerarRelatorioUso(Date inicio, Date fim) {
        Map<Insumo, Double> relatorio = new HashMap<>();

        for (MovimentacaoEstoque mov : listarPorPeriodo(inicio, fim)) {
            if (mov.getTipo() == TipoMovimentacao.SAIDA
                    && mov.getLote() != null
                    && mov.getLote().getInsumo() != null) {
                Insumo insumo = mov.getLote().getInsumo();
                double atual = relatorio.getOrDefault(insumo, 0.0);
                relatorio.put(insumo, atual + mov.getQuantidade());
            }
        }
        return relatorio;
    }

    // Sem setters: o histórico de movimentações não deve ser alterado depois de registrado.
    public Integer getId() { return id; }
    public TipoMovimentacao getTipo() { return tipo; }
    public String getMotivo() { return motivo; }
    public Double getQuantidade() { return quantidade; }
    public Date getDataHora() { return dataHora; }
    public String getObservacao() { return observacao; }
    public Lote getLote() { return lote; }
    public Usuario getUsuario() { return usuario; }
}
