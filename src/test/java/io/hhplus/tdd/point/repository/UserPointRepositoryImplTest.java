package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPointRepositoryImplTest {

    @Mock
    private UserPointTable userPointTable;

    @InjectMocks
    private UserPointRepositoryImpl userPointRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 포인트_저장() {
        //given
        long userId = 1L;
        long amount = 1000L;

        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        when(userPointTable.insertOrUpdate(userId, amount)).thenReturn(userPoint);

        //when
        UserPoint result = userPointRepository.save(userPoint);

        //then
        assertEquals(userPoint, result);
        verify(userPointTable).insertOrUpdate(userId, amount);
    }

    @Test
    void 포인트_조회() {
        //given
        long userId = 1L;
        long amount = 1000L;
        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        //when
        UserPoint result = userPointRepository.findById(userId);

        //then
        assertEquals(userPoint, result);
        verify(userPointTable).selectById(userId);

    }
}