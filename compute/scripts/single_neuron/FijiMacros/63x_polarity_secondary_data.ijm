// 63x_polarity_secondary_data.imj
// Revision level: 0.5
// Date released:  2014-10-05
// Description:
// Macro for generating MIP and movies from 63x case 3 polarity original lsm
// files or stitched file
// - adjust intensity
// - ramp signals in Z axis for neuron channels
// - mask presynaptic marker channel by membrane channel
// Input parameters:
//   prefix: title (usually the line name and tile)
//   basedir: base output directory
//   image: image path
//   channelspec: channel specification
//   colorspec: color specification


setBackgroundColor(0,0,0);
setForegroundColor(255,255,255);
setBatchMode(true);

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
if (image == "") exit("No image argument!");
channelspec = toLowerCase(arg[3]);
if (channelspec == "") exit ("No channel specification argument!");
colorspec = toUpperCase(arg[4]);
if (colorspec == "") exit ("No color specification argument!");

titleMIP = prefix + "_MIP";
titleAvi = prefix + ".avi";
titleSignalMIP = prefix + "-Signal_MIP";
titleSignalAvi = prefix + "-Signal.avi";

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
  run("Z Project...", "projection=[Max Intensity]");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  close();
  run("Subtract...", "value="+mean+" stack");
  
  run("Magenta");
  rename(title);
  // Process signal channel NeuronC2 (membrane)
  processChannel("signal2");
  run("Green");
  run("Z Project...", "projection=[Max Intensity]");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  close();
  Mean2Std = mean + 2*std;
  run("Duplicate...", "title=signal2_mask duplicate");
  setThreshold(Mean2Std, 255);
  run("Convert to Mask", "background=Dark black");
  run("Divide...", "value=255 stack");
  imageCalculator("Multiply stack", "signal1","signal2_mask");
  selectWindow("signal2_mask");
  close();
  selectWindow("signal2");
  run("Subtract...", "value="+mean+" stack");
}
else {
  processChannel("signal1");
  run("Green");
  run("Z Project...", "projection=[Max Intensity]");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  close();
  run("Subtract...", "value="+mean+" stack");

}

// Process reference channel
print("Processing reference channel");
selectWindow("reference");
title = getTitle();
performHistogramStretching();
run("Divide...", "value=2 stack");
run("8-bit");
rename(title);

// Merge, save MIP and AVI

print("Creating MIPs");

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
selectWindow("MAX_RGB");
saveAs("PNG",basedir+'/'+titleSignalMIP);
rename("MAX_RGB");
imageCalculator("Add", "MAX_RGB","STD_reference");
selectWindow("MAX_RGB");
saveAs("PNG",basedir+'/'+titleMIP);
close();
selectWindow("STD_reference");
close();

print("Creating movies");

selectWindow("RGB");
run("Duplicate...", "title=SignalMovie duplicate");
padImageDimensions("SignalMovie");
print("Saving Signal AVI");
run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleSignalAvi);
close();

selectWindow("reference");
run("RGB Color");
imageCalculator("Add create stack", "RGB","reference");
rename("FinalMovie");
selectWindow("RGB");
close();
selectWindow("reference");
close();
padImageDimensions("FinalMovie");
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
  rename(title);
}

function performHistogramStretching() {
  ImageProcessing = getImageID();
  getDimensions(width, height, channels, slices, frames);
  W = round(width/5);
  run("Z Project...", "projection=[Max Intensity]");
  run("Size...", "width="+W+" height="+W+" depth=1 constrain average interpolation=Bilinear");
  run("Select All");
  getStatistics(area, mean, min, max, std, histogram);
  close();
  selectImage (ImageProcessing);
  setMinAndMax(min, max);
  run("8-bit");
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

