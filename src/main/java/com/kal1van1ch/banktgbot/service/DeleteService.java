package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.model.entity.User;
import com.kal1van1ch.banktgbot.repository.TransactionRepository;
import com.kal1van1ch.banktgbot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;
import java.util.Map;

@Service
public class DeleteService {

    private final GeneralMessageService generalMessageService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final static Logger logger = LoggerFactory.getLogger(DeleteService.class);

    public DeleteService(
            GeneralMessageService generalMessageService,
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ){
        this.generalMessageService = generalMessageService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public void ifDelete(
            long chatId,
            Map<Long, Status> statusMap
    ){
        String message = "Вы уверены, что хотите удалить свой аккаунт? При удалении вся история транзакций будет безвозвратно удалена";

        InlineKeyboardButton but1 = generalMessageService.createButton("Да", "YES");
        InlineKeyboardButton but2 = generalMessageService.createButton("Нет", "NO");

        List<InlineKeyboardRow> keyboardRows = List.of(
                new InlineKeyboardRow(but1),
                new InlineKeyboardRow(but2)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);

        generalMessageService.sendInlineButtonMessage(
                chatId,
                message,
                markup
        );

        statusMap.put(chatId, Status.DELETE_ACTION);
    }

    @Transactional
    public void delete(
            long chatId,
            Map<Long, Status> statusMap,
            String text,
            String tgId
    ){
        if (text.equals("NO")){
            generalMessageService.sendMessage(chatId, "Удаление аккаунта отменено");
        }
        else{
            try{
                User user = userRepository.getByTgId(tgId);
                transactionRepository.deleteByUser(user);
                userRepository.delete(user);

                generalMessageService.sendMessage(chatId, "Аккаунт был успешно удалён");
            }
            catch (Exception e){
                logger.error("При попытке удалить пользователя с tgId {} произошла внутренняя ошибка", tgId, e);
                generalMessageService.sendMessage(chatId, "Произошла внутрення ошибка");
            }
        }
        statusMap.put(chatId, Status.DEFAULT);
    }
}
