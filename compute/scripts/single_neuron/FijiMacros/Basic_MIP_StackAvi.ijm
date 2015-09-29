// Date released:  2014-10-01
// FIJI macro for generating intensity-normalized Movies and MIPs
//
// Argument should be in this format: "Basedir,BrainPrefix,VNCPrefix,BrainPath,VNCPath,Laser,Gain,ChanSpec,ColorSpec,DivSpec,Outputs"
// 
// Input parameters:
//     Basedir: output directory for MIPs and movies
//     BrainPrefix: output filename prefix for the brain input
//     VNCPrefix: output filename prefix for the VNC input
//     BrainPath: absolute path to the Brain stack 
//     VNCPath: absolute path to the VNC stack (may be blank if there is no VNC)
//     Laser: laser power (BC1) for the signal channel (optional annotation)
//     Gain: detector gain for the signal channel (optional annotation)
//     ChannelSpec: image channel specification
//     ColorSpec: color specification [(R)ed, (G)reen, (B)lue, grey(1), (C)yan, (M)agenta, (Y)ellow]
//     DivSpec: divisor for each channel (optional)
//     Outputs: colon-delimited list of outputs to generate ["mips","movies","legends"], e.g. "mips:legends"
//
// Calling this macro from the command line looks something like this:
//     /path/to/ImageJ -macro 20x_MIP_StackAvi.ijm out,/output/dir,file_brain.lsm,file_vnc.lsm,rs,3,490
//

// Global variables

var numChannels;
var numOutputChannels;
var reverseMapping;
var mipFormat = "PNG";
var outputs = "mips:movies"

// Initialization

setBackgroundColor(0,0,0);
setForegroundColor(255,255,255);
setBatchMode(true);

// Arguments

args = split(getArgument(),",");
basedir = args[0];
brainPrefix = args[1];
vncPrefix = args[2];
brainImage = args[3];
vncImage = args[4];
laser = args[5];
gain = args[6];
chanspec = toLowerCase(args[7]);
colorspec = toUpperCase(args[8]);
divspec = args[9];
if (args.length > 10) {
    outputs = toLowerCase(args[10]);
}

numChannels = lengthOf(chanspec);

if (divspec == "") {
    // Default divisor is for ref channel only
    for (i=0; i<numChannels; i++) {
        cc = substring(chanspec,i,i+1);
        if (cc == 'r') {
            divspec = divspec + "2";
        }
        else {
            divspec = divspec + "1";
        }
    }
}

print("Output dir: "+basedir);
print("Output brain prefix: "+brainPrefix);
print("Output VNC prefix: "+vncPrefix);
print("Brain image: "+brainImage);
print("VNC image: "+vncImage);
print("Laser power: "+laser);
print("Detector gain: "+gain);
print("Channel spec: "+chanspec);
print("Color spec: "+colorspec);
print("Divisor spec: "+divspec);
print("Outputs: "+outputs);

createMIPS = false;
createMovies = false;
displayLegends = false;
if (outputs!="") {
    if (matches(outputs,".*mips.*")) {
        createMIPS = true;
    }
    if (matches(outputs,".*movies.*")) {
        createMovies = true;
    }
    if (matches(outputs,".*legends.*")) {
        displayLegends = true;
    }
}

if (!createMIPS && !createMovies) {
    print("No outputs selected, exiting.");
    run("Quit");
}

// Open input files

openChannels(brainImage, "Brain");
if (vncImage!="") {
    openChannels(vncImage, "VNC");
}

// Figure out how to map the channels in the final image

brainChannelMapping = getChannelMapping("Brain");
vncChannelMapping = getChannelMapping("VNC");
print("Brain channel mapping: "+brainChannelMapping);
print("VNC channel mapping: "+vncChannelMapping);

var allChannels = "";
var signalChannels = "";
var refChannels = "";
for (j=0; j<numOutputChannels; j++) {
    i = reverseMapping[j];
    cc = substring(chanspec,i,i+1);
    print("reverse mapped "+j+" -> "+i+ " ("+cc+")");
    allChannels += "1";
    if (cc == 'r') {
        signalChannels += "0";
        refChannels += "1";
    }
    else {
        signalChannels += "1";
        refChannels += "0";
    }
}

print("All channels: "+allChannels);
print("Signal channels: "+signalChannels);
print("Reference channels: "+refChannels);

// Array of the max values for each Brain channel
brainMax=newArray(numChannels);

