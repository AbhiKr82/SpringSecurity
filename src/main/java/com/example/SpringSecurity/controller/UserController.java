package com.example.SpringSecurity.controller;


import com.example.SpringSecurity.UserDAO;
import com.example.SpringSecurity.entity.User;
import com.example.SpringSecurity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/addUser")
    public ResponseEntity<User> createUser(@RequestBody UserDAO userDAO){
        return new ResponseEntity<>(userService.saveUser(userDAO), HttpStatus.CREATED);
    }
}
