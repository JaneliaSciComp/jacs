package org.janelia.it.jacs.model.domain.gui.search.filters;

import org.janelia.it.jacs.model.domain.enums.Operator;

public class AttributeFilter implements Filter {

    private String attributeName;
    private Operator operator;
    private String parameter1;
    private String parameter2;

    @Override
    public String getLabel() {
        return attributeName;
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getParameter1() {
        return parameter1;
    }

    public void setParameter1(String parameter1) {
        this.parameter1 = parameter1;
    }

    public String getParameter2() {
        return parameter2;
    }

    public void setParameter2(String parameter2) {
        this.parameter2 = parameter2;
    }
}
