package com.springbootangular.backend.dao;

import com.springbootangular.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserDaoTest {

    @Autowired
    UserDao userDao;

    @BeforeEach
    void setUp() {
        userDao.deleteAll();
        userDao.save(new User(null, "Alice"));
        userDao.save(new User(null, "Bob"));
        userDao.save(new User(null, "Charles"));
    }

    @Test
    void findByNameContaining_exactName_returnsMatchingUser() {
        List<User> result = userDao.findByNameContaining("Alice");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void findByNameContaining_partialSubstring_returnsMultipleMatches() {
        // "Alice" and "Charles" both contain "l"
        List<User> result = userDao.findByNameContaining("l");
        assertThat(result).hasSize(2);
    }

    @Test
    void findByNameContaining_noMatch_returnsEmptyList() {
        List<User> result = userDao.findByNameContaining("Zzz");
        assertThat(result).isEmpty();
    }

    @Test
    void findByNameContaining_caseSensitive_doesNotMatchWrongCase() {
        // PostgreSQL LIKE is case-sensitive; H2 MODE=PostgreSQL matches that behaviour
        // This documents and pins the expected case-sensitivity contract
        List<User> result = userDao.findByNameContaining("alice");
        assertThat(result).isEmpty();
    }

    @Test
    void save_persistsNewUserAndAssignsGeneratedId() {
        User saved = userDao.save(new User(null, "Diana"));
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Diana");
    }

    @Test
    void deleteById_removesUserFromRepository() {
        User saved = userDao.save(new User(null, "Eve"));
        Integer id = saved.getId();
        userDao.deleteById(id);
        assertThat(userDao.findById(id)).isEmpty();
    }
}
