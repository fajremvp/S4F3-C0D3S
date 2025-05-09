
/*
 * S4F3-C0D3S - Recovery Codes Manager
 * Developed by Fajre
 * Originally distributed exclusively via the author's GitHub: https://github.com/fajremvp/S4F3-C0D3S
 * Licensed under the MIT License
 */


package s4f3c0d3s;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TelaInicial extends JFrame {
    private static final long serialVersionUID = 1L;

    private boolean senhaVisivel = false;
    private JPasswordField campoSenha;
    private JLabel placeholderLabel;
    private File pastaDados;

    private ResourceBundle bundle;
    private Preferences prefs = Preferences.userRoot().node("s4f3c0d3s");
    
    public TelaInicial() {
    	
    	ImageIcon iconeJanela = new ImageIcon(getClass().getResource("/logoBarraDeTarefas.png"));
        setIconImage(iconeJanela.getImage());
        
        if (prefs.get("language", null) == null) {
            prefs.put("language", "en");
        }
    	
        Color corFundo = new Color(245, 245, 245);
        Color corTexto = Color.BLACK;
        Color corBotaoFundo = new Color(230, 230, 230);
        Color corBotaoTexto = Color.BLACK;
        
        String lang = prefs.get("language", "en"); // Valor padrão é en
        Locale locale = lang.equals("en") ? new Locale("en") : new Locale("pt");
        bundle = ResourceBundle.getBundle("messages", locale);

        setTitle(bundle.getString("telaInicial.titulo"));
        setSize(600, 420);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        String dadosPath = prefs.get("dadosPath", "");
        if (dadosPath.isEmpty()) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(bundle.getString("titulo.selecionarDiretorioDados"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            SwingUtilities.invokeLater(() -> {
    	        chooser.getRootPane().setDefaultButton(null);
    	    });
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
            	File selectedDir = chooser.getSelectedFile();
            	File pastaRaiz = new File(selectedDir, "S4F3-C0D3S");
            	pastaDados = new File(pastaRaiz, "dados");

            	pastaDados.mkdirs(); // Cria S4F3-C0D3S/dados (ambos se não existirem)
            	prefs.put("dadosPath", pastaDados.getAbsolutePath());
            } else {
            	JOptionPane.showMessageDialog(this,
            		    bundle.getString("mensagem.diretorioNaoSelecionado"),
            		    bundle.getString("titulo.atencao"), JOptionPane.PLAIN_MESSAGE);
                System.exit(0);
            }
        } else {
            pastaDados = new File(dadosPath);
            if (!pastaDados.exists()) pastaDados.mkdirs();
        }

        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        painel.setBackground(corFundo);

        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/logoComTextoEmbaixoTemaClaro.png"));
        Image logoImg = logoIcon.getImage().getScaledInstance(200, 120, Image.SCALE_SMOOTH);
        JLabel labelLogo = new JLabel(new ImageIcon(logoImg));
        labelLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        campoSenha = new JPasswordField();
        campoSenha.setEchoChar('•');
        campoSenha.setForeground(corTexto);
        campoSenha.setCaretColor(corTexto);
        campoSenha.setBackground(Color.WHITE);
        campoSenha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(5, 5, 5, 28)
        ));

        placeholderLabel = new JLabel(bundle.getString("label.senha"));
        placeholderLabel.setForeground(Color.GRAY);
        placeholderLabel.setEnabled(false);

        campoSenha.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { verificar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { verificar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { verificar(); }

            private void verificar() {
            	char[] senhaChars = campoSenha.getPassword();
            	placeholderLabel.setVisible(senhaChars.length == 0);
            	Arrays.fill(senhaChars, '\0');
            }
        });

        JLayeredPane layeredPane = new JLayeredPane();
        int larguraCampo = 300;
        int larguraPane = 300;

        layeredPane.setPreferredSize(new Dimension(larguraPane, 30));

        int margem = (larguraPane - larguraCampo) / 2;
        campoSenha.setBounds(margem, 0, larguraCampo, 30);
        placeholderLabel.setBounds(margem + 5, 0, larguraCampo - 5, 30);
        
        ImageIcon iconMostrarOriginal = new ImageIcon(getClass().getResource("/esconderTemaClaro.png"));
        ImageIcon iconEsconderOriginal = new ImageIcon(getClass().getResource("/verTemaClaro.png"));

        ImageIcon iconeMostrar = new ImageIcon(iconMostrarOriginal.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
        ImageIcon iconeEsconder = new ImageIcon(iconEsconderOriginal.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));


        JButton botaoOlho = new JButton(iconeMostrar);
        botaoOlho.setBounds(margem + larguraCampo - 30, 0, 30, 30);
        botaoOlho.setBorderPainted(false);
        botaoOlho.setContentAreaFilled(false);
        botaoOlho.setFocusPainted(false);
        
        botaoOlho.addActionListener(e -> {
            senhaVisivel = !senhaVisivel;
            campoSenha.setEchoChar(senhaVisivel ? (char) 0 : '•');
            botaoOlho.setIcon(senhaVisivel ? iconeEsconder : iconeMostrar);
        });


        layeredPane.add(campoSenha, Integer.valueOf(1));
        layeredPane.add(placeholderLabel, Integer.valueOf(2));
        
        layeredPane.add(botaoOlho, Integer.valueOf(3));

        JPanel painelSenha = new JPanel();
        painelSenha.setLayout(new BoxLayout(painelSenha, BoxLayout.X_AXIS));
        painelSenha.setMaximumSize(new Dimension(320, 45));
        painelSenha.setBackground(corFundo);
        painelSenha.add(layeredPane);
        painelSenha.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton botaoAcessar = new JButton(bundle.getString("botao.acessar"));
        JButton botaoCriarSenha = new JButton(bundle.getString("botao.criar"));
        JButton botaoImportarCofre = new JButton(bundle.getString("botao.importar"));
        JButton botaoSobre = new JButton(bundle.getString("botao.sobre"));
        
        JButton botaoAlterarDiretorio = new JButton(bundle.getString("botao.alterarDiretorio"));
        botaoAlterarDiretorio.setBackground(corBotaoFundo);
        botaoAlterarDiretorio.setForeground(corBotaoTexto);
        botaoAlterarDiretorio.setAlignmentX(Component.CENTER_ALIGNMENT);
        botaoAlterarDiretorio.addActionListener(e -> {
            String caminhoAtual = pastaDados.getAbsolutePath();
            int resposta = JOptionPane.showConfirmDialog(this,
                "Diretório atual:\n" + caminhoAtual + "\n\nDeseja alterar o diretório de dados?",
                bundle.getString("titulo.aviso"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

            if (resposta == JOptionPane.YES_OPTION) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(bundle.getString("titulo.selecionarDiretorioDados"));
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                SwingUtilities.invokeLater(() -> {
        	        chooser.getRootPane().setDefaultButton(null);
        	    });
                int res = chooser.showOpenDialog(this);
                if (res == JFileChooser.APPROVE_OPTION) {
                	File novoDir = chooser.getSelectedFile();
                	File novaRaiz = new File(novoDir, "S4F3-C0D3S");
                	File novaPastaDados = new File(novaRaiz, "dados");
                    novaPastaDados.mkdir();
                    prefs.put("dadosPath", novaPastaDados.getAbsolutePath());
                    this.pastaDados = novaPastaDados;
                    JOptionPane.showMessageDialog(this,
                        bundle.getString("mensagem.diretorioAlteradoSucesso").replace("{0}", novaPastaDados.getAbsolutePath()),
                        bundle.getString("titulo.sucesso"), JOptionPane.PLAIN_MESSAGE);
                }
            }
        });

        botaoAcessar.setBackground(corBotaoFundo);
        botaoCriarSenha.setBackground(corBotaoFundo);
        botaoImportarCofre.setBackground(corBotaoFundo);
        botaoSobre.setBackground(corBotaoFundo);
        botaoAcessar.setForeground(corBotaoTexto);
        botaoCriarSenha.setForeground(corBotaoTexto);
        botaoImportarCofre.setForeground(corBotaoTexto);
        botaoSobre.setForeground(corBotaoTexto);
        botaoAcessar.setAlignmentX(Component.CENTER_ALIGNMENT);
        botaoCriarSenha.setAlignmentX(Component.CENTER_ALIGNMENT);
        botaoImportarCofre.setAlignmentX(Component.CENTER_ALIGNMENT);

        botaoAcessar.addActionListener(e -> acessarCofre());
        botaoCriarSenha.addActionListener(e -> criarNovoCofre());
        botaoImportarCofre.addActionListener(e -> importarCofre());
        botaoSobre.addActionListener(e ->
        JOptionPane.showMessageDialog(this,
            bundle.getString("mensagem.sobre"),
            bundle.getString("botao.sobre"),
            JOptionPane.PLAIN_MESSAGE));

        JPanel painelBotoesFinais = new JPanel(new BorderLayout());
        painelBotoesFinais.setMaximumSize(new Dimension(600, 40));
        painelBotoesFinais.setBackground(corFundo);
        painelBotoesFinais.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        painelBotoesFinais.add(botaoSobre, BorderLayout.WEST);
        JPanel centro = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        centro.setBackground(corFundo);
        centro.add(Box.createRigidArea(new Dimension(20, 0)));
        centro.add(botaoAlterarDiretorio);
        painelBotoesFinais.add(centro, BorderLayout.CENTER);

        String[] idiomas = {
        	    bundle.getString("idioma.pt"),
        	    bundle.getString("idioma.en")
        	};

        JComboBox<String> comboLang = new JComboBox<>(idiomas);

        // Seleciona o idioma atual salvo nas preferências
        String idiomaAtual = prefs.get("language", "pt");
        comboLang.setSelectedItem(idiomaAtual.equals("en") ? "English" : "Português");

        comboLang.addActionListener(e -> {
        	int indiceSelecionado = comboLang.getSelectedIndex();
        	String novoLang = (indiceSelecionado == 1) ? "en" : "pt";

            // Salva a nova escolha
            prefs.put("language", novoLang);

            // Recria a TelaInicial com o novo idioma
            SwingUtilities.invokeLater(() -> {
                prefs.put("language", novoLang);
                dispose();
                new TelaInicial();
            });

        });

        painelBotoesFinais.add(comboLang, BorderLayout.EAST);

        painel.add(labelLogo);
        painel.add(Box.createVerticalStrut(15));
        painel.add(painelSenha);
        painel.add(Box.createVerticalStrut(10));
        painel.add(botaoAcessar);
        painel.add(Box.createVerticalStrut(10));
        painel.add(botaoCriarSenha);
        painel.add(Box.createVerticalStrut(10));
        painel.add(botaoImportarCofre);
        painel.add(Box.createVerticalStrut(15));
        painel.add(painelBotoesFinais);

        add(painel);
        setVisible(true);
    }

    
    public TelaInicial(boolean exibirMensagemDesconexao) {
        this();
        if (exibirMensagemDesconexao) {
            SwingUtilities.invokeLater(() -> {
            	JOptionPane.showMessageDialog(this,
            		    bundle.getString("mensagem.expirou"),
            		    bundle.getString("titulo.aviso"),
            		    JOptionPane.PLAIN_MESSAGE);
            });
        }
    }

    private void acessarCofre() {
        File[] arquivos = pastaDados.listFiles((dir, name) -> name.endsWith(".enc"));
        // Verifica se existem cofres no diretório
        if (arquivos == null || arquivos.length == 0) {
            JOptionPane.showMessageDialog(this,
                bundle.getString("mensagem.semCofres"),
                bundle.getString("titulo.aviso"),
                JOptionPane.PLAIN_MESSAGE);
            return;
        }
        // Filtra arquivos válidos (tamanho mínimo)
        List<File> arquivosValidos = new ArrayList<>();
        for (File f : arquivos) {
            if (f.length() > 16) arquivosValidos.add(f);
        }
        if (arquivosValidos.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                bundle.getString("mensagem.semCofresValidos"),
                bundle.getString("titulo.aviso"),
                JOptionPane.PLAIN_MESSAGE);
            return;
        }
        // Obtém senha digitada
        char[] senha = campoSenha.getPassword();
        if (senha.length == 0) {
            JOptionPane.showMessageDialog(this,
                bundle.getString("mensagem.senhaVazia"),
                bundle.getString("titulo.aviso"),
                JOptionPane.PLAIN_MESSAGE);
            return; // Senha vazia, encerra sem contar tentativa
        }
        
        // SwingWorker para executar tentativa de desbloqueio em background
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<CofreDesbloqueado> desbloqueados = new ArrayList<>();
            private List<File> arquivosFalhos = new ArrayList<>();
            private List<String> mensagensDestruicao = new ArrayList<>();
            private int menorRestante = Integer.MAX_VALUE;

            @Override
            protected Void doInBackground() throws Exception {
                long inicio = System.currentTimeMillis();
                // Tenta descriptografar cada cofre com a senha fornecida
                for (File arquivo : arquivosValidos) {
                    try {
                        byte[] conteudo = Files.readAllBytes(arquivo.toPath());
                        byte[] salt = Arrays.copyOfRange(conteudo, 0, 16);
                        byte[] dados = Arrays.copyOfRange(conteudo, 16, conteudo.length);
                        SecretKey chave = null;
                        try {
                            chave = CriptografiaUtils.gerarChaveAES(senha, salt);
                            String resultado = CriptografiaUtils.descriptografar(
                                new String(dados, StandardCharsets.UTF_8), chave);
                            JsonObject testJson = JsonParser.parseString(resultado).getAsJsonObject();
                            if (testJson.has("registros")) {
                                desbloqueados.add(new CofreDesbloqueado(arquivo, chave, salt));
                            }
                        } finally {
                            chave = null; // Libera referência da chave para GC
                        }
                    } catch (Exception ex) {
                        arquivosFalhos.add(arquivo);
                    }
                }
                // Se não desbloqueou nenhum cofre, registra tentativas falhas
                if (desbloqueados.isEmpty() && !arquivosFalhos.isEmpty()) {
                    for (File arquivo : arquivosFalhos) {
                        String msgDestr = registrarTentativa(arquivo);
                        if (msgDestr != null) mensagensDestruicao.add(msgDestr);
                        // Lê o número de tentativas já usadas
                        File tentativaFile = new File(arquivo.getPath().replace(".enc", ".attempts"));
                        int usadas = 0;
                        if (tentativaFile.exists()) {
                        	try (BufferedReader br = new BufferedReader(new FileReader(tentativaFile))) {
                        	    br.readLine(); // Pula o timestamp
                        	    String linha = br.readLine(); // Lê o contador
                        	    if (linha != null && linha.matches("\\d+")) {
                        	        usadas = Integer.parseInt(linha);
                        	    }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        int restantes = 10 - usadas;
                        if (restantes < menorRestante) menorRestante = restantes;
                    }
                }
                // Garante delay mínimo de 3 segundos para mitigar ataques de timing
                long decorrido = System.currentTimeMillis() - inicio;
                if (decorrido < 3000) {
                    Thread.sleep(3000 - decorrido);
                }
                return null;
            }

            @Override
            protected void done() {
                // Verifica resultado das tentativas
                boolean nenhumDesbloqueado = desbloqueados.isEmpty();
                if (nenhumDesbloqueado && !arquivosFalhos.isEmpty()) {
                    // Exibe alertas de destruição, se houver
                    if (!mensagensDestruicao.isEmpty()) {
                        StringBuilder builder = new StringBuilder(bundle.getString("mensagem.cofresDestruidos") + "\n\n");
                        for (String msg : mensagensDestruicao) {
                            builder.append("• ").append(msg).append("\n");
                        }
                        JOptionPane.showMessageDialog(TelaInicial.this, builder.toString(),
                            bundle.getString("titulo.aviso"), JOptionPane.PLAIN_MESSAGE);
                    }

                    // Mostra somente a mensagem de tentativas se ainda restarem cofres
                    File[] restantes = pastaDados.listFiles((dir, name) -> name.endsWith(".enc"));
                    if (restantes != null && restantes.length > 0) {
                        JOptionPane.showMessageDialog(TelaInicial.this,
                            bundle.getString("mensagem.senhaIncorreta") + "\n" +
                            bundle.getString("mensagem.tentativasRestantes").replace("{0}", String.valueOf(menorRestante)),
                            bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
                    }

                    Arrays.fill(senha, '\0');
                    return;
                }
                // Caso tenha desbloqueado um ou mais cofres
                CofreDesbloqueado selecionado = null;
                if (desbloqueados.size() == 1) {
                    selecionado = desbloqueados.get(0);
                } else if (desbloqueados.size() > 1) {
                    CofreDesbloqueado[] opcoes = desbloqueados.toArray(new CofreDesbloqueado[0]);
                    CofreDesbloqueado escolha = (CofreDesbloqueado) JOptionPane.showInputDialog(
                        TelaInicial.this, bundle.getString("mensagem.multiplosCofres"),
                        bundle.getString("titulo.selecioneCofre"), JOptionPane.PLAIN_MESSAGE,
                        null, opcoes, opcoes[0]);
                    if (escolha != null) {
                        selecionado = escolha;
                    } else {
                        Arrays.fill(senha, '\0');
                        return; // Usuário cancelou seleção
                    }
                }
                // Exibe mensagem de acesso permitido
                JOptionPane.showMessageDialog(TelaInicial.this,
                    bundle.getString("mensagem.acessoPermitido"),
                    bundle.getString("titulo.sucesso"), JOptionPane.PLAIN_MESSAGE);
                dispose();
                try {
                    // Abre o cofre selecionado
                    CofreManager cofre = new CofreManager(selecionado.arquivo, selecionado.chave, selecionado.salt);
                    cofre.getHistorico().add(timestamp() + " " + bundle.getString("historico.cofreAcessado"));
                    cofre.salvar();
                    // Reseta contador de tentativas do cofre aberto para 0
                    byte[] salt = selecionado.salt;
                    SecretKeySpec chaveHMAC = CriptografiaUtils.gerarChaveHMAC("verificacao_interna".toCharArray(), salt);
                    File attemptsFile = new File(selecionado.arquivo.getPath().replace(".enc", ".attempts"));
                    long ts = System.currentTimeMillis();
                    String base = ts + ":0";
                    String hmac = CriptografiaUtils.gerarHMAC(base, chaveHMAC);
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(attemptsFile))) {
                        bw.write(Long.toString(ts));
                        bw.newLine();
                        bw.write("0");
                        bw.newLine();
                        bw.write(hmac);
                    }
                    Preferences.userRoot().node("s4f3c0d3s." + attemptsFile.getName()).putLong("tsUltimo", ts);

                    new TelaPrincipal(cofre, bundle.getLocale());
                    Arrays.fill(senha, '\0');
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private String registrarTentativa(File arquivo) {
        try {
            // Lê salt do próprio .enc
            byte[] conteudo = java.nio.file.Files.readAllBytes(arquivo.toPath());
            byte[] salt = Arrays.copyOfRange(conteudo, 0, 16);

            // Prepara a chave HMAC
            SecretKeySpec chaveHMAC = CriptografiaUtils.gerarChaveHMAC("verificacao_interna".toCharArray(), salt);

            // Aponta para o .attempts
            File tentativaFile = new File(
                arquivo.getPath().replace(".enc", ".attempts")
            );
            int limite = 10;

            // Se não existir, é adulteração: sobrescreve destrói o cofre
            if (!tentativaFile.exists()) {
                return destruirPorAdulteracao(arquivo);
            }

            // Lê e valida estrutura (3 linhas: timestamp, contador, HMAC)
            List<String> linhas = java.nio.file.Files.readAllLines(tentativaFile.toPath());
            if (linhas.size() != 3) {
                return destruirPorAdulteracao(arquivo);
            }
            long ts = Long.parseLong(linhas.get(0));
            int contador = Integer.parseInt(linhas.get(1));
            String hmacLido = linhas.get(2);

            String base = ts + ":" + contador;
            String hmacEsperado = CriptografiaUtils.gerarHMAC(base, chaveHMAC);

            if (!MessageDigest.isEqual(
                    hmacLido.getBytes("UTF-8"),
                    hmacEsperado.getBytes("UTF-8")
                )) {
                return destruirPorAdulteracao(arquivo);
            }

            // VERIFICAÇÃO DE ROLLBACK (timestamp não pode regredir)
            Preferences prefs = Preferences.userRoot().node("s4f3c0d3s." + arquivo.getName());
            long tsUltimo = prefs.getLong("tsUltimo", 0);
            if (ts < tsUltimo) {
                return destruirPorAdulteracao(arquivo);
            }
            prefs.putLong("tsUltimo", ts);


            if (!MessageDigest.isEqual(
                    hmacLido.getBytes("UTF-8"),
                    hmacEsperado.getBytes("UTF-8")
                )) {
                return destruirPorAdulteracao(arquivo);
            }

            // Incrementa o contador
            contador++;
            long novoTs = System.currentTimeMillis();
            String novoBase = novoTs + ":" + contador;
            String novoHmac = CriptografiaUtils.gerarHMAC(novoBase, chaveHMAC);

            if (contador >= limite) {
                sobrescreverEDeletar(arquivo);
                tentativaFile.delete();
                return arquivo.getName();
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(tentativaFile))) {
                bw.write(Long.toString(novoTs));
                bw.newLine();
                bw.write(Integer.toString(contador));
                bw.newLine();
                bw.write(novoHmac);
            }
            prefs.putLong("tsUltimo", novoTs);


        } catch (Exception ex) {
            System.err.println("Erro ao registrar tentativa: " + ex.getMessage());
        }

        return null;
    }
    
    private String destruirPorAdulteracao(File arquivo) {
        sobrescreverEDeletar(arquivo);
        
        JOptionPane.showMessageDialog(this,
        		bundle.getString("mensagem.adulteracao") + "\n\n" +
        				bundle.getString("mensagem.adulteracaoDetalhe").replace("{0}", arquivo.getName()),
        				bundle.getString("titulo.seguranca"),
            JOptionPane.PLAIN_MESSAGE);
        
        return null;
    }

    private void sobrescreverEDeletar(File arquivo) {
        try {
            long tamanho = arquivo.length();
            SecureRandom rand = new SecureRandom();
            FileOutputStream fos = new FileOutputStream(arquivo);

            byte[] lixo = new byte[4096];
            long escrito = 0;

            while (escrito < tamanho) {
                rand.nextBytes(lixo);
                long restante = tamanho - escrito;
                fos.write(lixo, 0, (int) Math.min(lixo.length, restante));
                escrito += Math.min(lixo.length, restante);
            }

            fos.flush();
            fos.close();

            boolean deletado = arquivo.delete();
            if (!deletado) {
                JOptionPane.showMessageDialog(this, bundle.getString("erro.naoDeletado"), "Erro grave", JOptionPane.PLAIN_MESSAGE);
            }

            File salt = new File(arquivo.getPath().replace(".enc", ".salt"));
            File attempts = new File(arquivo.getPath().replace(".enc", ".attempts"));
            if (salt.exists()) salt.delete();
            if (attempts.exists()) attempts.delete();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, bundle.getString("erro.apagarCofre"), bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
        }
    }

    private static class CofreDesbloqueado {
        public File arquivo;
        public SecretKey chave;
        public byte[] salt;

        public CofreDesbloqueado(File arquivo, SecretKey chave, byte[] salt) {
            this.arquivo = arquivo;
            this.chave = chave;
            this.salt = salt;
        }

        @Override
        public String toString() {
            return arquivo.getName();
        }
    }

    private String timestamp() {
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
               .format(LocalDateTime.now());
    }

    private void criarNovoCofre() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));

        JTextField campoNome = new JTextField();
        campoNome.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        campoNome.setBackground(Color.WHITE);
        campoNome.setBounds(0, 0, 250, 30);

        JLayeredPane painelNome = new JLayeredPane();
        painelNome.setPreferredSize(new Dimension(250, 30));
        painelNome.add(campoNome, Integer.valueOf(1));

        JPasswordField campo1 = new JPasswordField();
        JPasswordField campo2 = new JPasswordField();
        campo1.setEchoChar('•');
        campo2.setEchoChar('•');

        boolean[] visivel = { false, false };

        ImageIcon iconMostrar = new ImageIcon(getClass().getResource("/esconderTemaClaro.png"));
        ImageIcon iconEsconder = new ImageIcon(getClass().getResource("/verTemaClaro.png"));
        ImageIcon iconeMostrar = new ImageIcon(iconMostrar.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
        ImageIcon iconeEsconder = new ImageIcon(iconEsconder.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));

        JLayeredPane painelSenha1 = criarPainelSenhaComOlho(campo1, visivel, 0, iconeMostrar, iconeEsconder);
        JLayeredPane painelSenha2 = criarPainelSenhaComOlho(campo2, visivel, 1, iconeMostrar, iconeEsconder);

        painel.add(new JLabel(bundle.getString("label.nomeCofre")));
        painel.add(Box.createVerticalStrut(5));
        painel.add(painelNome);
        painel.add(Box.createVerticalStrut(10));
        painel.add(new JLabel(bundle.getString("label.novaSenha")));
        painel.add(Box.createVerticalStrut(5));
        painel.add(painelSenha1);
        painel.add(Box.createVerticalStrut(10));
        painel.add(new JLabel(bundle.getString("label.confirmarSenha")));
        painel.add(Box.createVerticalStrut(5));
        painel.add(painelSenha2);

        while (true) {
            int opcao = JOptionPane.showConfirmDialog(this, painel, bundle.getString("titulo.criarCofre"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcao != JOptionPane.OK_OPTION) return;

            String nome = campoNome.getText().trim();
            char[] senha1 = campo1.getPassword();
            char[] senha2 = campo2.getPassword();

            if (nome.isEmpty() || senha1.length == 0 || senha2.length == 0) {
                JOptionPane.showMessageDialog(this, bundle.getString("mensagem.preenchaCampos"), bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
                Arrays.fill(senha1, '\0');
                Arrays.fill(senha2, '\0');
                continue;
            }

            if (!Arrays.equals(senha1, senha2)) {
                JOptionPane.showMessageDialog(this, bundle.getString("mensagem.senhasNaoCoincidem"), bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
                Arrays.fill(senha1, '\0');
                Arrays.fill(senha2, '\0');
                continue;
            }

            String nomeArquivo = nome.toLowerCase().replaceAll("[^a-zA-Z0-9_\\-]", "_");
            File cofreExistente = new File(pastaDados, nomeArquivo + ".enc");
            if (cofreExistente.exists()) {
                JOptionPane.showMessageDialog(this, bundle.getString("mensagem.nomeCofreExistente"), bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
                continue;
            }

            try {
                File novoArquivo = new File(pastaDados, nomeArquivo + ".enc");
                
                byte[] salt = CriptografiaUtils.gerarSalt();
                SecretKey chave = CriptografiaUtils.gerarChaveAES(senha1, salt);

                Arrays.fill(senha1, '\0');
                Arrays.fill(senha2, '\0');

                CofreManager cofre = new CofreManager(novoArquivo, chave, salt);
                cofre.getHistorico().add(timestamp() + " " +  bundle.getString("historico.cofreCriado"));
                cofre.salvar();

                // Inicia .attempts com HMAC usando chave separada
                SecretKeySpec chaveHMAC = CriptografiaUtils.gerarChaveHMAC("verificacao_interna".toCharArray(), salt);
                File attemptsFile = new File(novoArquivo.getPath().replace(".enc", ".attempts"));
                long ts = System.currentTimeMillis();
                String base = ts + ":0";
                String hmac = CriptografiaUtils.gerarHMAC(base, chaveHMAC);

                try (BufferedWriter bw = new BufferedWriter(new FileWriter(attemptsFile))) {
                    bw.write(Long.toString(ts));
                    bw.newLine();
                    bw.write("0");
                    bw.newLine();
                    bw.write(hmac);
                }
                Preferences.userRoot().node("s4f3c0d3s." + attemptsFile.getName()).putLong("tsUltimo", ts);

                JOptionPane.showMessageDialog(this, bundle.getString("mensagem.cofreCriadoSucesso"), bundle.getString("titulo.sucesso"), JOptionPane.PLAIN_MESSAGE);
                break;

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, bundle.getString("mensagem.erroCriarCofre"), bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
                ex.printStackTrace();
                break;
            }
        }
    }
    
    private void importarCofre() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(bundle.getString("titulo.selecionarCofreImportar"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(bundle.getString("filtro.cofres"), "enc"));
        SwingUtilities.invokeLater(() -> {
            fileChooser.getRootPane().setDefaultButton(null);
        });
        int resultado = fileChooser.showOpenDialog(this);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File arquivoSelecionado = fileChooser.getSelectedFile();

        String nomeArquivo = arquivoSelecionado.getName();
        File destino = new File(this.pastaDados, nomeArquivo);

        while (destino.exists()) {
            while (true) {
                String novoNome = (String) JOptionPane.showInputDialog(this,
                        bundle.getString("mensagem.cofreExistenteNovoNome"),
                        bundle.getString("titulo.erro"),
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        nomeArquivo.replace(".enc", ""));

                if (novoNome == null) {
                    return; // Usuário cancelou
                }
                novoNome = novoNome.trim();
                if (novoNome.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        bundle.getString("mensagem.nomeInvalido"),
                        bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
                    continue; // Repete o input
                }
                nomeArquivo = novoNome.toLowerCase().replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".enc";
                destino = new File(pastaDados, nomeArquivo);
                break;
            }
        }

        Object[] opcoes = { "OK", "Cancelar" };
        int escolha = JOptionPane.showOptionDialog(this,
                bundle.getString("mensagem.importarAviso"),
                bundle.getString("titulo.aviso"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                opcoes,
                opcoes[0]);

        if (escolha != 0) {
            return; // Usuário cancelou
        }

        // Tentativas de senha
        int tentativas = 0;
        boolean senhaValida = false;
        String senhaArquivo = null;
        SecretKey chave = null;
        byte[] salt = null;

        // Preparar ícones e estado de visibilidade
        boolean[] visivelImport = { false };
        ImageIcon iconMostrarRaw  = new ImageIcon(getClass().getResource("/esconderTemaClaro.png"));
        ImageIcon iconEsconderRaw = new ImageIcon(getClass().getResource("/verTemaClaro.png"));
        ImageIcon iconeMostrar    = new ImageIcon(iconMostrarRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
        ImageIcon iconeEsconder   = new ImageIcon(iconEsconderRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));

        // Realiza até 3 tentativas
        while (tentativas < 3 && !senhaValida) {
            // Cria o campo de senha com ícone de olho
            JPasswordField campoSenhaImport = new JPasswordField();
            JLayeredPane painelSenhaImport = criarPainelSenhaComOlho(
                campoSenhaImport,
                visivelImport,
                0,
                iconeMostrar,
                iconeEsconder
            );

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            JLabel label = new JLabel(bundle.getString("label.digiteSenha"));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(label);
            content.add(Box.createVerticalStrut(8));
            painelSenhaImport.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(painelSenhaImport);

            // Exibe diálogo de senha
            int res = JOptionPane.showConfirmDialog(
                this,
                content,
                bundle.getString("titulo.atencao"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );
            if (res != JOptionPane.OK_OPTION) return;

            char[] senhaArquivoChars = campoSenhaImport.getPassword();
            if (senhaArquivoChars.length == 0) {
                // Se a senha está vazia, mostra erro imediatamente (sem delay)
                JOptionPane.showMessageDialog(this,
                        bundle.getString("mensagem.senhaVazia"),
                        bundle.getString("titulo.campoVazio"),
                        JOptionPane.PLAIN_MESSAGE);
                continue; // Não conta tentativa nem delay
            }
            
            // Usuário digitou alguma coisa: marca o tempo de início
            long inicio = System.currentTimeMillis();
            
            try {
                // Tenta descriptografar o cofre
                byte[] conteudo = Files.readAllBytes(arquivoSelecionado.toPath());
                salt = Arrays.copyOfRange(conteudo, 0, 16);
                byte[] dados = Arrays.copyOfRange(conteudo, 16, conteudo.length);
                chave = CriptografiaUtils.gerarChaveAES(senhaArquivoChars, salt);
                String resultadoJson = CriptografiaUtils.descriptografar(new String(dados), chave);
                JsonObject testJson = JsonParser.parseString(resultadoJson).getAsJsonObject();
                if (testJson.has("registros")) {
                    senhaValida = true;
                } else {
                    throw new Exception("Formato inválido");
                }
            } catch (Exception e) {
                tentativas++;
                if (tentativas < 3) {
                    // Erro de senha —> feedback virá após o delay
                }
            }

            // Aqui, fora do try/catch: delay se senha não estava vazia
            long decorrido = System.currentTimeMillis() - inicio;
            if (decorrido < 3000) {
                try {
                    Thread.sleep(3000 - decorrido);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            // Agora o feedback (erro ou sucesso)
            if (!senhaValida && senhaArquivoChars.length > 0 && tentativas < 3) {
                JOptionPane.showMessageDialog(this,
                    bundle.getString("mensagem.tentativasRestantesImport")
                          .replace("{0}", String.valueOf(3 - tentativas)),
                    bundle.getString("titulo.erro"),
                    JOptionPane.PLAIN_MESSAGE);
            }
            Arrays.fill(senhaArquivoChars, '\0');
        }

        // Copiar arquivo e registrar (se possível)
        try {
            // Copia o .enc para a pasta de dados
            Files.copy(arquivoSelecionado.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Gera arquivo .attempts com valor 0 e HMAC
            try {
                SecretKeySpec chaveHMAC = CriptografiaUtils.gerarChaveHMAC("verificacao_interna".toCharArray(), salt);
                File attemptsFile = new File(destino.getPath().replace(".enc", ".attempts"));
                long ts = System.currentTimeMillis();
                String base = ts + ":0";
                String hmac = CriptografiaUtils.gerarHMAC(base, chaveHMAC);
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(attemptsFile))) {
                    bw.write(Long.toString(ts));
                    bw.newLine();
                    bw.write("0");
                    bw.newLine();
                    bw.write(hmac);
                }
                Preferences.userRoot().node("s4f3c0d3s." + attemptsFile.getName()).putLong("tsUltimo", ts);

            } catch (Exception ex) {
                // Só registra no log, não interrompe a importação
                ex.printStackTrace();
            }


            if (senhaValida) {
                CofreManager cofreTemp = new CofreManager(destino, chave, salt);
                cofreTemp.getHistorico().add(timestamp() + " " + bundle.getString("historico.cofreImportado"));
                cofreTemp.salvar();
                JOptionPane.showMessageDialog(
                    this,
                    bundle.getString("mensagem.importadoSucesso"),
                    bundle.getString("titulo.importacaoCompleta"),
                    JOptionPane.PLAIN_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    bundle.getString("mensagem.importadoParcial"),
                    bundle.getString("titulo.importacaoParcial"),
                    JOptionPane.PLAIN_MESSAGE
                );
            }

        } catch (IOException e) {
            // Erro na cópia do arquivo: aguarda até 3 segundos se necessário, depois exibe mensagem de erro
            long elapsed = System.currentTimeMillis() - /* tempo do início da última tentativa */ 
                           (tentativas > 0 ? (System.currentTimeMillis() - 0) : 0);
            if (elapsed < 3000) {
                try { Thread.sleep(3000 - elapsed); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            JOptionPane.showMessageDialog(
                this, 
                bundle.getString("erro.importarCofre"),
                bundle.getString("titulo.erro"), 
                JOptionPane.PLAIN_MESSAGE
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String gerarNomeArquivoUnico(String nomeBase) {
        String base = nomeBase.trim().toLowerCase().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String nomeFinal = base;
        int contador = 1;
        while (new File(pastaDados, nomeFinal + ".enc").exists()) {
            nomeFinal = base + "_" + contador;
            contador++;
        }
        return nomeFinal;
    }

    public class TempoUtils {
        private static final DateTimeFormatter FORMATADOR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        public static String timestamp() {
            return LocalDateTime.now().format(FORMATADOR);
        }
    }
    
    public JLayeredPane criarPainelSenhaComOlho(JPasswordField campoSenha, boolean[] visivel, int index, ImageIcon iconeMostrar, ImageIcon iconeEsconder) {
        int larguraCampo = 250;
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(larguraCampo, 30));

        campoSenha.setBounds(0, 0, larguraCampo, 30);
        campoSenha.setBackground(Color.WHITE);
        campoSenha.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 28)
        ));

        JButton botaoOlho = new JButton(iconeMostrar);
        botaoOlho.setBounds(larguraCampo - 30, 0, 30, 30);
        botaoOlho.setContentAreaFilled(false);
        botaoOlho.setBorderPainted(false);
        botaoOlho.setFocusPainted(false);

        botaoOlho.addActionListener(e -> {
            visivel[index] = !visivel[index];
            campoSenha.setEchoChar(visivel[index] ? (char) 0 : '•');
            botaoOlho.setIcon(visivel[index] ? iconeEsconder : iconeMostrar);
        });

        layeredPane.add(campoSenha, Integer.valueOf(1));
        layeredPane.add(botaoOlho, Integer.valueOf(2));

        return layeredPane;
    }

    public static void main(String[] args) {
        LookAndFeelUtils.loadAndSetDefaultFont("/fonts/segoeui.ttf", Font.PLAIN, 13f);
        SwingUtilities.invokeLater(TelaInicial::new);
    }
    
}
