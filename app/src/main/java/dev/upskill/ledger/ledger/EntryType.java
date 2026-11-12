package dev.upskill.ledger.ledger;

public enum EntryType {
    DEPOSIT("Deposit", true),
    WITHDRAWAL("Withdrawal", false),
    TRANSFER_IN("Transfer in", true),
    TRANSFER_OUT("Transfer out", false);

    private final String label;
    private final boolean credit;

    EntryType(String label, boolean credit) {
        this.label = label;
        this.credit = credit;
    }

    public String getLabel() {
        return label;
    }

    public boolean isCredit() {
        return credit;
    }
}
