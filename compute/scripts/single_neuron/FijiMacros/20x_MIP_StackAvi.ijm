//Date released:  2014-10-01
// FIJI macro for generating intensity-normalized Movies and MIPs
// Images should be 1024x1024
// Argument should be in this format: "BrainPath,VNCPath,Laser,Gain,ChanSpec"
// The VNCPath is optional.
//
// To call this macro from the command line:
// /path/to/ImageJ -macro 20x_MIP_StackAvi.ijm out,file_brain.lsm,file_vnc.lsm,3,490,rs
//

mipFormat = "PNG"; // possible values: Jpeg, PNG, Tiff

setBatchMode(true);
argstr = getArgument();
args = split(argstr,",");

brainImage = "";
vncImage = "";
if (lengthOf(args)>5) {
    prefix = args[0];
    brainImage = args[1];
    vncImage = args[2];
    laser = args[3];
    gain = args[4];
    chanspec = args[5];
}
else {
    prefix = args[0];
    brainImage = args[1];
    laser = args[2];
    gain = args[3];
    chanspec = args[4];
}

print("Prefix: "+prefix);
print("Brain: "+brainImage);
print("VNC: "+vncImage);
print("Laser: "+laser);
print("Gain: "+gain);
print("ChanSpec: "+chanspec);

title0 = prefix;
titleBrain = title0+"-Brain_MIP";
titleBrainC2 = titleBrain+"-Signal";
titleBrainAvi = title0+"-Brain.avi";
titleBrainAviC2 = title0+"-Brain-Signal.avi";
titleVNC = title0+"-VNC_MIP";
titleVNCC2 = titleVNC+"-Signal";
titleVNCAvi = title0+"-VNC.avi";
titleVNCAviC2 = title0+"-VNC-Signal.avi";

open(brainImage);
brainId = getImageID();
print("Brain Id: "+brainId);
if (vncImage!="") {
    open(vncImage);
    vncId = getImageID();
    print("VNC Id: "+vncId);
}

function flipChannels(imageId) {
    selectImage(imageId);
    title = getTitle();
    run("Split Channels");
    run("Merge Channels...", "c1=C2-"+title+" c2=C1-"+title+" create");
    return getImageID();
}

if (chanspec=="sr") {
    // Switch the channels so that the reference channel comes first
    print("Flipping channels on Brain");
    brainId = flipChannels(brainId);
    if (vncImage!="") {
        print("Flipping channels on VNC");
        vncId = flipChannels(vncId);
    }
}

selectImage(brainId);
rename("Brain");
if (vncImage!="") {
    selectImage(vncId);
    rename("VNC");
}

// Histogram stretching by using histogram of downscaled images
// Save avi and MIP

selectWindow("Brain");
run("Split Channels");
selectWindow("C1-Brain");
run("Z Project...", "projection=[Max Intensity]");
run("Scale...", "x=0.2 y=0.2 width=204 height=204 interpolation=Bilinear average create title=01");
run("Select All");
getStatistics(area, mean, min, max, std, histogram);

brainMin = min;
brainMax = max;

close();
close();
selectWindow("C1-Brain");

if(bitDepth==16){
    MaxBrainC1 = max;
    setMinAndMax(min, max);
    run("8-bit");
} else {
    MaxBrainC1 = 255/max;
    run("Multiply...", "value=MaxBrainC1 stack");
}


selectWindow("C2-Brain");
run("Z Project...", "projection=[Max Intensity]");
run("Scale...", "x=0.2 y=0.2 width=204 height=204 interpolation=Bilinear average create title=01");
run("Select All");
getStatistics(area, mean, min, max, std, histogram);
close();
close();

selectWindow("C2-Brain");

if(bitDepth ==16 ){
    MaxBrainC2 = max;
    setMinAndMax(min, max);
    run("8-bit");
} else {
    MaxBrainC2 = 255/max;
    run("Multiply...", "value=MaxBrainC2 stack");
}

selectWindow("C1-Brain");
run("Magenta");
selectWindow("C2-Brain");
run("Green");

