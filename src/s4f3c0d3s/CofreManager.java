
/*
 * S4F3-C0D3S - Recovery Codes Manager
 * Developed by Fajre
 * Originally distributed exclusively via the author's GitHub: https://github.com/fajremvp/S4F3-C0D3S
 * Licensed under the MIT License
 */

package s4f3c0d3s;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CofreManager {
    private File arquivo;
    private byte[] salt;
    private List<Registro> registros;
    private List<String> historico = new ArrayList<>();
    private SecretKey chave;
    private String dadosCriptografados;

    public CofreManager(File arquivo, SecretKey chave, byte[] salt) throws Exception {
        this.arquivo = arquivo;
        this.chave = chave;
        this.salt = salt;

        if (!arquivo.exists()) {
            registros = new ArrayList<>();
            historico = new ArrayList<>();
            salvar(); // Cria um novo cofre com dados vazios
        } else {
            carregar(); // Carrega dados existentes
        }
    }
    
    private int tempoSessaoMinutos = 5; // Tempo padrão

    private void carregar() throws Exception {

        this.dadosCriptografados = CriptografiaUtils.carregarArquivoComoTexto(arquivo, salt.length);
        String json = CriptografiaUtils.descriptografar(dadosCriptografados, chave);

        Gson gson = new Gson();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // Registros
        registros = new ArrayList<>();
        if (root.has("registros")) {
            Registro[] array = gson.fromJson(root.get("registros"), Registro[].class);
            if (array != null) {
                for (Registro r : array) registros.add(r);
            }
        }

        // Histórico
        if (root.has("historico")) {
            String[] hist = gson.fromJson(root.get("historico"), String[].class);
            historico = new ArrayList<>(List.of(hist));
        } else {
            historico = new ArrayList<>();
        }
        
        if (root.has("tempoSessaoMinutos")) {
            tempoSessaoMinutos = root.get("tempoSessaoMinutos").getAsInt();
        }

    }

    public void salvar() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject root = new JsonObject();
            root.add("registros", gson.toJsonTree(registros));
            root.add("historico", gson.toJsonTree(historico));
            root.addProperty("tempoSessaoMinutos", tempoSessaoMinutos);
            String json = gson.toJson(root);
            this.dadosCriptografados = CriptografiaUtils.criptografar(json, chave);
            CriptografiaUtils.salvarArquivoComSalt(arquivo, salt, dadosCriptografados);
        } catch (Exception e) {
            Logger.getLogger(CofreManager.class.getName()).log(Level.SEVERE, "Erro ao salvar cofre", e);
            throw new RuntimeException("Falha ao salvar cofre: consulte o log para mais detalhes", e);
        }
    }

    public void adicionarRegistro(Registro r) {
        registros.add(0, r); // Adiciona no início da lista
        salvar();
    }

    public void removerRegistro(Registro r) {
        registros.remove(r);
        salvar();
    }

    public void recriptografar(char[] senhaAntiga, char[] novaSenha) throws Exception {
        SecretKey chaveAntiga = null;
        SecretKey chaveNova = null;
        String json = null;

        try {
            byte[] novoSalt = CriptografiaUtils.gerarSalt();
            byte[] saltAntigo = this.salt;

            chaveAntiga = CriptografiaUtils.gerarChaveAES(senhaAntiga, saltAntigo);
            json = CriptografiaUtils.descriptografar(dadosCriptografados, chaveAntiga);

            chaveNova = CriptografiaUtils.gerarChaveAES(novaSenha, novoSalt);
            this.salt = novoSalt;

            this.dadosCriptografados = CriptografiaUtils.criptografar(json, chaveNova);
            this.chave = chaveNova;

            salvar();

            Arrays.fill(saltAntigo, (byte) 0);
        } finally {
            Arrays.fill(senhaAntiga, '\0');
            Arrays.fill(novaSenha, '\0');

            if (chaveAntiga != null && chaveAntiga.getEncoded() != null) {
                Arrays.fill(chaveAntiga.getEncoded(), (byte) 0);
            }
            if (chaveNova != null && chaveNova.getEncoded() != null) {
                Arrays.fill(chaveNova.getEncoded(), (byte) 0);
            }

            chaveAntiga = null;
            chaveNova = null;
            json = null;
        }
        
        // Após salvar o cofre com novo salt (por ex. dentro de recriptografar):
        File attemptsFile = new File(arquivo.getPath().replace(".enc", ".attempts"));
        String contagem = "0";  // Ou use o valor atual de tentativas, se quiser preservá-lo
        // Deriva a chave HMAC com o novo salt
        long timestamp = System.currentTimeMillis();
        String base = timestamp + ":" + contagem;

        SecretKeySpec chaveHMAC = CriptografiaUtils.gerarChaveHMAC("verificacao_interna".toCharArray(), salt);
        String hmac = CriptografiaUtils.gerarHMAC(base, chaveHMAC);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(attemptsFile))) {
            bw.write(Long.toString(timestamp));
            bw.newLine();
            bw.write(contagem);
            bw.newLine();
            bw.write(hmac);
        }


    }
    
    public void encerrarSessao() {
        if (chave != null) {
            byte[] keyBytes = chave.getEncoded();
            if (keyBytes != null) {
                Arrays.fill(keyBytes, (byte) 0);
            }
            chave = null; // Libera referência
        }

        // Limpa salt
        if (salt != null) {
            Arrays.fill(salt, (byte) 0);
            salt = null;
        }

        // Limpa dados criptografados
        if (dadosCriptografados != null) {
            dadosCriptografados = null;
        }

        // Limpa registros
        if (registros != null) {
            for (Registro r : registros) {
                r.servico = null;
                r.usuario = null;
                r.codigos = null;
                r.notas = null;
            }
            registros.clear();
        }

        // Limpa histórico
        if (historico != null) {
            for (int i = 0; i < historico.size(); i++) {
                historico.set(i, null);
            }
            historico.clear();
        }
    }

    public File getArquivo() {
        return this.arquivo;
    }

    public List<Registro> getRegistros() {
        return registros;
    }

    public List<String> getHistorico() {
        return historico;
    }
    
    public int getTempoSessaoMinutos() {
        return tempoSessaoMinutos;
    }
    public void setTempoSessaoMinutos(int minutos) {
        this.tempoSessaoMinutos = minutos;
        salvar();
    }

    public byte[] getSalt() {
        return this.salt;
    }
    
    String getDadosCriptografados() {
        return this.dadosCriptografados;
    }

}
