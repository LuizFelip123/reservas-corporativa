package br.com.estudo.reservas.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservas",
        indexes = {
            @Index(name = "idx_reserva_recurso_periodo", columnList = "recurso_id,inicio,fim"),
            @Index(name = "idx_reserva_usuario", columnList = "usuario_id")
        })
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_id", nullable = false)
    private Recurso recurso;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(nullable = false, length = 20)
    private String status;

    public Reserva() {}

    public Reserva(Recurso recurso, Usuario usuario, LocalDateTime inicio, LocalDateTime fim) {
        this.recurso = recurso;
        this.usuario = usuario;
        this.inicio = inicio;
        this.fim = fim;
        this.status = "PENDENTE";
    }

    public long getId() {
        return id;
    }

    public long getRecursoId() {
        return recurso.getId();
    }

    public String getUsuarioLogin() {
        return usuario.getLogin();
    }

    public String getSolicitante() {
        return usuario.getNome();
    }

    public String getInicio() {
        return inicio.toString();
    }

    public String getFim() {
        return fim.toString();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonbTransient
    public Recurso getRecurso() {
        return recurso;
    }

    @JsonbTransient
    public Usuario getUsuario() {
        return usuario;
    }
}
