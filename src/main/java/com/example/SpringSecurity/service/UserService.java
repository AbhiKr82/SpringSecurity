package com.example.SpringSecurity.service;

import com.example.SpringSecurity.UserDAO;
import com.example.SpringSecurity.entity.Role;
import com.example.SpringSecurity.entity.User;
import com.example.SpringSecurity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleService roleService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public User saveUser(UserDAO userDAO) {

        User user = new User();
        user.setUsername(userDAO.getUsername());
        user.setPassword(encoder.encode(userDAO.getPassword()));

        // Reuse existing "USER" role instead of inserting a duplicate every time
        Role userRole = roleService.findOrCreate("USER");

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public User makeAdmin(Integer id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user not found"));

        Set<Role> roles = user.getRoles();

        // Reuse existing "ADMIN" role and add it to the user's roles
        Role admin = roleService.findOrCreate("ADMIN");
        roles.add(admin);

        user.setRoles(roles);
        userRepository.save(user);
        return user;

    }
}
