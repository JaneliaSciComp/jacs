
package org.janelia.it.jacs.shared.processors.recruitment;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.shared.tasks.GenbankFileInfo;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 27, 2007
 * Time: 2:30:52 PM
 */
public class RecruitmentDataHelper {

    public static final String EMPTY_VALUE = "@NONE@";

    // Indexes are with respect to the combinedHits file
    public static final int OLD_SAMPLE_NAME_INDEX = 22;
    public static final int HITS_READ_ID_INDEX = 5;
    public static final int MATE_READ_ID_INDEX = 23;
    public static final int MATE_CATEGORY_INDEX = 24;

    // Indexes w.r.t sample.info file which has sample/site information
    public static final int INFO_FILE_SAMPLE_UNIQUE_NUMBER_INDEX = 0;
    public static final int INFO_FILE_SAMPLE_NAME_INDEX = 3;
    public static final int INFO_FILE_SAMPLE_DESCRIPTION_INDEX = 2;
    public static final int INFO_FILE_PROJECT_INDEX = 4;
    public static final int INFO_FILE_PROJECT_NAME_INDEX = 5;

    // Recruitment Viewer - Property-based values used by the command line executions
    // Size of the image tiles we are generating
    public static final int TILE_SIDE_PIXELS = 256; //SystemConfigurationProperties.getInt("RecruitmentViewer.TileSize");
    public static final int RECRUITMENT_MAX_ZOOM = 4;//SystemConfigurationProperties.getInt("RecruitmentViewer.MaxZoom");//4;
    public static final String IMAGE_EXTENSION = ".png";
    public static final String ALL_GENBANK_INFO_FILE = "allGenbankInfo.tab";

    // Statics for Annotation rendering
    public static final int TRACK_HEIGHT = 10;
    public static final int FEATURE_HEIGHT = 10;

    private String pathToSourceData = "";
    private String resultNodeDirectoryPath = "";
    private String pathToSampleFile = "";
    private String pathToGenbankFile = "";
    private String colorizationType = RecruitmentViewerFilterDataTask.COLORIZATION_SAMPLE;
    private ArrayList<String> data = new ArrayList<String>();
    private HashMap<Object, Color> legendMap = new HashMap<Object, Color>();
    private ArrayList<SampleData> sampleList = new ArrayList<SampleData>();
    private ArrayList<String> geneEntries = new ArrayList<String>();
    private GenbankFile genbankFile;

    // These attributes describe the bounds of the plot
    // param refXBegin - begin of the plot in nt-space
    // param refXEnd - end of the plot in nt-space
    // param refYBegin - begin of the plot in percent identity-space
    // param refYEnd - end of the plot in percent identity-space
    private double refYBegin, refYEnd, refXBegin, refXEnd;
    private List commaSeparatedSampleList;
    private String mateBits = "";
    private String annotationFlag = "";
    public static final String DEFLINE_SAMPLE_NAME = "/sample_name=";
    public static final String DEFLINE_SOURCE      = "/source=";

    public RecruitmentDataHelper(String pathToSampleFile) {
        this.pathToSampleFile = pathToSampleFile;
    }

    public RecruitmentDataHelper(String pathToSourceData, String resultNodeDirectoryPath, String genbankFilePath,
                                 String pathToSampleFile, String commaSeparatedSampleList,
                                 String pidMinInPercent, String pidMaxInPercent, String refBegin, String refEnd,
                                 String mateBits, String annotationFlag, String mateSpanPoint, String colorizationType) {
        this.refXEnd = Double.parseDouble(refEnd);
        this.refXBegin = Double.parseDouble(refBegin);
        this.refYEnd = Double.parseDouble(pidMaxInPercent);
        this.refYBegin = Double.parseDouble(pidMinInPercent);
        this.pathToSourceData = pathToSourceData;
        this.resultNodeDirectoryPath = resultNodeDirectoryPath;
        this.pathToSampleFile = pathToSampleFile;

        if (null == genbankFilePath || "".equals(genbankFilePath) || EMPTY_VALUE.equals(genbankFilePath)) {
            this.pathToGenbankFile = null;
        }
        else {
            this.pathToGenbankFile = genbankFilePath;
        }

        if (null == commaSeparatedSampleList || "".equals(commaSeparatedSampleList) || EMPTY_VALUE.equals(commaSeparatedSampleList)) {
            this.commaSeparatedSampleList = new ArrayList();
        }
        else {
            this.commaSeparatedSampleList = Arrays.asList(commaSeparatedSampleList.split(","));
        }
        this.mateBits = mateBits;

        if (null == annotationFlag || EMPTY_VALUE.equals(annotationFlag)) {
            this.annotationFlag = "";
        }
        else {
            this.annotationFlag = annotationFlag;
        }

        if (null == mateSpanPoint || EMPTY_VALUE.equals(mateSpanPoint)) {
        }
        else {
        }

        if (null == colorizationType || EMPTY_VALUE.equals(colorizationType)) {
            this.colorizationType = RecruitmentViewerFilterDataTask.COLORIZATION_SAMPLE;
        }
        else {
            this.colorizationType = colorizationType;
        }
        //System.out.println("Sample size is: "+this.commaSeparatedSampleList.size());
    }

