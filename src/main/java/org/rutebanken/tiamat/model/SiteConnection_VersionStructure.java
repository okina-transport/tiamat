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

package org.rutebanken.tiamat.model;

public class SiteConnection_VersionStructure
        extends Transfer_VersionStructure {

    protected SiteConnectionEndStructure from;
    protected SiteConnectionEndStructure to;
    protected NavigationPaths_RelStructure navigationPaths;

    public SiteConnectionEndStructure getFrom() {
        return from;
    }

    public void setFrom(SiteConnectionEndStructure value) {
        this.from = value;
    }

    public SiteConnectionEndStructure getTo() {
        return to;
    }

    public void setTo(SiteConnectionEndStructure value) {
        this.to = value;
    }

    public NavigationPaths_RelStructure getNavigationPaths() {
        return navigationPaths;
    }

    public void setNavigationPaths(NavigationPaths_RelStructure value) {
        this.navigationPaths = value;
    }

}
