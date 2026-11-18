package dev.upskill.e2e.workshop.support;

import java.util.List;

/**
 * Mission B0: checks that the books balance for some accounts, using only what the JSON API shows.
 * The contract below is fixed (later missions call it); the implementation is yours.
 * See lessons/B0-ledger-invariants.md.
 *
 * <pre>
 * new LedgerInvariants(auditorApi).assertBalanced(fromId, toId);
 * </pre>
 */
public final class LedgerInvariants {

    /** The rules a correct ledger always keeps. */
    public enum Invariant {
        BALANCE_MATCHES_ENTRIES("balance = sum of entries"),
        RUNNING_BALANCE("each entry's balanceAfter = running total"),
        TRANSFER_PAIRING("every transfer has one matching out and in leg"),
        NO_NEGATIVE_BALANCE("no negative balance"),
        CLOSED_IS_EMPTY("closed accounts hold 0.00"),
        AMOUNTS_IN_PENCE("amounts have 2 decimal places");

        private final String rule;

        Invariant(String rule) {
            this.rule = rule;
        }

        public String rule() {
            return rule;
        }
    }

    /** One broken rule, on one account, with what was seen. */
    public record Violation(Invariant invariant, String accountNumber, String detail) {
        @Override
        public String toString() {
            return "account " + accountNumber + " breaks \"" + invariant.rule() + "\": " + detail;
        }
    }

    private final LedgerApi api;

    /** Pass a client logged in as the auditor, so the check can never change what it is checking. */
    public LedgerInvariants(LedgerApi auditorApi) {
        this.api = auditorApi;
    }

    /** Fails once per broken rule (all reported together with assertAll), naming the account and the rule. */
    public void assertBalanced(long... accountIds) {
        throw new UnsupportedOperationException("TODO mission B0, exercise 1");
    }

    /** Every broken rule for these accounts, and for the other leg of each of their transfers. */
    public List<Violation> check(long... accountIds) {
        throw new UnsupportedOperationException("TODO mission B0, exercise 1");
    }
}
