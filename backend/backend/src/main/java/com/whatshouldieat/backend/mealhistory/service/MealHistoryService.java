package com.whatshouldieat.backend.mealhistory.service;

import com.whatshouldieat.backend.mealhistory.domain.MealHistory;
import com.whatshouldieat.backend.mealhistory.dto.MealHistoryCreateRequest;
import com.whatshouldieat.backend.mealhistory.dto.MealHistoryResponse;
import com.whatshouldieat.backend.mealhistory.dto.MealHistoryUpdateRequest;
import com.whatshouldieat.backend.mealhistory.dto.PageResponse;
import com.whatshouldieat.backend.mealhistory.exception.MealHistoryNotFoundException;
import com.whatshouldieat.backend.mealhistory.repository.MealHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// @Service 어노테이션을 달면 해당 클래스가 비즈니스 로직을 담당하는 Spring Bean이라는 걸 나타낸다.
@Service
public class MealHistoryService {


    // 생성자 주입 코드, Spring은 이미 "MealHistoryRepository"의 구현 객체를 자동으로 만들어서 Bean에 등록했다.
    private final MealHistoryRepository mealHistoryRepository;
    // 그리고 아래의 코드를 통해 MealHistoryService를 만들 때 그 Repository 객체를 생성자에 전달한다. 이것을 의존성 주입이라고 한다.
    public MealHistoryService(MealHistoryRepository mealHistoryRepository) {
        this.mealHistoryRepository = mealHistoryRepository;
    }

    // @Transactional은 여러 개의 DB 작업을 하나의 작업 단위로 묶어준다.
    // 이렇게 되면, 묶여있는 작업 중 1개라도 실패했을 시, Rollback를 할 수 있어 안정성에 좋다!
    @Transactional
    public MealHistoryResponse create(MealHistoryCreateRequest request) {
        // Request DTO에서 꺼낸 값을 토대로 생성자를 호출
        MealHistory mealHistory = new MealHistory(
                // MealHistoryCreateRequest에서 값을 꺼내온다. (Request DTO에서 값을 꺼내온다.)
                request.foodName(),
                request.category(),
                request.price(),
                request.rating(),
                request.memo(),
                request.wouldEatAgain(),
                request.isPublic(),
                request.ateAt()
        );

        // Repository에 생성한 mealHistory를 저장. 이때 @prePersist가 동작하여 위 생성자에서 추가하지 않은 id, createdAt, updatedAt 값을 추가하여 저장
        // savedMealHistory에는 prePersist를 통해 추가된 값이 같이 저장되어있다.
        MealHistory savedMealHistory = mealHistoryRepository.save(mealHistory);

        // savedMealHistory를 .from()을 이용해 Response DTO로 변환한 후에 반환한다.
        // DTO로 변환하고 반환하면 Entity의 구조를 숨길 수 있고 보내줄 값을 제어하는 등의 이점이 많다!
        return MealHistoryResponse.from(savedMealHistory);
    }

    // @Transactional은 여러 개의 DB 작업을 하나의 작업 단위로 묶어준다.
    // 거기에 readonly 조건을 걸어 해당 트랜잭션이 읽기전용이라는 걸 JPA에게 알려준다.
    // 기능자체를 제한하는 권한은 없으므로 유의
    @Transactional(readOnly = true)
    public List<MealHistoryResponse> findAll() {
        return mealHistoryRepository
                // repository에서 제작한 메서드로, 모든 값을 조회하여 정렬해서 반환하는 요청
                .findAllByOrderByAteAtDescCreatedAtDesc()
                // 목록의 데이터를 순서대로 처리한다.
                .stream()
                // 각각의 MealHistory를 MealHistoryResponse로 변환
                // 아래의 문법은 람다식의 축약으로 원래 코드는 아래와 같다.
                /*
                .map(mealHistory -> MealHistoryResponse.from(mealHistory)
                */
                .map(MealHistoryResponse::from)
                // 변환된 결과를 새로운 List로 생성
                .toList();
    }

