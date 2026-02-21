package com.example.SpringSecurity.service;


import com.example.SpringSecurity.UserDAO;
import com.example.SpringSecurity.entity.User;
import com.example.SpringSecurity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {


    @Autowired
    private UserRepository userRepository;

    private BCryptPasswordEncoder encoder= new BCryptPasswordEncoder(10);

    public User saveUser(UserDAO userDAO) {

        User user= new User();
        user.setUsername(userDAO.getUsername());
        user.setPassword(encoder.encode(userDAO.getPassword()));

        return userRepository.save(user);
    }
}
