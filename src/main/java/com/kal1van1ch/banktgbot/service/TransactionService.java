package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.error.SHA256Exception;
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
    private final GeneralMessageService generalMessageService;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionValidation transactionValidation;
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(
            GeneralMessageService generalMessageService,
            TransactionMapper transactionMapper,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            TransactionValidation transactionValidation
    ){
        this.generalMessageService = generalMessageService;
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
        generalMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_FOR_NUMBER);
    }

    public void numberInputMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){

        if (!transactionValidation.isValidAmount(text)){
            generalMessageService.sendMessage(chatId, "Сумма введена некорректно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл сумму не в том формате", chatId);
            return;
        }

        TransactionDto t = inputData.get(chatId);
        t.setAmount(Long.parseLong(text));

        String message = "Введите номер телефона для перевода в формате \"9161112233\"";
        generalMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_FOR_BANK);
    }

    public void bankInputMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ) {

        if (!transactionValidation.isValidPhoneNumber(text)){
            generalMessageService.sendMessage(chatId, "Телефон введён неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл номер телефона не в том формате", chatId);
            return;
        }

        try{
            String encodedPhoneNumber = generalMessageService.encodeDataToSha256(text);
            TransactionDto t = inputData.get(chatId);
            t.setPhoneNumber(encodedPhoneNumber);

            String message = "Выберите банк, с которого будет совершён перевод";

            InlineKeyboardButton but1 = generalMessageService.createButtonWithCallbackData(Bank.TBANK.getName(), Bank.TBANK.getCallbackData());
            InlineKeyboardButton but2 = generalMessageService.createButtonWithCallbackData(Bank.SBERBANK.getName(), Bank.SBERBANK.getCallbackData());
            InlineKeyboardButton but3 = generalMessageService.createButtonWithCallbackData(Bank.ALFABANK.getName(), Bank.ALFABANK.getCallbackData());
            InlineKeyboardButton but4 = generalMessageService.createButtonWithCallbackData(Bank.GAZPROMBANK.getName(), Bank.GAZPROMBANK.getCallbackData());

            List<InlineKeyboardRow> keyboardRows = List.of(
                    new InlineKeyboardRow(but1),
                    new InlineKeyboardRow(but2),
                    new InlineKeyboardRow(but3),
                    new InlineKeyboardRow(but4)
            );

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);

            generalMessageService.sendInlineButtonMessage(
                    chatId,
                    message,
                    markup
            );

            statusMap.put(chatId, Status.WAITING);
        }

        catch (SHA256Exception e){
            logger.info("Ошибка при хэшировании данных в чате {} SHA-256", chatId, e);
            generalMessageService.sendMessage(chatId, "Внутренняя ошибка, попробуйте снова позже");
        }
    }

    @Transactional
    public void transferLinkMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        TransactionDto t = inputData.get(chatId);

        switch (text){
            case "TBANK" -> t.setBankFrom(Bank.TBANK);
            case "SBER" -> t.setBankFrom(Bank.SBERBANK);
            case "ALFA" -> t.setBankFrom(Bank.ALFABANK);
            case "GPB" -> t.setBankFrom(Bank.GAZPROMBANK);
        }

        String message = "Нажмите на кнопку для перевода";

        /*
         TODO: В текущей реализации используется заглушка для совершения транзакции.
          Прямой вызов банковских deeplink-ссылок (tbank:// и др.) блокируется Telegram API
          в целях безопасности. Использование промежуточной Web-страницы (через ngrok)
          также ограничено: попытки автоматического перехода в банковское приложение
          блокируются системой безопасности мобильной ОС (протестировано на Android).

         Ожидаемая реализация: здесь должен быть реализован надежный механизм перевода
         средств, учитывающий ограничения мобильных сред выполнения.
         */
        InlineKeyboardButton but1 = generalMessageService.createButtonWithCallbackData("Совершить перевод", "MAKE_TRANSACTION");

        List<InlineKeyboardRow> keyboardRows = List.of(
                new InlineKeyboardRow(but1)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);

        generalMessageService.sendInlineButtonMessage(
                chatId,
                message,
                markup
        );

        statusMap.put(chatId, Status.WAITING);
    }

    public void makeTransaction(
            long chatId,
            Map<Long, Status> statusMap,
            String tgId
    ){
        TransactionDto t = inputData.get(chatId);
        User u = userRepository.getByTgId(tgId);
        t.setUser(u);

        try{
            t.setDate(LocalDateTime.now());
            Transaction transaction = transactionMapper.toEntity(t);

            transactionRepository.save(transaction);

            generalMessageService.sendMessage(chatId, "Транзакция прошла успешно");


            inputData.remove(chatId);
        }
        catch (DataAccessException e){
            generalMessageService.sendMessage(chatId, "Не удалось сохранить историю транзакции.");
            logger.info("Не удалось занести данные о транзакции пользователя {} в БД", tgId);
        }
        statusMap.put(chatId, Status.DEFAULT);
    }
}
