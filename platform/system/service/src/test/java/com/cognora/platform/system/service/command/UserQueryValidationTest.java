package com.cognora.platform.system.service.command;

import com.cognora.platform.common.dto.user.BatchUserRequest;
import com.cognora.platform.common.dto.user.UserQueryRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserQueryValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsPageSizeAboveCap() {
        UserQueryRequest request = new UserQueryRequest();
        request.setSize(101);

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("size")));
    }

    @Test
    void rejectsUnknownSortField() {
        UserQueryRequest request = new UserQueryRequest();
        request.setSortBy("password");

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("sortBy")));
    }

    @Test
    void rejectsBatchAboveHundredIds() {
        BatchUserRequest request = new BatchUserRequest();
        request.setIds(java.util.stream.IntStream.range(0, 101)
                .mapToObj(i -> UUID.randomUUID())
                .toList());

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void acceptsDefaultQuery() {
        assertEquals(0, validator.validate(new UserQueryRequest()).size());
        assertEquals(1, new UserQueryRequest().getPage());
        assertEquals(10, new UserQueryRequest().getSize());
    }
}
