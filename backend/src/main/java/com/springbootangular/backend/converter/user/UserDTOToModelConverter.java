package com.springbootangular.backend.converter.user;

import com.springbootangular.backend.converter.BaseConverter;
import com.springbootangular.backend.dto.UserDTO;
import com.springbootangular.backend.model.User;
import org.springframework.stereotype.Component;

@Component("userDTOToModelConverter")
public class UserDTOToModelConverter implements BaseConverter<UserDTO, User> {

    @Override
    public User convert(UserDTO source) {
        User target = new User();
        target.setId(source.id());
        target.setName(source.name());
        return target;
    }
}
