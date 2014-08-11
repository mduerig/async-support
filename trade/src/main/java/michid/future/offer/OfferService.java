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
package michid.future.offer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static michid.future.util.PromiseUtils.all;
import static michid.future.util.PromiseUtils.any;

import java.util.stream.Stream;

import michid.future.offer.Vendor.Offer;
import michid.future.util.Scheduler;
import org.osgi.util.promise.Promise;

public class OfferService {
    private final Exchange primaryExchange;
    private final Exchange secondaryExchange;
    private final Scheduler scheduler;

    public OfferService(Exchange primaryExchange, Exchange secondaryExchange, Scheduler scheduler) {
        this.primaryExchange = primaryExchange;
        this.secondaryExchange = secondaryExchange;
        this.scheduler = scheduler;
    }

    public Promise<Stream<Offer>> getOffers(Stream<Vendor> vendors, String item) {
        return all(
            vendors.map(vendor ->
                getOfferWithTimeout(vendor, item)));
    }

    private Promise<Offer> getOfferWithTimeout(Vendor vendor, String item) {
        return any(
            getOffer(vendor, item),
            scheduler.timeOut(3000, MILLISECONDS, vendor.noOffer()));
    }

    private Promise<Offer> getOffer(Vendor vendor, String item) {
        return vendor
            .getOffer(item)
            .flatMap(this::convertToEuro)
            .recover(failed -> vendor.noOffer());
    }

    private Promise<Offer> convertToEuro(Offer offer) {
        return convert(primaryExchange, offer, "EUR")
                .recoverWith(failed ->
                    convert(secondaryExchange, offer, "EUR"));
    }

    private static Promise<Offer> convert(Exchange exchange, Offer offer, String currency) {
        return exchange
            .convert(offer.price, offer.currency, currency)
            .map(euros -> offer.toCurrency(euros, currency));
    }

//    public Promise<Stream<Offer>> getOffers2(Stream<Vendor> vendors, String item) {
//        return all(
//            vendors.map(vendor -> any(vendor
//                .getOffer(item).flatMap(offer -> primaryExchange
//                .convert(offer.price, offer.currency, "EUR")
//                .recoverWith(failed -> secondaryExchange
//                .convert(offer.price, offer.currency, "EUR"))
//                .map(euros -> offer.toCurrency(euros, "EUR")))
//                .recover(failed -> vendor.noOffer()),
//            scheduler.timeOut(3000, MILLISECONDS, vendor.noOffer()))));
//    }

}
