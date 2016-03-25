package org.janelia.it.jacs.compute.engine.def;

import java.io.Serializable;
import java.util.Collection;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This class represents a condition within forEach, if, and loopUntil attributes
 *
 * @author Tareq Nabeel
 */
public class Condition implements Serializable {

    /**
     * The name of the parameter in process data to look for when evaluating
     * this condition
     */
    private String name;

    /**
     * The value for <code>name</code> in process defintion
     */
    private Object expectedValue;

    /**
     * The operator to use when evaluating this condition at runtime
     */
    private Operator operator;

    /**
     * Constructor
     *
     * @param name          The name of the parameter in process data to look for when evaluating this condition
     * @param expectedValue The expected value for <code>name</code> in process data at runtime
     *                      when this condition is evaluated
     * @param operator      The operator to use when evaluating this condition at runtime
     */
    protected Condition(String name, Object expectedValue, Operator operator) {
        this.name = name;
        this.expectedValue = expectedValue;
        this.operator = operator;
    }

    /**
     * This method returns true if expectedValue in this condition is satisfied with the actual value
     * in processData at runtime.  It does the appropriate comparison using the operator in this
     * condition
     *
     * @param processData ProcessData or QueueMessage instance
     * @return true if condition is met, false otherwise
     */
    public boolean isSatisfiedBy(IProcessData processData) {
        Object actualValue = processData.getItem(name);
        switch (operator) {
            case EQUALS:
                return expectedValue.equals(actualValue);
            case NOT_EQUALS:
                return !expectedValue.equals(actualValue);
            case GREATER_THAN:
            case LESS_THAN:
            case GREATER_THAN_OR_EQUALS:
            case LESS_THAN_OR_EQUALS:
                return compareTo(actualValue, expectedValue, operator);
            case IS_NOT_NULL:
                return actualValue!=null;
            case IS_NULL:
                return actualValue==null;
            case IS_NOT_EMPTY:
                if (actualValue instanceof Collection) {
                    return !((Collection<?>)actualValue).isEmpty();
                }
                else {
                    return actualValue!=null && !StringUtils.isEmpty(actualValue.toString());
                }
            case IS_EMPTY:
                if (actualValue instanceof Collection) {
                    return ((Collection<?>)actualValue).isEmpty();
                }
                else {
                    return actualValue==null || StringUtils.isEmpty(actualValue.toString());
                }
            default:
                throw new RuntimeException("not supported");
        }
    }

    /**
     * Used before casting actual value and expected value to Comparable
     *
     * @param actualValue   the value of <code>name</code> in process data at runtime
     * @param expectedValue the value of <code>name</code> in process definition
     */
    private void validateComparables(Object actualValue, Object expectedValue) {
        if (!(actualValue instanceof Comparable)) {
            throw new IllegalArgumentException("Actual value not an instanceof Comparable:" + (actualValue == null ? "null" : actualValue.getClass().getName()));
        }
        if (!(expectedValue instanceof Comparable)) {
            throw new IllegalArgumentException("Expected value not an instanceof Comparable:" + (expectedValue == null ? "null" : expectedValue.getClass().getName()));
        }
    }

    /**
     * Returns the result of comparable1.compareTo(comparable2)
     *
     * @param actualValue   the value of <code>name</code> in process data at runtime
     * @param expectedValue the value of <code>name</code> in process definition
     * @param operator      the operator to use to make the evaluation
     * @return boolean to tell comparison truth
     */
    private boolean compareTo(Object actualValue, Object expectedValue, Operator operator) {
        validateComparables(actualValue, expectedValue);
        Comparable actualValueCompare = (Comparable) actualValue;
        Comparable expectedValueCompare = (Comparable) expectedValue;
        int compareToValue = actualValueCompare.compareTo(expectedValueCompare);
        switch (operator) {
            case GREATER_THAN_OR_EQUALS:
                return compareToValue >= 0;
            case LESS_THAN_OR_EQUALS:
                return compareToValue <= 0;
            case GREATER_THAN:
                return compareToValue > 0;
            case LESS_THAN:
                return compareToValue < 0;
            default:
                throw new IllegalArgumentException("Illegal operator:" + operator);
        }
    }

