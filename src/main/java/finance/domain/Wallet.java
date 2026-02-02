package finance.domain;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class Wallet implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<String, BigDecimal> budgets = new HashMap<>();

    public void add(Transaction tx) {
        transactions.add(tx);
    }

    public void setBudget(String category, BigDecimal limit) {
        budgets.put(category, limit);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public Map<String, BigDecimal> getBudgets() {
        return Collections.unmodifiableMap(budgets);
    }

    public BigDecimal totalIncome() {
        return totalByType(TransactionType.INCOME);
    }

    public BigDecimal totalExpense() {
        return totalByType(TransactionType.EXPENSE);
    }

    public BigDecimal totalByType(TransactionType type) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction tx : transactions) {
            if (tx.getType() == type) sum = sum.add(tx.getAmount());
        }
        return sum;
    }

    public Map<String, BigDecimal> sumsByCategory(TransactionType type) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Transaction tx : transactions) {
            if (tx.getType() != type) continue;
            map.merge(tx.getCategory(), tx.getAmount(), BigDecimal::add);
        }
        return map;
    }

    public BigDecimal sumForCategory(TransactionType type, String category) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction tx : transactions) {
            if (tx.getType() == type && tx.getCategory().equals(category)) {
                sum = sum.add(tx.getAmount());
            }
        }
        return sum;
    }

    public BigDecimal sumForCategories(TransactionType type, List<String> categories) {
        BigDecimal sum = BigDecimal.ZERO;
        for (String cat : categories) {
            sum = sum.add(sumForCategory(type, cat));
        }
        return sum;
    }
}
