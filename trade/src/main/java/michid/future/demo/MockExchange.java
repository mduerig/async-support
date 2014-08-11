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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Map;
import java.util.Random;

import michid.future.offer.Exchange;
import michid.future.util.Scheduler;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockExchange implements Exchange {
    private static final Logger LOG = LoggerFactory.getLogger(MockExchange.class);
    private static final Random RND = new Random();

    private final Scheduler scheduler;
    private final Map<String, Double> rates;

    public MockExchange(Scheduler scheduler, Map<String, Double> rates) {
        this.scheduler = scheduler;
        this.rates = rates;
    }

    @Override
    public String toString() {
        return "Exchange{rates=" + rates + '}';
    }

    @Override
    public Promise<Double> convert(double amount, String from, String to) {
        return scheduler.runOnce(randomDelay(), MILLISECONDS,
                () -> doConvert(amount, from, to));
    }

    private static int randomDelay() {
        return 500 + RND.nextInt(1000);
    }

    private double doConvert(double amount, String from, String to)
            throws Exception {
        LOG.debug(this + " converting " + amount + " from {} to {}", from, to);
        return amount * getRate(rates, from, to);
    }

    private double getRate(Map<String, Double> rates, String from, String to) throws Exception {
        if (from.equals(to)) {
            return 1.0;
        } else {
            Double rate = rates.get(from + ':' + to);
            if (rate == null) {
                String message = this + ": no exchange rate for " + from + " to " + to;
                LOG.warn(message);
                throw new Exception(message);
            } else {
                return rate;
            }
        }
    }
}
