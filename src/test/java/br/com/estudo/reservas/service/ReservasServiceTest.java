package br.com.estudo.reservas.service;

import static org.junit.jupiter.api.Assertions.*;

import br.com.estudo.reservas.dto.NovaReserva;
import br.com.estudo.reservas.exception.RegraNegocioException;
import br.com.estudo.reservas.model.*;
import br.com.estudo.reservas.repository.ReservasRepository;
import br.com.estudo.reservas.security.AutenticacaoService;
import jakarta.persistence.*;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;

class ReservasServiceTest {
    private static EntityManagerFactory factory;
    private EntityManager em;
    private ReservasRepository repository;
    private ReservasService service;
    private Usuario admin, funcionario, outro;
    private long recursoId;

    @BeforeAll
    static void criarBanco() {
        factory =
                Persistence.createEntityManagerFactory(
                        "reservasTestPU",
                        Map.of(
                                "jakarta.persistence.jdbc.url",
                                "jdbc:h2:mem:"
                                        + UUID.randomUUID()
                                        + ";MODE=LEGACY;DB_CLOSE_DELAY=-1"));
    }

    @AfterAll
    static void fecharBanco() {
        if (factory != null) {
            factory.close();
        }
    }

    @BeforeEach
    void preparar() {
        em = factory.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("delete from Reserva").executeUpdate();
        em.createQuery("delete from Recurso").executeUpdate();
        em.createQuery("delete from Usuario").executeUpdate();
        repository = new ReservasRepository(em);
        new DadosIniciaisService(repository).inicializar();
        service = new ReservasService(repository);
        admin = repository.buscarUsuario("admin").identidade();
        funcionario = repository.buscarUsuario("funcionario").identidade();
        outro = repository.buscarUsuario("funcionario2").identidade();
        recursoId = service.listarRecursos(admin).get(0).getId();
    }

