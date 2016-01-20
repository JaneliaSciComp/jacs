// Date released:  2014-10-05
// FIJI macro for generating enchanced MIPs and movies for Polarity and MCFO data.
//  
// The argument should be in this format: "OutputDir,Prefix,Type,Image,Chanspec,Colorspec"
// 
// Input parameters: 
//     OutputDir: base output directory
//     Prefix: output filename prefix
//     Mode: processing mode, i.e. ["none","mcfo","polarity"]
//     Image: image path
//     ChannelSpec: channel specification
//     ColorSpec: color specification [(R)ed, (G)reen, (B)lue, grey(1), (C)yan, (M)agenta, (Y)ellow]
//     Outputs: colon-delimited list of outputs to generate ["mips","movies"], e.g. "mips:movies"
// 
// Currently this macro always outputs a grey reference channel, thus the colorspec should 
// be "1" in the same position that the chanspec is "r".
// 
// If mode is "mcfo" then the input file should have 4 channels, and the signal channels will  
// be considered to be multi-color channels. In mcfo mode, this macro accomplishes the following:
// - adjust intensity
// - ramp signals in Z axis for neuron channels
// - remove speckles based on size in 2D projection from two directions  

// If the mode is "polarity" then the input file must have 2 or 3 channels, with type:
// [presynatic,membrane,reference] or [membrane,reference]
// Thus, the chanspec is expected to be either "ssr" or "sr". In polarity mode, this macro 
// accomplishes the following:
// - adjust intensity
// - ramp signals in Z axis for neuron channels
// - mask presynaptic marker channel by membrane channel
//

// Global variables

var mipFormat = "PNG";
var outputs = "mips:movies"

// Initialization

setBackgroundColor(0,0,0);
setForegroundColor(255,255,255);
setBatchMode(true);

// Arguments

args = split(getArgument(),",");
basedir = args[0];
prefix = args[1];
mode = args[2];
image = args[3];
chanspec = toLowerCase(args[4]);
colorspec = toUpperCase(args[5]);
if (args.length > 6) {
    outputs = toLowerCase(args[6]);
}

print("Output dir: "+basedir);
print("Output prefix: "+prefix);
print("Processing mode: "+mode);
print("Input image: "+image);
print("Channel spec: "+chanspec);
print("Color spec: "+colorspec);
print("Outputs: "+outputs);

createMIPS = false;
createMovies = false;
if (outputs!="") {
    if (matches(outputs,".*mips.*")) {
        createMIPS = true;
    }
    if (matches(outputs,".*movies.*")) {
        createMovies = true;
    }
}

if (!createMIPS && !createMovies) {
    print("No outputs selected, exiting.");
    run("Quit");
}

// Open input files

var width, height, channels, slices, frames;
openChannels();
var numChannels = channels;

// Figure out how to map the channels in the final image

merge_name = getChannelMapping();

if (mode=="mcfo" || mode=="polarity") {
    // Z-intensity compensation to ramp signals in neuron channels
    print("Preparing for Z compensation");
    newImage("Ramp", "32-bit ramp", slices, width, height);
    run("Add...", "value=1 stack");
    run("Reslice [/]...", "output=1.000 start=Right avoid");
    rename("ZRamp");
    selectWindow("Ramp");
    close();
}

