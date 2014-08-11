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
package michid.future.demo;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import michid.future.util.Scheduler;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

public class SimpleScheduler implements Scheduler, Closeable {
    private final ScheduledExecutorService scheduler;

    public SimpleScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public <T> Promise<T> runOnce(int delay, TimeUnit unit, Callable<T> task) {
        Deferred<T> deferred = new Deferred<>();
        scheduler.schedule(() -> {
            try {
                deferred.resolve(task.call());
            } catch (Exception e) {
                deferred.fail(e);
            }
        }, delay, unit);

        return deferred.getPromise();
    }

    @Override
    public <T> Promise<T> timeOut(int delay, TimeUnit unit, T value) {
        return runOnce(delay, unit, () -> value);
    }

    @Override
    public void close() throws IOException {
        scheduler.shutdownNow();
    }
}
