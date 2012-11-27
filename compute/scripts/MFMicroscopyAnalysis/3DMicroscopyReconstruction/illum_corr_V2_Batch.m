%Programme written to correct illumation. Take a stack of 9 slices and then
%normalize the intensity of the other 8 slices to that of slice 5 according
%to correction factor obtained from the fluorescence beads experiment.
%Created by Jiji Chen in 2011 (Batch processing).

%% 
function illumcorrBatch(correctionfactorFile)
% User define the folder OR pick up folder.
h = waitbar(0,'Start...');
folder_name = uigetdir
%Look for subfolders. Be sure only to be processed images stored in the subfolder exist in the parentfolder.
B=dir(folder_name);
m=size(B,1);
%correctionfactorFile='D:\MF microscopy analysis\Example Files\Nikon 100Xcorrectionfactor.mat'
%read correction factors
load(correctionfactorFile,'correctionfactor');

for f=3:m
files=dir(fullfile(folder_name,'\',B(f).name,'\','*.tif'));
for i=1:length(files)
    %Debug
    filenames=[folder_name, '\',B(f).name,'\', files(i).name];
    info=imfinfo(filenames);

    %%
    %corr=[2.4771    2.0811    2.0579    2.0416    1.0000    2.2291    2.0734    2.5255    2.5956]; % Define the correction factor for each slice compared to slice 5.
    corr=1./correctionfactor;
    
    averreadnoise=[107.089,107.39,107.395,107.41,107.711,107.739,107.609,107.958,107.932];
    averagedark=[0,0,0,0,0,0,0,0,0];
    %averagedark=[98.463,	99.326	,99.417	,98.220	,98.993	,99.092	,98.317	,99.137	,99.248];
    
    %averreadnoise=[392.152428740091,431.399772305616,417.547436329904,386.313079777366,425.846601450498,412.345167819194,385.704376792039,425.610347444763,412.372828470231];This
    %value is for 1004x1002 pixel front illumanted camera.
    
    %averagedark=[307.299207286220,375.044737729803,354.492072862203,306.933757800641,374.976640242874,355.194299207286,309.555447798954,378.219345589475,358.764926631810];This
    %value is for 1004x1002 pixel front illumanted camera.
%%
    %parentFolder=[folder_name,'\',B(f).name];
    newfold=[B(f).name '_corrected'];
    newfold=[folder_name,'\',newfold];
    mkdir(newfold)
    writename=[newfold,'\',B(f).name,num2str(i-1,'%.4d') '.tif'];
    C(:,:,9)=zeros(info(1).Height,info(1).Width,'uint16');
    %outF=[outFilename num2str(ff-1,'%.4d') '.tif'];
    for k=1:length(info)
        if k==5
            C(:,:,k)=imread(filenames,k)-averreadnoise(5)-averagedark(5);
            imwrite(C(:,:,k),writename, 'Compression', 'none', 'WriteMode', 'append');
        else
            C(:,:,k)=(imread(filenames,k)-averreadnoise(k)-averagedark(k))*corr(k);
            imwrite(C(:,:,k),writename, 'Compression', 'none', 'WriteMode', 'append');
        end
    end
end
perc= (f-3+1)/(m-3+1)*100
waitbar(perc/100,h,sprintf('%3.2g%% along...',perc))
end
perc=100;
waitbar(perc,h,sprintf('Finish! Corrected images in the same folder',perc))
end