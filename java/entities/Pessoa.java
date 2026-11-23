package entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Dados comuns a usuários e fornecedores (generalização).
 * É abstrata: no sistema toda pessoa é um Usuario ou um Fornecedor.
 * No banco de dados vira a tabela "pessoas"; "usuarios" e "fornecedores" usam o mesmo id.
 */
public abstract class Pessoa {

    // Todas as pessoas do sistema (equivale à tabela "pessoas").
    public static List<Pessoa> listaPessoas = new ArrayList<>();

    private Integer id;
    private String nome;
    private String email;
    private String telefone;

    public Pessoa() {
    }

    public Pessoa(Integer id, String nome, String email, String telefone) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    /**
     * Valida os dados comuns, gera o id e guarda a pessoa na lista.
     * É chamado por Usuario.criarAcesso() e Fornecedor.cadastrar(), depois que eles
     * validam os próprios dados.
     */
    protected void registrar() {
        if (listaPessoas.contains(this)) {
            throw new IllegalStateException("Este cadastro já foi feito.");
        }
        if (this.nome == null || this.nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Informe o nome.");
        }
        if (this.email != null && this.email.trim().isEmpty()) {
            this.email = null;
        }
        if (this.email != null && emailEmUso(this.email, this)) {
            throw new IllegalStateException("Este e-mail já está em uso.");
        }
        this.nome = this.nome.trim();
        if (this.email != null) {
            this.email = this.email.trim();
        }
        if (this.telefone != null && this.telefone.trim().isEmpty()) {
            this.telefone = null;
        }
        if (this.id == null) {
            this.id = listaPessoas.size() + 1;
        }
        listaPessoas.add(this);
    }

    /** true se o e-mail já pertence a outra pessoa (sem diferenciar maiúsculas e minúsculas). */
    public static boolean emailEmUso(String email, Pessoa ignorar) {
        if (email == null) {
            return false;
        }
        for (Pessoa pessoa : listaPessoas) {
            if (pessoa != ignorar && pessoa.getEmail() != null
                    && pessoa.getEmail().equalsIgnoreCase(email.trim())) {
                return true;
            }
        }
        return false;
    }

    /** Remove pontos, traços, barras e espaços: "111.222.333-44" vira "11122233344". */
    protected static String somenteDigitos(String texto) {
        return texto == null ? null : texto.replaceAll("\\D", "");
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
