package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.model.Status;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransferService {

    private final Map<String, String> inputInfoMap = new ConcurrentHashMap<>();
    private final GeneralService generalService;

    public TransferService(
            GeneralService generalService
    ){
        this.generalService = generalService;
    }

    public void amountInputMessage(
            long chatId,
            Map<Long, Status> statusMap
    ){
        String message = "Введите сумму для перевода в формате \"100\"";
        generalService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_FOR_NUMBER);
    }

    public void numberInputMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        inputInfoMap.put("amount", text);

        String message = "Введите номер телефона для перевода в формате \"9161112233\"";
        generalService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_FOR_BANK);
    }

    public void bankInputMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        inputInfoMap.put("phoneNumber", text);

        String message = "Введите банк для перевода в формате \"Тбанк, Сбербанк, Альфа-банк\"";
        generalService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.TRANSFER_LINK);
    }

    public void transferLinkMessage(
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

        generalService.sendMessage(chatId, sb.toString());
        statusMap.put(chatId, Status.DEFAULT);
    }
}
