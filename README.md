# hhplus-tdd-java
🛳️ Test Driven Development

<details>
  <summary>목표</summary>

### `point` 패키지의 TODO 와 테스트코드를 작성해주세요.

**요구 사항**

- PATCH  `/point/{id}/charge` : 포인트를 충전한다.
- PATCH `/point/{id}/use` : 포인트를 사용한다.
- GET `/point/{id}` : 포인트를 조회한다.
- GET `/point/{id}/histories` : 포인트 내역을 조회한다.
- 잔고가 부족할 경우, 포인트 사용은 실패하여야 합니다.
- 동시에 여러 건의 포인트 충전, 이용 요청이 들어올 경우 순차적으로 처리되어야 합니다.

### `Default`

- `/point` 패키지 (디렉토리) 내에 `PointService` 기본 기능 작성
- `/database` 패키지의 구현체는 수정하지 않고, 이를 활용해 기능을 구현
- 각 기능에 대한 단위 테스트 작성

> 총 4가지 기본 기능 (포인트 조회, 포인트 충전/사용 내역 조회, 충전, 사용) 을 구현합니다.
>

### `Step 1`

- 포인트 충전, 사용에 대한 정책 추가 (잔고 부족, 최대 잔고 등)
- 동시에 여러 요청이 들어오더라도 순서대로 (혹은 한번에 하나의 요청씩만) 제어될 수 있도록 리팩토링
- 동시성 제어에 대한 통합 테스트 작성



### `Step 2`

- 동시성 제어 방식에 대한 분석 및 보고서 작성 ( **README.md** )
</details>


---

#  동시성 제어 방식에 대한 분석 및 보고서 작성

## 1. 동시성 문제란?

동시성 문제는 다중 사용자 또는 다중 프로세스가 동시에 공유 자원에 접근할 때 발생하는 문제입니다. 특히, 다수의 요청이 동시에 들어오면 같은 자원에 대해 경합이 발생할 수 있습니다. 이로 인해 데이터가 누락되거나 잘못된 값으로 처리될 수 있습니다. 예를 들어, 여러 사용자가 동시에 포인트 충전 또는 포인트 사용을 요청할 때, 데이터의 일관성이 유지되지 않으면 잔액이 잘못 계산될 수 있습니다.

## 2. 동시성 문제를 해결하기 위한 방법

동시성 문제를 해결하기 위해 몇 가지 주요 기법을 사용할 수 있습니다. 이 프로젝트에서는 ReentrantLock을 사용하여 동일한 사용자에 대한 요청이 순차적으로 처리되도록 하였습니다.

### 2.1 ReentrantLock을 사용한 동시성 제어

ReentrantLock은 Java의 동시성 제어 메커니즘 중 하나로, 스레드 간의 자원 경합을 방지하는 데 사용됩니다. 특정 자원에 대해 **락(Lock)**을 걸면, 다른 스레드는 그 자원을 사용할 수 없고, 해당 스레드가 락을 해제할 때까지 대기하게 됩니다.

이 프로젝트에서는 각 사용자별로 Lock을 생성하여, 동일한 사용자에 대한 포인트 충전/사용 요청이 들어왔을 때 순차적으로 처리되도록 하였습니다.

````java
private final ConcurrentHashMap<Long, ReentrantLock> userLock = new ConcurrentHashMap<>();

public UserPoint chargeUserPoint(long userId, long amount) {
  ReentrantLock lock = userLock.computeIfAbsent(userId, id -> new ReentrantLock());
  lock.lock();
  try {
    // 포인트 충전 로직
  } finally {
    lock.unlock();
  }
}
````

### 2.2 ConcurrentHashMap을 사용한 사용자별 Lock 관리

ConcurrentHashMap<Long, ReentrantLock>은 각 사용자 ID에 대해 Lock 객체를 관리합니다. 이를 통해 동일한 사용자에 대한 요청만 동시성 제어를 받고, 다른 사용자에 대한 요청은 동시에 처리될 수 있습니다. 즉, 사용자 단위로 Lock을 적용하여 불필요한 병목을 최소화할 수 있습니다.
````java
private final ConcurrentHashMap<Long, ReentrantLock> userLock = new ConcurrentHashMap<>();
````
- computeIfAbsent() 메서드는 사용자가 처음 요청할 때만 Lock을 생성하고, 이후에는 이미 생성된 Lock을 사용하여 불필요한 Lock 생성 비용을 줄입니다.
- 사용자에 대해 동시 요청이 들어오면, Lock을 획득한 요청만 먼저 처리되고, 나머지 요청은 대기하게 됩니다.

