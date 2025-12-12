# S4F3-C0D3S (CLI Edition)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-Integrated-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Cross--Platform-lightgrey?style=for-the-badge)

> **"Not your keys, not your coins. Not your storage, not your data."**

**S4F3-C0D3S** é um gerenciador de códigos de recuperação (2FA) e segredos **CLI (Command Line Interface)**. Projetado para ser minimalista, auditável, offline e soberano. Focado em usuários que preferem a eficiência do terminal e a transparência do código aberto.

---

## 💡 Filosofia e Motivação

- O **S4F3-C0D3S** surgiu de uma necessidade real e pessoal de guardar os códigos de recuperação (2FA) com segurança. Muitas vezes, acabamos salvando essas informações sensíveis em **blocos de notas**, **capturas de tela**, **fotos** ou em **arquivos desprotegidos**, o que coloca em risco nossa segurança digital.
- Embora gerenciadores de senhas como o **Bitwarden** ou o **KeePass** sejam muito populares e eficazes para armazenar credenciais, o ditado **"não guarde todos os seus ovos na mesma cesta"** nos lembra que é importante separar diferentes tipos de dados sensíveis, como os códigos de recuperação 2FA. Com o **S4F3-C0D3S**, você pode separar essa informação em um cofre criptografado dedicado, evitando riscos de comprometer múltiplas camadas de segurança de uma única vez.

---

## 🕐 Quando você vai usar o programa?

- Na primeira vez: para reunir e armazenar todos os seus códigos de recuperação 2FA já existentes.
- Sempre que você ativar a 2FA em um novo sistema: assim que ativar, crie um novo registro e salve imediatamente os códigos recebidos.
- Em casos de emergência (tomara que isso não aconteça): se você perder o acesso ao seu autenticador 2FA, basta abrir o programa e utilizar seu código de recuperação salvo para restaurar seu acesso.


---

## 🛠️ Stack Tecnológica

Reescrito do zero para performance e manutenibilidade:

* **Linguagem:** Java 21 (LTS) - Utilizando recursos modernos (Records, Text Blocks, Virtual Threads).
* **Banco de Dados:** SQLite (Modo Embedded) via JDBC.
* **Interface:** CLI via [Picocli](https://picocli.info/).
* **Criptografia:** AES-256 GCM (Authenticated Encryption) + PBKDF2 (Key Derivation).
* **Build:** Maven (Gera um único binário "Fat Jar" portátil).

---

## 🧩 Funcionalidades (Roadmap)

O projeto está em desenvolvimento ativo. O objetivo é implementar:

### Core (O Cofre)
- [x] **Database Init:** Criação automática do arquivo `.db` local.
- [ ] **Master Password:** Derivação de chave segura na inicialização.
- [ ] **Crypto Engine:** Criptografia de colunas (metadados visíveis, segredos em blob cifrado).
- [ ] **Audit Log:** Registro histórico de acessos e modificações (quem acessou o quê e quando).

### Comandos (CLI)
- [ ] `init`: Inicializa um novo cofre seguro.
- [ ] `add`: Adiciona um novo registro (interativo ou via argumentos).
- [ ] `show <service>`: Busca e descriptografa um registro (exibe na tela ou copia para clipboard).
- [ ] `ls`: Lista todos os serviços (sem revelar segredos).
- [ ] `edit <id>`: Edita um registro existente.
- [ ] `rm <id>`: Remove um registro permanentemente.
- [ ] `passwd`: Rotaciona a chave mestra e recriptografa o banco.
- [ ] `export`: Exporta dados decriptados para backup (JSON/CSV).
- [ ] `import`: Importa dados de backups externos.
- [ ] `history`: Histórico de ações realizadas no cofre atual.
- [ ] `shred`: Destruição segura do banco de dados (sobrescrita de bytes).

### Segurança Avançada
- [ ] **Stateless Security:** O cofre permanece trancado por padrão. A senha mestre é exigida a cada operação (stateless), garantindo que nada fique residente na RAM.
- [ ] **Memória Limpa:** Limpeza ativa de arrays de char/byte após o uso.

---

## 🔒 Arquitetura de Segurança

A versão CLI adota uma abordagem de **Banco de Dados Híbrido** com padrões criptográficos rigorosos (OWASP 2024 recommendations):

1. **Estrutura:** O arquivo `.db` é um SQLite padrão, garantindo portabilidade.
2. **Proteção de Dados (Confidencialidade & Integridade):**
    * Campos sensíveis são cifrados com **AES-256** no modo **GCM (Galois/Counter Mode)**.
    * O modo GCM fornece autenticação nativa: qualquer bit alterado no arquivo criptografado (corrupção ou ataque) causará falha na descriptografia, impedindo a injeção de dados falsos.
3. **Derivação de Chave (KDF):**
    * A senha mestre nunca é salva. Ela deriva a chave AES via **PBKDF2WithHmacSHA256**.
    * Configurado com **600.000 iterações** e **Salt aleatório de 16 bytes**. Isso torna ataques de força bruta computacionalmente inviáveis em hardware comum.
    * Cada registro possui seu próprio IV (12 bytes) único.

---

## 🤝 Contribuindo
Pull Requests são bem-vindos. Para mudanças importantes, abra uma issue primeiro para discutir o que você gostaria de mudar.

-----

## 💡 Unlicense (public domain)

“Only scarce resources are subject to property.” — Stephan Kinsella
