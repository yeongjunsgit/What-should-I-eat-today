# API/DB 설계 정리

작성일: 2026-07-14

## 1. 설계 기준

이번 MVP의 기준은 `Next.js 프론트엔드 + Java/Spring Boot 백엔드 + PostgreSQL DB + JWT 로그인`이다.

핵심 방향:

- 프론트엔드는 Spring Boot REST API만 호출한다.
- 로그인 후 필요한 API는 `Authorization: Bearer {accessToken}` 방식으로 보호한다.
- `userId`는 요청 body로 받지 않고, 서버가 JWT에서 로그인 사용자를 식별한다.
- AI API Key, DB 비밀번호, JWT Secret은 백엔드/인프라에서만 관리한다.
- PostgreSQL의 `jsonb`는 AI 응답, 태그 배열, 스냅샷처럼 구조가 유동적인 데이터에 사용한다.
- 외부 음식점 검색 결과는 기본적으로 DB에 저장하지 않고 API 응답으로만 사용한다.

## 2. 설계 원칙과 이유

| 원칙 | 이유 |
| --- | --- |
| 인증이 필요한 데이터는 JWT 기준으로 사용자 식별 | 클라이언트가 임의로 `userId`를 보내면 다른 사람 데이터에 접근할 위험이 있음 |
| 추천 요청과 추천 음식 후보를 분리 | 추천 1회에 여러 음식 후보가 나올 수 있으므로 `recommendations`와 `recommended_foods`를 분리하는 편이 자연스러움 |
| 실제 먹은 기록은 `meal_histories`에 저장 | 추천받은 음식과 실제 먹은 음식은 다를 수 있으므로 별도 기록이 필요함 |
| 알레르기/못 먹는 재료는 별도 테이블로 분리 | 사용자는 여러 알레르기와 여러 기피 재료를 가질 수 있으므로 1:N 구조가 적합함 |
| AI 분석 결과는 스냅샷으로 저장 | 나중에 DB 데이터가 바뀌어도 당시 분석 결과를 그대로 다시 볼 수 있음 |
| 먹패턴 분석은 이력 저장 + 최신 조회 방식 사용 | 사용자는 최신 분석을 빠르게 보고, 필요하면 과거 분석도 다시 볼 수 있음 |
| soft delete는 선택 사항으로 둠 | 복구/감사/이력 보존이 필요하면 사용하고, 단순 MVP라면 hard delete로 시작 가능 |

## 3. API 설계

### 3.1 인증/회원 API

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | 불필요 | 회원가입 |
| POST | `/api/auth/login` | 불필요 | 로그인, Access Token/Refresh Token 발급 |
| POST | `/api/auth/refresh` | 불필요 | Refresh Token으로 Access Token 재발급 |
| POST | `/api/auth/logout` | 필요 | 로그아웃, Refresh Token 폐기 처리 |
| GET | `/api/users/me` | 필요 | 내 정보 조회 |
| PATCH | `/api/users/me` | 필요 | 내 정보 수정 |

회원가입 Request 예시:

```json
{
  "email": "user@example.com",
  "password": "password1234",
  "nickname": "밥고민러",
  "allergies": ["새우", "땅콩"],
  "dislikedIngredients": ["고수"]
}
```

로그인 Response 예시:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "nickname": "밥고민러"
  }
}
```

이유:

- Access Token은 일반 API 인증에 사용한다.
- Refresh Token은 Access Token 재발급과 로그아웃 관리를 위해 DB에 hash 형태로 저장한다.
- 로그아웃 시 Refresh Token을 `revoked=true`로 처리하면, 이미 발급된 Refresh Token을 더 이상 사용할 수 없게 만들 수 있다.

### 3.2 AI 추천 API

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/recommendations` | 필요 | AI 음식 추천 생성 |
| GET | `/api/recommendations` | 필요 | 내 추천 기록 목록 조회 |
| GET | `/api/recommendations/{recommendationId}` | 필요 | 추천 기록 상세 조회 |
| POST | `/api/recommendations/{recommendationId}/foods/{foodId}/save-to-history` | 필요 | 추천받은 음식을 먹히스토리로 저장 |

추천 생성 Request 예시:

```json
{
  "mood": "피곤함",
  "dailyEvent": "회의가 많아서 지쳤고 점심을 제대로 못 먹었어.",
  "desiredMealMoods": ["따뜻한", "든든한"],
  "budget": 12000,
  "mealType": "DINNER",
  "avoidFoods": ["김치찌개"]
}
```

