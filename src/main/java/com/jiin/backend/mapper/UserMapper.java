package com.jiin.backend.mapper;

import com.jiin.backend.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByProviderAndProviderId(@Param("provider") String provider,
                                    @Param("providerId") String providerId);

    User findById(@Param("userId") Long userId);

    void upsertUser(User user);
}
