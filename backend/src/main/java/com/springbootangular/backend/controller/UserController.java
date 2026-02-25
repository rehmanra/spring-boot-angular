package com.springbootangular.backend.controller;

import com.springbootangular.backend.converter.user.UserDTOToModelConverter;
import com.springbootangular.backend.converter.user.UserToDTOConverter;
import com.springbootangular.backend.dto.UserDTO;
import com.springbootangular.backend.model.User;
import com.springbootangular.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

@RequestMapping("api/user")
@RestController
@Validated
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserToDTOConverter userToDTOConverter;
    private final UserDTOToModelConverter userDTOToModelConverter;

    public UserController(UserService userService, UserToDTOConverter userToDTOConverter, UserDTOToModelConverter userDTOToModelConverter) {
        this.userService = userService;
        this.userToDTOConverter = userToDTOConverter;
        this.userDTOToModelConverter = userDTOToModelConverter;
    }

    @GetMapping(value = "/")
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestParam(required = false, value = "name") String name) {
        log.debug("getAllUsers() ENTERED");
        List<User> users;
        if(name != null && !name.isBlank()) {
            users = userService.getUsersContainingName(name);
        } else {
            users = userService.getAllUsers();
        }

        List<UserDTO> dtos = userToDTOConverter.convertAll(users);
        log.debug("getAllUsers() EXITED");
        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Integer id) {
        log.debug("getUserById({}) ENTERED", id);
        Optional<User> userById = userService.getUserById(id);
        if (userById.isPresent()) {
            UserDTO userDTO = userToDTOConverter.convert(userById.get());
            log.debug("getUserById({}) EXITED - 200", id);
            return ResponseEntity.ok(userDTO);
        } else {
            log.debug("getUserById({}) EXITED - 404", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        log.debug("createUser({}) ENTERED", userDTO);
        if (userDTO.id() != null) {
            log.debug("createUser - id present on create request, returning 400");
            return ResponseEntity.badRequest().build();
        }
        User user = userDTOToModelConverter.convert(userDTO);
        User saved = userService.saveUser(user);
        UserDTO dto = userToDTOConverter.convert(saved);
        log.debug("createUser({}) EXITED - 201", userDTO);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping(value = "/")
    public ResponseEntity<UserDTO> saveUser(@Valid @RequestBody UserDTO userDTO) {
        log.debug("saveUser({}) ENTERED", userDTO);
        if (userDTO.id() == null) {
            log.debug("saveUser - no id, returning 404");
            return ResponseEntity.notFound().build();
        }
        if (userService.getUserById(userDTO.id()).isEmpty()) {
            log.debug("saveUser - id {} not found, returning 404", userDTO.id());
            return ResponseEntity.notFound().build();
        }
        User user = userDTOToModelConverter.convert(userDTO);
        User saved = userService.saveUser(user);
        UserDTO dto = userToDTOConverter.convert(saved);
        log.debug("saveUser({}) EXITED - 200", userDTO);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Integer id) {
        log.debug("deleteUser({}) ENTERED", id);
        userService.deleteUserById(id);
        log.debug("deleteUser({}) EXITED", id);
    }
}
