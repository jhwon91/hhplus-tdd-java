package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class PointControllerTest {

    @Mock
    private PointService pointService; // Mock 서비스 계층

    @InjectMocks
    private PointController pointController; // 실제 컨트롤러 테스트

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); // @Mock, @InjectMocks 활성화
        mockMvc = MockMvcBuilders.standaloneSetup(pointController).build(); // MockMvc 설정
    }

    @Test
    public void 포인트_조회_테스트() throws Exception {
        // given
        UserPoint mockUserPoint = new UserPoint(1L, 100L, System.currentTimeMillis());
        when(pointService.getUserPoint(anyLong())).thenReturn(mockUserPoint);

        // when
        mockMvc.perform(get("/point/1"))
                .andExpect(status().isOk()) // HTTP 200 OK 기대
                .andExpect(jsonPath("$.id").value(1L)) // 반환된 UserPoint의 id 확인
                .andExpect(jsonPath("$.point").value(100L)); // 반환된 포인트 확인

        // then
        verify(pointService, times(1)).getUserPoint(anyLong());
    }

    @Test
    public void 포인트_내역_조회_테스트() throws Exception {
        // given
        PointHistory history1 = new PointHistory(0, 1, 500L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory history2 = new PointHistory(0, 1, 300L, TransactionType.USE, System.currentTimeMillis());

        when(pointService.getUserPointHistory(1)).thenReturn(List.of(history1, history2));

        // when
        mockMvc.perform(get("/point/1/histories"))
                .andDo(print())  // 응답 본문을 출력하여 JSON 구조 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(500L)) // 첫 번째 히스토리의 금액 확인 (500L)
                .andExpect(jsonPath("$[0].type").value("CHARGE")) // 첫 번째 히스토리의 타입 확인
                .andExpect(jsonPath("$[1].amount").value(300L)) // 두 번째 히스토리의 금액 확인 (300L)
                .andExpect(jsonPath("$[1].type").value("USE")); // 두 번째 히스토리의 타입 확인

        // then
        verify(pointService, times(1)).getUserPointHistory(anyLong());
    }

    @Test
    public void 포인트_충전_테스트() throws Exception {
        // Given: 서비스에서 반환될 객체 정의
        UserPoint mockUserPoint = new UserPoint(1L, 200L, System.currentTimeMillis());
        when(pointService.chargeUserPoint(anyLong(), anyLong())).thenReturn(mockUserPoint);

        // When: PATCH 요청을 보냄
        mockMvc.perform(patch("/point/1/charge")
                        .contentType("application/json")
                        .content("100")) // 100 포인트 충전
                .andExpect(status().isOk()) // HTTP 200 OK 기대
                .andExpect(jsonPath("$.id").value(1L)) // 반환된 UserPoint의 id 확인
                .andExpect(jsonPath("$.point").value(200L)); // 충전 후 포인트 확인

        // Then: 서비스가 호출되었는지 확인
        verify(pointService, times(1)).chargeUserPoint(anyLong(), anyLong());
    }

    @Test
    public void 포인트_사용_테스트() throws Exception {
        // Given: 서비스에서 반환될 객체 정의
        UserPoint mockUserPoint = new UserPoint(1L, 50L, System.currentTimeMillis());
        when(pointService.useUserPoint(anyLong(), anyLong())).thenReturn(mockUserPoint);

        // When: PATCH 요청을 보냄
        mockMvc.perform(patch("/point/1/use")
                        .contentType("application/json")
                        .content("50")) // 50 포인트 사용 요청
                .andExpect(status().isOk()) // HTTP 200 OK 기대
                .andExpect(jsonPath("$.id").value(1L)) // 반환된 UserPoint의 id 확인
                .andExpect(jsonPath("$.point").value(50L)); // 사용 후 포인트 확인

        // Then: 서비스가 호출되었는지 확인
        verify(pointService, times(1)).useUserPoint(anyLong(), anyLong());
    }
}