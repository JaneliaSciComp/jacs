package org.janelia.it.jacs.compute.wsrest.json;

import org.codehaus.jackson.annotate.JsonProperty;
import org.janelia.it.jacs.model.tasks.Event;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;

/**
 * Created by goinac on 10/23/15.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonTaskEvent {

    private Long taskId;
    private String eventType;
    private int eventIndex;
    private String description;
    private Date timestamp;

    JsonTaskEvent() {
        // needed by JAXB serializer
    }

    public JsonTaskEvent(Event e) {
        taskId = e.getTask().getObjectId();
        eventType = e.getEventType();
        eventIndex = e.getEventIndex();
        description = e.getDescription();
        timestamp = e.getTimestamp();
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventIndex() {
        return eventIndex;
    }

    public String getDescription() {
        return description;
    }

    public Date getTimestamp() {
        return timestamp;
    }

}
