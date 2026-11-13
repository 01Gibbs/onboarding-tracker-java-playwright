package dev.upskill.ledger.api;

import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.account.AccountType;
import dev.upskill.ledger.customer.CustomerStatus;
import dev.upskill.ledger.ledger.EntryType;
import dev.upskill.ledger.ledger.MovementType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Every choice the pages offer (filters, radio buttons), with the label shown, in the order shown. */
@RestController
public class OptionsApi {

    public record Option(String value, String label) {
    }

    public record Options(List<Option> accountStatuses, List<Option> accountTypes, List<Option> customerStatuses,
                          List<Option> entryTypes, List<Option> movementTypes) {
    }

    @GetMapping("/api/options")
    public Options options() {
        return new Options(
                options(AccountStatus.values(), AccountStatus::getLabel),
                options(AccountType.values(), AccountType::getLabel),
                options(CustomerStatus.values(), CustomerStatus::getLabel),
                options(EntryType.values(), EntryType::getLabel),
                options(MovementType.values(), MovementType::getLabel));
    }

    private static <E extends Enum<E>> List<Option> options(E[] values, Function<E, String> label) {
        return Arrays.stream(values).map(e -> new Option(e.name(), label.apply(e))).toList();
    }
}
