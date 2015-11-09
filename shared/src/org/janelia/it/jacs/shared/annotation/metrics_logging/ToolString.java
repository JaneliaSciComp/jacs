/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.annotation.metrics_logging;

/**
 * This string describes a tool.  Limited to a certain size.
 *
 * @author fosterl
 */
public class ToolString extends MetricsLoggingWrapper {
    private static final int MAX_LEN = 20;
    public ToolString(String string) {
        super(string);
        if (string.length() > MAX_LEN) {
            throw new IllegalArgumentException("Exceeded max string length for this purpose.  Max is " + MAX_LEN);
        }
    }
}
