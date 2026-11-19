package dev.upskill.ledger.api;

import dev.upskill.ledger.api.ApiDtos.LoginRequest;
import dev.upskill.ledger.api.ApiDtos.LoginResponse;
import dev.upskill.ledger.api.ApiDtos.SessionUserDto;
import dev.upskill.ledger.auth.CurrentUser;
import dev.upskill.ledger.auth.Sessions;
import dev.upskill.ledger.auth.TokenStore;
import dev.upskill.ledger.auth.UserDirectory;
import dev.upskill.ledger.auth.UserDirectory.LoginResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthApi {

    private final UserDirectory users;
    private final TokenStore tokens;

    public AuthApi(UserDirectory users, TokenStore tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    /** A bearer token, for API clients and tests. */
    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        LoginResult result = users.authenticate(
                request.username() == null ? "" : request.username(),
                request.password() == null ? "" : request.password());
        return switch (result) {
            case LoginResult.Success(var user) -> ResponseEntity.ok(new LoginResponse(
                    tokens.issue(user), user.username(), user.displayName(), user.role().name()));
            case LoginResult.Failure(var message) -> unauthorized(message);
        };
    }

    /** A browser session, as the Thymeleaf sign-in form creates: what the React and Angular front ends use. */
    @PostMapping("/api/auth/session")
    public ResponseEntity<?> signIn(@RequestBody LoginRequest body, HttpServletRequest request) {
        LoginResult result = users.authenticate(
                body.username() == null ? "" : body.username(),
                body.password() == null ? "" : body.password());
        return switch (result) {
            case LoginResult.Success(var user) -> {
                Sessions.start(request, user);
                yield ResponseEntity.ok(view(user));
            }
            case LoginResult.Failure(var message) -> unauthorized(message);
        };
    }

    @DeleteMapping("/api/auth/session")
    public ResponseEntity<Void> signOut(HttpServletRequest request) {
        Sessions.end(request);
        return ResponseEntity.noContent().build();
    }

    /** The signed-in user, by session or bearer token (ApiAuthInterceptor has already checked there is one). */
    @GetMapping("/api/auth/me")
    public SessionUserDto me(@RequestAttribute(Sessions.USER_ATTRIBUTE) CurrentUser user) {
        return view(user);
    }

    private static SessionUserDto view(CurrentUser user) {
        return new SessionUserDto(user.username(), user.displayName(), user.role().name(), user.role().getLabel(),
                user.canEdit());
    }

    private static ResponseEntity<ApiError> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(401, "Unauthorized", message, Map.of()));
    }
}
