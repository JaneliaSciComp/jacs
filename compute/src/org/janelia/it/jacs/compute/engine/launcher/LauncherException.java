
package org.janelia.it.jacs.compute.engine.launcher;

import org.janelia.it.jacs.compute.api.ComputeException;

/**
 * This class represents the exception that will be thrown by an implementation of ILauncher
 * when it fails to launch a SeriesDef
 *
 * @author Tareq Nabeel
 */
public class LauncherException extends ComputeException {
    /**
     * Construct a LauncherException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public LauncherException(String msg) {
        super(msg);
    }

    /**
     * Construct a LauncherException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public LauncherException(Throwable e) {
        super(e);
    }

    /**
     * Construct a LauncherException to wrap another exception.
     *
     * @param msg message of the exception
     * @param e   The exception to be wrapped.
     */
    public LauncherException(String msg, Throwable e) {
        super(msg, e);
    }
}
