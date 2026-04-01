package com.jiin.backend.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TestMapperTest {

    @Autowired
    TestMapper testMapper;

    @Test
    void up_front_test_SP_호출() {
        List<Map<String, Object>> result = testMapper.callFrontTest();
        assertNotNull(result);
        System.out.println("결과: " + result);
    }
}
