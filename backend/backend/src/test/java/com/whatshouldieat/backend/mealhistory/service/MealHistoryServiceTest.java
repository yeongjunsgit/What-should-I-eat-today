package com.whatshouldieat.backend.mealhistory.service;


import com.whatshouldieat.backend.mealhistory.domain.MealHistory;
import com.whatshouldieat.backend.mealhistory.dto.PageResponse;
import com.whatshouldieat.backend.mealhistory.exception.InvalidDateRangeException;
import com.whatshouldieat.backend.mealhistory.repository.MealHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// JUnit 테스트에서 Mockito를 사용할 수 있도록 연결해준다. JUnit 테스트는 메서드 단위의 테스트를 할때 사용하며, 만약 Spring의 기능까지 함께 테스트하고 싶다면
// SpringBootTest를 같이 사용하면 된다.
// 해당 테스트는 메서드 단위의 작업만 테스트하므로 @SpringBootTest를 사용하지 않는다.
@ExtendWith(MockitoExtension.class)
class MealHistoryServiceTest {

    // @Mock = 실제 Repository가 아닌 가짜 Repository 객체를 만든다.
    @Mock
    private MealHistoryRepository mealHistoryRepository;

    // @Mock을 통해 만든 Repository를 @InjectMocks를 통해 Service에 넣어준다.
    @InjectMocks
    private MealHistoryService mealHistoryService;

    // @Test = 아래의 메서드가 테스트 메서드라는걸 JUnit에게 알려주는 어노테이션
    @Test
    // @DisplayName = 아래의 메서드의 테스트가 끝난 후 표시되는 메서드의 이름을 정하는 어노테이션 결과창에 인자로 넢은 String이 출력되고 테스트 성공 여부가 주어진다.
    @DisplayName("시작일이 종료일보다 늦으면 예외가 발생한다")
    // from과 to의 구간이 유효하지 않을 때 정상 작동되는지 테스트하는 메서드
    void findAllThrowsExceptionWhenFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 8, 31);
        LocalDate to = LocalDate.of(2026, 8, 1);
        Pageable pageable = PageRequest.of(0, 10);

        // assertThatThrownBy() = 인자로 들어온 코드를 실행했을 때 예외가 발생하는지 검사하는 타이틀
        assertThatThrownBy(
                () -> mealHistoryService.findAll(
                        null,
                        from,
                        to,
                        pageable
                )
        )
                // .isInstanceOf() = 발생한 예외 타입이 인자로 넣은 예외 타입과 정확한지 확인한다.
                .isInstanceOf(InvalidDateRangeException.class)
                // .hasMessageContaining = 예외 메세지에 필요한 문구가 들어 있는지 확인한다.
                .hasMessageContaining(
                        "시작일은 종료일보다 늦을 수 없습니다."
                );

        verifyNoInteractions(mealHistoryRepository);

    }

    @Test
    @DisplayName("카테고리의 앞뒤 공백을 제거하고 조회 조건을 Repository에 전달한다")
    void findAllPassesNormalizedFiltersToRepository() {
        String category = "   한식   ";
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 30);
        Pageable pageable = PageRequest.of(0, 10);

        // 실제 DB를 사용하지 않으므로, Mock Repository가 반환할 빈 페이지를 만든다.
        Page<MealHistory> emptyPage = Page.empty(pageable);

        // when().thenReturn()은 Mock Respository의 동작을 미리 정하는 메서드이다.
        // when()의 코드와 동일한 조건으로 메서드가 호출되면 thenReturn의 값을 반환한다.
        // 이는, Mock 메서드가 null을 반환할 수 있기 때문에 null이 반환되어 Service단계에서 null에 .map()을 시도하지 않게하고자 하는 것이다.
        when(
                mealHistoryRepository.findAllWithFilters(
                        "한식",
                        from,
                        to,
                        pageable
                )
        ).thenReturn(emptyPage);

        // Service를 호출하는 코드 이렇게 되면 catrgory가 Service의 findAll()에 의해서 .strip() 된다.
        // <?> = 와일드카드라는 뜻으로 어떤 자료형이 들어올지는 모르겠지만 어떤 자료형이 들어감을 암시하는 것이다.
        // 따라서 <?> 를 사용하면 어떤 자료형의 값이 들어오던 허용을 하게 된다.
        PageResponse<?> response = mealHistoryService.findAll(
                category,
                from,
                to,
                pageable
        );

        // verify()는 Mock 객체에서 특정 메서드가 우리가 기대한 인자로 호출되었는지 확인한다. ("   한식  "이 "한식"으로 정상 변경되어 호출되는지 확인하는 것)
        // 만약 지정한 조건대로 호출된 적이 없다면 테스트 진행을 멈추고 실패를 반환한다.
        // 기본적으로 호출 횟수를 1번만 되었는지 체크하며 여러번 체크 되었는지 확인하려면 인자로 횟수를 같이 주면 된다 ``` verify(mealHistoryRepository, times(5)) ```
        verify(mealHistoryRepository).findAllWithFilters(
                "한식",
                from,
                to,
                pageable
        );

        // 응답 검증을 하는 코드들
        // 정상적으로 strip되어 빈페이지가 저장되었는지 확인
        assertThat(response.content()).isEmpty();
        // 페이지의 번호가 0인지 확인
        assertThat(response.page()).isEqualTo(0);
        // 페이지의 사이즈가 10인지 확인
        assertThat(response.size()).isEqualTo(10);
        // 아무런 값도 존재하지 않는지 확인
        assertThat(response.totalElements()).isZero();

    }

}
