package com.example.pproject.common.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StartController {

    @Value("${app.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    @GetMapping({"/", "/index"})
    public String start() {
        return "redirect:" + frontBaseUrl + "/";
    }
}
