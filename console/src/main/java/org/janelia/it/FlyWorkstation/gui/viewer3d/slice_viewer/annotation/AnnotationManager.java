package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;



// workstation imports


import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import sun.awt.ModalityListener;

import javax.swing.*;


public class AnnotationManager
{

    ModelMgr modelMgr;

    // annotation model object
    private AnnotationModel annotationModel;
    
    private Entity initialEntity;

    // current workspace, brain sample
    // current neuron

    // current neurite (?)

    // current annotation

    // constants
    public static final String WORKSPACES_FOLDER_NAME = "Workspaces";



    public AnnotationManager(AnnotationModel annotationModel) {


        this.annotationModel = annotationModel;

        modelMgr = ModelMgr.getModelMgr();



    }


    public Entity getInitialEntity() {
        return initialEntity;
    }

    public void setInitialEntity(Entity initialEntity) {
        TmWorkspace workspace;

        if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            // pass
        }

        else if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            // get associated brain sample and load it
            // not done yet

            // load the workspace itself
            try {
                workspace = new TmWorkspace(initialEntity);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            annotationModel.loadWorkspace(workspace);
        }

        this.initialEntity = initialEntity;





        // (eventually) update state to saved state (selection, visibility, etc)



        // (now) update (populate) neuron list; or: model does this, notifies?




    }
    

    // methods that are called by actions from the 2d view; should be not
    //  much more than what tool is active and where the click was;
    //  we are responsible for 

    public void addAnnotation(Vec3 xyz) {

        // get current workspace, brain, neurite, etc.
        // if they don't exist, error

        // if an annotation is selected, it's the parent
        // if not, check for existing root (currently can't handle > 1);
        //  if no existing root, new root annotation; else, fail

        // annModel.addRootAnnotation(currentWorkspace, currentNeuron, xyz);

        // update stuff?  or does this happen automatically?
        //  - 2d view
        //  - neurite tree 

        // select new annotation
        // update selection listeners

    }

    public void createNeuron() {
        // is there a workspace?  if not, fail; actually, doesn't the model know?
        //  yes; who should test?  who should pop up UI feedback to user?
        //  model shouldn't, but should annMgr? 

        if (annotationModel.getCurrentWorkspace() == null) {
            // dialog?

            return;
        }

        // ask user for name; you *can* rename on the sidebar, but that will 
        //  trigger a need to reload the slice viewer, so don't make the user go 
        //  through that
        String neuronName = (String)JOptionPane.showInputDialog(
            null,
            "Neuron name:",
            "Create neuron",
            JOptionPane.PLAIN_MESSAGE,
            null,                           // icon
            null,                           // choice list; absent = freeform
            "new neuron");

        // validate neuron name;  are there any rules for entity names?
        if ((neuronName == null) || (neuronName.length() == 0)) {
            neuronName = "new neuron";
        }


        // create it:
        if (!annotationModel.createNeuron(neuronName)) {
            JOptionPane.showMessageDialog(null, 
                "Could not create neuron!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }

    }

    public void createWorkspace() {

        // first we need to figure out the brain sample; the user may
        //  open the slice viewer from either a brain sample or a workspace; if
        //  it's the latter, grab its brain sample
        // (currently can't open Slice Viewer without an initial entity)

        // NOTE: ask the user if you're creating a new workspace when one is
        //  already active
        Entity sampleEntity;
        if (annotationModel.getCurrentWorkspace() != null) {
            // dialog
            int ans = JOptionPane.showConfirmDialog(null,
                "You already have an active workspace!  Close and create another?",
                "Workspace exists",
                JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
                Long sampleID = annotationModel.getCurrentWorkspace().getSampleID();
                try {
                    sampleEntity = modelMgr.getEntityById(sampleID);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                // users says no
                return;
            }
        } else {
            // no workspace, look at initial entity; it must be a brain sample!
            if (!initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                JOptionPane.showMessageDialog(null,
                        "You must load a brain sample before creating a workspace!",
                        "No brain sample!",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            sampleEntity= initialEntity;
        }

        // for now, we'll put the new workspace into a default, top-level folder
        //  named "Workspaces", which we will create if it does not exit; later,
        //  we'll create a dialog to let the user choose the location of the
        //  new workspace, and perhaps the brain sample, too
        Entity workspaceRootEntity;
        try {
            workspaceRootEntity = modelMgr.getCommonRootEntityByName(WORKSPACES_FOLDER_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (workspaceRootEntity == null) {
            try {
                workspaceRootEntity = modelMgr.createCommonRoot(WORKSPACES_FOLDER_NAME);
            } catch (Exception e) {
                e.printStackTrace();
                // fail: dialog
                JOptionPane.showMessageDialog(null,
                    "Could not create Workspaces top-level folder!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // get a name for the new workspace and validate (are there any rules for entity names?)
        String workspaceName = (String)JOptionPane.showInputDialog(
            null,
            "Workspace name:",
            "Create workspace",
            JOptionPane.PLAIN_MESSAGE,
            null,                           // icon
            null,                           // choice list; absent = freeform
            "new workspace");
        if ((workspaceName == null) || (workspaceName.length() == 0)) {
            workspaceName = "new workspace";
        }

        // create it
        if (!annotationModel.createWorkspace(workspaceRootEntity, sampleEntity, workspaceName)) {
            JOptionPane.showMessageDialog(null, 
                "Could not create workspace!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }

    }

}















