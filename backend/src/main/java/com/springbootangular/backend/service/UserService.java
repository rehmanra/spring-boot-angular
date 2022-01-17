package com.springbootangular.backend.service;

import com.springbootangular.backend.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();
    List<User> getUsersContainingName(String name);
    Optional<User> getUserById(Integer id);
    User saveUser(User user);
    void deleteUserById(Integer id);
}
