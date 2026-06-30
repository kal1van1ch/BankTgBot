package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.mapper.TransactionMapper;
import com.kal1van1ch.banktgbot.model.Bank;
import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.model.dto.TransactionDto;
import com.kal1van1ch.banktgbot.model.entity.Transaction;
import com.kal1van1ch.banktgbot.model.entity.User;
import com.kal1van1ch.banktgbot.repository.TransactionRepository;
import com.kal1van1ch.banktgbot.repository.UserRepository;
import com.kal1van1ch.banktgbot.validation.TransactionValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransactionService {

    private final Map<Long, TransactionDto> inputData = new ConcurrentHashMap<>();
    private final SendMessageService sendMessageService;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionValidation transactionValidation;
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(
            SendMessageService sendMessageService,
            TransactionMapper transactionMapper,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            TransactionValidation transactionValidation
    ){
        this.sendMessageService = sendMessageService;
        this.transactionMapper = transactionMapper;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.transactionValidation = transactionValidation;
    }

    public void amountInputMessage(
            long chatId,
            Map<Long, Status> statusMap
    ){
        inputData.put(chatId, new TransactionDto());
        String message = "Введите сумму для перевода в формате \"100\"";
        sendMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_FOR_NUMBER);
    }

    public void numberInputMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){

        if (!transactionValidation.isValidAmount(text)){
            sendMessageService.sendMessage(chatId, "Сумма введена некорректно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл сумму не в том формате", chatId);
            return;
        }

        TransactionDto t = inputData.get(chatId);
        t.setAmount(Long.parseLong(text));

        String message = "Введите номер телефона для перевода в формате \"9161112233\"";
        sendMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_FOR_BANK);
    }

    public void bankInputMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){

        if (!transactionValidation.isValidPhoneNumber(text)){
            sendMessageService.sendMessage(chatId, "Телефон введён неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл номер телефона не в том формате", chatId);
            return;
        }

        TransactionDto t = inputData.get(chatId);
        t.setPhoneNumber(text);

        String message = "Выберите банк для перевода";

        InlineKeyboardButton but1 = InlineKeyboardButton
                .builder()
                .text("Тбанк")
                .callbackData("TBANK")
                .build();

        InlineKeyboardButton but2 = InlineKeyboardButton
                .builder()
                .text("Сбербанк")
                .callbackData("SBER")
                .build();

        InlineKeyboardButton but3 = InlineKeyboardButton
                .builder()
                .text("Альфа-банк")
                .callbackData("ALFA")
                .build();

        InlineKeyboardButton but4 = InlineKeyboardButton
                .builder()
                .text("Газпромбанк")
                .callbackData("GPB")
                .build();

        List<InlineKeyboardRow> keyboardRows = List.of(
                new InlineKeyboardRow(but1),
                new InlineKeyboardRow(but2),
                new InlineKeyboardRow(but3),
                new InlineKeyboardRow(but4)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);

        sendMessageService.sendInlineButtonMessage(
                chatId,
                message,
                markup
        );

        statusMap.put(chatId, Status.TRANSFER_LINK);
    }

    @Transactional
    public void transferLinkMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap,
            String tgId
    ){
        TransactionDto t = inputData.get(chatId);

        switch (text){
            case "TBANK" -> t.setBank(Bank.TBANK);
            case "SBER" -> t.setBank(Bank.SBERBANK);
            case "ALFA" -> t.setBank(Bank.ALFABANK);
            case "GPB" -> t.setBank(Bank.GAZPROMBANK);
        }

        t.setDate(LocalDateTime.now());

        User u = userRepository.getByTgId(tgId);
        t.setUser(u);

        Transaction transaction = transactionMapper.toEntity(t);
        try{
            transactionRepository.save(transaction);

            sendMessageService.sendMessage(chatId, "Транзакция прошла успешно");
            statusMap.put(chatId, Status.DEFAULT);

            inputData.remove(chatId);
        }
        catch (DataAccessException e){
            sendMessageService.sendMessage(chatId, "Не удалось сохранить историю транзакции.");
            logger.info("Не удалось занести данные о транзакции пользователя {} в БД", tgId);
        }
    }
}
