package com.kal1van1ch.banktgbot;


import com.kal1van1ch.banktgbot.model.Command;
import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.service.SendMessageService;
import com.kal1van1ch.banktgbot.service.UserService;
import com.kal1van1ch.banktgbot.service.TransferService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Consumer implements LongPollingSingleThreadUpdateConsumer {

    private final Map<Long, Status> statusMap = new ConcurrentHashMap<>();
    private final TransferService transferService;
    private final SendMessageService sendMessageService;
    private final UserService userService;

    public Consumer(
            TransferService transferService,
            SendMessageService sendMessageService,
            UserService userService
    ) {
        this.transferService = transferService;
        this.sendMessageService = sendMessageService;
        this.userService = userService;
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

            if (
                    text.equals(Command.START.getCommand())
                    || text.equals(Command.RESTART.getCommand())
            ) {

                if (!userService.isRegistered(
                        String.valueOf(update
                                        .getMessage()
                                        .getFrom()
                                        .getId())
                )){
                    statusMap.put(chatId, Status.WAITING_FIRST_NAME);
                }
                else {
                    statusMap.put(chatId, Status.WAITING_FOR_AMOUNT);
                }
                status = statusMap.get(chatId);
            }

            else if (status == Status.DEFAULT) sendMessageService.unknownMessage(chatId);

            switch (status){
                case WAITING_FOR_AMOUNT -> transferService.amountInputMessage(chatId, statusMap);
                case WAITING_FOR_NUMBER -> transferService.numberInputMessage(chatId, text, statusMap);
                case WAITING_FOR_BANK -> transferService.bankInputMessage(chatId, text, statusMap);
                case TRANSFER_LINK -> transferService.transferLinkMessage(
                        chatId,
                        text,
                        statusMap,
                        String.valueOf(update
                                .getMessage()
                                .getFrom()
                                .getId())
                );
                case WAITING_FIRST_NAME -> userService.registerFirstName(chatId, statusMap);
                case WAITING_LAST_NAME -> userService.registerLastName(chatId, text, statusMap);
                case WAITING_PATRONYMIC -> userService.registerPatronymic(chatId, text, statusMap);
                case WAITING_PHONE_NUMBER -> userService.registerPhoneNumber(chatId, text, statusMap);
                case TRANSFER_SUCCESSFUL_REGISTRATION -> userService.transferSuccessfulRegistrationMessage(
                        chatId,
                        text,
                        statusMap,
                        String.valueOf(update
                                .getMessage()
                                .getFrom()
                                .getId())
                );
            }
        }
    }
}
