# Reservas corporativas

Projeto de estudo com Java 17, Jakarta EE 10, JSF 4, JPA 3.1, MySQL e Open Liberty. Usuários, salas, equipamentos e reservas ficam salvos no banco.

## Configurar o MySQL instalado

1. No MySQL Workbench, execute `database/criar-banco.sql` ou crie um banco vazio chamado `reservas_corporativas`.
2. Escolha uma conta do MySQL com acesso a esse banco. Para criar/estender as tabelas no ambiente de estudo, a conta precisa de SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX e REFERENCES sobre esse banco.
3. Copie `.env.example` para `.env` e informe host, porta, banco, usuário e senha. O `.env` é ignorado pelo Git. Não coloque a senha no `server.xml` nem no README.

```dotenv
DB_HOST=localhost
DB_PORT=3306
DB_NAME=reservas_corporativas
DB_USER=reservas
DB_PASSWORD=ALTERE_AQUI
```

Os valores acima são exemplos. A conta MySQL deve existir e ter a senha informada; o projeto não altera contas ou senhas do seu servidor MySQL.

## Abrir e rodar no VS Code

1. Abra esta pasta pelo menu Arquivo > Abrir Pasta.
2. Instale a extensão sugerida **Extension Pack for Java**.
3. Confira `java -version` e `mvn -version`. Use JDK 17 ou 21 e Maven 3.9.
4. Depois de configurar o MySQL e o `.env`, execute no terminal PowerShell:

```powershell
powershell -NoProfile -File .\scripts\iniciar.ps1
```

O script carrega o `.env` no ambiente do processo e executa `mvn liberty:dev`. O Maven instala o servidor, as features e o driver JDBC na primeira execução. Também é possível usar Terminal > Executar Tarefa > Executar reservas.

Abra http://localhost:9080/reservas/login.xhtml. Pressione Ctrl+C no terminal para parar.

Se preferir executar `mvn liberty:dev` diretamente, configure antes as cinco variáveis DB_* no ambiente do terminal; o Maven não lê `.env` por conta própria.

## Contas de estudo

Na primeira inicialização com banco acessível, o projeto cria somente as contas que ainda não existem:

| Usuário | Senha inicial | Perfil |
| --- | --- | --- |
| admin | Admin@123 | Administrador |
| funcionario | Funcionario@123 | Funcionário |
| funcionario2 | Funcionario@123 | Funcionário |

Essas são contas demonstrativas de execução local. As senhas são armazenadas com PBKDF2-HMAC-SHA256, salt individual e 210.000 iterações. A inicialização não redefine a senha de contas existentes.

## Permissões

| Operação | Funcionário | Administrador |
| --- | --- | --- |
| Consultar recursos e disponibilidade | Sim | Sim |
| Solicitar reserva em seu nome | Sim | Sim |
| Ver reservas | Apenas próprias | Todas |
| Cancelar reserva | Apenas próprias | Todas |
| Aprovar reserva | Não | Sim |
| Cadastrar salas e equipamentos | Não | Sim |

O servidor valida as permissões nas regras de negócio e não confia no nome do solicitante enviado pelo cliente. O formulário usa a identidade autenticada. Há logout, renovação do identificador da sessão após login, expiração por 30 minutos de inatividade, cookie HttpOnly e proteção CSRF nas operações de alteração.

## Persistência e modelos

- `Usuario`: tabela `usuarios`, login único, nome, perfil e hash da senha.
- `Recurso`: tabela `recursos`, com nome e tipo SALA ou EQUIPAMENTO.
- `Reserva`: tabela `reservas`, com relacionamentos ManyToOne para usuário e recurso, início/fim em DATETIME e situação.

A unidade JPA `reservasPU` usa o datasource JNDI `jdbc/ReservasDS`. O EclipseLink, provedor JPA incluído no Open Liberty, cria ou estende as tabelas no banco informado para este projeto de estudo. Não é utilizado drop-and-create no MySQL. Mudanças complexas de esquema deverão usar migrações em uma próxima etapa.

As operações usam transações JTA. A criação de reservas bloqueia a linha do recurso com PESSIMISTIC_WRITE antes de consultar conflitos e inserir, com isolamento READ_COMMITTED. Reservas pendentes e aprovadas bloqueiam o horário; canceladas liberam o recurso. Horários consecutivos são permitidos. Datas representam o horário local da empresa.

