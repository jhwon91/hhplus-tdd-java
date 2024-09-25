package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PointServiceTest {

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointService pointService;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void 포인트_충전() {
        //given
        long userId = 1L;
        long initialAmount = 1000L;
        long chargeAmount = 500L;

        UserPoint userPoint = new UserPoint(userId, initialAmount, System.currentTimeMillis());
        when(userPointRepository.findById(userId)).thenReturn(userPoint);

        //when
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount);

        //then
        assertEquals(initialAmount + chargeAmount, result.point());

        verify(userPointRepository).findById(userId);  // findById가 호출됐는지 검증
        verify(userPointRepository).save(any(UserPoint.class)); // 저장 메서드가 호출됐는지 확인
        verify(pointHistoryRepository).save(any(PointHistory.class)); // 히스토리 저장 메서드가 호출됐는지 확인
    }

    @Test
    public void 포인트_사용() {
        //given
        long userId = 1L;
        long amount = 1000L;
        long useAmount = 400L;

        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        when(userPointRepository.findById(userId)).thenReturn(userPoint);

        //when
        UserPoint result = pointService.useUserPoint(userId, useAmount);

        //then
        assertEquals(amount - useAmount,result.point());

        verify(userPointRepository).findById(userId);
        verify(userPointRepository).save(any(UserPoint.class));
        verify(pointHistoryRepository).save(any(PointHistory.class));
    }

    @Test
    public void 포인트_사용_잔액_부족_실패() {
        //given
        long userId = 1L;
        long amount = 100L;
        long useAmount = 400L;

        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        when(userPointRepository.findById(userId)).thenReturn(userPoint);

        //when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.useUserPoint(userId, useAmount);
        });

        //then
        assertEquals("잔액이 부족합니다.", exception.getMessage());
        verify(userPointRepository, never()).save(any(UserPoint.class));
        verify(pointHistoryRepository, never()).save(any(PointHistory.class));
    }

}