package com.springbootangular.backend.controller;

import com.springbootangular.backend.converter.user.UserDTOToModelConverter;
import com.springbootangular.backend.converter.user.UserToDTOConverter;
import com.springbootangular.backend.dto.UserDTO;
import com.springbootangular.backend.model.User;
import com.springbootangular.backend.service.UserService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequestMapping("api/user")
@RestController
public class UserController {

    private final Log log = LogFactory.getLog(UserController.class);

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
        log.debug("getUserById(" + id + ") ENTERED");
        Optional<User> userById = userService.getUserById(id);
        if (userById.isPresent()) {
            UserDTO userDTO = userToDTOConverter.convert(userById.get());
            log.debug("getUserById(" + id + ") EXITED - 200");
            return ResponseEntity.ok(userDTO);
        } else {
            log.debug("getUserById(" + id + ") EXITED - 404");
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        log.debug("createUser(" + userDTO + ") ENTERED");
        User user = userDTOToModelConverter.convert(userDTO);
        if (user != null && user.getId() == null) {
            User saveUser = userService.saveUser(user);
            UserDTO dto = userToDTOConverter.convert(saveUser);
            log.debug("createUser(" + userDTO + ") EXITED - 201");
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } else if (user != null && user.getId() != null){
            log.debug("createUser(" + userDTO + ") EXITED - 400");
            return ResponseEntity.badRequest().build();
        } else {
            log.debug("createUser(" + userDTO + ") EXITED - 500");
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/")
    public ResponseEntity<UserDTO> saveUser(@RequestBody UserDTO userDTO) {
        log.debug("saveUser(" + userDTO + ") ENTERED");
        User user = userDTOToModelConverter.convert(userDTO);
        if (user != null && user.getId() == null) {
            log.debug("saveUser(" + userDTO + ") EXITED - 404");
            return ResponseEntity.notFound().build();
        } else if (user != null && user.getId() != null){
            User saveUser = userService.saveUser(user);
            UserDTO dto = userToDTOConverter.convert(saveUser);
            log.debug("saveUser(" + userDTO + ") EXITED - 200");
            return ResponseEntity.ok(dto);
        } else {
            log.debug("saveUser(" + userDTO + ") EXITED - 500");
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Integer id) {
        log.debug("deleteUser(" + id + ") ENTERED");
        userService.deleteUserById(id);
        log.debug("deleteUser(" + id + ") EXITED");
    }
}
