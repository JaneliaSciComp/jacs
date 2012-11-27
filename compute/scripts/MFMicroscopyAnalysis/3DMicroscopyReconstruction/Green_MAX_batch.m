% program that reads a tiff stack and does max projection
clear all
disp('Please select the folder which ONLY contains the raw image stack which is needed to be processed!!')
folder_name = uigetdir
%folder_name = 'C:\Users\labadmin\Desktop\Analysis\Output\Reconstruction\PSF beads3 07-26 cut by 07-26 mat'
files=dir(fullfile(folder_name,'*.tif'));
outFolder= folder_name;
%h = waitbar(0,'Start...');
mkdir(outFolder,'MAX_projection')
for i=1:length(files)
    imFilename=[folder_name '\' files(i).name];

    outFilename=[outFolder,'\MAX_projection\',files(i).name(1:end-4)];
    
    info=imfinfo(imFilename);
    numFrames=length(info);

    for k=1:numFrames
            %A=imread('C:\Users\hajjb\Documents\Bassam folder\DATA_MULTI_FOCUS_MICROSCOPE\2011-11-04 NPC green and red\NPC-0.5um grating\MAX_3929green_Fri Nov 04 2011_11.41.02_002.tif');
            A=imread(imFilename,k);
            if k==1
            [m,n]=size(A);
            B=zeros(m,n,'uint16');
            end
            B=max(A,B);                
    end 
imwrite(B,[outFilename,'.tif']);

end
display('done')