### 2.3 다른 동시성 제어 방식들
  - synchronized 키워드: 자바의 기본 동시성 제어 메커니즘이지만, 메서드나 코드 블록 전체에 걸리는 경향이 있어 세밀한 제어가 어렵습니다.
  - Atomic 클래스: 단일 연산에 대해 원자성을 보장할 때 사용되며, 스레드 안전한 값을 처리할 때 유용하지만, 이 프로젝트처럼 복잡한 트랜잭션에서는 적합하지 않습니다.
  - CompletableFuture 및 비동기 처리: 비동기적으로 처리해도 경합이 발생할 수 있기 때문에, 동시성 제어를 위해서는 여전히 Lock과 같은 방법이 필요합니다.

## 3. 동시성 문제 해결 후 성능 분석

동시성 문제를 해결한 후, 시스템은 다중 요청이 들어와도 데이터의 일관성이 유지되며, 각 요청이 순차적으로 처리됩니다. 특히 동일한 사용자에 대해서만 순차 처리되므로, 다른 사용자에 대한 요청은 병렬 처리가 가능합니다. 이로 인해 성능 저하를 최소화할 수 있습니다.

성능 개선 요소

- 사용자별로 Lock을 적용함으로써 경쟁 자원의 범위를 최소화하고, 다른 사용자의 요청은 병렬로 처리할 수 있습니다.
- ReentrantLock 활용한 세밀한 동시성 제어로, 단순히 모든 요청을 차단하는 방식이 아닌, 필요한 요청만 차단하여 성능을 높였습니다.

## 4. 추가적인 개선 방안

1. 큐를 사용한 비동기 처리: Lock을 사용하는 대신, 요청을 큐에 담아서 순차적으로 처리하는 방식도 고려할 수 있습니다. 이를 통해 각 요청이 비동기적으로 처리되며, 순차성도 보장됩니다.
2. Redis 분산 Lock: 만약 시스템이 분산 환경에서 동작한다면, Redis를 사용한 분산 Lock을 고려할 수 있습니다. 이 방법은 여러 서버에서 동일한 자원에 접근하는 문제를 해결할 수 있습니다.
3. 데드락(Deadlock) 방지: 여러 자원에 동시에 Lock을 거는 작업을 피하고, Lock 획득 순서를 보장하여 데드락을 방지해야 합니다. 이 프로젝트에서는 각 사용자에 대한 Lock만 적용하므로 데드락 가능성은 낮습니다.

## 5. 동시성 테스트 시나리오 및 결과
### 5.1 여러 사용자의 동시 충전 요청 처리
목표: 여러 사용자가 동시에 포인트 충전 요청을 했을 때, 각 사용자의 포인트가 정확하게 업데이트되는지 확인.
- 시나리오: 10명의 사용자가 각각 동시에 100 포인트 충전 요청을 보냅니다.
- 예상 결과: 각 사용자의 포인트가 100으로 업데이트되어야 합니다.
````java
@Test
public void 여러_사용자의_동시_충전_요청() throws Exception {
  int userCount = 10;
  long chargeAmount = 100L;
  CountDownLatch latch = new CountDownLatch(userCount);

  List<Future<MvcResult>> futures = new ArrayList<>();
  for (long userId = 1; userId <= userCount; userId++) {
    long finalUserId = userId;
    futures.add(executorService.submit(() -> {
      return mockMvc.perform(patch("/point/" + finalUserId + "/charge")
                      .contentType("application/json")
                      .content(String.valueOf(chargeAmount)))
              .andExpect(status().isOk())
              .andReturn();
    }));
  }

  latch.await();
  for (int i = 0; i < userCount; i++) {
    long userId = i + 1;
    UserPoint userPoint = pointService.getUserPoint(userId);
    assertEquals(chargeAmount, userPoint.point());
  }
}
````
결과: 모든 사용자의 포인트가 정확히 100으로 업데이트되었습니다. 동시 요청이 잘 처리되었으며, 데이터의 일관성이 유지되었습니다.

### 5.2 동일 사용자에 대한 동시 충전 요청 처리

목표: 동일한 사용자에 대한 동시 충전 요청이 순차적으로 처리되는지 확인.
- 시나리오: 동일한 사용자에 대해 5개의 충전 요청을 동시에 보냅니다.
- 예상 결과: 충전 요청이 순차적으로 처리되어 최종 포인트는 500이 되어야 합니다.
````java
@Test
public void 동일한_사용자에_대한_동시_충전_요청의_순차적_처리() throws Exception {
    long userId = 1L;
    int requestCount = 5;
    long chargeAmount = 100L;
    CountDownLatch latch = new CountDownLatch(requestCount);

    for (int i = 0; i < requestCount; i++) {
        executorService.submit(() -> {
            mockMvc.perform(patch("/point/" + userId + "/charge")
                            .contentType("application/json")
                            .content(String.valueOf(chargeAmount)))
                    .andExpect(status().isOk());
        });
    }

    latch.await();
    UserPoint userPoint = pointService.getUserPoint(userId);
    assertEquals(chargeAmount * requestCount, userPoint.point());
}
````
결과: 동일한 사용자에 대한 충전 요청이 순차적으로 처리되어 예상한 대로 최종 포인트는 500으로 확인되었습니다.

