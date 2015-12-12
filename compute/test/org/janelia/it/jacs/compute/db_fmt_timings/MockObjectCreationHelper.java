/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.db_fmt_timings;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author fosterl
 */
public class MockObjectCreationHelper {
    public static final int NUM_STA = 5;
    public static final int NUM_GEO_ANNO = 1000;
    public static final int NUM_APS = 30;
    public static final int NUM_NEURONS = 5000;

    private final static String LOREM_IPSUM_EXCERPT
            = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a diam lectus. Sed sit amet ipsum mauris. Maecenas congue ligula ac quam viverra nec consectetur ante hendrerit. Donec et mollis dolor. Praesent et diam eget libero egestas mattis sit amet vitae augue. Nam tincidunt congue enim, ut porta lorem lacinia consectetur. Donec ut libero sed arcu vehicula ultricies a non tortor. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean ut gravida lorem. Ut turpis felis, pulvinar a semper sed, adipiscing id dolor. Pellentesque auctor nisi id magna consequat sagittis. Curabitur dapibus enim sit amet elit pharetra tincidunt feugiat nisl imperdiet. Ut convallis libero in urna ultrices accumsan. Donec sed odio eros. Donec viverra mi quis quam pulvinar at malesuada arcu rhoncus. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. In rutrum accumsan ultricies. Mauris vitae nisi at sem facilisis semper ac in est.\n"
            + "\n"
            + "\n"
            + "Vivamus fermentum semper porta. Nunc diam velit, adipiscing ut tristique vitae, sagittis vel odio. Maecenas convallis ullamcorper ultricies. Curabitur ornare, ligula semper consectetur sagittis, nisi diam iaculis velit, id fringilla sem nunc vel mi. Nam dictum, odio nec pretium volutpat, arcu ante placerat erat, non tristique elit urna et turpis. Quisque mi metus, ornare sit amet fermentum et, tincidunt et orci. Fusce eget orci a orci congue vestibulum. Ut dolor diam, elementum et vestibulum eu, porttitor vel elit. Curabitur venenatis pulvinar tellus gravida ornare. Sed et erat faucibus nunc euismod ultricies ut id justo. Nullam cursus suscipit nisi, et ultrices justo sodales nec. Fusce venenatis facilisis lectus ac semper. Aliquam at massa ipsum. Quisque bibendum purus convallis nulla ultrices ultricies. Nullam aliquam, mi eu aliquam tincidunt, purus velit laoreet tortor, viverra pretium nisi quam vitae mi. Fusce vel volutpat elit. Nam sagittis nisi dui.\n"
            + "\n"
            + "\n"
            + "Suspendisse lectus leo, consectetur in tempor sit amet, placerat quis neque. Etiam luctus porttitor lorem, sed suscipit est rutrum non. Curabitur lobortis nisl a enim congue semper. Aenean commodo ultrices imperdiet. Vestibulum ut justo vel sapien venenatis tincidunt. Phasellus eget dolor sit amet ipsum dapibus condimentum vitae quis lectus. Aliquam ut massa in turpis dapibus convallis. Praesent elit lacus, vestibulum at malesuada et, ornare et est. Ut augue nunc, sodales ut euismod non, adipiscing vitae orci. Mauris ut placerat justo. Mauris in ultricies enim. Quisque nec est eleifend nulla ultrices egestas quis ut quam. Donec sollicitudin lectus a mauris pulvinar id aliquam urna cursus. Cras quis ligula sem, vel elementum mi. Phasellus non ullamcorper urna.\n"
            + "\n"
            + "\n"
            + "Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. In euismod ultrices facilisis. Vestibulum porta sapien adipiscing augue congue id pretium lectus molestie. Proin quis dictum nisl. Morbi id quam sapien, sed vestibulum sem. Duis elementum rutrum mauris sed convallis. Proin vestibulum magna mi. Aenean tristique hendrerit magna, ac facilisis nulla hendrerit ut. Sed non tortor sodales quam auctor elementum. Donec hendrerit nunc eget elit pharetra pulvinar. Suspendisse id tempus tortor. Aenean luctus, elit commodo laoreet commodo, justo nisi consequat massa, sed vulputate quam urna quis eros. Donec vel. ";

    // Making 5000 objects.
    private int numGeoAnno = 0;
    private int numAps = 0;
    private int numSta = 0;

