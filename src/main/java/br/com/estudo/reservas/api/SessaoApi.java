package br.com.estudo.reservas.api;

import br.com.estudo.reservas.security.SessaoUsuario;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("sessao")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class SessaoApi {
    @Inject private SessaoUsuario sessao;

    @GET
    public Map<String, Object> consultar() {
        return Map.of("usuario", sessao.getUsuario(), "csrfToken", sessao.getCsrfToken());
    }
}
