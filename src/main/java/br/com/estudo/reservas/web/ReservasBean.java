package br.com.estudo.reservas.web;

import br.com.estudo.reservas.dto.NovaReserva;
import br.com.estudo.reservas.exception.RegraNegocioException;
import br.com.estudo.reservas.model.Recurso;
import br.com.estudo.reservas.model.Reserva;
import br.com.estudo.reservas.security.SessaoUsuario;
import br.com.estudo.reservas.service.ReservasService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

@Named("reservasBean")
@RequestScoped
public class ReservasBean {
    @Inject private ReservasService service;
    @Inject private SessaoUsuario sessao;
    private String nome;
    private String tipo = "SALA";
    private String solicitante;
    private String inicio;
    private String fim;
    private Long recursoId;

    public List<Recurso> getRecursos() {
        return service.listarRecursos(sessao.getUsuario());
    }

    public List<Reserva> getReservas() {
        return service.listarReservas(sessao.getUsuario());
    }

    public long getPendentes() {
        return service.listarReservas(sessao.getUsuario()).stream()
                .filter(reserva -> "PENDENTE".equals(reserva.getStatus()))
                .count();
    }

    public long getAprovadas() {
        return service.listarReservas(sessao.getUsuario()).stream()
                .filter(reserva -> "APROVADA".equals(reserva.getStatus()))
                .count();
    }

    public String nomeRecurso(long id) {
        return service.listarRecursos(sessao.getUsuario()).stream()
                .filter(recurso -> recurso.getId() == id)
                .map(Recurso::getNome)
                .findFirst()
                .orElse("Recurso removido");
    }

    public void cadastrar() {
        executar(
                () -> {
                    service.cadastrarRecurso(sessao.getUsuario(), new Recurso(0, nome, tipo));
                    nome = null;
                },
                "Recurso cadastrado.");
    }

    public void reservar() {
        executar(
                () -> {
                    NovaReserva reserva =
                            new NovaReserva(recursoId == null ? 0 : recursoId, inicio, fim);
                    service.solicitarReserva(sessao.getUsuario(), reserva);
                },
                "Reserva solicitada. Aguarde a aprovação.");
    }

    public void consultar() {
        executar(
                () -> {
                    boolean disponivel =
                            service.consultarDisponibilidade(
                                    sessao.getUsuario(),
                                    recursoId == null ? 0 : recursoId,
                                    inicio,
                                    fim);
                    mensagem(
                            disponivel ? FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_WARN,
                            disponivel
                                    ? "Recurso disponível nesse horário."
                                    : "Recurso ocupado nesse horário.");
                },
                null);
    }

    public void aprovar(long id) {
        executar(() -> service.aprovarReserva(sessao.getUsuario(), id), "Reserva aprovada.");
    }

    public void cancelar(long id) {
        executar(
                () -> service.cancelarReserva(sessao.getUsuario(), id),
                "Reserva cancelada. O horário foi liberado.");
    }

    private void executar(Runnable acao, String sucesso) {
        try {
            acao.run();
            if (sucesso != null) {
                mensagem(FacesMessage.SEVERITY_INFO, sucesso);
            }
        } catch (RegraNegocioException exception) {
            mensagem(FacesMessage.SEVERITY_ERROR, exception.getMessage());
        }
    }

    private void mensagem(FacesMessage.Severity nivel, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(nivel, texto, null));
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getSolicitante() {
        return sessao.getUsuario().getNome();
    }

    public String getInicio() {
        return inicio;
    }

    public void setInicio(String inicio) {
        this.inicio = inicio;
    }

    public String getFim() {
        return fim;
    }

    public void setFim(String fim) {
        this.fim = fim;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(Long recursoId) {
        this.recursoId = recursoId;
    }
}