    @AfterEach
    void encerrar() {
        if (em != null && em.isOpen()) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    @Test
    void bloqueiaSobreposicaoPendenteEAprovada() {
        Reserva r = service.solicitarReserva(funcionario, entrada("09:00", "10:00"));
        assertFalse(
                service.consultarDisponibilidade(
                        funcionario, recursoId, data("09:30"), data("10:30")));
        assertCodigo(409, () -> service.solicitarReserva(outro, entrada("08:30", "09:30")));
        service.aprovarReserva(admin, r.getId());
        assertEquals("APROVADA", r.getStatus());
        assertFalse(
                service.consultarDisponibilidade(outro, recursoId, data("08:00"), data("11:00")));
    }

    @Test
    void permiteHorariosConsecutivosERecursosDiferentes() {
        service.solicitarReserva(funcionario, entrada("09:00", "10:00"));
        assertTrue(
                service.consultarDisponibilidade(outro, recursoId, data("10:00"), data("11:00")));
        assertTrue(
                service.consultarDisponibilidade(outro, recursoId, data("08:00"), data("09:00")));
        long outroRecurso = service.listarRecursos(admin).get(1).getId();
        assertDoesNotThrow(
                () ->
                        service.solicitarReserva(
                                outro,
                                new NovaReserva(outroRecurso, data("09:00"), data("10:00"))));
    }

    @Test
    void cancelamentoLiberaHorario() {
        Reserva r = service.solicitarReserva(funcionario, entrada("09:00", "10:00"));
        service.cancelarReserva(funcionario, r.getId());
        assertTrue(
                service.consultarDisponibilidade(outro, recursoId, data("09:00"), data("10:00")));
        assertCodigo(409, () -> service.aprovarReserva(admin, r.getId()));
    }

    @Test
    void funcionarioNaoAprovaNemCadastraRecursos() {
        Reserva r = service.solicitarReserva(funcionario, entrada("09:00", "10:00"));
        assertCodigo(403, () -> service.aprovarReserva(funcionario, r.getId()));
        assertCodigo(
                403, () -> service.cadastrarRecurso(funcionario, new Recurso(0, "Sala 3", "SALA")));
        assertEquals("PENDENTE", r.getStatus());
    }

    @Test
    void funcionarioSoListaECancelaPropriasReservas() {
        Reserva r = service.solicitarReserva(funcionario, entrada("09:00", "10:00"));
        service.solicitarReserva(outro, entrada("10:00", "11:00"));
        assertEquals(1, service.listarReservas(funcionario).size());
        assertEquals(1, service.listarReservas(outro).size());
        assertEquals(2, service.listarReservas(admin).size());
        assertCodigo(403, () -> service.cancelarReserva(outro, r.getId()));
        assertEquals("PENDENTE", r.getStatus());
        service.cancelarReserva(admin, r.getId());
        assertEquals("CANCELADA", r.getStatus());
    }

    @Test
    void rejeitaAcessoAnonimo() {
        assertCodigo(401, () -> service.listarRecursos(null));
        assertCodigo(401, () -> service.listarReservas(null));
        assertCodigo(401, () -> service.solicitarReserva(null, entrada("09:00", "10:00")));
        assertCodigo(401, () -> service.aprovarReserva(null, 1));
    }

    @Test
    void validaPeriodoEIdentificadores() {
        assertCodigo(400, () -> service.solicitarReserva(funcionario, entrada("10:00", "09:00")));
        assertCodigo(
                400,
                () ->
                        service.consultarDisponibilidade(
                                funcionario, recursoId, "inválida", data("10:00")));
        assertCodigo(400, () -> service.solicitarReserva(funcionario, entrada("10:00", "10:00")));
        assertCodigo(
                404,
                () ->
                        service.solicitarReserva(
                                funcionario,
                                new NovaReserva(Long.MAX_VALUE, data("09:00"), data("10:00"))));
        assertCodigo(404, () -> service.cancelarReserva(admin, Long.MAX_VALUE));
    }

    @Test
    void adminCadastraRecursoEValidaTipo() {
        Recurso r = service.cadastrarRecurso(admin, new Recurso(0, " Sala 2 ", "SALA"));
        assertEquals("Sala 2", r.getNome());
        assertTrue(r.getId() > 0);
        assertEquals(3, service.listarRecursos(admin).size());
        assertCodigo(400, () -> service.cadastrarRecurso(admin, new Recurso(0, "Sala", "OUTRO")));
    }

    @Test
    void loginConfereHashERetornaIdentidadeSemSenha() {
        AutenticacaoService auth = new AutenticacaoService(repository);
        Usuario identidade = auth.autenticar("admin", "Admin@123".toCharArray());
        assertNotNull(identidade);
        assertTrue(identidade.isAdmin());
        assertNull(identidade.getSenhaHash());
        assertNull(auth.autenticar("admin", "errada".toCharArray()));
        assertNull(auth.autenticar("inexistente", "Admin@123".toCharArray()));
        assertFalse(auth.autenticar("funcionario", "Funcionario@123".toCharArray()).isAdmin());
    }

    @Test
    void inicializacaoNaoDuplicaUsuariosOuRecursos() {
        new DadosIniciaisService(repository).inicializar();
        assertEquals(
                3L, em.createQuery("select count(u) from Usuario u", Long.class).getSingleResult());
        assertEquals(2, service.listarRecursos(admin).size());
    }

    @Test
    void reservaPersisteAoAbrirOutraSessaoDoBanco() {
        Reserva r = service.solicitarReserva(funcionario, entrada("09:00", "10:00"));
        long id = r.getId();
        em.getTransaction().commit();
        em.close();
        em = factory.createEntityManager();
        em.getTransaction().begin();
        service = new ReservasService(new ReservasRepository(em));
        Reserva persistida = service.listarReservas(funcionario).get(0);
        assertEquals(id, persistida.getId());
        assertEquals("funcionario", persistida.getUsuarioLogin());
        assertEquals("Funcionário", persistida.getSolicitante());
        assertEquals(recursoId, persistida.getRecursoId());
    }

    private NovaReserva entrada(String inicio, String fim) {
        return new NovaReserva(recursoId, data(inicio), data(fim));
    }

    private String data(String horario) {
        return "2026-10-20T" + horario;
    }

    private void assertCodigo(int codigo, Runnable acao) {
        assertEquals(codigo, assertThrows(RegraNegocioException.class, acao::run).getCodigo());
    }
}
