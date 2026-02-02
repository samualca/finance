package finance.service;

import finance.domain.User;
import finance.domain.Wallet;

import java.util.Optional;

public class AuthService {
    private final UserStore store;
    private User currentUser;

    public AuthService(UserStore store) {
        this.store = store;
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public Result register(String login, String password) {
        if (isBlank(login) || isBlank(password)) {
            return Result.error("Login and password must be non-empty.");
        }
        if (store.exists(login)) {
            return Result.error("User already exists: " + login);
        }

        store.put(new User(login, password, new Wallet()));
        return Result.ok("User registered: " + login);
    }

    public Result login(String login, String password) {
        if (isBlank(login) || isBlank(password)) {
            return Result.error("Login and password must be non-empty.");
        }

        User user = store.get(login);
        if (user == null) {
            return Result.error("User not found: " + login);
        }
        if (!password.equals(user.getPassword())) {
            return Result.error("Invalid password.");
        }

        currentUser = user;
        return Result.ok("Logged in as: " + login);
    }

    public void logout() {
        currentUser = null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public record Result(boolean success, String message) {
        public static Result ok(String msg) { return new Result(true, msg); }
        public static Result error(String msg) { return new Result(false, msg); }
    }
}
