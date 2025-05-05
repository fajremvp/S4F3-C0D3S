
/*
 * S4F3-C0D3S - Recovery Codes Manager
 * Developed by Fajre
 * Originally distributed exclusively via the author's GitHub: https://github.com/fajremvp/S4F3-C0D3S
 * Licensed under the MIT License
 */

package s4f3c0d3s;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CriptografiaUtils {

	// AES/GCM assegura confidencialidade + integridade sem HMAC externo
	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH = 128; // bits

    private static final String DERIVATION = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 200000;
    private static final int TAM_CHAVE = 256;
    private static final int TAM_SALT = 16;

    // Gera um novo salt em memória (não salva em disco)
    public static byte[] gerarSalt() {
        byte[] salt = new byte[TAM_SALT];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    // Deriva a chave AES-256 a partir da senha e salt
    public static SecretKey gerarChaveAES(char[] senha, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(DERIVATION);
        PBEKeySpec spec = new PBEKeySpec(senha, salt, ITERACOES, TAM_CHAVE);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec chaveFinal = new SecretKeySpec(tmp.getEncoded(), "AES");
        spec.clearPassword();
        return chaveFinal;
    }
    
    // Deriva key para HMAC (se ainda usar HMAC em .attempts)
    public static SecretKeySpec gerarChaveHMAC(char[] senha, byte[] salt) throws Exception {
        SecretKeyFactory f = SecretKeyFactory.getInstance(DERIVATION);
        PBEKeySpec spec = new PBEKeySpec(senha, salt, ITERACOES, TAM_CHAVE);
        byte[] keyBytes = f.generateSecret(spec).getEncoded();
        spec.clearPassword();
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }


    // Criptografa um texto e retorna em base64 (iv + texto)
    public static String criptografar(String texto, SecretKey chave) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, chave, spec);
        byte[] cipherText = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));

        // Concatena IV + ciphertext (que já inclui tag)
        byte[] out = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
        return Base64.getEncoder().encodeToString(out);
    }

    // Descriptografa base64 (iv + texto)
    public static String descriptografar(String dado, SecretKey chave) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(dado);
        byte[] iv = Arrays.copyOfRange(decoded, 0, 12);
        byte[] cipherText = Arrays.copyOfRange(decoded, 12, decoded.length);

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, chave, spec);
        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, StandardCharsets.UTF_8);
    }

    // Lê o conteúdo do arquivo ignorando os primeiros N bytes
    public static String carregarArquivoComoTexto(File arquivo, int offset) throws IOException {
        byte[] bytes = Files.readAllBytes(arquivo.toPath());
        if (bytes.length < offset) {
            throw new IOException("Arquivo muito pequeno: esperado pelo menos " + offset + " bytes, mas encontrou " + bytes.length);
        }
        byte[] dados = Arrays.copyOfRange(bytes, offset, bytes.length);
        return new String(dados, StandardCharsets.UTF_8);
    }

    // Salva o salt + conteúdo criptografado em um único arquivo
    public static void salvarArquivoComSalt(File arquivo, byte[] salt, String conteudoCriptografado) throws Exception {
        byte[] dadosCriptografados = conteudoCriptografado.getBytes("UTF-8");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(salt); // primeiros 16 bytes
        outputStream.write(dadosCriptografados); // resto

        Files.write(arquivo.toPath(), outputStream.toByteArray());
    }
    
    public static String gerarHMAC(String tentativas, SecretKey chave) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(chave.getEncoded(), "HmacSHA256");
        mac.init(keySpec);
        byte[] hmacBytes = mac.doFinal(tentativas.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

}
