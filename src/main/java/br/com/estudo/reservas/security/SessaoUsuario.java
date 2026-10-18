package br.com.estudo.reservas.security;

import br.com.estudo.reservas.model.Usuario;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

@RequestScoped
public class SessaoUsuario {
    public static final String USUARIO = "usuarioAutenticado";
    public static final String CSRF = "csrfToken";
    @Inject private HttpServletRequest request;

    public Usuario getUsuario() {
        HttpSession sessao = request.getSession(false);
        return sessao == null ? null : (Usuario) sessao.getAttribute(USUARIO);
    }

    public String getCsrfToken() {
        HttpSession sessao = request.getSession(true);
        synchronized (sessao) {
            String token = (String) sessao.getAttribute(CSRF);
            if (token == null) {
                byte[] bytes = new byte[32];
                new SecureRandom().nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                sessao.setAttribute(CSRF, token);
            }
            return token;
        }
    }
}
