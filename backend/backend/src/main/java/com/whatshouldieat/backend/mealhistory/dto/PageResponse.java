package com.whatshouldieat.backend.mealhistory.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// record PageResponse를 선언 이때, generic을 이용하여 재사용성을 증대
public record PageResponse<T> (
        // content는 List 이되, 그 안의 자료형은 제네릭으로 정한 타입이 된다.
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        // 첫페이지인지 아닌지 여부를 담당하는 first
        boolean first,
        // 마지막 페이지인지 아닌지 여부를 담당하는 last
        boolean last
) {
    // 팩토리 메서드 from()을 이용하여 PageResponse를 만든다. 이때 제네릭 타입 T는 인자로 받은 Page배열의 타입이다.
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
