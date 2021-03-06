package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniCreateFromDeferredSupplier<T> extends AbstractUni<T> {
    private final Supplier<Uni<? extends T>> supplier;

    public UniCreateFromDeferredSupplier(Supplier<Uni<? extends T>> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void subscribing(UniSubscriber<? super T> subscriber) {
        nonNull(subscriber, "subscriber");
        Uni<? extends T> uni;
        try {
            uni = supplier.get();
        } catch (Throwable e) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(e);
            return;
        }

        if (uni == null) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
        } else {
            AbstractUni.subscribe(uni, subscriber);
        }
    }
}
