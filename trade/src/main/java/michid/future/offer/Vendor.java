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

import org.osgi.util.promise.Promise;

public interface Vendor {
    class Offer {
        public final Vendor vendor;
        public final String item;
        public final double price;
        public final String currency;

        public Offer(Vendor vendor, String item, double price, String currency) {
            this.vendor = vendor;
            this.item = item;
            this.price = price;
            this.currency = currency;
        }

        public Offer toCurrency(double price, String currency) {
            return new Offer(vendor, item, price, currency);
        }

        @Override
        public String toString() {
            return "Offer{" +
                "vendor='" + vendor + "'," +
                "item='" + item + "'," +
                "price=" + price + "'," +
                "currency='" + currency + '\'' +
            '}';
        }
    }

    Promise<Offer> getOffer(String item);

    default Offer noOffer() {
        return new Offer(this, "", -1, "");
    }

}
