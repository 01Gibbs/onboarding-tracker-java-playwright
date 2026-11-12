package dev.upskill.ledger.api;

import dev.upskill.ledger.api.ApiDtos.LoginRequest;
import dev.upskill.ledger.api.ApiDtos.LoginResponse;
import dev.upskill.ledger.auth.TokenStore;
import dev.upskill.ledger.auth.UserDirectory;
import dev.upskill.ledger.auth.UserDirectory.LoginResult;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        LoginResult result = users.authenticate(
                request.username() == null ? "" : request.username(),
                request.password() == null ? "" : request.password());
        return switch (result) {
            case LoginResult.Success(var user) -> ResponseEntity.ok(new LoginResponse(
                    tokens.issue(user), user.username(), user.displayName(), user.role().name()));
            case LoginResult.Failure(var message) -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "Unauthorized", message, Map.of()));
        };
    }
}
