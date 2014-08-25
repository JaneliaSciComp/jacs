// Macro for generating MIP and movies from 63x MCFO two original lsm files for one tile.
// adjust intensity
// ramp signals in Z axis for neuron channels
// remove speckles based on size in 2D projection from two directions  
// 
// Title, reporter, tile, flp, and heat shock period need to be obtained from the database.

arg = split(getArgument(),",");
if (arg.length < 1) exit("Missing prefix argument");
prefix = arg[0];
if (arg.length < 2) exit("Missing destination directory");
basedir = arg[1] + '/';
print("Prefix: "+prefix);
print("Output directory: "+basedir);
if ((arg.length == 4) || (arg.length == 6)) {
  image1 = arg[2];
  channelspec1 = arg[3];
  print("Image 1: "+image1);
  print("Channel spec 1: "+channelspec1);
  openStack(image1,channelspec1);
  if (arg.length == 6) {
    image2 = arg[4];
    channelspec2 = arg[5];
    print("Image 2: "+image2);
    print("Channel spec 2: "+channelspec2);
    openStack(image2,channelspec2);
  }
}
else {
  exit("Missing image/channel spec");
}
title0 = prefix + "_MIP";
titleAvi = prefix + ".avi";

setBatchMode(true);
MinimalParticleSize = 50;
MaximalParticleSize = 500000;
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
    rename("1");
    selectImage(ImageB);
    rename("2");
  }
  else {
    rename("2");
    selectImage(ImageB);
    rename("1");
  }
  selectWindow("1");
  run("Split Channels");
  selectWindow("2");
  run("Split Channels");
  selectWindow("C1-1");
  close();
}
if (endsWith(TITLE, ".v3dpbd")) {
  getDimensions(width, height, channels, slices, frames);
  rename("original");
  run("Split Channels");
  selectWindow("C4-original");
  rename("C1-2");
  selectWindow("C1-original");
  rename("C2-2");
  selectWindow("C2-original");
  rename("C3-2");
  selectWindow("C3-original");
  rename("C2-1");   
}

// Z-intensity compesation to ramp singals in neuron channels
print("Performing Z compensation");
newImage("Ramp", "32-bit ramp", slices, width, height);
run("Add...", "value=1 stack");
run("Reslice [/]...", "output=1.000 start=Right avoid");
rename("ZRamp");
selectWindow("Ramp");
close();

// Process signal channels
processChannel("C2-1");
processChannel("C2-2");
processChannel("C3-2");

// Process reference channel
print("Processing reference channel");
selectWindow("C1-2");
title = getTitle();
rename("original");
imageCalculator("Multiply create 32-bit stack", "original","ZRamp");
selectWindow("original");
close();
selectWindow("Result of original");
rename("original");
run("Z Project...", "projection=[Max Intensity]");
run("Scale...", "x=0.1 y=0.1 width=102 height=102 interpolation=Bilinear average create title=01");
run("Select All");
getStatistics(area, mean, min, max, std, histogram);
close();
close();
selectWindow("original");
setMinAndMax(min, max);
run("Divide...", "value=1.5 stack");
run("8-bit");
rename(title);

// Merge, save MIP and AVI
print("Creating MIP");
selectWindow("ZRamp");
close();    
run("Merge Channels...", "c1=C2-2 c2=C3-2 c3=C2-1");
selectWindow("RGB");
run("Z Project...", "projection=[Max Intensity]");
selectWindow("C1-2");
run("Z Project...", "projection=[Standard Deviation]");
run("8-bit");
run("Divide...", "value=3");
run("RGB Color");
setBatchMode("exit & display");
imageCalculator("Add", "MAX_RGB","STD_C1-2");
saveAs("PNG",basedir+title0);
print("Creating movie");
selectWindow("C1-2");
run("RGB Color");
imageCalculator("Add create stack", "RGB","C1-2");
run("AVI... ", "compression=JPEG frame=20 save="+basedir+titleAvi);
run("Close All");
run("Quit");



function openStack(image,channelspec) {
  open(image);
}


function processChannel(channel_name) {
  selectWindow(channel_name);
  print("Processing signal channel "+channel_name);
  title = getTitle();
  rename("original");
  imageCalculator("Multiply create 32-bit stack", "original","ZRamp");
  selectWindow("original");
  close();
  selectWindow("Result of original");
  rename("original");
  run("Z Project...", "projection=[Max Intensity]");
  run("Scale...", "x=0.1 y=0.1 width=102 height=102 interpolation=Bilinear average create title=01");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  close();
  close();
  selectWindow("original");
  setMinAndMax(min, max);
  run("8-bit");

  run("Reslice [/]...", "output=0.380 start=Top avoid");
  run("Z Project...", "projection=[Max Intensity]");
  setAutoThreshold("Triangle dark");
  setOption("BlackBackground", true);
  performMasking();
  imageCalculator("Multiply create stack", "Reslice of original","Mask of MAX_Reslice of original");
  selectWindow("Mask of MAX_Reslice of original");
  close();
  selectWindow("MAX_Reslice of original");
  close();
  selectWindow("Reslice of original");
  close();
  selectWindow("original");
  close();
  selectWindow("Result of Reslice of original");

  run("Reslice [/]...", "output=0.188 start=Top avoid");
  selectWindow("Result of Reslice of original");
  close();
  selectWindow("Reslice of Result");
  run("Z Project...", "projection=[Max Intensity]");
  // run("Threshold...");
  setAutoThreshold("Triangle dark");
  performMasking();
  imageCalculator("Multiply stack", "Reslice of Result","Mask of MAX_Reslice of Result");
  selectWindow("MAX_Reslice of Result");
  close();
  selectWindow("Mask of MAX_Reslice of Result");
  close();
  rename(title);
}


function performMasking() {
  run("Convert to Mask");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  if (mean>MaximalSignalsOccupancy) {
    run("Select All");
    run("Clear", "slice");
  }             
  run("Analyze Particles...", "size=MinimalParticleSize-MaximalParticleSize pixel circularity=0.00-1.00 show=Masks clear");
  run("Divide...", "value=255.000");
}
