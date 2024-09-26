package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PointControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private ExecutorService executorService;

    @BeforeEach
    public void setUp() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    @DirtiesContext
    public void 여러_사용자의_동시_충전_요청() throws Exception {
        int userCount = 10;
        long chargeAmount = 100L;
        CountDownLatch latch = new CountDownLatch(userCount);

        List<Future<MvcResult>> futures = new ArrayList<>();
        for (long userId = 1; userId <= userCount; userId++) {
            long finalUserId = userId;
            futures.add(executorService.submit(() -> {
                try {
                    return mockMvc.perform(patch("/point/" + finalUserId + "/charge")
                                    .contentType("application/json")
                                    .content(String.valueOf(chargeAmount)))
                            .andExpect(status().isOk())
                            .andReturn();
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await();

        for (int i = 0; i < userCount; i++) {
            MvcResult result = futures.get(i).get();
            assertNotNull(result);
            assertEquals(200, result.getResponse().getStatus());

            long userId = i + 1;
            UserPoint userPoint = pointService.getUserPoint(userId);
            assertEquals(chargeAmount, userPoint.point());
        }
    }

    @Test
    @DirtiesContext
    public void 동일한_사용자에_대한_동시_충전_요청의_순차적_처리() throws Exception {
        long userId = 1L;
        int requestCount = 5;
        long chargeAmount = 100L;
        CountDownLatch latch = new CountDownLatch(requestCount);

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/" + userId + "/charge")
                                    .contentType("application/json")
                                    .content(String.valueOf(chargeAmount)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        UserPoint userPoint = pointService.getUserPoint(userId);
        assertEquals(chargeAmount * requestCount, userPoint.point());
    }

    @Test
    @DirtiesContext
    public void 잔액_부족_상황에서의_동시_사용_요청_처리를_검증() throws Exception {
        long userId = 2L;
        long initialBalance = 500L;
        long useAmount = 300L;

        // 초기 잔액 설정
        pointService.chargeUserPoint(userId, initialBalance);

        CountDownLatch latch = new CountDownLatch(2);
        List<Future<MvcResult>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    return mockMvc.perform(patch("/point/" + userId + "/use")
                                    .contentType("application/json")
                                    .content(String.valueOf(useAmount)))
                            .andReturn();
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await();

        int successCount = 0;
        int failCount = 0;
        for (Future<MvcResult> future : futures) {
            MvcResult result = future.get();
            if (result.getResponse().getStatus() == 200) {
                successCount++;
            } else {
                failCount++;
            }
        }

        assertEquals(1, successCount);
        assertEquals(1, failCount);

        UserPoint finalUserPoint = pointService.getUserPoint(userId);
        assertEquals(200L, finalUserPoint.point());
    }

    @Test
    @DirtiesContext
    public void 충전과_사용_요청의_동시처리와_순서보장을_확인() throws Exception {
        long userId = 3L;
        long initialBalance = 500L;
        long chargeAmount = 300L;
        long useAmount = 200L;

        // 초기 잔액 설정
        pointService.chargeUserPoint(userId, initialBalance);

        CountDownLatch latch = new CountDownLatch(2);
        Future<MvcResult> chargeFuture = executorService.submit(() -> {
            try {
                return mockMvc.perform(patch("/point/" + userId + "/charge")
                                .contentType("application/json")
                                .content(String.valueOf(chargeAmount)))
                        .andReturn();
            } finally {
                latch.countDown();
            }
        });

        Future<MvcResult> useFuture = executorService.submit(() -> {
            try {
                return mockMvc.perform(patch("/point/" + userId + "/use")
                                .contentType("application/json")
                                .content(String.valueOf(useAmount)))
                        .andReturn();
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        MvcResult chargeResult = chargeFuture.get();
        MvcResult useResult = useFuture.get();

        assertEquals(200, chargeResult.getResponse().getStatus());
        assertEquals(200, useResult.getResponse().getStatus());

        UserPoint finalUserPoint = pointService.getUserPoint(userId);
        assertEquals(600L, finalUserPoint.point());
    }

    @Test
    @DirtiesContext
    public void 다수_사용자의_동시_요청_성능검증() throws Exception {
        int userCount = 10;
        long initialBalance = 200L;
        long chargeAmount = 100L;
        long useAmount = 50L;
        CountDownLatch latch = new CountDownLatch(userCount * 2);  // 충전과 사용 각각 10번

        long startTime = System.currentTimeMillis();

        for (long userId = 1; userId <= userCount; userId++) {
            long finalUserId = userId;
            pointService.chargeUserPoint(finalUserId, initialBalance);
            executorService.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/" + finalUserId + "/charge")
                                    .contentType("application/json")
                                    .content(String.valueOf(chargeAmount)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });

            executorService.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/" + finalUserId + "/use")
                                    .contentType("application/json")
                                    .content(String.valueOf(useAmount)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("총시: " + duration + " ms");

        for (long userId = 1; userId <= userCount; userId++) {
            UserPoint userPoint = pointService.getUserPoint(userId);
            assertEquals(initialBalance + chargeAmount - useAmount, userPoint.point());
        }
    }
}