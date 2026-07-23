package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.error.SHA256Exception;
import com.kal1van1ch.banktgbot.model.Scenario;
import com.kal1van1ch.banktgbot.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Service
public class GeneralMessageService {
    private final TelegramClient telegramClient;
    private final static Logger logger = LoggerFactory.getLogger(GeneralMessageService.class);

    public GeneralMessageService(TelegramClient telegramClient){
        this.telegramClient = telegramClient;
    }

    public void executeMessageSend(
            SendMessage message,
            String methodName,
            long chatId
    ){
        try{
            telegramClient.execute(message);
        }
        catch (TelegramApiException e){
            logger.error("""
                    \n
                    ====================================================================================================
                    Произошла ошибка при попытке отправки сообщения
                    Метод: {}
                    Чат: {}
                    ====================================================================================================
                    \n
                    """, methodName, chatId, e);
        }
    }

    public String getMethodName(){
        return StackWalker.getInstance()
                .walk(frames -> frames.skip(1).findFirst())
                .map(StackWalker.StackFrame::getMethodName)
                .orElse("unknown");
    }

    public void unknownMessage(
            long chatId
    ){
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text("Неизвестная команда. Для перезапуска бота используйте команду /start")
                .build();

        executeMessageSend(
                message,
                getMethodName(),
                chatId
        );
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

        executeMessageSend(
                message,
                getMethodName(),
                chatId
        );
    }

    public void helpMessage(
            long chatId,
            Map<Long, Status> statusMap,
            Map<Long, Scenario> scenarioMap
    ){
        String message = """
                Вас приветствует бот Bank, созданный для удобных операций по переводу денежных средств.
                
                Список команд:
                /help - вывод общей информации о боте и списка команд.
                /start - запуск бота.
                /restart - перезапуск бота.
                /register - зарегистрироваться.
                /edit - редактировать информацию о себе.
                /delete - удалить аккаунт.
                
                Важно: если собираетесь создавать новый аккаунт в телеграме, удалите свой аккаунт в данном боте, тк иначе впоследствии будет невозможно зарегистрироваться с данным номером телефона 
                """;

        sendMessage(chatId, message);
        statusMap.put(chatId, Status.DEFAULT);
        scenarioMap.put(chatId, Scenario.NOTHING);
    }

    public void sendButtonMessage(
            long chatId,
            String text,
            InlineKeyboardMarkup markup
    ){
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();

        message.setReplyMarkup(markup);

        executeMessageSend(
                message,
                getMethodName(),
                chatId
        );
    }

    public void sendButtonMessage(
            long chatId,
            String text,
            ReplyKeyboardMarkup markup
    ){
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();

        message.setReplyMarkup(markup);

        executeMessageSend(
                message,
                getMethodName(),
                chatId
        );
    }

    public void removeKeyboard(
            long chatId,
            String text
    ){
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();

        message.setReplyMarkup(new ReplyKeyboardRemove(true));

        executeMessageSend(
                message,
                getMethodName(),
                chatId
        );
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

    public void editMessage(
            long chatId,
            Map<Long, Status> statusMap
    ){
        String message = "Выберите, что хотите изменить";

        InlineKeyboardButton but1 = createButtonWithCallbackData("Имя", "FIRST_NAME_EDIT_SERVICE");
        InlineKeyboardButton but2 = createButtonWithCallbackData("Фамилия", "LAST_NAME_EDIT_SERVICE");
        InlineKeyboardButton but3 = createButtonWithCallbackData("Отчество", "PATRONYMIC_EDIT_SERVICE");
        InlineKeyboardButton but4 = createButtonWithCallbackData("Номер телефона", "PHONE_NUMBER_EDIT_SERVICE");
        InlineKeyboardButton but5 = createButtonWithCallbackData("Ничего", "NOTHING_EDIT_SERVICE");

        List<InlineKeyboardRow> keyboardRows = List.of(
                new InlineKeyboardRow(but1),
                new InlineKeyboardRow(but2),
                new InlineKeyboardRow(but3),
                new InlineKeyboardRow(but4),
                new InlineKeyboardRow(but5)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);

        sendButtonMessage(
                chatId,
                message,
                markup
        );

        statusMap.put(chatId, Status.WAITING);
    }

    public void waitMessage(
            long chatId
    ){
        sendMessage(chatId, "Бот ожидает иное действие");
    }

    public InlineKeyboardButton createButtonWithCallbackData(
            String text,
            String callback
    ){
        return InlineKeyboardButton
                .builder()
                .text(text)
                .callbackData(callback)
                .build();
    }

    public InlineKeyboardButton createButtonWithURL(
            String text,
            String url
    ){
        return InlineKeyboardButton
                .builder()
                .text(text)
                .url(url)
                .build();
    }
}
