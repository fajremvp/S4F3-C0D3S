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
- [ ] **Crypto Engine:** Criptografia completa (metadados e segredos cifrados, nada em texto plano).
- [ ] **Audit Log:** Registro histórico de acessos e modificações (o que foi feito e quando).

### Comandos (CLI)
> **Nota sobre o Banco de Dados:** O programa busca o arquivo `.db` na seguinte ordem de prioridade:
> 1. Argumento explícito: `--db <caminho>`
> 2. Variável de ambiente: `S4F3-C0D3S_DB`
> 3. Arquivo padrão no diretório atual: `./s4f3-c0d3s-vault.db`
- [ ] `init [name/path]`: Inicializa um novo cofre. Se nenhum nome for informado, cria o padrão `s4f3-c0d3s-vault.db` no diretório atual.
- [ ] `--db <path>`: (Opcional) Define manualmente o caminho do banco para qualquer comando, ignorando o padrão.
- [ ] `add`: Adiciona um novo registro (interativo ou via argumentos).
- [ ] `show <service>`: Busca e descriptografa um registro (exibe na tela ou copia para clipboard).
- [ ] `ls`: Lista todos os serviços (sem revelar segredos, também exigindo a senha mestra).
- [ ] `edit <id>`: Edita um registro existente.
- [ ] `rm <id>`: Remove um registro permanentemente.
- [ ] `passwd`: Rotaciona a chave mestra e recriptografa o banco.
- [ ] `export <path>`: Exporta dados decriptados para backup (JSON/CSV).
- [ ] `import <path>`: Importa dados de backups externos.
- [ ] `history`: Exibe o log de auditoria descriptografado (Data/Hora | Ação | Alvo).
- [ ] `shred`: Destruição segura do banco de dados (sobrescrita de bytes).

### Segurança Avançada
- [ ] **Strict Stateless Mode:** O programa não mantém sessões nem roda em background (daemon). A senha mestre é exigida e validada a cada execução de comando, garantindo que as chaves de criptografia existam na memória RAM apenas durante os milissegundos de operação.
- [ ] **Memória Limpa:** Limpeza ativa de arrays de char/byte após o uso.

---

## 🔒 Arquitetura de Segurança

A versão CLI adota uma abordagem de **Banco de Dados Híbrido** com padrões criptográficos rigorosos (OWASP 2024 recommendations):

1. **Estrutura de Dados (Hybrid SQLite):**
    * O banco de dados segue um schema estrito onde metadados de negócio não existem como colunas separadas.

    ```sql
    -- Tabela 'meta': Configurações públicas (Bootstrap)
    CREATE TABLE meta (
        key TEXT PRIMARY KEY,  -- Ex: 'master_salt', 'db_version'
        value TEXT             -- Dados em claro necessários para iniciar a criptografia
    );

    -- Tabela 'records': Onde seus segredos vivem
    CREATE TABLE records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        iv TEXT NOT NULL,      -- IV único (12 bytes) gerado para este registro
        data BLOB NOT NULL,    -- O JSON Criptografado contendo {service, user, recovery_codes, notes}
        created_at INTEGER,    -- Timestamp de criação (Unix Epoch)
        updated_at INTEGER     -- Timestamp de atualização
    );

    -- Tabela 'history': Auditoria criptografada
    CREATE TABLE history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        iv TEXT NOT NULL,      -- IV único para este log
        data BLOB NOT NULL,    -- JSON Criptografado: {action: "VIEW/EDIT", target_alias: "Binance", details: "..."}
        timestamp INTEGER      -- Data da ação (Aberto para permitir ordenação rápida)
    );
    ```

    * **Tabela `meta`:** Armazena apenas o necessário para validar a chave (Salt).
    * **Tabela `records`:** O campo `data` é um container opaco. O banco sabe que existe um registro, mas todo o conteúdo (Nome, Usuário, Notas) está fundido e cifrado dentro desse único campo BLOB.
    * **Tabela `history`:** Garante auditoria sem vazamento de privacidade. A ação realizada e o alvo do log são cifrados no BLOB, mantendo apenas o `timestamp` visível para permitir ordenação cronológica rápida sem expor o conteúdo.

2. **Proteção de Dados (Zero Metadata Leakage):**
    * Diferente de soluções que apenas cifram o segredo, o S4F3-C0D3S cifra também os **metadados** (nome do serviço, usuário, notas).
    * Um atacante com acesso ao arquivo `.db` vê apenas blobs aleatórios, sem saber sequer quais serviços você utiliza (Binance? Email? Banco?).
    * Uso de **AES-256 GCM**: Garante confidencialidade e autenticidade. Qualquer bit alterado no arquivo invalida a descriptografia.
3. **Derivação de Chave Robusta (KDF):**
    * A senha mestre nunca é salva. Ela deriva a chave AES via **PBKDF2WithHmacSHA256**.
    * Configurado com **600.000 iterações** e **Salt aleatório de 16 bytes**.
    * Esse alto custo computacional é a principal barreira contra ataques de força bruta, tornando desnecessários delays artificiais ou contadores de tentativas falhas armazenados em disco.

---

## 🤝 Contribuindo
Pull Requests são bem-vindos. Para mudanças importantes, abra uma issue primeiro para discutir o que você gostaria de mudar.

-----

## 💡 Unlicense (public domain)

“Only scarce resources are subject to property.” — Stephan Kinsella
