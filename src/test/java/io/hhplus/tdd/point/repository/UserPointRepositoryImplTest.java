package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserPointRepositoryImplTest {

    UserPointRepository  userPointRepository= new UserPointRepositoryImpl(new UserPointTable());

    @Test
    void findById() {
        long userId = 1L;
        long amount = 1000L;

        // 포인트 저장
        userPointRepository.save(new UserPoint(userId, amount, System.currentTimeMillis()));

        // 포인트 조회
        UserPoint userPoint = userPointRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("user not Found"));

        assertThat(amount).isEqualTo(userPoint.point());
    }

    @Test
    void save() {
        long userId = 1L;
        long initialAmount = 1000L;
        long updatedAmount = 2000L;

        // 초기 포인트 저장
        userPointRepository.save(new UserPoint(userId, initialAmount, System.currentTimeMillis()));
        userPointRepository.save(new UserPoint(userId, updatedAmount, System.currentTimeMillis()));

        // 포인트 조회
        UserPoint userPoint = userPointRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("user not Found"));

        assertThat(updatedAmount).isEqualTo(userPoint.point());
    }
}