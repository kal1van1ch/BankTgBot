package com.kal1van1ch.banktgbot.service;

import com.kal1van1ch.banktgbot.error.SHA256Exception;
import com.kal1van1ch.banktgbot.mapper.UserMapper;
import com.kal1van1ch.banktgbot.model.Scenario;
import com.kal1van1ch.banktgbot.model.Status;
import com.kal1van1ch.banktgbot.model.dto.UserDto;
import com.kal1van1ch.banktgbot.model.entity.User;
import com.kal1van1ch.banktgbot.repository.UserRepository;
import com.kal1van1ch.banktgbot.validation.UserValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private GeneralMessageService generalMessageService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserValidation userValidation;

    @InjectMocks
    private UserService userService;

    private Map<Long, Status> statusMap;
    private Map<Long, Scenario> scenarioMap;

    @BeforeEach
    public void setUp(){
        statusMap = new ConcurrentHashMap<>();
    }

    @Test
    public void isRegisteredTest(){
        String tgId = "1234567890";

        when(userRepository.containsTgId(tgId)).thenReturn(true);
        boolean res = userService.isRegistered(tgId);

        assertTrue(res);
        verify(userRepository, times(1)).containsTgId(tgId);
    }

    @Test
    public void notIsRegisteredTest(){
        String tgId = "983753941";

        when(userRepository.containsTgId(tgId)).thenReturn(false);
        boolean res = userService.isRegistered(tgId);

        assertFalse(res);
        verify(userRepository, times(1)).containsTgId(tgId);
    }

    @Test
    public void registerFirstNameTest(){
        long chatId = 12984120;

        userService.registerFirstName(chatId, statusMap);

        verify(generalMessageService, times(1))
                .sendMessage(eq(chatId), eq("Введите своё имя"));
        assertEquals(Status.WAITING_LAST_NAME, statusMap.get(chatId));
    }

    @Test
    public void registerLastNameWrongFirstName(){
        long chatId = 128420;
        String firstName = "34985hfdkasd";

        userService.registerLastName(chatId, firstName, statusMap);

        verify(generalMessageService, times(1))
                .sendMessage(eq(chatId), eq("Имя введено неверно, попробуйте ещё раз"));
    }

    @Test
    public void registerLastNameTest(){
        long chatId = 1298412;
        String firstName = "Обама";

        when(userValidation.isValidFirstName(firstName)).thenReturn(true);

        userService.registerFirstName(chatId, statusMap);
        userService.registerLastName(chatId, firstName, statusMap);

        verify(generalMessageService, times(1))
                .sendMessage(eq(chatId), eq("Введите свою фамилию"));
        assertEquals(Status.WAITING_PATRONYMIC, statusMap.get(chatId));
    }

    @Test
    public void registerPatronymicWrongLastName(){
        long chatId = 1228420;
        String lastName = "34985hfdkasd";

        userService.registerPatronymic(chatId, lastName, statusMap);

        verify(generalMessageService, times(1))
                .sendMessage(eq(chatId), eq("Фамилия введена неверно, попробуйте ещё раз"));
    }

    @Test
    public void registerPatronymicTest(){
        long chatId = 12983412;
        String firstName = "Обама";
        String lastName = "Барак";

        when(userValidation.isValidFirstName(firstName)).thenReturn(true);
        when(userValidation.isValidLastName(lastName)).thenReturn(true);

        userService.registerFirstName(chatId, statusMap);
        userService.registerLastName(chatId, firstName, statusMap);
        userService.registerPatronymic(chatId, lastName, statusMap);

        verify(generalMessageService, times(1))
                .sendMessage(eq(chatId), eq("Введите своё отчество (при отсутствии введите \"-\")"));
        assertEquals(Status.WAITING_PHONE_NUMBER, statusMap.get(chatId));
    }

    @Test
    public void registerPhoneNumberWrongPatronymic(){
        long chatId = 1228420;
        String patronymic = "34985hfdkasd";

        userService.registerPhoneNumber(chatId, patronymic, statusMap);

        verify(generalMessageService, times(1))
                .sendMessage(eq(chatId), eq("Отчество введено в неверном формате, попробуйте ещё раз"));
    }

    @Test
    public void registerPhoneNumberTest(){
        long chatId = 12983412;
        String firstName = "Обама";
        String lastName = "Барак";
        String patronymic = "Бомбииракович";

        when(userValidation.isValidFirstName(firstName)).thenReturn(true);
        when(userValidation.isValidLastName(lastName)).thenReturn(true);
        when(userValidation.isValidPatronymic(patronymic)).thenReturn(true);

        userService.registerFirstName(chatId, statusMap);
        userService.registerLastName(chatId, firstName, statusMap);
        userService.registerPatronymic(chatId, lastName, statusMap);
        userService.registerPhoneNumber(chatId, patronymic, statusMap);

        verify(generalMessageService, times(1))
                .sendButtonMessage(eq(chatId), eq("Нажмите кнопку ниже, чтобы отправить номер телефона"), any(ReplyKeyboardMarkup.class));
        assertEquals(Status.WAITING, statusMap.get(chatId));
    }

    @Test
    public void transferSuccessfulRegistrationMessageSHA256ExceptionTest(){
        long chatId = 12983412;
        String firstName = "Обама";
        String lastName = "Барак";
        String patronymic = "Бомбииракович";
        String phoneNumber = "79161112233";
        String tgId = "1234567890";
        scenarioMap = new ConcurrentHashMap<>();
        scenarioMap.put(chatId, Scenario.REGISTRATION);

        when(userValidation.isValidFirstName(firstName)).thenReturn(true);
        when(userValidation.isValidLastName(lastName)).thenReturn(true);
        when(userValidation.isValidPatronymic(patronymic)).thenReturn(true);
        when(generalMessageService.encodeDataToSha256(anyString()))
                .thenThrow(new SHA256Exception("Ошибка при попытке инициализировать алгоритм SHA-256", new NoSuchAlgorithmException()));

        userService.registerFirstName(chatId, statusMap);
        userService.registerLastName(chatId, firstName, statusMap);
        userService.registerPatronymic(chatId, lastName, statusMap);
        userService.registerPhoneNumber(chatId, patronymic, statusMap);

        userService.transferSuccessfulRegistrationMessage(
                chatId,
                phoneNumber,
                statusMap,
                scenarioMap,
                tgId
        );

        verify(generalMessageService, times(1))
                .sendMessage(eq(chatId), eq("Внутренняя ошибка, попробуйте снова позже"));

        assertEquals(Status.DEFAULT, statusMap.get(chatId));
        assertEquals(Scenario.NOTHING, scenarioMap.get(chatId));
    }

    @Test
    public void transferSuccessfulRegistrationMessageDataIntegrityViolationExceptionTest(){
        long chatId = 12983412;
        String firstName = "Обама";
        String lastName = "Барак";
        String patronymic = "Бомбииракович";
        String phoneNumber = "79161112233";
        String tgId = "1234567890";
        scenarioMap = new ConcurrentHashMap<>();
        scenarioMap.put(chatId, Scenario.REGISTRATION);

        when(userValidation.isValidFirstName(firstName)).thenReturn(true);
        when(userValidation.isValidLastName(lastName)).thenReturn(true);
        when(userValidation.isValidPatronymic(patronymic)).thenReturn(true);
        when(userMapper.toEntity(any(UserDto.class))).thenReturn(new User());
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Попытка дублирования уникальных данных"));

        userService.registerFirstName(chatId, statusMap);
        userService.registerLastName(chatId, firstName, statusMap);
        userService.registerPatronymic(chatId, lastName, statusMap);
        userService.registerPhoneNumber(chatId, patronymic, statusMap);

        userService.transferSuccessfulRegistrationMessage(
                chatId,
                phoneNumber,
                statusMap,
                scenarioMap,
                tgId
        );

        verify(generalMessageService, times(1))
                .sendMessage(eq(chatId), eq("Пользователь с таким id или номеров телефона уже существует"));

        assertEquals(Status.DEFAULT, statusMap.get(chatId));
        assertEquals(Scenario.NOTHING, scenarioMap.get(chatId));
    }

    @Test
    public void transferSuccessfulRegistrationMessageDataAccessExceptionTest(){
        long chatId = 12983412;
        String firstName = "Обама";
        String lastName = "Барак";
        String patronymic = "Бомбииракович";
        String phoneNumber = "79161112233";
        String tgId = "1234567890";
        scenarioMap = new ConcurrentHashMap<>();
        scenarioMap.put(chatId, Scenario.REGISTRATION);

        when(userValidation.isValidFirstName(firstName)).thenReturn(true);
        when(userValidation.isValidLastName(lastName)).thenReturn(true);
        when(userValidation.isValidPatronymic(patronymic)).thenReturn(true);
        when(userMapper.toEntity(any(UserDto.class))).thenReturn(new User());
        when(userRepository.save(any(User.class))).thenThrow(new QueryTimeoutException("Неудачная попытка зарегистрироваться, время ожидания БД истекло"));

        userService.registerFirstName(chatId, statusMap);
        userService.registerLastName(chatId, firstName, statusMap);
        userService.registerPatronymic(chatId, lastName, statusMap);
        userService.registerPhoneNumber(chatId, patronymic, statusMap);

        userService.transferSuccessfulRegistrationMessage(
                chatId,
                phoneNumber,
                statusMap,
                scenarioMap,
                tgId
        );

        verify(generalMessageService, times(1))
                .sendMessage(eq(chatId), eq("Не удалось зарегистрироваться. Попробуйте ещё раз позже"));

        assertEquals(Status.DEFAULT, statusMap.get(chatId));
        assertEquals(Scenario.NOTHING, scenarioMap.get(chatId));
    }

    @Test
    public void transferSuccessfulRegistrationMessageTest(){
        long chatId = 12983412;
        String firstName = "Обама";
        String lastName = "Барак";
        String patronymic = "Бомбииракович";
        String phoneNumber = "79161112233";
        String tgId = "1234567890";
        scenarioMap = new ConcurrentHashMap<>();
        scenarioMap.put(chatId, Scenario.REGISTRATION);

        when(userValidation.isValidFirstName(firstName)).thenReturn(true);
        when(userValidation.isValidLastName(lastName)).thenReturn(true);
        when(userValidation.isValidPatronymic(patronymic)).thenReturn(true);
        when(userMapper.toEntity(any(UserDto.class))).thenReturn(new User());

        userService.registerFirstName(chatId, statusMap);
        userService.registerLastName(chatId, firstName, statusMap);
        userService.registerPatronymic(chatId, lastName, statusMap);
        userService.registerPhoneNumber(chatId, patronymic, statusMap);

        userService.transferSuccessfulRegistrationMessage(
                chatId,
                phoneNumber,
                statusMap,
                scenarioMap,
                tgId
        );

        verify(userRepository, times(1)).save(any(User.class));
        verify(generalMessageService, times(1))
                .removeKeyboard(eq(chatId), eq("Регистрация прошла успешно"));

        assertEquals(Status.DEFAULT, statusMap.get(chatId));
        assertEquals(Scenario.NOTHING, scenarioMap.get(chatId));
    }
}
