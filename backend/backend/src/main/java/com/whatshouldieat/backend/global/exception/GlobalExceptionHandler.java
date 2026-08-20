package com.whatshouldieat.backend.global.exception;


import com.whatshouldieat.backend.mealhistory.exception.MealHistoryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// @RestControllerAdvice = REST Controller에서 발생하는 예외를 공통적으로 처리하는 클래스임을 나타내는 어노테이션
// Controller에서 예외가 발생하면, GlobalExceptionHandler가 확인하며, 일치하는 @ExceptionHandler를 실행하여 응답을 반환하게 한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @ExceptionHandler({Exception}) = 인자로 들어온 Exception이 발생하면 아래의 메서드를 사용하게 하는 어노테이션
    // orElseThrow에서 인자로 넣은 exception 값을 기준으로 찾게 된다.
    // .class는 특정 클래스 자체의 타입 정보를 나타내는 Class 객체를 가져오는 Java 문법이다.
    // orElseThrow에서는 lambda 식을 이용해 새로운 인스턴스를 만들어 이를 인자로 보내준다.
    // 그러나 현재 인자로 받는 것에 대해서 생각해보면 int wow 이런식으로 받는다. 그런데 명시용 exception 객체를 받으려면 어떻게 적어야할지 애매하다.
    // 이때 쓰이는 문법이 .class 이다. 이를 이용해서 MealHistoryNotFoundException가 들어 올것이라고 하는 것. 즉, 그 틀을 알려주는 것이다.
    // Spring이 처리 대상을 구분할 수 있도록 예외 클래스의 타입 정보를 전달하는 표현인 것
    // MealHistoryNotFoundException만 적으면 에러가 나니 주의
    @ExceptionHandler(MealHistoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMealHistoryNotFound (MealHistoryNotFoundException exception) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        // ErrorResponse의 of() 메서드를 이용해 HTTP 상태코드, 에러코드, 메세지에 시간을 추가하여 저장
        ErrorResponse response = ErrorResponse.of(
            status.value(),
            "MEAL_HISTORY_NOT_FOUND",
            // MealHistoryNotFoundException에서 super로 지정한 메시지를 꺼내는 메서드
            exception.getMessage()
        );

        // 완성한 ErrorResponse를 반환
        return ResponseEntity.status(status).body(response);
    }
}
