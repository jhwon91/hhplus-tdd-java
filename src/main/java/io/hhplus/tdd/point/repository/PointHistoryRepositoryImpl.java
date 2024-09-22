package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.PointHistory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PointHistoryRepositoryImpl implements PointHistoryRepository{

    private final PointHistoryTable pointHistoryTable;

    public PointHistoryRepositoryImpl(PointHistoryTable pointHistoryTable) {
        this.pointHistoryTable = pointHistoryTable;
    }

    @Override
    public List<PointHistory> findByUserId(Long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    @Override
    public PointHistory save(PointHistory pointHistory) {
        return pointHistoryTable.insert(
                pointHistory.userId(),
                pointHistory.amount(),
                pointHistory.type(),
                pointHistory.updateMillis()
        );
    }
}
