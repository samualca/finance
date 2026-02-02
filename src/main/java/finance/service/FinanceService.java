package finance.service;

import finance.domain.Transaction;
import finance.domain.TransactionType;
import finance.domain.User;
import finance.domain.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class FinanceService {

    public Result addIncome(User user, String category, BigDecimal amount, String comment) {
        var v = validate(category, amount);
        if (!v.success) return v;

        Wallet w = user.getWallet();
        w.add(new Transaction(TransactionType.INCOME, category, amount, LocalDateTime.now(), comment));
        return Result.ok("Income added.");
    }

    public Result addExpense(User user, String category, BigDecimal amount, String comment) {
        var v = validate(category, amount);
        if (!v.success) return v;

        Wallet w = user.getWallet();
        w.add(new Transaction(TransactionType.EXPENSE, category, amount, LocalDateTime.now(), comment));

        StringBuilder warn = new StringBuilder();

        BigDecimal limit = w.getBudgets().get(category);
        if (limit != null) {
            BigDecimal spent = w.sumForCategory(TransactionType.EXPENSE, category);
            BigDecimal remaining = limit.subtract(spent);
            if (remaining.signum() < 0) {
                warn.append("WARNING: budget exceeded for category '")
                        .append(category)
                        .append("'. Remaining: ")
                        .append(remaining)
                        .append("\n");
            }
        }

        if (w.totalExpense().compareTo(w.totalIncome()) > 0) {
            warn.append("WARNING: total expenses exceeded total income.\n");
        }

        if (!warn.isEmpty()) {
            return Result.warn("Expense added.\n" + warn);
        }
        return Result.ok("Expense added.");
    }

    public Result setBudget(User user, String category, BigDecimal limit) {
        if (isBlank(category)) return Result.error("Category must be non-empty.");
        if (limit == null || limit.signum() < 0) return Result.error("Budget limit must be >= 0.");

        user.getWallet().setBudget(category, limit);
        return Result.ok("Budget set.");
    }

    public StatsReport buildStats(User user) {
        Wallet w = user.getWallet();

        BigDecimal totalIncome = w.totalIncome();
        BigDecimal totalExpense = w.totalExpense();

        Map<String, BigDecimal> incomeByCat = w.sumsByCategory(TransactionType.INCOME);
        Map<String, BigDecimal> expenseByCat = w.sumsByCategory(TransactionType.EXPENSE);

        Map<String, BudgetLine> budgetLines = new HashMap<>();
        for (Map.Entry<String, BigDecimal> e : w.getBudgets().entrySet()) {
            String cat = e.getKey();
            BigDecimal limit = e.getValue();
            BigDecimal spent = expenseByCat.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal remaining = limit.subtract(spent);
            budgetLines.put(cat, new BudgetLine(limit, remaining));
        }

        return new StatsReport(totalIncome, totalExpense, incomeByCat, expenseByCat, budgetLines);
    }

    public MultiCategoryResult sumByCategories(User user, TransactionType type, List<String> categories) {
        Wallet w = user.getWallet();
        Map<String, BigDecimal> byCat = w.sumsByCategory(type);

        List<String> notFound = new ArrayList<>();
        for (String c : categories) {
            if (!byCat.containsKey(c)) notFound.add(c);
        }

        BigDecimal sum = w.sumForCategories(type, categories);
        return new MultiCategoryResult(sum, notFound);
    }

    private Result validate(String category, BigDecimal amount) {
        if (isBlank(category)) return Result.error("Category must be non-empty.");
        if (amount == null) return Result.error("Amount must be a number.");
        if (amount.signum() <= 0) return Result.error("Amount must be > 0.");
        return Result.ok("OK");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static class StatsReport {
        public final BigDecimal totalIncome;
        public final BigDecimal totalExpense;
        public final Map<String, BigDecimal> incomeByCategory;
        public final Map<String, BigDecimal> expenseByCategory;
        public final Map<String, BudgetLine> budgets;

        public StatsReport(BigDecimal totalIncome,
                           BigDecimal totalExpense,
                           Map<String, BigDecimal> incomeByCategory,
                           Map<String, BigDecimal> expenseByCategory,
                           Map<String, BudgetLine> budgets) {
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.incomeByCategory = incomeByCategory;
            this.expenseByCategory = expenseByCategory;
            this.budgets = budgets;
        }
    }

    public static class BudgetLine {
        public final BigDecimal limit;
        public final BigDecimal remaining;

        public BudgetLine(BigDecimal limit, BigDecimal remaining) {
            this.limit = limit;
            this.remaining = remaining;
        }
    }

    public static class MultiCategoryResult {
        public final BigDecimal sum;
        public final List<String> notFound;

        public MultiCategoryResult(BigDecimal sum, List<String> notFound) {
            this.sum = sum;
            this.notFound = notFound;
        }
    }

    public static class Result {
        public final boolean success;
        public final boolean warning;
        public final String message;

        private Result(boolean success, boolean warning, String message) {
            this.success = success;
            this.warning = warning;
            this.message = message;
        }

        public static Result ok(String msg) { return new Result(true, false, msg); }
        public static Result warn(String msg) { return new Result(true, true, msg); }
        public static Result error(String msg) { return new Result(false, false, msg); }
    }
}
