package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.model.entity.User;
import com.kal1van1ch.banktgbot.repository.UserRepository;
import com.kal1van1ch.banktgbot.validation.UserValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class EditUserService {
    private final SendMessageService sendMessageService;
    private final UserValidation userValidation;
    private final static Logger logger = LoggerFactory.getLogger(EditUserService.class);
    private final UserRepository userRepository;

    public EditUserService(
            SendMessageService sendMessageService,
            UserValidation userValidation,
            UserRepository userRepository
    ){
        this.sendMessageService = sendMessageService;
        this.userValidation = userValidation;
        this.userRepository = userRepository;
    }

    public void editData(
            Long chatId,
            String text,
            Map<Long, Status> statusMap
    ){
        switch (text){
            case "FIRST_NAME" -> {
                sendMessageService.sendMessage(chatId, "Введите новое имя");
                statusMap.put(chatId, Status.EDIT_FIRST_NAME);
            }
            case "LAST_NAME" -> {
                sendMessageService.sendMessage(chatId, "Введите новую фамилию");
                statusMap.put(chatId, Status.EDIT_LAST_NAME);
            }
            case "PATRONYMIC" -> {
                sendMessageService.sendMessage(chatId, "Введите новое отчество");
                statusMap.put(chatId, Status.EDIT_PATRONYMIC);
            }
            case "PHONE_NUMBER" -> {
                sendMessageService.sendMessage(chatId, "Введите новый номер телефона");
                statusMap.put(chatId, Status.EDIT_PHONE_NUMBER);
            }
            case "NOTHING" -> {
                sendMessageService.sendMessage(chatId, "Редактирование данных отменено");
                statusMap.put(chatId, Status.DEFAULT);
            }
        }
    }

    @Transactional
    public void editFirstName(
            long chatId,
            String text,
            String tgId,
            Map<Long, Status> statusMap
    ){
        if (!userValidation.isValidFirstName(text)){
            sendMessageService.sendMessage(chatId, "Имя введено неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл имя в неверном формате", chatId);
            return;
        }

        try{
            User user = userRepository.getByTgId(tgId);
            user.setFirstName(text);
            userRepository.flush();
            sendMessageService.sendMessage(chatId, "Имя успешно изменено");
        }
        catch (DataIntegrityViolationException e){
            logger.error("Ошибка при обновлении имени пользователя с tgId {}", tgId, e);
            sendMessageService.sendMessage(chatId, "Ошибка при обновлении имени, попробуйте в другой раз");
        }
        catch (Exception e) {
            logger.error("Критическая ошибка базы данных: ", e);
            sendMessageService.sendMessage(chatId, "Сервис временно недоступен. Попробуйте позже");
        }
        statusMap.put(chatId, Status.DEFAULT);
        sendMessageService.editMessage(chatId, statusMap);
    }

    @Transactional
    public void editLastName(
            long chatId,
            String text,
            String tgId,
            Map<Long, Status> statusMap
    ){
        if (!userValidation.isValidLastName(text)){
            sendMessageService.sendMessage(chatId, "Фамилия введена неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл фамилию в неверном формате", chatId);
            return;
        }
        try{
            User user = userRepository.getByTgId(tgId);
            user.setLastName(text);
            userRepository.flush();
            sendMessageService.sendMessage(chatId, "Фамилия успешно изменена");
        }
        catch (DataIntegrityViolationException e){
            logger.error("Ошибка при обновлении фамилии пользователя с tgIg {}", tgId, e);
            sendMessageService.sendMessage(chatId, "Ошибка при обновлении фамилии, попробуйте в другой раз");
        }
        catch (Exception e) {
            logger.error("Критическая ошибка базы данных: ", e);
            sendMessageService.sendMessage(chatId, "Сервис временно недоступен. Попробуйте позже");
        }
        statusMap.put(chatId, Status.DEFAULT);
        sendMessageService.editMessage(chatId, statusMap);
    }

    @Transactional
    public void editPatronymic(
            long chatId,
            String text,
            String tgId,
            Map<Long, Status> statusMap
    ){
        if (!userValidation.isValidPatronymic(text)){
            sendMessageService.sendMessage(chatId, "Отчество введено неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл отчество в неверном формате", chatId);
            return;
        }

        try{
            User user = userRepository.getByTgId(tgId);
            user.setPatronymic(text);
            userRepository.flush();
            sendMessageService.sendMessage(chatId, "Отчество успешно изменено");
        }
        catch (DataIntegrityViolationException e){
            logger.error("Ошибка при обновлении отчества пользователя с tgIg {}", tgId, e);
            sendMessageService.sendMessage(chatId, "Ошибка при обновлении отчества, попробуйте в другой раз");
        }
        catch (Exception e) {
            logger.error("Критическая ошибка базы данных: ", e);
            sendMessageService.sendMessage(chatId, "Сервис временно недоступен. Попробуйте позже");
        }
        statusMap.put(chatId, Status.DEFAULT);
        sendMessageService.editMessage(chatId, statusMap);
    }

    @Transactional
    public void editPhoneNumber(
            long chatId,
            String text,
            String tgId,
            Map<Long, Status> statusMap
    ){
        if (!userValidation.isValidPhoneNumber(text)){
            sendMessageService.sendMessage(chatId, "Номер телефона введён неверно, попробуйте ещё раз");
            logger.info("Пользователь из чата {} ввёл номер телефона в неверном формате", chatId);
            return;
        }

        try{
            User user = userRepository.getByTgId(tgId);

            String newPhoneNumber = sendMessageService.encodeDataToSha256(text);

            user.setPhoneNumber(newPhoneNumber);
            userRepository.flush();

            sendMessageService.sendMessage(chatId, "Номер телефона успешно изменён");
        }
        catch (DataIntegrityViolationException e){
            logger.error("Ошибка при обновлении номера телефона пользователя с tgIg {}", tgId, e);
            sendMessageService.sendMessage(chatId, "Ошибка при обновлении номера телефона, попробуйте в другой раз");
        }
        catch (Exception e) {
            logger.error("Критическая ошибка базы данных: ", e);
            sendMessageService.sendMessage(chatId, "Сервис временно недоступен. Попробуйте позже");
        }
        statusMap.put(chatId, Status.DEFAULT);
        sendMessageService.editMessage(chatId, statusMap);
    }
}
