import java.sql.*;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Create fictional accounts in an empty local demonstration database only. */
public class InitDemoAccounts {
    static String env(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
    }
    static void update(Connection db, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = db.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }
    public static void main(String[] args) throws Exception {
        String password = env("DEMO_PASSWORD", "");
        if (password.length() < 8 || password.getBytes("UTF-8").length > 72)
            throw new IllegalArgumentException("Set DEMO_PASSWORD to your own 8+ character password (at most 72 UTF-8 bytes).");
        String url = env("DB_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/web?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        try (Connection db = DriverManager.getConnection(url, env("DB_USER", "nuanxun"), env("DB_PASSWORD", ""))) {
            db.setAutoCommit(false);
            try {
                try (Statement statement = db.createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users")) {
                    result.next();
                    if (result.getInt(1) != 0)
                        throw new IllegalStateException("The demo initializer requires an empty users table; existing accounts are never modified.");
                }
                String hash = new BCryptPasswordEncoder().encode(password);
                String[][] accounts = {
                    {"admin", "灯塔管理员", "灯塔管理员", "admin-demo@example.invalid"},
                    {"family", "家属清禾", "清禾", "family-demo@example.invalid"},
                    {"rescuer", "队员远航", "远航", "rescuer-demo@example.invalid"}
                };
                for (String[] account : accounts) {
                    String uuid = UUID.randomUUID().toString();
                    update(db, "INSERT INTO users(uuid,username,password,phone,gender,user_type,status) VALUES(?,?,?,?,? ,?,'active')",
                        uuid, account[1], hash, account[3], "other", account[0]);
                    update(db, "INSERT INTO account_profiles(uuid,name,location) VALUES(?,?,?)", uuid, account[2], "演示地区");
                    if (account[0].equals("family"))
                        update(db, "INSERT INTO families(uuid,name,task_ids) VALUES(?,?,JSON_ARRAY())", uuid, account[2]);
                    if (account[0].equals("rescuer"))
                        update(db, "INSERT INTO rescuers(uuid,name,status,location,skills_description,skill_tags,task_ids) VALUES(?,?,'available',?,?,JSON_ARRAY('急救技能','沟通能力'),JSON_ARRAY())",
                            uuid, account[2], "演示地区", "演示用搜救队员，具备急救与沟通技能");
                }
                db.commit();
                System.out.println("Created fictional local accounts: 灯塔管理员 / 家属清禾 / 队员远航");
                System.out.println("All use the DEMO_PASSWORD you supplied. No email or external API was called.");
            } catch (Exception error) {
                db.rollback();
                throw error;
            }
        }
    }
}
