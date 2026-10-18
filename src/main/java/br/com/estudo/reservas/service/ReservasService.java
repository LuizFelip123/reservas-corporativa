package br.com.estudo.reservas.service;

import br.com.estudo.reservas.dto.NovaReserva;
import br.com.estudo.reservas.exception.RegraNegocioException;
import br.com.estudo.reservas.model.*;
import br.com.estudo.reservas.repository.ReservasRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@ApplicationScoped
@Transactional
public class ReservasService {
    @Inject private ReservasRepository repository;

    public ReservasService() {}

    public ReservasService(ReservasRepository repository) {
        this.repository = repository;
    }

    public List<Recurso> listarRecursos(Usuario usuario) {
        exigirAutenticado(usuario);
        return repository.listarRecursos();
    }

    public Recurso cadastrarRecurso(Usuario usuario, Recurso entrada) {
        exigirAdmin(usuario);
        if (entrada == null
                || entrada.getNome() == null
                || entrada.getNome().isBlank()
                || entrada.getNome().trim().length() > 120
                || !("SALA".equals(entrada.getTipo()) || "EQUIPAMENTO".equals(entrada.getTipo()))) {
            throw new RegraNegocioException(
                    400, "Informe nome de até 120 caracteres e tipo SALA ou EQUIPAMENTO.");
        }
        Recurso recurso = new Recurso(0, entrada.getNome().trim(), entrada.getTipo());
        repository.salvar(recurso);
        return recurso;
    }

    public List<Reserva> listarReservas(Usuario usuario) {
        exigirAutenticado(usuario);
        return repository.listarReservas(usuario);
    }

    public boolean consultarDisponibilidade(
            Usuario usuario, long recursoId, String inicio, String fim) {
        exigirAutenticado(usuario);
        exigirRecurso(recursoId, false);
        LocalDateTime de = converterData(inicio), ate = converterData(fim);
        validarPeriodo(de, ate);
        return !repository.existeConflito(recursoId, de, ate);
    }

    public Reserva solicitarReserva(Usuario usuario, NovaReserva entrada) {
        exigirAutenticado(usuario);
        if (entrada == null) {
            throw new RegraNegocioException(400, "Informe os dados da reserva.");
        }
        LocalDateTime de = converterData(entrada.getInicio()),
                ate = converterData(entrada.getFim());
        validarPeriodo(de, ate);
        // O bloqueio da linha do recurso serializa solicitações concorrentes no MySQL.
        Recurso recurso = exigirRecurso(entrada.getRecursoId(), true);
        if (repository.existeConflito(recurso.getId(), de, ate)) {
            throw new RegraNegocioException(409, "Recurso ocupado nesse horário.");
        }
        Usuario solicitante = repository.buscarUsuario(usuario.getLogin());
        if (solicitante == null) {
            throw new RegraNegocioException(401, "Usuário não encontrado. Entre novamente.");
        }
        Reserva reserva = new Reserva(recurso, solicitante, de, ate);
        repository.salvar(reserva);
        return reserva;
    }

    public Reserva aprovarReserva(Usuario usuario, long id) {
        exigirAdmin(usuario);
        Reserva reserva = exigirReservaBloqueada(id);
        if (!"PENDENTE".equals(reserva.getStatus())) {
            throw new RegraNegocioException(409, "Somente reservas pendentes podem ser aprovadas.");
        }
        reserva.setStatus("APROVADA");
        return reserva;
    }

    public Reserva cancelarReserva(Usuario usuario, long id) {
        exigirAutenticado(usuario);
        Reserva reserva = exigirReservaBloqueada(id);
        if (!usuario.isAdmin() && !usuario.getLogin().equals(reserva.getUsuarioLogin())) {
            throw new RegraNegocioException(403, "Você só pode cancelar suas próprias reservas.");
        }
        reserva.setStatus("CANCELADA");
        return reserva;
    }

    private Recurso exigirRecurso(long id, boolean bloquear) {
        Recurso recurso = repository.buscarRecurso(id, bloquear);
        if (recurso == null) {
            throw new RegraNegocioException(404, "Recurso não encontrado.");
        }
        return recurso;
    }

    private Reserva exigirReservaBloqueada(long id) {
        Reserva reserva = repository.buscarReserva(id);
        if (reserva == null) {
            throw new RegraNegocioException(404, "Reserva não encontrada.");
        }
        exigirRecurso(reserva.getRecursoId(), true);
        repository.bloquearReserva(reserva);
        return reserva;
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (!inicio.isBefore(fim)) {
            throw new RegraNegocioException(400, "O fim deve ser posterior ao início.");
        }
    }

    private LocalDateTime converterData(String valor) {
        try {
            return LocalDateTime.parse(valor == null ? "" : valor);
        } catch (DateTimeParseException exception) {
            throw new RegraNegocioException(400, "Use data e hora como 2026-10-20T09:00.");
        }
    }

    private void exigirAutenticado(Usuario usuario) {
        if (usuario == null) {
            throw new RegraNegocioException(401, "Faça login para continuar.");
        }
    }

    private void exigirAdmin(Usuario usuario) {
        exigirAutenticado(usuario);
        if (!usuario.isAdmin()) {
            throw new RegraNegocioException(403, "Operação permitida apenas para administradores.");
        }
    }
}
