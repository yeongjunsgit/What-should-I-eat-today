package com.whatshouldieat.backend.mealhistory.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

// update에 사용할 DTO를 만듬.
// 만약, patch를 사용한다면 이전과 다르게 @Notnull과 @Notblank를 사용하지 않음
// patch는 일부만 수정할 수 있기 때문에, 일부만 받아도 작동하도록 해야 하기때문
// 하지만 이번에는 PUT을 사용 할 것이기 떄문에 Create DTO와 동일하게 작성되어야함
public record MealHistoryUpdateRequest (
    @NotBlank
    @Size(max = 100)
    String foodName,

    @Size(max = 50)
    String category,

    @PositiveOrZero
    Integer price,

    @Min(1)
    @Max(5)
    Integer rating,
    String memo,
    Boolean wouldEatAgain,

    // 이전에 createDTO에서는 boolean을 사용했다. 그래서 null 값을 허용하지 않았었는데, patch를 한다면 Boolean으로 하여 null 값을 허용하여 관리해야한다.
    // 그 이유는 null값이 들어와야, isPublic에 대한 업데이트가 시도되는지 알 수 있기 때문.
    // 왜 지금은 굳이 boolean을 쓰지 않고, @NotNull + Boolean 조합을 이용할까?
    // boolean은 default 값이 false 이기 때문에, 값을 입력하지 않으면 자동으로 false가 된다.
    // 이를 방지하기 위해 @NotNull + Boolean를 사용하면 주어지는 값이 false라면 이는 반드시 사용자가 직접 준 값이 되므로 사용자의 의도를 잘 파악할 수 있다.
    @NotNull
    Boolean isPublic,

    @NotNull
    LocalDate ateAt

) {

}
