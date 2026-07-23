package com.kal1van1ch.banktgbot.validation;

import org.springframework.stereotype.Component;

@Component
public class TransactionValidation {

    public boolean isValidAmount(String amount){
        return amount.matches("^[1-9]+([0-9]+)?$");
    }

    public boolean isValidPhoneNumber(String phoneNumber){
        return phoneNumber.matches("^\\d{11}$");
    }
}
