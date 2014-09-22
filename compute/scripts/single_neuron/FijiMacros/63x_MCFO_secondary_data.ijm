// 63x_mcfo_secondary_data.imj
// Revision level: 0.1
// Date released:  2014-09-12
// Description:
// Macro for generating MIP and movies from 63x MCFO two original lsm files for one tile.
// adjust intensity
// ramp signals in Z axis for neuron channels
// remove speckles based on size in 2D projection from two directions  
// 
// Four to six command-line parameters are expected:
//   prefix: title (usually the line name and tile)
//   basedir: output directory for MIP and movie
//   image1: 1st image
//   channelspec1: 1st image channel specification
//   colorspec1: 1st image color specification
//   image2: 2nd image (optional if 1st image is a .pbd)
//   channelspec2: 2nd image channel specification (optional if 1st image is a .pbd)
//   colorspec2: 2nd image color specification


var channelspec1,channelspec2,channelspec;
var colorspec1,colorspec2;
arg = split(getArgument(),",");
if (arg.length < 1) exit("Missing prefix argument");
prefix = arg[0];
if (arg.length < 2) exit("Missing destination directory");
basedir = arg[1];
print("Prefix: "+prefix);
print("Output directory: "+basedir);
if ((arg.length == 5) || (arg.length == 8)) {
  image1 = arg[2];
  channelspec1 = arg[3];
  colorspec1 = arg[4];
  openStack(image1,channelspec1);
  if (arg.length == 8) {
    image2 = arg[5];
    channelspec2 = arg[6];
    colorspec2 = arg[7];
    openStack(image2,channelspec2);
  }
}
else {
  exit("Missing image/channel spec");
}
title0 = prefix + "_MIP";
titleAvi = prefix + ".avi";

run("Colors...", "foreground=white background=black selection=yellow");
setBatchMode(true);
MinimalParticleSize = 2500;
MaximalSignalsOccupancy = 128;

//Open two lsm  files (2 channels and 3 channels) or a stitched v3dpbd (4 channels)
TITLE = getTitle();
if(nImages==2) {
  ImageA = getImageID();
  run("Put Behind [tab]");
  ImageB = getImageID();
  selectImage(ImageA);
  getDimensions(width, height, channels, slices, frames);
  if(channels==2) {
    channelspec = channelspec2;
    rename("1");
    selectImage(ImageB);
    rename("2");
  }
  else {
    channelspec = channelspec1;
    rename("2");
    selectImage(ImageB);
    rename("1");
  }
  // First image (2 channel)
  selectWindow("1");
  run("Split Channels");
  selectWindow("2");
  run("Split Channels");
  selectWindow("C1-2");
  rename("reference");
  selectWindow("C2-2");
  rename("NeuronC1");
  selectWindow("C3-2");
  rename("NeuronC2");
  if (channelspec == 'rs') {
    selectWindow("C1-1");
    close();
    selectWindow("C2-1");
    rename("NeuronC3");
  }
  else {
    selectWindow("C2-1");
    close();
    selectWindow("C1-1");
    rename("NeuronC3");
  }
}
else {
  getDimensions(width, height, channels, slices, frames);
  rename("original");
  run("Split Channels");
  selectWindow("C1-original");
  rename("NeuronC1");
  selectWindow("C2-original");
  rename("NeuronC2");
  selectWindow("C3-original");
  rename("NeuronC3");   
  selectWindow("C4-original");
  rename("reference");
}

// Z-intensity compesation to ramp singals in neuron channels
print("Preparing for Z compensation");
newImage("Ramp", "32-bit ramp", slices, width, height);
run("Add...", "value=1 stack");
run("Reslice [/]...", "output=1.000 start=Right avoid");
rename("ZRamp");
selectWindow("Ramp");
close();

// Process signal channels
processChannel("NeuronC1");
processChannel("NeuronC2");
processChannel("NeuronC3");

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
run("Merge Channels...", "c1=NeuronC1 c2=NeuronC2 c3=NeuronC3");
selectWindow("RGB");
run("Z Project...", "projection=[Max Intensity]");
selectWindow("reference");
run("Z Project...", "projection=[Standard Deviation]");
run("8-bit");
run("Divide...", "value=3");
run("RGB Color");
setBatchMode("exit & display");
imageCalculator("Add", "MAX_RGB","STD_reference");
saveAs("PNG",basedir+'/'+title0);
print("Creating movie");
selectWindow("reference");
run("RGB Color");
imageCalculator("Add create stack", "RGB","reference");
selectWindow("RGB");
getDimensions(width, height, channels, slices, frames);
if (height % 2 != 0) {
    newHeight = height+1;
    run("Canvas Size...", "width=&width height=&newHeight position=Top-Center");
}
run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleAvi);
run("Close All");
run("Quit");


function openStack(image,chanspec) {
  open(image);
  print(image+" "+chanspec);
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
  rename("processing");
  
  rename(title);
}


function performHistogramStretching() {
  ImageProcessing = getImageID();
  getDimensions(width, height, channels, slices, frames);
  W = round(width/10);
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
  rename("MIP");
  setAutoThreshold("Triangle dark");
  setOption("BlackBackground", true);
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
    selectWindow("MIP");
    close();
    selectWindow("processing");
    run("Z Project...", "projection=[Max Intensity]");
    rename("MIP");
    setAutoThreshold("Li dark");
    setOption("BlackBackground", true);
    run("Convert to Mask");
    run("Select All");
    getStatistics(area, mean, min, max, std, histogram);
    if (mean>MaximalSignalsOccupancy) {
        run("Select All");
        run("Clear", "slice");
    }
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
