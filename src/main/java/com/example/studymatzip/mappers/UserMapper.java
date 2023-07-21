package com.example.studymatzip.mappers;

import com.example.studymatzip.entities.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper  {
    int deleteRecoverEmailCode(RecoverEmailCodeEntity recoverEmailCode);

    int insertRecoverContactCode(RecoverContactCodeEntity recoverContactCode);

    int insertRecoverEmailCode(RecoverEmailCodeEntity recoverEmailCode);

    int insertRegisterContactCode(RegisterContactCodeEntity registerContactCode);

    int insertRegisterEmailCode(RegisterEmailCodeEntity registerEmailCode);

    int insertUser(UserEntity user);

    UserEntity selectUserByEmail(@Param(value = "email") String email);

    UserEntity selectUserByNickname(@Param(value = "nickname") String nickname);

    UserEntity selectUserByContact(@Param(value = "contact") String contact);

    RecoverContactCodeEntity selectRecoverContactCodeByContactCodeSalt(RecoverContactCodeEntity recoverContactCode);

    RecoverEmailCodeEntity selectRecoverEmailCodeByEmailCodeSalt(RecoverEmailCodeEntity recoverEmailCode);

    RegisterContactCodeEntity selectRegisterContactCodeByContactCodeSalt(RegisterContactCodeEntity registerContactCode);

    RegisterEmailCodeEntity selectRegisterEmailCodeByEmailCodeSalt(RegisterEmailCodeEntity registerEmailCode);

    int updateUser(UserEntity user);

    int updateRecoverContactCode(RecoverContactCodeEntity recoverContactCode);

    int updateRecoverEmailCode(RecoverEmailCodeEntity recoverEmailCode);

    int updateRegisterContactCode(RegisterContactCodeEntity registerContactCode);

    int updateRegisterEmailCode(RegisterEmailCodeEntity registerEmailCode);
}
