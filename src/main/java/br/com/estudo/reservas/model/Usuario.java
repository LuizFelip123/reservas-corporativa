package br.com.estudo.reservas.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "login"))
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 80)
    private String login;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    public Usuario() {}

    public Usuario(String login, String nome, Perfil perfil) {
        this.login = login;
        this.nome = nome;
        this.perfil = perfil;
    }

    public long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getNome() {
        return nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }

    @JsonbTransient
    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Usuario identidade() {
        Usuario identidade = new Usuario(login, nome, perfil);
        identidade.id = id;
        return identidade;
    }
}
