package com.example.studymatzip.services;


import com.example.studymatzip.entities.*;
import com.example.studymatzip.enums.*;
import com.example.studymatzip.mappers.UserMapper;
import com.example.studymatzip.utils.CryptoUtil;
import com.example.studymatzip.utils.NCloudUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Date;

@Service
public class UserService {
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine springTemplateEngine;
    private final UserMapper userMapper;

    @Autowired
    public UserService(JavaMailSender javaMailSender, SpringTemplateEngine springTemplateEngine, UserMapper userMapper) {
        this.javaMailSender = javaMailSender;
        this.springTemplateEngine = springTemplateEngine;
        this.userMapper = userMapper;
    }

    public SendRegisterContactCodeResult sendRegisterContactCode(RegisterContactCodeEntity registerContactCode) {
        if (registerContactCode == null ||
                registerContactCode.getContact() == null ||
                !registerContactCode.getContact().matches("^(010)(\\d{8})$")) {
            return SendRegisterContactCodeResult.FAILURE;
        }
        if (this.userMapper.selectUserByContact(registerContactCode.getContact()) != null) {
            return SendRegisterContactCodeResult.FAILURE_DUPLICATE;
        }
        String code = RandomStringUtils.randomNumeric(6);
        String salt = CryptoUtil.hashSha512(String.format("%s%s%f%f",
                registerContactCode.getContact(),
                code,
                Math.random(),
                Math.random()));
        Date createdAt = new Date();
        Date expiresAt = DateUtils.addMinutes(createdAt, 5);
        registerContactCode.setCode(code)
                .setSalt(salt)
                .setCreatedAt(createdAt)
                .setExpiresAt(expiresAt)
                .setExpired(false);
        NCloudUtil.sendSms(registerContactCode.getContact(), String.format("[맛집 회원가입] 인증번호 [%s]를 입력해 주세요.", registerContactCode.getCode()));
        return this.userMapper.insertRegisterContactCode(registerContactCode) > 0
                ? SendRegisterContactCodeResult.SUCCESS
                : SendRegisterContactCodeResult.FAILURE;
    }

    public VerifyRegisterContactCodeResult verifyRegisterContactCode(RegisterContactCodeEntity registerContactCode) {
        registerContactCode = this.userMapper.selectRegisterContactCodeByContactCodeSalt(registerContactCode);
        if (registerContactCode == null) {
            return VerifyRegisterContactCodeResult.FAILURE;
        }
        if (new Date().compareTo(registerContactCode.getExpiresAt()) > 0) {
            return VerifyRegisterContactCodeResult.FAILURE_EXPIRED;
        }
        registerContactCode.setExpired(true);
        return this.userMapper.updateRegisterContactCode(registerContactCode) > 0
                ? VerifyRegisterContactCodeResult.SUCCESS
                : VerifyRegisterContactCodeResult.FAILURE;
    }

    public CheckEmailResult checkEmail(String email) {
        return this.userMapper.selectUserByEmail(email) == null
                ? CheckEmailResult.OKAY
                : CheckEmailResult.DUPLICATE;
    }

    public CheckNicknameResult checkNickname(String nickname) {
        return this.userMapper.selectUserByNickname(nickname) == null
                ? CheckNicknameResult.OKAY
                : CheckNicknameResult.DUPLICATE;
    }

