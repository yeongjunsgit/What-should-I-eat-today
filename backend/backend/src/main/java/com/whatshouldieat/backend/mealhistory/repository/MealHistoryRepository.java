package com.whatshouldieat.backend.mealhistory.repository;

import com.whatshouldieat.backend.mealhistory.domain.MealHistory;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// JpaRepository에서, 인자로 들어가는 첫번째 값은 이 Repository가 관리할 Entity를, 두번째 값은 해당 Entity의 id 타입을 넣어야 한다.

// 보통 직접 만든 Class에서는 @Repository 어노테이션을 달아서 JPA가 이를 인식하게 만들어준다.
// 그러나 지금 interface는 JpaRepository를 상속하고 있고, 이렇게 되면 JPA가 해당 인터페이스를 Repository로 자동으로 인식하여 Bean으로 등록해준다!
public interface MealHistoryRepository
    extends JpaRepository<MealHistory, UUID> {

    // 이름이 아주 특이한 메서드로 인자도, 어떠한 구현도 하지않았다(이건 인터페이스라서)
    // 하지만 이름을 보면 이 메서드가 하는 역할을 알 수 있다.
    // findAll = 전체조회, OrderBy = 정렬한다, AteAtDesc = ateAt값을 내림차순으로, CreatedAtDesc = 앞조건이 같다면 생성날짜를 내림차순으로
    // 페이징 하지 않는 기존의 findAllByOrderByAteAtDescCreatedAtDesc을 삭제
//    List<MealHistory> findAllByOrderByAteAtDescCreatedAtDesc();

    // 현재 따로 구현하는 메서드가 없다. 그러나 정상 작동할 예정이다.
    // 그 이유는 바로 JPA가 interface를 발견하고, 이에 대한 구현 객체를 자동으로 생성하기 때문이다.
    // save({args}) = 저장
    // findAll() = 전체 목록 조회
    // findById({id 값}) = ID로 조회 (결과값은 Optional)[null이 반환될 수 있기 때문에]
    // existById({id 값}) = ID로 해당 값이 존재하는지 조회 (결과값은 boolean)
    // delete({entity}) = 인자로 들어온 entity와 일치하는 ID값을 가진 값을 DB에서 제거
    // deleteById({id 값}) = 인자로 들어온 id 값과 일치하는 값을 DB에서 제거
    // count() = 개수 조회

    // 페이징을 이용하기 위한 Repository 메서드로 Pageable을 인자로 받는다. 이때 Page에서 정한 제네릭 타입을 MealHistory로 한다.
    // Pageable은 페이지 조회 조건에 대한 정보를 담고 있다. (몇번째 페이지인지, 페이지 당 몇개를 조회하는지, 어떤 기준으로 정렬하는지)
    // 그러나 현재 메서드 명에서 정렬하는 기준을 적어 두었기 때문에, 따로 정렬기준은 제공하지 않는다.
//    Page<MealHistory> findAllByOrderByAteAtDescCreatedAtDesc(
//            Pageable pageable
//    );


    // 해당 메서드는 category 값을 기준으로 필터링 한 결과값을 조회하는 메서드이다. 페이징이 구현되어야하여 Pageable 값을 받고, 필터링 기준인 category 값을 받는다.
//    Page<MealHistory> findAllByCategoryOrderByAteAtDescCreatedAtDesc(
//            String category,
//            Pageable pageable
//    );

    // 여태까지 위의 메소드들은 메소드의 이름을 보고 JPA가 Query문을 작성해주었다. 그렇기 때문에 별다른 SQL문을 작성하지 않아도 DB에서 값을 꺼내올 수 있었다.
    // 그러나, 조건이 너무 많아지면 메소드 명이 너무 길어지게 되고 이는 유지보수측면이나, 사용면에서 불편함을 일으킬 수 있다. 이를 방지하기 위해서 메소드명을 이용하지 않고
    // 직접 Query문을 작성할 수 있게 해줄 수 있는데 이것이 @Query 어노테이션이다.
    // 단, 작성한 Query문은 JPQL이기 때문에 그대로 사용하는 것은 아니고, 이를 JPA가 SQL로 변환하여 요청하게된다.
    // JPQL은 SQL과 문법이 조금 차이가 나며 아래에서 보이듯이, Java에서 사용하는 필드명을 사용한다.
    // Query문 중 COALESCE()는 인자로 들어간 값 중 NULL이 아닌 더 앞에 있는 값을 반환한다.
    @Query("""
            SELECT m
            FROM MealHistory m
            WHERE (:category IS NULL OR m.category = :category)
              AND m.ateAt >= COALESCE(:fromDate, m.ateAt)
              AND m.ateAt <= COALESCE(:toDate, m.ateAt)
            ORDER BY m.ateAt DESC, m.createdAt DESC
            """)
    Page<MealHistory> findAllWithFilters(
            // @Param 어노테이션은 위의 Query 문에서 :{필드명}으로 값을 비교하고 있는데, 이를 매칭시켜주는데 사용하는 어노테이션이다.
            // 반드시 위에 필드명과 괄호안의 필드명이 동일해야만 매칭된다. 매칭이 되어야 Query가 정상적으로 돌아간다!
            @Param("category") String category,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

}
