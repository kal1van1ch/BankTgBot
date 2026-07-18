package com.kal1van1ch.banktgbot.model;

import lombok.Getter;

@Getter
public enum Bank {
    TBANK("Тбанк", "TBANK"),
    SBERBANK("Сбербанк", "SBER"),
    ALFABANK("Альфа-банк", "ALFA"),
    GAZPROMBANK("Газпромбанк", "GPB");

    private final String name;
    private final String callbackData;

    Bank(String name, String callbackData){
        this.name = name;
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
