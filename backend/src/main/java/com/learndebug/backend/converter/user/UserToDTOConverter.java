package com.learndebug.backend.converter.user;

import com.learndebug.backend.converter.BaseConverter;
import com.learndebug.backend.dto.UserDTO;
import com.learndebug.backend.model.User;
import org.springframework.stereotype.Component;

@Component("userToDTOConverter")
public class UserToDTOConverter implements BaseConverter<User, UserDTO> {
    @Override
    public UserDTO convert(User source) {
        UserDTO target = new UserDTO();

        target.setId(source.getId());
        target.setName(source.getName());

        return target;
    }
}
