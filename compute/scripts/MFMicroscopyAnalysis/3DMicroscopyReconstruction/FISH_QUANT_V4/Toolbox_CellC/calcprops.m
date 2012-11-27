function result=calcprops(labeled_total,total,resize_factor,do_ratio,ratio,spec);
%CALCPROPS   Calculate properties of objects in two labeled images.
%   result=calcprops(labeled_total,total,resize_factor,do_ratio,ratio,spec)
%
%   In:
%   labeled_total, is a labeled cell image for total object count. total is
%   the same image but in grayscale (for intensity calculations).
%   resize_factor, the factor how much the image was resized before
%   processing. This allows scaling of results to match the original size.
%   do_ratio, binary value to specify whether scaling from pixels to micrometers 
%   is needed 
%   ratio, scaling ratio between pixels and micrometers
%   spec, (OPTIONAL) labeled image of specifically stained cells if analysis of 
%   specifically stained image is needed
%   
%   Out:
%   result, a cell array of headings and data values calculated from
%           labeled_total and total input images
%
%   User defined parameters can be edited by editing the source code to 
%   calibrate the analysis procedure.
%
%   See also CELLC

% Copyright (C) Jyrki Selinummi, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% calculate areas and solidities of total
properties=regionprops(labeled_total,'Area','Solidity');
areas=[properties.Area];
solidities=[properties.Solidity];
% cancel the effect of resizing
areas=areas*((1/resize_factor)^2);

% scale to micrometers if needed
if do_ratio
    areas=areas.*(ratio.^2);
end


% Cell# is saved to the first column of result matrix.
result=(1:length(areas))';
global nos;
nos=length(result);

isfound=zeros(max(max(labeled_total)),1);


if nargin>5
    % prepare for specific count, binarize first
    spec=spec>0;
end

% prepare for calculation of intensities
if (size(total,3))>1
    % input image to grayscale
    gray_total = rgb2gray(total);
else 
    gray_total=total;
end
gray_total=double(gray_total);


for iter=1:max(max(labeled_total))
    % create a mask
    slab=ismember(labeled_total,iter);
    % check which objects exist in both of the input images,
    % if the object is also found in the "spec" image...
    if nargin>5
        if sum(sum(slab&spec))~=0
            isfound(iter)=1;
        end
    end
    
    % perimeter calculation
    perimeter=bwperim(slab,4);
    perimlengths(iter)=sum(sum(perimeter));
    
    % calculate intensity values
    one_object=gray_total(slab);
    mean_intensities(iter)=mean(one_object);
    max_intensities(iter)=max(one_object);
    
    % object compactness
    compactness(iter)=(4*pi*areas(iter))/(perimlengths(iter)^2);
end

% specific-stain results to the result matrix
if nargin>5
    result(:,size(result,2)+1) = isfound';
    global inspecific;
    inspecific=sum(isfound);
end

% Properties of the cells can be added to following columns of "result"
result(:,size(result,2)+1) = areas';

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% calculate some other properties of total
% See:
% Bloem J., Veninga M., Shepherd J., 1995. Fully Automatic
% Determination of Soil Bacterium Numbers, Cell Volumes, and 
% Frequencies of Dividing Cells by Confocal Laser Scanning Microscopy and 
% Image Analysis. Appl. Environ. Microbiol. 21, 579-587.

% cancel the effect of resizing
perimlengths=perimlengths*(1/resize_factor);
% to micrometers..
if do_ratio
    perimlengths=perimlengths.*ratio;
end

% the part from the equation that is inside the square root
inroot=perimlengths.^2-16.*areas;
% if the value inside the root is below zero, length and width is
% calculated using a circle of equal area
for iter=1:length(inroot)
    if inroot(iter)<0
        % equivalent circle diameter
        leng(iter)=perimlengths(iter)/pi;
        widt(iter)=perimlengths(iter)/pi;
    else
        % the default case
        leng(iter)=(perimlengths(iter)+sqrt(inroot(iter)))/4;
        widt(iter)=(perimlengths(iter)-sqrt(inroot(iter)))/4;
    end
   
end

% volume
volum=(pi/4).*widt.^2.*(leng-widt/3);

% to columns of result matrix...
result(:,size(result,2)+1) = volum';
result(:,size(result,2)+1) = leng';
result(:,size(result,2)+1) = widt';
result(:,size(result,2)+1) = mean_intensities';
result(:,size(result,2)+1) = max_intensities';
result(:,size(result,2)+1) = solidities';
result(:,size(result,2)+1) = compactness';


% No rounding. This causes Excel to present some of the results in "date"
% format, but that can be easily repaired using "format cells" tool in Excel.
%result=round(result);

% headings of result columns
%
% NOTE that headings of summary-file columns are given in file
% saveresults.m, this is because saveresults collects all the data gathered
% while batch processing
if nargin>5
    columnnames={'Cell´s serial number' 'Exists in Figure2 (YES=1, NO=0)' 'Area of cell' 'Approximate volume'...
        'Length' 'Width' 'Intensity mean' 'Intensity maximum' 'Solidity' 'Compactness'};
else
    columnnames={'Cell´s serial number' 'Area of cell' 'Approximate volume'...
        'Length' 'Width' 'Intensity mean' 'Intensity maximum' 'Solidity' 'Compactness'};
end
% means of values
if nargin>5
    meanvalues=[0 0 mean(areas) mean(volum) mean(leng) mean(widt)...
        mean(mean_intensities) mean(max_intensities) mean(solidities) mean(compactness)];
else
    meanvalues=[0 mean(areas) mean(volum) mean(leng) mean(widt)...
        mean(mean_intensities) mean(max_intensities) mean(solidities) mean(compactness)];
end

% No rounding. This causes Excel to present some of the results in "date"
% format, but that can be easily repaired using "format cells" tool in
% Excel.
%meanvalues=round(meanvalues);


% append all results into a cell array, then combine these arrays into a
% one large cell array of cell arrays
% This array is then sent to saveresults.m that parses the array, and saves
% the data to disk

data={columnnames result};
means={{'Statistical means of columns'} meanvalues};
result={data means};

