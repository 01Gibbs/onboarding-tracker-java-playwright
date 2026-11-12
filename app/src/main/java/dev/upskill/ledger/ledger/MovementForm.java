package dev.upskill.ledger.ledger;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Input for a deposit, withdrawal or transfer. {@code toAccountId} is only used for transfers. */
public class MovementForm {

    @NotNull(message = "Choose a type")
    private MovementType type = MovementType.DEPOSIT;

    // The amount's own rules (positive, limit, pence) live in Amount and come back as field errors.
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private Long toAccountId;

    @Size(max = 80, message = "Description must be 80 characters or fewer")
    private String description;

    public MovementType getType() {
        return type;
    }

    public void setType(MovementType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
