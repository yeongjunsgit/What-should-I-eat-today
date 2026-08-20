package com.whatshouldieat.backend.mealhistory.dto;

import com.whatshouldieat.backend.mealhistory.domain.MealHistory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MealHistoryResponse (
    UUID id,
    String foodName,
    String category,
    Integer price,
    Integer rating,
    String memo,
    Boolean wouldEatAgain,
    // 분명 Entity를 만들때는 이름이 publicRecord였는데 DTO에서는 DB에 저장된 is_public의 카멜케이스를 이용하고 있다. 이는 혼란을 줄이기 위해 DB의 이름과 매칭 시킨것이다.
    boolean isPublic,
    LocalDate ateAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

    // from()은 Entity를 Response DTO로 변환하는 메서드이다. 특별한 이름이 정해져있는건 아니나, 기능을 명시적으로 하기 위해 from()으로 적은 것.
    public static MealHistoryResponse from(MealHistory mealHistory) {
        return new MealHistoryResponse(
            mealHistory.getId(),
            mealHistory.getFoodName(),
            mealHistory.getCategory(),
            mealHistory.getPrice(),
            mealHistory.getRating(),
            mealHistory.getMemo(),
            mealHistory.getWouldEatAgain(),
            mealHistory.isPublicRecord(),
            mealHistory.getAteAt(),
            mealHistory.getCreatedAt(),
            mealHistory.getUpdatedAt()
        );
    }
}
