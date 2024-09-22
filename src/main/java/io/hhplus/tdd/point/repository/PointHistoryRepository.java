package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.PointHistory;

import java.util.List;

public interface PointHistoryRepository {
    List<PointHistory> findByUserId(Long id);
    PointHistory save (PointHistory pointHistory);
}

