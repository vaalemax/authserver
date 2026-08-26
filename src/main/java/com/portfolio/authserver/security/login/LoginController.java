package com.portfolio.authserver.security.login;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String realm,
                        @RequestParam(name = "continue", required = false) String continueUrl,
                        @RequestParam(required = false) String error,
                        Model model) {
        model.addAttribute("realm", realm);
        model.addAttribute("continueUrl", continueUrl);
        model.addAttribute("error", error!=null);
        return "login";
    }
}