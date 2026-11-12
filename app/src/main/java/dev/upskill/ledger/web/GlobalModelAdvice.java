package dev.upskill.ledger.web;

import dev.upskill.ledger.auth.CurrentUser;
import dev.upskill.ledger.auth.Sessions;
import dev.upskill.ledger.common.Money;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the signed-in user and the current path available to every page template. */
@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {

    private final Money money;

    public GlobalModelAdvice(Money money) {
        this.money = money;
    }

    @ModelAttribute("currentUser")
    public CurrentUser currentUser(HttpServletRequest request) {
        return Sessions.currentUser(request).orElse(null);
    }

    @ModelAttribute("currencySymbol")
    public String currencySymbol() {
        return money.getSymbol();
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
