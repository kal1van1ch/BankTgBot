package com.kal1van1ch.banktgbot.model;

import lombok.Getter;

@Getter
public enum Bank {
    TBANK("TBANK"),
    SBERBANK("SBER"),
    ALFABANK("ALFA"),
    GAZPROMBANK("GPB");

    private final String callbackData;

    Bank(String callbackData){
        this.callbackData = callbackData;
    }

    public static boolean isBank(String bank){
        for (Bank b: values()){
            if (b.getCallbackData().equals(bank)){
                return true;
            }
        }

        return false;
    }
}
