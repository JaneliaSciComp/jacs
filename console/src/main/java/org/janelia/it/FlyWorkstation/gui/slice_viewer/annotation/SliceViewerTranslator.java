package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;


import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.SliceViewer;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton.Anchor;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton.Skeleton;
import org.janelia.it.FlyWorkstation.octree.ZoomLevel;
import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;
import org.janelia.it.FlyWorkstation.signal.Signal;
import org.janelia.it.FlyWorkstation.signal.Signal1;
import org.janelia.it.FlyWorkstation.signal.Slot1;
import org.janelia.it.FlyWorkstation.tracing.AnchoredVoxelPath;
import org.janelia.it.FlyWorkstation.tracing.PathTraceRequest;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: olbrisd
 * Date: 7/9/13
 * Time: 2:06 PM
 *
 * this class translates between the AnnotationModel, which says things like "I changed
 * a neuron", and the SliceViewer proper, which only wants to be told what to draw.
 * this class *only* handles the viewer, not the other traditional UI elements.
 *
 * this class's slots generally connect to the AnnotationModel, while its signals go
 * out to various UI elements.
 *
 * unfortunately, this class's comments and methods tends to use "anchor" and "annotation"
 * somewhat interchangeably, which can be confusing
 */
public class SliceViewerTranslator {

    private AnnotationModel annModel;
    private SliceViewer sliceViewer;

    // ----- slots
    public Slot1<TmWorkspace> loadWorkspaceSlot = new Slot1<TmWorkspace>() {
        @Override
        public void execute(TmWorkspace workspace) {
            workspaceLoaded(workspace);
        }
    };

