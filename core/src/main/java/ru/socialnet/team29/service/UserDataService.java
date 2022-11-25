package ru.socialnet.team29.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.socialnet.team29.answers.MessageAnswer;
import ru.socialnet.team29.answers.NegativeResponseUserRegister;
import ru.socialnet.team29.answers.ResponseUserRegister;
import ru.socialnet.team29.answers_interface.CommonAnswer;
import ru.socialnet.team29.model.Person;
import ru.socialnet.team29.payloads.ContactConfirmationPayload;
import ru.socialnet.team29.serviceInterface.feign.DBConnectionFeignInterface;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDataService {

    private final DBConnectionFeignInterface feignInterface;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setPasswordEncoder(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public CommonAnswer saveNewUserInDb(ContactConfirmationPayload payload) {
        if (!payload.getPasswd1().equals(payload.getPasswd2()) | payload.getCode().isBlank()) {
            return getAnswer("invalid_request", "Поля пароль и подтверждение пароля не совпадают, или не заполнено поле code.");
        } else {
            Person person = Person.builder()
                    .firstName(payload.getFirstName())
                    .lastName(payload.getLastName())
                    .password(payload.getPasswd1())
                    .build();
            try {
                feignInterface.savePerson(person);
            } catch (Exception e) {
                return getAnswer("invalid_request", "Во время сохранения произошла ошибка.");
            }
            return getAnswer("", "");
        }
    }

    private CommonAnswer getAnswer(String error, String description) {
        if (!error.isBlank()) {
            NegativeResponseUserRegister negativeResponseUserRegister = new NegativeResponseUserRegister();
            negativeResponseUserRegister.setErrorDescription(description);
            negativeResponseUserRegister.setError("invalid_request");
            return negativeResponseUserRegister;
        }
        ResponseUserRegister responseUserRegister = new ResponseUserRegister();
        responseUserRegister.setTimestamp(System.currentTimeMillis());
        responseUserRegister.setMessageAnswer(new MessageAnswer("ok"));
        return responseUserRegister;
    }

    public Person getPersonByEmail(String email) throws IOException {
//        log.info(this.getClass().getSimpleName() + ": " + "Отправили запрос на БД. Для получения информации о person" + email);
        Person result = feignInterface.getPersonByEmail(email);
//        log.info(this.getClass().getSimpleName() + ": " + "Получили персон из БД." + email);
        result.setPassword(passwordEncoder.encode(result.getPassword()));//TODO Убрать, когда в базу данных будут сохраняться пароли с BcryptEncoder
        return result;
    }
}
