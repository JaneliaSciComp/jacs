package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableDataSourceI;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.Neuron;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/** Implements the data source, to avoid having to mock up an entire context just to test. */
public class ABContextDataSource implements RenderableDataSourceI {

    // This data was manually extracted from some Yoshi/standard-space-size data.  It is named as:
    //   prefix_1822248087368761442_9.mask
    // where '9' above is the label number, the usual mask/chan extensions apply, and the number shown
    // should match the location for the previously-known files fetched from the older pipeline.
    private static final String TEMP_ROOT_PATH = "/Volumes/jacsData/maskChannelTestYoshi/prefix_";
    private static final String MASK_EXTENSION = ".mask";
    private AlignmentBoardContext context;
    private String[] filenames;

    private Logger logger = LoggerFactory.getLogger( ABContextDataSource.class );
    public ABContextDataSource(String[] filenames) {
        this.filenames = filenames;
    }

    public ABContextDataSource( AlignmentBoardContext context ) {
        this.context = context;
//        filenames = new String[] {
//                rootPath + "/prefix_1.mask",
//                rootPath + "/prefix_1.chan",
//                rootPath + "/prefix_2.mask",
//                rootPath + "/prefix_2.chan",
//                rootPath + "/prefix_3.mask",
//                rootPath + "/prefix_3.chan",
//                rootPath + "/prefix_4.mask",
//                rootPath + "/prefix_4.chan",
//                rootPath + "/prefix_5.mask",
//                rootPath + "/prefix_5.chan",
//                rootPath + "/prefix_6.mask",
//                rootPath + "/prefix_6.chan",
//                rootPath + "/prefix_7.mask",
//                rootPath + "/prefix_7.chan",
//                rootPath + "/prefix_8.mask",
//                rootPath + "/prefix_8.chan",
//                rootPath + "/prefix_9.mask",
//                rootPath + "/prefix_9.chan",
//                rootPath + "/prefix_10.mask",
//                rootPath + "/prefix_10.chan",
//                rootPath + "/prefix_11.mask",
//                rootPath + "/prefix_11.chan",
//                rootPath + "/prefix_12.mask",
//                rootPath + "/prefix_12.chan",
//                rootPath + "/prefix_13.mask",
//                rootPath + "/prefix_13.chan",
//                rootPath + "/prefix_14.mask",
//                rootPath + "/prefix_14.chan",
//                rootPath + "/prefix_15.mask",
//                rootPath + "/prefix_15.chan",
//                rootPath + "/prefix_16.mask",
//                rootPath + "/prefix_16.chan",
//                rootPath + "/prefix_17.mask",
//                rootPath + "/prefix_17.chan",
//                rootPath + "/prefix_18.mask",
//                rootPath + "/prefix_18.chan",
//                rootPath + "/prefix_19.mask",
//                rootPath + "/prefix_19.chan",
//                rootPath + "/prefix_20.mask",
//                rootPath + "/prefix_20.chan",
//        };
    }

    @Override
    public String getName() {
        return ABContextDataSource.class.getSimpleName() + " Data Source";
    }

    @Override
    public Collection<MaskChanRenderableData> getRenderableDatas() {
        logger.info( "Getting renderable datas." );
        Collection<MaskChanRenderableData> rtnVal = new ArrayList<MaskChanRenderableData>();

        int nextTranslatedNum = 0;

        // Establish the fragment renderables.
        for ( AlignedItem alignedItem : context.getAlignedItems() ) {

            EntityWrapper itemEntity = alignedItem.getItemWrapper();
            if ( itemEntity instanceof Sample) {
                Sample sample = (Sample)itemEntity;
                RenderableBean sampleDataBean = new RenderableBean();
                sampleDataBean.setLabelFileNum(0);
                sampleDataBean.setTranslatedNum(nextTranslatedNum++);
                sampleDataBean.setRgb(
                        new byte[]{
                                (byte) 0f, (byte) 0f, (byte) 0f, RenderMappingI.NON_RENDERING
                        }
                );
                sampleDataBean.setRenderableEntity(sample.getInternalEntity());
                MaskChanRenderableData sampleRenderable = new MaskChanRenderableData();
                sampleRenderable.setBean(sampleDataBean);
                sampleRenderable.setCompartment(false);

                rtnVal.add( sampleRenderable );

                long sampleId = sample.getId();

                Collection<AlignedItem> childItems = alignedItem.getAlignedItems();
                int translatedNum = 1;
                if ( childItems != null ) {
                    for ( AlignedItem item: childItems ) {
                        if ( item.getItemWrapper() instanceof Neuron) {
                            RenderableBean neuronBean = createRenderableBean( translatedNum, item );
                            MaskChanRenderableData nfRenderable = new MaskChanRenderableData();
                            nfRenderable.setBean( neuronBean );
                            nfRenderable.setCompartment( false );
                            rtnVal.add( nfRenderable );

                            Neuron neuronItem = (Neuron)item.getItemWrapper();
                            nfRenderable.setMaskPath( getMaskPath( neuronItem, sampleId ) );
                            nfRenderable.setChannelPath( getChannelPath( neuronItem, sampleId ) );

                            rtnVal.add( nfRenderable );
                            translatedNum ++;
                        }
                    }
                }


            }
        }


        return rtnVal;
    }

