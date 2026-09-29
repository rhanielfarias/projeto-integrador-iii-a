import java.sql.*;
import java.time.LocalDate;
import java.util.Scanner;

/** Protótipo didático de cadastro e fila de atendimentos persistida no MySQL. */
public class App {
    private static final String URL = System.getenv().getOrDefault("DB_URL",
        "jdbc:mysql://localhost:3306/instituto_atendimentos?serverTimezone=America/Sao_Paulo");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    private static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Scanner entrada = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n1 Cadastrar pessoa | 2 Enfileirar | 3 Próximo | 4 Chamar | 5 Concluir | 6 Histórico | 0 Sair");
                String opcao = entrada.nextLine().trim();
                try {
                    switch (opcao) {
                        case "1" -> cadastrar(entrada);
                        case "2" -> enfileirar(entrada);
                        case "3" -> listarProximo();
                        case "4" -> chamar();
                        case "5" -> concluir(entrada);
                        case "6" -> historico(entrada);
                        case "0" -> { return; }
                        default -> System.out.println("Opção inválida.");
                    }
                } catch (SQLException | IllegalArgumentException e) {
                    System.err.println("Operação não concluída: " + e.getMessage());
                }
            }
        }
    }

    private static void cadastrar(Scanner entrada) throws SQLException {
        System.out.print("Nome fictício: "); String nome = entrada.nextLine().trim();
        if (nome.isBlank()) throw new IllegalArgumentException("Nome obrigatório.");
        System.out.print("Data de nascimento AAAA-MM-DD (opcional): ");
        String data = entrada.nextLine().trim();
        try (Connection c = conectar(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO pessoa_atendida(nome,data_nascimento) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, nome);
            if (data.isBlank()) s.setNull(2, Types.DATE);
            else s.setDate(2, Date.valueOf(LocalDate.parse(data)));
            s.executeUpdate();
            try (ResultSet rs = s.getGeneratedKeys()) { if (rs.next()) System.out.println("Pessoa id=" + rs.getLong(1)); }
        }
    }

    private static void enfileirar(Scanner entrada) throws SQLException {
        System.out.print("ID da pessoa: "); long pessoaId = Long.parseLong(entrada.nextLine());
        System.out.print("Tipo de atendimento: "); String tipo = entrada.nextLine().trim();
        if (tipo.isBlank()) throw new IllegalArgumentException("Tipo obrigatório.");
        try (Connection c = conectar(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO atendimento(pessoa_id,tipo) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, pessoaId); s.setString(2, tipo); s.executeUpdate();
            try (ResultSet rs = s.getGeneratedKeys()) { if (rs.next()) System.out.println("Atendimento id=" + rs.getLong(1)); }
        }
    }

    private static void listarProximo() throws SQLException {
        try (Connection c = conectar(); PreparedStatement s = c.prepareStatement(
                "SELECT a.id,p.nome,a.tipo,a.chegada FROM atendimento a JOIN pessoa_atendida p ON p.id=a.pessoa_id " +
                "WHERE a.estado='AGUARDANDO' ORDER BY a.chegada,a.id LIMIT 1"); ResultSet rs = s.executeQuery()) {
            if (rs.next()) imprimir(rs); else System.out.println("Fila vazia.");
        }
    }

    /** Bloqueia o primeiro registro dentro da transação para impedir dupla chamada concorrente. */
    private static void chamar() throws SQLException {
        try (Connection c = conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement busca = c.prepareStatement(
                    "SELECT a.id,p.nome,a.tipo,a.chegada FROM atendimento a JOIN pessoa_atendida p ON p.id=a.pessoa_id " +
                    "WHERE a.estado='AGUARDANDO' ORDER BY a.chegada,a.id LIMIT 1 FOR UPDATE")) {
                long id;
                try (ResultSet rs = busca.executeQuery()) {
                    if (!rs.next()) { c.commit(); System.out.println("Fila vazia."); return; }
                    imprimir(rs); id = rs.getLong("id");
                }
                try (PreparedStatement atualiza = c.prepareStatement(
                        "UPDATE atendimento SET estado='EM_ATENDIMENTO',inicio=CURRENT_TIMESTAMP(6) WHERE id=? AND estado='AGUARDANDO'")) {
                    atualiza.setLong(1, id);
                    if (atualiza.executeUpdate() != 1) throw new SQLException("Atendimento indisponível.");
                }
                c.commit(); System.out.println("Chamado id=" + id);
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    private static void concluir(Scanner entrada) throws SQLException {
        System.out.print("ID do atendimento em andamento: "); long id = Long.parseLong(entrada.nextLine());
        try (Connection c = conectar(); PreparedStatement s = c.prepareStatement(
                "UPDATE atendimento SET estado='CONCLUIDO',fim=CURRENT_TIMESTAMP(6) " +
                "WHERE id=? AND estado='EM_ATENDIMENTO'")) {
            s.setLong(1, id);
            System.out.println(s.executeUpdate() == 1 ? "Concluído." : "ID não encontrado ou estado incompatível.");
        }
    }

    private static void historico(Scanner entrada) throws SQLException {
        System.out.print("ID da pessoa: "); long id = Long.parseLong(entrada.nextLine());
        try (Connection c = conectar(); PreparedStatement s = c.prepareStatement(
                "SELECT id,tipo,chegada,inicio,fim,estado FROM atendimento WHERE pessoa_id=? ORDER BY chegada DESC,id DESC")) {
            s.setLong(1, id);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) System.out.printf("%d | %s | %s | %s | %s | %s%n", rs.getLong("id"),
                    rs.getString("tipo"), rs.getTimestamp("chegada"), rs.getTimestamp("inicio"),
                    rs.getTimestamp("fim"), rs.getString("estado"));
            }
        }
    }

    private static void imprimir(ResultSet rs) throws SQLException {
        System.out.printf("Próximo: %d | %s | %s | %s%n", rs.getLong("id"), rs.getString("nome"),
            rs.getString("tipo"), rs.getTimestamp("chegada"));
    }
}
