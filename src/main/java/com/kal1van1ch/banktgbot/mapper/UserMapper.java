package com.kal1van1ch.banktgbot.mapper;

import com.kal1van1ch.banktgbot.model.dto.UserDto;
import com.kal1van1ch.banktgbot.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(UserDto u){
        User user = new User();

        user.setTgId(u.getTgId());
        user.setFirstName(u.getFirstName());
        user.setLastName(u.getLastName());
        user.setPatronymic(u.getPatronymic());
        user.setPhoneNumber(u.getPhoneNumber());

        return user;
    }

    public UserDto toDto(User u){
        UserDto user = new UserDto();

        user.setId(u.getId());
        user.setTgId(u.getTgId());
        user.setFirstName(u.getFirstName());
        user.setLastName(u.getLastName());
        user.setPatronymic(u.getPatronymic());
        user.setPhoneNumber(u.getPhoneNumber());

        return user;
    }
}
