package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.model.Status;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConsumerService {

    private final TelegramClient telegramClient;
    private final Map<String, String> inputInfoMap = new ConcurrentHashMap<>();

    public ConsumerService(TelegramClient telegramClient){
        this.telegramClient = telegramClient;
    }

    public void amountInputMessage(
            long chatId,
            Map<Long, Status> statusMap
    ){
        String message = "Введите сумму для перевода в формате \"100\"";
        sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_FOR_NUMBER);
    }

    public void numberInputMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        inputInfoMap.put("amount", text);

        String message = "Введите номер телефона для перевода в формате \"9161112233\"";
        sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_FOR_BANK);
    }

    public void bankInputMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        inputInfoMap.put("phoneNumber", text);

        String message = "Введите банк для перевода в формате \"Тбанк, Сбербанк, Альфа-банк\"";
        sendMessage(chatId, message);
        statusMap.put(chatId, Status.TRANSFER_LINK);
    }

    public void transferLinkHandler(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        inputInfoMap.put("bank", text);
        StringBuilder sb = new StringBuilder("Итоговая информация о пользователе:\n");

        for (Map.Entry<String, String> en: inputInfoMap.entrySet()){
            String key = en.getKey();
            String val = en.getValue();

            sb.append(String.format("%s : %s \n", key, val));
        }

        sendMessage(chatId, sb.toString());
        statusMap.put(chatId, Status.DEFAULT);
    }

    public void unknownMessage(
            long chatId
    ){
        String message = "Неизвестная команда. Для перезапуска бота используйте команду /start";
        sendMessage(chatId, message);
    }

    private void sendMessage(
            long chatId,
            String text
    ){
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();
        try{
            telegramClient.execute(message);
        }
        catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
}
