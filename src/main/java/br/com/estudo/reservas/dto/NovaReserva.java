package br.com.estudo.reservas.dto;

/** Dados recebidos do formulário ou JSON. O solicitante vem exclusivamente da sessão. */
public class NovaReserva {
    private long recursoId;
    private String inicio;
    private String fim;

    public NovaReserva() {}

    public NovaReserva(long recursoId, String inicio, String fim) {
        this.recursoId = recursoId;
        this.inicio = inicio;
        this.fim = fim;
    }

    public long getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(long recursoId) {
        this.recursoId = recursoId;
    }

    public String getInicio() {
        return inicio;
    }

    public void setInicio(String inicio) {
        this.inicio = inicio;
    }

    public String getFim() {
        return fim;
    }

    public void setFim(String fim) {
        this.fim = fim;
    }
}