for (i=0; i<numChannels; i++) {
    
    // Process Brain channel, save its intensity
    bname = "C" + (i+1) + "-Brain";
    selectWindow(bname);
    print("Processing "+bname);
    brainMinMax = performHistogramStretching();
    minBrain = brainMinMax[0];
    maxBrain = brainMinMax[1];
    brainMax[i] = maxBrain;
    
    // Process VNC channel
    if (vncImage!="") {
        vname = "C" + (i+1) + "-VNC";
        selectWindow(vname);
        print("Processing "+vname);
        
        cc = substring(chanspec,i,i+1);
        if (cc == 'r') {
            performHistogramStretching();
        }
        else {
            // Normalize VNC signal channel to corresponding Brain signal channel
            if (bitDepth==16) {
                setMinAndMax(minBrain, maxBrain);
                run("8-bit");
            } 
            else {
                scalar = 255/maxBrain;
                run("Multiply...", "value=scalar stack");
            }
        }
    }
}

saveMipsAndMovies("Brain", brainPrefix, brainMax, brainChannelMapping);
if (vncImage!="") {
    saveMipsAndMovies("VNC", vncPrefix, brainMax, vncChannelMapping);
}

print("Done");
run("Close All");
run("Quit");


// Open the given image and give it a name. Then split into individual channels. 
function openChannels(image,name) {
    open(image);
    print(image+" "+chanspec+" "+colorspec);
    getDimensions(width, height, channels, slices, frames);
    if (channels!=numChannels) {
        print("Image "+name+" has incorrect number of channels: "+channels+" (Expected "+numChannels+")");
    }
    print(width + "x" + height + "  Slices: " + slices + "  Channels: " + channels);
    print("Splitting and renaming channels");
    if (channels==1) {
        rename("C1-"+name);
    }
    else {
        rename(name);
        run("Split Channels");
    }
}

// Create a channel mapping string for the image with the given name. 
// Also set the global signalChannels variable with a signal channel bitmask. 
function getChannelMapping(name) {
    var merge_name = "";
    var targets = newArray(numChannels);
    numOutputChannels = 0;
    
    for (i=0; i<numChannels; i++) {
        cname = "C" + (i+1) + "-" + name;
        cc = substring(chanspec,i,i+1);
        col = substring(colorspec,i,i+1);
        
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
        
        targets[i] = targetChannel;
        if (targetChannel > 0) {
            merge_name += "c" + targetChannel + "=" + cname + " ";
            numOutputChannels++;
        }
    }
    
    // Compute reverse mapping
    var ordered = Array.copy(targets);
    Array.sort(ordered);
        
    reverseMapping = newArray(numOutputChannels);
    j = 0;
    for (ij=0; ij<numChannels; ij++) {
        if (ordered[ij]>0) {
            for (i=0; i<numChannels; i++) {
                if (ordered[ij]==targets[i]) {
                    reverseMapping[j] = i;
                }
            }   
            j++;
        }
    }
    
    return merge_name;
}

