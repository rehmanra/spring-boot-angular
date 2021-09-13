package com.learndebug.backend.converter.user;

import com.learndebug.backend.converter.BaseConverter;
import com.learndebug.backend.dto.UserDTO;
import com.learndebug.backend.model.User;
import com.learndebug.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("userDTOToModelConverter")
public class UserDTOToModelConverter implements BaseConverter<UserDTO, User> {
    private final UserService userService;

    public UserDTOToModelConverter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public User convert(UserDTO source) {
        User target;
        Integer id = source.getId();
        if(id != null) {
            Optional<User> userById = userService.getUserById(id);
            target = userById.orElseGet(User::new);
        } else {
            target = new User();
        }
        target.setName(source.getName());

        return target;
    }
}
