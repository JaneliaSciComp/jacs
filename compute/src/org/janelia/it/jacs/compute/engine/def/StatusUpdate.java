
package org.janelia.it.jacs.compute.engine.def;

/**
 * When an operation should update process status.  Comes into play in an asynchronous
 * context
 */
public enum StatusUpdate {
    ON_SUCCESS, ON_FAILURE, NEVER
}
