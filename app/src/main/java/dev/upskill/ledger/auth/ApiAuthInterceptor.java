package dev.upskill.ledger.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Protects the JSON API. Accepts either {@code Authorization: Bearer <token>} (from POST /api/auth/login)
 * or a logged-in browser session, so the UI's own fetch() calls work too.
 * Failures are thrown so ApiExceptionHandler renders them in the usual ApiError shape.
 */
public class ApiAuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokens;

    public ApiAuthInterceptor(TokenStore tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Optional<CurrentUser> user = fromBearer(request).or(() -> Sessions.currentUser(request));
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (!"GET".equals(request.getMethod()) && !user.get().canEdit()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to do that");
        }
        request.setAttribute(Sessions.USER_ATTRIBUTE, user.get());
        return true;
    }

    private Optional<CurrentUser> fromBearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return tokens.find(header.substring("Bearer ".length()).trim());
    }
}
