
package org.janelia.it.jacs.compute.util;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2006
 * Time: 1:44:01 PM
 */
public class SafeTimeID {

    private static long lastRequest = new Date().getTime();

    public static synchronized long nextID() throws Exception {
        long nextID = new Date().getTime();
        int sanityCheck = 10000;
        while (nextID == lastRequest && sanityCheck > 0) {
            try {
                Thread.sleep(1L);
            }
            catch (Exception ex) {
                // Catching the thread sleep wake-up
            }
            nextID = new Date().getTime();
            sanityCheck--;
        }
        if (sanityCheck == 0) throw new Exception("Malfunction in ID SafeID loop");
        lastRequest = nextID;
        return nextID;
    }

}
