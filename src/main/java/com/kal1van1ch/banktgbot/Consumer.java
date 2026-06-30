package com.kal1van1ch.banktgbot;


import com.kal1van1ch.banktgbot.model.Command;
import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.service.SendMessageService;
import com.kal1van1ch.banktgbot.service.UserService;
import com.kal1van1ch.banktgbot.service.TransactionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Consumer implements LongPollingSingleThreadUpdateConsumer {

    private final Map<Long, Status> statusMap = new ConcurrentHashMap<>();
    private final TransactionService transactionService;
    private final SendMessageService sendMessageService;
    private final UserService userService;

    public Consumer(
            TransactionService transactionService,
            SendMessageService sendMessageService,
            UserService userService
    ) {
        this.transactionService = transactionService;
        this.sendMessageService = sendMessageService;
        this.userService = userService;
    }

    @Override
    public void consume(Update update) {

        Status status;
        String text = "no_data";
        long chatId = 0;
        long tgId = 0;

        if (
                update.hasMessage()
                && update.getMessage().hasText()
        ) {
            chatId = update.getMessage().getChatId();
            text = update.getMessage().getText();
            tgId = update.getMessage().getFrom().getId();
            status = statusMap.getOrDefault(chatId, Status.DEFAULT);

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
                    sendMessageService.sendMessage(
                            chatId,
                            "Извините, вы не зарегистрированы. Для продолжения работы необходимо зарегистрироваться при помощи команды /register"
                    );
                    statusMap.put(chatId, Status.DEFAULT);
                }
                else {
                    statusMap.put(chatId, Status.WAITING_FOR_AMOUNT);
                }
            }

            else if (text.equals(Command.REGISTER.getCommand())){
                if (!userService.isRegistered(
                        String.valueOf(update
                                .getMessage()
                                .getFrom()
                                .getId())
                )){
                    statusMap.put(chatId, Status.WAITING_FIRST_NAME);
                }
                else{
                    sendMessageService.sendMessage(chatId, "Вы уже зарегистрированы");
                }
            }

            else if (text.equals(Command.HELP.getCommand())){
                statusMap.put(chatId, Status.HELP);
                sendMessageService.helpMessage(chatId, statusMap);
            }

            else if (status == Status.DEFAULT){
                sendMessageService.unknownMessage(chatId);
            }
        }

        else if (update.hasCallbackQuery()){
            chatId = update.getCallbackQuery().getMessage().getChatId();
            text = update.getCallbackQuery().getData();
            tgId = update.getCallbackQuery().getFrom().getId();
        }

        status = statusMap.getOrDefault(chatId, Status.DEFAULT);

        switch (status){
            case WAITING_FOR_AMOUNT -> transactionService.amountInputMessage(chatId, statusMap);
            case WAITING_FOR_NUMBER -> transactionService.numberInputMessage(chatId, text, statusMap);
            case WAITING_FOR_BANK -> transactionService.bankInputMessage(chatId, text, statusMap);
            case TRANSFER_LINK -> transactionService.transferLinkMessage(
                    chatId,
                    text,
                    statusMap,
                    String.valueOf(tgId)
            );



            case WAITING_FIRST_NAME -> userService.registerFirstName(chatId, statusMap);
            case WAITING_LAST_NAME -> userService.registerLastName(chatId, text, statusMap);
            case WAITING_PATRONYMIC -> userService.registerPatronymic(chatId, text, statusMap);
            case WAITING_PHONE_NUMBER -> userService.registerPhoneNumber(chatId, text, statusMap);
            case TRANSFER_SUCCESSFUL_REGISTRATION -> userService.transferSuccessfulRegistrationMessage(
                    chatId,
                    text,
                    statusMap,
                    String.valueOf(tgId)
            );
        }
    }
}