서버가 자동으로 참고할 데이터:

- 로그인 사용자 정보
- 사용자의 알레르기
- 사용자의 못 먹는 재료
- 최근 먹히스토리
- 최근 추천 기록

추천 생성 Response 예시:

```json
{
  "recommendationId": "uuid",
  "contextAnalysis": {
    "emotion": "피곤함",
    "situationSummary": "회의가 많고 점심을 제대로 먹지 못해 회복감 있는 식사가 필요한 상태",
    "recommendedDirection": "따뜻하고 든든하지만 최근 반복된 매운맛은 줄이는 방향"
  },
  "recommendedFoods": [
    {
      "id": "uuid",
      "rank": 1,
      "name": "소고기 쌀국수",
      "category": "베트남 음식",
      "finalScore": 91,
      "reason": "따뜻한 국물과 든든한 고기가 있어 피곤한 저녁에 잘 맞습니다.",
      "tasteTags": ["따뜻함", "담백함", "국물", "든든함"],
      "estimatedPrice": "10000~12000원"
    }
  ]
}
```

이유:

- 사용자가 매번 알레르기/최근 기록을 직접 보내지 않아도 된다.
- 서버가 로그인 사용자 기준으로 필요한 맥락을 모아 AI에게 전달한다.
- 추천 결과는 나중에 다시 볼 수 있도록 저장한다.

### 3.3 먹히스토리 API

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/meal-histories` | 필요 | 먹히스토리 직접 생성 |
| GET | `/api/meal-histories` | 필요 | 내 먹히스토리 목록 조회 |
| GET | `/api/meal-histories/{mealHistoryId}` | 필요 | 먹히스토리 상세 조회 |
| PATCH | `/api/meal-histories/{mealHistoryId}` | 필요 | 먹히스토리 수정 |
| DELETE | `/api/meal-histories/{mealHistoryId}` | 필요 | 먹히스토리 삭제 |

생성 Request 예시:

```json
{
  "foodName": "김치찌개",
  "category": "한식",
  "price": 10000,
  "rating": 5,
  "memo": "오늘 날씨랑 잘 어울렸음",
  "tasteTags": ["매운맛", "국물", "든든함"],
  "recommendBased": false,
  "wouldEatAgain": true,
  "isPublic": false,
  "ateAt": "2026-07-14"
}
```

목록 조회 Query 예시:

```text
from=2026-07-01
to=2026-07-31
category=한식
page=0
size=20
```

이유:

- `price`, `rating`, `tasteTags`, `recommendBased`, `ateAt`은 나중에 통계를 만들기 위한 핵심 값이다.
- `isPublic=true`인 기록만 다른 사용자에게 랜덤으로 노출한다.
- 추천 결과에서 저장한 기록은 `recommendation_id`, `recommended_food_id`로 연결한다.

### 3.4 통계/먹패턴 API

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/statistics/meal-pattern` | 필요 | 식사 통계 대시보드 |
| POST | `/api/statistics/meal-pattern/analyze` | 필요 | AI 먹패턴 분석 생성 |
| GET | `/api/statistics/meal-pattern/reports/latest` | 필요 | 최신 AI 먹패턴 분석 조회 |
| GET | `/api/statistics/meal-pattern/reports` | 필요 | AI 먹패턴 분석 기록 목록 |
| GET | `/api/statistics/meal-pattern/reports/{id}` | 필요 | AI 먹패턴 분석 상세 |
| DELETE | `/api/statistics/meal-pattern/reports/{id}` | 필요 | AI 먹패턴 분석 삭제 (`soft delete` 선택 시 `deleted_at` 기록) |

통계 조회 Query 예시:

```text
period=MONTH
from=2026-07-01
to=2026-07-31
```

AI 분석 생성 Request 예시:

```json
{
  "period": "MONTH",
  "from": "2026-07-01",
  "to": "2026-07-31"
}
```

AI 분석 생성 처리 규칙:

- 같은 `user_id + period_type + period_start + period_end` 기준 기존 최신 리포트는 `is_latest=false`로 변경한다.
- 새 리포트는 `version=이전 version+1`, `is_latest=true`로 저장한다.
- `dashboard_snapshot`과 `ai_profile`은 생성 시점의 결과를 스냅샷으로 저장한다.
- 기본 조회에서는 `deleted_at is null` 조건을 사용한다. (`soft delete` 선택 시)

