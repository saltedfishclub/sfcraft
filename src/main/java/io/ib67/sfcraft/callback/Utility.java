package io.ib67.sfcraft.callback;

import com.mojang.datafixers.util.Either;
import net.minecraft.util.Unit;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class Utility {
    private static final Either<?, Unit> EITHER_UNIT_R = Either.right(Unit.INSTANCE);

    static <T> Either<T, Unit> getEitherUnitR() {
        return (Either<T, Unit>) EITHER_UNIT_R;
    }

    static <T, R> R first(T[] ts, Function<T, R> action) {
        if (ts.length == 0) return null;
        return action.apply(ts[0]);
    }

    static <T> void forEach(T[] ts, Consumer<T> action) {
        for (T t : ts) {
            action.accept(t);
        }
    }

    static <T> boolean anyMatch(T[] ts, Predicate<T> predicate) {
        for (T handler : ts) {
            if (predicate.test(handler)) {
                return false;
            }
        }
        return true;
    }

    static <T, A> Optional<A> anyMatch(T[] ts, Function<T, A> function, Predicate<A> failurePredicate) {
        A r;
        for (T handler : ts) {
            r = function.apply(handler);
            if (failurePredicate.test(r)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }
}