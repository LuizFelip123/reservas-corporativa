package br.com.estudo.reservas.security;

import br.com.estudo.reservas.model.Usuario;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@WebFilter("/*")
public class AutenticacaoFilter implements Filter {
    private static final Set<String> LEITURA = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest entrada, ServletResponse saida, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) entrada;
        HttpServletResponse response = (HttpServletResponse) saida;
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        if (caminho.startsWith("/jakarta.faces.resource/")) {
            chain.doFilter(entrada, saida);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        HttpSession sessao = request.getSession(false);
        Usuario usuario =
                sessao == null ? null : (Usuario) sessao.getAttribute(SessaoUsuario.USUARIO);
        boolean api = caminho.equals("/api") || caminho.startsWith("/api/");
        boolean login = caminho.equals("/login.xhtml");
        if (!login && usuario == null) {
            if (api) {
                responder(response, 401, "Faça login para continuar.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login.xhtml");
            }
            return;
        }
        if (!LEITURA.contains(request.getMethod())) {
            String esperado =
                    sessao == null ? null : (String) sessao.getAttribute(SessaoUsuario.CSRF);
            String recebido =
                    api ? request.getHeader("X-CSRF-Token") : request.getParameter("csrfToken");
            if (!tokenValido(esperado, recebido)) {
                responder(response, 403, "Sessão inválida. Atualize a página e tente novamente.");
                return;
            }
        }
        if (caminho.equals("/recursos.xhtml") && !usuario.isAdmin()) {
            response.sendError(403, "Apenas administradores podem cadastrar recursos.");
            return;
        }
        chain.doFilter(entrada, saida);
    }

    private boolean tokenValido(String esperado, String recebido) {
        return esperado != null
                && recebido != null
                && MessageDigest.isEqual(
                        esperado.getBytes(StandardCharsets.UTF_8),
                        recebido.getBytes(StandardCharsets.UTF_8));
    }

    private void responder(HttpServletResponse response, int status, String mensagem)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}
