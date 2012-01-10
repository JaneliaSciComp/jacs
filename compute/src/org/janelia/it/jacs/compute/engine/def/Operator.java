
package org.janelia.it.jacs.compute.engine.def;

/**
 * The class represents the different operators that can be used for loopUntil, forEach, and if attribute values
 * in process, sequence, and operation definitions
 *
 * @author Tareq Nabeel
 */
public enum Operator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN_OR_EQUALS
}