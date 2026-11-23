package entities;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Pessoa que acessa o sistema (pessoa física). Herda id, nome, e-mail e telefone de Pessoa. */
public class Usuario extends Pessoa {

    // Ações que o FUNCIONARIO pode executar (funções operacionais).
    // O GERENTE pode executar qualquer ação.
    private static final Set<String> ACOES_FUNCIONARIO = Set.of(
            "RECEBER_PEDIDO",
            "REGISTRAR_ENTRADA",
            "REGISTRAR_SAIDA",
            "REGISTRAR_PERDA",
            "CONSULTAR_SALDO",
            "CONSULTAR_ALERTAS"
    );

    private static final int TAMANHO_MINIMO_SENHA = 6;
    private static final String CARACTERES_SENHA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    // Mesma codificação das colunas VARCHAR do banco (collation Latin1_General),
    // para o hash de uma senha com acento ficar igual ao do HASHBYTES.
    private static final Charset CODIFICACAO_BANCO = Charset.forName("windows-1252");

    // Usuários do sistema (equivale à tabela "usuarios").
    public static List<Usuario> listaUsuarios = new ArrayList<>();

    private String cpf;
    private String senhaHash; // SHA-256 da senha, como na coluna usuarios.senha_hash
    private Perfil perfil;
    private boolean ativo;

    public Usuario() {
        super();
        this.ativo = false;
    }

