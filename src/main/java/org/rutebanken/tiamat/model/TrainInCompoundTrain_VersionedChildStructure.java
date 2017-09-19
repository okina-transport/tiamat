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

import java.math.BigInteger;


public class TrainInCompoundTrain_VersionedChildStructure
        extends VersionedChildStructure {

    protected MultilingualStringEntity description;
    protected CompoundTrainRef compoundTrainRef;
    protected TrainRefStructure trainRef;
    protected Train train;
    protected Boolean reversedOrientation;
    protected MultilingualStringEntity label;
    protected BigInteger order;

    public MultilingualStringEntity getDescription() {
        return description;
    }

    public void setDescription(MultilingualStringEntity value) {
        this.description = value;
    }

    public CompoundTrainRef getCompoundTrainRef() {
        return compoundTrainRef;
    }

    public void setCompoundTrainRef(CompoundTrainRef value) {
        this.compoundTrainRef = value;
    }

    public TrainRefStructure getTrainRef() {
        return trainRef;
    }

    public void setTrainRef(TrainRefStructure value) {
        this.trainRef = value;
    }

    public Train getTrain() {
        return train;
    }

    public void setTrain(Train value) {
        this.train = value;
    }

    public Boolean isReversedOrientation() {
        return reversedOrientation;
    }

    public void setReversedOrientation(Boolean value) {
        this.reversedOrientation = value;
    }

    public MultilingualStringEntity getLabel() {
        return label;
    }

    public void setLabel(MultilingualStringEntity value) {
        this.label = value;
    }

    public BigInteger getOrder() {
        return order;
    }

    public void setOrder(BigInteger value) {
        this.order = value;
    }

}
