package dev.upskill.ledger.web;

import dev.upskill.ledger.common.BusinessRuleException;
import org.springframework.validation.BindingResult;

final class Forms {

    private Forms() {
    }

    /** Shows a domain rule failure on the form: next to its field when known, otherwise at the top. */
    static void reject(BindingResult result, BusinessRuleException e) {
        if (e.getField() != null) {
            result.rejectValue(e.getField(), "rule", e.getMessage());
        } else {
            result.reject("rule", e.getMessage());
        }
    }
}
