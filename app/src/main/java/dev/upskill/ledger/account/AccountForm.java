package dev.upskill.ledger.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Input for opening an account. Shared by the HTML form and the JSON API. */
public class AccountForm {

    @NotNull(message = "Choose an account holder")
    private Long customerId;

    @NotNull(message = "Choose an account type")
    private AccountType type;

    @NotBlank(message = "Nickname is required")
    @Size(max = 40, message = "Nickname must be 40 characters or fewer")
    private String nickname;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
