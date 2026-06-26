package com.kal1van1ch.banktgbot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

@Component
public class MyTgBot implements SpringLongPollingBot {

    private final UpdateConsumer updateConsumer;

    public MyTgBot(UpdateConsumer updateConsumer){
        this.updateConsumer = updateConsumer;
    }

    @Override
    public String getBotToken() {
        return System.getenv("BANK_BOT_KEY");
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updateConsumer;
    }
}
