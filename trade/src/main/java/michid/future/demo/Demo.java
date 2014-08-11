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

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.io.Closeable;
import java.io.IOException;
import java.util.Scanner;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import michid.future.offer.Exchange;
import michid.future.offer.OfferService;
import michid.future.offer.Vendor;

public class Demo implements Closeable {
    private final SimpleScheduler scheduler = new SimpleScheduler(newSingleThreadScheduledExecutor());

    private final Exchange exchange1 = new MockExchange(scheduler,
        ImmutableMap.of(
            "USD:EUR", 0.75,
            "CHF:EUR", 0.82));

    private final Exchange exchange2 = new MockExchange(scheduler,
        ImmutableMap.of(
            "GBP:EUR", 1.26));

    private final OfferService offerService = new OfferService(exchange1, exchange2, scheduler);

    private Stream<Vendor> vendors() {
        return Stream.of(
            new MockVendor("vendor1", scheduler),
            new MockVendor("vendor2", scheduler),
            new MockVendor("vendor3", scheduler),
            new MockVendor("vendor4", scheduler));
    }

    private void printOffers(String item) {
        offerService.getOffers(vendors(), item).then(
            success -> {
                System.out.println("Offers for item " + item);
                success.getValue().forEach(System.out::println);
                return null;
            },
            failed -> {
                System.out.println("Could not get offer for item " + item);
                System.out.println(failed.getFailure().getMessage());
            });
    }

    public void run(Scanner scanner) {
        while(scanner.hasNextLine()) {
            String cmd = scanner.nextLine();
            if ("exit".equals(cmd)) {
                return;
            }
            printOffers(cmd);
        }
    }

    @Override
    public void close() throws IOException {
        scheduler.close();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Enter name of an item to get an offer for or 'exit' to terminate.");
        Demo demo = new Demo();
        demo.run(new Scanner(System.in));
        demo.close();
    }
}
