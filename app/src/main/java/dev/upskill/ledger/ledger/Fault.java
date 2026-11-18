package dev.upskill.ledger.ledger;

import java.util.Arrays;
import java.util.Optional;

/**
 * Planted bugs for mission B0 (lessons/B0-ledger-invariants.md). Each one makes MoneyService break exactly one
 * ledger invariant, for one account, so learners can prove their checks catch it. Armed only through test support.
 */
public enum Fault {
    /** The new entry records the balance from before the movement: the running balance is wrong. */
    STALE_BALANCE_AFTER,
    /** A withdrawal moves the money but leaves no entry: the balance no longer matches the entries. */
    SKIP_WITHDRAWAL_ENTRY,
    /** Armed on the destination: the transfer-in leg is a penny short of the transfer-out leg. */
    TRANSFER_IN_SHORT,
    /** Armed on the destination: the transfer-in leg gets its own transfer id, so neither leg has a partner. */
    UNPAIRED_TRANSFER,
    /** Armed on the source: withdrawals and transfers skip the funds check, so the balance can go negative. */
    OVERDRAFT;

    /** The name used in URLs, e.g. {@code skip-withdrawal-entry}. */
    public String slug() {
        return name().toLowerCase().replace('_', '-');
    }

    public static Optional<Fault> fromSlug(String slug) {
        return Arrays.stream(values()).filter(f -> f.slug().equals(slug)).findFirst();
    }
}
