package io.hhplus.tdd.point;

public record PointHistory(
        Long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {

    // id가 있는 경우의 생성자
    public PointHistory(Long id, long userId, long amount, TransactionType type, long updateMillis) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.updateMillis = updateMillis;
    }

    // id 없이 객체를 생성하는 생성자 (id는 나중에 자동 생성)
    public PointHistory(long userId, long amount, TransactionType type, long updateMillis) {
        this(null, userId, amount, type, updateMillis);  // id는 null로 설정
    }
}
