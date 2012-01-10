
package org.janelia.it.jacs.model.vo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 31, 2006
 * Time: 4:12:19 PM
 */
public class MultiSelectVO extends ParameterVO {
    public static final String PARAM_MULTI_SELECT  = "MultiSelect";
    /**
     * Values are all the items which were actually selected
     */
    List<String> potentialChoices = new ArrayList<String>();

    /**
     * Choices describe all possible items which can be selected
     */
    List<String> actualUserChoices = new ArrayList<String>();

    /**
     * For GWT support
     */
    public MultiSelectVO() {
        super();
    }

    public MultiSelectVO(List<String> values, List<String> choices) {
        this.potentialChoices = values;
        this.actualUserChoices = choices;
    }

    public List<String> getPotentialChoices() {
        return this.potentialChoices;
    }

    public void setPotentialChoices(List<String> potentialChoices) {
        this.potentialChoices = potentialChoices;
    }

    public List<String> getActualUserChoices() {
        return this.actualUserChoices;
    }

    public String getStringValue() {
        StringBuffer sbuf = new StringBuffer();
        try {
            //sbuf.append("{");
            for (Iterator iterator = actualUserChoices.iterator(); iterator.hasNext();) {
                String s = (String) iterator.next();
                sbuf.append(s);
                if (iterator.hasNext()) {
                    sbuf.append(",");
                }
            }
            //sbuf.append("}");
        }
        catch (Exception e) {
            sbuf.append("ERROR IN LOOP");
            e.printStackTrace();
        }
        return sbuf.toString();
    }

    public String[] getValuesAsStringArray() {
        String[] returnStrings = new String[actualUserChoices.size()];
        for (int i = 0; i < actualUserChoices.size(); i++) {
            returnStrings[i] = actualUserChoices.get(i);
        }
        return returnStrings;
    }

    public void addActualChoice(String choice) {
        if (!actualUserChoices.contains(choice)) {
            actualUserChoices.add(choice);
        }
    }

    public void removeActualChoice(String choice) {
        if (actualUserChoices.contains(choice)) {
            actualUserChoices.remove(choice);
        }
    }

    public void setActualUserChoices(List<String> userChoices) {
        this.actualUserChoices = userChoices;
    }

    public boolean isValid() {
        for (String actualUserChoice : actualUserChoices) {
            if (!potentialChoices.contains(actualUserChoice)) {
                return false;
                //throw new ParameterException("Select list does not contain value " + value);
            }
        }
        return true;
    }

    public String getType() {
        return PARAM_MULTI_SELECT;
    }


    public String toString() {
        StringBuffer choicelist = new StringBuffer("");
        StringBuffer valuelist = new StringBuffer("");
        if (actualUserChoices != null) {
            for (Object actualUserChoice : actualUserChoices) {
                String s = (String) actualUserChoice;
                choicelist.append(s).append(", ");
            }
        }
        if (potentialChoices != null) {
            for (Object potentialChoice : potentialChoices) {
                String s = (String) potentialChoice;
                valuelist.append(s).append(", ");
            }
        }
        return "MultiSelectVO{" + super.toString() + ", " +
                "choices=" + choicelist.toString() +
                ", values=" + valuelist.toString() +
                '}';
    }
}
