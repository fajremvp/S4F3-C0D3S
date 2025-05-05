
/*
 * S4F3-C0D3S - Recovery Codes Manager
 * Developed by Fajre
 * Originally distributed exclusively via the author's GitHub: https://github.com/fajremvp/S4F3-C0D3S
 * Licensed under the MIT License
 */

package s4f3c0d3s;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import s4f3c0d3s.TelaInicial.TempoUtils;

public class TelaPrincipal extends JFrame {
	private static final long serialVersionUID = 1L;

	private final CofreManager cofre;
	private JPanel painelRegistros;
	private final List<String> historicoLogs = new ArrayList<>();
	private final List<JPanel> paineisAtivos = new ArrayList<>();
	private final List<Registro> registros = new ArrayList<>();
	private JTextField campoBusca;
	private boolean sessaoEncerrada = false;

	private Timer sessaoTimer;

	private boolean saidaManual = false;

	private ResourceBundle bundle;
	private Preferences prefs = Preferences.userRoot().node("s4f3c0d3s");

	public TelaPrincipal(CofreManager cofre, Locale locale) {

		this.bundle = ResourceBundle.getBundle("messages", locale);

		ImageIcon iconeJanela = new ImageIcon(getClass().getResource("/logoBarraDeTarefas.png"));
		setIconImage(iconeJanela.getImage());

		this.cofre = cofre;
		this.registros.addAll(cofre.getRegistros());

		setTitle(bundle.getString("telaPrincipal.titulo"));

		setSize(785, 650);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		iniciarTimerSessao();
		addActivityListeners();

		JPanel painelPrincipal = new JPanel(new BorderLayout());
		painelRegistros = new JPanel();
		painelRegistros.setLayout(new BoxLayout(painelRegistros, BoxLayout.Y_AXIS));

		// Envolve o painelRegistros com um JPanel para não esticar à força
		JPanel painelWrapper = new JPanel(new BorderLayout());
		painelWrapper.add(painelRegistros, BorderLayout.NORTH); // Força a alinhar ao topo

		JScrollPane scrollPane = new JScrollPane(painelWrapper);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Rolagem mais suave

		painelPrincipal.add(scrollPane, BorderLayout.CENTER);

		campoBusca = new JTextField(10);
		campoBusca.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				filtrar();
				resetarTimer();
			}

			public void removeUpdate(DocumentEvent e) {
				filtrar();
				resetarTimer();
			}

