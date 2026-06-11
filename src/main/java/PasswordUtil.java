import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Hashes a raw plain-text password using BCrypt.
     * Call this inside RegisterServlet.java right before executing the INSERT statement.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            return plainPassword;
        }
        return encoder.encode(plainPassword);
    }

    /**
     * Verifies an entered password against the stored database value.
     * Automatically switches between BCrypt matching and legacy plain-text checks.
     * Call this inside LoginServlet.java.
     */
    public static boolean verifyPassword(String enteredPassword, String storedPasswordFromDb) {
        if (enteredPassword == null || storedPasswordFromDb == null) {
            return false;
        }

        // If the password string in the database looks like a Bcrypt hash, verify it using the encoder
        if (storedPasswordFromDb.startsWith("$2a$") || storedPasswordFromDb.startsWith("$2b$") || storedPasswordFromDb.startsWith("$2y$")) {
            if (storedPasswordFromDb.length() == 60) {
                return encoder.matches(enteredPassword, storedPasswordFromDb);
            }
        }
        
        // Fallback: If it's an old plain-text password from your friend's database, match it directly
        return enteredPassword.equals(storedPasswordFromDb);
    }
}