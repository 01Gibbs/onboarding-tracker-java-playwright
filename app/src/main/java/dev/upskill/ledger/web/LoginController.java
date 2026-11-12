package dev.upskill.ledger.web;

import dev.upskill.ledger.auth.Sessions;
import dev.upskill.ledger.auth.UserDirectory;
import dev.upskill.ledger.auth.UserDirectory.LoginResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    private final UserDirectory users;

    public LoginController(UserDirectory users) {
        this.users = users;
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        return Sessions.currentUser(request).isPresent() ? "redirect:/" : "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(defaultValue = "") String username,
                        @RequestParam(defaultValue = "") String password,
                        HttpServletRequest request, Model model) {
        LoginResult result = users.authenticate(username.trim(), password);
        if (result instanceof LoginResult.Failure(String message)) {
            model.addAttribute("error", message);
            model.addAttribute("username", username);
            return "login";
        }
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        request.getSession(true).setAttribute(Sessions.USER_ATTRIBUTE, ((LoginResult.Success) result).user());
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes redirect) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirect.addFlashAttribute("info", "You have been signed out");
        return "redirect:/login";
    }
}
