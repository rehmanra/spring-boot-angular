package com.springbootangular.backend.converter.user;

import com.springbootangular.backend.converter.BaseConverter;
import com.springbootangular.backend.dto.UserDTO;
import com.springbootangular.backend.model.User;
import org.springframework.stereotype.Component;

@Component("userToDTOConverter")
public class UserToDTOConverter implements BaseConverter<User, UserDTO> {
    @Override
    public UserDTO convert(User source) {
        return new UserDTO(source.getId(), source.getName());
    }
}
