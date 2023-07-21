package com.example.studymatzip.controllers;


import com.example.studymatzip.entities.*;
import com.example.studymatzip.enums.*;
import com.example.studymatzip.services.UserService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;


@Controller
@RequestMapping(value = "/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = "contactCode",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getContactCode(RegisterContactCodeEntity registerContactCode) {
        SendRegisterContactCodeResult result = this.userService.sendRegisterContactCode(registerContactCode);
        JSONObject responseObject = new JSONObject() {{
            put("result", result.name().toLowerCase());
        }};
        if (result == SendRegisterContactCodeResult.SUCCESS) {
            responseObject.put("salt", registerContactCode.getSalt());
        }
        return responseObject.toString();
    }

    @RequestMapping(value = "contactCode",
            method = RequestMethod.PATCH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String patchContactCode(RegisterContactCodeEntity registerContactCode) {
        VerifyRegisterContactCodeResult result = this.userService.verifyRegisterContactCode(registerContactCode);
        JSONObject responseObject = new JSONObject() {{
            put("result", result.name().toLowerCase());
        }};
        return responseObject.toString();
    }

    @RequestMapping(value = "emailCount",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getEmailCount(@RequestParam(value = "email") String email) {
        CheckEmailResult result = this.userService.checkEmail(email);
        JSONObject responseObject = new JSONObject() {{
            put("result", result.name().toLowerCase());
        }};
        return responseObject.toString();
    }

    @RequestMapping(value = "nicknameCount",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getNicknameCount(@RequestParam(value = "nickname") String nickname) {
        CheckNicknameResult result = this.userService.checkNickname(nickname);
        JSONObject responseObject = new JSONObject() {{
            put("result", result.name().toLowerCase());
        }};
        return responseObject.toString();
    }

    @RequestMapping(value="register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postRegister(UserEntity user, RegisterContactCodeEntity registerContactCode) throws MessagingException {
        RegisterResult result = this.userService.register(user, registerContactCode);
        JSONObject responseObject = new JSONObject(){{
            put("result", result.name().toLowerCase());
        }};
        return responseObject.toString();
    }


    @RequestMapping(value = "login",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postLogin(HttpSession session, UserEntity user) {
        LoginResult result = this.userService.login(user);
        if (result == LoginResult.SUCCESS) {
            session.setAttribute("user", user);
        }
        JSONObject responseObject = new JSONObject() {{
            put("result", result.name().toLowerCase());
        }};
        return responseObject.toString();
    }

    // http://localhost:6795/user/emailCode?email=inst.yhp@gmail.com&code=b89vXg&salt=49aa5c04b872b2306e6f650ccf633f390095708c8b9dcc5c25a6d1748f98248ea6ede99394cd3cf835a0dc1dd98b611e949b9adb85512645608160c0bb32e7ac
    @RequestMapping(value = "emailCode",
            method = RequestMethod.GET,
            produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getEmailCode(RegisterEmailCodeEntity registerEmailCode) {
        VerifyRegisterEmailCodeResult result = this.userService.verifyRegisterEmailCode(registerEmailCode);
        return new ModelAndView() {{
            setViewName("user/emailCode");
            addObject("result", result.name());
        }};
    }

    @RequestMapping(value = "contactCodeRec",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getContactCodeRec(RecoverContactCodeEntity recoverContactCode) {
        SendRecoverContactCodeResult result = this.userService.sendRecoverContactCode(recoverContactCode);
        JSONObject responseObject = new JSONObject() {{
            put("result", result.name().toLowerCase());
        }};
        if (result == SendRecoverContactCodeResult.SUCCESS) {
            responseObject.put("salt", recoverContactCode.getSalt());
        }
        return responseObject.toString();
    }

    @RequestMapping(value = "contactCodeRec",
            method = RequestMethod.PATCH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String patchContactCodeRec(RecoverContactCodeEntity recoverContactCode) {
        VerifyRecoverContactCodeResult result = this.userService.recoverContactCodeResult(recoverContactCode);
        JSONObject responseObject = new JSONObject() {{
            put("result", result.name().toLowerCase());
        }};
        if (result == VerifyRecoverContactCodeResult.SUCCESS) {
            UserEntity user = this.userService.getUserByContact(recoverContactCode.getContact());
            responseObject.put("email", user.getEmail());
        }
        return responseObject.toString();
    }

    @RequestMapping(value = "recoverPassword",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postRecoverPassword(RecoverEmailCodeEntity recoverEmailCode) throws MessagingException {
        SendRecoverEmailCodeResult result = this.userService.sendRecoverEmailCode(recoverEmailCode);
        JSONObject responseObject = new JSONObject() {{
            put("result", result.name().toLowerCase());
        }};
        return responseObject.toString();
    }

    @RequestMapping(value = "recoverPassword",
            method = RequestMethod.GET,
            produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getRecoverPassword(RecoverEmailCodeEntity recoverEmailCode) {
        VerifyRecoverEmailCodeResult result = this.userService.verifyRecoverEmailCode(recoverEmailCode);
        ModelAndView modelAndView = new ModelAndView("user/recoverPassword");
        modelAndView.addObject("result", result.name().toLowerCase());
        modelAndView.addObject("recoverEmailCode", recoverEmailCode);
        return modelAndView;
    }

    @RequestMapping(value = "recoverPassword",
            method = RequestMethod.PATCH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String patchRecoverPassword(RecoverEmailCodeEntity recoverEmailCode, UserEntity user) {
        RecoverPasswordResult result = this.userService.recoverPassword(recoverEmailCode, user);
        JSONObject responseObject = new JSONObject() {{
            put("result", result.name().toLowerCase());
        }};
        return responseObject.toString();
    }

    @RequestMapping(value = "logout", method = RequestMethod.GET)
    public ModelAndView getLogout(HttpSession session) {
        session.setAttribute("user",null); //user는 null이다
        ModelAndView modelAndView = new ModelAndView("redirect:/");//누르면 바로간다
        return modelAndView;
    }
}