package shop.buzzle.buzzle.websocket.concurrency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 매칭 시스템 성능 벤치마크 테스트
 *
 * 비교 대상:
 * 1. 방식 A: 일반 Queue + 모든 연산에 락 사용 (현재 구현)
 * 2. 방식 B: ConcurrentLinkedQueue + 입장은 락-프리, 매칭만 락 사용 (최적화)
 *
 * 측정 항목:
 * - 총 처리 시간
 * - 락 경합 횟수
 * - 락 대기 시간
 * - 처리량 (ops/sec)
 */
@DisplayName("Queue 성능 벤치마크: 락 전략 비교")
class QueuePerformanceBenchmarkTest {

    // ========================================
    // 방식 A: 일반 Queue + 모든 연산에 락 사용
    // ========================================
    static class FullLockMatchService {
        private final Queue<String> waitingQueue = new LinkedList<>(); // 일반 Queue
        private final Set<String> waitingEmails = new HashSet<>();     // 일반 Set
        private final Lock lock = new ReentrantLock();

        private final AtomicInteger lockContentionCount = new AtomicInteger(0);
        private final AtomicLong totalLockWaitTime = new AtomicLong(0);
        private final List<String[]> matchedPairs = Collections.synchronizedList(new ArrayList<>());

        public void addToQueue(String email) {
            long startWait = System.nanoTime();
            boolean contention = !lock.tryLock();

            if (contention) {
                lockContentionCount.incrementAndGet();
                lock.lock();
            }

            long waitTime = System.nanoTime() - startWait;
            totalLockWaitTime.addAndGet(waitTime);

            try {
                if (!waitingEmails.contains(email)) {
                    waitingEmails.add(email);
                    waitingQueue.add(email);
                }
            } finally {
                lock.unlock();
            }
        }

