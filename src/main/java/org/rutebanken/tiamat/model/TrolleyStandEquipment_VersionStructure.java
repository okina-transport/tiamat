

package org.rutebanken.tiamat.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


    "freeToUse",
    "charge",
    "currency",
public class TrolleyStandEquipment_VersionStructure
    extends SiteEquipment_VersionStructure
{

    protected Boolean freeToUse;
    protected BigDecimal charge;
    protected String currency;
    protected List<PaymentMethodEnumeration> paymentMethods;

    public Boolean isFreeToUse() {
        return freeToUse;
    }

    public void setFreeToUse(Boolean value) {
        this.freeToUse = value;
    }

    public BigDecimal getCharge() {
        return charge;
    }

    public void setCharge(BigDecimal value) {
        this.charge = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String value) {
        this.currency = value;
    }

    public List<PaymentMethodEnumeration> getPaymentMethods() {
        if (paymentMethods == null) {
            paymentMethods = new ArrayList<PaymentMethodEnumeration>();
        }
        return this.paymentMethods;
    }

}
