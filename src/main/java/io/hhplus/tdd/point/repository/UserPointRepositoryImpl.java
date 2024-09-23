package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class UserPointRepositoryImpl implements UserPointRepository{

    private final UserPointTable userPointTable;

    public UserPointRepositoryImpl(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    @Override
    public Optional<UserPoint> findById(Long id) {
        return Optional.ofNullable(userPointTable.selectById(id));
    }

    @Override
    public UserPoint save(UserPoint userPoint) {
        return userPointTable.insertOrUpdate(userPoint.id(),userPoint.point());
    }
}
