package com.example.lab1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class rbpolab1komyagin {

    @GetMapping("/hello")
    public String hello() {
        return "Лабораторная работа запущена";
    }

    @GetMapping("/number")
    public int number() {
        return 1 + 2;
    }
}