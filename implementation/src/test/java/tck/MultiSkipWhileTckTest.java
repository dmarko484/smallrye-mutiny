package tck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiSkipWhileTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void dropWhileStageShouldSupportDroppingElements() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3, 4, 0)
                .skip().first(i -> i < 3)
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList(3, 4, 0));
    }

    @Test
    public void dropWhileStageShouldHandleErrors() {
        assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Integer>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .skip().first(i -> {
                        throw new QuietRuntimeException("failed");
                    })
                    .collect().asList()
                    .subscribeAsCompletionStage();
            await(cancelled);
            await(result);
        });
    }

    @Test
    public void dropWhileStageShouldPropagateUpstreamErrorsWhileDropping() {
        assertThrows(QuietRuntimeException.class,
                () -> await(Multi.createFrom().<Integer> failure(new QuietRuntimeException("failed"))
                        .skip().first(i -> i < 3)
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void dropWhileStageShouldPropagateUpstreamErrorsAfterFinishedDropping() {
        assertThrows(QuietRuntimeException.class, () -> await(infiniteStream()
                .onItem().invoke(i -> {
                    if (i == 4) {
                        throw new QuietRuntimeException("failed");
                    }
                })
                .skip().first(i -> i < 3)
                .collect().asList()
                .subscribeAsCompletionStage()));
    }

    @Test
    public void dropWhileStageShouldNotRunPredicateOnceItsFinishedDropping() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3, 4)
                .skip().first(i -> {
                    if (i < 3) {
                        return true;
                    } else if (i == 4) {
                        throw new RuntimeException("4 was passed");
                    } else {
                        return false;
                    }
                })
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList(3, 4));
    }

    @Test
    public void dropWhileStageShouldAllowCompletionWhileDropping() {
        assertEquals(await(Multi.createFrom().items(1, 1, 1, 1)
                .skip().first(i -> i < 3)
                .collect().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test
    public void dropWhileStageShouldPropagateCancel() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        infiniteStream()
                .onTermination().invoke(() -> cancelled.complete(null))
                .skip().first(i -> i < 3)
                .subscribe().withSubscriber(new AssertSubscriber<>(10, true));
        await(cancelled);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .skip().first(i -> false);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .skip().first(i -> false);
    }
}
