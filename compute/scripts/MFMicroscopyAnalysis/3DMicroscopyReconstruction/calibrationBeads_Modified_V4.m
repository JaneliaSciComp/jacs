%{
INPUT:

pathIm: path where calibration series is stored

basenameIm: name of the tif file (WITHOUT .tif EXTENSION) that contains the focal stack fro calibration

OUTPUT:

[pathIm basenameIm.mat] file containing teh necessary transformations to align each 2D image of raw data from the microscope
%}

%function calibrationBeads(pathIm,basenameIm)

% % %debugging-------------------------------------
% % pathIm='/Users/amatf/Tracking-SaraAbrahamsson/data/Fernando_RedAndGreenBeads/'
% % basenameIm='FourBeadFocalSeries_20100611T131018'
% % %----------------------------------------------



%%
%read image stack for calibration
clear variables

%pathIm='/Users/labadmin/Desktop/analysis/'
%basenameIm='PSF beads3 07_26_11'
%filenameIm=[pathIm basenameIm '.tif'];

channels={'red','green'};%Note: the first channel should be the reference channel

for numChannels=1:length(channels)
    
    if numChannels==1
        [FileName,PathName] = uigetfile('*.tif','Select the Beads STACK file for red channel');
    else 
        [FileName,PathName] = uigetfile('*.tif','Select the Beads STACK file for green channel');
    end
    filenameIm=[PathName,FileName];
    info=imfinfo(filenameIm);
    
    stack2D=zeros(info(1).Height,info(1).Width);
    hh=info(1).Height;
    ww=info(1).Width;
    hh=ceil(hh/3);
    ww=ceil(ww/3);
    
    for kk=1:length(info)
        imAux=double(imread(filenameIm,'Index',kk));
        %if(strcmp(channels{numChannels},'green'))
         %   imAux=fliplr(imAux);
        %end
        stack2D=max(stack2D,imAux);
    end
    %stack2D=stack2D/length(info);
    
    
    
    %%
    %sigma=2;
    %Gaussian smoothing
    %h=fspecial('Gaussian',4*sigma,sigma);
    %stack2Dsmooth=imfilter(stack2D,h,'same');
    stack2Dsmooth=stack2D;

    %% Median filter
    %stack2Dsmooth=medfilt2(stack2D);
    %imagesc(stack2Dsmooth);
    %%
    %stack2Dsmooth=stack2Dsmooth';
    %region with average intensity greater than the threshold
    figure
  
     imagesc(stack2Dsmooth);
    if(numChannels==1)
        disp('Please pick up the beads in the central part') 
        coordinate0 = ginput();
        coordinatesbis =floor(coordinate0);
        [m n]= size(coordinate0);
        numberofbeadsOrig=m;
    else
        disp(['Please pick up the same ' num2str(numberofbeadsOrig) ' beads in the center region']);
        coordinatesbis = ginput(numberofbeadsOrig);
        coordinatesbis=floor(coordinatesbis);
    end

    numberofbeads=9*numberofbeadsOrig;
    
    %coordinates1=[coordinatesbis;coordinatesbis(:,1)+hh+1 coordinatesbis(:,2);coordinatesbis(:,1)+2*hh+1 coordinatesbis(:,2)];
    %coordinates2=[coordinatesbis(:,1) coordinatesbis(:,2)+ww+1;coordinatesbis(:,1)+hh+1 coordinatesbis(:,2)+ww+1;coordinatesbis(:,1)+2*hh+1 coordinatesbis(:,2)+ww+1];
    %coordinates3=[coordinatesbis(:,1) coordinatesbis(:,2)+2*ww+1;coordinatesbis(:,1)+hh+1 coordinatesbis(:,2)+2*ww+1;coordinatesbis(:,1)+2*hh+1 coordinatesbis(:,2)+2*ww+1];
    %coordinates=[coordinates1;coordinates2;coordinates3];

    if numChannels==2
        co1=-2;
        co2=-4;
    else
        co1=4;%correction factor1. For unbinned data, recommend 6.
        co2=2;%correction factor2. For unbinned data, recommend 3.
    end
    coordinates1=[coordinatesbis(:,1)-hh-co1 coordinatesbis(:,2)-ww-1-co2;coordinatesbis(:,1) coordinatesbis(:,2)-ww-1-co2;coordinatesbis(:,1)+hh+co1 coordinatesbis(:,2)-ww-1-co2];
    coordinates2=[coordinatesbis(:,1)-hh-co1 coordinatesbis(:,2);coordinatesbis;coordinatesbis(:,1)+hh+co1 coordinatesbis(:,2)];
    coordinates3=[coordinatesbis(:,1)-hh-co1 coordinatesbis(:,2)+ww+1+co2;coordinatesbis(:,1) coordinatesbis(:,2)+ww+1+co2;coordinatesbis(:,1)+hh+co1 coordinatesbis(:,2)+ww+1+co2];
    coordinates=[coordinates1;coordinates2;coordinates3];
    
    figure
    imagesc(stack2D);
    hold on
    scatter(coordinates(:,1),coordinates(:,2),'r');
    
    
    
    
    
    %debug
    %numberofbeads=9;
    %coordinates=[93,84;264,84;434,84;92,254;262,253;432,255;90,425;260,426;431,425;];
    aa=8; %half of the width of the region around the spot. For unbinned data, recommend 10.
    [x y]=meshgrid(0:2*aa,0:2*aa);
    fitparams=zeros(numberofbeads,5);
    for i=1:numberofbeads
        tt=[coordinates(i,1)-aa:coordinates(i,1)+aa;coordinates(i,2)-aa:coordinates(i,2)+aa];
        patch2=stack2Dsmooth(coordinates(i,2)-aa:coordinates(i,2)+aa,coordinates(i,1)-aa:coordinates(i,1)+aa);
        
        [r c] =find(patch2==max(patch2(:))); %finds the maximum value in the matrix
        coordinates(i,1)= max(c)+coordinates(i,1)-aa;
        coordinates(i,2)= max(r)+coordinates(i,2)-aa;
        patch2=stack2Dsmooth(coordinates(i,2)-aa:coordinates(i,2)+aa,coordinates(i,1)-aa:coordinates(i,1)+aa);
        %patch2=stack2Dsmooth(r+coordinates(i,2)-2*aa:r+coordinates(i,2), c+coordinates(i,1)-2*aa:c+coordinates(i,1));%recentring the matrix arround the maximum
        
        meanbackground=mean([mean(patch2(1,:)) mean(patch2(2*aa+1,:)) mean(patch2(:,1)) mean(patch2(:,2*aa+1))]);
        patch2=patch2-meanbackground;
        %define the Gaussian 2D function
        myfun = @(A) A(1)*exp(-(((x(:)-A(2)).^2/(2*A(3)^2))+((y(:)-A(4)).^2/(2*A(5)^2))))-patch2(:);
        % Initial guess parameters
        A0=[1000;aa;1;aa;1];
        %Add lots of debugging info
        %opts = optimset('Display','Iter');
        options = optimset('Algorithm',{'levenberg-marquardt',0.005});
        fitparams(i,:) = (lsqnonlin(myfun,A0,[],[],options))';
    end
    fitparams(:,2)=fitparams(:,2)+coordinates(:,1)-aa;
    fitparams(:,4)=fitparams(:,4)+coordinates(:,2)-aa;
    
    %statsBW=regionprops(BW,'EquivDiameter','Centroid'); %Find the center position and the diameter for the PSF beads
    
    
    %debugging
    %figure;subplot(2,1,1);imagesc(stack2D);subplot(2,1,2);imagesc(BW);
    figure;imagesc(stack2Dsmooth);
    hold on
    scatter(fitparams(:,2),fitparams(:,4),'y','filled');
    scatter(coordinates(:,1),coordinates(:,2),'r')
    hold off
    
    
    
    %%
    %detect peaks with subpixel accuracy
    
    
    
    peaks=zeros(numberofbeads,2);
    peaks(:,1)= fitparams(:,4);
    peaks(:,2)= fitparams(:,2);
    
    
    % % % %debugging
    % % % figure;imagesc(stack2D);colormap gray
    % % % hold on;plot(peaks(:,2),peaks(:,1),'go')
    % % % %to access elements stack2D(round(peaks(kk,1)),round(peaks(kk,2)))
    
    %%
    %decompose peaks in 9 different slices
    xx=ceil(linspace(1,size(stack2D,1),4));
    yy=ceil(linspace(1,size(stack2D,2),4));% change the 1 to value served as offset to cut in the image
    
    
    numBeadsPerSlice=numberofbeads/9;
    peaksPerSlice=zeros(numBeadsPerSlice,2,9);%contains the (x,y) coordinate of each peak with respect to corner reference (xMin,yMin)
    cornerPerSlice=zeros(9,4);%contains the bottom left corner (xMin,yMin) for each slice
    for ii=1:length(xx)-1
        for jj=1:length(yy)-1
            pos=find(peaks(:,1)>=xx(ii) & peaks(:,1)<xx(ii+1) & peaks(:,2)>=yy(jj) & peaks(:,2)<yy(jj+1));
            if(length(pos)~=numBeadsPerSlice)
                error(['Slice ' num2str((ii-1)*3+jj) ' does not contain the right number of beads']);
            end
            peaksPerSlice(:,:,(ii-1)*3+jj)=peaks(pos,:)-repmat([xx(ii) yy(jj)],[numBeadsPerSlice 1]);
            %sort points based on distance to origin to make sure the
            %correspondence is correct
            [ss pp]=sortrows(peaksPerSlice(:,:,(ii-1)*3+jj));
            peaksPerSlice(:,:,(ii-1)*3+jj)=peaksPerSlice(pp,:,(ii-1)*3+jj);
            cornerPerSlice((ii-1)*3+jj,:)=[xx(ii) yy(jj) xx(ii+1) yy(jj+1)];
        end
    end
    
    % % % %
    % % % % %debugging
    % % % % figure
    % % % % zz=lines(9);
    % % % % for kk=1:9
    % % % % hold on;plot(peaksPerSlice(:,1,kk),peaksPerSlice(:,2,kk),'+','Color',zz(kk,:));
    % % % % end
    % % % % %--------------------
    
    %%
    %align all the slices using teh Generalized Procrustes Algorithm (fancy
    %term for an iterative method that finds translation, rotation and scaling)
    %http://en.wikipedia.org/wiki/Procrustes_distance
    tolD=1e-3;    
    if(numChannels==1)%calculate reference iteratively
        %find mean location of points
        Pref=mean(peaksPerSlice,3);
        
        dTotal=1e6;
        dTotalOld=dTotal*10;
        
        iter=0;
        while(abs(dTotalOld-dTotal)>tolD && iter<1000)
            dTotalOld=dTotal;
            %calculate transformation for each slice
            dTotal=0;
            PrefAux=0;
            for kk=1:9
                [d, Z] = procrustes(Pref,peaksPerSlice(:,:,kk),'Reflection',false);
                dTotal=dTotal+sum(sqrt(sum((Pref-Z).^2,2)));
                PrefAux=PrefAux+Z;
            end
            Pref=PrefAux/9;
            dTotal=dTotal/(9*numBeadsPerSlice);
            %disp(['Iter=' num2str(iter) '.Average distance is ' num2str(dTotal) ])
            iter=iter+1;
        end
        disp(['Number of iteration is ' num2str(iter) ])
        disp(['Average distance is ' num2str(dTotal) ])
    end
    
    %calculate transformation for each frame
    %for channels>1 Pref is the set of points found in channel 1
    transformPerSlice=cell(9,1);
    for kk=1:9
        [d, Z, tr] = procrustes(Pref,peaksPerSlice(:,:,kk),'Reflection',false);
        transformPerSlice{kk}=tr;
    end
    
    
    %debugging: check that everything aligns perfectly
    figure;
    zz=lines(9);
    plot(Pref(:,2),Pref(:,1),'ko');
    for kk=1:9
        pp=transformPerSlice{kk}.b * peaksPerSlice(:,:,kk) * transformPerSlice{kk}.T + transformPerSlice{kk}.c;
        hold on;plot(pp(:,2),pp(:,1),'+','Color',zz(kk,:));
    end
    hold off;
    title 'All crosses should superimpose one on top of the other around the black circle'
    %--------------------
    
    %%
    %saves transformations
    %disp(['Saving transformations at ' [pathIm basenameIm '.mat']])
    %save([pathIm basenameIm '.mat'],'transformPerSlice','cornerPerSlice','peaksPerSlice');
    
    disp(['Saving transformations at ' [PathName FileName(1:end-4) '_' channels{numChannels} '.mat']])
    save([PathName FileName(1:end-4) '_' channels{numChannels} '.mat'],'transformPerSlice','cornerPerSlice','peaksPerSlice');
    
end