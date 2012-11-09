package org.janelia.it.jacs.compute.service.fileDiscovery;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 10/16/12
 * Time: 11:38 AM
 */
public class SampleRun {
    public long fragmentCollection;
    public String owner;

    public SampleRun(long fragmentCollection, String owner) {
        this.fragmentCollection = fragmentCollection;
        this.owner = owner;
    }

    public long getFragmentCollection() {
        return fragmentCollection;
    }

    public void setFragmentCollection(long fragmentCollection) {
        this.fragmentCollection = fragmentCollection;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}