## Telas JSF

- `/login.xhtml`: autenticação.
- `/index.xhtml`: visão geral, com indicadores filtrados pelo usuário.
- `/recursos.xhtml`: cadastro e listagem, exclusiva do administrador.
- `/nova-reserva.xhtml`: disponibilidade e solicitação.
- `/reservas.xhtml`: reservas próprias ou gestão geral, conforme perfil.

## API REST

Prefixo: http://localhost:9080/reservas/api

A API usa a mesma sessão autenticada pelo login JSF. Clientes devem enviar o cookie da sessão. Consulte GET `/sessao` para obter o token CSRF da sessão; envie-o no cabeçalho `X-CSRF-Token` de POST e PATCH.

- GET `/sessao`: identidade da sessão e token CSRF.
- GET `/recursos`: listar salas e equipamentos.
- POST `/recursos`: cadastrar com JSON `{"nome":"Sala 2","tipo":"SALA"}`; exige admin.
- GET `/reservas`: listar conforme o perfil.
- POST `/reservas`: solicitar com JSON `{"recursoId":1,"inicio":"2026-10-20T09:00","fim":"2026-10-20T10:00"}`.
- GET `/recursos/1/disponibilidade?inicio=2026-10-20T09:00&fim=2026-10-20T10:00`.
- PATCH `/reservas/1/aprovar`: exige admin.
- PATCH `/reservas/1/cancelar`: exige dono da reserva ou admin.

Envie JSON com Content-Type: application/json. Erros: 400 para dados inválidos, 401 para ausência de autenticação, 403 para falta de permissão ou token CSRF inválido, 404 para identificador inexistente e 409 para conflito.

## Organização

Código Java em `src/main/java/br/com/estudo/reservas`:

- `config`: configuração REST e inicialização dos dados de estudo.
- `model`: entidades JPA e perfis.
- `dto`: dados recebidos para criar reserva, sem identidade do solicitante.
- `repository`: consultas e persistência com EntityManager.
- `service`: regras de negócio e transações.
- `exception`: erros esperados de negócio.
- `api`: endpoints REST e respostas HTTP.
- `security`: autenticação, sessão, hash de senhas e filtro de acesso/CSRF.
- `web`: controladores JSF.

Outras pastas:

- `src/main/resources/META-INF/persistence.xml`: unidade JPA de produção/MySQL.
- `src/main/liberty/config/server.xml`: servidor e datasource.
- `src/main/webapp`: telas JSF, template e CSS.
- `database`: script de criação do banco.
- `scripts`: inicialização local com `.env`.
- `src/test`: testes, com unidade JPA separada e H2 em memória; não acessam seu MySQL.

## Formatação e testes

```powershell
mvn spotless:apply
mvn spotless:check package
```

Os testes validam autenticação, permissões, CSRF, conflitos, cancelamentos e persistência JPA entre diferentes sessões do banco. O H2 é usado somente nos testes; a conexão com o MySQL real deve ser validada com suas credenciais.

O WAR é gerado em `target/reservas.war`. Há configuração `.editorconfig` e formatação Java pelo Spotless/Google Java Format, no estilo AOSP.

Referências: [JPA no Open Liberty](https://openliberty.io/docs/latest/reference/feature/persistence-3.1.html), [datasource e driver JDBC](https://openliberty.io/docs/latest/relational-database-connections-JDBC.html) e [configuração do EclipseLink](https://eclipse.dev/eclipselink/documentation/4.0/jpa/extensions/jpa-extensions.html).

## Validação da implementação

- Build Maven concluído e 16 testes automatizados aprovados.
- Testes de JPA executados com EclipseLink e H2, sem acessar o MySQL instalado.
- Conexão ao MySQL real depende da configuração local do `.env`.

## Solução de problemas

- **Access denied / conexão recusada:** confira host, porta, conta, senha, serviço MySQL e permissões sobre o banco definido no `.env`.
- **Unknown database:** execute `database/criar-banco.sql` antes de iniciar.
- **Erro 403 ao chamar a API:** confira o perfil autenticado e o cabeçalho `X-CSRF-Token` obtido em GET `/api/sessao`.
- **Porta 9080 ocupada:** encerre outra instância do projeto antes de iniciar uma nova.
- **Espaço insuficiente no disco:** libere espaço para o download do servidor, driver e dependências Maven.