if (mode=="mcfo") {
    processChannel("signal1");
    if (numChannels > 2) {
        processChannel("signal2");
        if (numChannels > 3) {
            processChannel("signal3");
        }
    }
}
else if (mode=="polarity") {
    if (numChannels > 2) {
      // Process signal channel NeuronC1 (presynaptic)
      print("Processing presynaptic channel");
      selectWindow("signal1");
      title = getTitle();
      rename("original");
      imageCalculator("Multiply create 32-bit stack", "original", "ZRamp");
      rename("processing");
      performHistogramStretching();
      selectWindow("original");
      close();
      selectWindow("processing");
      run("Z Project...", "projection=[Max Intensity]");
      run("Select All");
      getStatistics(area, mean, min, max, std, histogram);
      close();
      run("Subtract...", "value="+mean+" stack");
      rename(title);
      
      // Process signal channel NeuronC2 (membrane)
      print("Processing membrane channel");
      processChannel("signal2");
      run("Z Project...", "projection=[Max Intensity]");
      run("Select All");
      getStatistics(area, mean, min, max, std, histogram);
      close();
      Mean2Std = mean + 2*std;
      run("Duplicate...", "title=signal2_mask duplicate");
      setThreshold(Mean2Std, 255);
      run("Convert to Mask", "background=Dark black");
      run("Divide...", "value=255 stack");
      imageCalculator("Multiply stack", "signal1", "signal2_mask");
      selectWindow("signal2_mask");
      close();
      selectWindow("signal2");
      run("Subtract...", "value="+mean+" stack");
    }
    else if (numChannels > 1) {
      // Process signal channel NeuronC1 (membrane)
      print("Processing membrane channel");
      processChannel("signal1");
      run("Z Project...", "projection=[Max Intensity]");
      run("Select All");
      getStatistics(area, mean, min, max, std, histogram);
      close();
      run("Subtract...", "value="+mean+" stack");
    }
}

if (mode=="mcfo" || mode=="polarity") {
    selectWindow("ZRamp");
    close();
}

// Process reference channel
print("Processing reference channel");
selectWindow("reference");
title = getTitle();
performHistogramStretching();
run("Divide...", "value=2 stack");
run("8-bit");
rename(title);

if (merge_name!="") {
    print("Merging channels: "+merge_name);
    run("Merge Channels...", merge_name+" ignore");
    // Sometimes the merge creates a composite image, this will fix it
    getDimensions(width, height, channels, slices, frames);
    if (channels == 2) {
        run("RGB Color", "slices");
        rename("RGB");
    }
}

if (createMIPS) {
    print("Creating MIPs");
    titleMIP = prefix + "_all";
    titleSignalMIP = prefix + "_signal";
    titleRefMIP = prefix + "_reference";
    
    if (numChannels==1) {
        selectWindow("reference");
        run("Z Project...", "projection=[Standard Deviation]");
        run("8-bit");
        run("Divide...", "value=3");
        run("RGB Color");
        saveAs(mipFormat, basedir+'/'+titleRefMIP);
        saveAs(mipFormat, basedir+'/'+titleMIP);
    }
    else {
        selectWindow("RGB");
        run("Z Project...", "projection=[Max Intensity]");
        selectWindow("reference");
        run("Z Project...", "projection=[Standard Deviation]");
        run("8-bit");
        run("Divide...", "value=3");
        run("RGB Color");
        saveAs(mipFormat, basedir+'/'+titleRefMIP);
        rename("STD_reference");
        
        selectWindow("MAX_RGB");
        saveAs(mipFormat, basedir+'/'+titleSignalMIP);
        rename("MAX_RGB");
        
        imageCalculator("Add", "MAX_RGB", "STD_reference");
        selectWindow("MAX_RGB");
        saveAs(mipFormat, basedir+'/'+titleMIP);
        close();
        
        selectWindow("STD_reference");
        close();
    }
}

if (createMovies) {
    print("Creating movies");
    titleAvi = prefix + "_all.avi";
    titleSignalAvi = prefix + "_signal.avi";
    titleRefAvi = prefix + "_reference.avi";
    
    if (numChannels==1) {
        selectWindow("reference");
        run("RGB Color");
        rename("RefMovie");
        padImageDimensions("RefMovie");
        print("Saving Reference AVI");
        run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleRefAvi);
        print("Saving AVI");
        run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleAvi);
        close();
    }
    else {
        selectWindow("RGB");
        run("Duplicate...", "title=SignalMovie duplicate");
        padImageDimensions("SignalMovie");
        print("Saving Signal AVI");
        run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleSignalAvi);
        close();
        
        selectWindow("reference");
        run("RGB Color");
        run("Duplicate...", "title=RefMovie duplicate");
        padImageDimensions("RefMovie");
        print("Saving Reference AVI");
        run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleRefAvi);
        close();
        
        imageCalculator("Add create stack", "RGB", "reference");
        rename("FinalMovie");
        selectWindow("RGB");
        close();
        selectWindow("reference");
        close();
        padImageDimensions("FinalMovie");
        print("Saving AVI");
        run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleAvi);
    }
}