이유:

- 같은 기간을 여러 번 분석해도 과거 결과를 잃지 않는다.
- 화면에서는 `reports/latest`를 호출해 최신 분석만 빠르게 보여줄 수 있다.
- 사용자가 과거 분석을 보고 싶을 때는 `reports` 목록을 제공할 수 있다.

### 3.5 인기 음식/공개 기록/음식점 API

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/popular-foods` | 선택 | 이달의 인기 음식 조회 |
| GET | `/api/public-meal-histories/random` | 선택 | 공개된 다른 사람 먹히스토리 랜덤 조회 |
| GET | `/api/restaurants` | 선택 | 추천 음식 기반 근처 음식점 검색 |

공개 먹히스토리 랜덤 조회 Query 예시:

```text
size=10
```

서버 조건:

- `meal_histories.is_public = true`인 기록만 조회한다.
- 탈퇴/비활성 사용자 기록은 제외한다.
- 응답에는 `userId`, `email` 같은 개인정보를 포함하지 않는다.

음식점 검색 Query 예시:

```text
keyword=김치찌개
lat=37.5665
lng=126.9780
```

이유:

- 공개 기록 랜덤 조회는 개인정보를 숨기는 것이 중요하다.
- 음식점 검색 결과는 외부 지도 API 결과이므로 MVP에서는 DB 저장 없이 응답으로만 사용한다.
- 인기 음식은 MVP에서는 수동 seed 데이터로 시작하고, 이후 batch로 자동 집계할 수 있다.

## 4. 공통 에러 응답

```json
{
  "code": "VALIDATION_ERROR",
  "message": "요청값이 올바르지 않습니다.",
  "errors": [
    {
      "field": "email",
      "reason": "이메일 형식이 올바르지 않습니다."
    }
  ]
}
```

대표 에러 코드:

| 코드 | 의미 |
| --- | --- |
| `VALIDATION_ERROR` | 요청값 검증 실패 |
| `UNAUTHORIZED` | 로그인 필요 |
| `FORBIDDEN` | 권한 없음 |
| `NOT_FOUND` | 리소스 없음 |
| `DUPLICATED_EMAIL` | 이미 가입된 이메일 |
| `INVALID_PASSWORD` | 비밀번호 불일치 |
| `AI_API_ERROR` | AI API 호출 실패 |
| `EXTERNAL_API_ERROR` | 지도/장소 API 호출 실패 |

## 5. DB 설계

타입 기준:

- `uuid`: 기본 식별자
- `varchar`: 짧은 문자열
- `text`: 긴 문자열
- `int`: 숫자
- `boolean`: 참/거짓
- `date`: 날짜
- `timestamptz`: 시간대 포함 일시
- `jsonb`: 배열 또는 AI 응답처럼 유연한 JSON 데이터

### 5.1 users

사용자 계정 정보.

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `email` | varchar | unique, not null |
| `password_hash` | varchar | not null. 암호화된 비밀번호 |
| `nickname` | varchar | not null |
| `role` | varchar | not null, default `USER` |
| `created_at` | timestamptz | not null |
| `updated_at` | timestamptz | not null |

이유:

- 비밀번호 원문은 절대 저장하지 않는다.
- `password_hash`에는 BCrypt 등으로 암호화된 값만 저장한다.
- `role`은 MVP에서는 `USER`만 사용해도 되지만, 나중에 관리자 기능을 붙이기 쉽다.

### 5.2 user_allergies

사용자의 알레르기 정보.

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `user_id` | uuid | users 테이블 id 참조 |
| `name` | varchar | not null |

이유:

- 한 사용자가 여러 알레르기를 가질 수 있으므로 별도 테이블이 적합하다.
- `users` 테이블에 배열로 넣는 것보다 검색, 수정, 삭제가 명확하다.

### 5.3 user_disliked_ingredients

사용자가 못 먹거나 싫어하는 재료.

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `user_id` | uuid | users 테이블 id 참조 |
| `name` | varchar | not null |

이유:

- 알레르기는 건강/안전 문제이고, 기피 재료는 취향 문제이므로 분리한다.
- 추천 프롬프트에서도 두 값을 다르게 다룰 수 있다.

### 5.4 refresh_tokens

로그인 유지와 토큰 재발급을 위한 테이블.

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `user_id` | uuid | users 테이블 id 참조 |
| `token_hash` | varchar | not null. Refresh Token hash |
| `expires_at` | timestamptz | not null |
| `revoked` | boolean | not null, default false |
| `created_at` | timestamptz | not null |

이유:

- Refresh Token 원문 대신 hash를 저장하면 DB가 유출되더라도 피해를 줄일 수 있다.
- 로그아웃 시 `revoked=true`로 바꿔 재사용을 막는다.
- 만료된 토큰은 주기적으로 정리하거나 로그인/재발급 시점에 정리할 수 있다.

### 5.5 recommendations

AI 추천 1회 요청의 상위 기록.

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `user_id` | uuid | users 테이블 id 참조 |
| `mood` | varchar | 사용자의 기분 |
| `daily_event` | text | 오늘 있었던 일 |
| `desired_meal_moods` | jsonb | 원하는 식사 느낌 배열 |
| `budget` | int | 예산 |
| `meal_type` | varchar | BREAKFAST, LUNCH, DINNER 등 |
| `avoid_foods` | jsonb | 피하고 싶은 음식 배열 |
| `context_analysis` | jsonb | AI 상황 분석 결과 |
| `avoided_patterns` | jsonb | 제외한 패턴과 이유 |
| `created_at` | timestamptz | not null |

이유:

- 추천 요청 자체의 맥락을 저장한다.
- AI가 어떤 상황 분석을 했는지 남겨두면 추천 결과를 다시 설명하기 좋다.
- `desired_meal_moods`, `avoid_foods`는 MVP에서는 `jsonb`로 빠르게 구현한다.

### 5.6 recommended_foods

추천 1회에서 나온 음식 후보들.

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `recommendation_id` | uuid | recommendations 테이블 id 참조 |
| `rank` | int | not null. 추천 순위 |
| `name` | varchar | not null. 음식명 |
| `category` | varchar | 음식 카테고리 |
| `reason` | text | 추천 이유 |
| `estimated_price` | varchar | 예상 가격대 |
| `final_score` | int | 최종 추천 점수 |
| `scores` | jsonb | 세부 점수 |
| `taste_tags` | jsonb | 맛 태그 배열 |
| `created_at` | timestamptz | not null |

이유:

- 추천 후보가 3개라면 `recommended_foods`에 3행이 저장된다.
- 추천 요청과 음식 후보를 분리하면 후보별 저장/선택/히스토리 연결이 쉬워진다.

### 5.7 meal_histories

사용자가 실제로 먹은 음식 기록.

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `user_id` | uuid | users 테이블 id 참조 |
| `recommendation_id` | uuid | nullable, recommendations 테이블 id 참조 |
| `recommended_food_id` | uuid | nullable, recommended_foods 테이블 id 참조 |
| `food_name` | varchar | not null. 음식명 |
| `category` | varchar | 음식 카테고리 |
| `price` | int | 가격 |
| `rating` | int | 만족도 |
| `memo` | text | 메모 |
| `taste_tags` | jsonb | 맛 태그 배열 |
| `recommend_based` | boolean | not null, default false |
| `would_eat_again` | boolean | 다시 먹고 싶은지 여부 |
| `is_public` | boolean | not null, default false |
| `ate_at` | date | not null. 먹은 날짜 |
| `created_at` | timestamptz | not null |
| `updated_at` | timestamptz | not null |

이유:

- 직접 기록한 음식이면 `recommendation_id`, `recommended_food_id`는 null 가능하다.
- 추천 결과에서 저장한 음식이면 두 id를 연결한다.
- `is_public=true`인 기록만 다른 사람에게 랜덤 노출한다.
- `taste_tags`, `rating`, `price`, `ate_at`은 통계와 AI 먹패턴 분석의 핵심 데이터다.

### 5.8 meal_pattern_reports

AI 먹패턴 분석 결과 저장.

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `user_id` | uuid | users 테이블 id 참조 |
| `period_type` | varchar | not null. WEEK, MONTH 등 |
| `period_start` | date | not null |
| `period_end` | date | not null |
| `version` | int | not null, default 1. 같은 기간 분석 버전 |
| `is_latest` | boolean | not null, default true. 최신 리포트 여부 |
| `dashboard_snapshot` | jsonb | not null. 통계 스냅샷 |
| `ai_profile` | jsonb | not null. AI 분석 결과 |
| `created_at` | timestamptz | not null |
| `updated_at` | timestamptz | not null. 최신 여부 변경 시 갱신 |
| `deleted_at` | timestamptz | nullable (`soft delete` 선택 시 사용) |

이유:

- 같은 사용자/기간에 새 분석을 생성하면 기존 최신 리포트는 `is_latest=false`가 된다.
- 최신 리포트 조회는 `is_latest=true` 조건을 사용한다.
- 기본 조회에서는 `deleted_at is null` 조건을 사용한다. (`soft delete` 선택 시)
- hard delete를 선택하면 `deleted_at` 컬럼 없이 실제 삭제해도 된다.

### 5.9 popular_foods

이달의 인기 음식 데이터.

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `month` | varchar | not null. 기준 월 |
| `food_name` | varchar | not null. 음식명 |
| `category` | varchar | 음식 카테고리 |
| `selection_count` | int | not null, default 0 |
| `avg_price` | int | 평균 가격 |
| `rank` | int | 인기 순위 |
| `created_at` | timestamptz | not null |
| `updated_at` | timestamptz | not null |

이유:

- MVP에서는 batch 없이 수동 seed 데이터로 시작해도 된다.
- 이후 `meal_histories` 기반으로 월별 집계해서 업데이트할 수 있다.
- `month`를 저장하면 월별 인기 음식 화면을 만들기 쉽다.

## 6. 주요 관계 정리

```text
users 1 - N user_allergies
users 1 - N user_disliked_ingredients
users 1 - N refresh_tokens
users 1 - N recommendations
users 1 - N meal_histories
users 1 - N meal_pattern_reports

