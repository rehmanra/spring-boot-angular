package com.learndebug.backend.dao;

import com.learndebug.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDao extends JpaRepository<User, Integer> {
    List<User> findByNameContaining(String name);
}
