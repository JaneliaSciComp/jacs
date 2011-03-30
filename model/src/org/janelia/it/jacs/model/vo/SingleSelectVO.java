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

package org.janelia.it.jacs.model.vo;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 31, 2006
 * Time: 4:12:08 PM
 */
public class SingleSelectVO extends ParameterVO {
    public static final String PARAM_SINGLE_SELECT = "SingleSelect";
    List<String> potentialChoices;
    String actualUserChoice;

    /**
     * For GWT support
     */
    public SingleSelectVO() {
        super();
    }

    public SingleSelectVO(List<String> potentialChoices, String actualChoice) {
        this.potentialChoices = potentialChoices;
        this.actualUserChoice = actualChoice;
    }

    public List<String> getPotentialChoices() {
        return this.potentialChoices;
    }

    public void setPotentialChoices(List<String> potentialChoices) {
        this.potentialChoices = potentialChoices;
    }

    public String getStringValue() {
        return getActualUserChoice();
    }

    public String getActualUserChoice() {
        return this.actualUserChoice;
    }

    public void setActualUserChoice(String choice) {
        this.actualUserChoice = choice;
    }

    public boolean isValid() {
        return potentialChoices.contains(actualUserChoice);
    }

    public String getType() {
        return PARAM_SINGLE_SELECT;
    }

    public String toString() {
        StringBuffer potentialsList = new StringBuffer();
        for (Object potentialChoice : potentialChoices) {
            String s = (String) potentialChoice;
            potentialsList.append(s).append(", ");
        }
        return "SingleSelectVO{" + super.toString() + ", " +
                ", values=" + potentialsList.toString() +
                ", choice=" + actualUserChoice +
                '}';
    }

}
