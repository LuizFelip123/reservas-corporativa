package br.com.estudo.reservas.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class SenhaHash {
    private static final int ITERACOES = 210_000;

    private SenhaHash() {}

    public static String gerar(char[] senha) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derivar(senha, salt, ITERACOES);
        return "pbkdf2-sha256$"
                + ITERACOES
                + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean confere(char[] senha, String codificado) {
        if (senha == null || codificado == null) {
            return false;
        }
        try {
            String[] partes = codificado.split("\\$");
            if (partes.length != 4 || !"pbkdf2-sha256".equals(partes[0])) {
                return false;
            }
            int iteracoes = Integer.parseInt(partes[1]);
            if (iteracoes < 100_000 || iteracoes > 1_000_000) {
                return false;
            }
            byte[] hash = Base64.getDecoder().decode(partes[3]);
            byte[] candidato = derivar(senha, Base64.getDecoder().decode(partes[2]), iteracoes);
            try {
                return MessageDigest.isEqual(hash, candidato);
            } finally {
                Arrays.fill(candidato, (byte) 0);
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] derivar(char[] senha, byte[] salt, int iteracoes) {
        PBEKeySpec especificacao = new PBEKeySpec(senha, salt, iteracoes, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacao)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível verificar a senha.", exception);
        } finally {
            especificacao.clearPassword();
        }
    }
}