    /**
     * This main is used by the SGE nodes to execute the RecruitmentViewer image generation action.
     * This processor takes the combined.hits file and generates all necessary image tiles.
     *
     * @param args command line arguments which will determine image tile generation
     */
    public static void main(String[] args) {
        // Not very flexible but who cares, this isn't for general consumption
        try {
            for (int i = 0; i < args.length; i++) {
                System.out.println("Arg(" + i + ")=" + args[i]);
                i++;
                System.out.println("Arg(" + i + ")=" + args[i]);
            }
            if (args.length == 26 && args[0].equals("-src") && args[2].equals("-out") && args[4].equals("-genbankFile")
                    && args[6].equals("-samplePath")
                    && args[8].equals("-sampleList") && args[10].equals("-pidMin") && args[12].equals("-pidMax")
                    && args[14].equals("-refBegin") && args[16].equals("-refEnd") && args[18].equals("-mateBits")
                    && args[20].equals("-annotFilter") && args[22].equals("-mateSpanPoint")
                    && args[24].equals("-colorizationType")) {
                RecruitmentDataHelper processor = new RecruitmentDataHelper(args[1], args[3], args[5],
                        args[7], args[9], args[11], args[13], args[15], args[17], args[19], args[21], args[23], args[25]);
                processor.generateAllFiles();
            }
            // todo add args to generate on-the-fly images
            else {
                System.err.println("The flags do not match the intended usage.  Check the source and try again.");
            }
        }
        catch (Exception e) {
            System.out.println("Error in RecruitmentDataHelper \n" + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * This method is used by the Recruitment.process to have the grid do all necessary operations
     * while recruiting reads and creating image tiles. If unable to perform, drops
     * a failure token file into the result dir
     */
    public void generateAllFiles() {
        long allStart = System.currentTimeMillis();

        try {
            // Create all necessary files
            cleanDirectory();
            // Populate sample information
            sampleList = importSamplesInformation();
            // Recruit reads for display
            filterAndImportTableData(true);
            // Record the number of recruited reads
            createNumberRecruitedReadsFile();
            // Assign colors and write the legend
            setColorsAndCreateLegendFile();
            // Build the images
            try {
                loadAnnotationData();
            }
            catch (Throwable e) {
                System.out.println("Error: " + e.getMessage());
                FileUtil.dropTokenFile(resultNodeDirectoryPath, "annotationsFailure");
                geneEntries = new ArrayList<String>();
            }
            generateImages();
        }
        catch (Exception e) {
            try {
                FileUtil.dropTokenFile(resultNodeDirectoryPath, "processingFailure");
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
            System.out.println("Failed file generation for data in: " + pathToSourceData + ", output: " + resultNodeDirectoryPath);
            e.printStackTrace();
        }

        long allStop = System.currentTimeMillis();
        System.out.println("All files generated in " + ((allStop - allStart) / 1000) + " seconds.");
    }


    /**
     * This method grabs the Genbank data about the organism and stores for later
     * rendering of annotations.
     *
     * @throws FileNotFoundException - could not find the Genbank file
     */
    private void loadAnnotationData() throws Exception {
        if (null != pathToGenbankFile && new File(pathToGenbankFile).exists()) {
            genbankFile = new GenbankFile(pathToGenbankFile);
            genbankFile.populateAnnotations();
            // Throw in a check for size here.  The data in the file should match our value
            // Giving a 10% window +/-
            if (genbankFile.getMoleculeLength() > (refXEnd * 1.1) ||
                    genbankFile.getMoleculeLength() < (refXEnd * 0.9)) {
                throw new Exception("The database size of the entity is greater than 10% off the Genbank value.\nDatabase size=" +
                        refXEnd + "\nGenbank size =" + genbankFile.getMoleculeLength() + "\nStopping.");
            }

            // Get the gene info
            geneEntries = genbankFile.getGeneEntries(annotationFlag);
        }
        else {
            System.out.println("No genbank file (" + pathToGenbankFile + ") exists. No annotations will be shown.");
        }
    }


    /**
     * Establish colors for samples listed in the sampleToOrderMap.
     * Write out results into a legend file
     *
     * @throws java.io.IOException - cannot write the legend file
     */
    private void setColorsAndCreateLegendFile() throws IOException {
        FileWriter writer = null;
        try {
            //System.out.println("In setColorsAndCreateLegendFile");
            writer = new FileWriter(resultNodeDirectoryPath + File.separator + RecruitmentResultFileNode.LEGEND_FILENAME);

            if (RecruitmentViewerFilterDataTask.COLORIZATION_SAMPLE.equals(colorizationType)) {
                float colorSegments = 0.83f / legendMap.size();
                float currentHue = 0;

                // These should be sorted by ordering index already
                for (SampleData sample : sampleList) {
                    if (legendMap.containsKey(sample.getName())) {
                        Color tmpColor = Color.getHSBColor(currentHue, 1, 1);
                        legendMap.put(sample.getName(), tmpColor);
                        // Correct the sample name for the legend file.
                        writer.write(sample.getName() + "\t" + sample.getDescription() + "\t" + tmpColor.getRed() + "\t" +
                                tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                        currentHue += colorSegments;
                    }
                }
            }
            // todo Why can't this be generated once then left forever?????
            else if (RecruitmentViewerFilterDataTask.COLORIZATION_MATE.equals(colorizationType)) {
                Color tmpColor;
                // Good Left
                tmpColor = new Color(0, 255, 0);
                legendMap.put(0, tmpColor);
                writer.write("0\tGood Left\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Good Right
                tmpColor = new Color(0, 160, 0);
                legendMap.put(1, tmpColor);
                writer.write("1\tGood Right\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Too Close Left
                tmpColor = new Color(255, 203, 0);
                legendMap.put(2, tmpColor);
                writer.write("2\tToo Close Left\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Too Close Right
                tmpColor = new Color(249, 255, 0);
                legendMap.put(3, tmpColor);
                writer.write("3\tToo Close Right\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // No Mate Left
                tmpColor = new Color(0, 0, 0);
                legendMap.put(4, tmpColor);
                writer.write("4\tNo Mate Left\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // No Mate Right
                tmpColor = new Color(0, 0, 0);
                legendMap.put(5, tmpColor);
                writer.write("5\tNo Mate Right\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Too Far Left
                tmpColor = new Color(240, 0, 0);
                legendMap.put(6, tmpColor);
                writer.write("6\tToo Far Left\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Too far Right
                tmpColor = new Color(255, 125, 0);
                legendMap.put(7, tmpColor);
                writer.write("7\tToo Far Right\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Anti-Oriented Left
                tmpColor = new Color(167, 134, 89);
                legendMap.put(8, tmpColor);
                writer.write("8\tAnti-Oriented Left\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Anti-Oriented Right
                tmpColor = new Color(187, 94, 69);
                legendMap.put(9, tmpColor);
                writer.write("9\tAnti-Oriented Right\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Normal Oriented Left
                tmpColor = new Color(68, 188, 157);
                legendMap.put(10, tmpColor);
                writer.write("10\tNormal Oriented Left\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Normal Oriented Right
                tmpColor = new Color(50, 102, 138);
                legendMap.put(11, tmpColor);
                writer.write("11\tNormal Oriented Right\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Outie-Oriented Left
                tmpColor = new Color(139, 0, 255);
                legendMap.put(12, tmpColor);
                writer.write("12\tOutie-Oriented Left\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Outie-Oriented Right
                tmpColor = new Color(226, 0, 255);
                legendMap.put(13, tmpColor);
                writer.write("13\tOutie-Oriented Right\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Missing Mate Left
                tmpColor = new Color(0, 255, 243);
                legendMap.put(14, tmpColor);
                writer.write("14\tMissing Mate Left\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
                // Missing Mate Right
                tmpColor = new Color(0, 180, 255);
                legendMap.put(15, tmpColor);
                writer.write("15\tMissing Mate Right\t" + tmpColor.getRed() + "\t" + tmpColor.getGreen() + "\t" + tmpColor.getBlue() + "\n");
            }
        }
        finally {
            if (null != writer) {
                writer.flush();
                writer.close();
            }
        }
    }

    /**
     * This method loads in information relating to bio samples (name, description, etc) and sorts the final list
     * by an index which mirrors the Sorcerer trip.
     *
     * @throws FileNotFoundException - cannot access the sample data file
     */
    public ArrayList<SampleData> importSamplesInformation() throws FileNotFoundException {
        ArrayList<SampleData> tmpList = new ArrayList<SampleData>();
        Scanner scanner = new Scanner(new File(pathToSampleFile));
        try {
            while (scanner.hasNextLine()) {
                String scanLine = scanner.nextLine();
                String[] split = scanLine.split("\t");
                tmpList.add(new SampleData(split[INFO_FILE_SAMPLE_NAME_INDEX], split[INFO_FILE_SAMPLE_DESCRIPTION_INDEX],
                        new Integer(split[INFO_FILE_SAMPLE_UNIQUE_NUMBER_INDEX]), split[INFO_FILE_PROJECT_INDEX],
                        split[INFO_FILE_PROJECT_NAME_INDEX]));
            }
            Collections.sort(tmpList);
        }
        finally {
            scanner.close();
        }
        return tmpList;
    }

    /**
     * Remove old images before regenerating
     * NOTE: Technically, we don't have to do this.  The number and names of files are conserved between tile generation
     * runs
     */
    private void cleanDirectory() {
//        long cleanStart = System.currentTimeMillis();
        File tmpDir = new File(resultNodeDirectoryPath);
        if (tmpDir.exists() && tmpDir.isDirectory()) {
            String[] fileList = tmpDir.list();
            for (String s : fileList) {
//                if (s.startsWith("centerTile") && s.endsWith(IMAGE_EXTENSION)) {
//                    new File(s).delete();
//                }
                if (s.indexOf("Error") > 0 || s.indexOf("Output") > 0) {
                    boolean deleteSuccess = new File(s).delete();
                    if (!deleteSuccess){
                        System.out.println("Did not successfully delete file "+s+". Continuing...");
                    }
                }
            }
        }

//        long cleanStop = System.currentTimeMillis();
        //System.out.println("File cleaning took "+((cleanStop-cleanStart)/1000)+" seconds.");
    }

    /**
     * Scan raw data from combinedHits file and recruit according to criteria provided in the constructor
     *
     * @param buildLegend - boolean to prevent legend generation when only filtering data
     * @throws FileNotFoundException - unable to find the combinedHits file
     */
    private void filterAndImportTableData(boolean buildLegend) throws Exception {
//        long dataStart = System.currentTimeMillis();
        Scanner scanner = new Scanner(new File(pathToSourceData + File.separator + RecruitmentFileNode.COMBINED_FILENAME));
        try {
            while (scanner.hasNextLine()) {
                String scanLine = scanner.nextLine();
                String[] split = scanLine.split("\t");
                // Note: potential improvement -  Pre-sort the reads and stop this loop if the read begin exceeds the reference end
                // If the read falls within the range, then draw it
                double readXBegin = Double.parseDouble(split[2]);
                double readXEnd = Double.parseDouble(split[3]);
                double readPid = (Double.parseDouble(split[10]) / Double.parseDouble(split[16])) * 100;

                // Now verify if read meets requirements for recruitment
                boolean inBounds = (refXBegin <= readXBegin) && (refXEnd >= readXEnd) &&
                        (refYBegin <= readPid) && (refYEnd >= readPid);
                boolean sampleIncluded = commaSeparatedSampleList.contains(split[OLD_SAMPLE_NAME_INDEX]);
                boolean mateIncluded = isMateCategoryIncluded(Integer.parseInt(split[MATE_CATEGORY_INDEX]));

                // If all plays out, then add to the data collection
                if (inBounds && sampleIncluded && mateIncluded) {
                    data.add(scanLine);

                    // If coloring by sample, piggyback this scan to collect sample types
                    if (buildLegend && RecruitmentViewerFilterDataTask.COLORIZATION_SAMPLE.equals(colorizationType)) {
                        // capture the list of samples for future color setting
                        legendMap.put(split[OLD_SAMPLE_NAME_INDEX], Color.black);
                    }
                }
            }
        }
        finally {
            scanner.close();
        }

//        long dataStop = System.currentTimeMillis();
        //System.out.println("Data scanned in "+((dataStop-dataStart)/1000)+" seconds.");
    }

    private boolean isMateCategoryIncluded(int mateCategory) throws Exception {
        if (null == mateBits || mateBits.equals("") || mateBits.length() != 16) {
            throw new Exception("There is no mate information in this request to generate images.\nStopping.");
        }
        // Mate calculation - NOTE:  Reads with mate values higher than 16 have "ambiguous" mates and recruited more
        //                    than once against the genome.  We could eventually let people choose best reads with one
        //                    mate vs. ambiguous reads with > 1 mate.
        int mateValue = (mateCategory - 1) % 16;
        return (mateBits.charAt(mateValue) == '1');
    }


    /**
     * NOTE: Once we have database acess from the grid, this can directly modifiy the info
     *
     * @throws IOException - unable to write the num recruited reads file
     */
    private void createNumberRecruitedReadsFile() throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(new File(resultNodeDirectoryPath + File.separator + RecruitmentResultFileNode.NUM_HITS_FILENAME));
            os.write((Integer.toString(data.size()) + "\n").getBytes());
        }
        finally {
            if (null != os) {
                os.flush();
                os.close();
            }
        }

    }

    /**
     * Method takes the filtered reads and creates an in-memory plot of them
     *
     * @throws IOException problem reading/writing the files
     */
    private void generateImages() throws Exception {
//        long overallStart = System.currentTimeMillis();

        // Loop and generate image tiles. NOTE: memory blows up at zoom level 5
        for (int zoomLevel = 0; zoomLevel <= RECRUITMENT_MAX_ZOOM; zoomLevel++) {
            String zoomDirname = "zoomlevel" + zoomLevel;
            File zoomDir = new File(resultNodeDirectoryPath + File.separator + zoomDirname);
            if (!zoomDir.exists()) {
                if (!zoomDir.mkdirs()) {
                    throw new Exception("Could not create image directory " + zoomDir.getAbsolutePath());
                }
            }
            // Squeezing out any performance benefit.  We do not show axes for zoom 0, currently.
            if (zoomLevel != 0) {
                generateXAxisImages(zoomLevel);
                generateYAxisImages(zoomLevel);
            }
            generateCentralDataImages(zoomLevel);

            // Generate Helper HTML
            //generateHTMLForTiles(resultNodeDirectoryPath + File.separator + zoomDirname + File.separator + "TestZoomXAxis"+zoomLevel+".html", "xAxisTile", zoomLevel);
            //generateHTMLForTiles(resultNodeDirectoryPath + File.separator + zoomDirname + File.separator + "TestZoomYAxis"+zoomLevel+".html", "yAxisTile", zoomLevel);
            //generateHTMLForTiles(resultNodeDirectoryPath + File.separator + zoomDirname + File.separator + "TestZoomCenter"+zoomLevel+".html", "centerTile", zoomLevel);
        }
//        long overallStop = System.currentTimeMillis();
        //System.out.println("Overall processing time: "+((overallStop-overallStart)/1000)+" seconds.");
    }


    /**
     * Method generates the y-axis images required for display in the RV
     *
     * @param zoomLevel - we need to know how many tiles are required
     * @throws IOException - problem generating the image files
     */
    private void generateYAxisImages(int zoomLevel) throws IOException {
//        long start = System.currentTimeMillis();
        if (zoomLevel == 0) return;
        // Set up the image buffer and background
        int pixelMax = (int) Math.pow(2, zoomLevel) * TILE_SIDE_PIXELS;

        BufferedImage buffer = new BufferedImage(pixelMax, pixelMax, BufferedImage.TYPE_INT_RGB);
        Graphics g = buffer.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, pixelMax, pixelMax);

        int pixelSpaceCenterline = pixelMax / 2 + 5;
        int textLine = pixelSpaceCenterline - 15;

        // Calculate and draw the hashmarks
        for (double y = refYBegin; y <= refYEnd; y++) {
            addAxisHashmark(g, y, pixelMax, Integer.toString((int) y), pixelSpaceCenterline, textLine, false,
                    (zoomLevel >= 4 || y % 5 == 0));
        }

        // Crank out the tiles
        int numTiles = (int) Math.pow(2, zoomLevel);
        int axisStart = ((numTiles / 2) - 1) < 0 ? 0 : numTiles / 2 - 1;
        cutTiles("yAxis", buffer, zoomLevel, axisStart, axisStart + 1, 0, numTiles - 1);

//        long stop = System.currentTimeMillis();
        //System.out.println("Zoom = "+zoomLevel+" time to read and create y-axis tiles: "+((stop-start)/1000)+" seconds");
    }


    /**
     * Method generates the x-axis images required for display in the RV
     *
     * @param zoomLevel - we need to know how many tiles are required
     * @throws IOException - problem generating the image files
     */
    private void generateXAxisImages(int zoomLevel) throws IOException {
//        long start = System.currentTimeMillis();
        if (zoomLevel == 0) return;
        // Set up the image buffer and background
        int pixelMax = (int) Math.pow(2, zoomLevel) * TILE_SIDE_PIXELS;

        BufferedImage buffer = new BufferedImage(pixelMax, pixelMax, BufferedImage.TYPE_INT_RGB);
        Graphics g = buffer.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, pixelMax, pixelMax);

        int pixelSpaceCenterline = pixelMax / 2 - 25;
        int textLine = pixelSpaceCenterline + 12;

        // Calculate and draw the hashmarks
        int powerForLabel = (int) Math.log10(refXEnd);
        double segments = Math.pow(10, powerForLabel - 1);
        for (double x = refXBegin; x <= refXEnd; x += segments) {
            addAxisHashmark(g, x - refXBegin, pixelMax, getAxisLabel(x, powerForLabel, false, true), pixelSpaceCenterline,
                    textLine, true, (zoomLevel >= 4 || (x % (5 * segments) == 0)));
        }

        //If including the annotations, render on the image
        // todo the pixel offset could be handled better - like a Track pointer
        // todo could use an annotation boolean here
        if (null != pathToGenbankFile) {
            try {
                addAnnotations(pixelMax, g, pixelSpaceCenterline + 15);
            }
            catch (Throwable e) {
                System.out.println("Error: " + e.getMessage());
                FileUtil.dropTokenFile(resultNodeDirectoryPath, "annotationsFailure");
                geneEntries = new ArrayList<String>();
            }
        }

        //Make the tiles
        int numTiles = (int) Math.pow(2, zoomLevel);
        int axisStart = ((numTiles / 2) - 1) < 0 ? 0 : numTiles / 2 - 1;
        cutTiles("xAxis", buffer, zoomLevel, 0, numTiles - 1, axisStart, axisStart + 1);

//        long stop = System.currentTimeMillis();
        //System.out.println("Zoom = "+zoomLevel+" time to read and create x-axis tiles: "+((stop-start)/1000)+" seconds");
    }


    /**
     * This method parses the NCBI gb file and renders the gene annotations along the axis
     *
     * @param pixelMax             - px length of the "world"
     * @param g                    - graphics context
     * @param pixelSpaceCenterline - location of the line, about which, hashmarks will be set @throws java.io.IOException - could not find the annotation file or a problem parsing it
     * @throws java.io.IOException - unable to get or access the NCBI file
     */
    private void addAnnotations(int pixelMax, Graphics g, int pixelSpaceCenterline) throws IOException {
        // Render the genes
        //System.out.println("Gene section:\n"+gbFeatureSection);
        if (null == geneEntries || 0 == geneEntries.size()) {
            return;
        }
        // List to prevent collision of features
        ArrayList<Long> endList = new ArrayList<Long>();

        for (String entry : geneEntries) {
            double heightPointer = pixelSpaceCenterline + 5;
            //System.out.println("\nLooking at entry:"+entry);
            Color orientationColor = Color.cyan;
            if (entry.indexOf("complement") >= 0) {
                orientationColor = Color.magenta;
            }
            // Cut the rest of the entry off at newline
            // todo FIX THIS!  SKIPPING ANNOTATIONS DEFINED IN PIECES!
            if (entry.indexOf("join") >= 0 || entry.indexOf("..") < 0) {
                continue;
            }

            Long xBegin = genbankFile.getAnnotationCoordinate(entry, true);
            Long xEnd = genbankFile.getAnnotationCoordinate(entry, false);

            // Pack the features here.  Set the heights.  Assumes the features are sorted in x
            for (Long tmpEnd : endList) {
                if (xBegin < tmpEnd) {
                    heightPointer += TRACK_HEIGHT;
                }
                else {
                    endList.clear();
                    break;
                }
            }
            endList.add(xEnd);

            //System.out.println("("+xBegin+", "+xEnd+")");
            addRect(getXPixelLocForReadLoc(xBegin, pixelMax), getXPixelLocForReadLoc(xEnd, pixelMax),
                    heightPointer, g, orientationColor, FEATURE_HEIGHT / 2);
        }
    }


    /**
     * Method determines what label should be applies to an axis plot
     *
     * @param numberValue    - number for the label
     * @param powerOfTen     - getting a magnitude value for the label
     * @param roundToInteger - whether to round the number suppied or not
     * @param useUnits       - whether to add units in the label
     * @return the label for the axis
     */
    private String getAxisLabel(double numberValue, int powerOfTen, boolean roundToInteger, boolean useUnits) {
        String value;
        String unit = "";
        NumberFormat formatter = NumberFormat.getInstance();    // only want 2 significant digits
        formatter.setMaximumFractionDigits(1);

        if (powerOfTen < 3) {
            if (useUnits) {
                unit = "b";
            }
        }
        else if (powerOfTen < 6) {
            numberValue = (numberValue / Math.pow(10, 3));
            if (useUnits) {
                unit = "k";
            }
        }
        else if (powerOfTen < 9) {
            numberValue = numberValue / Math.pow(10, 6);
            if (useUnits) {
                unit = "M";
            }
        }
        else if (powerOfTen < 12) {
            numberValue = numberValue / Math.pow(10, 9);
            if (useUnits) {
                unit = "G";
            }
        }

        if (roundToInteger) {
            numberValue = Math.round(numberValue);
        }
        value = formatter.format(numberValue);
        return value + unit;
    }


    /**
     * Method to add a hashmark to the axis image
     *
     * @param g                    - graphics context
     * @param relativeValue        - relative value which will be converted to pixel-space
     * @param pixelMax             - maximum value in pixel-space
     * @param hashLabel            the label used for the hashmark
     * @param pixelSpaceCenterline - location of the line, about which, hashmarks will be set
     * @param textLine             - location of the line, about which, hashmark labels will be set
     * @param isXAxis              - whether this hashmark is for the x or y axis
     * @param largeHashmark        - determines whether this is for a major or minor hashmark
     */
    private void addAxisHashmark(Graphics g, double relativeValue, double pixelMax, String hashLabel,
                                 int pixelSpaceCenterline, int textLine, boolean isXAxis, boolean largeHashmark) {
        int xAxisPosition1;
        int xAxisPosition2;
        int yAxisPosition1;
        int yAxisPosition2;
        int xTextPosition;
        int yTextPosition;

        // Set the values according to the axis being drawn
        if (isXAxis) {
            xAxisPosition1 = (int) getXPixelLocForReadLoc(relativeValue, pixelMax);
            xAxisPosition2 = xAxisPosition1;
            xTextPosition = xAxisPosition1;
            yTextPosition = textLine;
            if (!largeHashmark) {
                yAxisPosition1 = pixelSpaceCenterline - 7;
                yAxisPosition2 = yAxisPosition1 - 8;
            }
            else {
                yAxisPosition1 = pixelSpaceCenterline;
                yAxisPosition2 = yAxisPosition1 - 15;
            }
        }
        else {
            yAxisPosition1 = (int) getYPixelLocForPidLoc(relativeValue, pixelMax);
            yAxisPosition2 = yAxisPosition1;
            xTextPosition = textLine;
            yTextPosition = yAxisPosition1 + 5;
            if (!largeHashmark) {
                xAxisPosition1 = pixelSpaceCenterline + 7;
                xAxisPosition2 = xAxisPosition1 + 8;
            }
            else {
                xAxisPosition1 = pixelSpaceCenterline;
                xAxisPosition2 = xAxisPosition1 + 15;
            }
        }

        // Axis hashmark
        g.setColor(Color.black);
        g.drawLine(xAxisPosition1, yAxisPosition1, xAxisPosition2, yAxisPosition2);
        // Axis Label
        if (largeHashmark) {
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.drawString(hashLabel, xTextPosition, yTextPosition);
        }
    }


    /**
     * Method does the work of plotting the recruited reads and creating the image tiles
     *
     * @param zoomLevel - sets the scaling of the nt/%-id space to pixel-space
     * @throws IOException - problem trying to create image tiles
     */
    private void generateCentralDataImages(int zoomLevel) throws IOException {
//        long start = System.currentTimeMillis();
        double pixelYAdjust = 2;   // pixel-space
        // Set up the image buffer and background
        int pixelMax = (int) Math.pow(2, zoomLevel) * TILE_SIDE_PIXELS;

        BufferedImage buffer = new BufferedImage(pixelMax, pixelMax, BufferedImage.TYPE_INT_RGB);
        Graphics g = buffer.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, pixelMax, pixelMax);

        // Create the graphics
        for (String aData : data) {
            String[] split = aData.split("\t");
            // If the read falls within the range, then draw it
            double readXBegin = Double.parseDouble(split[2]);
            double readXEnd = Double.parseDouble(split[3]);
            double readPid = (Double.parseDouble(split[10]) / Double.parseDouble(split[16])) * 100;
            addRect(getXPixelLocForReadLoc(readXBegin, pixelMax), getXPixelLocForReadLoc(readXEnd, pixelMax),
                    getYPixelLocForPidLoc(readPid, pixelMax), g, getColorForData(split),
                    pixelYAdjust);
        }

        // Since zoomLevel=0 is for thumbnails and Overview, no grid is necessary.
        if (zoomLevel != 0) {
            // Create the helper grid
            // Calculate and draw the y-axis
            Color lineColor = new Color(0, 0, 0, 75);
            g.setColor(lineColor);
            for (double y = refYBegin; y <= refYEnd; y++) {
                if (zoomLevel >= 4 || y % 5 == 0) {
                    g.drawLine((int) getXPixelLocForReadLoc(refXBegin, pixelMax),
                            (int) getYPixelLocForPidLoc(y, pixelMax),
                            (int) getXPixelLocForReadLoc(refXEnd, pixelMax),
                            (int) getYPixelLocForPidLoc(y, pixelMax));
                }
            }
            // Calculate and draw the x-axis
            int powerForLabel = (int) Math.log10(refXEnd);
            double segments = Math.pow(10, powerForLabel - 1);
            for (double x = (refXBegin + segments); x <= refXEnd; x += segments) {
                if (zoomLevel >= 4 || (x % (5 * segments) == 0)) {
                    g.drawLine((int) getXPixelLocForReadLoc(x, pixelMax),
                            (int) getYPixelLocForPidLoc(refYBegin, pixelMax),
                            (int) getXPixelLocForReadLoc(x, pixelMax),
                            (int) getYPixelLocForPidLoc(refYEnd, pixelMax));
                }
            }
        }

        // Crank out the tiles
        int numTiles = (int) Math.pow(2, zoomLevel);
        cutTiles("center", buffer, zoomLevel, 0, numTiles - 1, 0, numTiles - 1);

//        long stop = System.currentTimeMillis();
        //System.out.println("Zoom = "+zoomLevel+" time to read and create tiles: "+((stop-start)/1000)+" seconds");
    }


    /**
     * Method takes the desired image name prefix, buffer of the graphic, and the zoomLevel imvolved and cranks out
     * image tiles.  Is used for axis tile generation and also central data generation.
     *
     * @param tilePrefix - prefix to the image files: central, yAxis, xAxis
     * @param buffer     - buffer containing the plots of reads in pixel-space
     * @param zoomLevel  - used to determine the number of tiles required
     * @throws IOException - problem creating the tile files
     */
    private void cutTiles(String tilePrefix, BufferedImage buffer, int zoomLevel,
                          int xTileStart, int xTileStop, int yTileStart, int yTileStop) throws IOException {
        for (int x = xTileStart; x <= xTileStop; x++) {
            for (int y = yTileStart; y <= yTileStop; y++) {
                BufferedImage subImage = buffer.getSubimage(x * TILE_SIDE_PIXELS, y * TILE_SIDE_PIXELS,
                        TILE_SIDE_PIXELS, TILE_SIDE_PIXELS);
                ImageIO.write(subImage, "png", new FileOutputStream(resultNodeDirectoryPath + File.separator + "zoomlevel" + zoomLevel +
                        File.separator + tilePrefix + "Tile" + x + "_" + y + "_" + zoomLevel + IMAGE_EXTENSION));
            }
        }
    }

    /**
     * Method (in pixel-space) which creates a colored rectangle.
     * It plots the reads and features against the reference axis.
     *
     * @param pixXBegin    - begin px of the read
     * @param pixXEnd      - end px of the read
     * @param pixYCenter   - px y-value of the read
     * @param g            - graphics object which represents the final plot
     * @param c            - color of the read in the plot
     * @param pixelYAdjust - value which centers the read about the percent identity value in pixel-space
     */
    private void addRect(double pixXBegin, double pixXEnd, double pixYCenter, Graphics g, Color c,
                         double pixelYAdjust) {
        // Offset by the reference X and Y begin values
        double pixYBegin = pixYCenter - pixelYAdjust;
        double pixYEnd = pixYCenter + pixelYAdjust;

        // If the scaled pixel range is too small , then make it 1 pixel
        int pixRange = (int) (pixXEnd - pixXBegin);
        // NOTE: if the grid line is 1 px then the below value may need to be 3, so we can center about the line
        if (pixRange == 0) {
            pixRange = 2;
        }

        // Add the rect - If the legend is empty, default to Black
        if (null == c) {
            c = Color.BLACK;
        }
        g.setColor(c);
        g.fillRect((int) pixXBegin, (int) pixYBegin, pixRange, (int) (pixYEnd - pixYBegin));
    }

    /**
     * Method to convert read nt-position to pixel-space location
     *
     * @param readPosition - nt location of the read against some axis
     * @param pixelMax     - extent of pixel-space
     * @return pixel value against x-axis
     */
    private double getXPixelLocForReadLoc(double readPosition, double pixelMax) {
        return ((readPosition - refXBegin) / (refXEnd - refXBegin)) * pixelMax;
    }

    /**
     * Method to convert read percent id value to pixel-space location
     *
     * @param readPid  - percent identity of the read
     * @param pixelMax - extent of pixel-space
     * @return pixel value against y-axis
     */
    private double getYPixelLocForPidLoc(double readPid, double pixelMax) {
        return ((1 - (readPid - refYBegin) / (refYEnd - refYBegin))) * pixelMax;
    }


    /**
     * This method creates HTML files which can be used offline to view all generated tiles, layed out in order
     *
     * @param outputFilenameWithPath - name of the HTML file produced
     * @param tilePrefix             - prefix string of the image tiles, used for HTML layout
     * @param zoomLevel              - sets the size of the table in HTML
     * @throws IOException - problem creating the HTML file
     */
//    private void generateHTMLForTiles(String outputFilenameWithPath, String tilePrefix, int zoomLevel) throws IOException {
//        FileOutputStream fos = new FileOutputStream(outputFilenameWithPath);
//        int numTilesPerSide = (int) Math.pow(2, zoomLevel);
//        StringBuffer sbuf = new StringBuffer("<html>\n");
//        sbuf.append(" <table border=\"1\" cellspacing=\"0\" cellpadding=\"0\">\n");
//        // Show the tiles
//        for (int y = 0; y < numTilesPerSide; y++) {
//            sbuf.append("  <tr valign=\"top\">\n");
//            for (int x = 0; x < numTilesPerSide; x++) {
//                String filename = tilePrefix + x + "_" + y + "_" + zoomLevel + IMAGE_EXTENSION;
//                sbuf.append("     <th><image src=\"").append(filename).append("\" alt=\"").append(filename).append("\"/></th>\n");
//            }
//            sbuf.append("  </tr>\n");
//        }
//        sbuf.append(" </table>\n");
//        sbuf.append("</html>\n");
//        fos.write(sbuf.toString().getBytes());
//        fos.flush();
//        fos.close();
//    }

    /**
     * This method is intended to be used with the "Export Selected Sequences" functionality of the FRV.
     * This RVProcessor is constructed with
     *
     * @return set of accessions for Reads which were rubberbanded
     * @throws FileNotFoundException unable to read the file
     */
    public Set<String> exportSelectedSequenceIds() throws Exception {
        Set<String> selectedReadAccession = new HashSet<String>();
        filterAndImportTableData(false);
        for (String s : data) {
            String[] pieces = s.split("\t");
            if (null != pieces[HITS_READ_ID_INDEX]) {
                selectedReadAccession.add(pieces[HITS_READ_ID_INDEX]);
            }
        }
        return selectedReadAccession;
    }

    /**
     * Since the reads Doug used had different accessions, we need to change them with these
     * prefixes
     *
     * @param oldSampleName - name of the sample
     * @return which accession to prefix any read with
     */
    public static String getPrefixForReadAccession(String oldSampleName) {
        if (null == oldSampleName) return "";
        else if (oldSampleName.startsWith("de")) {
            return "HOT_READ_";
        }
        else if (oldSampleName.startsWith("GOM")) {
            return "SCUMS_READ_";
        }
        return "JCVI_READ_";
    }

    public void filterAnnotations() throws IOException {
        long allStart = System.currentTimeMillis();
        // Load the annotation data
        try {
            loadAnnotationData();
        }
        catch (Throwable e) {
            System.out.println("Error: " + e.getMessage());
            FileUtil.dropTokenFile(resultNodeDirectoryPath, "annotationsFailure");
            geneEntries = new ArrayList<String>();
        }

        for (int zoomLevel = 1; zoomLevel <= RECRUITMENT_MAX_ZOOM; zoomLevel++) {
            generateXAxisImages(zoomLevel);
        }

        long allStop = System.currentTimeMillis();
        System.out.println("All files generated in " + ((allStop - allStart) / 1000) + " seconds.");
    }

    public void regenerateDataOnly() {
        long allStart = System.currentTimeMillis();

        try {
            // Populate sample information
            sampleList = importSamplesInformation();
            // Recruit reads for display
            filterAndImportTableData(true);
            // Record the number of recruited reads
            createNumberRecruitedReadsFile();
            // Assign colors and write the legend
            setColorsAndCreateLegendFile();
            for (int zoomLevel = 0; zoomLevel <= RECRUITMENT_MAX_ZOOM; zoomLevel++) {
                generateCentralDataImages(zoomLevel);
            }
        }
        catch (Exception e) {
            try {
                FileUtil.dropTokenFile(resultNodeDirectoryPath, "processingFailure");
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
            System.out.println("Failed file generation for data in: " + pathToSourceData + ", output: " + resultNodeDirectoryPath);
            e.printStackTrace();
        }

        long allStop = System.currentTimeMillis();
        System.out.println("All files generated in " + ((allStop - allStart) / 1000) + " seconds.");
    }

    public void regenerateDataAndAnnotations() {
        long allStart = System.currentTimeMillis();

        try {
            // Populate sample information
            sampleList = importSamplesInformation();
            // Recruit reads for display
            filterAndImportTableData(true);
            // Record the number of recruited reads
            createNumberRecruitedReadsFile();
            // Assign colors and write the legend
            setColorsAndCreateLegendFile();
            // Build the images
            try {
                loadAnnotationData();
            }
            catch (Throwable e) {
                System.out.println("Error: " + e.getMessage());
                FileUtil.dropTokenFile(resultNodeDirectoryPath, "annotationsFailure");
                geneEntries = new ArrayList<String>();
            }
            // Loop and generate image tiles. NOTE: memory blows up at zoom level 5
            for (int zoomLevel = 0; zoomLevel <= RECRUITMENT_MAX_ZOOM; zoomLevel++) {
                generateXAxisImages(zoomLevel);
                generateCentralDataImages(zoomLevel);
            }
        }
        catch (Exception e) {
            try {
                FileUtil.dropTokenFile(resultNodeDirectoryPath, "processingFailure");
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
            System.out.println("Failed file generation for data in: " + pathToSourceData + ", output: " + resultNodeDirectoryPath);
            e.printStackTrace();
        }

        long allStop = System.currentTimeMillis();
        System.out.println("All files generated in " + ((allStop - allStart) / 1000) + " seconds.");
    }


    /**
     * Method to determine the color of a feature based on the predefined colorization scheme; by sample, or by mate
     * category
     *
     * @param rowOfData - row of info from the combinedHits file
     * @return color for the row of data
     */
    public Color getColorForData(String[] rowOfData) {
        // Coloring by sample
        if (RecruitmentViewerFilterDataTask.COLORIZATION_SAMPLE.equals(colorizationType)) {
            return legendMap.get(rowOfData[OLD_SAMPLE_NAME_INDEX]);
        }
        // Coloring by mate
        else if (RecruitmentViewerFilterDataTask.COLORIZATION_MATE.equals(colorizationType)) {
            int mateCategory = (Integer.valueOf(rowOfData[MATE_CATEGORY_INDEX]) - 1) % 16;
            return legendMap.get(mateCategory);
        }
        // else if no clear colorization type, return black
        return Color.BLACK;
    }


    /**
     * In order to limit the memory footprint of the server this method was changed from querying the db for GenomeProjects
     * to scanning the system/GenomeProject dir for node directories.
     *
     * @return list of GenbankFileInfo objects
     * @throws Exception - problem finding/scanning the genome project, genbank files
     */
    public static synchronized void buildGenbankFileList() throws Exception {
        String systemGenomeProjectDir = SystemConfigurationProperties.getString("FileStore.CentralDir") + File.separator +
                User.SYSTEM_USER_LOGIN + File.separator + GenomeProjectFileNode.SUB_DIRECTORY;
        File gpDir = new File(systemGenomeProjectDir);
        if (!gpDir.exists() || !gpDir.isDirectory()) {
            throw new Exception("Cannot find the system GenomeProject directory");
        }
        File allGenbankInfoFile = new File(systemGenomeProjectDir+File.separator+ALL_GENBANK_INFO_FILE);
        String tmpFileName = systemGenomeProjectDir+File.separator+ALL_GENBANK_INFO_FILE+".tmp";
        File tmpGenbankInfoFile = new File(tmpFileName);
        // Clean up the old tmp file if it exists
        if (tmpGenbankInfoFile.exists()){
            boolean deleteSuccess = tmpGenbankInfoFile.delete();
            if (!deleteSuccess){
                throw new Exception("Unable to delete "+tmpGenbankInfoFile.getAbsolutePath());
            }
        }
        FileWriter writer = new FileWriter(tmpGenbankInfoFile);
        File[] genomeProjects = gpDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir.getAbsolutePath() + File.separator + name).isDirectory();
            }
        });
        System.out.println("There are " + genomeProjects.length + " genome projects found.");
        try {
            long totalBases = 0l;
            int totalGenbankFiles = 0;
            for (File gpNodeDir : genomeProjects) {
                // Get the sequences
                File[] genbankFiles = gpNodeDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return (name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_COMPLETE)||
                                name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_ALTERNAME_COMPLETE)||
                                name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_NOT_STRUCTURAL)) &&
                                name.endsWith(GenomeProjectFileNode.GENBANK_FILE_EXTENSION);
                    }
                });
                // Check for no gbk files.  Shouldn't happen but still.
                if (null == genbankFiles) {
                    System.out.println("The list of *.gbk files returned null.  Continuing...");
                    continue;
                }
                totalGenbankFiles += genbankFiles.length;
                // add the local Genbank files to the complete list
                for (File genbankFile : genbankFiles) {
                    try {
                        GenbankFile tmpGF = new GenbankFile(genbankFile.getAbsolutePath());
                        long tmpBaseLength = tmpGF.getMoleculeLength();
                        String tmpSequenceWithoutGaps = tmpGF.getFastaFormattedSequence().replaceAll("[Nn\n]","");
                        totalBases += tmpBaseLength;
                        writer.append(gpNodeDir.getName()).append("\t").
                               append(genbankFile.getAbsolutePath()).append("\t").
                               append(Long.toString(tmpBaseLength)).append("\t").
                               append(Long.toString(tmpSequenceWithoutGaps.length())).append("\n");
                    }
                    catch (Exception e) {
                        System.out.println("Error trying to parse file " + genbankFile.getAbsolutePath() + "\n" + e.getMessage());
                    }
                }
            }
            System.out.println("\nTotal Genome Projects: " + genomeProjects.length +
                    "\nTotal Genbank Files found:   " + totalGenbankFiles +
                    "\nTotal sequence found:        " + totalBases + " bases");
            if (allGenbankInfoFile.exists()) {
                FileUtil.moveFileUsingSystemCall(allGenbankInfoFile.getAbsolutePath(), allGenbankInfoFile.getAbsolutePath()+"."+System.currentTimeMillis());
            }
            FileUtil.moveFileUsingSystemCall(tmpGenbankInfoFile, allGenbankInfoFile);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (null!=writer){
                writer.close();
            }
        }
    }

    /**
     * This method returns a map of GBK name, GenbankFileInfo object.  This is so we can quickly search for the file path
     * and associated GenomeProject information.
     *
     * @return a map of GBK file name and GenbankFileInfo object
     * @throws Exception thrown when there is a problem building the GenbankFileInfo objects
     */
    public static HashMap<String, GenbankFileInfo> getGenbankFileMap() throws Exception {
        List<GenbankFileInfo> genbankList = getGenbankFileList();
        HashMap<String, GenbankFileInfo> returnMap = new HashMap<String, GenbankFileInfo>();
        for (GenbankFileInfo genbankFileInfo : genbankList) {
            returnMap.put(genbankFileInfo.getGenbankFile().getName(), genbankFileInfo);
        }
        return returnMap;
    }

    public static List<GenbankFileInfo> getGenbankFileList() {
        String systemGenomeProjectDir = SystemConfigurationProperties.getString("FileStore.CentralDir") + File.separator +
                User.SYSTEM_USER_LOGIN + File.separator + GenomeProjectFileNode.SUB_DIRECTORY;
        File allGenbankInfoFile = new File(systemGenomeProjectDir+File.separator+ALL_GENBANK_INFO_FILE);
        ArrayList<GenbankFileInfo> completeFileList = new ArrayList<GenbankFileInfo>();
        Scanner scanner=null;
        try {
            scanner = new Scanner(allGenbankInfoFile);
            while (scanner.hasNextLine()) {
                String[] split = scanner.nextLine().split("\t");
                completeFileList.add(new GenbankFileInfo(Long.valueOf(split[0]),
                        new File(split[1]), Long.valueOf(split[2]), Long.valueOf(split[3])));
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            if (null!=scanner) {
                scanner.close();
            }
        }
        return completeFileList;
    }
}
