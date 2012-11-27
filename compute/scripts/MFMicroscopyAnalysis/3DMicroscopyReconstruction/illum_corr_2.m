%Programme written to correct illumation. 

%% 
function illumcorrV2(reconstructedFolderPath, correctionfactorFile, averageSettingFile)
% User define the folder OR pick up folder.
folder_name=reconstructedFolderPath;
%folder_name = 'C:\Users\chenj12\Desktop\Analysis2\Output\06 28\009'
files=dir(fullfile(folder_name,'*.tif'));
% correctionfactorFile='D:\Analysis\3D tracking for Consortium meeting\psfbeads for calibration\Nikon 100X objective-Olumpus tube lense -0.25um stackflipped.tifpos-int-cor .mat'
load(correctionfactorFile,'correctionfactor');
mkdir(folder_name,'corrected')
%filenames={files.name};
for i=1:length(files)
    filenames=[folder_name '/' files(i).name];
    info=imfinfo(filenames);
    
    %%
    %corr=[2.4771    2.0811    2.0579    2.0416    1.0000    2.2291    2.0734    2.5255    2.5956]; % Define the correction factor for each slice compared to slice 5.
     corr=1./correctionfactor;
     
    %for the Back illuminated
%     averreadnoise=[107.089,107.39,107.395,107.41,107.711,107.739,107.609,107.958,107.932];
%     averagedark=[50,50,50,50,50,50,50,50,50];
    [averreadnoise, averagedark] = textread(averageSettingFile, '%f, %f');
    
    %averreadnoise=averreadnoise-averagedark;
    
    % for the CMOS
    %averreadnoise=[400 400 400 400 400 400 400 400 400];
    %averagedark=[0 0 0 0 0 0 0 0 0];
    
    %%
    writename=[folder_name '/corrected/',files(1).name(1:end-8),num2str(i-1,'%.4d') '.tif'];
    %writename=['C:/Users/chenj12/Desktop' '/corrected' num2str(i-1,'%.4d') '.tif']
    C(:,:,9)=zeros(info(1).Height,info(1).Width,'uint16');
    %outF=[outFilename num2str(ff-1,'%.4d') '.tif'];
    for k=1:length(info)
        if k==5
            C(:,:,k)=abs(imread(filenames,k)-averreadnoise(5)-averagedark(5));
            imwrite(C(:,:,5),writename, 'Compression', 'none', 'WriteMode', 'append');
        else
            C(:,:,k)=abs((imread(filenames,k)-averreadnoise(k)-averagedark(k))*corr(k));
            imwrite(C(:,:,k),writename, 'Compression', 'none', 'WriteMode', 'append');
        end
    end
end
disp('Done')
end
