package dev.upskill.ledger.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Protects the JSON API. Accepts either {@code Authorization: Bearer <token>} (from POST /api/auth/login)
 * or a logged-in browser session, so the UI's own fetch() calls work too.
 */
public class ApiAuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokens;

    public ApiAuthInterceptor(TokenStore tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Optional<CurrentUser> user = fromBearer(request).or(() -> Sessions.currentUser(request));
        if (user.isEmpty()) {
            writeError(response, 401, "Unauthorized", "Authentication required");
            return false;
        }
        if (!"GET".equals(request.getMethod()) && !user.get().canEdit()) {
            writeError(response, 403, "Forbidden", "You do not have permission to do that");
            return false;
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

    private static void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}"
                .formatted(status, error, message));
    }
}
