package br.com.estudo.reservas.config;

import br.com.estudo.reservas.service.DadosIniciaisService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

@Singleton
@Startup
public class DadosIniciais {
    @Inject private DadosIniciaisService service;

    @PostConstruct
    public void inicializar() {
        service.inicializar();
    }
}
