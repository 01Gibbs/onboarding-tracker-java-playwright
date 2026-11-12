package dev.upskill.ledger.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/** Protects HTML pages: anonymous users go to the login page, auditors cannot change data. */
public class PageAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Optional<CurrentUser> user = Sessions.currentUser(request);
        if (user.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        boolean readOnlyRequest = "GET".equals(request.getMethod()) || request.getRequestURI().endsWith("/logout");
        if (!readOnlyRequest && !user.get().canEdit()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to do that");
        }
        request.setAttribute(Sessions.USER_ATTRIBUTE, user.get());
        return true;
    }
}
