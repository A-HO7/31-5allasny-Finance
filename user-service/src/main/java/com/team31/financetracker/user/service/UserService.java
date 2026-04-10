package com.team31.financetracker.user.service;

import com.team31.financetracker.user.dto.UserTransactionSummaryDTO;
import com.team31.financetracker.user.model.User;
import com.team31.financetracker.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.team31.financetracker.user.model.Role;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Create
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // Read All
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Read by ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // Update
    public User updateUser(Long id, User userDetails) {
        User existingUser = getUserById(id);
        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());
        existingUser.setPassword(userDetails.getPassword());
        existingUser.setRole(userDetails.getRole());
        return userRepository.save(existingUser);
    }

    // Delete
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    //Search with Filter (S1-F1)
    public List<User> searchUsers(String name, String email, Role role) {
        return userRepository.searchUsers(name, email, role != null ? role.name() : null);
    }


    // Get User Transaction Summary (S1-F3)
    public UserTransactionSummaryDTO getUserTransactionSummary(Long userId) {
        User user = getUserById(userId);

        Object[] result = userRepository.getUserTransactionSummary(userId);

        if (result == null || result.length == 0) {
            return new UserTransactionSummaryDTO(
                    user.getId(),
                    user.getName(),
                    0L,
                    0L,
                    0L,
                    0.0,
                    0.0
            );
        }

        return new UserTransactionSummaryDTO(
                ((Number) result[0]).longValue(),
                (String) result[1],
                ((Number) result[2]).longValue(),
                ((Number) result[3]).longValue(),
                ((Number) result[4]).longValue(),
                ((Number) result[5]).doubleValue(),
                ((Number) result[6]).doubleValue()
        );
    }

}