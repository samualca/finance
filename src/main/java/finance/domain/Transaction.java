package finance.domain;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final TransactionType type;
    private final String category;
    private final BigDecimal amount;
    private final LocalDateTime createdAt;
    private final String comment;

    public Transaction(TransactionType type, String category, BigDecimal amount, LocalDateTime createdAt, String comment) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.createdAt = createdAt;
        this.comment = comment;
    }

    public TransactionType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getComment() {
        return comment;
    }
}