			public void changedUpdate(DocumentEvent e) {
				filtrar();
				resetarTimer();
			}
		});

		JPanel painelSuperior = new JPanel(new FlowLayout(FlowLayout.CENTER));
		painelSuperior.add(new JLabel(bundle.getString("label.buscar")));
		painelSuperior.add(campoBusca);

		JButton botaoAdicionar = new JButton(bundle.getString("botao.adicionarRegistro"));

		JButton botaoImportar = new JButton(bundle.getString("botao.importarCodigos"));

		JButton botaoExportar = new JButton(bundle.getString("botao.exportar"));

		JButton botaoHistorico = new JButton(bundle.getString("botao.historico"));

		JButton botaoOpcoes = new JButton(bundle.getString("botao.opcoes"));

		JButton botaoSair = new JButton(bundle.getString("botao.sair"));

		botaoAdicionar.addActionListener(e -> {
			resetarTimer();
			new JanelaAdicionarRegistro(this);
		});
		botaoImportar.addActionListener(e -> {
			resetarTimer();
			importarCofre();
		});
		botaoExportar.addActionListener(e -> {
			resetarTimer();
			exportarCofre();
		});
		botaoHistorico.addActionListener(e -> {
			resetarTimer();
			exibirHistorico();
		});
		botaoOpcoes.addActionListener(e -> {
			resetarTimer();
			exibirOpcoes();
		});
		botaoSair.addActionListener(e -> {
			saidaManual = true;
			if (sessaoTimer != null)
				sessaoTimer.stop();
			registrarSaida(bundle.getString("log.saidaManual"));
			cofre.encerrarSessao();
			dispose();
			new TelaInicial();
		});

		painelSuperior.add(botaoAdicionar);
		painelSuperior.add(botaoImportar);
		painelSuperior.add(botaoExportar);
		painelSuperior.add(botaoHistorico);
		painelSuperior.add(botaoOpcoes);
		painelSuperior.add(botaoSair);

		painelPrincipal.add(painelSuperior, BorderLayout.NORTH);
		add(painelPrincipal);

		carregarRegistros();
		SwingUtilities.invokeLater(() -> {
			scrollPane.getVerticalScrollBar().setValue(0);
		});

		setVisible(true);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!saidaManual)
					registrarSaida(bundle.getString("log.fecharManual"));
			}

			@Override
			public void windowClosed(WindowEvent e) {
				if (!saidaManual)
					registrarSaida(bundle.getString("log.fecharManual"));
			}
		});
	}

	private void iniciarTimerSessao() {
		try {
			int tempoMinutos = cofre.getTempoSessaoMinutos() > 0 ? cofre.getTempoSessaoMinutos() : 5;
			int timeoutMs = tempoMinutos * 60 * 1000;

			// Aqui, o timer já se para sozinho na primeira execução
			sessaoTimer = new Timer(timeoutMs, e -> {
				((Timer) e.getSource()).stop(); // Garante que só dispare uma vez
				encerrarSessao();
			});
			sessaoTimer.setInitialDelay(timeoutMs);
			sessaoTimer.start();
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, bundle.getString("erro.iniciarTemporizador"), bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
		}
	}

	private void resetarTimer() {
		if (sessaoEncerrada) {
			// Sessão já encerrada, não faz nada
			return;
		}
		if (sessaoTimer != null) {
			sessaoTimer.restart();
		}
	}

	private void encerrarSessao() {
	    if (sessaoEncerrada)
	        return;
	    sessaoEncerrada = true;

	    // Registrar saída
	    registrarSaida(bundle.getString("log.expirou"));

	    // Parar o timer
	    if (sessaoTimer != null) {
	        sessaoTimer.stop(); // Para de vez o timer
	        sessaoTimer = null;
	    }

	    // Limpeza e encerramento da sessão
	    cofre.encerrarSessao();
	    dispose(); // Fecha a TelaPrincipal

	    // Volta para a TelaInicial após a sessão
	    SwingUtilities.invokeLater(() -> new TelaInicial(true)); // Exibe a tela inicial com aviso de expiração
	}

	private void addActivityListeners() {
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (event instanceof MouseEvent || event instanceof KeyEvent) {
				resetarTimer();
			}
		}, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
	}

	private void carregarRegistros() {
		painelRegistros.removeAll();
		paineisAtivos.clear();

		// Se não houver registros, mostra a mensagem de "Cofre Vazio"

		// Sincroniza a lista local de registros com o cofre
		registros.clear();
		registros.addAll(cofre.getRegistros());

		if (registros.isEmpty()) {
			painelRegistros.setLayout(new BoxLayout(painelRegistros, BoxLayout.Y_AXIS));

			painelRegistros.add(Box.createVerticalStrut(250));

			// Texto centralizado
			JLabel labelVazio = new JLabel(bundle.getString("mensagem.cofreVazio"));
			labelVazio.setFont(labelVazio.getFont().deriveFont(16f));
			labelVazio.setAlignmentX(Component.CENTER_ALIGNMENT);
			labelVazio.setForeground(new Color(180, 180, 180));
			painelRegistros.add(labelVazio);

			// Espaço abaixo
			painelRegistros.add(Box.createVerticalGlue());

			painelRegistros.revalidate();
			painelRegistros.repaint();
			return;
		}

		// Caso contrário, exibe normalmente cada painel de código
		registros.clear(); // Sincroniza com o cofre
		registros.addAll(cofre.getRegistros());

		for (Registro r : registros) {
			JPanel painel = criarPainelRegistro(r);
			painelRegistros.add(painel);
			paineisAtivos.add(painel);
		}

		painelRegistros.revalidate();
		painelRegistros.repaint();
	}

	public void adicionarRegistro(String servico, String usuario, String codigos, String notas) {
		// Se era o primeiro registro, limpa o placeholder de "cofre vazio"
		boolean primeiro = registros.isEmpty();

		// Cria e persiste o novo registro
		Registro r = new Registro(servico, usuario, codigos, notas);
		registros.add(0, r);
		cofre.getRegistros().add(0, r); // Força a ficar o registro novo no topo

		if (primeiro) {
			painelRegistros.removeAll();
			paineisAtivos.clear();
		}

		// Adiciona o painel do novo registro
		JPanel painel = criarPainelRegistro(r);
		// Depois:
		painelRegistros.add(painel, 0); // Insere o JPanel novo no topo
		paineisAtivos.add(0, painel); // Mantém o array de painéis consistente

		// Registra no histórico
		String log = timestamp() + " " + MessageFormat.format(bundle.getString("log.registroAdicionado"), servico);
		if (!cofre.getHistorico().contains(log)) {
			cofre.getHistorico().add(log);
			cofre.salvar();
		}

		// Atualiza a interface
		painelRegistros.revalidate();
		painelRegistros.repaint();
	}

	private JPanel criarPainelRegistro(Registro r) {
		// Painel principal:
		JPanel painelCodigo = new JPanel(new BorderLayout());
		painelCodigo.setBorder(BorderFactory.createTitledBorder(r.servico));
		painelCodigo.setBackground(Color.WHITE);
		painelCodigo.setOpaque(true);

		// Conta
		JTextField campoConta = new JTextField(r.usuario);
		campoConta.setEditable(false);
		campoConta.setBorder(null);
		JPanel painelConta = new JPanel(new FlowLayout(FlowLayout.LEFT));
		painelConta.add(campoConta);

		// Códigos
		JTextArea areaTexto = new JTextArea(r.codigos);
		areaTexto.setEditable(false);
		areaTexto.setLineWrap(true);
		areaTexto.setWrapStyleWord(true);
		JScrollPane scrollCodigos = new JScrollPane(areaTexto);

		// Notas
		JTextArea areaNotas = null;
		JPanel painelCentro = new JPanel();
		painelCentro.setLayout(new javax.swing.BoxLayout(painelCentro, javax.swing.BoxLayout.Y_AXIS));
		painelCentro.add(scrollCodigos);
		if (r.notas != null && !r.notas.trim().isEmpty()) {
			areaNotas = new JTextArea(r.notas);
			areaNotas.setEditable(false);
			areaNotas.setLineWrap(true);
			areaNotas.setWrapStyleWord(true);
			areaNotas.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
			painelCentro.add(javax.swing.Box.createVerticalStrut(5));
			painelCentro.add(areaNotas);
		}

		// Esconde códigos e notas por padrão
		painelCentro.setVisible(false);

		// Botões (também invisíveis por padrão)
		JButton botaoEditar = new JButton(bundle.getString("botao.editar"));
		JButton botaoRemover = new JButton(bundle.getString("botao.remover"));
		JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
		painelBotoes.add(botaoEditar);
		painelBotoes.add(botaoRemover);
		painelBotoes.setVisible(false);

		// Ações dos botões
		botaoEditar.addActionListener(e -> {
			resetarTimer();
			new JanelaEditarRegistro(this, r, painelCodigo);
		});
		botaoRemover.addActionListener(e -> {
		    resetarTimer();
		    int confirm = JOptionPane.showOptionDialog(this, bundle.getString("mensagem.removerRegistro"),
		            bundle.getString("titulo.confirmar"), JOptionPane.DEFAULT_OPTION,
		            JOptionPane.PLAIN_MESSAGE, null,
		            new Object[] { bundle.getString("botao.sim"), bundle.getString("botao.cancelar") },
		            bundle.getString("botao.cancelar")); // O botão "Cancelar" será o padrão

		    if (confirm == 0) {
		        cofre.removerRegistro(r);
		        String logRemocao = timestamp() + " " + MessageFormat.format(bundle.getString("log.registroRemovido"), r.servico);
		        if (!cofre.getHistorico().contains(logRemocao)) {
		            cofre.getHistorico().add(logRemocao);
		            cofre.salvar();
		        }
		        carregarRegistros(); // <- Reconstrói tudo, e mostra placeholder se estiver vazio
		    }
		});

		MouseAdapter showHide = new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				painelBotoes.setVisible(true);
				painelCentro.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				Component src = (Component) e.getSource();
				Point pt = SwingUtilities.convertPoint(src, e.getPoint(), painelCodigo);
				if (!painelCodigo.contains(pt)) {
					painelBotoes.setVisible(false);
					painelCentro.setVisible(false);
				}
			}
		};
		MouseMotionAdapter mover = new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				painelBotoes.setVisible(true);
				painelCentro.setVisible(true);
			}
		};

		// Anexa aos pontos críticos (incluindo viewport do scroll e área de notas)
		painelCodigo.addMouseListener(showHide);
		painelCodigo.addMouseMotionListener(mover);

		painelConta.addMouseListener(showHide);
		painelConta.addMouseMotionListener(mover);

		scrollCodigos.getViewport().addMouseListener(showHide);
		scrollCodigos.getViewport().addMouseMotionListener(mover);

		areaTexto.addMouseListener(showHide);
		areaTexto.addMouseMotionListener(mover);

		if (areaNotas != null) {
			painelCentro.addMouseListener(showHide);
			painelCentro.addMouseMotionListener(mover);
			areaNotas.addMouseListener(showHide);
			areaNotas.addMouseMotionListener(mover);
		}

		// Montagem final
		painelCodigo.add(painelConta, BorderLayout.NORTH);
		painelCodigo.add(painelCentro, BorderLayout.CENTER);
		painelCodigo.add(painelBotoes, BorderLayout.SOUTH);

		return painelCodigo;
	}

	private void filtrar() {
		String termo = campoBusca.getText().toLowerCase();
		painelRegistros.removeAll();
		for (int i = 0; i < registros.size(); i++) {
			Registro r = registros.get(i);
			JPanel painel = paineisAtivos.get(i);
			if (r.servico.toLowerCase().contains(termo) || r.usuario.toLowerCase().contains(termo)) {
				painelRegistros.add(painel);
			}
		}
		painelRegistros.revalidate();
		painelRegistros.repaint();
	}

	private void exibirHistorico() {
		JTextArea area = new JTextArea();

		List<String> combinado = new ArrayList<>(new LinkedHashSet<>(cofre.getHistorico()));

		if (combinado.isEmpty()) {
			area.setText(bundle.getString("mensagem.semHistorico"));
		} else {
			for (String linha : combinado) {
				area.append("• " + linha + "\n");
			}
		}

		area.setEditable(false);
		area.setFocusable(false);
		area.setCaretColor(new Color(0, 0, 0, 0));
		area.setLineWrap(true);
		area.setWrapStyleWord(true);

		JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(500, 300));

		// Substitui showMessageDialog por showOptionDialog com botão "Voltar"
		JOptionPane.showOptionDialog(this, scroll, bundle.getString("titulo.historico"), JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, new Object[] { bundle.getString("botao.voltar") },
				bundle.getString("botao.voltar"));
	}

	private void exibirOpcoes() {
		JPanel painel = new JPanel();
		painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));

		JButton alterar = new JButton(bundle.getString("botao.alterarSenha"));
		JButton sessao = new JButton(bundle.getString("botao.tempoSessao"));
		JButton apagar = new JButton(bundle.getString("botao.apagarCofre"));

		alterar.setAlignmentX(Component.CENTER_ALIGNMENT);
		sessao.setAlignmentX(Component.CENTER_ALIGNMENT);
		apagar.setAlignmentX(Component.CENTER_ALIGNMENT);

		painel.add(alterar);
		painel.add(Box.createVerticalStrut(10));
		painel.add(sessao);
		painel.add(Box.createVerticalStrut(10));
		painel.add(apagar);
		painel.add(Box.createVerticalStrut(15));

		// Cria o JOptionPane sem opções
		JOptionPane opcoesPane = new JOptionPane(painel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null,
				new Object[] {}, null);
		JDialog dialog = opcoesPane.createDialog(this, bundle.getString("titulo.opcoes"));

		JButton botaoVoltar = new JButton(bundle.getString("botao.voltar"));
		botaoVoltar.setAlignmentX(Component.CENTER_ALIGNMENT);
		painel.add(botaoVoltar);

		botaoVoltar.addActionListener(e -> dialog.dispose());

		// Ações dos botões
		alterar.addActionListener(e -> {
			dialog.dispose();
			alterarSenha();
		});

		sessao.addActionListener(e -> {
			dialog.dispose();
			configurarTempoLimite();
		});

		apagar.addActionListener(e -> {
			dialog.dispose();
			apagarCofre();
		});

		dialog.pack();
		dialog.setVisible(true);
	}

	private String timestamp() {
		return TempoUtils.timestamp();
	}

	private class JanelaAdicionarRegistro extends JDialog {
		private static final long serialVersionUID = 1L;

		public JanelaAdicionarRegistro(JFrame parent) {
			super(parent, bundle.getString("titulo.adicionarRegistro"), true);
			setSize(500, 550);
			setLocationRelativeTo(parent);

			JPanel painelFormulario = new JPanel();
			painelFormulario.setLayout(new BoxLayout(painelFormulario, BoxLayout.Y_AXIS));
			painelFormulario.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

			// Serviço
			JLabel labelServico = new JLabel(bundle.getString("label.servico"));
			labelServico.setAlignmentX(Component.LEFT_ALIGNMENT);
			painelFormulario.add(labelServico);
			JTextField campoNome = new JTextField();
			campoNome.setAlignmentX(Component.LEFT_ALIGNMENT);
			campoNome.setMaximumSize(new Dimension(Integer.MAX_VALUE, campoNome.getPreferredSize().height));
			painelFormulario.add(campoNome);
			painelFormulario.add(Box.createVerticalStrut(8));

			// Usuário/Email
			JLabel labelUsuario = new JLabel(bundle.getString("label.usuario"));
			labelUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);
			painelFormulario.add(labelUsuario);
			JTextField campoUsuario = new JTextField();
			campoUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);
			campoUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, campoUsuario.getPreferredSize().height));
			painelFormulario.add(campoUsuario);
			painelFormulario.add(Box.createVerticalStrut(12));

			// Códigos
			JLabel labelCodigos = new JLabel(bundle.getString("label.codigos"));
			labelCodigos.setAlignmentX(Component.LEFT_ALIGNMENT);
			painelFormulario.add(labelCodigos);
			JTextArea campoCodigos = new JTextArea(10, 20);
			campoCodigos.setLineWrap(true);
			campoCodigos.setWrapStyleWord(true);
			JScrollPane scrollCodigos = new JScrollPane(campoCodigos);
			scrollCodigos.setAlignmentX(Component.LEFT_ALIGNMENT);
			scrollCodigos.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollCodigos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
			painelFormulario.add(scrollCodigos);
			painelFormulario.add(Box.createVerticalStrut(12));

			// Notas
			JLabel labelNotas = new JLabel(bundle.getString("label.notas"));
			labelNotas.setAlignmentX(Component.LEFT_ALIGNMENT);
			painelFormulario.add(labelNotas);
			JTextArea campoNotas = new JTextArea(5, 20);
			campoNotas.setLineWrap(true);
			campoNotas.setWrapStyleWord(true);
			JScrollPane scrollNotas = new JScrollPane(campoNotas);
			scrollNotas.setAlignmentX(Component.LEFT_ALIGNMENT);
			scrollNotas.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollNotas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
			painelFormulario.add(scrollNotas);

			// Botões
			JButton botaoAdicionar = new JButton(bundle.getString("botao.adicionar"));
			JButton botaoCancelar = new JButton(bundle.getString("botao.cancelar"));
			JPanel painelBotoes = new JPanel();
			painelBotoes.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
			painelBotoes.add(botaoAdicionar);
			painelBotoes.add(botaoCancelar);

			botaoAdicionar.addActionListener(e -> {
				String nome = campoNome.getText().trim();
				String usuario = campoUsuario.getText().trim();
				String codigos = campoCodigos.getText().trim();
				String notas = campoNotas.getText().trim();
				if (!nome.isEmpty() && !usuario.isEmpty() && !codigos.isEmpty()) {
					adicionarRegistro(nome, usuario, codigos, notas);
					dispose();
				} else {
					JOptionPane.showMessageDialog(this, bundle.getString("mensagem.preenchaObrigatorios"),
							bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
				}
			});
			botaoCancelar.addActionListener(e -> dispose());

			getContentPane().setLayout(new BorderLayout(10, 10));
			add(painelFormulario, BorderLayout.CENTER);
			add(painelBotoes, BorderLayout.SOUTH);

			SwingUtilities.invokeLater(() -> getRootPane().requestFocusInWindow());
			setVisible(true);
		}
	}

	private class JanelaEditarRegistro extends JDialog {
		private static final long serialVersionUID = 1L;

		public JanelaEditarRegistro(JFrame parent, Registro r, JPanel painelAntigo) {
			super(parent, bundle.getString("titulo.editarRegistro"), true);
			setSize(500, 550);
			setLocationRelativeTo(parent);

			String origServico = r.servico;
			String origUsuario = r.usuario;
			String origCodigos = r.codigos;
			String origNotas = r.notas != null ? r.notas : "";

			JPanel painelFormulario = new JPanel();
			painelFormulario.setLayout(new BoxLayout(painelFormulario, BoxLayout.Y_AXIS));
			painelFormulario.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

			// Serviço
			JLabel labelServico = new JLabel(bundle.getString("label.servico"));
			labelServico.setAlignmentX(Component.LEFT_ALIGNMENT);
			painelFormulario.add(labelServico);
			JTextField campoNome = new JTextField(origServico);
			campoNome.setAlignmentX(Component.LEFT_ALIGNMENT);
			campoNome.setMaximumSize(new Dimension(Integer.MAX_VALUE, campoNome.getPreferredSize().height));
			painelFormulario.add(campoNome);
			painelFormulario.add(Box.createVerticalStrut(8));

			// Usuário/Email
			JLabel labelUsuario = new JLabel(bundle.getString("label.usuario"));
			labelUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);
			painelFormulario.add(labelUsuario);
			JTextField campoUsuario = new JTextField(origUsuario);
			campoUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);
			campoUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, campoUsuario.getPreferredSize().height));
			painelFormulario.add(campoUsuario);
			painelFormulario.add(Box.createVerticalStrut(12));

			// Códigos
			JLabel labelCodigos = new JLabel(bundle.getString("label.codigos"));
			labelCodigos.setAlignmentX(Component.LEFT_ALIGNMENT);
			painelFormulario.add(labelCodigos);
			JTextArea campoCodigos = new JTextArea(origCodigos, 10, 20);
			campoCodigos.setLineWrap(true);
			campoCodigos.setWrapStyleWord(true);
			JScrollPane scrollCodigos = new JScrollPane(campoCodigos);
			scrollCodigos.setAlignmentX(Component.LEFT_ALIGNMENT);
			scrollCodigos.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			painelFormulario.add(scrollCodigos);
			painelFormulario.add(Box.createVerticalStrut(12));

			// Notas (opcional)
			JLabel labelNotas = new JLabel(bundle.getString("label.notas"));
			labelNotas.setAlignmentX(Component.LEFT_ALIGNMENT);
			painelFormulario.add(labelNotas);
			JTextArea campoNotas = new JTextArea(origNotas, 5, 20);
			campoNotas.setLineWrap(true);
			campoNotas.setWrapStyleWord(true);
			JScrollPane scrollNotas = new JScrollPane(campoNotas);
			scrollNotas.setAlignmentX(Component.LEFT_ALIGNMENT);
			scrollNotas.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			painelFormulario.add(scrollNotas);

			// Botões
			JButton botaoSalvar = new JButton(bundle.getString("botao.salvar"));
			JButton botaoCancelar = new JButton(bundle.getString("botao.cancelar"));
			JPanel painelBotoes = new JPanel();
			painelBotoes.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
			painelBotoes.add(botaoSalvar);
			painelBotoes.add(botaoCancelar);

			botaoSalvar.addActionListener(e -> {
				String novoServico = campoNome.getText().trim();
				String novoUsuario = campoUsuario.getText().trim();
				String novoCodigos = campoCodigos.getText().trim();
				String novoNotas = campoNotas.getText().trim();

				if (novoServico.isEmpty() || novoUsuario.isEmpty() || novoCodigos.isEmpty()) {
					JOptionPane.showMessageDialog(this, bundle.getString("mensagem.preenchaObrigatorios"),
							bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
					return;
				}

				if (novoServico.equals(origServico) && novoUsuario.equals(origUsuario)
						&& novoCodigos.equals(origCodigos) && novoNotas.equals(origNotas)) {
					dispose();
					return;
				}

				r.servico = novoServico;
				r.usuario = novoUsuario;
				r.codigos = novoCodigos;
				r.notas = novoNotas;
				cofre.salvar();
				   String logEdicao = timestamp()
				       + " " + MessageFormat.format(bundle.getString("log.registroEditado"),
				                              origServico, novoServico);
				   if (!cofre.getHistorico().contains(logEdicao)) {
				       cofre.getHistorico().add(logEdicao);
				       cofre.salvar();
				   }
				   
				   TelaPrincipal.this.carregarRegistros();

				dispose();
			});
			botaoCancelar.addActionListener(e -> dispose());

			getContentPane().setLayout(new BorderLayout(10, 10));
			add(painelFormulario, BorderLayout.CENTER);
			add(painelBotoes, BorderLayout.SOUTH);

			SwingUtilities.invokeLater(() -> getRootPane().requestFocusInWindow());
			setVisible(true);
		}
	}

	private void configurarTempoLimite() {
		JPanel painel = new JPanel(new FlowLayout());

		int tempoAtualMinutos = cofre.getTempoSessaoMinutos();
		SpinnerNumberModel model = new SpinnerNumberModel(tempoAtualMinutos, 1, 60, 1);
		JSpinner spinner = new JSpinner(model);

		painel.add(new JLabel(bundle.getString("label.tempoSessao")));
		painel.add(spinner);

		Object[] opcoes = { bundle.getString("botao.salvar"), bundle.getString("botao.cancelar") };
		int opcao = JOptionPane.showOptionDialog(this, painel, bundle.getString("titulo.tempoSessao"),
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opcoes, opcoes[0]);

		if (opcao == 0) { // Clicou em "Salvar"
			int novoTempo = (int) spinner.getValue();
			if (novoTempo != tempoAtualMinutos) {
				cofre.setTempoSessaoMinutos(novoTempo);

				int novoTimeout = novoTempo * 60 * 1000;
				sessaoTimer.setInitialDelay(novoTimeout);
				sessaoTimer.restart();

				String log = timestamp() + " "
						+ bundle.getString("log.tempoSessaoAlterado").replace("{0}", String.valueOf(novoTempo));
				if (!cofre.getHistorico().contains(log)) {
					cofre.getHistorico().add(log);
					cofre.salvar();
				}

				String mensagem = MessageFormat.format(bundle.getString("mensagem.tempoDefinido"), novoTempo);
				JOptionPane.showMessageDialog(this, mensagem, bundle.getString("titulo.sucesso"),
						JOptionPane.PLAIN_MESSAGE);
			}
		}
	}

	private void alterarSenha() {
		boolean[] visivel = { false, false, false };
		JPasswordField campoSenhaAtual = new JPasswordField();
		JPasswordField campoNova = new JPasswordField();
		JPasswordField campoConfirmar = new JPasswordField();

		ImageIcon iconMostrarRaw = new ImageIcon(getClass().getResource("/esconderTemaClaro.png"));
		ImageIcon iconEsconderRaw = new ImageIcon(getClass().getResource("/verTemaClaro.png"));
		ImageIcon iconeMostrar = new ImageIcon(iconMostrarRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
		ImageIcon iconeEsconder = new ImageIcon(
				iconEsconderRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));

		JLayeredPane paneAtual = criarPainelSenhaComOlho(campoSenhaAtual, visivel, 0, iconeMostrar, iconeEsconder);
		JLayeredPane paneNova = criarPainelSenhaComOlho(campoNova, visivel, 1, iconeMostrar, iconeEsconder);
		JLayeredPane paneConfirmar = criarPainelSenhaComOlho(campoConfirmar, visivel, 2, iconeMostrar, iconeEsconder);

		JPanel painel = new JPanel();
		painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
		painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		painel.add(new JLabel(bundle.getString("label.senhaAtual")));
		painel.add(Box.createVerticalStrut(5));
		painel.add(paneAtual);
		painel.add(Box.createVerticalStrut(15));
		painel.add(new JLabel(bundle.getString("label.novaSenha")));
		painel.add(Box.createVerticalStrut(5));
		painel.add(paneNova);
		painel.add(Box.createVerticalStrut(15));
		painel.add(new JLabel(bundle.getString("label.confirmarNovaSenha")));
		painel.add(Box.createVerticalStrut(5));
		painel.add(paneConfirmar);

		while (true) {
			int opcao = JOptionPane.showConfirmDialog(this, painel, bundle.getString("titulo.alterarSenha"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (opcao != JOptionPane.OK_OPTION)
				return;

			char[] atual = campoSenhaAtual.getPassword();
			char[] nova = campoNova.getPassword();
			char[] confirmar = campoConfirmar.getPassword();

			if (atual.length == 0 || nova.length == 0 || confirmar.length == 0) {
			    JOptionPane.showMessageDialog(this, bundle.getString("mensagem.preenchaObrigatorios"),
			        bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
			    continue;
			}

			if (!Arrays.equals(nova, confirmar)) {
			    JOptionPane.showMessageDialog(this, bundle.getString("mensagem.novaSenhaNaoConfere"),
			        bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
			    continue;
			}

			try {
				cofre.recriptografar(atual, nova);
				Arrays.fill(atual, '\0');
				Arrays.fill(nova, '\0');
				Arrays.fill(confirmar, '\0');
				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.sucessoAlterarSenha"),
						bundle.getString("titulo.sucesso"), JOptionPane.PLAIN_MESSAGE);
				String log = timestamp() + " " + bundle.getString("log.senhaAlterada");
				if (!cofre.getHistorico().contains(log)) {
					cofre.getHistorico().add(log);
					cofre.salvar();
				}
				break;
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.senhaAtualIncorreta"),
						bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
			}
			
			Arrays.fill(atual, '\0');  // Limpa a senha atual
		    Arrays.fill(nova, '\0');    // Limpa a nova senha
		    Arrays.fill(confirmar, '\0'); // Limpa a confirmação da nova senha
		}
	}

	private void registrarSaida(String motivo) {
		String log = timestamp() + " - " + motivo;
		if (!cofre.getHistorico().contains(log)) {
			cofre.getHistorico().add(log);
			cofre.salvar();
		}
	}

	private void importarCofre() {
		// Abrir o explorador de arquivos
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(bundle.getString("titulo.selecionarCofreImportar"));
		fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Cofres (.enc)", "enc"));

		int resultado = fileChooser.showOpenDialog(this);

		if (resultado != JFileChooser.APPROVE_OPTION) {
			return; // Se o usuário cancelar a seleção, não faz nada
		}

		File arquivoSelecionado = fileChooser.getSelectedFile();

		// Variável para contar tentativas de senha incorreta
		int tentativas = 0;
		String senhaArquivo = null;
		boolean senhaValida = false;
		SecretKey chave = null;
		byte[] salt = null;

		while (tentativas < 3 && !senhaValida) {
			boolean[] senhaVisivel = { false };

			// Cria o campo de senha
			JPasswordField passwordField = new JPasswordField();
			passwordField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
					BorderFactory.createEmptyBorder(5, 5, 5, 28)));
			passwordField.setBackground(Color.WHITE);

			// Carrega ícones (mesmos recursos de TelaInicial)
			ImageIcon iconMostrarRaw = new ImageIcon(getClass().getResource("/esconderTemaClaro.png"));
			ImageIcon iconEsconderRaw = new ImageIcon(getClass().getResource("/verTemaClaro.png"));
			ImageIcon iconeMostrar = new ImageIcon(
					iconMostrarRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
			ImageIcon iconeEsconder = new ImageIcon(
					iconEsconderRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));

			// Monta o JLayeredPane
			int larguraCampo = 260; // ajuste conforme achar melhor
			JLayeredPane layeredPane = new JLayeredPane();
			layeredPane.setPreferredSize(new Dimension(larguraCampo, 30));

			// Posiciona o campo de senha
			passwordField.setBounds(0, 0, larguraCampo, 30);

			// Cria o botão “olhinho”
			JButton btnOlho = new JButton(iconeMostrar);
			btnOlho.setBounds(larguraCampo - 30, 0, 30, 30);
			btnOlho.setBorderPainted(false);
			btnOlho.setContentAreaFilled(false);
			btnOlho.setFocusPainted(false);

			// Alterna visibilidade da senha ao clicar
			btnOlho.addActionListener(e -> {
				senhaVisivel[0] = !senhaVisivel[0];
				passwordField.setEchoChar(senhaVisivel[0] ? (char) 0 : '•');
				btnOlho.setIcon(senhaVisivel[0] ? iconeEsconder : iconeMostrar);
			});

			// Adiciona ao layered pane
			layeredPane.add(passwordField, Integer.valueOf(1));
			layeredPane.add(btnOlho, Integer.valueOf(2));

			// Monta o painel de conteúdo
			JPanel content = new JPanel();
			content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
			JLabel lbl = new JLabel(bundle.getString("label.digiteSenhaImport"));
			lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
			content.add(lbl);
			content.add(Box.createVerticalStrut(5));
			layeredPane.setAlignmentX(Component.LEFT_ALIGNMENT);
			content.add(layeredPane);

			// Exibe o diálogo
			int option = JOptionPane.showConfirmDialog(this, content, bundle.getString("titulo.atencao"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (option != JOptionPane.OK_OPTION) {
				return;
			}

			// Lê a senha oculta
			senhaArquivo = new String(passwordField.getPassword());

			// Se o usuário cancelar (retorna null), interrompe o processo sem contar a tentativa
			if (senhaArquivo == null) {
				return;
			}

			if (senhaArquivo.isEmpty()) {
				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.senhaVaziaImport"),
						bundle.getString("titulo.campoVazio"), JOptionPane.PLAIN_MESSAGE);
				// NÃO conta tentativa
				continue;
			}

			if (senhaArquivo.isEmpty()) {
				tentativas++;
				if (tentativas >= 3) {
					JOptionPane.showMessageDialog(this, bundle.getString("mensagem.limiteTentativas"),
							bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
					String log = timestamp() + " " + bundle.getString("log.limiteTentativasImport");
					cofre.getHistorico().add(log);
					cofre.salvar();
					if (sessaoTimer != null) {
						sessaoTimer.stop();
						sessaoTimer = null;
					}
					
					saidaManual = true;
					dispose();
					SwingUtilities.invokeLater(() -> new TelaInicial(false));
					return;
				} else {
					JOptionPane.showMessageDialog(this, bundle.getString("mensagem.tentativasRestantesImport"),
							bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
					continue; // Repete a solicitação
				}
			}

			try {
				byte[] conteudoArquivo = Files.readAllBytes(arquivoSelecionado.toPath());
				salt = Arrays.copyOfRange(conteudoArquivo, 0, 16); // O salt ocupa os primeiros 16 bytes
				byte[] dadosCriptografados = Arrays.copyOfRange(conteudoArquivo, 16, conteudoArquivo.length); // O restante é o conteúdo criptografado

				chave = CriptografiaUtils.gerarChaveAES(senhaArquivo.toCharArray(), salt);
				// Tenta descriptografar para validar a senha
				String dadosDescriptografados = CriptografiaUtils.descriptografar(new String(dadosCriptografados),
						chave);
				// Se não lançar exceção, a senha é válida
				senhaValida = true;
			} catch (Exception ex) {
				tentativas++;
				if (tentativas >= 3) {
					JOptionPane.showMessageDialog(this, bundle.getString("mensagem.limiteTentativas"),
							bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
					String log = timestamp() + " " + bundle.getString("log.limiteTentativasImport");
					cofre.getHistorico().add(log);
					cofre.salvar();
					if (sessaoTimer != null) {
						sessaoTimer.stop();
						sessaoTimer = null;
					}
					
					saidaManual = true;
					dispose();
					SwingUtilities.invokeLater(() -> new TelaInicial(false));
					return; // Encerra o método após o encerramento da sessão
				} else {
					String msg = MessageFormat.format(bundle.getString("mensagem.tentativasRestantesImport"),
							(3 - tentativas));
					JOptionPane.showMessageDialog(this, msg, bundle.getString("titulo.erro"),
							JOptionPane.PLAIN_MESSAGE);
				}
			}
		}

		// Se a senha foi validada, prossegue para a importação
		try {
			byte[] conteudoArquivo = Files.readAllBytes(arquivoSelecionado.toPath());
			// Reaproveita o salt já obtido anteriormente
			byte[] dadosCriptografados = Arrays.copyOfRange(conteudoArquivo, 16, conteudoArquivo.length);
			String dadosDescriptografados = CriptografiaUtils.descriptografar(new String(dadosCriptografados), chave);

			// Extrair apenas os códigos
			Gson gson = new Gson();
			JsonObject jsonObject = JsonParser.parseString(dadosDescriptografados).getAsJsonObject();

			if (jsonObject.has("registros")) {
				Registro[] registrosImportados = gson.fromJson(jsonObject.get("registros"), Registro[].class);

				// Adicionar os registros ao cofre atual dentro de importarCofre(), após validar e pegar registrosImportados

				for (int i = registrosImportados.length - 1; i >= 0; i--) {
					cofre.adicionarRegistro(registrosImportados[i]);
				}

				// Reconstruir a UI de códigos
				carregarRegistros();

				// Registrar no histórico e mensagem final
				String logImportacao = timestamp() + " " + MessageFormat.format(bundle.getString("log.importacaoRegistros"),
						Arrays.stream(registrosImportados).map(r -> r.servico).collect(Collectors.joining(", ")));
				if (!cofre.getHistorico().contains(logImportacao)) {
					cofre.getHistorico().add(logImportacao);
					cofre.salvar();
				}

				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.importarRegistroSucesso"),
						bundle.getString("titulo.sucesso"), JOptionPane.PLAIN_MESSAGE);

			} else {
				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.nenhumCodigoImportado"),
						bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
			}

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, bundle.getString("erro.importarCofre"), bundle.getString("titulo.erro"),
					JOptionPane.PLAIN_MESSAGE);
		}
	}

	private void exportarCofre() {
		// Verificar se o cofre está vazio
		if (cofre.getRegistros().isEmpty()) {
			JOptionPane.showMessageDialog(this, bundle.getString("mensagem.cofreVazioExport"),
					bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
			return;
		}

		// Solicitar a senha atual
		boolean[] visAtual = { false };
		JPasswordField campoAtual = new JPasswordField();
		campoAtual.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
				BorderFactory.createEmptyBorder(5, 5, 5, 28)));
		campoAtual.setBackground(Color.WHITE);

		// Prepara ícones
		ImageIcon iconMostrarRaw = new ImageIcon(getClass().getResource("/esconderTemaClaro.png"));
		ImageIcon iconEsconderRaw = new ImageIcon(getClass().getResource("/verTemaClaro.png"));
		ImageIcon icMostrar = new ImageIcon(iconMostrarRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
		ImageIcon icEsconder = new ImageIcon(iconEsconderRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));

		// Monta painel de senha atual
		int largura = 260;
		JLayeredPane paneAtual = new JLayeredPane();
		paneAtual.setPreferredSize(new Dimension(largura, 30));
		campoAtual.setBounds(0, 0, largura, 30);
		JButton olhoAtual = new JButton(icMostrar);
		olhoAtual.setBounds(largura - 30, 0, 30, 30);
		olhoAtual.setBorderPainted(false);
		olhoAtual.setContentAreaFilled(false);
		olhoAtual.setFocusPainted(false);
		olhoAtual.addActionListener(e -> {
			visAtual[0] = !visAtual[0];
			campoAtual.setEchoChar(visAtual[0] ? (char) 0 : '•');
			olhoAtual.setIcon(visAtual[0] ? icEsconder : icMostrar);
		});
		paneAtual.add(campoAtual, Integer.valueOf(1));
		paneAtual.add(olhoAtual, Integer.valueOf(2));

		JPanel conteudo1 = new JPanel();
		conteudo1.setLayout(new BoxLayout(conteudo1, BoxLayout.Y_AXIS));
		conteudo1.add(new JLabel(bundle.getString("label.digiteSenhaAtual")));
		conteudo1.add(Box.createVerticalStrut(5));
		paneAtual.setAlignmentX(Component.LEFT_ALIGNMENT);
		conteudo1.add(paneAtual);

		int res1 = JOptionPane.showConfirmDialog(this, conteudo1, bundle.getString("titulo.atencao"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (res1 != JOptionPane.OK_OPTION)
			return;
		String senhaAtual = new String(campoAtual.getPassword());

		try {
			SecretKey chaveAtual = CriptografiaUtils.gerarChaveAES(senhaAtual.toCharArray(), cofre.getSalt());
			String dadosCriptografados = cofre.getDadosCriptografados();
			String dadosDescriptografados;

			try {
				dadosDescriptografados = CriptografiaUtils.descriptografar(dadosCriptografados, chaveAtual);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.senhaIncorreta"),
						bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
				return;
			}

			if (dadosDescriptografados == null) {
				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.senhaIncorreta"),
						bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
				return;
			}

			// Escolher tipo de exportação
			int resposta = JOptionPane.showOptionDialog(this, bundle.getString("mensagem.escolherExportacao"),
			        bundle.getString("titulo.atencao"), JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
			        new Object[] { bundle.getString("mensagem.exportarCofre"), bundle.getString("mensagem.exportarRegistros") },
			        bundle.getString("titulo.atencao"));

			if (resposta == JOptionPane.CLOSED_OPTION || resposta == -1) {
				return;
			}

			List<Registro> registrosExportados = new ArrayList<>(cofre.getRegistros());
			boolean exportarTudo = true;
			if (resposta == 1) {
				registrosExportados = escolherRegistrosParaExportacao();
				exportarTudo = false;
				if (registrosExportados == null || registrosExportados.isEmpty()) {
					return;
				}
			}

			// Solicitar nova senha para criptografar o arquivo de exportação
			boolean senhasCoincidem = false;
			boolean[] visNova = { false };
			boolean[] visConfirmar = { false };

			JPasswordField campoNovaSenha = new JPasswordField();
			campoNovaSenha.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
					BorderFactory.createEmptyBorder(5, 5, 5, 28)));
			campoNovaSenha.setBackground(Color.WHITE);
			JPasswordField campoConfirmarSenha = new JPasswordField();
			campoConfirmarSenha.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
					BorderFactory.createEmptyBorder(5, 5, 5, 28)));
			campoConfirmarSenha.setBackground(Color.WHITE);

			JLayeredPane paneNova = new JLayeredPane();
			paneNova.setPreferredSize(new Dimension(260, 30));
			campoNovaSenha.setBounds(0, 0, 260, 30);
			JButton olhoNova = new JButton(icMostrar);
			olhoNova.setBounds(230, 0, 30, 30);
			olhoNova.setBorderPainted(false);
			olhoNova.setContentAreaFilled(false);
			olhoNova.setFocusPainted(false);
			olhoNova.addActionListener(e -> {
				visNova[0] = !visNova[0];
				campoNovaSenha.setEchoChar(visNova[0] ? (char) 0 : '•');
				olhoNova.setIcon(visNova[0] ? icEsconder : icMostrar);
			});
			paneNova.add(campoNovaSenha, Integer.valueOf(1));
			paneNova.add(olhoNova, Integer.valueOf(2));

			JLayeredPane paneConfirmar = new JLayeredPane();
			paneConfirmar.setPreferredSize(new Dimension(260, 30));
			campoConfirmarSenha.setBounds(0, 0, 260, 30);
			JButton olhoConfirmar = new JButton(icMostrar);
			olhoConfirmar.setBounds(230, 0, 30, 30);
			olhoConfirmar.setBorderPainted(false);
			olhoConfirmar.setContentAreaFilled(false);
			olhoConfirmar.setFocusPainted(false);
			olhoConfirmar.addActionListener(e -> {
				visConfirmar[0] = !visConfirmar[0];
				campoConfirmarSenha.setEchoChar(visConfirmar[0] ? (char) 0 : '•');
				olhoConfirmar.setIcon(visConfirmar[0] ? icEsconder : icMostrar);
			});
			paneConfirmar.add(campoConfirmarSenha, Integer.valueOf(1));
			paneConfirmar.add(olhoConfirmar, Integer.valueOf(2));

			while (!senhasCoincidem) {
				JPanel painelSenha = new JPanel();
				painelSenha.setLayout(new BoxLayout(painelSenha, BoxLayout.Y_AXIS));
				painelSenha.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

				painelSenha.add(new JLabel(bundle.getString("label.novaSenhaExport")));
				painelSenha.add(Box.createVerticalStrut(5));
				paneNova.setAlignmentX(Component.LEFT_ALIGNMENT);
				painelSenha.add(paneNova);

				painelSenha.add(Box.createVerticalStrut(15));
				painelSenha.add(new JLabel(bundle.getString("label.confirmarSenhaExport")));
				painelSenha.add(Box.createVerticalStrut(5));
				paneConfirmar.setAlignmentX(Component.LEFT_ALIGNMENT);
				painelSenha.add(paneConfirmar);

				int opcao = JOptionPane.showConfirmDialog(this, painelSenha, bundle.getString("titulo.senhaExportacao"),
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (opcao != JOptionPane.OK_OPTION) {
					return;
				}

				char[] novaSenha = campoNovaSenha.getPassword();
				char[] confirmarSenha = campoConfirmarSenha.getPassword();

				if (novaSenha.length == 0 || confirmarSenha.length == 0) {
					JOptionPane.showMessageDialog(this, bundle.getString("mensagem.senhaExportVazia"),
							bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
					continue;
				}

				if (Arrays.equals(novaSenha, confirmarSenha)) {
					senhasCoincidem = true;
					SecretKey chaveNova = CriptografiaUtils.gerarChaveAES(novaSenha, cofre.getSalt());
					String dadosCriptografadosExportacao = criptografarCofreParaExportacao(registrosExportados,
							chaveNova, exportarTudo);

					// Sugerir nome do arquivo exportado
					File arquivoOriginal = cofre.getArquivo();
					String nomeOriginal = arquivoOriginal.getName();
					String nomeSugerido = (resposta == 0) ? nomeOriginal.replace(".enc", "_backup.enc")
							: nomeOriginal.replace(".enc", "_registros_especificos.enc");

					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setDialogTitle(bundle.getString("label.selecioneLocalExportar"));
					fileChooser.setSelectedFile(new File(nomeSugerido));
					int result = fileChooser.showSaveDialog(this);

					if (result == JFileChooser.APPROVE_OPTION) {
						File arquivoDestino = fileChooser.getSelectedFile();

						// Garante que o arquivo termine com ".enc"
						if (!arquivoDestino.getName().toLowerCase().endsWith(".enc")) {
							arquivoDestino = new File(arquivoDestino.getParentFile(),
									arquivoDestino.getName() + ".enc");
						}

						CriptografiaUtils.salvarArquivoComSalt(arquivoDestino, cofre.getSalt(),
								dadosCriptografadosExportacao);
						JOptionPane.showMessageDialog(this, bundle.getString("mensagem.exportadoSucesso"),
								bundle.getString("titulo.sucesso"), JOptionPane.PLAIN_MESSAGE);

						// Registra no histórico
						String log;
						if (exportarTudo) {
							log = timestamp() + " " + bundle.getString("log.exportacaoCompleta");
						} else {
							List<String> nomesRegistrosExportados = new ArrayList<>();
							for (Registro r : registrosExportados) {
								nomesRegistrosExportados.add(r.servico);
							}
							log = timestamp() + " " + MessageFormat.format(bundle.getString("log.registrosExportados"),
									String.join(", ", nomesRegistrosExportados));
						}

						cofre.getHistorico().add(log);
						cofre.salvar();
					}
				} else {
					JOptionPane.showMessageDialog(this, bundle.getString("mensagem.senhasExportNaoCoincidem"),
							bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
				}
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, bundle.getString("erro.falhaExportacao"),
					bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
			e.printStackTrace();
		}
	}

	private String criptografarCofreParaExportacao(List<Registro> registrosExportados, SecretKey chaveNova,
			boolean exportarTudo) throws Exception {
		// Cria um objeto JSON com os dados a serem exportados (registros e histórico)
		Gson gson = new Gson();
		JsonObject jsonObj = new JsonObject();

		// Adiciona os registros ao JSON
		jsonObj.add("registros", gson.toJsonTree(registrosExportados));
		jsonObj.add("historico", gson.toJsonTree(cofre.getHistorico()));

		if (exportarTudo) {
			jsonObj.addProperty("tempoSessaoMinutos", cofre.getTempoSessaoMinutos());
		}

		// Converte o objeto JSON para string
		String jsonString = gson.toJson(jsonObj);

		// Criptografa a string JSON com a chave fornecida
		return CriptografiaUtils.criptografar(jsonString, chaveNova);
	}

	private List<Registro> escolherRegistrosParaExportacao() {
		List<Registro> registrosSelecionados = new ArrayList<>();
		List<Registro> registrosDisponiveis = cofre.getRegistros();

		// Cria uma lista com os nomes dos serviços dos registros para exibir ao usuário
		List<String> nomesServicos = new ArrayList<>();
		for (Registro r : registrosDisponiveis) {
			nomesServicos.add(r.servico);
		}

		// Cria um painel de seleção com checkboxes
		JPanel painelSelecao = new JPanel(new GridLayout(nomesServicos.size(), 1));
		List<JCheckBox> checkboxes = new ArrayList<>();
		for (String nome : nomesServicos) {
			JCheckBox checkBox = new JCheckBox(nome);
			painelSelecao.add(checkBox);
			checkboxes.add(checkBox);
		}

		int opcao;
		do {
			// Exibe a janela para o usuário selecionar os registros
			opcao = JOptionPane.showConfirmDialog(this, painelSelecao,
					bundle.getString("titulo.selecioneRegistrosExportar"), JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);

			// Se o usuário cancelar, retorna null para interromper o fluxo
			if (opcao != JOptionPane.OK_OPTION) {
				return null;
			}

			// Verifica se pelo menos um registro foi selecionado
			registrosSelecionados.clear(); // Limpa a lista para verificar novamente
			for (int i = 0; i < checkboxes.size(); i++) {
				if (checkboxes.get(i).isSelected()) {
					registrosSelecionados.add(registrosDisponiveis.get(i));
				}
			}

			// Se não houver códigos selecionados, mostra mensagem de erro e repete a seleção
			if (registrosSelecionados.isEmpty()) {
				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.nenhumRegistroSelecionado"),
						bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
			}

		} while (registrosSelecionados.isEmpty()); // Continua até que pelo menos um registro seja selecionado

		return registrosSelecionados;
	}

	private void apagarCofre() {
		// Controla visibilidade do campo
		boolean[] visivel = { false };
		JPasswordField campoSenha = new JPasswordField();

		// Deixa um espaço de 28px à direita para não sobrepor o olhinho
		campoSenha.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
				BorderFactory.createEmptyBorder(5, 5, 5, 28)));
		campoSenha.setBackground(Color.WHITE);

		ImageIcon iconMostrarRaw = new ImageIcon(getClass().getResource("/esconderTemaClaro.png"));
		ImageIcon iconEsconderRaw = new ImageIcon(getClass().getResource("/verTemaClaro.png"));
		ImageIcon iconeMostrar = new ImageIcon(iconMostrarRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
		ImageIcon iconeEsconder = new ImageIcon(
				iconEsconderRaw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));

		JLayeredPane painelSenha = criarPainelSenhaComOlho(campoSenha, visivel, 0, iconeMostrar, iconeEsconder);

		JPanel painel = new JPanel();
		painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
		JLabel label = new JLabel(bundle.getString("label.digiteSenhaApagar"));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		painel.add(label);
		painel.add(Box.createVerticalStrut(8));
		painelSenha.setAlignmentX(Component.LEFT_ALIGNMENT);
		painel.add(painelSenha);

		// Loop de validação de senha
		while (true) {
			int resposta = JOptionPane.showConfirmDialog(this, painel, bundle.getString("titulo.confirmarSenha"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (resposta != JOptionPane.OK_OPTION)
				return;

			char[] senhaDigitada = campoSenha.getPassword();
			if (senhaDigitada.length == 0) {
				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.senhaObrigatoria"),
						bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
				continue;
			}

			try {
				SecretKey chaveTeste = CriptografiaUtils.gerarChaveAES(senhaDigitada, cofre.getSalt());
				CriptografiaUtils.descriptografar(cofre.getDadosCriptografados(), chaveTeste); // valida

				int confirm = JOptionPane.showOptionDialog(this, bundle.getString("mensagem.confirmarApagarCofre"),
						bundle.getString("titulo.confirmarExclusao"), JOptionPane.YES_NO_OPTION,
						JOptionPane.PLAIN_MESSAGE, null,
						new Object[] { bundle.getString("botao.sim"), bundle.getString("botao.cancelar") }, "Cancelar");

				if (confirm == JOptionPane.YES_OPTION) {
					sobrescreverEDeletarArquivo(cofre.getArquivo());
					saidaManual = true;
					dispose();
					SwingUtilities.invokeLater(() -> {
						new TelaInicial();
						JOptionPane.showMessageDialog(null, bundle.getString("mensagem.cofreApagado"),
								bundle.getString("titulo.sucesso"), JOptionPane.PLAIN_MESSAGE);
					});
				}
				return;

			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, bundle.getString("mensagem.senhaIncorretaApagar"),
						bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
			}
		}
	}

	private void sobrescreverEDeletarArquivo(File arquivo) {
		try {
			long tamanho = arquivo.length();
			SecureRandom rand = new SecureRandom();

			try (FileOutputStream fos = new FileOutputStream(arquivo)) {
				byte[] lixo = new byte[4096];
				long escrito = 0;

				while (escrito < tamanho) {
					rand.nextBytes(lixo);
					long restante = tamanho - escrito;
					fos.write(lixo, 0, (int) Math.min(lixo.length, restante));
					escrito += Math.min(lixo.length, restante);
				}

				fos.flush();
			}

			boolean deletado = arquivo.delete();
			if (!deletado) {
				arquivo.deleteOnExit();
			}

			// Deleta arquivos auxiliares se existirem
			File tentativaFile = new File(arquivo.getPath().replace(".enc", ".attempts"));
			File saltFile = new File(arquivo.getPath().replace(".enc", ".salt"));
			if (tentativaFile.exists())
				tentativaFile.delete();
			if (saltFile.exists())
				saltFile.delete();

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Erro ao sobrescrever e apagar o cofre.",
					bundle.getString("titulo.erro"), JOptionPane.PLAIN_MESSAGE);
		}
	}

	private JLayeredPane criarPainelSenhaComOlho(JPasswordField campo, boolean[] visivel, int indice,
			ImageIcon iconeMostrar, ImageIcon iconeEsconder) {
		int larguraCampo = 260;
		JLayeredPane painel = new JLayeredPane();
		painel.setPreferredSize(new Dimension(larguraCampo, 30));

		campo.setBackground(Color.WHITE);
		campo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
				BorderFactory.createEmptyBorder(5, 5, 5, 28)));
		campo.setEchoChar('•');
		campo.setBounds(0, 0, larguraCampo, 30);

		JButton botaoOlho = new JButton(iconeMostrar);
		botaoOlho.setBounds(larguraCampo - 30, 0, 30, 30);
		botaoOlho.setBorderPainted(false);
		botaoOlho.setContentAreaFilled(false);
		botaoOlho.setFocusPainted(false);

		botaoOlho.addActionListener(e -> {
			visivel[indice] = !visivel[indice];
			campo.setEchoChar(visivel[indice] ? (char) 0 : '•');
			botaoOlho.setIcon(visivel[indice] ? iconeEsconder : iconeMostrar);
		});

		painel.add(campo, Integer.valueOf(1));
		painel.add(botaoOlho, Integer.valueOf(2));

		return painel;
	}

}
