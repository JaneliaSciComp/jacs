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
 * Time: 5:04:37 PM
 */
public class MIGSExporter {

//    private ObjectFactory jbObjectFactory = new ObjectFactory();
//    static String NOT_SPECIFIED_STR = new String("Not specified.");
//    static String UNKNOWN_STR = "unknown";
//    JAXBContext jaxbContext=null;
//    DatatypeFactory dtFactory=DatatypeFactory.newInstance();
//
//    public MIGSExporter() throws Exception {
//        jaxbContext=JAXBContext.newInstance("org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb");
//    }
//
//    void serialize(Writer writer, Library library) throws Exception{
//
//        // Create the main container
//        Migs migs = populateMigs(library);
//
//        // Write to file
//        //Marshaller jbMarshaller = jbObjectFactory.createMarshaller();
//        Marshaller jbMarshaller = jaxbContext.createMarshaller();
//        jbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//        jbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
//        jbMarshaller.marshal(migs, writer);
//
//    }
//    public static void main(String[] args){
////        MIGSExporter migsE = new MIGSExporter();
////        Library testSamp = new Library();
//
//        HibernateSessionSource sessionSource = new HibernateSessionSource();
//        Session session = sessionSource.getOrCreateSession();
//        session.beginTransaction();
//
//        List<Library> libraries = (List<Library>)session.createCriteria(Library.class).list();
//
//
//
//        try{
//            for(Library lib: libraries){
//                System.out.println(lib.getLibraryAcc());
//                MIGSExporter migsExp = new MIGSExporter();
//
//
//                //Writer w=new OutputStreamWriter(System.out);
//                FileWriter w = new FileWriter(new File(lib.getLibraryAcc()+".xml"));
//
//                migsExp.serialize(w, lib);
//            }
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//
////        try{
////        String minmax[] = extractMinMax("10-20");
////        System.out.println("min: "+minmax[0]+" max:"+ minmax[1]);
////
////        minmax = extractMinMax("-10-20");
////        System.out.println("min: "+minmax[0]+" max:"+ minmax[1]);
////
////        minmax= extractMinMax("-10--9");
////        System.out.println("min: "+minmax[0]+" max:"+ minmax[1]);
////
////        minmax= extractMinMax("10+/-4");
////        System.out.println("min: "+minmax[0]+" max:"+ minmax[1]);
////
////        minmax= extractMinMax("-10+/-4");
////        System.out.println("min: "+minmax[0]+" max:"+ minmax[1]);
////        }catch(Exception e){
////            e.printStackTrace();
////        }
//
//        session.close();
//
////        testSamp.setBioMaterials();
////        testSamp.setComments();
////        testSamp.setFilterMax();
//    }
//
//    Migs populateMigs(Library library) throws Exception{
//        Migs jbMigs = jbObjectFactory.createMigs();
//
//        Migs.Investigation jbInvestigation;
//        jbInvestigation = populateInvestigation(library);
//        jbMigs.setInvestigation(jbInvestigation);
//
//        Migs.GenomeCatalogue jbGenomeCatalogue;
//        jbGenomeCatalogue = populateGenomeCatalogue(library);
//        jbMigs.setGenomeCatalogue(jbGenomeCatalogue);
//
//        return jbMigs;
//    }
//
//    Migs.GenomeCatalogue populateGenomeCatalogue(Library library) throws Exception{
//        Migs.GenomeCatalogue jbGenomeCatalogue = jbObjectFactory.createMigsGenomeCatalogue();
//
//        Migs.GenomeCatalogue.GenomeReportTitle grt = jbObjectFactory.createMigsGenomeCatalogueGenomeReportTitle();
//        Migs.GenomeCatalogue.TaxonomicGroup tg = jbObjectFactory.createMigsGenomeCatalogueTaxonomicGroup();
//        Migs.GenomeCatalogue.Relevance r = jbObjectFactory.createMigsGenomeCatalogueRelevance();
//
//        // Set values from library
//        tg.setValue(TaxonomicGroupRestriction.METAGENOME);
//
//        // Extract set of locations and collection times
//        Set<Sample> sampleSet = library.getSamples();
//        SortedSet<String> locationSet = new TreeSet<String>();
//        SortedSet<Date> startDateSet = new TreeSet<Date>();
//        SortedSet<Date> stopDateSet = new TreeSet<Date>();
//        for(Iterator smpIt=sampleSet.iterator(); smpIt.hasNext(); ){
//            Sample curSmp = (Sample)smpIt.next();
//            Set<BioMaterial> biomatSet =  (Set<BioMaterial>)curSmp.getBioMaterials();
//            for(Iterator biomatIt = biomatSet.iterator(); biomatIt.hasNext(); ){
//                BioMaterial curBiomat = (BioMaterial)biomatIt.next();
//                CollectionSite curColSite = curBiomat.getCollectionSite();
//                locationSet.add(curColSite.getLocation());
//                startDateSet.add(curBiomat.getCollectionStartTime());
//                stopDateSet.add(curBiomat.getCollectionStopTime());
//            }
//        }
//
//        // TODO:  we need to add a "title" to the library class.
//        // if library.title.isNull then
//        // Generate title based on locations and sample dates
//        String title;
//        if(locationSet.size()>1){
//            title = "Various Locations Sampled";
//        }else{
//            title = locationSet.first();
//        }
//        title += " ("+ startDateSet.first() + " - " + stopDateSet.last() +")";
//        grt.setValue(title);
//
//
//        // TODO:  we need to a "relevance"/"summary" to the library class
//        // if library.summary.isNull then
//        // Generate summary based on locations and sample dates
//        String relevance="";
//        for(Iterator locIt = locationSet.iterator(); locIt.hasNext(); ){
//            relevance += locIt.next();
//            if(locIt.hasNext()){
//                relevance += ", ";
//            }
//        }
//        r.setValue(relevance);
//
//        // Set elements
//        jbGenomeCatalogue.setGenomeReportTitle(grt);
//        jbGenomeCatalogue.setTaxonomicGroup(tg);
//        jbGenomeCatalogue.setRelevance(r);
//
//        return jbGenomeCatalogue;
//    }
//
//    Migs.Investigation populateInvestigation(Library library) throws Exception{
//
//        Migs.Investigation jbInvestigation = jbObjectFactory.createMigsInvestigation();
//
//        Migs.Investigation.Study jbStudy = populateStudy(library);
//
//        jbInvestigation.setStudy(jbStudy);
//
//        return jbInvestigation;
//    }
//
//    Migs.Investigation.Study populateStudy(Library library) throws Exception{
//        Migs.Investigation.Study jbStudy = jbObjectFactory.createMigsInvestigationStudy();
//
//        Migs.Investigation.Study.Phenotype jbPhenotype = populatePhenotype(library);
//        Migs.Investigation.Study.Sources jbSources = populateSources(library);
//        Assay jbAssay = populateAssay(library);
//
//        jbStudy.setAssay(jbAssay);
//        jbStudy.setPhenotype(jbPhenotype);
//        jbStudy.setSources(jbSources);
//
//        // Get the list so we can add elements to it.
//        List<Migs.Investigation.Study.Biomaterial> bioMatList = jbStudy.getBiomaterial();
//        populateBiomaterialList(bioMatList, library);
//
//        return jbStudy;
//
//    }
//
//    void populateBiomaterialList(List<Migs.Investigation.Study.Biomaterial> biomatList, Library library) throws Exception{
//
//        // For each set of samples, we will need to create a Biomaterial object and insert it into the biomatList
//        Set<Sample> sampleSet = library.getSamples();
//
//        if(sampleSet.size()>1){
//            throw new Exception("Pooling of samples not implemented.  Only pooling of biomaterials allowed.");
//        }
//
//        for(Iterator sampleSetItr = sampleSet.iterator(); sampleSetItr.hasNext();){
//            Sample smp = (Sample)sampleSetItr.next();
//
//            Set<BioMaterial> biomatSet = smp.getBioMaterials();
//            for(Iterator biomatSetItr = biomatSet.iterator(); biomatSetItr.hasNext();){
//                BioMaterial bm = (BioMaterial)biomatSetItr.next();
//                Migs.Investigation.Study.Biomaterial biomat=populateBiomaterial(bm);
//                biomatList.add(biomat);
//            }
//        }
//
//    }
//
//    Migs.Investigation.Study.Biomaterial populateBiomaterial(BioMaterial bm) throws Exception{
//        Migs.Investigation.Study.Biomaterial jbBiomaterial=
//                jbObjectFactory.createMigsInvestigationStudyBiomaterial();
//
//        // Biosample
//        // NULL
//
//        // environment
//        Environment jbEnvironment = jbObjectFactory.createEnvironment();
//        Extensions jbExtensions = jbObjectFactory.createExtensions();
//        MIMS jbMIMS = populateMIMS(bm); //= jbObjectFactory.createMIMS();
//        jbExtensions.setMIMS(jbMIMS);
//        jbEnvironment.setExtensions(jbExtensions);
//
//        Environment.Organism jbOrganism = populateOrganism(bm);
//        jbEnvironment.setOrganism(jbOrganism);
//
//        // one of sediment/sediment_pore_water/water_body
//        // todo: See Collection Observation for type
//        Migs.Investigation.Study.Biomaterial.WaterBody jbWaterBody = populateWaterBody(bm);
//
//        List<Biosample> jbBiosampleList = (List<Biosample>)jbBiomaterial.getBiosample();
//        Biosample jbBiosample = jbObjectFactory.createBiosample();
//        jbBiosampleList.add(jbBiosample);
//
//        jbBiomaterial.setWaterBody(jbWaterBody);
//        jbBiomaterial.setEnvironment(jbEnvironment);
//
//        return jbBiomaterial;
//    }
//
//    MIMS populateMIMS(BioMaterial bm) throws Exception{
//
//        MIMS jbMIMS = jbObjectFactory.createMIMS();
//
//        MIMS.GeographicalLocation jbGeographicalLocation = jbObjectFactory.createMIMSGeographicalLocation();
//        populateGeographicalLocation(jbGeographicalLocation, bm.getCollectionSite());
//        jbMIMS.setGeographicalLocation(jbGeographicalLocation);
//
//        Date startTime = bm.getCollectionStartTime();
//        XMLGregorianCalendar gregCal = createXMLGregorianCalendarFromDate(startTime);
//
//        MIMS.Date jbDate = jbObjectFactory.createMIMSDate();
//        jbDate.setValue(gregCal);
//        jbMIMS.setDate(jbDate);
//
//        MIMS.Time jbTime = jbObjectFactory.createMIMSTime();
//        jbTime.setValue(gregCal);
//        jbMIMS.setTime(jbTime);
//
//        // todo:
//        MIMS.HabitatType jbHabitate= jbObjectFactory.createMIMSHabitatType();
//        jbHabitate.setValue(HabitatTypeRestriction.UNKNOWN);
//        jbMIMS.setHabitatType(jbHabitate);
//
//        return jbMIMS;
//    }
//
//    XMLGregorianCalendar createXMLGregorianCalendarFromDate(Date date) {
//        GregorianCalendar g=new GregorianCalendar();
//        g.setTime(date);
//        XMLGregorianCalendar gregCal=dtFactory.newXMLGregorianCalendar(g);
//        return gregCal;
//    }
//
//    void populateGeographicalLocation(MIMS.GeographicalLocation jbGeoLoc, CollectionSite colSite) throws Exception{
//
//        if(colSite instanceof GeoPoint){
//
//            GeographicalPoint jbGeographicalPoint = jbObjectFactory.createGeographicalPoint();
//            populateGeographicalPoint(jbGeographicalPoint, (GeoPoint)colSite);
//            jbGeoLoc.setGeographicalPoint(jbGeographicalPoint);
//
//        }else if(colSite instanceof GeoPath){
//
//            MIMS.GeographicalLocation.GeographicalPath jbGeographicalPath = jbObjectFactory.createMIMSGeographicalLocationGeographicalPath();
//            List<GeoPoint> geoPtList = (List<GeoPoint>)((GeoPath)colSite).getPoints();
//            List<GeographicalPoint> jbGeoPath = (List<GeographicalPoint>)jbGeographicalPath.getGeographicalPoint();
//            for(Iterator geoPtItr = geoPtList.iterator(); geoPtItr.hasNext();){
//                GeographicalPoint jbGeographicalPoint = jbObjectFactory.createGeographicalPoint();
//                populateGeographicalPoint(jbGeographicalPoint, (GeoPoint)geoPtItr.next());
//                jbGeoPath.add(jbGeographicalPoint);
//            }
//
//        }else {//if(colSite instanceof GeoRegion)
//            throw new Exception ("Unknown subtype of CollectionSite");
//        }
//
//    }
//
//    void populateGeographicalPoint(GeographicalPoint jbGeographicalPoint, GeoPoint geoPt) throws Exception{
//
//        Double longitude = geoPt.getLongitudeAsDouble();
//        Double latitude = geoPt.getLatitudeAsDouble();
//        String depth = geoPt.getDepth();
//        String altitude = geoPt.getAltitude();
//
//        if(altitude!=null && !altitude.equals("")){
//            jbGeographicalPoint.setAltitude (new BigDecimal(altitude));
//        }
//        if(depth!=null && !depth.equals("")){
//            depth = depth.replace("m","");
//            String[] minMax = extractMinMax(depth);
//            if(!minMax[2].equals(UNKNOWN_STR)){
//                jbGeographicalPoint.setDepth (new BigDecimal(minMax[2]));
//            }
//        }
//
//        GeographicalPoint.Longitude.Decimal jbLongDecimal = jbObjectFactory.createGeographicalPointLongitudeDecimal();
//        jbLongDecimal.setValue(new BigDecimal(longitude));
//        GeographicalPoint.Longitude jbLongitude = jbObjectFactory.createGeographicalPointLongitude();
//        jbLongitude.setDecimal(jbLongDecimal);
//        jbGeographicalPoint.setLongitude (jbLongitude);
//
//        GeographicalPoint.Latitude.Decimal jbLatDecimal = jbObjectFactory.createGeographicalPointLatitudeDecimal();
//        jbLatDecimal.setValue(new BigDecimal(latitude));
//        GeographicalPoint.Latitude jbLatitude = jbObjectFactory.createGeographicalPointLatitude();
//        jbLatitude.setDecimal(jbLatDecimal);
//        jbGeographicalPoint.setLatitude (jbLatitude);
//    }
//
//    Environment.Organism populateOrganism(BioMaterial bm) throws Exception {
//        Environment.Organism jbOrganism = jbObjectFactory.createEnvironmentOrganism();
//
//        List<Environment.Organism.CompleteGeneticLineage>  jbCompleteGeneticLineageList = jbOrganism.getCompleteGeneticLineage();
//        Environment.Organism.CompleteGeneticLineage jbCompleteGeneticLineage = jbObjectFactory.createEnvironmentOrganismCompleteGeneticLineage();
//        jbCompleteGeneticLineage.setValue(CompleteGeneticLineageRestriction.UNKNOWN);
//        jbCompleteGeneticLineageList.add(jbCompleteGeneticLineage);
//
//        Environment.Organism.PloidyLevel jbPloidyLevel =
//                jbObjectFactory.createEnvironmentOrganismPloidyLevel();
//        jbPloidyLevel.setValue(PloidyLevelRestriction.UNKNOWN);
//        jbOrganism.setPloidyLevel(jbPloidyLevel);
//
//        //jbOrganism.getReferenceForBiomaterial();
//
//        //jbOrganism.setNumberOfReplicons();
//        //jbOrganism.setExtrachromosomalElements(jbExtrachromosomalElements);
//        //jbOrganism.setEstimatedSize();
//
//        List<Environment.Organism.ReferenceForBiomaterial>  jbReferenceForBiomaterialList = jbOrganism.getReferenceForBiomaterial();
//        Environment.Organism.ReferenceForBiomaterial jbReferenceForBiomaterial = jbObjectFactory.createEnvironmentOrganismReferenceForBiomaterial();
//        jbReferenceForBiomaterial.setValue(UNKNOWN_STR);
//        jbReferenceForBiomaterialList.add(jbReferenceForBiomaterial);
//
//        //jbOrganism.getSourceMaterialIdentifiers();
//
//        Environment.Organism.BioticRelationship jbBioticRelationship =
//                jbObjectFactory.createEnvironmentOrganismBioticRelationship();
//        jbBioticRelationship.setValue(BioticRelationshipRestriction.UNKNOWN);
//        jbOrganism.setBioticRelationship(jbBioticRelationship);
//
//        //jbOrganism.setSpecificHost();
//
//        Environment.Organism.HostSpecificityAndRange jbHostSpecificityAndRange =
//                jbObjectFactory.createEnvironmentOrganismHostSpecificityAndRange();
//        jbHostSpecificityAndRange.setValue(UNKNOWN_STR);
//        jbOrganism.setHostSpecificityAndRange(jbHostSpecificityAndRange);
//
//        Environment.Organism.HealthDiseaseStatusOfSpecificHost jbHealthDiseaseStatusOfSpecificHost =
//                jbObjectFactory.createEnvironmentOrganismHealthDiseaseStatusOfSpecificHost();
//        jbHealthDiseaseStatusOfSpecificHost.setValue(UNKNOWN_STR);
//        jbOrganism.setHealthDiseaseStatusOfSpecificHost(jbHealthDiseaseStatusOfSpecificHost);
//
//        Environment.Organism.NormallyPathogenicityOrNot jbNormallyPathogenicityOrNot =
//                jbObjectFactory.createEnvironmentOrganismNormallyPathogenicityOrNot();
//        jbNormallyPathogenicityOrNot.setValue(UNKNOWN_STR);
//        jbOrganism.setNormallyPathogenicityOrNot(jbNormallyPathogenicityOrNot);
//
//        Environment.Organism.TrophicLevel jbTrophicLevel =
//                jbObjectFactory.createEnvironmentOrganismTrophicLevel();
//        jbTrophicLevel.setValue(TrophicLevelRestriction.UNKNOWN);
//        jbOrganism.setTrophicLevel(jbTrophicLevel);
//
//        Environment.Organism.EstimatedCommunityDiversity jbEstimatedCommunityDiversity =
//                jbObjectFactory.createEnvironmentOrganismEstimatedCommunityDiversity();
//        jbEstimatedCommunityDiversity.setValue(UNKNOWN_STR);
//        jbOrganism.setEstimatedCommunityDiversity(jbEstimatedCommunityDiversity);
//
//        return jbOrganism;
//    }
//
//    MeasurementType populateMeasurement(String min, String max, String unit) throws Exception{
//        MeasurementType msrmnt = jbObjectFactory.createMeasurementType();
//
//        try{
//            if(min!=null && !min.equals(UNKNOWN_STR))
//                msrmnt.setMin(new BigDecimal(min.replaceAll(",","")));
//
//            if(max!=null && !max.equals(UNKNOWN_STR))
//                msrmnt.setMax(new BigDecimal(max.replaceAll(",","")));
//
//            if(unit!=null)
//                msrmnt.setUnit(unit);
//        }catch(Exception e){
//            System.err.println("populateMeasurement(min="+min+", max="+max+", unit="+unit+")\n");
//            e.printStackTrace();
//            //throw new Exception("Error populatingMeasurement",e);
//        }
//        return msrmnt;
//    }
//
//    Migs.Investigation.Study.Biomaterial.WaterBody  populateWaterBody(BioMaterial bm) throws Exception{
//        Migs.Investigation.Study.Biomaterial.WaterBody jbWaterBody =
//                jbObjectFactory.createMigsInvestigationStudyBiomaterialWaterBody();
//
//        Map<String, CollectionObservation> obsMap = bm.getObservations();
//        Set<String> obsKeySet= obsMap.keySet();
//        for(Iterator obsKeyItr = obsKeySet.iterator(); obsKeyItr.hasNext();){
//            String curObsKey = (String)obsKeyItr.next();
//
//            MeasurementType msrmnt;
//            String minmax[];
//
//            CollectionObservation colObs = obsMap.get(curObsKey);
//            String val = colObs.getValue();
//
//            if(val == null) continue;
//            minmax=extractMinMax(val);
//
//            try{
//                msrmnt = populateMeasurement(minmax[0], minmax[1], obsMap.get(curObsKey).getUnits());
//            }catch(Exception e){
//                continue;
//            }
//
//            String lcCurObsKey = curObsKey.toLowerCase();
//
//            if(lcCurObsKey.equals("temperature")){
//                jbWaterBody.setTemperature(msrmnt);
//            }else if(lcCurObsKey.equals("ph")){
//                jbWaterBody.setPH(msrmnt);
//            }else if(lcCurObsKey.equals("salinity")){
//                jbWaterBody.setSalinity(msrmnt);
//            }else if(lcCurObsKey.equals("pressure")){
//                jbWaterBody.setPressure(msrmnt);
//            }else if(lcCurObsKey.equals("chlorophyl") ||lcCurObsKey.equals("chlorophyll") ){
//                jbWaterBody.setChlorophyl(msrmnt);
//            }else if(lcCurObsKey.equals("conductivity")){
//                jbWaterBody.setConductivity(msrmnt);
//            }else if(lcCurObsKey.equals("light intensity")){
//                jbWaterBody.setLightIntensity(msrmnt);
//            }else if(lcCurObsKey.equals("doc") || lcCurObsKey.equals("dissolved organic carbon")){
//                jbWaterBody.setDOC(msrmnt);
//            }else if(lcCurObsKey.equals("current")){
//                jbWaterBody.setCurrent(msrmnt);
//            }else if(lcCurObsKey.equals("atmospheric data")){
//                jbWaterBody.setAtmosphericData(msrmnt);
//            }else if(lcCurObsKey.equals("density")){
//                jbWaterBody.setDensity(msrmnt);
//            }else if(lcCurObsKey.equals("alkalinity")){
//                jbWaterBody.setAlkalinity(msrmnt);
//            }else if(lcCurObsKey.equals("dissolved oxygen")){
//                jbWaterBody.setDissolvedOxygen(msrmnt);
//            }else if(lcCurObsKey.equals("poc") || lcCurObsKey.equals("particulate organic carbon")){
//                jbWaterBody.setPOC(msrmnt);
//            }else if(lcCurObsKey.equals("phosphate")){
//                jbWaterBody.setPhosphate(msrmnt);
//            }else if(lcCurObsKey.equals("nitrate")){
//                jbWaterBody.setNitrate(msrmnt);
//            }else if(lcCurObsKey.equals("sulphates")){
//                jbWaterBody.setSulphates(msrmnt);
//            }else if(lcCurObsKey.equals("sulphides")){
//                jbWaterBody.setSulphides(msrmnt);
//            }else if(lcCurObsKey.equals("primary production")){
//                jbWaterBody.setPrimaryProduction(msrmnt);
//            }
//        }
//
//        return jbWaterBody;
//    }
//
//    static String [] extractMinMax(String range) throws Exception{
//        String spaceFree;
//        spaceFree = range.replaceAll("\\s+", "");
//        String minmax[]= new String[3];
//        Double min, max, avg;
//
//        String components[] = spaceFree.split("\\+\\/-");
//        if(components.length==2){
//            Double base = Double.valueOf(components[0]);
//            Double deviation = Double.valueOf(components[1]);
//            min = ((Double)(base-deviation));
//            max = ((Double)(base+deviation));
//            avg = (min+max)/2;
//            minmax[0]=min.toString(); minmax[1]=max.toString();
//            minmax[2]=avg.toString();
//            return minmax;
//        }
//
//        components = spaceFree.split("--");
//        if(components.length==2){
//            Double lb = Double.valueOf(components[0]);
//            Double ub = Double.valueOf(components[1]);
//            min = ((Double)(lb));
//            max = ((Double)(-ub));
//            avg = (min+max)/2;
//            minmax[0]=min.toString(); minmax[1]=max.toString();
//            minmax[2]=avg.toString();
//            return minmax;
//        }
//
//        boolean lbIsNeg = false;
//        if(spaceFree.charAt(0)=='-'){
//            lbIsNeg=true;
//            spaceFree=spaceFree.replaceFirst("\\-","");
//        }
//        components = spaceFree.split("-",2);
//        if(components.length==2){
//            Double lb = Double.valueOf(components[0]);
//            Double ub = Double.valueOf(components[1]);
//            if(lbIsNeg){
//                min = ((Double)(-lb));
//            }else{
//                min = ((Double)(lb));
//            }
//            max = ((Double)(ub));
//            avg = (min+max)/2;
//            minmax[0]=min.toString(); minmax[1]=max.toString();
//            minmax[2]=avg.toString();
//            return minmax;
//        }
//
//        if(components.length==1){
//            if(spaceFree.charAt(0)=='<'){
//                minmax[0]=UNKNOWN_STR;
//                minmax[1]=spaceFree.replaceAll("<","");
//                minmax[2]=UNKNOWN_STR;
//            }else if(spaceFree.charAt(0)=='>'){
//                minmax[0]=spaceFree.replaceAll(">","");
//                minmax[1]=UNKNOWN_STR;
//                minmax[2]=UNKNOWN_STR;
//            }else{
//                minmax[0]=spaceFree;
//                minmax[1]=spaceFree;
//                minmax[2]=spaceFree;
//            }
//            return minmax;
//        }
//
//        return null;
//
//    }
//
//    Migs.Investigation.Study.Phenotype populatePhenotype(Library library) throws Exception{
//
//        Migs.Investigation.Study.Phenotype jbPhenotype =
//                jbObjectFactory.createMigsInvestigationStudyPhenotype();
//
//        Migs.Investigation.Study.Phenotype.Propagation jbPropagation =
//                jbObjectFactory.createMigsInvestigationStudyPhenotypePropagation();
//        Migs.Investigation.Study.Phenotype.RelationshipToOxygen jbRelationshipToOxygen =
//                jbObjectFactory.createMigsInvestigationStudyPhenotypeRelationshipToOxygen();
//
//        jbPropagation.setValue(PropagationRestriction.UNKNOWN);
//        jbRelationshipToOxygen.setValue(RelationshipToOxygenRestriction.UNKNOWN);
//
//        jbPhenotype.setPropagation(jbPropagation);
//        jbPhenotype.setRelationshipToOxygen(jbRelationshipToOxygen);
//
//        // Allow encoded_traits to be an empty list
//        //jbPhenotype.getEncodedTraits();
//
//        return jbPhenotype;
//    };
//
//    Migs.Investigation.Study.Sources populateSources(Library library) throws Exception{
//        Migs.Investigation.Study.Sources jbSources =
//                jbObjectFactory.createMigsInvestigationStudySources();
//
//        // Both ncbi_genome_projects_prokaryotes and gold will be emtpy.
//
//        return jbSources;
//    };
//
//    Assay populateAssay(Library library) throws Exception{
//
//        Assay jbAssay = jbObjectFactory.createAssay();
//
//        // data_processing will be empty
//
//        Assay.SampleProcessing jbSampleProcessing = jbObjectFactory.createAssaySampleProcessing();
//
//        // Isolation and Growth Conditions
//        Assay.SampleProcessing.IsolationAndGrowthConditions jbIsolationAndGrowthConditions=
//                jbObjectFactory.createAssaySampleProcessingIsolationAndGrowthConditions();
//        jbIsolationAndGrowthConditions.setValue(NOT_SPECIFIED_STR);
//        jbSampleProcessing.setIsolationAndGrowthConditions(jbIsolationAndGrowthConditions);
//
//        // volume of sample
//        // NULL
//        Assay.SampleProcessing.VolumeOfSample jbVolumeOfSample=
//                jbObjectFactory.createAssaySampleProcessingVolumeOfSample();
//        jbSampleProcessing.setVolumeOfSample(jbVolumeOfSample);
//
//
//        // Sampling Strategy
//        Assay.SampleProcessing.SamplingStrategy jbSamplingStrategy =
//                jbObjectFactory.createAssaySampleProcessingSamplingStrategy();
//        jbSamplingStrategy.setValue(SamplingStrategyRestriction.UNKNOWN);
//        jbSampleProcessing.setSamplingStrategy(jbSamplingStrategy);
//
//        // Nucleic Acid Extraction
//        Assay.SampleProcessing.NucleicAcidExtraction jbNucleicAcidExtraction =
//                jbObjectFactory.createAssaySampleProcessingNucleicAcidExtraction();
//
//        // Nucleic Acid Extraction::nucleic acid extraction method
//        Assay.SampleProcessing.NucleicAcidExtraction.NucleicAcidExtractionMethod jbNucleicAcidExtractionMethod =
//                jbObjectFactory.createAssaySampleProcessingNucleicAcidExtractionNucleicAcidExtractionMethod();
//        jbNucleicAcidExtractionMethod.setValue(NucleicAcidExtractionRestriction.UNKNOWN);
//        jbNucleicAcidExtraction.setNucleicAcidExtractionMethod(jbNucleicAcidExtractionMethod);
//
//        // Nucleic Acid Extraction::dna amplification method
//        Assay.SampleProcessing.NucleicAcidExtraction.DnaAmplificationMethod jbDnaAmplificationMethod =
//                jbObjectFactory.createAssaySampleProcessingNucleicAcidExtractionDnaAmplificationMethod();
//        jbDnaAmplificationMethod.setValue(DnaAmplificationRestriction.UNKNOWN);
//        jbNucleicAcidExtraction.setDnaAmplificationMethod(jbDnaAmplificationMethod);
//
//        jbSampleProcessing.setNucleicAcidExtraction(jbNucleicAcidExtraction);
//
//        // library construction
//        List<Assay.SampleProcessing.LibraryConstruction> jbLibraryConstructionList =
//                jbSampleProcessing.getLibraryConstruction();
//        populateLibraryConstructionList(jbLibraryConstructionList, library);
//
//        // sequencing method
//        List<Assay.SampleProcessing.SequencingMethod> jbSequencingMethodList =
//                jbSampleProcessing.getSequencingMethod();
//        populateSequencingMethodList(jbSequencingMethodList, library);
//
//        // Populate jbAssay with Sample Processing before we leave
//        jbAssay.setSampleProcessing(jbSampleProcessing);
//
//        return jbAssay;
//    }
//
//    void populateLibraryConstructionList(List<Assay.SampleProcessing.LibraryConstruction> jbLibraryConstructionList, Library library) throws Exception{
//
//        // jbObjectFactory.createAssaySampleProcessingLibraryConstructionLibrarySize();
//        // jbObjectFactory.createAssaySampleProcessingLibraryConstructionNumberOfClonesSequenced();
//        Assay.SampleProcessing.LibraryConstruction.Vector jbVector =
//                jbObjectFactory.createAssaySampleProcessingLibraryConstructionVector();
//
//        Assay.SampleProcessing.LibraryConstruction jbLibraryConstruction =
//                jbObjectFactory.createAssaySampleProcessingLibraryConstruction();
//
//        // Trying to populate Vector
//        // todo:  we need to store vector in Library someday
//        jbVector.setValue(VectorRestriction.UNKNOWN);
//        jbLibraryConstruction.setVector(jbVector);
//
//        // Trying to populate insert size
//        Assay.SampleProcessing.LibraryConstruction.InsertSize jbInsertSize =
//                jbObjectFactory.createAssaySampleProcessingLibraryConstructionInsertSize();
//        if(library.getMaxInsertSize()!=null){
//            BigInteger maxInsSz = new BigInteger(library.getMaxInsertSize().toString());
//            jbInsertSize.setMaxBps(maxInsSz);
//        }
//        if(library.getMinInsertSize()!=null){
//            BigInteger minInsSz = new BigInteger(library.getMinInsertSize().toString());
//            jbInsertSize.setMinBps(minInsSz);
//        }
//        jbLibraryConstruction.setInsertSize(jbInsertSize);
//
//
//        // jbLibraryConstruction.setNumberOfClonesSequenced();
//        // jbLibraryConstruction.setLibrarySize();
//
//        jbLibraryConstructionList.add(jbLibraryConstruction);
//
//    }
//
//    void populateSequencingMethodList(List<Assay.SampleProcessing.SequencingMethod> jbSequencingMethodList, Library library) throws Exception{
//
//        Assay.SampleProcessing.SequencingMethod jbSequencingMethod =
//                jbObjectFactory.createAssaySampleProcessingSequencingMethod();
//
//        String seqTech = library.getSequencingTechnology();
//        SequencingMethodRestriction migsSeqTech;
//        if(seqTech!=null && (seqTech.equals("pyrosequencing") || seqTech.equals("dideoxysequencing")) ){
//            if (seqTech.equals("pyrosequencing")) {
//                migsSeqTech = SequencingMethodRestriction.PYROSEQUENCING;
//            } else if (seqTech.equals("dideoxysequencing")) {
//                migsSeqTech = SequencingMethodRestriction.DIDEOXYSEQUENCING;
//            } else {
//                migsSeqTech = SequencingMethodRestriction.DIDEOXYSEQUENCING_AND_PYROSEQUENCING;
//            }
//        }else{
//            migsSeqTech = SequencingMethodRestriction.UNKNOWN;
//        }
//
//        jbSequencingMethod.setValue(migsSeqTech);
//        jbSequencingMethodList.add(jbSequencingMethod);
//    }
}
