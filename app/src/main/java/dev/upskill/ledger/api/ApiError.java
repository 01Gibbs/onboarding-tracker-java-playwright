package dev.upskill.ledger.api;

import java.util.Map;

/** Body of every non-2xx API response. {@code fieldErrors} maps input names to messages. */
public record ApiError(int status, String error, String message, Map<String, String> fieldErrors) {
}