    public RegisterResult register(UserEntity user, RegisterContactCodeEntity registerContactCode) throws
            MessagingException {
        if (this.userMapper.selectUserByEmail(user.getEmail()) != null) {
            return RegisterResult.FAILURE_DUPLICATE_EMAIL;
        }
        if (this.userMapper.selectUserByContact(user.getContact()) != null) {
            return RegisterResult.FAILURE_DUPLICATE_CONTACT;
        }
        if (this.userMapper.selectUserByNickname(user.getNickname()) != null) {
            return RegisterResult.FAILURE_DUPLICATE_NICKNAME;
        }
        registerContactCode = this.userMapper.selectRegisterContactCodeByContactCodeSalt(registerContactCode);
        if (registerContactCode == null || !registerContactCode.isExpired()) {
            return RegisterResult.FAILURE;
        }
        user.setPassword(CryptoUtil.hashSha512(user.getPassword()));
        user.setStatus("EMAIL_PENDING");
        user.setAdmin(false);

        RegisterEmailCodeEntity registerEmailCode = new RegisterEmailCodeEntity();
        registerEmailCode.setEmail(user.getEmail());
        registerEmailCode.setCode(RandomStringUtils.randomAlphanumeric(6));
        registerEmailCode.setSalt(CryptoUtil.hashSha512(String.format("%s%s%f%f",
                registerEmailCode.getEmail(),
                registerEmailCode.getCode(),
                Math.random(),
                Math.random())));
        registerEmailCode.setCreatedAt(new Date());
        registerEmailCode.setExpiresAt(DateUtils.addHours(registerEmailCode.getCreatedAt(), 1));
        registerEmailCode.setExpired(false);

        String url = String.format("http://localhost:6795/user/emailCode?email=%s&code=%s&salt=%s",
                registerEmailCode.getEmail(),
                registerEmailCode.getCode(),
                registerEmailCode.getSalt());
        Context context = new Context();
        context.setVariable("url", url);

        MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        mimeMessageHelper.setSubject("[맛집 회원가입] 이메일 인증");
        mimeMessageHelper.setFrom("inst.yhp@gmail.com"); // 당신 이메일
        mimeMessageHelper.setTo(user.getEmail());
        mimeMessageHelper.setText(this.springTemplateEngine.process("_registerEmail", context), true);
        this.javaMailSender.send(mimeMessage);

        return this.userMapper.insertUser(user) > 0 && this.userMapper.insertRegisterEmailCode(registerEmailCode) > 0
                ? RegisterResult.SUCCESS
                : RegisterResult.FAILURE;
    }

    public LoginResult login(UserEntity user) {
        if (user.getEmail() == null ||
                user.getPassword() == null ||
                !user.getEmail().matches("^(?=.{10,50}$)([\\da-zA-Z\\-_.]{5,25})@([\\da-z][\\da-z\\-]*[\\da-z]\\.)?([\\da-z][\\da-z\\-]*[\\da-z])\\.([a-z]{2,15})(\\.[a-z]{2})?$") ||
                !user.getPassword().matches("^([\\da-zA-Z`~!@#$%^&*()\\-_=+\\[{\\]};:'\",<.>/?]{8,50})$")) {
            return LoginResult.FAILURE;
        }
        UserEntity existingUser = this.userMapper.selectUserByEmail(user.getEmail());
        if (existingUser == null) {
            return LoginResult.FAILURE;
        }
        user.setPassword(CryptoUtil.hashSha512(user.getPassword())); // test1234 -> hashed
        if (!user.getPassword().equals(existingUser.getPassword())) {
            return LoginResult.FAILURE;
        }

        user.setNickname(existingUser.getNickname())
                .setContact(existingUser.getContact())
                .setStatus(existingUser.getStatus())
                .setAdmin(existingUser.isAdmin())
                .setRegisteredAt(existingUser.getRegisteredAt());

        if (user.getStatus().equals("DELETED")) {
            return LoginResult.FAILURE;
        }
        if (user.getStatus().equals("EMAIL_PENDING")) {
            return LoginResult.FAILURE_EMAIL_NOT_VERIFIED;
        }
        if (user.getStatus().equals("SUSPENDED")) {
            return LoginResult.FAILURE_SUSPENDED;
        }
        return LoginResult.SUCCESS;
    }

    public VerifyRegisterEmailCodeResult verifyRegisterEmailCode(RegisterEmailCodeEntity registerEmailCode) {
        System.out.println(registerEmailCode.getCode().matches("^([\\da-z]{6})$"));
        if (registerEmailCode.getEmail() == null ||
                registerEmailCode.getCode() == null ||
                registerEmailCode.getSalt() == null ||
                !registerEmailCode.getEmail().matches("^(?=.{10,50}$)([\\da-zA-Z\\-_.]{5,25})@([\\da-z][\\da-z\\-]*[\\da-z]\\.)?([\\da-z][\\da-z\\-]*[\\da-z])\\.([a-z]{2,15})(\\.[a-z]{2})?$") ||
                !registerEmailCode.getCode().matches("^([\\da-zA-Z]{6})$") ||
                !registerEmailCode.getSalt().matches("^([\\da-f]{128})")) {
            return VerifyRegisterEmailCodeResult.FAILURE;
        }
        registerEmailCode = this.userMapper.selectRegisterEmailCodeByEmailCodeSalt(registerEmailCode);
        if (registerEmailCode == null) {
            return VerifyRegisterEmailCodeResult.FAILURE;
        }
        if (new Date().compareTo(registerEmailCode.getExpiresAt()) > 0) {
            return VerifyRegisterEmailCodeResult.FAILURE_EXPIRED;
        }
        registerEmailCode.setExpired(true);
        UserEntity user = this.userMapper.selectUserByEmail(registerEmailCode.getEmail());
        user.setStatus("OKAY");
        return this.userMapper.updateRegisterEmailCode(registerEmailCode) > 0 && this.userMapper.updateUser(user) > 0
                ? VerifyRegisterEmailCodeResult.SUCCESS
                : VerifyRegisterEmailCodeResult.FAILURE;
    }

