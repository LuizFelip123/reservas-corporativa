package br.com.estudo.reservas.repository;

import br.com.estudo.reservas.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReservasRepository {
    @PersistenceContext(unitName = "reservasPU")
    private EntityManager em;

    public ReservasRepository() {}

    public ReservasRepository(EntityManager em) {
        this.em = em;
    }

    public List<Recurso> listarRecursos() {
        return em.createQuery("select r from Recurso r order by r.id", Recurso.class)
                .getResultList();
    }

    public void salvar(Object entidade) {
        em.persist(entidade);
        em.flush();
    }

    public Recurso buscarRecurso(long id, boolean bloquear) {
        return bloquear
                ? em.find(Recurso.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(Recurso.class, id);
    }

    public Usuario buscarUsuario(String login) {
        return em.createQuery("select u from Usuario u where u.login = :login", Usuario.class)
                .setParameter("login", login)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public List<Reserva> listarReservas(Usuario usuario) {
        String jpql =
                "select r from Reserva r join fetch r.recurso join fetch r.usuario"
                        + (usuario.isAdmin() ? "" : " where r.usuario.login = :login")
                        + " order by r.id desc";
        TypedQuery<Reserva> query = em.createQuery(jpql, Reserva.class);
        if (!usuario.isAdmin()) {
            query.setParameter("login", usuario.getLogin());
        }
        return query.getResultList();
    }

    public Reserva buscarReserva(long id) {
        return em.createQuery(
                        "select r from Reserva r join fetch r.recurso join fetch r.usuario where r.id = :id",
                        Reserva.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public void bloquearReserva(Reserva reserva) {
        em.refresh(reserva, LockModeType.PESSIMISTIC_WRITE);
    }

    public boolean existeConflito(long recursoId, LocalDateTime inicio, LocalDateTime fim) {
        return em.createQuery(
                                "select count(r) from Reserva r where r.recurso.id = :recurso"
                                        + " and r.status <> 'CANCELADA' and r.inicio < :fim and r.fim > :inicio",
                                Long.class)
                        .setParameter("recurso", recursoId)
                        .setParameter("inicio", inicio)
                        .setParameter("fim", fim)
                        .getSingleResult()
                > 0;
    }
}
