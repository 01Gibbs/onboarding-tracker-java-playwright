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

    /** One ledger entry, read once from the JSON so the checks below can stay readable. */
    private record Entry(long id, String type, BigDecimal amount, BigDecimal balanceAfter,
                         String accountNumber, String counterparty, String transferId) {

        static Entry from(JsonObject json) {
            return new Entry(json.get("id").getAsLong(), json.get("type").getAsString(),
                    json.get("amount").getAsBigDecimal(), json.get("balanceAfter").getAsBigDecimal(),
                    json.get("accountNumber").getAsString(), text(json, "counterpartyAccountNumber"),
                    text(json, "transferId"));
        }

        /** Deposits and transfers in add; withdrawals and transfers out subtract. */
        BigDecimal signedAmount() {
            return type.equals("DEPOSIT") || type.equals("TRANSFER_IN") ? amount : amount.negate();
        }

        boolean isTransferLeg() {
            return transferId != null;
        }

        private static String text(JsonObject json, String field) {
            JsonElement value = json.get(field);
            return value == null || value.isJsonNull() ? null : value.getAsString();
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
        Map<String, List<Entry>> entriesByAccount = new LinkedHashMap<>();

        for (long id : accountIds) {
            JsonObject account = api.getAccount(id);
            List<Entry> entries = entriesOldestFirst(id);
            entriesByAccount.put(account.get("accountNumber").getAsString(), entries);
            checkAccount(account, entries, violations);
        }
        checkTransfers(entriesByAccount, violations);
        return violations;
    }

    private void checkAccount(JsonObject account, List<Entry> entries, List<Violation> violations) {
        String number = account.get("accountNumber").getAsString();
        BigDecimal balance = account.get("balance").getAsBigDecimal();

        BigDecimal running = BigDecimal.ZERO;
        boolean chainBroken = false;
        for (Entry entry : entries) {
            running = running.add(entry.signedAmount());
            if (!chainBroken && entry.balanceAfter().compareTo(running) != 0) {
                chainBroken = true; // report the first break only; everything after it is off too
                violations.add(new Violation(Invariant.RUNNING_BALANCE, number, "entry " + entry.id() + " says "
                        + entry.balanceAfter() + " but the running total is " + running));
            }
            if (entry.balanceAfter().signum() < 0) {
                violations.add(new Violation(Invariant.NO_NEGATIVE_BALANCE, number,
                        "entry " + entry.id() + " left the balance at " + entry.balanceAfter()));
            }
            if (entry.amount().scale() != 2 || entry.balanceAfter().scale() != 2) {
                violations.add(new Violation(Invariant.AMOUNTS_IN_PENCE, number, "entry " + entry.id()
                        + " has amount " + entry.amount() + ", balanceAfter " + entry.balanceAfter()));
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

    /** Each transfer must have exactly one OUT and one IN leg, for the same amount, naming each other. */
    private void checkTransfers(Map<String, List<Entry>> entriesByAccount, List<Violation> violations) {
        Set<String> transferIds = new LinkedHashSet<>();
        Set<String> counterparties = new LinkedHashSet<>();
        for (List<Entry> entries : entriesByAccount.values()) {
            for (Entry entry : entries) {
                if (entry.isTransferLeg()) {
                    transferIds.add(entry.transferId());
                    counterparties.add(entry.counterparty());
                }
            }
        }
        // The other leg may sit on an account we weren't asked about: read those accounts' entries too.
        counterparties.removeAll(entriesByAccount.keySet());
        if (!counterparties.isEmpty()) {
            Map<String, Long> idByNumber = accountIdsByNumber();
            for (String number : counterparties) {
                Long id = idByNumber.get(number);
                entriesByAccount.put(number, id == null ? List.of() : entriesOldestFirst(id));
            }
        }

        for (String transferId : transferIds) {
            List<Entry> legs = entriesByAccount.values().stream().flatMap(List::stream)
                    .filter(e -> transferId.equals(e.transferId())).toList();
            List<Entry> outs = legs.stream().filter(l -> l.type().equals("TRANSFER_OUT")).toList();
            List<Entry> ins = legs.stream().filter(l -> l.type().equals("TRANSFER_IN")).toList();
            String where = legs.getFirst().accountNumber();
            if (outs.size() != 1 || ins.size() != 1) {
                violations.add(new Violation(Invariant.TRANSFER_PAIRING, where, "transfer " + transferId
                        + " has " + outs.size() + " out and " + ins.size() + " in leg(s)"));
                continue;
            }
            Entry out = outs.getFirst();
            Entry in = ins.getFirst();
            if (out.amount().compareTo(in.amount()) != 0) {
                violations.add(new Violation(Invariant.TRANSFER_PAIRING, where,
                        "transfer " + transferId + " sent " + out.amount() + " but received " + in.amount()));
            }
            if (!out.counterparty().equals(in.accountNumber()) || !in.counterparty().equals(out.accountNumber())) {
                violations.add(new Violation(Invariant.TRANSFER_PAIRING, where,
                        "transfer " + transferId + " legs don't name each other's accounts"));
            }
        }
    }

    /** The API lists newest first; the running balance has to be replayed oldest first, by id (not time). */
    private List<Entry> entriesOldestFirst(long accountId) {
        List<Entry> entries = new ArrayList<>();
        for (JsonElement json : api.transactions(accountId)) {
            entries.add(Entry.from(json.getAsJsonObject()));
        }
        entries.sort(Comparator.comparingLong(Entry::id));
        return entries;
    }

    private Map<String, Long> accountIdsByNumber() {
        Map<String, Long> ids = new HashMap<>();
        for (JsonElement json : api.allAccounts()) {
            JsonObject account = json.getAsJsonObject();
            ids.put(account.get("accountNumber").getAsString(), account.get("id").getAsLong());
        }
        return ids;
    }
}
