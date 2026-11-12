package dev.upskill.ledger.api;

import dev.upskill.ledger.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catches any /api path no other endpoint matches, so clients get a 404 in the ApiError shape
 * instead of Spring's default error body. More specific mappings always win over this pattern.
 */
@RestController
public class ApiFallback {

    @RequestMapping("/api/**")
    public void unknown(HttpServletRequest request) {
        throw new NotFoundException("Endpoint", request.getMethod() + " " + request.getRequestURI());
    }
}
