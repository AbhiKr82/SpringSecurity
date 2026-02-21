package com.example.SpringSecurity.controller;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api")
public class GreetController {

    @GetMapping("/hello")
    public String hello(){
        return "Hello";
    }

    @GetMapping("/user")
    //@PreAuthorize("hasRole('USER')")
    public String helloUser(){
        return "Hello User";
    }

    @GetMapping("/admin")
    //@PreAuthorize("hasRole('ADMIN')")
    public String helloAdmin(){
        return "Hello Admin";
    }

    /*
    to use  @PreAuthorize("hasRole('ADMIN')") ,
    we have to enable @EnableMethodSecurity in securityConfig
     */


}
