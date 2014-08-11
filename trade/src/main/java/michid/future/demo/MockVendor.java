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

import java.util.Random;

import michid.future.offer.Vendor;
import michid.future.util.Scheduler;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockVendor implements Vendor {
    private static final Logger LOG = LoggerFactory.getLogger(MockVendor.class);
    private static final Random RND = new Random();

    private final String name;
    private final Scheduler scheduler;

    public MockVendor(String name, Scheduler scheduler) {
        this.name = name;
        this.scheduler = scheduler;
    }

    @Override
    public Promise<Offer> getOffer(String item) {
        return scheduler.runOnce(randomDelay(), MILLISECONDS,
                () -> randomOffer(item));
    }

    private static int randomDelay() {
        return 500 + RND.nextInt(1000);
    }

    private Offer randomOffer(String item) throws Exception {
        LOG.debug("{} creating offer for {}", this, item);
        if ("vendor.fail".equals(item)) {
            String message = name + " failed to produce an offer for " + item;
            LOG.warn(message);
            throw new Exception(message);
        } else if ("exchange.fail".equals(item)) {
            return new Offer(this, item, randomPrice(), "JPY");
        } else if (hasItem(item)) {
            return new Offer(this, item, randomPrice(), randomCurrency());
        } else {
            LOG.warn("{} no such item: {}", this, item);
            return noOffer();
        }
    }

    private static String randomCurrency() {
        switch (RND.nextInt(4)) {
            case 0: return "EUR";
            case 1: return "GBP";
            case 2: return "CHF";
            case 3: return "USD";
        }
        throw new AssertionError();
    }

    private static double randomPrice() {
        return RND.nextInt(1000);
    }

    private static boolean hasItem(String item) {
        return RND.nextInt(10) > 0;
    }

    @Override
    public String toString() {
        return name;
    }

}
