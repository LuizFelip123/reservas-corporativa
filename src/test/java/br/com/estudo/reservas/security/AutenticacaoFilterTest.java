package br.com.estudo.reservas.security;

import static org.junit.jupiter.api.Assertions.*;

import br.com.estudo.reservas.model.Perfil;
import br.com.estudo.reservas.model.Usuario;
import jakarta.servlet.http.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AutenticacaoFilterTest {
    @Test
    void visitanteRecebeRedirecionamentoNaTelaE401NaApi() throws Exception {
        Requisicao tela = new Requisicao("/index.xhtml", "GET");
        tela.executar();
        assertEquals("/reservas/login.xhtml", tela.redirecionamento);
        assertFalse(tela.prosseguiu);
        Requisicao api = new Requisicao("/api/reservas", "GET");
        api.executar();
        assertEquals(401, api.status);
        assertFalse(api.prosseguiu);
    }

    @Test
    void funcionarioNaoAbreCadastroDeRecursos() throws Exception {
        Requisicao r = new Requisicao("/recursos.xhtml", "GET");
        r.atributos.put(
                SessaoUsuario.USUARIO,
                new Usuario("funcionario", "Funcionário", Perfil.FUNCIONARIO));
        r.executar();
        assertEquals(403, r.status);
        assertFalse(r.prosseguiu);
    }

    @Test
    void bloqueiaAlteracoesSemTokenMesmoParaAdmin() throws Exception {
        Requisicao r = new Requisicao("/api/reservas/1/aprovar", "PATCH");
        r.atributos.put(SessaoUsuario.USUARIO, new Usuario("admin", "Admin", Perfil.ADMIN));
        r.atributos.put(SessaoUsuario.CSRF, "token-valido");
        r.executar();
        assertEquals(403, r.status);
        assertFalse(r.prosseguiu);
    }

    @Test
    void aceitaTokenDaSessaoERejeitaTokenDeOutraSessao() throws Exception {
        Requisicao r = new Requisicao("/api/reservas", "POST");
        r.atributos.put(
                SessaoUsuario.USUARIO,
                new Usuario("funcionario", "Funcionário", Perfil.FUNCIONARIO));
        r.atributos.put(SessaoUsuario.CSRF, "token-valido");
        r.token = "outro-token";
        r.executar();
        assertEquals(403, r.status);
        r.token = "token-valido";
        r.executar();
        assertTrue(r.prosseguiu);
    }

    @Test
    void loginTambemExigeTokenContraCsrf() throws Exception {
        Requisicao r = new Requisicao("/login.xhtml", "POST");
        r.executar();
        assertEquals(403, r.status);
        assertFalse(r.prosseguiu);
    }

    private static class Requisicao {
        final Map<String, Object> atributos = new HashMap<>();
        final String caminho;
        final String metodo;
        String token;
        int status;
        String redirecionamento;
        boolean prosseguiu;

        Requisicao(String caminho, String metodo) {
            this.caminho = caminho;
            this.metodo = metodo;
        }

        void executar() throws Exception {
            HttpSession sessao =
                    proxy(
                            HttpSession.class,
                            (obj, method, args) ->
                                    method.getName().equals("getAttribute")
                                            ? atributos.get(args[0])
                                            : null);
            HttpServletRequest request =
                    proxy(
                            HttpServletRequest.class,
                            (obj, method, args) ->
                                    switch (method.getName()) {
                                        case "getRequestURI" -> "/reservas" + caminho;
                                        case "getContextPath" -> "/reservas";
                                        case "getSession" -> atributos.isEmpty() ? null : sessao;
                                        case "getMethod" -> metodo;
                                        case "getHeader", "getParameter" -> token;
                                        default -> null;
                                    });
            HttpServletResponse response =
                    proxy(
                            HttpServletResponse.class,
                            (obj, method, args) -> {
                                switch (method.getName()) {
                                    case "setStatus", "sendError" -> status = (Integer) args[0];
                                    case "sendRedirect" -> redirecionamento = (String) args[0];
                                    case "getWriter" -> {
                                        return new PrintWriter(new StringWriter());
                                    }
                                    default -> {}
                                }
                                return null;
                            });
            new AutenticacaoFilter()
                    .doFilter(request, response, (entrada, saida) -> prosseguiu = true);
        }

        private <T> T proxy(Class<T> tipo, java.lang.reflect.InvocationHandler handler) {
            return tipo.cast(
                    Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] {tipo}, handler));
        }
    }
}
