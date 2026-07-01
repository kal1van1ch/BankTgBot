package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.error.SHA256Exception;
import com.kal1van1ch.banktgbot.model.Status;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
public class SendMessageService {
    private final TelegramClient telegramClient;

    public SendMessageService(TelegramClient telegramClient){
        this.telegramClient = telegramClient;
    }

    public void unknownMessage(
            long chatId
    ){
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

    public void sendMessage(
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

    public void helpMessage(
            Long chatId,
            Map<Long, Status> statusMap
    ){
        String message = """
                Вас приветствует бот Bank, созданный для удобных операций по переводу средств без входа в банковское приложение.
                
                Список команд:
                /help - вывод общей информации о боте и списка команд.
                /start - запуск бота.
                /restart - перезапуск бота.
                /register - зарегистрироваться.
                """;

        sendMessage(chatId, message);
        statusMap.put(chatId, Status.DEFAULT);
    }

    public void sendInlineButtonMessage(
            Long chatId,
            String text,
            InlineKeyboardMarkup markup
    ){
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();

        message.setReplyMarkup(markup);

        try{
            telegramClient.execute(message);
        }
        catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    public String encodeDataToSha256(String text){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encode = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            String encodedPhoneNumber = toHex(encode);
            return encodedPhoneNumber;
        }
        catch (NoSuchAlgorithmException e){
            throw new SHA256Exception("Ошибка при попытке инициализировать алгоритм SHA-256", e);
        }
    }

    private String toHex(byte[] arr){
        StringBuilder sb = new StringBuilder();

        for (byte b: arr){
            String hex = Integer.toHexString(0xff & b);

            if (hex.length() == 1){
                sb.append('0');
            }

            sb.append(hex);
        }
        return sb.toString();
    }
}
