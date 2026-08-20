package com.whatshouldieat.backend.global.exception;

import java.time.OffsetDateTime;

public record ErrorResponse (
        // status 는 HTTP 상태 코드를 보내줄 것이다. ex) 404
        int status,
        // code는 오류 종류를 구분할 코드를 보내줄 것이다. ex) NOT FOUND
        String code,
        // message는 발생한 오류에 관하여 개발자 또는 사용자가 받게 될 메세지이다. ex) 요청하신 것을 찾을 수가 없습니다.
        String message,
        // timestamp는 에러가 발생한 시간을 나타낸다. ex) 2026-08-20T16:30:00+09:00
        OffsetDateTime timestamp
) {
    // of() 메서드는 시간을 추가하는 메서드이다. 인자로 받은 3가지 정보에 시간을 더해서 ErrorResponse를 만들어 반환한다.
    public static ErrorResponse of(
            int status,
            String code,
            String message
    ) {
        return new ErrorResponse(
                status,
                code,
                message,
                OffsetDateTime.now()
        );
    }
}
