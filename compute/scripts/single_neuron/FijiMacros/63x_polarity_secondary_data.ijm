// 63x_polarity_secondary_data.imj
// Revision level: 0.5
// Date released:  2014-10-01
// Description:
// Macro for generating MIP and movies from 63x case 3 polarity original lsm
// files or stitched file
// - adjust intensity
// - ramp signals in Z axis for neuron channels
// - remove speckles based on size in 2D projection from three directions  
// - mask presynaptic marker channel by membrane channel
// Input parameters:
//   prefix: title (usually the line name and tile)
//   basedir: base output directory
//   image: image path
//   channelspec: channel specification
//   colorspec: color specification


run("Colors...", "foreground=white background=black selection=yellow");
setBatchMode(true);
MinimalParticleSize = 1000;
MaximalSignalsOccupancy = 40;

var width, height, channels, slices, frames;
var merge_name = "";
var signal_channels = newArray();

arg = split(getArgument(),",");
if (arg.length < 1) exit("Missing prefix argument");
prefix = arg[0];
if (arg.length < 2) exit("Missing destination directory");
basedir = arg[1];
print("Prefix: "+prefix);
print("Output directory: "+basedir);
image = arg[2];
if (image == "") exit ("No image argument!");
channelspec = toLowerCase(arg[3]);
if (channelspec == "") exit ("No channel specification argument!");
colorspec = toUpperCase(arg[4]);
if (colorspec == "") exit ("No color specification argument!");
title0 = prefix + "_MIP";
titleAvi = prefix + ".avi";
openStack();

// Z-intensity compesation to ramp signals in neuron channels
print("Preparing for Z compensation");
newImage("Ramp", "32-bit ramp", slices, width, height);
run("Add...", "value=1 stack");
run("Reslice [/]...", "output=1.000 start=Right avoid");
rename("ZRamp");
selectWindow("Ramp");
close();

if (channels > 2) {
  // Process signal channel NeuronC1 (presynaptic)
  print("Processing presynaptic channel");
  selectWindow("signal1");
  title = getTitle();
  rename("original");
  imageCalculator("Multiply create 32-bit stack", "original","ZRamp");
  rename("processing");
  performHistogramStretching();
  selectWindow("original");
  close();
  selectWindow("processing");
  run("Magenta");
  run("Z Project...", "projection=[Max Intensity]");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  close();
  selectWindow("processing");
  run("Subtract...", "value=Mean stack");
  rename(title);
  // Process signal channel NeuronC2 (membrane)
  processChannel("signal2");
  run("Green");
  run("Z Project...", "projection=[Max Intensity]");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  close();
  MeanPlusHalfStd = mean + 0.5*std;
  run("Subtract...", "value=MeanPlusHalfStd stack");
  run("Duplicate...", "title=signal2_mask duplicate");
  setAutoThreshold("Default dark stack");
  run("Convert to Mask", "method=Default background=Dark black");
  run("Divide...", "value=255 stack");
  imageCalculator("Multiply stack", "signal1","signal2_mask");
  selectWindow("signal2_mask");
  close();
}
else {
  processChannel("signal1");
}

// Process reference channel
print("Processing reference channel");
selectWindow("reference");
title = getTitle();
performHistogramStretching();
run("Divide...", "value=1.5 stack");
run("8-bit");
rename(title);

