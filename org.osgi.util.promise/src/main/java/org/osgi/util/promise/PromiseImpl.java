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

import static org.osgi.util.promise.Promises.checkNotNull;
import static scala.concurrent.duration.Duration.Inf;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Filter;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import org.osgi.util.function.Function;
import org.osgi.util.function.Predicate;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

/**
 * This implementation of {@code Promise} simply delegates back to the respective
 * functionality from the Akka library.
 * <p>
 * <em>Note</em>: This is a proof of concept implementation, which is not tested
 * beyond serving its purpose for the trade demo application.
 */
public class PromiseImpl<T> implements Promise<T> {
    private final ExecutionContext ec = ExecutionContexts.fromExecutor(Runnable::run);
    private final scala.concurrent.Promise<T> delegate = Futures.promise();

    void success(T value) {
        delegate.complete(new scala.util.Success<>(value));
    }

    void fail(Throwable failure) {
        delegate.complete(new scala.util.Failure<>(failure));
    }

    Promise<Void> resolveWith(Promise<? extends T> promise) {
        if (promise == null) {
            return Promises.resolved(null);
        }

        PromiseImpl<Void> result = new PromiseImpl<>();

        promise.onResolve(() -> {
            try {
                Throwable failure = promise.getFailure();
                if (failure != null) {
                    fail(failure);
                } else {
                    success(promise.getValue());
                }
                result.success(null);
            } catch (IllegalStateException e) {
                result.fail(e);
            } catch (Exception e) {
                // should never happen as promise is resolved already
                assert false;
            }
        });

        return result;
    }

    @Override
    public boolean isDone() {
        return delegate.isCompleted();
    }

    @Override
    public T getValue() throws InvocationTargetException, InterruptedException {
        try {
            return Await.result(delegate.future(), Inf());
        } catch (ExecutionException e) {
            throw new InvocationTargetException(e.getCause());
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

    @Override
    public Throwable getFailure() throws InterruptedException {
        try {
            getValue();
            return null;
        } catch (InvocationTargetException e) {
            return e.getCause();
        }
    }

    @Override
    public Promise<T> onResolve(Runnable callback) {
        checkNotNull(callback);
        delegate.future()
            .onComplete(new OnComplete<T>() {
                @Override
                public void onComplete(Throwable failure, T success) throws Throwable {
                    callback.run();
                }
            }, ec);
        return this;
    }

    @Override
    public <R> Promise<R> then(Success<? super T, ? extends R> success, Failure failure) {
        PromiseImpl<R> result = new PromiseImpl<>();

        delegate.future()
            .andThen(new OnComplete<T>() {
                @Override
                public void onComplete(Throwable fail, T value) throws Throwable {
                    try {
                        if (value != null) {
                            if (success != null) {
                                result.resolveWith(((Success<T, ? extends R>) success).call(PromiseImpl.this));
                            } else {
                                result.success(null);
                            }
                        } else {
                            if (failure != null) {
                                failure.fail(PromiseImpl.this);
                            }
                            result.fail(fail);
                        }
                    } catch (Throwable e) {
                        result.fail(e);
                    }
                }
            }, ec);

        return result;
    }

    @Override
    public <R> Promise<R> then(Success<? super T, ? extends R> success) {
        return then(success, null);
    }

    @Override
    public Promise<T> filter(Predicate<? super T> predicate) {
        PromiseImpl<T> result = new PromiseImpl<>();

        delegate.future()
            .filter(Filter.filterOf(predicate::test), ec)
            .onComplete(new OnComplete<T>() {
                @Override
                public void onComplete(Throwable failure, T value) throws Throwable {
                    if (value != null) {
                        result.success(value);
                    } else {
                        result.fail(failure);
                    }
                }
            }, ec);

        return result;
    }

    @Override
    public <R> Promise<R> map(Function<? super T, ? extends R> mapper) {
        checkNotNull(mapper);
        PromiseImpl<R> result = new PromiseImpl<>();

        delegate.future()
            .map(new Mapper<T, R>() {
                @Override
                public R apply(T value) {
                    return mapper.apply(value);
                }
            }, ec)
            .onComplete(new OnComplete<R>() {
                @Override
                public void onComplete(Throwable failure, R value) throws Throwable {
                    if (value != null) {
                        result.success(value);
                    } else {
                        result.fail(failure);
                    }
                }
            }, ec);

        return result;
    }

    @Override
    public <R> Promise<R> flatMap(Function<? super T, Promise<? extends R>> mapper) {
        checkNotNull(mapper);
        PromiseImpl<R> result = new PromiseImpl<>();

        delegate.future()
            .flatMap(new Mapper<T, Future<Promise<? extends R>>>() {
                @Override
                public Future<Promise<? extends R>> apply(T value) {
                    return Futures.successful(mapper.apply(value));
                }
            }, ec)
            .onComplete(new OnComplete<Promise<? extends R>>() {
                @Override
                public void onComplete(Throwable failure, Promise<? extends R> value) throws Throwable {
                    if (value != null) {
                        result.resolveWith(value);
                    } else {
                        result.fail(failure);
                    }
                }
            }, ec);

        return result;
    }

    @Override
    public Promise<T> recover(Function<Promise<?>, ? extends T> recovery) {
        checkNotNull(recovery);
        PromiseImpl<T> result = new PromiseImpl<>();

        delegate.future()
            .onComplete(new OnComplete<T>() {
                @Override
                public void onComplete(Throwable failure, T value) throws Throwable {
                    if (value != null) {
                        result.success(value);
                    } else {
                        try {
                            T recover = recovery.apply(PromiseImpl.this);
                            if (recover != null) {
                                result.success(recover);
                            } else {
                                result.fail(failure);
                            }
                        } catch (Throwable e) {
                            result.fail(e);
                        }
                    }
                }
            }, ec);

        return result;
    }

    @Override
    public Promise<T> recoverWith(Function<Promise<?>, Promise<? extends T>> recovery) {
        checkNotNull(recovery);
        PromiseImpl<T> result = new PromiseImpl<>();

        delegate.future()
            .onComplete(new OnComplete<T>() {
                @Override
                public void onComplete(Throwable failure, T value) throws Throwable {
                    if (value != null) {
                        result.success(value);
                    } else {
                        try {
                            Promise<? extends T> recover = recovery.apply(PromiseImpl.this);
                            if (recover != null) {
                                result.resolveWith(recover);
                            } else {
                                result.fail(failure);
                            }
                        } catch (Throwable e) {
                            result.fail(e);
                        }
                    }
                }
            }, ec);

        return result;
    }

    @Override
    public Promise<T> fallbackTo(Promise<? extends T> fallback) {
        checkNotNull(fallback);
        PromiseImpl<T> result = new PromiseImpl<>();

        delegate.future()
            .onComplete(new OnComplete<T>() {
                @Override
                public void onComplete(Throwable failure, T value) throws Throwable {
                    if (value != null) {
                        result.success(value);
                    } else {
                        fallback.onResolve(() -> {
                            try {
                                Throwable fbf = fallback.getFailure();
                                if (fbf != null) {
                                    result.fail(failure);
                                } else {
                                    result.success(fallback.getValue());
                                }
                            } catch (Exception e) {
                                // should never happen as fallback is resolved already
                                assert false;
                            }
                        });

                    }
                }
            }, ec);

        return result;
    }
}