drawLegend(0, MaxBrainC2, laser, gain, 3);

run("Merge Channels...", "c1=C2-Brain c2=C1-Brain create");

getDimensions(width, height, channels, slices, frames);
if (height % 2 != 0 || width % 2 != 0) {
    print("Adjusting Brain canvas size");
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

run("AVI... ", "compression=Uncompressed frame=20 save="+titleBrainAvi);
Stack.setActiveChannels("10");
run("AVI... ", "compression=Uncompressed frame=20 save="+titleBrainAviC2);
Stack.setActiveChannels("11");
rename("Brain");

run("Z Project...", "projection=[Max Intensity]");
run("Split Channels");
selectWindow("C2-MAX_Brain");
run("Divide...", "value=2");
run("Merge Channels...", "c1=C1-MAX_Brain c2=C2-MAX_Brain create");

saveAs(mipFormat, titleBrain);
Stack.setActiveChannels("10");
saveAs(mipFormat, titleBrainC2);
close();
close();

if(vncImage!="") {

    selectWindow("VNC");
    run("Split Channels");
    selectWindow("C2-VNC");

    if(bitDepth==16 ){
        setMinAndMax(min, max);
        run("8-bit");
    } else {
        run("Multiply...", "value=MaxBrainC2 stack");
    }

    selectWindow("C1-VNC");
    run("Z Project...", "projection=[Max Intensity]");
    run("Scale...", "x=0.2 y=0.2 width=204 height=204 interpolation=Bilinear average create title=01");
    run("Select All");
    getStatistics(area, mean, min, max, std, histogram);
    close();
    close();

    selectWindow("C1-VNC");

    if(bitDepth==16 ){
        MaxVNCC1 = max;
        setMinAndMax(min, max);
        run("8-bit");
    } else {
        MaxVNCC1 = 255/max;
        run("Multiply...", "value=MaxVNCC1 stack");
    }

    selectWindow("C1-VNC");
    run("Magenta");
    selectWindow("C2-VNC");
    run("Green");

    drawLegend(0, MaxBrainC2, laser, gain, 3);

    run("Merge Channels...", "c1=C2-VNC c2=C1-VNC create");
    
    getDimensions(width, height, channels, slices, frames);
    if (height % 2 != 0 || width % 2 != 0) {
        print("Adjusting VNC canvas size");
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

    run("AVI... ", "compression=Uncompressed frame=20 save="+titleVNCAvi);
    Stack.setActiveChannels("10");
    run("AVI... ", "compression=Uncompressed frame=20 save="+titleVNCAviC2);
    Stack.setActiveChannels("11");
    rename("VNC");

    run("Z Project...", "projection=[Max Intensity]");
    run("Split Channels");
    selectWindow("C2-MAX_VNC");
    run("Divide...", "value=2");
    run("Merge Channels...", "c1=C1-MAX_VNC c2=C2-MAX_VNC create");

    saveAs(mipFormat, titleVNC);
    Stack.setActiveChannels("10");
    saveAs(mipFormat, titleVNCC2);
    close();
    close();
}

print("Done");
//setBatchMode("exit & display");
run("Quit");


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

// position is closewise from upper right: 1=upper right, 2=upper left, 3=lower right, 4=lower left
function drawLegend(min_intensity, max_intensity, laser, gain, position) {

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
        x = margin;
        y = margin;
    }
    else if (position==2) {
        x = width-margin-lw;
        y = margin;
    }
    else if (position==3) {
        x = width-margin-lw;
        y = height-margin-lh;
    }
    else if (position==4) {
        x = margin;
        y = height-margin-lh;
    }

    gradientBar(x,y,bar_height,bar_width,min_intensity,max_intensity,12);

    // Annotations
    setFont("SansSerif",12,"antialiased");
    makeText("Laser: "+laser+"\nGain: "+gain, x, y+bar_height+10);
    run("Draw", "stack");

    // Debug outline
    //makeRectangle(x, y, lw, lh);
    //run("Draw","stack");
}
