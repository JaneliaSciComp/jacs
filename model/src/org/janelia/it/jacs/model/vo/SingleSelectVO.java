
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
