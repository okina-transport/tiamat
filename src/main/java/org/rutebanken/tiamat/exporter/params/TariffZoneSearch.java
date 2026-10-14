/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.exporter.params;

import com.google.common.base.MoreObjects;

import jakarta.ws.rs.QueryParam;

import java.time.Instant;

public class TariffZoneSearch implements SearchObject {

    @QueryParam(value = "tariffZoneQuery")
    private String query;

    private String organisationPrefix;

    private Instant pointInTime;

    /**
     * Zero-based page index. Only used when size is set.
     */
    private int page;

    /**
     * Page size. Null means no pagination (all matching tariff zones are returned).
     */
    private Integer size;

    private TariffZoneSearch(Builder builder) {
        this.query = builder.query;
        this.organisationPrefix = builder.organisationPrefix;
        this.pointInTime = builder.pointInTime;
        this.page = builder.page;
        this.size = builder.size;
    }

    public String getQuery() {
        return query;
    }

    public String getOrganisationPrefix() {
        return organisationPrefix;
    }

    public Instant getPointInTime() {
        return pointInTime;
    }

    public int getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("query", query)
                .add("organisationPrefix", organisationPrefix)
                .add("pointInTime", pointInTime)
                .add("page", size != null ? page : null)
                .add("size", size)
                .toString();
    }

    public static Builder newTariffZoneSearchBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String query;
        private String organisationPrefix;
        private Instant pointInTime;
        private int page;
        private Integer size;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder organisationPrefix(String organisationPrefix) {
            this.organisationPrefix = organisationPrefix;
            return this;
        }

        public Builder pointInTime(Instant pointInTime) {
            this.pointInTime = pointInTime;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public TariffZoneSearch build() {
            return new TariffZoneSearch(this);
        }
    }
}