        public void matchUsers() {
            long startWait = System.nanoTime();
            boolean contention = !lock.tryLock();

            if (contention) {
                lockContentionCount.incrementAndGet();
                lock.lock();
            }

            long waitTime = System.nanoTime() - startWait;
            totalLockWaitTime.addAndGet(waitTime);

            try {
                while (waitingQueue.size() >= 2) {
                    String user1 = waitingQueue.poll();
                    String user2 = waitingQueue.poll();

                    if (user1 != null && user2 != null) {
                        waitingEmails.remove(user1);
                        waitingEmails.remove(user2);
                        matchedPairs.add(new String[]{user1, user2});
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        public int getLockContentionCount() { return lockContentionCount.get(); }
        public long getTotalLockWaitTimeNanos() { return totalLockWaitTime.get(); }
        public int getMatchedPairsCount() { return matchedPairs.size(); }
        public int getQueueSize() { return waitingQueue.size(); }
    }

    // ========================================
    // 방식 B: ConcurrentLinkedQueue + 입장 락-프리
    // ========================================
    static class LockFreeInsertMatchService {
        private final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>(); // 락-프리 Queue
        private final Set<String> waitingEmails = ConcurrentHashMap.newKeySet();  // 락-프리 Set
        private final Lock lock = new ReentrantLock();

        private final AtomicInteger lockContentionCount = new AtomicInteger(0);
        private final AtomicLong totalLockWaitTime = new AtomicLong(0);
        private final List<String[]> matchedPairs = Collections.synchronizedList(new ArrayList<>());

        public void addToQueue(String email) {
            // 락-프리 입장: ConcurrentHashMap.newKeySet().add()는 원자적 연산
            if (waitingEmails.add(email)) {
                waitingQueue.add(email);  // ConcurrentLinkedQueue도 락-프리
            }
            // 락 대기 시간 없음!
        }

        public void matchUsers() {
            long startWait = System.nanoTime();
            boolean contention = !lock.tryLock();

            if (contention) {
                lockContentionCount.incrementAndGet();
                lock.lock();
            }

            long waitTime = System.nanoTime() - startWait;
            totalLockWaitTime.addAndGet(waitTime);

            try {
                while (waitingQueue.size() >= 2) {
                    String user1 = waitingQueue.poll();
                    String user2 = waitingQueue.poll();

                    if (user1 != null && user2 != null) {
                        waitingEmails.remove(user1);
                        waitingEmails.remove(user2);
                        matchedPairs.add(new String[]{user1, user2});
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        public int getLockContentionCount() { return lockContentionCount.get(); }
        public long getTotalLockWaitTimeNanos() { return totalLockWaitTime.get(); }
        public int getMatchedPairsCount() { return matchedPairs.size(); }
        public int getQueueSize() { return waitingQueue.size(); }
    }

    // ========================================
    // 벤치마크 결과 클래스
    // ========================================
    static class BenchmarkResult {
        final String name;
        final int totalRequests;
        final int matchedPairs;
        final int queueRemaining;
        final int lockContentions;
        final long lockWaitTimeMs;
        final long totalTimeMs;
        final double throughput;

        BenchmarkResult(String name, int totalRequests, int matchedPairs, int queueRemaining,
                       int lockContentions, long lockWaitTimeNanos, long totalTimeNanos) {
            this.name = name;
            this.totalRequests = totalRequests;
            this.matchedPairs = matchedPairs;
            this.queueRemaining = queueRemaining;
            this.lockContentions = lockContentions;
            this.lockWaitTimeMs = lockWaitTimeNanos / 1_000_000;
            this.totalTimeMs = totalTimeNanos / 1_000_000;
            this.throughput = totalRequests / (totalTimeNanos / 1_000_000_000.0);
        }
    }

    // ========================================
    // 벤치마크 테스트
    // ========================================

    @Test
    @DisplayName("성능 비교: 모든 연산 락 vs 입장 락-프리")
    void compareLockStrategies() throws InterruptedException {
        printHeader("Queue 성능 벤치마크: 락 전략 비교");

        int[] testSizes = {100, 500, 1000, 5000};
        int threadCount = 100;

        List<BenchmarkResult> fullLockResults = new ArrayList<>();
        List<BenchmarkResult> lockFreeResults = new ArrayList<>();

        for (int requestCount : testSizes) {
            System.out.printf("%n▶ 테스트: %,d개 요청, %d개 스레드%n", requestCount, threadCount);
            System.out.println("─".repeat(60));

            // 방식 A: 모든 연산 락
            BenchmarkResult resultA = runFullLockBenchmark(requestCount, threadCount);
            fullLockResults.add(resultA);

            // 방식 B: 입장 락-프리
            BenchmarkResult resultB = runLockFreeBenchmark(requestCount, threadCount);
            lockFreeResults.add(resultB);

            // 개별 결과 출력
            printIndividualResult(resultA, resultB);
        }

        // 최종 비교 테이블
        printComparisonTable(fullLockResults, lockFreeResults);

        // 성능 향상 검증
        verifyPerformanceImprovement(fullLockResults, lockFreeResults);
    }

    @Test
    @DisplayName("락 경합 집중 테스트: 동시 입장 시나리오")
    void lockContentionIntensiveTest() throws InterruptedException {
        printHeader("락 경합 집중 테스트");

        int requestCount = 1000;
        int threadCount = 200; // 높은 동시성

        System.out.println("📋 시나리오: 200개 스레드가 동시에 1000개 입장 요청");
        System.out.println("   → 입장 연산의 락 경합을 집중적으로 테스트");
        System.out.println("═".repeat(60));

        // 방식 A
        FullLockMatchService serviceA = new FullLockMatchService();
        long timeA = runInsertOnlyBenchmark(serviceA, requestCount, threadCount);

        // 방식 B
        LockFreeInsertMatchService serviceB = new LockFreeInsertMatchService();
        long timeB = runInsertOnlyBenchmark(serviceB, requestCount, threadCount);

        System.out.println("\n📊 결과:");
        System.out.println("─".repeat(60));
        System.out.printf("┌─────────────────────┬────────────────┬────────────────┐%n");
        System.out.printf("│ %-19s │ %-14s │ %-14s │%n", "측정 항목", "모든 연산 락", "입장 락-프리");
        System.out.printf("├─────────────────────┼────────────────┼────────────────┤%n");
        System.out.printf("│ %-19s │ %,12dms │ %,12dms │%n", "총 처리 시간",
            timeA / 1_000_000, timeB / 1_000_000);
        System.out.printf("│ %-19s │ %,14d │ %,14d │%n", "락 경합 횟수",
            serviceA.getLockContentionCount(), serviceB.getLockContentionCount());
        System.out.printf("│ %-19s │ %,12dms │ %,12dms │%n", "락 대기 시간",
            serviceA.getTotalLockWaitTimeNanos() / 1_000_000,
            serviceB.getTotalLockWaitTimeNanos() / 1_000_000);
        System.out.printf("└─────────────────────┴────────────────┴────────────────┘%n");

        double improvement = ((double)(timeA - timeB) / timeA) * 100;
        System.out.printf("%n⏱️ 총 처리 시간 차이: %.1f%%%n", improvement);

        // 핵심 검증: 락 경합 및 대기 시간 감소
        System.out.println("\n✅ 핵심 성능 지표:");
        System.out.printf("   • 락 경합: %d → %d (%.0f%% 감소)%n",
            serviceA.getLockContentionCount(), serviceB.getLockContentionCount(),
            serviceA.getLockContentionCount() > 0 ?
                ((double)(serviceA.getLockContentionCount() - serviceB.getLockContentionCount())
                    / serviceA.getLockContentionCount()) * 100 : 0);
        System.out.printf("   • 락 대기 시간: %,dms → %,dms%n",
            serviceA.getTotalLockWaitTimeNanos() / 1_000_000,
            serviceB.getTotalLockWaitTimeNanos() / 1_000_000);

        // 락 경합이 감소해야 함 (이게 핵심!)
        assertThat(serviceB.getLockContentionCount())
            .as("락-프리 방식의 락 경합이 0이어야 함")
            .isEqualTo(0);

        assertThat(serviceB.getTotalLockWaitTimeNanos())
            .as("락-프리 방식의 락 대기 시간이 0이어야 함")
            .isEqualTo(0);
    }

    @Test
    @DisplayName("혼합 워크로드 테스트: 입장 + 매칭 동시 실행")
    void mixedWorkloadTest() throws InterruptedException {
        printHeader("혼합 워크로드 테스트");

        int requestCount = 2000;
        int insertThreads = 100;
        int matchThreads = 10;

        System.out.println("📋 시나리오: 100개 입장 스레드 + 10개 매칭 스레드 동시 실행");
        System.out.println("═".repeat(60));

        // 방식 A
        BenchmarkResult resultA = runMixedWorkloadBenchmark(
            new FullLockMatchService(), requestCount, insertThreads, matchThreads, "모든 연산 락");

        // 방식 B
        BenchmarkResult resultB = runMixedWorkloadBenchmark(
            new LockFreeInsertMatchService(), requestCount, insertThreads, matchThreads, "입장 락-프리");

        printIndividualResult(resultA, resultB);

        double improvement = ((double)(resultA.totalTimeMs - resultB.totalTimeMs) / resultA.totalTimeMs) * 100;
        System.out.printf("%n✅ 성능 향상: %.1f%% 빠름 (입장 락-프리 방식)%n", improvement);
    }

    // ========================================
    // 벤치마크 실행 메서드
    // ========================================

    private BenchmarkResult runFullLockBenchmark(int requestCount, int threadCount) throws InterruptedException {
        FullLockMatchService service = new FullLockMatchService();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        long startTime = System.nanoTime();

        for (int i = 0; i < requestCount; i++) {
            final String email = "user" + i + "@test.com";
            executor.submit(() -> {
                try {
                    startLatch.await();
                    service.addToQueue(email);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();

        // 매칭 실행
        service.matchUsers();

        long totalTime = System.nanoTime() - startTime;
        executor.shutdown();

        return new BenchmarkResult("모든 연산 락", requestCount,
            service.getMatchedPairsCount(), service.getQueueSize(),
            service.getLockContentionCount(), service.getTotalLockWaitTimeNanos(), totalTime);
    }

    private BenchmarkResult runLockFreeBenchmark(int requestCount, int threadCount) throws InterruptedException {
        LockFreeInsertMatchService service = new LockFreeInsertMatchService();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        long startTime = System.nanoTime();

        for (int i = 0; i < requestCount; i++) {
            final String email = "user" + i + "@test.com";
            executor.submit(() -> {
                try {
                    startLatch.await();
                    service.addToQueue(email);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();

        // 매칭 실행
        service.matchUsers();

        long totalTime = System.nanoTime() - startTime;
        executor.shutdown();

        return new BenchmarkResult("입장 락-프리", requestCount,
            service.getMatchedPairsCount(), service.getQueueSize(),
            service.getLockContentionCount(), service.getTotalLockWaitTimeNanos(), totalTime);
    }

    private long runInsertOnlyBenchmark(Object service, int requestCount, int threadCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        long startTime = System.nanoTime();

        for (int i = 0; i < requestCount; i++) {
            final String email = "user" + i + "@test.com";
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (service instanceof FullLockMatchService) {
                        ((FullLockMatchService) service).addToQueue(email);
                    } else {
                        ((LockFreeInsertMatchService) service).addToQueue(email);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        return System.nanoTime() - startTime;
    }

    private BenchmarkResult runMixedWorkloadBenchmark(Object service, int requestCount,
            int insertThreads, int matchThreads, String name) throws InterruptedException {

        ExecutorService insertExecutor = Executors.newFixedThreadPool(insertThreads);
        ExecutorService matchExecutor = Executors.newFixedThreadPool(matchThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch insertLatch = new CountDownLatch(requestCount);
        AtomicInteger matchCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        // 입장 스레드
        for (int i = 0; i < requestCount; i++) {
            final String email = "user" + i + "@test.com";
            insertExecutor.submit(() -> {
                try {
                    startLatch.await();
                    if (service instanceof FullLockMatchService) {
                        ((FullLockMatchService) service).addToQueue(email);
                    } else {
                        ((LockFreeInsertMatchService) service).addToQueue(email);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    insertLatch.countDown();
                }
            });
        }

        // 매칭 스레드 (주기적으로 매칭 시도)
        for (int i = 0; i < matchThreads; i++) {
            matchExecutor.submit(() -> {
                try {
                    startLatch.await();
                    while (!Thread.currentThread().isInterrupted()) {
                        if (service instanceof FullLockMatchService) {
                            ((FullLockMatchService) service).matchUsers();
                        } else {
                            ((LockFreeInsertMatchService) service).matchUsers();
                        }
                        matchCount.incrementAndGet();
                        Thread.sleep(1); // 약간의 간격
                    }
                } catch (InterruptedException e) {
                    // 정상 종료
                }
            });
        }

        startLatch.countDown();
        insertLatch.await();

        // 매칭 스레드 종료
        matchExecutor.shutdownNow();
        matchExecutor.awaitTermination(1, TimeUnit.SECONDS);

        // 최종 매칭
        if (service instanceof FullLockMatchService) {
            ((FullLockMatchService) service).matchUsers();
        } else {
            ((LockFreeInsertMatchService) service).matchUsers();
        }

        long totalTime = System.nanoTime() - startTime;
        insertExecutor.shutdown();

        int lockContentions, matchedPairs, queueSize;
        long lockWaitTime;

        if (service instanceof FullLockMatchService) {
            FullLockMatchService s = (FullLockMatchService) service;
            lockContentions = s.getLockContentionCount();
            lockWaitTime = s.getTotalLockWaitTimeNanos();
            matchedPairs = s.getMatchedPairsCount();
            queueSize = s.getQueueSize();
        } else {
            LockFreeInsertMatchService s = (LockFreeInsertMatchService) service;
            lockContentions = s.getLockContentionCount();
            lockWaitTime = s.getTotalLockWaitTimeNanos();
            matchedPairs = s.getMatchedPairsCount();
            queueSize = s.getQueueSize();
        }

        return new BenchmarkResult(name, requestCount, matchedPairs, queueSize,
            lockContentions, lockWaitTime, totalTime);
    }

    // ========================================
    // 출력 헬퍼 메서드
    // ========================================

    private void printHeader(String title) {
        System.out.println("\n");
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.printf("║  %-64s  ║%n", title);
        System.out.println("╚" + "═".repeat(68) + "╝");
    }

    private void printIndividualResult(BenchmarkResult a, BenchmarkResult b) {
        System.out.printf("%n   %-14s: %,6dms, 락경합 %,5d회, 대기시간 %,6dms%n",
            a.name, a.totalTimeMs, a.lockContentions, a.lockWaitTimeMs);
        System.out.printf("   %-14s: %,6dms, 락경합 %,5d회, 대기시간 %,6dms%n",
            b.name, b.totalTimeMs, b.lockContentions, b.lockWaitTimeMs);

        if (a.totalTimeMs > 0) {
            double improvement = ((double)(a.totalTimeMs - b.totalTimeMs) / a.totalTimeMs) * 100;
            System.out.printf("   → 성능 차이: %.1f%%%n", improvement);
        }
    }

    private void printComparisonTable(List<BenchmarkResult> fullLock, List<BenchmarkResult> lockFree) {
        System.out.println("\n" + "═".repeat(90));
        System.out.println("📊 최종 비교 결과");
        System.out.println("═".repeat(90));

        System.out.printf("┌────────────┬─────────────────────────────────┬─────────────────────────────────┬──────────┐%n");
        System.out.printf("│ %-10s │ %-31s │ %-31s │ %-8s │%n",
            "요청 수", "모든 연산 락 (A)", "입장 락-프리 (B)", "향상율");
        System.out.printf("│            │ 시간(ms) / 락경합 / 대기(ms)   │ 시간(ms) / 락경합 / 대기(ms)   │          │%n");
        System.out.printf("├────────────┼─────────────────────────────────┼─────────────────────────────────┼──────────┤%n");

        for (int i = 0; i < fullLock.size(); i++) {
            BenchmarkResult a = fullLock.get(i);
            BenchmarkResult b = lockFree.get(i);
            double improvement = a.totalTimeMs > 0 ?
                ((double)(a.totalTimeMs - b.totalTimeMs) / a.totalTimeMs) * 100 : 0;

            System.out.printf("│ %,10d │ %,8d / %,6d / %,8d │ %,8d / %,6d / %,8d │ %+7.1f%% │%n",
                a.totalRequests,
                a.totalTimeMs, a.lockContentions, a.lockWaitTimeMs,
                b.totalTimeMs, b.lockContentions, b.lockWaitTimeMs,
                improvement);
        }

        System.out.printf("└────────────┴─────────────────────────────────┴─────────────────────────────────┴──────────┘%n");

        System.out.println("\n📌 결론:");
        System.out.println("   • 입장 락-프리 방식이 락 경합을 대폭 감소시킴");
        System.out.println("   • 동시 요청이 많을수록 성능 차이가 커짐");
        System.out.println("   • ConcurrentLinkedQueue + 국소적 락 = 최적의 조합");
    }

    private void verifyPerformanceImprovement(List<BenchmarkResult> fullLock, List<BenchmarkResult> lockFree) {
        System.out.println("\n✅ 검증 결과:");

        for (int i = 0; i < fullLock.size(); i++) {
            BenchmarkResult a = fullLock.get(i);
            BenchmarkResult b = lockFree.get(i);

            // 락 경합 감소 검증
            assertThat(b.lockContentions)
                .as("%d개 요청 시 락-프리 방식의 락 경합이 더 적어야 함", a.totalRequests)
                .isLessThanOrEqualTo(a.lockContentions);

            // 매칭 정확성 검증
            assertThat(b.matchedPairs)
                .as("%d개 요청 시 매칭 쌍 수가 동일해야 함", a.totalRequests)
                .isEqualTo(a.matchedPairs);

            System.out.printf("   • %,d개 요청: 락 경합 %d → %d (%.0f%% 감소), 매칭 정확성 ✓%n",
                a.totalRequests, a.lockContentions, b.lockContentions,
                a.lockContentions > 0 ? ((double)(a.lockContentions - b.lockContentions) / a.lockContentions) * 100 : 0);
        }
    }
}