    /*
    @deprecated
     */
    public Collection<MaskChanRenderableData> getRenderableDatas_old() {

        logger.info( "Getting renderable datas." );
        Collection<MaskChanRenderableData> rtnVal = new ArrayList<MaskChanRenderableData>();

        // Establish the renderable for the "signal".
        int nextTranslatedNum = 0;
        RenderableBean signalBean = new RenderableBean();
        signalBean.setLabelFileNum(0);
        signalBean.setTranslatedNum(nextTranslatedNum ++);
        signalBean.setRgb(
                new byte[]{
                        (byte) 0f, (byte) 0f, (byte) 0f, RenderMappingI.NON_RENDERING
                }
        );

        MaskChanRenderableData signalData = new MaskChanRenderableData();
        signalData.setBean(signalBean);
        signalData.setCompartment( false );

        // Must divie-up the inputs between compartments and Fragments.

        // Establish the compartment renderables.
        int nextArgument = 0;
        for ( int i = 0; i < filenames.length; i ++ ) {
            if ( ! filenames[ i ].toLowerCase().contains( "/compartment_" ) ) {
                break;
            }
            String fn = filenames[ i ];
            if ( fn.endsWith( MASK_EXTENSION ) ) {
                RenderableBean renderableBean = new RenderableBean();

                // File name should tell its own index number.
                int underPos = fn.lastIndexOf( '_' );
                String beanNumStr = fn.substring( underPos + 1, fn.indexOf( MASK_EXTENSION ) );
                int beanNum = Integer.parseInt( beanNumStr );
                renderableBean.setLabelFileNum(beanNum);
                renderableBean.setTranslatedNum(nextTranslatedNum);
                renderableBean.setRgb(
                        new byte[]{0, 0, 0, RenderMappingI.COMPARTMENT_RENDERING}
                );

                MaskChanRenderableData data = new MaskChanRenderableData();
                data.setBean( renderableBean );
                data.setCompartment( true );
                data.setMaskPath( fn );
                rtnVal.add(data);

                nextTranslatedNum ++;
            }
            nextArgument ++;
        }

        // Establish the fragment renderables.
        //  TEMP: just using the aligned items for coloring, not data.
        Collection<AlignedItem> neuronFragments = new ArrayList<AlignedItem>();
        for ( AlignedItem item: context.getAlignedItems() ) {
            for ( AlignedItem childItem: item.getAlignedItems() ) {
                if ( childItem.isVisible() )
                    neuronFragments.add( childItem );
            }
        }

        Iterator<AlignedItem> alignedItemIterator = neuronFragments.iterator();

        for ( int i = nextArgument; i < filenames.length; i += 2 ) {
            RenderableBean renderableBean = new RenderableBean();

            String fn = filenames[ i ];
            if ( fn.endsWith( MASK_EXTENSION ) ) {
                // File name should tell its own index number.
                int underPos = fn.lastIndexOf( '_' );
                String beanNumStr = fn.substring( underPos + 1, fn.indexOf( MASK_EXTENSION ) );
                int beanNum = Integer.parseInt( beanNumStr );

                renderableBean.setTranslatedNum( nextTranslatedNum );
                renderableBean.setLabelFileNum( beanNum );
                byte[] renderMethod = {
                        (byte) 255, (byte) 255, (byte) 255,
                        RenderMappingI.FRAGMENT_RENDERING
                };
                if ( alignedItemIterator.hasNext()) {
                    Color renderColor = null;
                    while ( renderColor == null  &&  alignedItemIterator.hasNext() ) {
                        AlignedItem item = alignedItemIterator.next();
                        renderColor = item.getColor();
                    }

                    if ( renderColor != null ) {
                        renderMethod[ 0 ] = (byte)renderColor.getRed();
                        renderMethod[ 1 ] = (byte)renderColor.getGreen();
                        renderMethod[ 2 ] = (byte)renderColor.getBlue();

                    }
                }

                renderableBean.setRgb(
                        renderMethod
                );

                MaskChanRenderableData data = new MaskChanRenderableData();
                data.setBean( renderableBean );
                data.setCompartment( false );
                data.setMaskPath( filenames[ i ] );
                data.setChannelPath( filenames[ i + 1 ] );
                rtnVal.add(data);

                nextTranslatedNum ++;
            }
            else {
                throw new IllegalArgumentException(
                        "Wrong file order.  Expecting .mask at position " + i + " in command-line arguments."
                );
            }
        }

        return rtnVal;
    }