// Merge, save MIP and AVI
print("Creating MIP");
selectWindow("ZRamp");
close();    
run("Merge Channels...", merge_name+" ignore");
getDimensions(width, height, channels, slices, frames);
if (channels == 2) {
  // Sometimes the merge creates a composite image
  run("RGB Color", "slices");
  rename("RGB");
}
selectWindow("RGB");
run("Z Project...", "projection=[Max Intensity]");
selectWindow("reference");
run("Z Project...", "projection=[Standard Deviation]");
run("8-bit");
run("Divide...", "value=3");
run("RGB Color");
setBatchMode("exit & display");
imageCalculator("Add", "MAX_RGB","STD_reference");
selectWindow("MAX_RGB");
saveAs("PNG",basedir+'/'+title0);
close();
selectWindow("STD_reference");
close();
print("Creating movie");
selectWindow("reference");
run("RGB Color");
imageCalculator("Add create stack", "RGB","reference");
rename("FinalMovie");
selectWindow("RGB");
close();
selectWindow("reference");
close();
selectWindow("FinalMovie");
getDimensions(width, height, channels, slices, frames);
if (height % 2 != 0 || width % 2 != 0) {
    print("Adjusting canvas size");
    newWidth = width;
    newHeight = height;
    if (width % 2 != 0) {
        newWidth = width+1;
    }
    if (height % 2 != 0) {
        newHeight = height+1;
    }
    run("Canvas Size...", "width=&newWidth height=&newHeight position=Top-Center");
}
print("Saving AVI");
run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleAvi);
run("Close All");
run("Quit");


function openStack() {
  //Open one lsm file (3 channels) or a stitched v3dpbd (3 channels)
  open(image);
  print(image+" "+channelspec+" "+colorspec);
  TITLE = getTitle();
  getDimensions(width, height, channels, slices, frames);
  print(width + "x" + height + "  Slices: " + slices + "  Channels: " + channels);
  print("Splitting and renaming channels");
  rename("original");
  run("Split Channels");
  signal_count = 0;
  for (i=0; i<lengthOf(channelspec); i++) {
    wname = "C" + (i+1) + "-original";
    selectWindow(wname);
    cc = substring(channelspec,i,i+1);
    col = substring(colorspec,i,i+1);
    if (cc == 'r') {
      rename('reference');
      print(" Renamed " + wname + " to reference");
    }
    else {
      signal_count++;
      cname = 'signal'+signal_count;
      rename(cname);
      print(" Renamed " + wname + " to " + cname);
      signal_channels = Array.concat(signal_channels,cname);
      if (col == 'R') {
        merge_name = merge_name + "c1=" + cname + " ";
      }
      if (col == 'G') {
        merge_name = merge_name + "c2=" + cname + " ";
      }
      if (col == 'B') {
        merge_name = merge_name + "c3=" + cname + " ";
      }
      if (col == '1') {
        merge_name = merge_name + "c4=" + cname + " ";
      }
      if (col == 'C') {
        merge_name = merge_name + "c5=" + cname + " ";
      }
      if (col == 'M') {
        merge_name = merge_name + "c6=" + cname + " ";
      }
      if (col == 'Y') {
        merge_name = merge_name + "c7=" + cname + " ";
      }
    }
  }
}


function processChannel(channel_name) {
  selectWindow(channel_name);
  print("Processing signal channel "+channel_name);
  title = getTitle();
  rename("original");
  imageCalculator("Multiply create 32-bit stack", "original","ZRamp");
  rename("processing");
  performHistogramStretching();
  selectWindow("original");
  close();
  selectWindow("processing");
  performMasking();
  run("Reslice [/]...", "output=0.380 start=Top avoid");
  selectWindow("processing");
  close();
  selectWindow("Reslice of processing");
  rename("processing");
  performMasking();
  run("Reslice [/]...", "output=0.188 start=Top avoid");
  selectWindow("processing");
  close();
  selectWindow("Reslice of processing");
  rename("processing");
  selectWindow("processing");
  run("Reslice [/]...", "output=1.000 start=Right rotate avoid");
  selectWindow("processing");
  close();
  selectWindow("Reslice of processing");
  rename("processing");
  performMasking();
  run("Reslice [/]...", "output=1.000 start=Left flip rotate avoid");
  selectWindow("processing");
  close();
  selectWindow("Reslice of processing");
  rename(title);
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
  selectImage (ImageProcessing);
  setMinAndMax(min, max);
  run("8-bit");
}


