package entities;

import java.util.ArrayList;
import java.util.List;

public class Categoria {

    private Integer id;
    private String nome;
    private String descricao;

    // Categorias do sistema (equivale à tabela "categorias").
    public static List<Categoria> listaCategorias = new ArrayList<>();

    public Categoria() {
        this.id = null;
        this.nome = null;
        this.descricao = null;
    }

    public Categoria(Integer id, String nome, String descricao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
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

    /** Valida o nome (obrigatório e sem repetir), gera o id e guarda a categoria na lista. */
    public void cadastrar() {
        if (listaCategorias.contains(this)) {
            throw new IllegalStateException("Esta categoria já foi cadastrada.");
        }
        if (this.nome == null || this.nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório.");
        }
        for (Categoria categoria : listaCategorias) {
            if (categoria.getNome().equalsIgnoreCase(this.nome.trim())) {
                throw new IllegalStateException("Já existe uma categoria com o nome " + this.nome.trim() + ".");
            }
        }
        this.nome = this.nome.trim();
        if (this.descricao != null && this.descricao.trim().isEmpty()) {
            this.descricao = null;
        }
        if (this.id == null) {
            this.id = listaCategorias.size() + 1;
        }
        listaCategorias.add(this);
    }
}