    //------------------------------------------------------------HELPERS

    /**
     * This will implement the fetch of mask file name.
     *
     * @param neuron has a mask file associated.
     * @return that mask file.
     */
    private String getMaskPath( Neuron neuron, long id ) {
        String path = TEMP_ROOT_PATH + id + "_" + ( 1 + neuron.getMaskIndex() ) + ".mask";
        if ( ! new File( path ).canRead() ) {
            path = null;
        }
        return path;
    }

    /**
     * This will implement the fetch of channel file name.
     *
     * @param neuron has a channel file associated.
     * @return that channel file.
     */
    private String getChannelPath( Neuron neuron, long id ) {
        String path = TEMP_ROOT_PATH + id + "_" + ( 1 + neuron.getMaskIndex() ) + ".chan";
        if ( ! new File( path ).canRead() ) {
            path = null;
        }
        return path;
    }

    private RenderableBean createRenderableBean( int translatedNum, AlignedItem item ) {
        Neuron neuron = (Neuron)item.getItemWrapper();
        logger.debug(
                "Creating Renderable Bean for: " + neuron.getName() + " original index=" + neuron.getMaskIndex() +
                        " new index=" + translatedNum
        );

        RenderableBean neuronBean = new RenderableBean();
        neuronBean.setLabelFileNum( neuron.getMaskIndex() + 1 ); // From 0-based to 1-based.
        neuronBean.setTranslatedNum(translatedNum);
        neuronBean.setRenderableEntity(neuron.getInternalEntity());

        // See to the appearance.
        Color neuronColor = item.getColor();
        if ( neuronColor == null ) {
            // If visible, leave RGB as null, and allow downstream automated-color to take place.
            // Otherwise, if not visible, ensure that the bean has a non-render setting.
            if ( ! item.isVisible() ) {
                byte[] rgb = new byte[ 4 ];
                rgb[ 0 ] = 0;
                rgb[ 1 ] = 0;
                rgb[ 2 ] = 0;
                rgb[ 3 ] = RenderMappingI.NON_RENDERING;
                neuronBean.setRgb( rgb );
            }
        }
        else {
            logger.info( "Neuron color is {} for {}.", neuronColor, item.getItemWrapper().getName() );
            // A Neuron Color was set, but the neuron could still be "turned off" for render.
            byte[] rgb = new byte[ 4 ];
            rgb[ 0 ] = (byte)neuronColor.getRed();
            rgb[ 1 ] = (byte)neuronColor.getGreen();
            rgb[ 2 ] = (byte)neuronColor.getBlue();
            rgb[ 3 ] = item.isVisible()    ?    RenderMappingI.FRAGMENT_RENDERING : RenderMappingI.NON_RENDERING;
            neuronBean.setRgb( rgb );
        }

        return neuronBean;
    }


}
