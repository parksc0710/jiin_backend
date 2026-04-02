package com.jiin.backend.mapper;

import com.jiin.backend.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByProviderAndProviderId(@Param("provider") String provider,
                                    @Param("providerId") String providerId);

    User findById(@Param("userId") Long userId);

    void insertUser(User user);

    int countByNickname(@Param("nickname") String nickname);

    void updateNickname(@Param("userId") Long userId, @Param("nickname") String nickname);

    void upsertUser(User user);
}
