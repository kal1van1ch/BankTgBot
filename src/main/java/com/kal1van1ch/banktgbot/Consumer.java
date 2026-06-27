package com.kal1van1ch.banktgbot;


import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.service.ConsumerService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Consumer implements LongPollingSingleThreadUpdateConsumer {

    private final Map<Long, Status> statusMap = new ConcurrentHashMap<>();
    private final ConsumerService consumerService;

    public Consumer(ConsumerService consumerService) {
        this.consumerService = consumerService;
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

            else if (!text.equals("/start") && status == Status.DEFAULT) consumerService.unknownMessage(chatId);

            switch (status){
                case WAITING_FOR_AMOUNT -> consumerService.amountInputMessage(chatId, statusMap);
                case WAITING_FOR_NUMBER -> consumerService.numberInputMessage(chatId, text, statusMap);
                case WAITING_FOR_BANK -> consumerService.bankInputMessage(chatId, text, statusMap);
                case TRANSFER_LINK -> consumerService.transferLinkHandler(chatId, text, statusMap);
            }
        }
    }
}
