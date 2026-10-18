package br.com.estudo.reservas.api;

import br.com.estudo.reservas.exception.RegraNegocioException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class RegraNegocioExceptionMapper implements ExceptionMapper<RegraNegocioException> {
    @Override
    public Response toResponse(RegraNegocioException exception) {
        return Response.status(exception.getCodigo())
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("erro", exception.getMessage()))
                .build();
    }
}