    // 페이징 기능을 추가한 findAll()을 추가한다.
    @Transactional(readOnly = true)
    public PageResponse<MealHistoryResponse> findAll(
            Pageable pageable
    ) {

        Page<MealHistoryResponse> page =
                // mealHistoryRepository의 findAllByOrderByAteAtDescCreatedAtDesc()에 인자를 넣으면 알아서 Page<>를 반환하는 메서드로 매칭된다.
                mealHistoryRepository
                        .findAllByOrderByAteAtDescCreatedAtDesc(pageable)
                        // Page에도 map 메서드가 존재하기 때문에 여기에 map 메서드를 사용할 수 있다. map() 을 통해 모든 mealHistory를 MealHistoryResponse로 변환한다.
                        .map(MealHistoryResponse::from);

        // Page<MealHistoryResponse>를 반환한다.
        return PageResponse.from(page);
    }



    // readOnly Transactional 어노테이션을 부여
    @Transactional(readOnly = true)
    public MealHistoryResponse findById(UUID id) {
        // id값을 받아 findById를 진행한다. 그 결과 값을 mealHistory에 저장한다.
        MealHistory mealHistory = mealHistoryRepository
                .findById(id)
                // 단, Optional 안에 아무런 값이 없다면 MealHistoryNotFoundException를 통해 예외 값을 mealHistory에 저장한다.
                .orElseThrow(() ->
                        new MealHistoryNotFoundException(id)
                );
        // 저장한 mealHistory를 반환한다.
        return MealHistoryResponse.from(mealHistory);
    }

    // 해당 메서드의 작업을 하나의 DB 작업 단위로 묶음
    @Transactional
    public MealHistoryResponse update(
            UUID id,
            MealHistoryUpdateRequest request
    ) {
        // 현재 수정하려는 게시물을 uuid 기반으로 찾아 mealHistory에 저장한다. (단, 없으면 NotFoundException을 반환)
        MealHistory mealHistory = mealHistoryRepository.findById(id)
                .orElseThrow(() -> new MealHistoryNotFoundException(id));

        // 해당 게시물을 request에 들어있는 필드값들로 PUT한다.
        mealHistory.update(
                request.foodName(),
                request.category(),
                request.price(),
                request.rating(),
                request.memo(),
                request.wouldEatAgain(),
                request.isPublic(),
                request.ateAt()
        );

        // 현재 update를 진행하면, 필드값이 변환이 잘되지만, updateAt이 갱신이 안되는 상황이 일어난다.
        // 이는 @PreUpdate가 실행되기 전에, Response DTO가 만들어졌기 때문이다. 이 때문에 updatedAt 또한 이전에 있었던 값으로 사용되는 것이다.
        // 이를 해결하기위해서 Repository에 flush() 메서드를 이용한다. 이를 이용하면 DTO가 생성되기전 @PreUpdate가 작동하여 updateAt값을 정상적으로 갱신해준다.
        // flush() = 현재 트랜잭션의 변경 내용을 DB에 반영되도록 JPA에 요청하는 메서드
        mealHistoryRepository.flush();

        // 수정한 mealHistory를 반환한다.
        return MealHistoryResponse.from(mealHistory);
    }

    // 해당 메서드의 작업을 하나의 DB 작업 단위로 묶음
    @Transactional
    public void delete(UUID id) {
        // id를 기반으로 삭제하고자하는 MealHistory를 찾아 저장 (단, 없는 경우 예외 발생)
        MealHistory mealHistory = mealHistoryRepository.findById(id)
                .orElseThrow(() -> new MealHistoryNotFoundException(id));

        // 삭제하고자 하는 Mealhistory를 인자로하여 delete() 메서드를 통해 삭제
        mealHistoryRepository.delete(mealHistory);
    }



}