// Save MIPs and movies for the image with the given name. The max intensity value of the each 
// channel should be provided as an array in parameter "maxValues".  
function saveMipsAndMovies(name, prefix, maxValues, merge_name) {
    
    if (displayLegends) {
        si = 0;
        for (j=0; j<numOutputChannels; j++) {
            i = reverseMapping[j];
            cc = substring(chanspec,i,i+1);
            wname = "C" + (i+1) + "-"+name;
            selectWindow(wname);
            if (cc != 'r') {
                drawLegend(0, maxValues[i], laser, gain, 3, si++);
            }
        }
    }
    
    run("Merge Channels...", merge_name+" create ignore");
    rename(name);
    
    if (createMIPS) {
        print("Creating MIPs for "+name);
        titleMIP = prefix + "_all";
        titleSignalMIP = prefix + "_signal";
        titleRefMIP = prefix + "_reference";
        
        run("Z Project...", "projection=[Max Intensity]");
        run("Duplicate...", "title=mip duplicate");
        
        if (refChannels=='1') {
            // Just save the single reference channel
            saveAs(mipFormat, basedir+'/'+titleRefMIP);
            run("Divide...", "value="+parseInt(divspec));
            saveAs(mipFormat, basedir+'/'+titleMIP);
        }
        else {
            // Must process multiple channels
            Stack.setActiveChannels(refChannels);
            saveAs(mipFormat, basedir+'/'+titleRefMIP);
            Stack.setActiveChannels(signalChannels);
            saveAs(mipFormat, basedir+'/'+titleSignalMIP);
            close();
            Stack.setActiveChannels(allChannels);

            // Divide channels and re-merge 
            run("Split Channels");
        
            merge_name = "";
            for (j=0; j<numOutputChannels; j++) {
                c = j+1;
                wname = "C" + c + "-MAX_"+name;
                i = reverseMapping[j];
                divisor = 1;
                if (i < lengthOf(divspec)) {
                    divisor =  substring(divspec,i,i+1);
                }
                if (divisor != '1') {
                    print("Applying divisor "+divisor+" to channel "+c);
                    selectWindow(wname);
                    run("Divide...", "value="+parseInt(divisor));
                }
                merge_name += "c" + c + "=" + wname + " ";
            }
            run("Merge Channels...", merge_name+" create");
            saveAs(mipFormat, basedir+'/'+titleMIP);
        }
        
        close();
    }
    
    if (createMovies) {
        print("Creating movies for "+name);
        titleAvi = prefix + "_all.avi";
        titleSignalAvi = prefix + "_signal.avi";
        titleRefAvi = prefix + "_reference.avi";
        
        padImageDimensions(name);
        
        if (refChannels=='1') {
            run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleAvi);
            run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleRefAvi);
        }
        else {
            run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleAvi);
            Stack.setActiveChannels(signalChannels);
            run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleSignalAvi);
            Stack.setActiveChannels(refChannels);
            run("AVI... ", "compression=Uncompressed frame=20 save="+basedir+'/'+titleRefAvi);
        }
    }
        
    close();
}

// Create a gradient bar with 3 labels (min/mid/max)
function gradientBar(bar_x, bar_y, bar_h, bar_w, min, max, font_size) {
    step = 255/bar_h;
    for(i=0; i<bar_h; i++) {
        makeRectangle(bar_x, bar_y+i, bar_w, 1);
        c = 255-i*step;
        setForegroundColor(c,c,c);
        run("Fill","stack");
    }
    c = 255;
    setForegroundColor(c,c,c);
    tx = bar_x+bar_w+5;
    ty = bar_y;
    setFont("SansSerif",font_size,"antialiased");
    makeText(max, tx, ty);
    run("Draw", "stack");
    makeText(round((max-min)/2), tx, (ty+bar_h/2)-(font_size/2));
    run("Draw", "stack");
    makeText(min, tx, (ty+bar_h)-(font_size));
    run("Draw", "stack");
}

// Position is closewise from upper right: 1=upper right, 2=upper left, 3=lower right, 4=lower left
// Index is the current number of legends on the image. For example, when drawing the second legend, set index=1. 
function drawLegend(min_intensity, max_intensity, laser, gain, position, index) {

    annotation_y_offset=30;
    getDimensions(width,height,channels,slices,frames);

    // distance from edge of image
    margin = 5;

    // bar dimensions
    bar_height = 64;
    bar_width = 10;

    // approximate legend dimensions
    lw = 60;
    lh = bar_height+40;

    if (position==1) {
        x = margin + index * (lw + margin);
        y = margin;
    }
    else if (position==2) {
        x = width - margin - lw - index * (lw + margin);
        y = margin;
    }
    else if (position==3) {
        x = width - margin - lw - index * (lw + margin);
        y = height - margin - lh;
    }
    else if (position==4) {
        x = margin + index * (lw + margin);
        y = height - margin - lh;
    }

    gradientBar(x,y,bar_height,bar_width,min_intensity,max_intensity,12);

    text = "";
    if (laser!="") {
        text = text+"Laser: "+laser;
    }
    if (gain!="") {
        text = text+"\nGain: "+gain;
    }
    
    if (text!="") {
        setFont("SansSerif",12,"antialiased");
        makeText(text, x, y+bar_height+10);
        run("Draw", "stack");
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
    run("Size...", "width="+W+" height="+W+" depth=1 constrain average interpolation=Bilinear");
    run("Select All");
    getStatistics(area, mean, min, max, std, histogram);
    close();
    selectImage(ImageProcessing);
    minMax=newArray(2);
    minMax[0] = min;
    minMax[1] = max;
    if(bitDepth==16){
        setMinAndMax(min, max);
        run("8-bit");
    } 
    else {
        scalar = 255/max;
        run("Multiply...", "value=scalar stack");
    }
    return minMax;
}
