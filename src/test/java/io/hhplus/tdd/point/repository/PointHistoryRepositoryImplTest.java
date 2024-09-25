package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointHistoryRepositoryImplTest {

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointHistoryRepositoryImpl pointHistoryRepository;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 포인트_내역_저장() {
        // given
        long userId = 1L;
        long amount = 500L;
        TransactionType type = TransactionType.CHARGE;

        PointHistory pointHistory = new PointHistory(1, userId, amount, type, System.currentTimeMillis());
        when(pointHistoryTable.insert(userId, amount, type, pointHistory.updateMillis())).thenReturn(pointHistory);

        // when
        PointHistory result = pointHistoryRepository.save(pointHistory);

        // then
        assertEquals(pointHistory, result);
        verify(pointHistoryTable).insert(userId, amount, type, pointHistory.updateMillis());
    }

    @Test
    void 포인트_내역_조회() {
        // given
        long userId = 1L;

        PointHistory history1 = new PointHistory(0, userId, 500L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory history2 = new PointHistory(0, userId, 300L, TransactionType.USE, System.currentTimeMillis());
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(List.of(history1, history2));
        // when
        List<PointHistory> result = pointHistoryRepository.findByUserId(userId);

        // then
        assertEquals(2, result.size());
        assertEquals(history1, result.get(0));
        assertEquals(history2, result.get(1));

        verify(pointHistoryTable).selectAllByUserId(userId);

    }
}