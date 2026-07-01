package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.error.SHA256Exception;
import com.kal1van1ch.banktgbot.mapper.UserMapper;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final Map<Long, UserDto> userRegisterData = new ConcurrentHashMap<>();
    private final SendMessageService sendMessageService;
    private final UserMapper userMapper;
    private final UserValidation userValidation;
    private final static Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(
            UserRepository userRepository,
            SendMessageService sendMessageService,
            UserMapper userMapper,
            UserValidation userValidation
    ){
        this.userRepository = userRepository;
        this.sendMessageService = sendMessageService;
        this.userMapper = userMapper;
        this.userValidation = userValidation;
    }

    public boolean isRegistered(String tgId){
        return userRepository.containsTgId(tgId);
    }

    public void registerFirstName(
            Long chatId,
            Map<Long, Status> statusMap
    ){
        userRegisterData.put(chatId, new UserDto());
        String message = "Введите своё имя";
        sendMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_LAST_NAME);
    }

    public void registerLastName(
            Long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        if (!userValidation.isValidFirstName(text)){
            sendMessageService.sendMessage(chatId, "Имя введено неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл имя в неверном формате", chatId);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setFirstName(text);

        String message = "Введите свою фамилию";
        sendMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_PATRONYMIC);
    }

    public void registerPatronymic(
            Long chatId,
            String text,
            Map<Long, Status> statusMap
    ){

        if(!userValidation.isValidLastName(text)){
            sendMessageService.sendMessage(chatId, "Фамилия введена неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл фамилию в неверном формате", chatId);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setLastName(text);

        String message = "Введите своё отчество (при отсутствии введите \"-\")";
        sendMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_PHONE_NUMBER);
    }

    public void registerPhoneNumber(
            Long chatId,
            String text,
            Map<Long, Status> statusMap
    ){

        if (!userValidation.isValidPatronymic(text)){
            sendMessageService.sendMessage(chatId, "Отчество введено в неверном формате, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл отчество в неверном формате", chatId);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setPatronymic(text);

        String message = "Введите свой номер телефона в формате \"9161112233\"";
        sendMessageService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.TRANSFER_SUCCESSFUL_REGISTRATION);
    }

    @Transactional
    public void transferSuccessfulRegistrationMessage(
            Long chatId,
            String text,
            Map<Long, Status> statusMap,
            String tgId
    ){

        if (!userValidation.isValidPhoneNumber(text)){
            sendMessageService.sendMessage(chatId, "Телефон введён неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл номер телефона в неверном формате", chatId);
            return;
        }


        try{
            UserDto user = userRegisterData.get(chatId);
            String newPhoneNumber = sendMessageService.encodeDataToSha256(text);
            user.setPhoneNumber(newPhoneNumber);
            user.setTgId(tgId);

            User u = userMapper.toEntity(user);
            userRepository.save(u);

            sendMessageService.sendMessage(chatId, "Регистрация прошла успешно");
            statusMap.put(chatId, Status.DEFAULT);

            userRegisterData.remove(chatId);
        }
        catch (SHA256Exception e){
            logger.info("Ошибка при хэшировании данных в чате {} SHA-256", chatId, e);
            sendMessageService.sendMessage(chatId, "Внутренняя ошибка, попробуйте снова позже");
        }
        catch (DataIntegrityViolationException e){
            sendMessageService.sendMessage(chatId, "Пользователь с таким id или номеров телефона уже существует");
            logger.info("Уже зарегистрированный пользователь {} предпринял попытку зарегистрироваться вновь", tgId);
        }
        catch (DataAccessException e){
            sendMessageService.sendMessage(chatId, "Не удалось зарегистрироваться. Попробуйте ещё раз позже");
            logger.info("Не удалось занести данные пользователя {} в БД", tgId);
        }
    }
}