recommendations 1 - N recommended_foods
recommendations 1 - N meal_histories
recommended_foods 1 - N meal_histories
```

## 7. 구현 우선순위

### 1단계: 인증/회원

- `users`
- `refresh_tokens`
- 회원가입
- 로그인
- Access Token/Refresh Token
- 내 정보 조회/수정

### 2단계: AI 추천

- `recommendations`
- `recommended_foods`
- AI 추천 생성 API
- 추천 기록 목록/상세 조회

### 3단계: 먹히스토리

- `meal_histories`
- 먹히스토리 CRUD
- 추천 음식 먹히스토리 저장
- 공개 여부 설정

### 4단계: 통계/먹패턴 분석

- 식사 통계 대시보드
- `meal_pattern_reports`
- AI 먹패턴 분석 생성
- latest report 조회
- report version 관리
- report 삭제 처리 (`soft delete` 선택 시 `deleted_at` 사용)

### 5단계: 부가 기능

- `popular_foods`
- 공개 먹히스토리 랜덤 조회
- 근처 음식점 검색
- 인기 음식 seed 데이터
- 추후 batch 집계

## 8. 나중에 확정하면 좋은 선택

| 선택 지점 | MVP 추천 | 나중에 확장 |
| --- | --- | --- |
| 배열 데이터 저장 | `jsonb` 사용 | 통계 쿼리가 복잡해지면 별도 테이블 분리 |
| 인기 음식 데이터 | 수동 seed | batch로 자동 집계 |
| 먹패턴 분석 삭제 | hard delete 또는 soft delete 중 선택 | 복구/감사가 필요하면 soft delete |
| Refresh Token 만료 | 3시간-7일 사이에서 UX/보안 기준으로 선택 | 기기별 세션 관리 추가 |
| 지도 API | 하나만 선택해서 연동 | Kakao/Naver/Google 중 복수 제공 |

## 9. 용어 메모

- Batch: 정해진 시간에 자동으로 실행되는 작업. 예를 들어 매일 새벽에 먹히스토리를 집계해서 인기 음식을 업데이트하는 작업.
- Seed 데이터: 개발자가 미리 넣어두는 초기 데이터. 예를 들어 MVP에서 인기 음식 목록을 직접 넣어두는 방식.
- Soft delete: 실제 데이터를 지우지 않고 `deleted_at` 같은 값만 기록해서 삭제된 것처럼 처리하는 방식.
- Hard delete: DB row를 실제로 삭제하는 방식.
- Snapshot: 특정 시점의 결과를 그대로 저장해두는 방식. 나중에 원본 데이터가 바뀌어도 당시 결과를 다시 볼 수 있다.
