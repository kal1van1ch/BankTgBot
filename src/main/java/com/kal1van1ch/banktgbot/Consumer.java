package com.kal1van1ch.banktgbot;


import com.kal1van1ch.banktgbot.model.Command;
import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.service.GeneralService;
import com.kal1van1ch.banktgbot.service.RegisterService;
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
    private final GeneralService generalService;
    private final RegisterService registerService;

    public Consumer(
            TransferService transferService,
            GeneralService generalService,
            RegisterService registerService
    ) {
        this.transferService = transferService;
        this.generalService = generalService;
        this.registerService = registerService;
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

            if (text.equals(Command.START.getCommand())) {

                if (!registerService.isRegistered(
                        String.valueOf(update
                                        .getMessage()
                                        .getFrom()
                                        .getId())
                )){
                    statusMap.put(chatId, Status.WAITING_FIRST_NAME);
                }
                else{
                    statusMap.put(chatId, Status.WAITING_FOR_AMOUNT);
                }
                status = statusMap.get(chatId);
            }

            else if (status == Status.DEFAULT) generalService.unknownMessage(chatId);

            switch (status){
                case WAITING_FOR_AMOUNT -> transferService.amountInputMessage(chatId, statusMap);
                case WAITING_FOR_NUMBER -> transferService.numberInputMessage(chatId, text, statusMap);
                case WAITING_FOR_BANK -> transferService.bankInputMessage(chatId, text, statusMap);
                case TRANSFER_LINK -> transferService.transferLinkMessage(chatId, text, statusMap);
                case WAITING_FIRST_NAME -> registerService.registerFirstName(chatId, statusMap);
                case WAITING_LAST_NAME -> registerService.registerLastName(chatId, text, statusMap);
                case WAITING_PATRONYMIC -> registerService.registerPatronymic(chatId, text, statusMap);
                case WAITING_PHONE_NUMBER -> registerService.registerPhoneNumber(chatId, text, statusMap);
                case TRANSFER_SUCCESSFUL_REGISTRATION -> registerService.transferSuccessfulRegistrationMessage(
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
