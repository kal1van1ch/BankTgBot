package com.kal1van1ch.banktgbot;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;

    public UpdateConsumer() {
        this.telegramClient = new OkHttpTelegramClient(System.getenv("BANK_BOT_KEY"));
    }

    @Override
    public void consume(Update update) {
        if (
                update.hasMessage()
                && update.getMessage().hasText()
        ) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            CommandHandler currentCommand = CommandHandler.knowCommand(text);

            switch (currentCommand){
                case  START -> handleStrat(chatId);
                default -> handleUnknown(chatId);
            }
        }
    }

    private void handleStrat(long chatId){
        executeMessage("Start", chatId);
    }

    private void handleUnknown(long chatId){
        executeMessage("Uknown command", chatId);
    }

    private void executeMessage(String text, long chatId){
        SendMessage newMessage = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();

        try{
            telegramClient.execute(newMessage);
        }
        catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
}
