package com.whatshouldieat.backend.mealhistory.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

// DTO를 만들때 class가 아닌 record가 이용되었다.
// 일반 클래스로 DTO를 만들면 필드, 생성자, getter 등을 작성해야한다. 하지만 record를 이용하면 필드, 생성자, getter 등등을 간결하게 작성할 수 있다.
// record는 요청 및 응답처럼 값을 전달하는 객체에 어울린다.
public record MealHistoryCreateRequest (
    // @NotBlank 어노테이션은 "", " ", null 값 모두를 허용하지 않는다. 즉, 어떠한 유효한 값이 꼭 필요하게 만드는 것
    @NotBlank(message = "음식 이름은 필수입니다.")
    // @Size는 입력되는 값의 크기를 제한한다.
    @Size(max = 100, message = "음식 이름은 100자 이하여야 합니다.")
    String foodName,

    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
    String category,

    // @PositiveOrZero는 값이 존재한다면, 그 값은 0 이상어야한다는 제한이다. 즉, null도 허용된다.
    @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
    Integer price,

    // @Min과 @Max는 들어오는 값의 최소값과 최대값을 제한하는 어노테이션으로, 값을 제한할 뿐 null값까지 막을 수는 없다. 값이 반드시 필요하다면 @NotNull도 추가해야한다.
    @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5 이하이어야 합니다.")
    Integer rating,

    String memo,

    Boolean wouldEatAgain,

    boolean isPublic,

    // @NotNull은 @NotBlank와 다르게 null 만 허용하지 않는다.
    @NotNull(message = "먹은 날짜는 필수입니다.")
    LocalDate ateAt
) {

}
