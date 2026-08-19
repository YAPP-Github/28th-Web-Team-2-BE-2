package com.example.demo.item.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.demo.item.application.result.OnlinePriceCollectionResult;
import com.example.demo.item.application.usecase.CollectOnlinePriceUseCase;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

class OnlinePriceSchedulerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void 스케줄은_서울_자정이고_서울_기준_날짜를_전달한다() throws Exception {
        final CollectOnlinePriceUseCase useCase = mock(CollectOnlinePriceUseCase.class);
        final OnlinePriceScheduler scheduler = new OnlinePriceScheduler(
                useCase,
                Clock.fixed(Instant.parse("2026-08-18T15:00:00Z"), SEOUL),
                Runnable::run);

        scheduler.collectBySchedule();

        verify(useCase).execute(LocalDate.of(2026, 8, 19));
        final Method scheduledMethod = OnlinePriceScheduler.class.getDeclaredMethod("collectBySchedule");
        final Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("0 0 0 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
        assertThat(Arrays.stream(OnlinePriceScheduler.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EventListener.class))
                .toList()).isEmpty();
    }

    @Test
    void schedule_trigger는_수집을_비동기로_넘기고_즉시_반환한다() throws Exception {
        final CollectOnlinePriceUseCase useCase = mock(CollectOnlinePriceUseCase.class);
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            release.await(2, TimeUnit.SECONDS);
            return null;
        }).when(useCase).execute(any(LocalDate.class));
        final ExecutorService workerExecutor = Executors.newSingleThreadExecutor();
        final OnlinePriceScheduler scheduler = new OnlinePriceScheduler(
                useCase,
                Clock.fixed(Instant.parse("2026-08-18T15:00:00Z"), SEOUL),
                workerExecutor);
        final ExecutorService triggerExecutor = Executors.newSingleThreadExecutor();
        try {
            final var trigger = triggerExecutor.submit(scheduler::collectBySchedule);
            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(trigger.get(1, TimeUnit.SECONDS)).isNull();
        } finally {
            release.countDown();
            triggerExecutor.shutdownNow();
            workerExecutor.shutdown();
            triggerExecutor.awaitTermination(2, TimeUnit.SECONDS);
            workerExecutor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void 실행_중_trigger는_동시에_실행되지_않고_pending_한_건으로_합쳐진다() throws Exception {
        final CollectOnlinePriceUseCase useCase = mock(CollectOnlinePriceUseCase.class);
        final CountDownLatch firstStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);
        final CountDownLatch pendingStarted = new CountDownLatch(1);
        final CountDownLatch releasePending = new CountDownLatch(1);
        final CountDownLatch thirdStarted = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger maxActive = new AtomicInteger();
        doAnswer(invocation -> {
            final int current = active.incrementAndGet();
            maxActive.updateAndGet(previous -> Math.max(previous, current));
            final int call = calls.incrementAndGet();
            if (call == 1) {
                firstStarted.countDown();
                releaseFirst.await(2, TimeUnit.SECONDS);
            }
            if (call == 2) {
                pendingStarted.countDown();
                releasePending.await(2, TimeUnit.SECONDS);
            }
            if (call == 3) {
                thirdStarted.countDown();
            }
            active.decrementAndGet();
            return null;
        }).when(useCase).execute(any(LocalDate.class));
        final ExecutorService workerExecutor = Executors.newSingleThreadExecutor();
        final OnlinePriceScheduler scheduler = new OnlinePriceScheduler(
                useCase,
                Clock.fixed(Instant.parse("2026-08-18T15:00:00Z"), SEOUL),
                workerExecutor);
        final ExecutorService triggerExecutor = Executors.newFixedThreadPool(4);
        try {
            final var first = triggerExecutor.submit(scheduler::collectBySchedule);
            assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(first.get(1, TimeUnit.SECONDS)).isNull();

            final var second = triggerExecutor.submit(scheduler::collectBySchedule);
            final var third = triggerExecutor.submit(scheduler::collectBySchedule);
            assertThat(second.get(1, TimeUnit.SECONDS)).isNull();
            assertThat(third.get(1, TimeUnit.SECONDS)).isNull();
            assertThat(calls).hasValue(1);

            releaseFirst.countDown();
            assertThat(pendingStarted.await(1, TimeUnit.SECONDS)).isTrue();
            final var fourth = triggerExecutor.submit(scheduler::collectBySchedule);
            final var fifth = triggerExecutor.submit(scheduler::collectBySchedule);
            assertThat(fourth.get(1, TimeUnit.SECONDS)).isNull();
            assertThat(fifth.get(1, TimeUnit.SECONDS)).isNull();
            releasePending.countDown();
            assertThat(thirdStarted.await(1, TimeUnit.SECONDS)).isTrue();

            assertThat(calls).hasValue(3);
            assertThat(maxActive).hasValue(1);
        } finally {
            releaseFirst.countDown();
            releasePending.countDown();
            triggerExecutor.shutdownNow();
            workerExecutor.shutdownNow();
            triggerExecutor.awaitTermination(2, TimeUnit.SECONDS);
            workerExecutor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void 예외가_발생해도_상태를_해제하고_다음_trigger를_실행한다() throws Exception {
        final CollectOnlinePriceUseCase useCase = mock(CollectOnlinePriceUseCase.class);
        final CountDownLatch firstStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);
        final CountDownLatch secondStarted = new CountDownLatch(1);
        final CountDownLatch releaseSecond = new CountDownLatch(1);
        final CountDownLatch thirdStarted = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                firstStarted.countDown();
                releaseFirst.await(2, TimeUnit.SECONDS);
                throw new IllegalStateException("provider response must not be logged");
            }
            if (calls.get() == 2) {
                secondStarted.countDown();
                releaseSecond.await(2, TimeUnit.SECONDS);
            }
            if (calls.get() == 3) {
                thirdStarted.countDown();
            }
            return new OnlinePriceCollectionResult(0, 0, 0, 0);
        }).when(useCase).execute(any(LocalDate.class));
        final ExecutorService workerExecutor = Executors.newSingleThreadExecutor();
        final OnlinePriceScheduler scheduler = new OnlinePriceScheduler(
                useCase,
                Clock.fixed(Instant.parse("2026-08-18T15:00:00Z"), SEOUL),
                workerExecutor);
        final ExecutorService triggerExecutor = Executors.newFixedThreadPool(2);
        try {
            final var first = triggerExecutor.submit(scheduler::collectBySchedule);
            assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(first.get(1, TimeUnit.SECONDS)).isNull();
            final var pending = triggerExecutor.submit(scheduler::collectBySchedule);
            assertThat(pending.get(1, TimeUnit.SECONDS)).isNull();
            releaseFirst.countDown();
            assertThat(secondStarted.await(1, TimeUnit.SECONDS)).isTrue();

            assertThatCode(scheduler::collectBySchedule).doesNotThrowAnyException();
            releaseSecond.countDown();
            assertThat(thirdStarted.await(1, TimeUnit.SECONDS)).isTrue();
            verify(useCase, times(3)).execute(any(LocalDate.class));
        } finally {
            releaseFirst.countDown();
            releaseSecond.countDown();
            triggerExecutor.shutdownNow();
            workerExecutor.shutdownNow();
        }
    }
}
