package br.com.estudo.reservas.security;

import br.com.estudo.reservas.model.Usuario;
import br.com.estudo.reservas.repository.ReservasRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Arrays;

@ApplicationScoped
@Transactional
public class AutenticacaoService {
    @Inject private ReservasRepository repository;
    private final String hashInexistente = SenhaHash.gerar("usuario-inexistente".toCharArray());

    public AutenticacaoService() {}

    public AutenticacaoService(ReservasRepository repository) {
        this.repository = repository;
    }

    public Usuario autenticar(String login, char[] senha) {
        if (senha == null) {
            return null;
        }
        try {
            Usuario usuario = repository.buscarUsuario(login == null ? "" : login.trim());
            boolean valida =
                    SenhaHash.confere(
                            senha, usuario == null ? hashInexistente : usuario.getSenhaHash());
            return usuario != null && valida ? usuario.identidade() : null;
        } finally {
            Arrays.fill(senha, '\0');
        }
    }
}
