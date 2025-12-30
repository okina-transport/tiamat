package org.rutebanken.tiamat.model;

import org.rutebanken.netex.model.PaymentMethodEnumeration;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class TypeOfPaymentMethod_ValueStructure extends TypeOfValue_VersionStructure {

    private Boolean automatedUse;

    @Enumerated(EnumType.STRING)
    private PaymentMethodEnumeration paymentMethod;

    public Boolean getAutomatedUse() {
        return automatedUse;
    }

    public void setAutomatedUse(Boolean automatedUse) {
        this.automatedUse = automatedUse;
    }

    public PaymentMethodEnumeration getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethodEnumeration paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
