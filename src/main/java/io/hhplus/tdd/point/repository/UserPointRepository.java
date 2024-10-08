package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.UserPoint;

import java.util.Optional;

public interface UserPointRepository {
    UserPoint findById(Long id);
    UserPoint save(UserPoint userPoint);
}
