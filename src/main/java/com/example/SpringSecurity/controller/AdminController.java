package com.example.SpringSecurity.controller;


import com.example.SpringSecurity.entity.User;
import com.example.SpringSecurity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AdminController {

    @Autowired
    private UserService userService;

    @PostMapping("admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> makeAdmin(@PathVariable Integer id){
        return new ResponseEntity<>(userService.makeAdmin(id), HttpStatus.ACCEPTED);
    }
}
