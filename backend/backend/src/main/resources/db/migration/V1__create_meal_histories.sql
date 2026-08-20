CREATE TABLE meal_histories
(
    id UUID PRIMARY KEY,

    food_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price INTEGER,
    rating INTEGER,
    memo TEXT,

    would_eat_again BOOLEAN,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    ate_at DATE NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_meal_histories_price
        CHECK (price IS NULL OR price >= 0),

    CONSTRAINT chk_meal_histories_rating
        CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)

);