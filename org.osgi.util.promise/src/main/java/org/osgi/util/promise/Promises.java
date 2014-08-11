/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.util.promise;

import static java.util.Arrays.asList;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static helper methods for {@link Promise}s.
 * 
 * @ThreadSafe
 */
public class Promises {
	private Promises() {
		// disallow object creation
	}

	/**
	 * Create a new Promise that has been resolved with the specified value.
	 * 
	 * @param <T> The value type associated with the returned Promise.
	 * @param value The value of the resolved Promise.
	 * @return A new Promise that has been resolved with the specified value.
	 */
	public static <T> Promise<T> resolved(T value) {
        PromiseImpl<T> result = new PromiseImpl<>();
        result.success(value);
		return result;
	}

	/**
	 * Create a new Promise that has been resolved with the specified failure.
	 * 
	 * @param <T> The value type associated with the returned Promise.
	 * @param failure The failure of the resolved Promise. Must not be
	 *        {@code null}.
	 * @return A new Promise that has been resolved with the specified failure.
	 */
	public static <T> Promise<T> failed(Throwable failure) {
        checkNotNull(failure);
        PromiseImpl<T> result = new PromiseImpl<>();
        result.fail(failure);
        return result;
	}

    /**
	 * Create a new Promise that is a latch on the resolution of the specified
	 * Promises.
	 * 
	 * <p>
	 * The new Promise acts as a gate and must be resolved after all of the
	 * specified Promises are resolved.
	 * 
	 * @param <T> The value type of the List value associated with the returned
	 *        Promise.
	 * @param <S> A subtype of the value type of the List value associated with
	 *        the returned Promise.
	 * @param promises The Promises which must be resolved before the returned
	 *        Promise must be resolved. Must not be {@code null} and all of the
	 *        elements in the collection must not be {@code null}.
	 * @return A Promise that is resolved only when all the specified Promises
	 *         are resolved. The returned Promise must be successfully resolved
	 *         with a List of the values in the order of the specified Promises
	 *         if all the specified Promises are successfully resolved. The List
	 *         in the returned Promise is the property of the caller and is
	 *         modifiable. The returned Promise must be resolved with a failure
	 *         of {@link FailedPromisesException} if any of the specified
	 *         Promises are resolved with a failure. The failure
	 *         {@link FailedPromisesException} must contain all of the specified
	 *         Promises which resolved with a failure.
	 */
	public static <T, S extends T> Promise<List<T>> all(Collection<Promise<S>> promises) {
		if (promises.isEmpty()) {
            return resolved(new ArrayList<>());
		}

        PromiseImpl<List<T>> result = new PromiseImpl<>();
        AtomicInteger count = new AtomicInteger(promises.size());
        AtomicBoolean failed = new AtomicBoolean(false);

        for (Promise<S> promise : promises) {
            promise.onResolve(() -> {
                try {
                    if (promise.getFailure() != null) {
                        failed.set(true);
                    }
                    if (count.decrementAndGet() == 0) {
                        if (failed.get()) {
                            result.fail(failedPromiseException(promises));
                        } else {
                            result.success(values(promises));
                        }
                    }
                } catch (Exception e) {
                    // should never happen as promise is resolved already
                    assert false;
                }
            });
        }

        return result;
    }

    private static <T, S extends T> FailedPromisesException failedPromiseException(
            Collection<Promise<S>> promises) throws InterruptedException {
        List<Promise<?>> failed = new ArrayList<>();
        for (Promise<S> promise : promises) {
            Throwable failure = promise.getFailure();
            if (failure != null) {
                failed.add(promise);
            }
        }
        return new FailedPromisesException(failed, failed.get(0).getFailure().getCause());
    }

    private static <T, S extends T> List<T> values(Collection<Promise<S>> promises)
            throws InvocationTargetException, InterruptedException {
        List<T> values = new ArrayList<>();
        for (Promise<S> promise : promises) {
            values.add(promise.getValue());
        }
        return values;
    }

    /**
	 * Create a new Promise that is a latch on the resolution of the specified
	 * Promises.
	 * 
	 * <p>
	 * The new Promise acts as a gate and must be resolved after all of the
	 * specified Promises are resolved.
	 * 
	 * @param <T> The value type associated with the specified Promises.
	 * @param promises The Promises which must be resolved before the returned
	 *        Promise must be resolved. Must not be {@code null} and all of the
	 *        arguments must not be {@code null}.
	 * @return A Promise that is resolved only when all the specified Promises
	 *         are resolved. The returned Promise must be successfully resolved
	 *         with a List of the values in the order of the specified Promises
	 *         if all the specified Promises are successfully resolved. The List
	 *         in the returned Promise is the property of the caller and is
	 *         modifiable. The returned Promise must be resolved with a failure
	 *         of {@link FailedPromisesException} if any of the specified
	 *         Promises are resolved with a failure. The failure
	 *         {@link FailedPromisesException} must contain all of the specified
	 *         Promises which resolved with a failure.
	 */
	public static <T> Promise<List<T>> all(Promise<? extends T>... promises) {
        @SuppressWarnings("unchecked")
        List<Promise<T>> list = asList((Promise<T>[]) promises);
        return all(list);
	}

    static <T> void checkNotNull(T ref) {
        if (ref == null) {
            throw new NullPointerException();
        }
    }

}
