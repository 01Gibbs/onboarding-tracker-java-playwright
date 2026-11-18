package dev.upskill.ledger.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.customer.CustomerForm;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/** Where a refused change shows on a page form: next to its field when the rule names one, otherwise at the top. */
class FormsTest {

    private final BindingResult result = new BeanPropertyBindingResult(new CustomerForm(), "form");

    @Test
    void aRuleAboutOneFieldIsShownNextToIt() {
        Forms.reject(result, new BusinessRuleException("A customer with this email already exists", "email"));

        assertThat(result.getFieldError("email").getDefaultMessage()).isEqualTo("A customer with this email already exists");
        assertThat(result.hasGlobalErrors()).isFalse();
    }

    @Test
    void aRuleAboutTheWholeFormIsShownAtTheTop() {
        Forms.reject(result, new BusinessRuleException("Something about the whole form"));

        assertThat(result.getGlobalError().getDefaultMessage()).isEqualTo("Something about the whole form");
        assertThat(result.hasFieldErrors()).isFalse();
    }
}
