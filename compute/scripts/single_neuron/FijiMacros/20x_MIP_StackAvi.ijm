// Macro for generating MIP and 
// Images should be 1024x1024
// Title, laser intensity and gain need to be obtained from the database.

//setBatchMode(true);

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

    // bar dimensions·
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

argstr = getArgument();
args = split(argstr,",");

// Argument should be in this format:
// BrainPath,VNCPath,Laser,Gain,ChanSpec

// For example, to call this macro from the command line:
// /path/to/ImageJ -macro 20x_MIP_StackAvi.ijm file_brain.lsm,file_vnc.lsm,3,490,rs

brainImage = "";
vncImage = "";

if (lengthOf(args)>4) {
    brainImage = args[0];
    vncImage = args[1];
    laser = args[2];
    gain = args[3];
    chanspec = args[4];
}
else {
    brainImage = args[0];
    laser = args[1];
    gain = args[2];
    chanspec = args[3];
}

print("Brain: "+brainImage);
print("VNC: "+vncImage);
print("Laser: "+laser);
print("Gain: "+gain);
print("ChanSpec: "+chanspec);

title0 = "out";
titleBrain = title0+"_Brain_MIP";
titleBrainC2 = titleBrain+"_C2";
titleBrainAvi = title0+"_Brain.avi";
titleBrainAviC2 = title0+"_Brain_C2.avi";
titleVNC = title0+"_VNC_MIP";
titleVNCC2 = titleVNC+"_C2";
titleVNCAvi = title0+"_VNC.avi";
titleVNCAviC2 = title0+"_VNC_C2.avi";

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
run("Scale...", "x=0.1 y=0.1 width=102 height=102 interpolation=Bilinear average create title=01");
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
run("Scale...", "x=0.1 y=0.1 width=102 height=102 interpolation=Bilinear average create title=01");
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

run("AVI... ", "compression=JPEG frame=10 save="+titleBrainAvi);
Stack.setActiveChannels("10");
run("AVI... ", "compression=JPEG frame=10 save="+titleBrainAviC2);
Stack.setActiveChannels("11");
rename("Brain");

run("Z Project...", "projection=[Max Intensity]");
run("Split Channels");
selectWindow("C2-MAX_Brain");
run("Divide...", "value=2");
run("Merge Channels...", "c1=C1-MAX_Brain c2=C2-MAX_Brain create");

//saveAs("Tiff", titleBrain);
saveAs("Jpeg", titleBrain);
Stack.setActiveChannels("10");
//saveAs("Tiff", titleBrainC2);
saveAs("Jpeg", titleBrainC2);
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
    run("Scale...", "x=0.1 y=0.1 width=102 height=102 interpolation=Bilinear average create title=01");
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
    
    run("AVI... ", "compression=JPEG frame=10 save="+titleVNCAvi);
    Stack.setActiveChannels("10");
    run("AVI... ", "compression=JPEG frame=10 save="+titleVNCAviC2);
    Stack.setActiveChannels("11");
    rename("VNC");

    run("Z Project...", "projection=[Max Intensity]");
    run("Split Channels");
    selectWindow("C2-MAX_VNC");
    run("Divide...", "value=2");
    run("Merge Channels...", "c1=C1-MAX_VNC c2=C2-MAX_VNC create");

    //saveAs("Tiff", titleVNC);
    saveAs("Jpeg", titleVNC);
    Stack.setActiveChannels("10");
    //saveAs("Tiff", titleVNCC2);
    saveAs("Jpeg", titleVNCC2);
    close();
    close();
}

print("Done");
//setBatchMode("exit & display");
run("Quit");

