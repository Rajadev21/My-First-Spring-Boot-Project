package com.smart.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smart.dao.UserRepository;
import com.smart.entity.User;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository; // Your user repository or service
    
    public boolean authenticate(String username, String password) {
        // Retrieve user from the database by username
        User user = userRepository.getuserByUserName(username);
        
        if (user != null && user.getPassword().equals(password)) {
            // Authentication successful
            return true;
        } else {
            // Authentication failed
            return false;
        }
    }
    
    // Other methods for user-related operations like registration, etc.
}