    /**
     * Used by DefLoader to create a Condition object out of the loopUntil, forEach, and if attributes
     * of actions in the process definition
     *
     * @param conditionStr the attribute value
     * @return the Condition instance
     */
    protected static Condition create(String conditionStr) {
        if (conditionStr == null) {
            return null;
        }
        Operator operator = getOperator(conditionStr);
        String[] nameValue = getNameValue(operator, conditionStr);
        Object value = createConditionValue(nameValue[1]);
        return new Condition(nameValue[0], value, operator);
    }

    /**
     * The operator used in the loopUntil, forEach, and if attribute
     *
     * @param condition the string value of the attrubute
     * @return the operation in the condition
     */
    private static Operator getOperator(String condition) {
        if (condition.contains("!=")) {
            return Operator.NOT_EQUALS;
        }
        else if (condition.contains(">=")) {
            return Operator.GREATER_THAN_OR_EQUALS;
        }
        else if (condition.contains("<=")) {
            return Operator.LESS_THAN_OR_EQUALS;
        }
        else if (condition.contains("=")) {
            return Operator.EQUALS;
        }
        else if (condition.contains(">")) {
            return Operator.GREATER_THAN;
        }
        else if (condition.contains("<")) {
            return Operator.LESS_THAN;
        }
        else if (condition.contains("is not null")) {
            return Operator.IS_NOT_NULL;
        }
        else if (condition.contains("is null")) {
            return Operator.IS_NULL;
        }
        else if (condition.contains("is not empty")) {
            return Operator.IS_NOT_EMPTY;
        }
        else if (condition.contains("is empty")) {
            return Operator.IS_EMPTY;
        }
        else {
            throw new IllegalArgumentException("Illegal operator in condition" + condition);
        }
    }

    /**
     * Returns the name and value of condition in the loopUntil, forEach, and if attribute
     *
     * @param operator  the operator
     * @param condition the string value of the attrubute
     * @return the name and value as a String array
     */
    private static String[] getNameValue(Operator operator, String condition) {
        String[] nameValue;
        switch (operator) {
            case NOT_EQUALS:
                nameValue = condition.split("!=");
                break;
            case EQUALS:
                nameValue = condition.split("=");
                break;
            case GREATER_THAN_OR_EQUALS:
                nameValue = condition.split(">=");
                break;
            case LESS_THAN_OR_EQUALS:
                nameValue = condition.split("<=");
                break;
            case GREATER_THAN:
                nameValue = condition.split(">");
                break;
            case LESS_THAN:
                nameValue = condition.split("<");
                break;
            case IS_NOT_NULL:
                String[] array = {condition.replaceFirst(" is not null", ""), ""};
                nameValue = array;
                break;
            case IS_NULL:
                String[] array2 = {condition.replaceFirst(" is null", ""), ""};
                nameValue = array2;
                break;
            case IS_NOT_EMPTY:
                String[] array3 = {condition.replaceFirst(" is not empty", ""), ""};
                nameValue = array3;
                break;
            case IS_EMPTY:
                String[] array4 = {condition.replaceFirst(" is empty", ""), ""};
                nameValue = array4;
                break;
            default:
                throw new IllegalArgumentException("Illegal operator:" + operator);
        }
        if (nameValue == null || nameValue.length != 2) {
            throw new IllegalArgumentException("Condition value cannot be empty");
        }
        return nameValue;
    }

    /**
     * Creates an object representing the expected value of the condition
     *
     * @param valueStr the string representation of the condition value in the process definition
     * @return the expected value object
     */
    private static Object createConditionValue(String valueStr) {
        Object value;
        if (isBoolean(valueStr)) {
            value = Boolean.valueOf(valueStr);
        }
        else {
            value = parseNumber(valueStr);
            if (value == null) { // must be String then
                value = valueStr;
            }
        }
        return value;
    }

    /**
     * Returns true if the condition value is a boolean
     * @param value string to check boolean on
     * @return boolean if string is a boolean
     */
    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    /**
     * Parses the condition value as a number
     * @param value string to check if a number
     * @return returns Number object if string passed is a number
     */
    private static Number parseNumber(String value) {
        try {
            if (value.indexOf('.') == -1) {    //german not supported :)
                return Long.parseLong(value);
            }
            else {
                return Double.parseDouble(value);
            }
        }
        catch (Exception e) {
            return null;
        }
    }

	public String getName() {
		return name;
	}

	public Object getExpectedValue() {
		return expectedValue;
	}

	public Operator getOperator() {
		return operator;
	}
}
