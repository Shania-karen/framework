package test.controllers;

import framework.annotation.Controller;
import framework.annotation.Get;

@Controller
public class LoginController {
    
    @Get("/login")
    public String showLoginPage() {
        return "hehe";
    }

}