package com.ea.common.domain;

import com.ea.common.enums.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RTest {

    @Test
    void okShouldReturnSuccessCode() {
        R<String> result = R.ok("hello");
        assertTrue(result.isSuccess());
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    void failShouldReturnFailCode() {
        R<Void> result = R.fail("error");
        assertFalse(result.isSuccess());
        assertEquals(ResultCode.FAIL.getCode(), result.getCode());
        assertEquals("error", result.getMessage());
    }
}
