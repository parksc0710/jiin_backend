package com.jiin.backend.mapper;

import com.jiin.backend.domain.RegisterToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegisterTokenMapper {

    void insertToken(RegisterToken token);

    RegisterToken consumeToken(@Param("token") String token);
}
