package com.kal1van1ch.banktgbot;


import com.kal1van1ch.banktgbot.model.Bank;
import com.kal1van1ch.banktgbot.model.Command;
import com.kal1van1ch.banktgbot.model.Scenario;
import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.service.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Consumer implements LongPollingSingleThreadUpdateConsumer {

    private final Map<Long, Status> statusMap = new ConcurrentHashMap<>();
    private final Map<Long, Scenario> scenarioMap = new ConcurrentHashMap<>();
    private final TransactionService transactionService;
    private final GeneralMessageService generalMessageService;
    private final UserService userService;
    private final EditUserService editUserService;
    private final DeleteService deleteService;

    public Consumer(
            TransactionService transactionService,
            GeneralMessageService generalMessageService,
            UserService userService,
            EditUserService editUserService,
            DeleteService deleteService) {
        this.transactionService = transactionService;
        this.generalMessageService = generalMessageService;
        this.userService = userService;
        this.editUserService = editUserService;
        this.deleteService = deleteService;
    }

    @Override
    public void consume(Update update) {

        Status status;
        Scenario scenario;
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

                scenarioMap.put(chatId, Scenario.TRANSACTION);

                if (!userService.isRegistered(String.valueOf(tgId))){

                    generalMessageService.sendMessage(
                            chatId,
                            "Извините, вы не зарегистрированы. Для продолжения работы необходимо зарегистрироваться при помощи команды /register"
                    );
                    statusMap.put(chatId, Status.DEFAULT);
                    scenarioMap.put(chatId, Scenario.NOTHING);
                }
                else {
                    statusMap.put(chatId, Status.WAITING_FOR_AMOUNT);
                }
            }

            else if (text.equals(Command.REGISTER.getCommand())){
                if (!userService.isRegistered(String.valueOf(tgId))){

                    statusMap.put(chatId, Status.WAITING_FIRST_NAME);
                    scenarioMap.put(chatId, Scenario.REGISTRATION);
                }
                else{
                    generalMessageService.sendMessage(chatId, "Вы уже зарегистрированы");
                    statusMap.put(chatId, Status.DEFAULT);
                    scenarioMap.put(chatId, Scenario.NOTHING);
                }
            }

            else if (text.equals(Command.HELP.getCommand())){
                generalMessageService.helpMessage(chatId, statusMap, scenarioMap);
            }

            else if (text.equals(Command.EDIT.getCommand())){
                if (!userService.isRegistered(String.valueOf(tgId))){
                    generalMessageService.sendMessage(
                            chatId,
                            "Извините, вы не зарегистрированы. Для продолжения работы необходимо зарегистрироваться при помощи команды /register"
                    );
                    statusMap.put(chatId, Status.DEFAULT);
                    scenarioMap.put(chatId, Scenario.NOTHING);
                }
                else{
                    statusMap.put(chatId, Status.EDIT);
                    scenarioMap.put(chatId, Scenario.EDIT);
                }
            }

            else if (text.equals(Command.DELETE.getCommand())){
                if (!userService.isRegistered(String.valueOf(tgId))){

                    generalMessageService.sendMessage(
                            chatId,
                            "Извините, вы не зарегистрированы. Для продолжения работы необходимо зарегистрироваться при помощи команды /register"
                    );
                    statusMap.put(chatId, Status.DEFAULT);
                    scenarioMap.put(chatId, Scenario.NOTHING);
                }
                else{
                    statusMap.put(chatId, Status.DELETE);
                    scenarioMap.put(chatId, Scenario.DELETE);
                }
            }

            else if (status == Status.DEFAULT){
                generalMessageService.unknownMessage(chatId);
            }

        }

        else if (update.hasCallbackQuery()){
            chatId = update.getCallbackQuery().getMessage().getChatId();
            text = update.getCallbackQuery().getData();
            tgId = update.getCallbackQuery().getFrom().getId();

            if (Bank.isBank(text)) statusMap.put(chatId, Status.TRANSFER_LINK);
            else if (text.equals("MAKE_TRANSACTION")) statusMap.put(chatId, Status.MAKE_TRANSACTION);
            else if (text.endsWith("_EDIT_SERVICE")) statusMap.put(chatId, Status.EDIT_DATA);
        }

        else if (update.getMessage().hasContact()){
            chatId = update.getMessage().getChatId();
            text = update.getMessage().getContact().getPhoneNumber();
            tgId = update.getMessage().getFrom().getId();

            scenario = scenarioMap.getOrDefault(chatId, Scenario.NOTHING);

            if (scenario.equals(Scenario.REGISTRATION)) statusMap.put(chatId, Status.TRANSFER_SUCCESSFUL_REGISTRATION);
            else if (scenario.equals(Scenario.EDIT)) statusMap.put(chatId, Status.EDIT_PHONE_NUMBER);
        }

        status = statusMap.getOrDefault(chatId, Status.DEFAULT);

        switch (status){
            case WAITING_FOR_AMOUNT -> transactionService.amountInputMessage(chatId, statusMap);
            case WAITING_FOR_NUMBER -> transactionService.numberInputMessage(chatId, text, statusMap);
            case WAITING_FOR_BANK -> transactionService.bankInputMessage(chatId, text, statusMap);
            case TRANSFER_LINK -> transactionService.transferLinkMessage(chatId, text, statusMap);
            case MAKE_TRANSACTION -> transactionService.makeTransaction(
                    chatId,
                    statusMap,
                    scenarioMap,
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
                    scenarioMap,
                    String.valueOf(tgId)
            );



            case EDIT -> generalMessageService.editMessage(chatId, statusMap);
            case EDIT_DATA -> editUserService.editData(chatId, text, statusMap, scenarioMap);
            case EDIT_FIRST_NAME -> editUserService.editFirstName(chatId, text, String.valueOf(tgId), statusMap, scenarioMap);
            case EDIT_LAST_NAME -> editUserService.editLastName(chatId, text, String.valueOf(tgId), statusMap, scenarioMap);
            case EDIT_PATRONYMIC -> editUserService.editPatronymic(chatId, text, String.valueOf(tgId), statusMap, scenarioMap);
            case EDIT_PHONE_NUMBER -> editUserService.editPhoneNumber(chatId, text, String.valueOf(tgId), statusMap, scenarioMap);



            case DELETE -> deleteService.ifDelete(chatId, statusMap);
            case DELETE_ACTION -> deleteService.delete(
                    chatId,
                    statusMap,
                    scenarioMap,
                    text,
                    String.valueOf(tgId)
            );

            case WAITING -> generalMessageService.waitMessage(chatId);
        }
    }
}
