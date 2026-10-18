package br.com.estudo.reservas.exception;

/** Falha esperada de validação ou de uma operação de reserva. */
public class RegraNegocioException extends RuntimeException {
    private final int codigo;

    public RegraNegocioException(int codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
