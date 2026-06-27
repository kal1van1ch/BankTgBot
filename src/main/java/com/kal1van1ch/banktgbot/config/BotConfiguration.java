package com.kal1van1ch.banktgbot.config;

import com.kal1van1ch.banktgbot.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class BotConfiguration implements SpringLongPollingBot {

    private final Consumer consumer;

    public BotConfiguration(@Lazy Consumer consumer) {
        this.consumer = consumer;
    }

    @Bean
    public TelegramClient telegramClient(){
        return new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public String getBotToken() {
        return System.getenv("BANK_BOT_KEY");
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return consumer;
    }
}
