package com.kal1van1ch.banktgbot.service;

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
public class RegisterService {
    private final UserRepository userRepository;
    private final Map<Long, UserDto> userRegisterData = new ConcurrentHashMap<>();
    private final GeneralService generalService;
    private final UserMapper userMapper;
    private final UserValidation userValidation;
    private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);

    public RegisterService(
            UserRepository userRepository,
            GeneralService generalService,
            UserMapper userMapper,
            UserValidation userValidation
    ){
        this.userRepository = userRepository;
        this.generalService = generalService;
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
        generalService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_LAST_NAME);
    }

    public void registerLastName(
            Long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        if (!userValidation.isValidFirstName(text)){
            generalService.sendMessage(chatId, "Имя введено неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл имя в неверном формате", chatId);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setFirstName(text);

        String message = "Введите свою фамилию";
        generalService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_PATRONYMIC);
    }

    public void registerPatronymic(
            Long chatId,
            String text,
            Map<Long, Status> statusMap
    ){

        if(!userValidation.isValidLastName(text)){
            generalService.sendMessage(chatId, "Фамилия введена неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл фамилию в неверном формате", chatId);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setLastName(text);

        String message = "Введите своё отчество (при отсутствии введите \"-\")";
        generalService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.WAITING_PHONE_NUMBER);
    }

    public void registerPhoneNumber(
            Long chatId,
            String text,
            Map<Long, Status> statusMap
    ){

        if (!userValidation.isValidPatronymic(text)){
            generalService.sendMessage(chatId, "Отчество введено в неверном формате, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл отчество в неверном формате", chatId);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setPatronymic(text);

        String message = "Введите свой номер телефона в формате \"9161112233\"";
        generalService.sendMessage(chatId, message);
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
            generalService.sendMessage(chatId, "Телефон введён неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл номер телефона в неверном формате", chatId);
            return;
        }

        UserDto user = userRegisterData.get(chatId);
        user.setPhoneNumber(text);
        user.setTgId(tgId);

        User u = userMapper.toEntity(user);

        try{
            userRepository.save(u);

            generalService.sendMessage(chatId, "Регистрация прошла успешно");
            statusMap.put(chatId, Status.DEFAULT);

            userRegisterData.remove(chatId);
        }
        catch (DataIntegrityViolationException e){
            generalService.sendMessage(chatId, "Пользователь с таким id уже существует");
            logger.info("Уже зарегистрированный пользователь {} предпринял попытку зарегистрироваться вновь", tgId);
        }
        catch (DataAccessException e){
            generalService.sendMessage(chatId, "Не удалось зарегистрироваться. Попробуйте ещё раз позже");
            logger.info("Не удалось занести данные пользователя {} в БД", tgId);
        }
    }
}
