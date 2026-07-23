package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.error.SHA256Exception;
import com.kal1van1ch.banktgbot.mapper.UserMapper;
import com.kal1van1ch.banktgbot.model.Scenario;
import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.model.dto.UserDto;
import com.kal1van1ch.banktgbot.model.entity.User;
import com.kal1van1ch.banktgbot.repository.UserRepository;
import com.kal1van1ch.banktgbot.validation.UserValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final Map<Long, UserDto> userRegisterData = new ConcurrentHashMap<>();
    private final GeneralMessageService generalMessageService;
    private final UserMapper userMapper;
    private final UserValidation userValidation;
    private final static Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(
            UserRepository userRepository,
            GeneralMessageService generalMessageService,
            UserMapper userMapper,
            UserValidation userValidation
    ){
        this.userRepository = userRepository;
        this.generalMessageService = generalMessageService;
        this.userMapper = userMapper;
        this.userValidation = userValidation;
    }

    public boolean isRegistered(String tgId){
        return userRepository.containsTgId(tgId);
    }

    public void registerFirstName(
            long chatId,
            Map<Long, Status> statusMap
    ){
        userRegisterData.put(chatId, new UserDto());
        String message = "Введите своё имя";
        generalMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_LAST_NAME);
    }

    public void registerLastName(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        if (!userValidation.isValidFirstName(text)){
            generalMessageService.sendMessage(chatId, "Имя введено неверно, попробуйте ещё раз");
            logger.info("""
                    \n
                    ====================================================================================================
                    Пользователь ввёл имя в неверном формате
                    Чат: {}
                    Введённое имя: {}
                    ====================================================================================================
                    \n
                    """, chatId, text);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setFirstName(text);

        String message = "Введите свою фамилию";
        generalMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_PATRONYMIC);
    }

    public void registerPatronymic(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){

        if(!userValidation.isValidLastName(text)){
            generalMessageService.sendMessage(chatId, "Фамилия введена неверно, попробуйте ещё раз");
            logger.info("""
                    \n
                    ====================================================================================================
                    Пользователь ввёл фамилию в неверном формате
                    Чат: {}
                    Введённая фамилия: {}
                    ====================================================================================================
                    \n
                    """, chatId, text);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setLastName(text);

        String message = "Введите своё отчество (при отсутствии введите \"-\")";
        generalMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_PHONE_NUMBER);
    }

    public void registerPhoneNumber(
            long chatId,
            String text,
            Map<Long, Status> statusMap
    ){

        if (!userValidation.isValidPatronymic(text)){
            generalMessageService.sendMessage(chatId, "Отчество введено в неверном формате, попробуйте ещё раз");
            logger.info("""
                    \n
                    ====================================================================================================
                    Пользователь ввёл отчество в неверном формате
                    Чат: {}
                    Введённое отчество: {}
                    ====================================================================================================
                    \n
                    """, chatId, text);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setPatronymic(text);

        KeyboardButton contactButton = KeyboardButton.builder()
                .text("Отправить номер телефона")
                .requestContact(true)
                .build();

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(contactButton)))
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .build();

        generalMessageService.sendButtonMessage(
                chatId,
                "Нажмите кнопку ниже, чтобы отправить номер телефона",
                keyboardMarkup
        );
        statusMap.put(chatId, Status.WAITING);
    }

    @Transactional
    public void transferSuccessfulRegistrationMessage(
            long chatId,
            String text,
            Map<Long, Status> statusMap,
            Map<Long, Scenario> scenarioMap,
            String tgId
    ){
        try{
            UserDto user = userRegisterData.get(chatId);
            String newPhoneNumber = generalMessageService.encodeDataToSha256(text);
            user.setPhoneNumber(newPhoneNumber);
            user.setTgId(tgId);

            User u = userMapper.toEntity(user);
            userRepository.save(u);

            generalMessageService.removeKeyboard(chatId, "Регистрация прошла успешно");

            userRegisterData.remove(chatId);
        }
        catch (SHA256Exception e){
            logger.error("""
                    \n
                    ====================================================================================================
                    Ошибка при хэшировании данных при помощи алгоритма SHA-256
                    Чат: {}
                    ====================================================================================================
                    \n
                    """, chatId, e);
            generalMessageService.sendMessage(chatId, "Внутренняя ошибка, попробуйте снова позже");
        }
        catch (DataIntegrityViolationException e){
            generalMessageService.sendMessage(chatId, "Пользователь с таким id или номеров телефона уже существует");
            logger.error("""
                    \n
                    ====================================================================================================
                    Уже зарегистрированный пользователь предпринял попытку зарегистрироваться вновь
                    Пользователь: {}
                    ====================================================================================================
                    \n
                    """, tgId, e);
        }
        catch (DataAccessException e){
            generalMessageService.sendMessage(chatId, "Не удалось зарегистрироваться. Попробуйте ещё раз позже");
            logger.error("""
                    \n
                    ====================================================================================================
                    Не удалось занести данные пользователя в БД
                    Чат: {}
                    ====================================================================================================
                    \n
                    """, tgId, e);
        }
        statusMap.put(chatId, Status.DEFAULT);
        scenarioMap.put(chatId, Scenario.NOTHING);
    }
}
