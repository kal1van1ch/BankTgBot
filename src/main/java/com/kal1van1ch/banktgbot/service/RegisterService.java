package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.model.dto.UserDto;
import com.kal1van1ch.banktgbot.model.entity.User;
import com.kal1van1ch.banktgbot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegisterService {
    private final UserRepository userRepository;
    private final Map<Long, UserDto> userRegisterData = new ConcurrentHashMap<>();
    private final GeneralService generalService;

    public RegisterService(
            UserRepository userRepository,
            GeneralService generalService
    ){
        this.userRepository = userRepository;
        this.generalService = generalService;
    }

    public boolean isRegistered(String tgId){
        User user = userRepository.containsTgId(tgId);
        return user != null;
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
        UserDto user = userRegisterData.get(chatId);
        user.setPatronymic(text);

        String message = "Введите свой номер телефона в формате \"9161112233\"";
        generalService.sendMessage(chatId, message);
        statusMap.put(chatId, Status.TRANSFER_SUCCESSFUL_REGISTRATION);
    }

    public void transferSuccessfulRegistrationMessage(
            Long chatId,
            String text,
            Map<Long, Status> statusMap,
            String tgId
    ){
        UserDto user = userRegisterData.get(chatId);
        user.setPhoneNumber(text);
        user.setTgId(tgId);

        StringBuilder sb = new StringBuilder("Регистрация прошла успешно, ваши данные:\n");
        String data = String.format(
                """
                Имя: %s
                Фамилия: %s
                Отчество: %s
                Номер телефона: %s
                """,
                user.getFirstName(),
                user.getLastName(),
                user.getPatronymic(),
                user.getPhoneNumber()
        );

        sb.append(data);

        generalService.sendMessage(chatId, sb.toString());
        statusMap.put(chatId, Status.DEFAULT);
    }
}
