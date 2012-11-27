%{
INPUT:

imFilename: filename (with complete path) of the 2D image containing the raw data raw data

outFilename: filename (with complete path) where the 3D stack consisting of 9 slices will be saved

calibrationFile: mat file generated by the routine calibrationBeads.m containing the needed transformations


Transformations are applied as follows:

transformPerSlice{kk}.b * XY * transformPerSlice{kk}.T + transformPerSlice{kk}.c

where XY are the image coordinates


OUTPUT:

stack: 3D stack obtained from 2D raw data and saved at outFilename
%}
%%
clear all
%[FileName,PathName]=uigetfile('*.mat', 'Select the transformation matrix for red channel');
%calibrationFile1=[PathName,FileName];
calibrationFile1='C:\Users\hajjb\Documents\Bassam folder\DATA_MULTI_FOCUS_MICROSCOPE\2011-11-17 NPC with membrane proteine green and red\PSF\PSF1 RED\beads_Thu Nov 17 2011_13.48.15_011.mat';
%[FileName,PathName]=uigetfile('*.mat', 'Select the transformation matrix for green channel');
%calibrationFile2=[PathName,FileName];
calibrationFile2='C:\Users\hajjb\Documents\Bassam folder\DATA_MULTI_FOCUS_MICROSCOPE\2011-11-17 NPC with membrane proteine green and red\PSF\PSF1 GREEN\PSF1 GREEN STACK .mat';
h = waitbar(0,'Initializing waitbar...');
%we took red channel as a reference: 1 corresponds to red and 2 to green

for channel=1:2

    if channel==1
        [FileName,PathName]=uigetfile('*.tif', 'pick the red raw data')
    else 
        [FileName,PathName]=uigetfile('*.tif', 'pick the green raw data')
    end


%read calibration parameters
if channel==1
    load(calibrationFile1,'transformPerSlice','cornerPerSlice');
else 
    load(calibrationFile2,'transformPerSlice','cornerPerSlice');
end

imFilename=[PathName,FileName];
mkdir(PathName,'Reconstructed');
outFilename=[PathName,'\Reconstructed\',FileName(1:end-4)];%write without tif extension




%%
%apply transformations to each image

info=imfinfo(imFilename);
numFrames=length(info);

%-3 is to make sure it fits and they all have the same size
stack=zeros(cornerPerSlice(1,3)-cornerPerSlice(1,1)-3,cornerPerSlice(1,4)-cornerPerSlice(1,2)-3,9,'uint16');
mask=true(size(stack,1),size(stack,2));


for ff=1:numFrames
    im=imread(imFilename,'Index',ff);
    for kk=1:9
        patch=im(cornerPerSlice(kk,1):cornerPerSlice(kk,1)+size(stack,1)-1,cornerPerSlice(kk,2):cornerPerSlice(kk,2)+size(stack,2)-1);
        
        [X Y]=meshgrid(0:size(patch,1)-1,0:size(patch,2)-1);%to match peak coordinates from calibrationBEads.m routine
        X=X';
        Y=Y';
        %apply transformation to meshgrid
        %aux=transformPerSlice{kk}.b * [X(:) Y(:)] * transformPerSlice{kk}.T + repmat(transformPerSlice{kk}.c(1,:),[size(patch,1)*size(patch,2) 1]);
        aux= ([X(:) Y(:)]-repmat(transformPerSlice{kk}.c(1,:),[size(patch,1)*size(patch,2) 1])) * transformPerSlice{kk}.T'/transformPerSlice{kk}.b;
        
        XI=reshape(aux(:,1),size(X));
        YI=reshape(aux(:,2),size(Y));
        %interpolate images
        stack(:,:,kk) = uint16(interp2(Y,X,double(patch),YI,XI,'*cubic'));
        
        if(ff==1)
            %find maximum region without zero-border filling
            maskPos=find(XI<cornerPerSlice(1,2) | YI<cornerPerSlice(1,1) | XI>size(patch,1) | YI>size(patch,2));
            mask(maskPos)=false;
        end
        
    end
    perc=75;
    waitbar(perc/100,h,sprintf('%d%% along...',perc))
    if(ff==1 && channel==1)
        %find cropping area to make sure we do not include zero-filling after
        %aligning the images
        border=1;
        while(sum(mask(border:end-border+1,border:end-border+1)==false)>0)
            border=border+1;
        end
    end
    
    outF=[outFilename num2str(ff-1,'%.4d') '.tif'];
    imwrite(stack(border:end-border+1,border:end-border+1, 1), outF, 'Compression', 'none', 'WriteMode', 'overwrite');
    for kk=2:size(stack,3)
        imwrite(stack(border:end-border+1,border:end-border+1, kk), outF, 'Compression', 'none', 'WriteMode', 'append');
    end
end
end
%%
perc=100;
waitbar(perc/100,h,sprintf('Finish. Images in the ''Reconstructed'' subfolder',perc))
disp(['Cropped ' num2str(border) ' pixels to avoid zero-filling after alignment'])
