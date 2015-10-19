
package org.janelia.it.jacs.web.gwt.search.client.panel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 7, 2007
 * Time: 3:16:17 PM
 */
public class SelectionCounter {
    int count = 0;

    public void increment() {
        count++;
    }

    public void decrement() {
        count--;
    }

    public int getCount() {
        return count;
    }
}
