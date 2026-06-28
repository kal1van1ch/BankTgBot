package com.kal1van1ch.banktgbot.validation;

import org.springframework.stereotype.Component;

@Component
public class UserValidation {

    public boolean isValidFirstName(String firstName){
        return firstName.matches("^[а-яА-ЯёЁ]+$");
    }

    public boolean isValidLastName(String lastName){
        return lastName.matches("^[а-яА-ЯёЁ]+(-[а-яА-Я]+)?$");
    }

    public boolean isValidPatronymic(String patronymic){
        return patronymic.matches("^[а-яА-ЯёЁ]+$") || patronymic.equals("-");
    }

    public boolean isValidPhoneNumber(String phoneNumber){
        return phoneNumber.matches("^\\d{10}$");
    }
}