function performMasking() {
  selectWindow("processing");
  run("Z Project...", "projection=[Max Intensity]");
  rename("MIP1");
  run("Enhance Local Contrast (CLAHE)", "blocksize=50 histogram=256 maximum=2 mask=*None*");
  run("Duplicate...", "title=MIP2");
  selectWindow("MIP1"); 
  run("Duplicate...", "title=MIP3");
  selectWindow("MIP1"); 
  run("Duplicate...", "title=MIP4");
  selectWindow("MIP1"); 
  run("Duplicate...", "title=MIP5");
  selectWindow("MIP1"); 
  run("Duplicate...", "title=MIP6");
  selectWindow("MIP1"); 
  run("Duplicate...", "title=MIP7");
  selectWindow("MIP1"); 
  run("Duplicate...", "title=MIP8");
  selectWindow("MIP1");
  setAutoThreshold("MaxEntropy dark");
  setOption("BlackBackground", true);
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
        run("Select All");
        run("Clear", "slice");
    }
  selectWindow("MIP2");          
  setAutoThreshold("Default dark");
  setOption("BlackBackground", true);
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
        run("Select All");
        run("Clear", "slice");
    }
  selectWindow("MIP3");          
  setAutoThreshold("Otsu dark");
  setOption("BlackBackground", true);
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
        run("Select All");
        run("Clear", "slice");
    }
  selectWindow("MIP4");         
  setAutoThreshold("Moments dark");
  setOption("BlackBackground", true);
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
        run("Select All");
        run("Clear", "slice");
    }
  selectWindow("MIP5");         
  setAutoThreshold("Li dark");
  setOption("BlackBackground", true);
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
        run("Select All");
        run("Clear", "slice");
    }
  selectWindow("MIP6");         
  setAutoThreshold("Yen dark");
  setOption("BlackBackground", true);
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
        run("Select All");
        run("Clear", "slice");
    }
  selectWindow("MIP7");         
  setAutoThreshold("Triangle dark");
  setOption("BlackBackground", true);
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
        run("Select All");
        run("Clear", "slice");
    }
  selectWindow("MIP8");         
  setAutoThreshold("Huang dark");
  setOption("BlackBackground", true);
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
        run("Select All");
        run("Clear", "slice");
    }
  imageCalculator("Max create", "MIP1","MIP2");
  rename("MIP12"); 
  selectWindow("MIP1");
  close();
  selectWindow("MIP2");
  close();
  imageCalculator("Max create", "MIP3","MIP4");
  rename("MIP34"); 
  selectWindow("MIP3");
  close();
  selectWindow("MIP4");
  close();
  imageCalculator("Max create", "MIP5","MIP6");
  rename("MIP56"); 
  selectWindow("MIP5");
  close();
  selectWindow("MIP6");
  close();
  imageCalculator("Max create", "MIP7","MIP8");
  rename("MIP78"); 
  selectWindow("MIP7");
  close();
  selectWindow("MIP8");
  close();
  imageCalculator("Max create", "MIP12","MIP34");
  rename("MIP1234"); 
  selectWindow("MIP12");
  close();
  selectWindow("MIP34");
  close();
  imageCalculator("Max create", "MIP56","MIP78");
  rename("MIP5678"); 
  selectWindow("MIP56");
  close();
  selectWindow("MIP78");
  close();
  imageCalculator("Max create", "MIP1234","MIP5678");
  rename("MIP"); 
  selectWindow("MIP1234");
  close();
  selectWindow("MIP5678");
  close();
  selectWindow("MIP");  
  run("Dilate");
  run("Dilate");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>85) {
        run("Select All");
        run("Clear", "slice");
    }
  run("Analyze Particles...", "size=MinimalParticleSize-Infinity pixel circularity=0.00-1.00 show=Masks clear");
  run("Divide...", "value=255.000");
  rename("mask");
  imageCalculator("Multiply create stack", "processing","mask");
  selectWindow("mask");
  close();
  selectWindow("MIP");
  close();
  selectWindow("processing");
  close();
  selectWindow("Result of processing");
  rename("processing");
}
