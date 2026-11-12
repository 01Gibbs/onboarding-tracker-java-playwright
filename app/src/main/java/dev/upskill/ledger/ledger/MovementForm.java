package dev.upskill.ledger.ledger;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Input for a deposit, withdrawal or transfer. {@code toAccountId} is only used for transfers. */
public class MovementForm {

    @NotNull(message = "Choose a type")
    private MovementType type = MovementType.DEPOSIT;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0", inclusive = false, message = "Amount must be greater than zero")
    @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1,000,000.00")
    @Digits(integer = 7, fraction = 2, message = "Amount can have at most 2 decimal places")
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
