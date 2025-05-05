
/*
 * S4F3-C0D3S - Recovery Codes Manager
 * Developed by Fajre
 * Originally distributed exclusively via the author's GitHub: https://github.com/fajremvp/S4F3-C0D3S
 * Licensed under the MIT License
 */

package s4f3c0d3s;

import java.io.*;

public class ConfiguracaoCofre {
    public int tempoSessaoMinutos = 5;

    public static ConfiguracaoCofre carregar(File arquivoConfig) {
        ConfiguracaoCofre cfg = new ConfiguracaoCofre();
        if (arquivoConfig.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(arquivoConfig))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    if (linha.startsWith("tempoSessaoMinutos=")) {
                        cfg.tempoSessaoMinutos = Integer.parseInt(linha.split("=")[1]);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cfg;
    }

    public void salvar(File arquivoConfig) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(arquivoConfig))) {
            bw.write("tempoSessaoMinutos=" + tempoSessaoMinutos + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
