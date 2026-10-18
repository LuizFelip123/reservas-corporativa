package br.com.estudo.reservas.web;

import br.com.estudo.reservas.model.Usuario;
import br.com.estudo.reservas.security.AutenticacaoService;
import br.com.estudo.reservas.security.SessaoUsuario;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("loginBean")
@RequestScoped
public class LoginBean {
    @Inject private AutenticacaoService autenticacao;
    @Inject private SessaoUsuario sessao;
    @Inject private HttpServletRequest request;
    private String login;
    private String senha;

    public String entrar() {
        Usuario usuario;
        try {
            usuario = autenticacao.autenticar(login, senha == null ? null : senha.toCharArray());
        } finally {
            senha = null;
        }
        if (usuario == null) {
            FacesContext.getCurrentInstance()
                    .addMessage(
                            null,
                            new FacesMessage(
                                    FacesMessage.SEVERITY_ERROR,
                                    "Usuário ou senha inválidos.",
                                    null));
            return null;
        }
        request.getSession(true);
        request.changeSessionId();
        request.getSession().setAttribute(SessaoUsuario.USUARIO, usuario);
        request.getSession().removeAttribute(SessaoUsuario.CSRF);
        return "/index?faces-redirect=true";
    }

    public String sair() {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return "/login?faces-redirect=true";
    }

    public Usuario getUsuario() {
        return sessao.getUsuario();
    }

    public boolean isAdmin() {
        return getUsuario() != null && getUsuario().isAdmin();
    }

    public String getCsrfToken() {
        return sessao.getCsrfToken();
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
