package com.kal1van1ch.banktgbot;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UpdateConsumer implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final Map<Long, Status> statusMap = new ConcurrentHashMap<>();
    private final Map<String, String> inputInfoMap = new ConcurrentHashMap<>();

    public UpdateConsumer() {
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public String getBotToken() {
        return System.getenv("BANK_BOT_KEY");
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (
                update.hasMessage()
                && update.getMessage().hasText()
        ) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            Status status = statusMap.getOrDefault(chatId, Status.DEFAULT);

            if (text.equals("/start")) {
                statusMap.put(chatId, Status.WAITING_FOR_AMOUNT);
                status = statusMap.get(chatId);
            }

            else if (!text.equals("/start") && status == Status.DEFAULT) unknownMessage(chatId);

            switch (status){
                case WAITING_FOR_AMOUNT -> amountInputMessage(chatId);
                case WAITING_FOR_NUMBER -> numberInputMessage(chatId, text);
                case WAITING_FOR_BANK -> bankInputMessage(chatId, text);
                case TRANSFER_LINK -> transferLinkHandler(chatId, text);
            }
        }
    }

    private void amountInputMessage(long chatId){
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text("Введите сумму для перевода в формате \"100\"")
                .build();
        try{
            telegramClient.execute(message);
            statusMap.put(chatId, Status.WAITING_FOR_NUMBER);
        }
        catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    private void numberInputMessage(long chatId, String text){

        inputInfoMap.put("amount", text);

        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text("Введите номер телефона для перевода в формате \"9161112233\"")
                .build();
        try{
            telegramClient.execute(message);
            statusMap.put(chatId, Status.WAITING_FOR_BANK);
        }
        catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    private void bankInputMessage(long chatId, String text){

        inputInfoMap.put("phoneNumber", text);

        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text("Введите банк для перевода в формате \"Тбанк, Сбербанк, Альфа-банк\"")
                .build();
        try{
            telegramClient.execute(message);
            statusMap.put(chatId, Status.TRANSFER_LINK);
        }
        catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    private void transferLinkHandler(long chatId, String text){

        inputInfoMap.put("bank", text);
        StringBuilder sb = new StringBuilder("Итоговая информация о пользователе:\n");

        for (Map.Entry<String, String> en: inputInfoMap.entrySet()){
            String key = en.getKey();
            String val = en.getValue();

            sb.append(String.format("%s : %s \n", key, val));
        }

        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(sb.toString())
                .build();
        try{
            telegramClient.execute(message);
            statusMap.put(chatId, Status.DEFAULT);
        }
        catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    private void unknownMessage(long chatId){
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text("Неизвестная команда. Для перезапуска бота используйте команду /start")
                .build();
        try{
            telegramClient.execute(message);
        }
        catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
}