print("Done");
run("Close All");
run("Quit");


function openChannels() {
    open(image);
    print(image+" "+chanspec+" "+colorspec);
    getDimensions(width, height, channels, slices, frames);
    print(width + "x" + height + "  Slices: " + slices + "  Channels: " + channels);
    print("Splitting and renaming channels");
    if (channels>1) {
        rename("original");
        run("Split Channels");
    }
    else {
        rename("C1-original");
    }
}

function getChannelMapping() {
    merge_name = "";
    signal_count = 0;
    
    for (i=0; i<lengthOf(chanspec); i++) {
        wname = "C" + (i+1) + "-original";
        selectWindow(wname);
        cc = substring(chanspec,i,i+1);
        col = substring(colorspec,i,i+1);
        if (cc == 'r') {
          rename('reference');
          print("  Renamed " + wname + " to reference");
        }
        else {
            signal_count++;
            cname = 'signal'+signal_count;
            rename(cname);
            print("  Renamed " + wname + " to " + cname);
      
            targetChannel = 0;
            if (col == 'R') {
                targetChannel = 1;
            }
            else if (col == 'G') {
                targetChannel = 2;
            }
            else if (col == 'B') {
                targetChannel = 3;
            }
            else if (col == '1') {
                targetChannel = 4;
            }
            else if (col == 'C') {
                targetChannel = 5;
            }
            else if (col == 'M') {
                targetChannel = 6;
            }
            else if (col == 'Y') {
                targetChannel = 7;
            }
            else {
                // ignore channel in output
            }
      
            if (targetChannel > 0) {
                merge_name = merge_name + "c" + targetChannel + "=" + cname + " ";
            }
        }
    }
    
    return merge_name;
}

function processChannel(channel_name) {
    selectWindow(channel_name);
    print("Processing signal channel "+channel_name);
    title = getTitle();
    rename("original");
    imageCalculator("Multiply create 32-bit stack", "original", "ZRamp");
    rename("processing");
    if (mode=="mcfo") {
        performMasking();
    }
    performHistogramStretching();
    selectWindow("original");
    close();
    selectWindow("processing");
    rename(title);
}

function performMasking() {
    selectWindow("processing");
    run("Z Project...", "projection=[Max Intensity]");
    run("Select All");
    getStatistics(area, mean, min, max, std, histogram);
    close();
    MeanMaxRatio = mean / max;
    if (MeanMaxRatio>0.08) {
        run("Select All");
        run("Clear", "stack");
    }
}

function padImageDimensions(window_name) {
    selectWindow(window_name);
    getDimensions(width, height, channels, slices, frames);
    if (height % 2 != 0 || width % 2 != 0) {
        print("Adjusting canvas size for "+window_name);
        newWidth = width;
        newHeight = height;
        if (width % 2 != 0) {
            newWidth = width+1;
        }
        if (height % 2 != 0) {
            newHeight = height+1;
        }
        run("Canvas Size...", "width="+newWidth+" height="+newHeight+" position=Top-Center"); 
    }
}

function performHistogramStretching() {
    ImageProcessing = getImageID();
    getDimensions(width, height, channels, slices, frames);
    W = round(width/5);
    run("Z Project...", "projection=[Max Intensity]");
    run("Size...", "width=W height=W depth=1 constrain average interpolation=Bilinear");
    run("Select All");
    getStatistics(area, mean, min, max, std, histogram);
    close();
    selectImage(ImageProcessing);
    setMinAndMax(min, max);
    run("8-bit");
}
