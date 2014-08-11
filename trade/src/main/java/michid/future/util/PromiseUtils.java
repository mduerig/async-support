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
package michid.future.util;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import org.osgi.util.function.Function;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

public final class PromiseUtils {
    private PromiseUtils() {}

    public static <T> Promise<Stream<T>> all(Stream<Promise<T>> promises) {
        return Promises.all(promises.collect(toList())).map(toStream());
    }

    private static <T> Function<List<T>, Stream<T>> toStream() {
        return list -> list.stream();
    }

    public static <T, S extends T> Promise<T> any(Promise<S>... promises) {
        Deferred<T> deferred = new Deferred<>();
        Stream.of(promises).forEach(p ->
            p.onResolve(() -> deferred.resolveWith(p)));
        return deferred.getPromise();
    }
}
