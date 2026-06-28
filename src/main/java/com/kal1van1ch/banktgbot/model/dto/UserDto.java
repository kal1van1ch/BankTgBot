package com.kal1van1ch.banktgbot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto{
        private Long id;
        private String tgId;
        private String firstName;
        private String lastName;
        private String patronymic;
        private String phoneNumber;
}