    public SendRecoverContactCodeResult sendRecoverContactCode(RecoverContactCodeEntity recoverContactCode) {
        if (recoverContactCode == null ||
                recoverContactCode.getContact() == null ||
                !recoverContactCode.getContact().matches("^(010\\d{8})$")) {
            return SendRecoverContactCodeResult.FAILURE;
        }
        UserEntity existingUser = this.userMapper.selectUserByContact(recoverContactCode.getContact());
        if (existingUser == null) {
            return SendRecoverContactCodeResult.FAILURE;
        }
        recoverContactCode
                .setCode(RandomStringUtils.randomNumeric(6))
                .setSalt(CryptoUtil.hashSha512(String.format("%s%s%f%f",
                        recoverContactCode.getCode(),
                        recoverContactCode.getContact(),
                        Math.random(),
                        Math.random())))
                .setCreatedAt(new Date())
                .setExpiresAt(DateUtils.addMinutes(recoverContactCode.getCreatedAt(), 5))
                .setExpired(false);
        NCloudUtil.sendSms(recoverContactCode.getContact(), String.format("[맛집 이메일 찾기] 인증번호 [%s]를 입력해 주세요.", recoverContactCode.getCode()));
        return this.userMapper.insertRecoverContactCode(recoverContactCode) > 0
                ? SendRecoverContactCodeResult.SUCCESS
                : SendRecoverContactCodeResult.FAILURE;
    }

    public VerifyRecoverContactCodeResult recoverContactCodeResult(RecoverContactCodeEntity recoverContactCode) {
        if (recoverContactCode == null ||
                recoverContactCode.getContact() == null ||
                recoverContactCode.getCode() == null ||
                recoverContactCode.getSalt() == null ||
                !recoverContactCode.getContact().matches("^(010\\d{8})$") ||
                !recoverContactCode.getCode().matches("^(\\d{6})$") ||
                !recoverContactCode.getSalt().matches("^([\\da-f]{128})$")) {
            return VerifyRecoverContactCodeResult.FAILURE;
        }
        recoverContactCode = this.userMapper.selectRecoverContactCodeByContactCodeSalt(recoverContactCode);
        if (recoverContactCode == null) {
            return VerifyRecoverContactCodeResult.FAILURE;
        }
        if (new Date().compareTo(recoverContactCode.getExpiresAt()) > 0) {
            return VerifyRecoverContactCodeResult.FAILURE_EXPIRED;
        }
        recoverContactCode.setExpired(true);
        return this.userMapper.updateRecoverContactCode(recoverContactCode) > 0
                ? VerifyRecoverContactCodeResult.SUCCESS
                : VerifyRecoverContactCodeResult.FAILURE_EXPIRED;
    }

    public UserEntity getUserByContact(String contact) {
        return this.userMapper.selectUserByContact(contact);
    }

