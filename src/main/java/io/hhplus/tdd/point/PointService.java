package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {
    private static final long MAX_POINT_BALANCE = 1000L; // 최대 잔고 제한 설정

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointService(UserPointRepository userPointRepository, PointHistoryRepository pointHistoryRepository) {
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }
    /*
    *  동시에 여러 건의 포인트 충전, 이용 요청이 들어올 경우 순차적으로 처리되어야 합니다. (X)
    */

    // 특정 사용자의 포인트 충전 로직 (포인트를 충전한다.)
    public UserPoint chargeUserPoint(long userId, long amount) {
        UserPoint userPoint = userPointRepository.findById(userId);

        if (amount <= 0) {
            throw new IllegalArgumentException("포인트 충전은 0 보다 커야 합니다.");
        }

        if (userPoint.point() + amount > MAX_POINT_BALANCE) {
            throw new IllegalArgumentException("최대 잔고를 초과 했습니다.");
        }

        UserPoint updateUserPoint = new UserPoint(userPoint.id(), userPoint.point() + amount, System.currentTimeMillis());
        userPointRepository.save(updateUserPoint);
        pointHistoryRepository.save(new PointHistory(0,userId, amount, TransactionType.CHARGE, System.currentTimeMillis()));

        return updateUserPoint;
    }

    // 특정 사용자의 포인트 사용 로직 (포인트를 사용한다.)
    // 잔고가 부족할 경우, 포인트 사용은 실패하여야 합니다.
    public UserPoint useUserPoint(long userId, long amount) {
        UserPoint userPoint = userPointRepository.findById(userId);

        if (amount <= 0) {
            throw new IllegalArgumentException("포인트 사용은 0 보다 커야 합니다.");
        }

        if (userPoint.point() < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        UserPoint updateUserPoint = new UserPoint(userPoint.id(), userPoint.point() - amount, System.currentTimeMillis());
        userPointRepository.save(updateUserPoint);
        pointHistoryRepository.save(new PointHistory(0,userId, amount, TransactionType.USE, System.currentTimeMillis()));

        return  updateUserPoint;
    }

    // 특정 사용자의 포인트 조회 로직
    public UserPoint getUserPoint(long userId) {
        return userPointRepository.findById(userId);
    }

    // 특정 사용자의 포인트 내역 조회(포인트 내역을 조회한다.)
    public List<PointHistory> getUserPointHistory(long userId) {
        return pointHistoryRepository.findByUserId(userId);
    }
}