    public List<MockNeuronJsonData> createObjects() {
		return createObjects(NUM_NEURONS);
    }
	
	public List<MockNeuronJsonData> createSmallSetOfObjects() {
		return createObjects(10);
	}
    
	public List<MockNeuronJsonData> createObjects(int count) {
        List<MockNeuronJsonData> neurons = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            neurons.add(createNeuron());
        }
        System.out.println("File contains " + numSta + " Struc-tex-anno, " + numGeoAnno + " geoAnnotations, and " + numAps + " Anchored Paths.");
        return neurons;
	}
    
    protected MockNeuronJsonData createNeuron() {
        
        MockNeuronJsonData data = new MockNeuronJsonData();
        
        List<MockNeuronJsonData.AnchoredPath> anchoredPaths = new ArrayList<>();
        List<MockNeuronJsonData.GeoAnnotation> geoAnnotations = new ArrayList<>();
        List<MockNeuronJsonData.StructuredTextAnnotation> structuredTextAnnotations = new ArrayList<>();
        
        for (int j = 0; j < NUM_APS; j++) {
            MockNeuronJsonData.AnchoredPath anchoredPath = new MockNeuronJsonData.AnchoredPath();
            MockNeuronJsonData.AnchoredPath.EndPoints ep = new MockNeuronJsonData.AnchoredPath.EndPoints();
            ep.setX1(randomX());
            ep.setY1(randomY());
            ep.setZ1(randomZ());
            
            ep.setX2(randomX());
            ep.setY2(randomY());
            ep.setZ2(randomZ());
            anchoredPath.setEndPoints(ep);
            
            int ranLen = (int) (Math.random() * 50.0) + 1;
            double[][] points = new double[ranLen][3];
            for (int k = 0; k < ranLen; k++) {
                points[k][0] = randomX();
                points[k][1] = randomY();
                points[k][2] = randomZ();
            }
            anchoredPath.setPoints(points);
            anchoredPaths.add(anchoredPath);
        }
        numAps += NUM_APS;
        
        for (int j = 0; j < NUM_GEO_ANNO; j++) {
            MockNeuronJsonData.GeoAnnotation geoAnnotation = new MockNeuronJsonData.GeoAnnotation();
            geoAnnotation.setId(randomId());
            geoAnnotation.setParentId(randomId());
            geoAnnotation.setRadius(1.0);
            geoAnnotation.setX(randomX());
            geoAnnotation.setY(randomY());
            geoAnnotation.setZ(randomZ());
            geoAnnotations.add(geoAnnotation);
        }
        numGeoAnno += NUM_GEO_ANNO;
        
        for (int j = 0; j < NUM_STA; j++) {
            MockNeuronJsonData.StructuredTextAnnotation sta = new MockNeuronJsonData.StructuredTextAnnotation();
            sta.setId(randomId());
            sta.setFmtVersion(1);
            sta.setParentId(randomId());
            sta.setParentType(1);
            sta.setData(randomLoremIpsum());
            structuredTextAnnotations.add(sta);
        }
        numSta += NUM_STA;
        
        data.setAnchoredPaths(anchoredPaths);
        data.setGeoAnnotations(geoAnnotations);
        data.setStructuredTextAnnotations(structuredTextAnnotations);
        
        data.setId(randomId());
        data.setCreateDate(new Date());
        data.setName("Neuron_" + data.getId());
        return data;
    }
    
    protected long randomId() {
        // This is an ID of size expected, that was borrowed from a recent
        // JBOSS log at time of writing.
        return (long) (Math.random() * 2214493058706178216L);
    }

    protected double randomZ() {
        return Math.random() * 11000.0;
    }

    protected double randomY() {
        return Math.random() * 42000.0;
    }

    protected double randomX() {
        return Math.random() * 75000.0;
    }

    protected String randomLoremIpsum() {
        int offsetA = (int) Math.random() * LOREM_IPSUM_EXCERPT.length();
        int offsetB = (int) Math.random() * LOREM_IPSUM_EXCERPT.length();
        int offsetStart = offsetA < offsetB ? offsetA : offsetB;
        int offsetEnd = offsetA > offsetB ? offsetA : offsetB;
        return LOREM_IPSUM_EXCERPT.substring(offsetStart, offsetEnd);
    }
}
