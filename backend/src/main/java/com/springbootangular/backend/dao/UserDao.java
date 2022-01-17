package com.springbootangular.backend.dao;

import com.springbootangular.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDao extends JpaRepository<User, Integer> {
    List<User> findByNameContaining(String name);
}