    public Slot1<TmNeuron> selectNeuronSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            neuronSelected(neuron);
        }
    };

    public Slot1<TmGeoAnnotation> addAnnotationSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            addAnnotation(annotation);
        }
    };

    public Slot1<List<TmGeoAnnotation>> deleteAnnotationsSlot = new Slot1<List<TmGeoAnnotation>>() {
        @Override
        public void execute(List<TmGeoAnnotation> annotationList) {
            deleteAnnotations(annotationList);
        }
    };

    public Slot1<TmGeoAnnotation> reparentAnnotationSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            reparentAnnotation(annotation);
        }
    };

    public Slot1<TmGeoAnnotation> unmoveAnnotationSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            unmoveAnnotation(annotation);
        }
    };

    public Slot1<TmAnchoredPath> addAnchoredPathSlot = new Slot1<TmAnchoredPath>() {
        @Override
        public void execute(TmAnchoredPath path) {
            addAnchoredPath(path);
        }
    };

    public Slot1<TmAnchoredPath> removeAnchoredPathSlot = new Slot1<TmAnchoredPath>() {
        @Override
        public void execute(TmAnchoredPath path) {
            removeAnchoredPath(path);
        }
    };

    public Slot1<TmGeoAnnotation> annotationClickedSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            setNextParentSignal.emit(annotation.getId());
        }
    };

    public Slot1<Vec3> cameraPanToSlot = new Slot1<Vec3>() {
        @Override
        public void execute(Vec3 location) {
            cameraPanToSignal.emit(location);
        }
    };

    // ----- signals
    public Signal1<Vec3> cameraPanToSignal = new Signal1<Vec3>();

    public Signal1<TmGeoAnnotation> anchorAddedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<TmGeoAnnotation> anchorDeletedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<TmGeoAnnotation> anchorReparentedSignal = new Signal1<TmGeoAnnotation>();
    public Signal1<TmGeoAnnotation> anchorMovedSignal = new Signal1<TmGeoAnnotation>();
    public Signal clearSkeletonSignal = new Signal();
    public Signal1<Long> setNextParentSignal = new Signal1<Long>();

    public Signal1<AnchoredVoxelPath> anchoredPathAddedSignal = new Signal1<AnchoredVoxelPath>();
    public Signal1<AnchoredVoxelPath> anchoredPathRemovedSignal = new Signal1<AnchoredVoxelPath>();

    public SliceViewerTranslator(AnnotationModel annModel, SliceViewer sliceViewer) {
        this.annModel = annModel;
        this.sliceViewer = sliceViewer;

        setupSignals();
    }

    public void connectSkeletonSignals(Skeleton skeleton) {
        anchorAddedSignal.connect(skeleton.addAnchorSlot);
        anchorDeletedSignal.connect(skeleton.deleteAnchorSlot);
        anchorReparentedSignal.connect(skeleton.reparentAnchorSlot);
        anchorMovedSignal.connect(skeleton.moveAnchorSlot);
        clearSkeletonSignal.connect(skeleton.clearSlot);

        setNextParentSignal.connect(sliceViewer.getSkeletonActor().setNextParentSlot);

        anchoredPathAddedSignal.connect(skeleton.addAnchoredPathSlot);
        anchoredPathRemovedSignal.connect(skeleton.removeAnchoredPathSlot);
    }

    private void setupSignals() {
        annModel.workspaceLoadedSignal.connect(loadWorkspaceSlot);
        annModel.neuronSelectedSignal.connect(selectNeuronSlot);

        annModel.annotationAddedSignal.connect(addAnnotationSlot);
        annModel.annotationsDeletedSignal.connect(deleteAnnotationsSlot);
        annModel.annotationReparentedSignal.connect(reparentAnnotationSlot);
        annModel.annotationNotMovedSignal.connect(unmoveAnnotationSlot);

        annModel.anchoredPathAddedSignal.connect(addAnchoredPathSlot);
        annModel.anchoredPathRemovedSignal.connect(removeAnchoredPathSlot);
    }

    /**
     * called when model adds a new annotation
     */
    public void addAnnotation(TmGeoAnnotation annotation) {
        if (annotation != null) {
            anchorAddedSignal.emit(annotation);
        }
    }

    /**
     * called when the model changes the current neuron
     */
    public void neuronSelected(TmNeuron neuron) {
        if (neuron == null) {
            return;
        }

        // if there's a selected annotation in the neuron already, don't change it:
        Anchor anchor = sliceViewer.getSkeletonActor().getNextParent();
        if (anchor != null && neuron.getGeoAnnotationMap().containsKey(anchor.getGuid())) {
            return;
        }

        // if neuron has no annotations, clear old one anyway
        if (neuron.getGeoAnnotationMap().size() == 0) {
            setNextParentSignal.emit(null);
            return;
        }

        // find some annotation in selected neuron and select it, too
        // let's select the first endpoint we find:
        TmGeoAnnotation firstRoot = neuron.getRootAnnotations().get(0);
        for (TmGeoAnnotation link: firstRoot.getSubTreeList()) {
            if (link.getChildren().size() == 0) {
                setNextParentSignal.emit(link.getId());
                return;
            }
        }

    }

    /**
     * called by the model when it deletes annotations
     */
    public void deleteAnnotations(List<TmGeoAnnotation> annotationList) {
        // remove all the individual annotations from 2D view

        for (TmGeoAnnotation ann: annotationList) {
            anchorDeletedSignal.emit(ann);
        }
    }

    /**
     * called by the model when it changes the parent of an annotation
     */
    public void reparentAnnotation(TmGeoAnnotation annotation) {
        // pretty much a pass-through to the skeleton
        anchorReparentedSignal.emit(annotation);
    }

    /**
     * called by the model when it needs an annotation's anchor moved, whether
     * because we moved it, or because the UI moved it and the operation failed,
     * and we want it moved back
     */
    public void unmoveAnnotation(TmGeoAnnotation annotation) {
        anchorMovedSignal.emit(annotation);
    }

    public void addAnchoredPath(TmAnchoredPath path) {
        anchoredPathAddedSignal.emit(TAP2AVP(path));
    }

    public void removeAnchoredPath(TmAnchoredPath path) {
        anchoredPathRemovedSignal.emit(TAP2AVP(path));
    }

    /**
     * called by the model when it loads a new workspace
     */
    public void workspaceLoaded(TmWorkspace workspace) {
        if (workspace == null) {
            return;
        }

        // clear existing
        clearSkeletonSignal.emit();

        // note that we must add annotations in parent-child sequence
        //  so lines get drawn correctly
        for (TmNeuron neuron: workspace.getNeuronList()) {
            for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                for (TmGeoAnnotation ann: root.getSubTreeList()) {
                    anchorAddedSignal.emit(ann);
                }
            }
        }

        // draw anchored paths, too
        // could fold into above loop at some point
        for (TmNeuron neuron: workspace.getNeuronList()) {
            for (TmAnchoredPath path: neuron.getAnchoredPathMap().values()) {
                addAnchoredPath(path);
            }
        }
    }

    /**
     * convert between path formats
     *
     * @param path = TmAnchoredPath
     * @return corresponding AnchoredVoxelPath
     */
    private AnchoredVoxelPath TAP2AVP(TmAnchoredPath path) {
        // prepare the data:
        TmAnchoredPathEndpoints endpoints = path.getEndpoints();
        final PathTraceRequest.SegmentIndex inputSegmentIndex = new PathTraceRequest.SegmentIndex(endpoints.getAnnotationID1(),
                endpoints.getAnnotationID2());

        final ArrayList<ZoomedVoxelIndex> inputPath = new ArrayList<ZoomedVoxelIndex>();
        for (List<Integer> point: path.getPointList()) {
            inputPath.add(new ZoomedVoxelIndex(new ZoomLevel(0), point.get(0), point.get(1), point.get(2)));
        }

        // do a quick implementation of the interface:
        AnchoredVoxelPath voxelPath = new AnchoredVoxelPath() {
            PathTraceRequest.SegmentIndex segmentIndex;
            List<ZoomedVoxelIndex> path;

            {
                this.segmentIndex = inputSegmentIndex;
                this.path = inputPath;
            }

            @Override
            public PathTraceRequest.SegmentIndex getSegmentIndex() {
                return segmentIndex;
            }

            @Override
            public List<ZoomedVoxelIndex> getPath() {
                return path;
            }
        };

        return voxelPath;
    }
}
