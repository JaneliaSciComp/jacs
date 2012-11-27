%% program that reads a tiff stack and does mean projection
%{
clear all
disp('Please select the folder which ONLY contains the raw image stack which is needed to be processed!!')
folder_name = uigetdir
%folder_name = 'C:\Users\labadmin\Desktop\Analysis\Output\Reconstruction\PSF beads3 07-26 cut by 07-26 mat'
files=dir(fullfile(folder_name,'*.tif'));
outFolder= folder_name;
%h = waitbar(0,'Start...');
mkdir(outFolder,'MEAN_projection')
for i=1:length(files)
    imFilename=[folder_name '\' files(i).name];
    outFilename=[outFolder,'\MEAN_projection\',files(i).name(1:end-4)];
    info=imfinfo(imFilename);
    numFrames=length(info);
    B=imread(imFilename,1);
    for k=2:numFrames
            A=imread(imFilename,k);
            B=A+B;                
    end 
    B=round(B/numFrames);
imwrite(B,[outFilename,'.tif']);

end
display('done')
%}
%% Second version Reads stacks that are already reconstructed
% program that reads a tiff stack and does max projection
clear all
%{
disp('Please select the folder which contains the reconstructed stacks ')
folder_name = uigetdir
%}
folder_name='C:\Users\hajjb\Documents\Bassam folder\DATA_MULTI_FOCUS_MICROSCOPE\2011-11-17 NPC with membrane proteine green and red\GREEN- Atto POM SCALED';
B=dir(folder_name);
m=size(B,1);
outFolder= folder_name;
mkdir(outFolder,'MEAN_projection')

for f=3:m
files=dir(fullfile(folder_name,'\',B(f).name,'\','*.tif'));
%outFilename=[outFolder,'\MEAN_projection\',files(1).name(1:end-4)];
%h = waitbar(0,'Start...');
    info=imfinfo([folder_name, '\',B(f).name,'\' ,files(1).name]);
    numFrames=length(info);%number of stacks
    w=info.Width;
    h=info.Height;
    
    C=zeros(w,h,numFrames,'uint16');
    x=0;
    y=0;
for i=1:length(files)
    imFilename=[folder_name, '\',B(f).name,'\' ,files(i).name];
    A=zeros(w,h,numFrames,'uint16');
    for k=1:numFrames
        if i==1
          C(:,:,k)=imread(imFilename,k);
          y=y+1;
        else
            A(:,:,k)=imread(imFilename,k);
            y=y+1;
        end                            
    end 
    C=A+C;
    
end
C=round(C/length(files));

for k=1:numFrames
    %imwrite(B,[outFilename,'.tif']);
    imwrite(C(:,:,k),[outFolder,'\MEAN_projection\MEAN',B(f).name(end-3:end),'.tif'], 'Compression', 'none', 'WriteMode', 'append');
end

end
display('done')