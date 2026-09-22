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

import com.google.common.base.MoreObjects;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;
import java.math.BigInteger;


@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AlternativeText
        extends VersionedChildStructure {

    protected VersionOfObjectRefStructure dataManagedObjectRef;

    protected String attributeName;

    protected String useForLanguage;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "text_value")),
            @AttributeOverride(name = "lang", column = @Column(name = "text_lang"))
    })
    @Embedded
    protected EmbeddableMultilingualString text;

    @Transient
    protected BigInteger order;

    public VersionOfObjectRefStructure getDataManagedObjectRef() {
        return dataManagedObjectRef;
    }

    public void setDataManagedObjectRef(VersionOfObjectRefStructure value) {
        this.dataManagedObjectRef = value;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String value) {
        this.attributeName = value;
    }

    public String getUseForLanguage() {
        return useForLanguage;
    }

    public void setUseForLanguage(String value) {
        this.useForLanguage = value;
    }

    public EmbeddableMultilingualString getText() {
        return text;
    }

    public void setText(EmbeddableMultilingualString value) {
        this.text = value;
    }

    public BigInteger getOrder() {
        return order;
    }

    public void setOrder(BigInteger value) {
        this.order = value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("attributeName", attributeName)
                .add("useForLanguage", useForLanguage)
                .add("text", text)
                .add("order", order)
                .toString();
    }
}
