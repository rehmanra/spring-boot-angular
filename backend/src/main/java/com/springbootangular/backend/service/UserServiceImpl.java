package com.springbootangular.backend.service;

import com.springbootangular.backend.dao.UserDao;
import com.springbootangular.backend.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    @Override
    public List<User> getUsersContainingName(String name) {
        return userDao.findByNameContaining(name);
    }

    @Override
    public Optional<User> getUserById(Integer id) {
        return userDao.findById(id);
    }

    @Override
    public User saveUser(User user) {
        return userDao.save(user);
    }

    @Override
    public void deleteUserById(Integer id) {
        userDao.deleteById(id);
    }
}
