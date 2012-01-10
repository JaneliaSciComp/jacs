
package org.janelia.it.jacs.model.metadata;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 5, 2006
 * Time: 4:01:43 PM
 */
public class CollectionObservation implements Serializable, IsSerializable {

    private String value = "";
    private String units = "";
    private String instrument = "";
    private String comment = "";

    public CollectionObservation() {
    }

    public CollectionObservation(String value, String units, String instrument, String comment) {
        this.value = value;
        this.units = units;
        this.instrument = instrument;
        this.comment = comment;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String toString() {
        if (value == null || value.equals(""))
            if (comment == null)
                return "";
            else
                return comment;
        else {
            String temp = value;
            if (units != null && !units.equals("")) temp = temp.concat(" ").concat(units);
            if (instrument != null && !instrument.equals("")) temp = temp.concat(" [").concat(instrument).concat("]");
            if (comment != null && !comment.equals("")) temp = temp.concat(" (").concat(comment).concat(")");
            return temp;
        }
    }
}
