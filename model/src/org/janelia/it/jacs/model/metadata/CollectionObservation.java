/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
