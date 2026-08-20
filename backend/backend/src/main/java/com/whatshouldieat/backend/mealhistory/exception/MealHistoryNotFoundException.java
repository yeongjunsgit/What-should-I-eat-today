package com.whatshouldieat.backend.mealhistory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

// 아래의 예외가 발생하면 Spring은 설정한 HTTP 에러인 404 Not Found를 반환하게 하는 어노테이션
// 현재는 exception.GlobalExceptionHandler에서 404 에러를 다루므로 해당 어노테이션은 제거
//@ResponseStatus(HttpStatus.NOT_FOUND)
public class MealHistoryNotFoundException extends RuntimeException{

    public MealHistoryNotFoundException(UUID id) {
        super("먹히스토리를 찾을 수 없습니다. id= " + id);
    }
}
