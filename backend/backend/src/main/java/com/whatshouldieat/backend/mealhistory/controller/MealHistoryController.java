package com.whatshouldieat.backend.mealhistory.controller;

import com.whatshouldieat.backend.mealhistory.dto.MealHistoryCreateRequest;
import com.whatshouldieat.backend.mealhistory.dto.MealHistoryResponse;
import com.whatshouldieat.backend.mealhistory.dto.MealHistoryUpdateRequest;
import com.whatshouldieat.backend.mealhistory.dto.PageResponse;
import com.whatshouldieat.backend.mealhistory.service.MealHistoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


// @RestController는 해당 class가 HTTP 요청을 처리하고 반환값을 JSON 응답으로 전송하는 Controller임을 나타낸다.
// @Controller라는 어노테이션도 있지만, 이는 주로 HTML의 이름을 반환할때 사용되며 Next.js에서 사용할 Rest API를 반환해야할 때는 RestController를 이용한다.
@RestController
// @RequestMapping 어노테이션은 Controller 안의 모든 API에 공통으로 적용할 URL를 명시한다. 여기에서 세부적으로 Mapping을 통해 HTML 프로토콜을 붙인다.
@RequestMapping("/api/meal-histories")
public class MealHistoryController {

    private final MealHistoryService mealHistoryService;

    public MealHistoryController(MealHistoryService mealHistoryService) {
        this.mealHistoryService = mealHistoryService;
    }

    // RequestMapping에 더해서 HTML 프로토콜을 설정하는 어노테이션 PostMapping이기 때문에 아래 메서드는 POST가 매칭된다.
    @PostMapping
    // @RequestBody 어노테이션은 HTTP 요청 본문의 JSON을 MealHistoryCreateRequest 객체로 변환한다.
    // @Vaild 어노테이션을 사용하면 Request DTO에 작성한 검증 조건을 실행한다.
    public ResponseEntity<MealHistoryResponse> create(@Valid @RequestBody MealHistoryCreateRequest request) {
        MealHistoryResponse response = mealHistoryService.create(request);

        // ResponseEntity는 HTTP 상태 코드와 응답 본문을 반환한다.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get 메서드를 통해 모든 회원 정보를 얻는 finaAll() 메서드
    // 페이징 방식으로 변경
    // + category를 받아 카테고리 필터링 기능을 추가
    @GetMapping
    public ResponseEntity<PageResponse<MealHistoryResponse> > findAll(
            // @RequestParam = URL의 Query Parameter중 category 값을 가져온다. 추가로 필수 파라미터가 아니라고 추가로 명시했다.
            @RequestParam(required = false)
            String category,
            // Pageable값의 Default를 정할때 사용하는 어노테이션이다.
            // pageable은 놀랍게도 Spring에서 URL에 적혀있는 값을 보고 알아서 만들어서 넣어준다.
            // ex) GET /api/meal-histories?page=1&size=5 = page = 1, size = 5
            @PageableDefault(page = 0, size = 10)
            Pageable pageable
    ) {
        // Service 에서 선언한 findAll()을 호출하여 Response를 담은 List를 담는다.
        // 이때 인자로 들어가는 category와 pageable은 필터링과 페이징 기능을 이용하기 위해서 넣는것이다.
        PageResponse<MealHistoryResponse> responses = mealHistoryService.findAll(category, pageable);

        // HTTP 200 OK와 함께 responses에 들어있는 List<MealHistoryResponse> 를 반환
        return ResponseEntity.ok(responses);
    }

    // Get 메서드를 통해 특정 id 값으로 해당 회원의 정보를 얻은 findById() 메서드
    @GetMapping("/{id}")
    public ResponseEntity<MealHistoryResponse> findById(@PathVariable UUID id) {
        // response에 findById 값을 저장
        MealHistoryResponse response = mealHistoryService.findById(id);

        // 해당 결과 값을 ok 값과 함께 반환
        return ResponseEntity.ok(response);
    }

    // Put 메서드를 이용해 update 진행
    // 게시글의 UUID를 기반으로 해당 게시글을 수정
    @PutMapping("/{id}")
    public ResponseEntity<MealHistoryResponse> update(
            // @PathVariable = URL에 있는 id 값을 추출하여 가져옴
            @PathVariable UUID id,
            // @Valid = DTO에서 설정한 Validation 진행
            // @RequestBody = 요청 JSON을 MealHistoryUpdateRequest로 변환
            @Valid @RequestBody MealHistoryUpdateRequest request
    ) {
        // Service에서 update()를 진행해 반환한 MealHistoryResponse를 response에 저장
        MealHistoryResponse response = mealHistoryService.update(id, request);

        // OK와 함께 반환
        return ResponseEntity.ok(response);
    }

    // Delete 메서드를 이용해 delete를 진행
    // 제공된 게시글의 id를 이용해 해당 게시글의 삭제를 진행
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        mealHistoryService.delete(id);
        // build()는 마지막에 객체를 완성하는 메서드이다.
        // 설정할 것이 있다면 앞에서 미리 설정해야한다. (ex) .noContent(), .header() 등등
        return ResponseEntity.noContent().build();
    }


}

