package finance.cli;

import finance.domain.TransactionType;
import finance.domain.User;
import finance.service.AuthService;
import finance.service.FinanceService;
import finance.service.FileStorage;
import finance.service.UserStore;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class CommandLoop {

    private static final String USERS_DATA_FILE = "data.dat";
    private static final String DEFAULT_STATS_FILE = "stats.txt";

    private final UserStore store = new UserStore();
    private final AuthService auth = new AuthService(store);
    private final FinanceService finance = new FinanceService();
    private final FileStorage storage = new FileStorage(USERS_DATA_FILE);

    private boolean statsToFile = false;
    private String statsFilePath = DEFAULT_STATS_FILE;

    public void run() {
        store.replaceAll(storage.loadUsersOrEmpty());

        System.out.println("Personal Finance Manager (CLI)");
        System.out.println("Type 'help' to see commands.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(prompt());

                if (!scanner.hasNextLine()) {
                    System.out.println();
                    break;
                }

                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                CommandResult r = handle(line);
                if (r == CommandResult.EXIT) break;
            }
        }

        storage.saveUsers(store.snapshot());
        System.out.println("Bye!");
    }

    private String prompt() {
        return auth.currentUser().map(u -> u.getLogin() + "> ").orElse("> ");
    }

    private CommandResult handle(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "help" -> {
                printHelp();
                return CommandResult.CONTINUE;
            }
            case "exit" -> {
                storage.saveUsers(store.snapshot());
                return CommandResult.EXIT;
            }

            case "register" -> {
                if (parts.length < 3) {
                    System.out.println("Usage: register <login> <password>");
                    return CommandResult.CONTINUE;
                }
                var res = auth.register(parts[1], parts[2]);
                System.out.println(res.message());
                return CommandResult.CONTINUE;
            }

            case "login" -> {
                if (parts.length < 3) {
                    System.out.println("Usage: login <login> <password>");
                    return CommandResult.CONTINUE;
                }
                var res = auth.login(parts[1], parts[2]);
                System.out.println(res.message());
                return CommandResult.CONTINUE;
            }

            case "logout" -> {
                auth.logout();
                System.out.println("Logged out.");
                return CommandResult.CONTINUE;
            }

            case "whoami" -> {
                System.out.println(auth.currentUser()
                        .map(u -> "You are logged in as: " + u.getLogin())
                        .orElse("You are not logged in."));
                return CommandResult.CONTINUE;
            }

            case "statsout" -> {
                handleStatsOut(line);
                return CommandResult.CONTINUE;
            }

            case "income" -> {
                return requireLogin(line, this::handleIncome);
            }
            case "expense" -> {
                return requireLogin(line, this::handleExpense);
            }
            case "budget" -> {
                return requireLogin(line, this::handleBudget);
            }
            case "stats" -> {
                return requireLogin(line, this::handleStats);
            }

            default -> {
                System.out.println("Unknown command: " + cmd + ". Type 'help'.");
                return CommandResult.CONTINUE;
            }
        }
    }

    private void handleStatsOut(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length == 1) {
            System.out.println("Stats output: " + (statsToFile ? ("file " + statsFilePath) : "console"));
            return;
        }

        String mode = parts[1].toLowerCase(Locale.ROOT);

        if ("console".equals(mode)) {
            statsToFile = false;
            System.out.println("Stats output switched to console.");
            return;
        }

        if ("file".equals(mode)) {
            statsToFile = true;
            if (parts.length >= 3) {
                statsFilePath = parts[2];
            } else {
                statsFilePath = DEFAULT_STATS_FILE;
            }
            System.out.println("Stats output switched to file: " + statsFilePath);
            return;
        }

        System.out.println("Usage:");
        System.out.println("  statsout");
        System.out.println("  statsout console");
        System.out.println("  statsout file [path]");
    }

    private CommandResult requireLogin(String line, Handler handler) {
        Optional<User> u = auth.currentUser();
        if (u.isEmpty()) {
            System.out.println("Please login first.");
            return CommandResult.CONTINUE;
        }
        return handler.handle(u.get(), line);
    }

    private CommandResult handleIncome(User user, String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            System.out.println("Usage: income <category> <amount> [comment...]");
            return CommandResult.CONTINUE;
        }

        String category = parts[1];
        BigDecimal amount = parseMoney(parts[2]);
        String comment = joinTail(parts, 3);

        var res = finance.addIncome(user, category, amount, comment);
        System.out.println(res.message);
        return CommandResult.CONTINUE;
    }

    private CommandResult handleExpense(User user, String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            System.out.println("Usage: expense <category> <amount> [comment...]");
            return CommandResult.CONTINUE;
        }

        String category = parts[1];
        BigDecimal amount = parseMoney(parts[2]);
        String comment = joinTail(parts, 3);

        var res = finance.addExpense(user, category, amount, comment);
        System.out.println(res.message);
        return CommandResult.CONTINUE;
    }

    private CommandResult handleBudget(User user, String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            System.out.println("Usage: budget <category> <limit>");
            return CommandResult.CONTINUE;
        }

        String category = parts[1];
        BigDecimal limit = parseMoney(parts[2]);

        var res = finance.setBudget(user, category, limit);
        System.out.println(res.message);
        return CommandResult.CONTINUE;
    }

    private CommandResult handleStats(User user, String line) {
        String[] parts = line.split("\\s+");

        if (parts.length == 1) {
            withStatsPrintStream(ps -> printFullStats(ps, user));
            return CommandResult.CONTINUE;
        }

        if (parts.length == 2) {
            String sub = parts[1].toLowerCase(Locale.ROOT);
            if (sub.equals("income")) {
                withStatsPrintStream(ps -> printCategorySums(ps, user, TransactionType.INCOME));
                return CommandResult.CONTINUE;
            }
            if (sub.equals("expense")) {
                withStatsPrintStream(ps -> printCategorySums(ps, user, TransactionType.EXPENSE));
                return CommandResult.CONTINUE;
            }
        }

        if (parts.length >= 4 && parts[1].equalsIgnoreCase("categories")) {
            TransactionType type = parseType(parts[2]);
            if (type == null) {
                System.out.println("Usage: stats categories <income|expense> <cat1,cat2,...>");
                return CommandResult.CONTINUE;
            }

            List<String> cats = Arrays.stream(parts[3].split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (cats.isEmpty()) {
                System.out.println("No categories provided.");
                return CommandResult.CONTINUE;
            }

            var res = finance.sumByCategories(user, type, cats);

            withStatsPrintStream(ps -> {
                ps.println("Sum (" + type.name().toLowerCase(Locale.ROOT) + ") for " + cats + " = " + res.sum);
                if (!res.notFound.isEmpty()) {
                    ps.println("WARNING: categories not found: " + res.notFound);
                }
            });

            return CommandResult.CONTINUE;
        }

        System.out.println("Usage:");
        System.out.println("  stats");
        System.out.println("  stats income");
        System.out.println("  stats expense");
        System.out.println("  stats categories <income|expense> <cat1,cat2,...>");
        return CommandResult.CONTINUE;
    }


    private void withStatsPrintStream(StatsPrinter printer) {
        if (!statsToFile) {
            PrintStream ps = System.out;
            ps.println("================================");
            ps.println("Stats at " + LocalDateTime.now());
            ps.println("--------------------------------");
            printer.print(ps);
            ps.println();
            return;
        }

        try (PrintStream ps = new PrintStream(new FileOutputStream(statsFilePath, true), true)) {
            ps.println("================================");
            ps.println("Stats at " + LocalDateTime.now());
            ps.println("--------------------------------");
            printer.print(ps);
            ps.println();
        } catch (Exception e) {
            System.out.println("ERROR: cannot write stats to file: " + statsFilePath);
            System.out.println("Reason: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("Stats will be printed to console instead.");

            PrintStream ps = System.out;
            ps.println("================================");
            ps.println("Stats at " + LocalDateTime.now());
            ps.println("--------------------------------");
            printer.print(ps);
            ps.println();
        }
    }

    private void printFullStats(PrintStream ps, User user) {
        var r = finance.buildStats(user);

        ps.println("Total income: " + r.totalIncome);
        ps.println("Income by categories:");
        printMapSorted(ps, r.incomeByCategory);

        ps.println("Total expense: " + r.totalExpense);
        ps.println("Expense by categories:");
        printMapSorted(ps, r.expenseByCategory);

        ps.println("Budgets by categories:");
        if (r.budgets.isEmpty()) {
            ps.println("  (no budgets set)");
        } else {
            var keys = new ArrayList<>(r.budgets.keySet());
            keys.sort(String::compareTo);
            for (String cat : keys) {
                var b = r.budgets.get(cat);
                ps.println("  " + cat + ": limit=" + b.limit + ", remaining=" + b.remaining);
            }
        }
    }

    private void printCategorySums(PrintStream ps, User user, TransactionType type) {
        var r = finance.buildStats(user);
        Map<String, BigDecimal> map = (type == TransactionType.INCOME) ? r.incomeByCategory : r.expenseByCategory;
        ps.println(type == TransactionType.INCOME ? "Income by categories:" : "Expense by categories:");
        printMapSorted(ps, map);
    }

    private void printMapSorted(PrintStream ps, Map<String, BigDecimal> map) {
        if (map.isEmpty()) {
            ps.println("  (empty)");
            return;
        }
        var keys = new ArrayList<>(map.keySet());
        keys.sort(String::compareTo);
        for (String k : keys) {
            ps.println("  " + k + ": " + map.get(k));
        }
    }


    private BigDecimal parseMoney(String s) {
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private TransactionType parseType(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "income" -> TransactionType.INCOME;
            case "expense" -> TransactionType.EXPENSE;
            default -> null;
        };
    }

    private String joinTail(String[] parts, int fromIdx) {
        if (parts.length <= fromIdx) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = fromIdx; i < parts.length; i++) {
            if (i > fromIdx) sb.append(' ');
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private void printHelp() {
        System.out.println("Auth:");
        System.out.println("  register <login> <password>");
        System.out.println("  login <login> <password>");
        System.out.println("  logout");
        System.out.println("  whoami");
        System.out.println();
        System.out.println("Finance (login required):");
        System.out.println("  income <category> <amount> [comment...]");
        System.out.println("  expense <category> <amount> [comment...]");
        System.out.println("  budget <category> <limit>");
        System.out.println("  stats");
        System.out.println("  stats income");
        System.out.println("  stats expense");
        System.out.println("  stats categories <income|expense> <cat1,cat2,...>");
        System.out.println();
        System.out.println("Stats output (ONLY affects stats):");
        System.out.println("  statsout                Show current stats output");
        System.out.println("  statsout console        Print stats to console");
        System.out.println("  statsout file [path]    Append stats to file (default: " + DEFAULT_STATS_FILE + ")");
        System.out.println();
        System.out.println("Other:");
        System.out.println("  help");
        System.out.println("  exit");
    }

    @FunctionalInterface
    private interface Handler {
        CommandResult handle(User user, String line);
    }

    @FunctionalInterface
    private interface StatsPrinter {
        void print(PrintStream ps);
    }
}
