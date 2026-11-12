package dev.upskill.e2e.solutions.support;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.function.Executable;

/**
 * Mission B0: checks that the books balance for some accounts, using only what the JSON API shows.
 * Give it a read-only (auditor) client so the check can never change what it is checking.
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

    public LedgerInvariants(LedgerApi auditorApi) {
        this.api = auditorApi;
    }

    /** Fails once per broken rule (all reported together), naming the account and the rule. */
    public void assertBalanced(long... accountIds) {
        List<Violation> violations = check(accountIds);
        assertAll("ledger invariants", violations.stream().map(v -> (Executable) () -> fail(v.toString())));
    }

    /** Every broken rule for these accounts, and for the other leg of each of their transfers. */
    public List<Violation> check(long... accountIds) {
        List<Violation> violations = new ArrayList<>();
        Map<String, List<JsonObject>> entriesByAccountNumber = new LinkedHashMap<>();
        Set<String> transferIds = new LinkedHashSet<>();

        for (long id : accountIds) {
            JsonObject account = api.getAccount(id);
            List<JsonObject> entries = entriesOldestFirst(id);
            entriesByAccountNumber.put(account.get("accountNumber").getAsString(), entries);
            checkAccount(account, entries, violations);
            entries.stream().filter(LedgerInvariants::isTransferLeg)
                    .forEach(e -> transferIds.add(e.get("transferId").getAsString()));
        }
        checkTransfers(transferIds, entriesByAccountNumber, violations);
        return violations;
    }

    private void checkAccount(JsonObject account, List<JsonObject> entries, List<Violation> violations) {
        String number = account.get("accountNumber").getAsString();
        BigDecimal balance = account.get("balance").getAsBigDecimal();

        BigDecimal running = BigDecimal.ZERO;
        boolean chainBroken = false;
        for (JsonObject entry : entries) {
            BigDecimal amount = entry.get("amount").getAsBigDecimal();
            BigDecimal balanceAfter = entry.get("balanceAfter").getAsBigDecimal();
            running = running.add(signed(entry));
            if (!chainBroken && balanceAfter.compareTo(running) != 0) {
                chainBroken = true; // report the first break only; everything after it is off too
                violations.add(new Violation(Invariant.RUNNING_BALANCE, number,
                        "entry " + entry.get("id") + " says " + balanceAfter + " but the running total is " + running));
            }
            if (balanceAfter.signum() < 0) {
                violations.add(new Violation(Invariant.NO_NEGATIVE_BALANCE, number,
                        "entry " + entry.get("id") + " left the balance at " + balanceAfter));
            }
            if (amount.scale() != 2 || balanceAfter.scale() != 2) {
                violations.add(new Violation(Invariant.AMOUNTS_IN_PENCE, number,
                        "entry " + entry.get("id") + " has amount " + amount + ", balanceAfter " + balanceAfter));
            }
        }
        if (balance.compareTo(running) != 0) {
            violations.add(new Violation(Invariant.BALANCE_MATCHES_ENTRIES, number,
                    "balance " + balance + " but its " + entries.size() + " entries add up to " + running));
        }
        if (balance.signum() < 0) {
            violations.add(new Violation(Invariant.NO_NEGATIVE_BALANCE, number, "balance is " + balance));
        }
        if (balance.scale() != 2) {
            violations.add(new Violation(Invariant.AMOUNTS_IN_PENCE, number, "balance is " + balance));
        }
        if ("CLOSED".equals(account.get("status").getAsString()) && balance.signum() != 0) {
            violations.add(new Violation(Invariant.CLOSED_IS_EMPTY, number, "closed with " + balance));
        }
    }

    /** Each transfer id must have exactly one OUT and one IN leg, for the same amount, naming each other. */
    private void checkTransfers(Set<String> transferIds, Map<String, List<JsonObject>> entriesByAccountNumber,
                                List<Violation> violations) {
        Map<String, Long> idByNumber = null;
        for (String transferId : transferIds) {
            List<JsonObject> legs = legs(transferId, entriesByAccountNumber);
            // The other leg may sit on an account we weren't asked about: fetch that account's entries too.
            for (JsonObject leg : List.copyOf(legs)) {
                String other = leg.get("counterpartyAccountNumber").getAsString();
                if (!entriesByAccountNumber.containsKey(other)) {
                    idByNumber = idByNumber != null ? idByNumber : accountIdsByNumber();
                    entriesByAccountNumber.put(other, entriesOldestFirst(idByNumber.get(other)));
                    legs = legs(transferId, entriesByAccountNumber);
                }
            }
            JsonObject out = only(legs, "TRANSFER_OUT");
            JsonObject in = only(legs, "TRANSFER_IN");
            String where = legs.getFirst().get("accountNumber").getAsString();
            if (legs.size() != 2 || out == null || in == null) {
                violations.add(new Violation(Invariant.TRANSFER_PAIRING, where,
                        "transfer " + transferId + " has " + legs.size() + " leg(s): " + types(legs)));
                continue;
            }
            if (out.get("amount").getAsBigDecimal().compareTo(in.get("amount").getAsBigDecimal()) != 0) {
                violations.add(new Violation(Invariant.TRANSFER_PAIRING, where, "transfer " + transferId
                        + " sent " + out.get("amount") + " but received " + in.get("amount")));
            }
            if (!out.get("counterpartyAccountNumber").getAsString().equals(in.get("accountNumber").getAsString())
                    || !in.get("counterpartyAccountNumber").getAsString().equals(out.get("accountNumber").getAsString())) {
                violations.add(new Violation(Invariant.TRANSFER_PAIRING, where,
                        "transfer " + transferId + " legs don't name each other's accounts"));
            }
        }
    }

    /** The API lists newest first; the running balance has to be replayed oldest first, by id (not time). */
    private List<JsonObject> entriesOldestFirst(long accountId) {
        List<JsonObject> entries = new ArrayList<>();
        for (JsonElement e : api.transactions(accountId)) {
            entries.add(e.getAsJsonObject());
        }
        entries.sort(Comparator.comparingLong(e -> e.get("id").getAsLong()));
        return entries;
    }

    private Map<String, Long> accountIdsByNumber() {
        Map<String, Long> ids = new HashMap<>();
        for (JsonElement a : api.allAccounts()) {
            ids.put(a.getAsJsonObject().get("accountNumber").getAsString(), a.getAsJsonObject().get("id").getAsLong());
        }
        return ids;
    }

    private static List<JsonObject> legs(String transferId, Map<String, List<JsonObject>> entriesByAccountNumber) {
        return entriesByAccountNumber.values().stream().flatMap(List::stream)
                .filter(e -> isTransferLeg(e) && e.get("transferId").getAsString().equals(transferId))
                .toList();
    }

    private static JsonObject only(List<JsonObject> legs, String type) {
        List<JsonObject> matching = legs.stream().filter(l -> type.equals(l.get("type").getAsString())).toList();
        return matching.size() == 1 ? matching.getFirst() : null;
    }

    private static List<String> types(List<JsonObject> legs) {
        return legs.stream().map(l -> l.get("type").getAsString()).toList();
    }

    private static boolean isTransferLeg(JsonObject entry) {
        return entry.has("transferId") && !entry.get("transferId").isJsonNull();
    }

    /** Deposits and transfers in add; withdrawals and transfers out subtract. */
    private static BigDecimal signed(JsonObject entry) {
        BigDecimal amount = entry.get("amount").getAsBigDecimal();
        String type = entry.get("type").getAsString();
        return type.equals("DEPOSIT") || type.equals("TRANSFER_IN") ? amount : amount.negate();
    }
}