    /** A senha chega como foi digitada; criarAcesso() confere o tamanho e guarda só o hash. */
    public Usuario(Integer id, String nome, String cpf, String email, String telefone,
                   String senha, Perfil perfil, boolean ativo) {
        super(id, nome, email, telefone);
        this.cpf = cpf;
        this.senhaHash = senha;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return this.ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    // ---------------------------------------------------------------
    // Criação de acessos e cadastre-se
    // ---------------------------------------------------------------

    /**
     * Criação de acesso feita pelo GERENTE: o gerente escolhe o perfil.
     * Valida os dados, impede CPF e e-mail repetidos, guarda a senha como hash,
     * ativa o usuário e guarda nas listas.
     */
    public void criarAcesso() {
        if (getEmail() == null || getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Informe o e-mail.");
        }
        String cpfLimpo = somenteDigitos(this.cpf);
        if (cpfLimpo == null || cpfLimpo.length() != 11) {
            throw new IllegalArgumentException("O CPF precisa ter 11 dígitos.");
        }
        if (buscarPorCpf(cpfLimpo) != null) {
            throw new IllegalStateException("Já existe um usuário com este CPF.");
        }
        validarSenha(this.senhaHash);
        if (this.perfil == null) {
            throw new IllegalArgumentException("Informe o perfil de acesso.");
        }
        registrar(); // dados de Pessoa: nome, e-mail único e id
        this.cpf = cpfLimpo;
        this.senhaHash = gerarHash(this.senhaHash);
        this.ativo = true;
        listaUsuarios.add(this);
    }

    /** Cadastre-se: a própria pessoa cria a conta. Todo novo cadastro entra como FUNCIONARIO. */
    public static Usuario cadastrarSe(String nome, String cpf, String email, String senha) {
        Usuario novo = new Usuario(null, nome, cpf, email, null, senha, Perfil.FUNCIONARIO, false);
        novo.criarAcesso();
        return novo;
    }

    // ---------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------

    /** Procura um usuário pelo e-mail (sem diferenciar maiúsculas e minúsculas). */
    public static Usuario buscarPorEmail(String email) {
        if (email == null) {
            return null;
        }
        for (Usuario usuario : listaUsuarios) {
            if (usuario.getEmail() != null && usuario.getEmail().equalsIgnoreCase(email.trim())) {
                return usuario;
            }
        }
        return null;
    }

    /** Login: devolve o usuário quando o e-mail e a senha conferem. */
    public static Usuario fazerLogin(String email, String senha) {
        Usuario usuario = buscarPorEmail(email);
        if (usuario == null || !usuario.autenticar(senha)) {
            // A mensagem não diz qual dos dois está errado, para não revelar quais e-mails existem.
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }
        return usuario;
    }

    /** Confere se o hash da senha digitada é igual ao hash guardado; usuário inativo não é autenticado. */
    public boolean autenticar(String senha) {
        return this.ativo
                && senha != null
                && this.senhaHash != null
                && this.senhaHash.equals(gerarHash(senha));
    }

    // ---------------------------------------------------------------
    // Meu perfil (todo usuário altera os próprios dados)
    // ---------------------------------------------------------------

    /** Altera nome, e-mail e telefone do próprio usuário. CPF e perfil de acesso NÃO mudam aqui. */
    public void editarPerfil(Usuario novosDados) {
        if (novosDados == null) {
            return;
        }
        if (novosDados.getEmail() != null && !novosDados.getEmail().trim().isEmpty()) {
            if (emailEmUso(novosDados.getEmail(), this)) {
                throw new IllegalStateException("Este e-mail já está em uso.");
            }
            setEmail(novosDados.getEmail().trim());
        }
        if (novosDados.getNome() != null && !novosDados.getNome().trim().isEmpty()) {
            setNome(novosDados.getNome().trim());
        }
        if (novosDados.getTelefone() != null) {
            String telefone = novosDados.getTelefone().trim();
            setTelefone(telefone.isEmpty() ? null : telefone);
        }
    }

    /** Troca a senha: exige a senha atual correta e uma nova senha válida. */
    public void alterarSenha(String senhaAtual, String novaSenha) {
        if (!autenticar(senhaAtual)) {
            throw new IllegalArgumentException("A senha atual não confere.");
        }
        validarSenha(novaSenha);
        this.senhaHash = gerarHash(novaSenha);
    }

    /** Só um GERENTE ativo pode alterar o perfil de acesso de um usuário. */
    public void alterarTipoAcesso(Perfil novoPerfil, Usuario gerente) {
        if (gerente == null || !gerente.isAtivo() || gerente.getPerfil() != Perfil.GERENTE) {
            throw new IllegalStateException("Somente um gerente pode alterar o perfil de acesso.");
        }
        if (novoPerfil == null) {
            throw new IllegalArgumentException("Informe o novo perfil de acesso.");
        }
        this.perfil = novoPerfil;
    }

    // ---------------------------------------------------------------
    // Lembrete de senha
    // ---------------------------------------------------------------

    /**
     * Lembrete de senha: gera uma senha provisória para o e-mail informado, guarda o hash
     * e devolve a senha para ser enviada ao usuário (o envio do e-mail é simulado).
     * Devolve null se o e-mail não for de um usuário ativo.
     */
    public static String solicitarLembreteSenha(String email) {
        Usuario usuario = buscarPorEmail(email);
        if (usuario == null || !usuario.isAtivo()) {
            return null;
        }
        StringBuilder provisoria = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            provisoria.append(CARACTERES_SENHA.charAt(ALEATORIO.nextInt(CARACTERES_SENHA.length())));
        }
        usuario.senhaHash = gerarHash(provisoria.toString());
        return provisoria.toString();
    }

    // ---------------------------------------------------------------
    // Permissões
    // ---------------------------------------------------------------

    /** GERENTE pode tudo; FUNCIONARIO só as ações operacionais; usuário inativo não pode nada. */
    public boolean temPermissao(String acao) {
        if (!this.ativo || this.perfil == null || acao == null) {
            return false;
        }
        if (this.perfil == Perfil.GERENTE) {
            return true;
        }
        return ACOES_FUNCIONARIO.contains(acao);
    }

    private static Usuario buscarPorCpf(String cpfSomenteDigitos) {
        for (Usuario usuario : listaUsuarios) {
            if (cpfSomenteDigitos.equals(usuario.getCpf())) {
                return usuario;
            }
        }
        return null;
    }

    /**
     * SHA-256 da senha em hexadecimal maiúsculo, igual ao que o banco grava com
     * CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', senha), 2).
     */
    private static String gerarHash(String senha) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(senha.getBytes(CODIFICACAO_BANCO));
            StringBuilder hex = new StringBuilder();
            for (byte b : resumo) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não está disponível nesta versão do Java.", e);
        }
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A senha precisa ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
    }
}
