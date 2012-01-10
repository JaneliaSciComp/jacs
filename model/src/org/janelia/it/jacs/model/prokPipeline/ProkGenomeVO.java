
package org.janelia.it.jacs.model.prokPipeline;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 17, 2009
 * Time: 9:27:32 AM
 */
public class ProkGenomeVO implements IsSerializable, Serializable {
    private List<Event> _events = new ArrayList<Event>();
    private String _localGenomeDirName;
    private String _targetOutputDirectory;

    public ProkGenomeVO() {
    }

    public ProkGenomeVO(String _localGenomeDirName) {
        this._localGenomeDirName = _localGenomeDirName;
    }

    public List<Event> getEvents() {
        return _events;
    }

    public void setEvents(List<Event> events) {
        this._events = events;
    }

    public String getLocalGenomeDirName() {
        return _localGenomeDirName;
    }

    public String getTargetOutputDirectory() {
        return _targetOutputDirectory;
    }

    public void setLocalGenomeDirName(String localGenomeDirName) {
        _localGenomeDirName = localGenomeDirName;
    }

    public void setTargetOutputDirectory(String directoryPath) {
        _targetOutputDirectory = directoryPath;
    }
}
