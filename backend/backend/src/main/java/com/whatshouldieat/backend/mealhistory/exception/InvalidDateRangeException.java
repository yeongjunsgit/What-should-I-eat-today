package com.whatshouldieat.backend.mealhistory.exception;

import java.time.LocalDate;

// 게시글의 구간이 잘못됬을 때 이를 확인하고 예외처리하는 기간 예외 클래스 생성
public class InvalidDateRangeException extends RuntimeException{
    public InvalidDateRangeException (
            LocalDate from,
            LocalDate to
    ) {
        super(
                "시작일은 종료일보다 늦을 수 없습니다."
                    + "from= " + from
                    + ", to= " + to
        );
    }

}
