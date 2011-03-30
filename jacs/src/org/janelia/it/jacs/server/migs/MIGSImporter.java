/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.server.migs;

/**
 * Created by IntelliJ IDEA.
 * User: kli
 * Date: Apr 4, 2007
 * Time: 5:05:25 PM
 */
public class MIGSImporter {

//    public static void main(String[] args){
//        MIGSImporter migsImport = new MIGSImporter();
//        ObjectFactory jbObjectFactory = new ObjectFactory();
//
//
//        try{
//            String contextPackage = ObjectFactory.class.getPackage().toString().split(" ")[1];
//            System.out.println("contextPackage = "+contextPackage);
//            Unmarshaller unmarshaller = JAXBContext.newInstance(contextPackage).createUnmarshaller();
//
//            Migs jbMigs = (Migs)unmarshaller.unmarshal(new File("JCVI_LIB_GS-02-01-01-1P6KB.xml"));
//
//            Library lib = new Library();
//            migsImport.deserialize(jbMigs, lib);
//
//            // Test writing out
//            MIGSExporter migsExport = new MIGSExporter();
//            migsExport.serialize(new FileWriter(new File("output.xml")), lib);
//
//            // Test persisting.
//            // Make sure you are pointing at correct db before running this.
//            // migsImport.persistLibrary(lib);
//
//            //do something with lib, like persist it.
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    final String HABITAT_TYPE_KEY = "habitat_type";
//
//    void persistLibrary(Library lib) throws Exception{
//        HibernateSessionSource sessionSource = new HibernateSessionSource();
//        Session session;
//        session = sessionSource.getOrCreateSessionFactory().openSession();
//        Transaction transaction = session.beginTransaction();
//
//        // Fill in not nulls
//        lib.setNumberOfReads(0);
//        lib.setLibraryAcc("Some accession");
//        Set<Sample> sampleSet = lib.getSamples();
//        for(Sample smp: sampleSet){
//            smp.setFilterMin((double)0);
//            smp.setFilterMax((double)1);
//        }
//
//        session.save(lib);
//
//        transaction.commit();
//
//    }
//
//
//    Map<String, CollectionObservation> getKeyValue(Object obj) throws Exception{
//
//        Map<String,CollectionObservation> attribMap = new HashMap<String,CollectionObservation>();
//
//        Class c = Class.forName(obj.getClass().getName());
//        Method m[]=c.getDeclaredMethods();
//        for(int i=0; i<m.length; i++){
//            String methodName = m[i].getName();
//            if(methodName.matches("^get.+") && !methodName.equals("getPrimaryInterface")){
//                String attrib = methodName.replaceFirst("^get","");
//                attrib = attrib.replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase();
//                //System.out.println(attrib);
//
//                String value;
//                Object ret = m[i].invoke(obj);
//
//                if(ret!=null){
//                    boolean no_min = true;
//                    boolean no_max = true;
//                    BigDecimal min = ((MeasurementType)ret).getMin();
//                    BigDecimal max = ((MeasurementType)ret).getMax();
//
//                    String result;
//                    if(min!=null && max==null){
//                        result = ">"+min;
//                    }else if(min==null && max!=null){
//                        result = "<"+max;
//                    }else{
//                        result = min + "-" + max;
//                    }
//
//                    String unit = ((MeasurementType)ret).getUnit();
//
//                    CollectionObservation colObs = new CollectionObservation();
//                    colObs.setUnits(unit);
//                    colObs.setValue(result);
//                    attribMap.put(attrib, colObs);
//                }
//            }
//        }
//
//        return attribMap;
//    }
//
//    void deserialize(Migs jbMigs, Library library) throws Exception{
//
//        // Todo:  These items from GenomeCatalogue should be stored in our DB
//        String genomeReportTitle = jbMigs.getGenomeCatalogue().getGenomeReportTitle().getValue();
//        TaxonomicGroupRestriction taxonomicGroup = jbMigs.getGenomeCatalogue().getTaxonomicGroup().getValue();
//        String relevence = jbMigs.getGenomeCatalogue().getRelevance().getValue();
//        System.out.println("Title: "+genomeReportTitle);
//        System.out.println("Taxon Grp: "+taxonomicGroup);
//        System.out.println("Relevance: "+relevence);
//
//        // Biomaterial
//        List<Migs.Investigation.Study.Biomaterial> jbBiomaterialList =
//                (List<Migs.Investigation.Study.Biomaterial>)jbMigs.getInvestigation().getStudy().getBiomaterial();
//        populateLibWithBiomaterials(library, jbBiomaterialList);
//
//        // Assay::SampleProcessing
//        // We aren't doing anything with data_processing
//        Assay.SampleProcessing jbSampleProcessing= jbMigs.getInvestigation().getStudy().getAssay().getSampleProcessing();
//        populateLibWithSampleProcessing(library, jbSampleProcessing);
//
//        // We are ignoring:
//        //  phenotype
//        //  sources
//
//    }
//    void populateLibWithBiomaterials(Library library, List<Migs.Investigation.Study.Biomaterial>  jbBiomaterialList) throws Exception{
//
//        if(library.getSamples()==null){
//            library.setSamples(new HashSet<Sample>());
//        }
//        Set<Sample> sampleSet = library.getSamples();
//        Sample sample = new Sample();
//        sampleSet.add(sample);
//
//        if((List<BioMaterial>)sample.getBioMaterials()==null){
//            sample.setBioMaterials(new HashSet<BioMaterial>());
//        }
//        Set<BioMaterial> biomatSet =(Set<BioMaterial>)sample.getBioMaterials();
//
//
//        for(Migs.Investigation.Study.Biomaterial biomat: jbBiomaterialList){
//
//            BioMaterial bm = new BioMaterial();
//
//            //Nothing in here we need to worry about right now.
//            //biomat.getBiosample();
//
//            populateBioMaterialWithEnvironment(bm, biomat.getEnvironment());
//
//            if(biomat.getSediment()!=null){
//                // todo: we need a db flag if we get this kind of differential data
//            }
//            if(biomat.getSedimentPoreWater()!=null){
//                // todo: we need a db flag if we get this kind of differential data
//            }
//            if(biomat.getWaterBody()!=null){
//                populateBioMaterialWithWaterBody(bm, biomat.getWaterBody());
//            }
//
//            biomatSet.add(bm);
//        }
//
//    }
//
//    void populateLibWithSampleProcessing(Library library, Assay.SampleProcessing jbSampleProcessing) throws Exception{
//
//        // todo: may want to store this someday.
//        //jbSampleProcessing.getIsolationAndGrowthConditions();
//
//        jbSampleProcessing.getLibraryConstruction();
//        List<Assay.SampleProcessing.LibraryConstruction> libraryConstructionList = jbSampleProcessing.getLibraryConstruction();
//        Iterator libConstrIt = libraryConstructionList.iterator();
//        if(libConstrIt.hasNext()){
//            Assay.SampleProcessing.LibraryConstruction jbLibraryConstruction =
//                    (Assay.SampleProcessing.LibraryConstruction)libConstrIt.next();
//
//            Assay.SampleProcessing.LibraryConstruction.InsertSize jbInsertSize= jbLibraryConstruction.getInsertSize();
//            library.setMaxInsertSize(jbInsertSize.getMaxBps().intValue());
//            library.setMinInsertSize(jbInsertSize.getMinBps().intValue());
//
//            // probably never need to store this
//            // jbLibraryConstruction.getLibrarySize();
//
//            // Probably never need to store this
//            // jbLibraryConstruction.getNumberOfClonesSequenced();
//
//            // todo: may want to store this
//            // jbLibraryConstruction.getVector();
//
//        }
//        if(libConstrIt.hasNext()){
//            throw new Exception("There is more than one library_construction for this sample.");
//        }
//
//        // todo: may want to store this someday
//        //jbSampleProcessing.getNucleicAcidExtraction();
//
//        // todo: may want to store this someday
//        //jbSampleProcessing.getSamplingStrategy();
//
//
//        // todo:  may need to deal with more than one sequencing technology per assay?
//        List<Assay.SampleProcessing.SequencingMethod> sequencingMethodList =
//                (List<Assay.SampleProcessing.SequencingMethod>)jbSampleProcessing.getSequencingMethod();
//        Iterator seqMethodIt = sequencingMethodList.iterator();
//        if(seqMethodIt.hasNext()){
//            SequencingMethodRestriction sequencingMethod=((Assay.SampleProcessing.SequencingMethod)seqMethodIt.next()).getValue();
//            library.setSequencingTechnology(sequencingMethod.toString());
//        }
//        if(seqMethodIt.hasNext()){
//            throw new Exception("There is more than one sequencing methodology for this sample.");
//        }
//
//        // unlikely want to store this
//        //jbSampleProcessing.getVolumeOfSample();
//
//    }
//
//    void populateBioMaterialWithEnvironment(BioMaterial bm, Environment jbEnvironment) throws Exception{
//        // todo: Not doing anything with this but will need to if we do studies like "gut", etc...
//        //jbEnvironment.getOrganism();
//
//        // Extract date and time, and stick into one Calendar object, which we can extract the Date from.
//        Extensions jbExtension = jbEnvironment.getExtensions();
//        MIMS jbMIMS = jbExtension.getMIMS();
//
//        MIMS.Date jbDate = jbMIMS.getDate();
//        Calendar dateCal = jbDate.getValue().toGregorianCalendar();
//        Integer year = dateCal.get(Calendar.YEAR);
//        Integer month = dateCal.get(Calendar.MONTH);  // January is 0
//        Integer date = dateCal.get(Calendar.DATE);   // First day of month is 1
//
//        MIMS.Time jbTime = jbMIMS.getTime();
//        Calendar timeCal = jbTime.getValue().toGregorianCalendar();
//        Integer hour = timeCal.get(Calendar.HOUR_OF_DAY);   // 24 hours
//        Integer minute = timeCal.get(Calendar.MINUTE);
//        Integer second = timeCal.get(Calendar.SECOND);
//
//        Calendar combinedCal = new GregorianCalendar();
//        combinedCal.set(year, month, date, hour, minute, second);
//        bm.setCollectionStartTime(combinedCal.getTime());
//        bm.setCollectionStopTime(combinedCal.getTime());
//
//
//        // Put habitat type into the observation list
//        MIMS.HabitatType jbHabitat  = jbMIMS.getHabitatType();
//        Map obsMap = bm.getObservations();
//        if(obsMap==null){
//            obsMap=new HashMap<String,String>();
//            bm.setObservations(obsMap);
//        }
//        obsMap.put(HABITAT_TYPE_KEY, jbHabitat.getValue());
//
//        // Populate collectionSite with lat, long, etc.
//        CollectionSite collectionSite;
//        collectionSite=populateCollectionSite(jbMIMS.getGeographicalLocation());
//        bm.setCollectionSite(collectionSite);
//
//    }
//
//    CollectionSite populateCollectionSite(MIMS.GeographicalLocation jbGeographicalLocation) throws Exception{
//        CollectionSite collectionSite=null;
//
//        if(jbGeographicalLocation.getGeographicalPath()!=null){
//            collectionSite = new GeoPath();
//            //(GeoPath)collectionSite.set
//            MIMS.GeographicalLocation.GeographicalPath jbGeographicalPath = jbGeographicalLocation.getGeographicalPath();
//            List<GeographicalPoint> jbGeographicaPointList = (List<GeographicalPoint>) jbGeographicalPath.getGeographicalPoint();
//
//            for(GeographicalPoint jbGeoPt: jbGeographicaPointList){
//                GeoPoint gp = populateGeoPointWithJAXBGeographicalPoint(jbGeoPt);
//                ((GeoPath)collectionSite).addPoint(gp);
//            }
//
//        }else if(jbGeographicalLocation.getGeographicalPoint()!=null){
//            GeographicalPoint jbGeoPt = jbGeographicalLocation.getGeographicalPoint();
//            collectionSite = populateGeoPointWithJAXBGeographicalPoint(jbGeoPt);
//        }else if(jbGeographicalLocation.getGeographicalRegion()!=null){
//            // todo: not implemented, may need GeoRegion
//            throw new Exception("GeoRegion not implemented.  Can not populate CollectionSite.");
//        }
//
//        return collectionSite;
//    }
//
//    GeoPoint populateGeoPointWithJAXBGeographicalPoint(GeographicalPoint jbGeoPt) throws Exception{
//
//        GeoPoint collectionSite = new GeoPoint();
//        if(jbGeoPt.getAltitude()!=null)
//            ((GeoPoint)collectionSite).setAltitude(jbGeoPt.getAltitude().toString());
//        if(jbGeoPt.getDepth()!=null)
//            ((GeoPoint)collectionSite).setDepth(jbGeoPt.getDepth().toString());
//
//        if(jbGeoPt.getLatitude().getDecimal()==null){
//            String latStr =
//                    jbGeoPt.getLatitude().getDegrees().toString() + "d "+
//                            jbGeoPt.getLatitude().getMinutes().toString() + "'"+
//                            jbGeoPt.getLatitude().getSeconds().toString() + "\""+
//                            jbGeoPt.getLatitude().getDirectionLatitude().toString().toLowerCase();
//            ((GeoPoint)collectionSite).setLatitude(latStr);
//        }else{
//            ((GeoPoint)collectionSite).setLatitude(jbGeoPt.getLatitude().getDecimal().getValue().doubleValue());
//        }
//
//        if(jbGeoPt.getLongitude().getDecimal()==null){
//            String lonStr =
//                    jbGeoPt.getLongitude().getDegrees().toString() + "d "+
//                            jbGeoPt.getLongitude().getMinutes().toString() + "'"+
//                            jbGeoPt.getLongitude().getSeconds().toString() + "\""+
//                            jbGeoPt.getLongitude().getDirectionLongitude().toString().toLowerCase();
//            ((GeoPoint)collectionSite).setLongitude(lonStr);
//        }else{
//            ((GeoPoint)collectionSite).setLongitude(jbGeoPt.getLongitude().getDecimal().getValue().doubleValue());
//        }
//
//        return collectionSite;
//    }
//
//    void populateBioMaterialWithWaterBody(BioMaterial bm, Migs.Investigation.Study.Biomaterial.WaterBody jbWaterBody) throws Exception{
//        Map<String, CollectionObservation> attributeMap = getKeyValue(jbWaterBody);
//        bm.setObservations(attributeMap);
//    }
//
//    void populateCollectionObservationList(List<CollectionObservation> colObsList, Map<String, String> attributeMap){
//
//        for(String key: attributeMap.keySet()){
//            CollectionObservation colObs = new CollectionObservation();
//
//            String val = attributeMap.get(key);
//            String comp[] = val.split(" ");
//
//            colObs.setValue(comp[0]);
//            colObs.setUnits(comp[1]);
//
//            colObsList.add(colObs);
//        }
//    }
//
//
}