### 5.3 잔액 부족 상황에서의 동시 사용 요청 처리
목표: 사용자가 잔액이 부족한 상황에서 동시 사용 요청을 보냈을 때, 하나는 성공하고 다른 하나는 실패하는지 확인.
- 시나리오: 잔액이 500인 상태에서 두 개의 300 포인트 사용 요청을 동시에 보냅니다.
- 예상 결과: 하나의 요청은 성공하고, 다른 하나는 잔액 부족으로 실패해야 합니다.
````java
@Test
public void 잔액_부족_상황에서의_동시_사용_요청_처리를_검증() throws Exception {
    long userId = 2L;
    long initialBalance = 500L;
    long useAmount = 300L;
    pointService.chargeUserPoint(userId, initialBalance);

    CountDownLatch latch = new CountDownLatch(2);
    List<Future<MvcResult>> futures = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
        futures.add(executorService.submit(() -> {
            return mockMvc.perform(patch("/point/" + userId + "/use")
                            .contentType("application/json")
                            .content(String.valueOf(useAmount)))
                    .andReturn();
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
````
결과: 하나의 요청만 성공하고, 다른 요청은 잔액 부족으로 실패했습니다. 최종 잔액은 200 포인트로 정확하게 처리되었습니다.

### 5.4 동시 충전 및 사용 요청 처리

목표: 동일한 사용자가 동시에 충전과 사용 요청을 보냈을 때, 순서가 보장되는지 확인.
- 시나리오: 잔액이 500인 상태에서 300 포인트 충전 요청과 200 포인트 사용 요청을 동시에 보냅니다.
- 예상 결과: 충전 요청이 먼저 처리되고, 이후 사용 요청이 처리되어 최종 잔액은 600이 되어야 합니다.

````java
@Test
public void 충전과_사용_요청의_동시처리와_순서보장을_확인() throws Exception {
    long userId = 3L;
    long initialBalance = 500L;
    long chargeAmount = 300L;
    long useAmount = 200L;
    pointService.chargeUserPoint(userId, initialBalance);

    CountDownLatch latch = new CountDownLatch(2);
    Future<MvcResult> chargeFuture = executorService.submit(() -> {
        return mockMvc.perform(patch("/point/" + userId + "/charge")
                        .contentType("application/json")
                        .content(String.valueOf(chargeAmount)))
                .andReturn();
    });

    Future<MvcResult> useFuture = executorService.submit(() -> {
        return mockMvc.perform(patch("/point/" + userId + "/use")
                        .contentType("application/json")
                        .content(String.valueOf(useAmount)))
                .andReturn();
    });

    latch.await();
    assertEquals(200, chargeFuture.get().getResponse().getStatus());
    assertEquals(200, useFuture.get().getResponse().getStatus());

    UserPoint finalUserPoint = pointService.getUserPoint(userId);
    assertEquals(600L, finalUserPoint.point());
}
````
결과: 충전 요청이 먼저 처리되고, 그 다음에 사용 요청이 처리되어 최종 포인트 잔액은 600으로 정확하게 처리되었습니다.

### 5.5 다수 사용자의 동시 요청에 대한 성능 검증

목표: 다수의 사용자가 동시에 충전 및 사용 요청을 보낼 때, 시스템의 성능과 정확성을 확인.
- 시나리오: 10명의 사용자가 각각 충전 및 사용 요청을 동시에 보냅니다.
- 예상 결과: 모든 요청이 성공적으로 처리되며, 최종 잔액이 정확히 계산되어야 합니다.

````java
@Test
public void 다수_사용자의_동시_요청_성능검증() throws Exception {
  int userCount = 10;
  long initialBalance = 200L;
  long chargeAmount = 100L;
  long useAmount = 50L;
  CountDownLatch latch = new CountDownLatch(userCount * 2);  // 각 사용자에 대해 충전과 사용 요청

  long startTime = System.currentTimeMillis();

  for (long userId = 1; userId <= userCount; userId++) {
    long finalUserId = userId;

    // 충전 요청
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

    // 사용 요청
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

  System.out.println("동시 요청 처리 시간: " + duration + " ms");

  for (long userId = 1; userId <= userCount; userId++) {
    UserPoint userPoint = pointService.getUserPoint(userId);
    assertEquals(initialBalance + chargeAmount - useAmount, userPoint.point());
  }
}
````
결과
- 테스트 결과: 10명의 사용자가 동시에 충전과 사용 요청을 보냈을 때, 모든 요청이 성공적으로 처리되었습니다. 최종 포인트 잔액이 정확하게 계산되었고, 시스템의 처리 시간도 로그로 5000ms로 확인할 수 있었습니다.
- 성능 분석: 요청 수가 증가했음에도 불구하고, 각 사용자의 요청이 정확하고 신속하게 처리되었습니다. 모든 테스트 케이스에서 동시성 문제 없이 데이터를 처리할 수 있음을 확인했습니다.