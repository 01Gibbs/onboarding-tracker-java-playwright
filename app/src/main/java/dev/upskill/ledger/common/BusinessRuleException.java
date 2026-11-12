package dev.upskill.ledger.common;

import org.springframework.http.HttpStatus;

/**
 * A request that is well-formed but breaks a domain rule (insufficient funds, duplicate email, ...).
 * {@code field} names the offending input so forms can show the message next to it; it may be null.
 */
public class BusinessRuleException extends RuntimeException {

    private final String field;
    private final HttpStatus status;

    public BusinessRuleException(String message) {
        this(message, null, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    public BusinessRuleException(String message, String field) {
        this(message, field, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    public BusinessRuleException(String message, String field, HttpStatus status) {
        super(message);
        this.field = field;
        this.status = status;
    }

    public static BusinessRuleException conflict(String message, String field) {
        return new BusinessRuleException(message, field, HttpStatus.CONFLICT);
    }

    public String getField() {
        return field;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
