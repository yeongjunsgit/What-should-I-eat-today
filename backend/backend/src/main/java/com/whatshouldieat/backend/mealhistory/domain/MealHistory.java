package com.whatshouldieat.backend.mealhistory.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;


// 이 클래스가 JPA 클래스라는 걸 알려주는 어노테이션, 이렇게 되면 spring boot가 실행되면 JPA가 이 클래스를 관리한다.
@Entity
// 이 entity가 연결될 실제 Postgre 상의 테이블 이름을 지정한다.
// 이를 생략해도 JPA가 클래스 이름을 기준으로 테이블 이름을 추론하지만, 명확하게 연결할때는 @Table을 이용해 직접 지정한다!
@Table(name = "meal_histories")
// 현재 클래스가 가지고 있는 모든 필드를 매개변수로 받는 생성자를 만들어준다.
//@AllArgsConstructor
// 현재 클래스가 가지고 있는 모든 필드에 대한 Getter를 생성해주는 어노테이션, (boolean에 대해서는 getxxX가 아니라 isxxX가 된다.
@Getter
public class MealHistory {

    // 현재 테이블의 PK를 암시하는 어노테이션 @ID
    @Id
    private UUID id;

    // jpa에게 java필드를 어떤 DB column에 매핑할지 알려주는 설정
    @Column(name = "food_name", nullable = false, length = 100)
    private String foodName;

    @Column(length = 50)
    private String category;

    private Integer price;

    private Integer rating;

    private String memo;

    @Column(name = "would_eat_again")
    private Boolean wouldEatAgain;

    @Column(name = "is_public", nullable = false)
    private boolean publicRecord;

    @Column(name = "ate_at", nullable = false)
    // LocalDate 자료형은 단순히 날짜만 저장하는 자료형이다.
    private LocalDate ateAt;

    @Column(name = "created_at", nullable = false)
    // OffsetDateTime 자료형은 날짜뿐만 아니라, 시간, UTC Offset을 함께 저장한다. 이는 PostgreDB의 TIMESTAMPTZ와 연결되는 자료형이다.
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // JPA는 DB 데이터를 Entity로 변환할 때 매개변수가 없는 생성자가 필요하다. 이를 위해 선언하는 기본 생성자
    // 추가로 생성자와 동일한 이름으로써, 아무런 값도 넣지않고 생성자를 호출할 경우 해당 기본 생성자가 실행되어 생성을 방지
    protected MealHistory() {
    }

    // 생성자 선언 (단, createAt 과 updateAt은 제외)
    public MealHistory(
        String foodName,
        String category,
        Integer price,
        Integer rating,
        String memo,
        Boolean wouldEatAgain,
        boolean publicRecord,
        LocalDate ateAt
    ) {
        this.foodName = foodName;
        this.category = category;
        this.price = price;
        this.rating = rating;
        this.memo = memo;
        this.wouldEatAgain = wouldEatAgain;
        this.publicRecord = publicRecord;
        this.ateAt = ateAt;
    }

    // update 요청이 들어왔을 때 실행할 메서드 update
    // 현재 들어온 값이 null이 아닌 필드들을 갱신한다.
    public void update(
            String foodName,
            String category,
            Integer price,
            Integer rating,
            String memo,
            Boolean wouldEatAgain,
            Boolean publicRecord,
            LocalDate ateAt
    ) {
        this.foodName = foodName;
        this.category = category;
        this.price = price;
        this.rating = rating;
        this.memo = memo;
        this.wouldEatAgain = wouldEatAgain;
        this.publicRecord = publicRecord;
        this.ateAt = ateAt;

    }

    // 글이 생성되었을때, 랜덤한 UUID와 생성 당시 날짜를 작성하는 메서드
    // @PrePersist 어노테이션은 Entity가 처음 DB로 저장되기 바로 직전에 실행되게 하는 어노테이션이다.
    // 즉, Entity가 DB에 저장되기전에, UUID를 통하여 랜덤한 id 값을 부여하고, 당시의 시간을 createAt과 updateAt에 할당하고 완성된 값을 DB에 넣는다.
    @PrePersist
    private void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        this.id = UUID.randomUUID();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Entity가 수정되어 update sql이 실행되게 된다면 @PreUpdate 어노테이션이 달린 메서드가 실행된다. 해당 값의 updateAt의 시간을 현재 시간으로 바꾼 후 요청을 진행
    @PreUpdate
    private void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }




}