    public SendRecoverEmailCodeResult sendRecoverEmailCode(RecoverEmailCodeEntity recoverEmailCode) throws MessagingException {
        if (recoverEmailCode == null ||
                recoverEmailCode.getEmail() == null ||
                !recoverEmailCode.getEmail().matches("^(?=.{10,50}$)([\\da-zA-Z\\-_\\.]{5,25})@([\\da-z][\\da-z\\-]*[\\da-z]\\.)?([\\da-z][\\da-z\\-]*[\\da-z])\\.([a-z]{2,15})(\\.[a-z]{2})?$")) {
            return SendRecoverEmailCodeResult.FAILURE;
        }
        if (this.userMapper.selectUserByEmail(recoverEmailCode.getEmail()) == null) {
            return SendRecoverEmailCodeResult.FAILURE;
        }
        recoverEmailCode
                .setCode(RandomStringUtils.randomAlphanumeric(6))
                .setSalt(CryptoUtil.hashSha512(String.format("%s%s%f%f",
                        recoverEmailCode.getCode(),
                        recoverEmailCode.getEmail(),
                        Math.random(),
                        Math.random())))
                .setCreatedAt(new Date())
                .setExpiresAt(DateUtils.addHours(recoverEmailCode.getCreatedAt(), 1))
                .setExpired(false);
        String url = String.format("http://localhost:6795/user/recoverPassword?email=%s&code=%s&salt=%s",
                recoverEmailCode.getEmail(),
                recoverEmailCode.getCode(),
                recoverEmailCode.getSalt());
        Context context = new Context();
        context.setVariable("url", url);

        MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        mimeMessageHelper.setSubject("[맛집 비밀번호 재설정] 이메일 인증");
        mimeMessageHelper.setFrom("chngdch35@gmail.com"); // 당신 이메일
        mimeMessageHelper.setTo(recoverEmailCode.getEmail());
        mimeMessageHelper.setText(this.springTemplateEngine.process("_recoverEmail", context), true);
        this.javaMailSender.send(mimeMessage);

        return this.userMapper.insertRecoverEmailCode(recoverEmailCode) > 0
                ? SendRecoverEmailCodeResult.SUCCESS
                : SendRecoverEmailCodeResult.FAILURE;
    }

    public VerifyRecoverEmailCodeResult verifyRecoverEmailCode(RecoverEmailCodeEntity recoverEmailCode) {
        if (recoverEmailCode == null ||
                recoverEmailCode.getEmail() == null ||
                recoverEmailCode.getCode() == null ||
                recoverEmailCode.getSalt() == null ||
                !recoverEmailCode.getEmail().matches("^(?=.{10,50}$)([\\da-zA-Z\\-_\\.]{5,25})@([\\da-z][\\da-z\\-]*[\\da-z]\\.)?([\\da-z][\\da-z\\-]*[\\da-z])\\.([a-z]{2,15})(\\.[a-z]{2})?$") ||
                !recoverEmailCode.getCode().matches("^([\\da-zA-Z]{6})$") ||
                !recoverEmailCode.getSalt().matches("^([\\da-f]{128})$")) {
            return VerifyRecoverEmailCodeResult.FAILURE;
        }
        recoverEmailCode = this.userMapper.selectRecoverEmailCodeByEmailCodeSalt(recoverEmailCode);
        if (recoverEmailCode == null) {
            return VerifyRecoverEmailCodeResult.FAILURE;
        }
        if (new Date().compareTo(recoverEmailCode.getExpiresAt()) > 0) {
            return VerifyRecoverEmailCodeResult.FAILURE_EXPIRED;
        }
        recoverEmailCode.setExpired(true);
        return this.userMapper.updateRecoverEmailCode(recoverEmailCode) > 0
                ? VerifyRecoverEmailCodeResult.SUCCESS
                : VerifyRecoverEmailCodeResult.FAILURE;
    }

    public RecoverPasswordResult recoverPassword(RecoverEmailCodeEntity recoverEmailCode, UserEntity user) {
        if (recoverEmailCode == null ||
                recoverEmailCode.getEmail() == null ||
                recoverEmailCode.getCode() == null ||
                recoverEmailCode.getSalt() == null ||
                user == null ||
                user.getPassword() == null) {
            return RecoverPasswordResult.FAILURE;
        }
        recoverEmailCode = this.userMapper.selectRecoverEmailCodeByEmailCodeSalt(recoverEmailCode);
        if (recoverEmailCode == null || !recoverEmailCode.isExpired()) {
            return RecoverPasswordResult.FAILURE;
        }
        String newPassword = CryptoUtil.hashSha512(user.getPassword());
        user = this.userMapper.selectUserByEmail(user.getEmail());
        if (user == null) {
            return RecoverPasswordResult.FAILURE;
        }
        user.setPassword(newPassword);
        return this.userMapper.updateUser(user) > 0 && this.userMapper.deleteRecoverEmailCode(recoverEmailCode) > 0
                ? RecoverPasswordResult.SUCCESS
                : RecoverPasswordResult.FAILURE;
    }
}
