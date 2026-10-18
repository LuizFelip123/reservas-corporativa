package br.com.estudo.reservas.service;

import br.com.estudo.reservas.model.*;
import br.com.estudo.reservas.repository.ReservasRepository;
import br.com.estudo.reservas.security.SenhaHash;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Arrays;

@ApplicationScoped
public class DadosIniciaisService {
    @Inject private ReservasRepository repository;

    public DadosIniciaisService() {}

    public DadosIniciaisService(ReservasRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void inicializar() {
        criarUsuario("admin", "Administrador", Perfil.ADMIN, "Admin@123");
        criarUsuario("funcionario", "Funcionário", Perfil.FUNCIONARIO, "Funcionario@123");
        criarUsuario("funcionario2", "Funcionário 2", Perfil.FUNCIONARIO, "Funcionario@123");
        if (repository.listarRecursos().isEmpty()) {
            repository.salvar(new Recurso(0, "Sala de reunião", "SALA"));
            repository.salvar(new Recurso(0, "Projetor", "EQUIPAMENTO"));
        }
    }

    private void criarUsuario(String login, String nome, Perfil perfil, String senha) {
        if (repository.buscarUsuario(login) != null) {
            return;
        }
        Usuario usuario = new Usuario(login, nome, perfil);
        char[] caracteres = senha.toCharArray();
        try {
            usuario.setSenhaHash(SenhaHash.gerar(caracteres));
        } finally {
            Arrays.fill(caracteres, '\0');
        }
        repository.salvar(usuario);
    }
}
