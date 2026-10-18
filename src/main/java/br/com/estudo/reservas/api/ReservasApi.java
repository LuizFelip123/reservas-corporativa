package br.com.estudo.reservas.api;

import br.com.estudo.reservas.dto.NovaReserva;
import br.com.estudo.reservas.model.Recurso;
import br.com.estudo.reservas.model.Reserva;
import br.com.estudo.reservas.security.SessaoUsuario;
import br.com.estudo.reservas.service.ReservasService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReservasApi {
    @Inject private ReservasService service;
    @Inject private SessaoUsuario sessao;

    @GET
    @Path("recursos")
    public List<Recurso> listarRecursos() {
        return service.listarRecursos(sessao.getUsuario());
    }

    @POST
    @Path("recursos")
    public Response cadastrarRecurso(Recurso entrada) {
        return Response.status(Response.Status.CREATED)
                .entity(service.cadastrarRecurso(sessao.getUsuario(), entrada))
                .build();
    }

    @GET
    @Path("reservas")
    public List<Reserva> listarReservas() {
        return service.listarReservas(sessao.getUsuario());
    }

    @GET
    @Path("recursos/{id}/disponibilidade")
    public Map<String, Boolean> consultarDisponibilidade(
            @PathParam("id") long id,
            @QueryParam("inicio") String inicio,
            @QueryParam("fim") String fim) {
        return Map.of(
                "disponivel",
                service.consultarDisponibilidade(sessao.getUsuario(), id, inicio, fim));
    }

    @POST
    @Path("reservas")
    public Response solicitarReserva(NovaReserva entrada) {
        return Response.status(Response.Status.CREATED)
                .entity(service.solicitarReserva(sessao.getUsuario(), entrada))
                .build();
    }

    @PATCH
    @Path("reservas/{id}/aprovar")
    public Reserva aprovarReserva(@PathParam("id") long id) {
        return service.aprovarReserva(sessao.getUsuario(), id);
    }

    @PATCH
    @Path("reservas/{id}/cancelar")
    public Reserva cancelarReserva(@PathParam("id") long id) {
        return service.cancelarReserva(sessao.getUsuario(), id);
    }
}
