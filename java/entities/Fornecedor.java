package entities;

import java.util.ArrayList;
import java.util.List;

/** Empresa que fornece insumos (pessoa jurídica). Herda id, nome, e-mail e telefone de Pessoa. */
public class Fornecedor extends Pessoa {

    // Fornecedores do sistema (equivale à tabela "fornecedores").
    public static List<Fornecedor> listaFornecedores = new ArrayList<>();

    private String cnpj;
    private boolean ativo;

    public Fornecedor() {
        super();
        this.ativo = false;
    }

    public Fornecedor(Integer id, String nome, String cnpj, String telefone, String email) {
        super(id, nome, email, telefone);
        this.cnpj = cnpj;
        this.ativo = false;
    }

    /** Valida o CNPJ (14 dígitos, sem repetir), registra os dados de Pessoa e ativa o fornecedor. */
    public void cadastrar() {
        String cnpjLimpo = somenteDigitos(this.cnpj);
        if (cnpjLimpo == null || cnpjLimpo.length() != 14) {
            throw new IllegalArgumentException("O CNPJ precisa ter 14 dígitos.");
        }
        for (Fornecedor fornecedor : listaFornecedores) {
            if (cnpjLimpo.equals(fornecedor.getCnpj())) {
                throw new IllegalStateException("Já existe um fornecedor com este CNPJ.");
            }
        }
        registrar(); // dados de Pessoa: nome, e-mail único e id
        this.cnpj = cnpjLimpo;
        this.ativo = true;
        listaFornecedores.add(this);
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public boolean isAtivo() {
        return this.ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
