package com.example.SpringSecurity.service;

import com.example.SpringSecurity.entity.Role;
import com.example.SpringSecurity.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    public Role addRole(Role role) {
        return roleRepository.save(role);
    }

    public Role findOrCreate(String roleName) {
        List<Role> existing = roleRepository.findByRole(roleName);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        Role newRole = new Role();
        newRole.setRole(roleName);
        return roleRepository.save(newRole);
    }
}
