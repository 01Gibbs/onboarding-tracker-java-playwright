package dev.upskill.e2e.solutions.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;

/** The "Open account" form at /accounts/new. */
public class OpenAccountPage {

    private final Page page;

    public OpenAccountPage(Page page) {
        this.page = page;
    }

    /** Holder is pre-selected when arriving from a customer page; pass null to keep it. */
    public AccountPage open(String holderLabel, String type, String nickname) {
        if (holderLabel != null) {
            page.getByLabel("Account holder").selectOption(new SelectOption().setLabel(holderLabel));
        }
        page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName(type)).check();
        page.getByLabel("Nickname").fill(nickname);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open account")).click();
        return new AccountPage(page);
    }
}
