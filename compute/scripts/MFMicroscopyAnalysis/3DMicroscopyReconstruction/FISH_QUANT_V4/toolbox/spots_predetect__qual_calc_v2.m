function  [spots_detected sub_spots sub_spots_filt] = spots_predetect__qual_calc_v2(image_struct,spots_detected,options,flag_struct)
%
%
% spots_detected(:,1:3) ... yxz position of detected local maximum in image
% spots_detected(:,4:9) ... xyz: min and max of subregion for fitting
%
% spots_detected(:,10)   ... Quality score;
% spots_detected(:,11)   ... Quality score normalized with maximum
% spots_detected(:,12)   ... Thresholded or not (based on quality score) 
%
% v4 Consider size of stack
%
% v3, May
% - Consider case when no spots are detected
%
% v2 April 14, 2010
% - Move open and close Matlabpool for parallel computing out of function. This way
%   pool will only be openend once when called in a loop - saves time!
%
% v1 March 28, 2010
% - Original implementation


% Number of spots
N_Spots        = size(spots_detected,1);    % Number of candidates 

%= Extract all options
size_detect      = options.size_detect;
PSF              = options.PSF;

%= Some parameters
image          = image_struct.data;
image_filt     = image_struct.data_filtered;
quality_score  = ones(N_Spots,1);
sub_spots      = cell(N_Spots,1);
sub_spots_filt = cell(N_Spots,1);  

%= Dimensions of image
[dim.Y dim.X dim.Z] = size(image);

%= Loop over spots (if any are detected)
if N_Spots

    %===  Extract immediate environment for each spot in 3d
    disp('... sub-spot mosaicing...');    

    for i = 1:N_Spots    
        
        y_min = spots_detected(i,1)-size_detect.xy;
        y_max = spots_detected(i,1)+size_detect.xy;
        
        x_min = spots_detected(i,2)-size_detect.xy;
        x_max = spots_detected(i,2)+size_detect.xy;
        
        z_min = spots_detected(i,3)-size_detect.z;
        z_max = spots_detected(i,3)+size_detect.z;
        
        if z_min < 1;     z_min = 1;     end
        if z_max > dim.Z; z_max = dim.Z; end        
              
        %- For raw data
        sub_spots{i} = double(image(y_min:y_max,x_min:x_max,z_min:z_max));

        %- For filtered data                        
        sub_spots_filt{i} = double(image_filt(y_min:y_max,x_min:x_max,z_min:z_max));  
        
        y_min_spots(i) = y_min;
        y_max_spots(i) = y_max;     
        x_min_spots(i) = x_min;  
        x_max_spots(i) = x_max;
        z_min_spots(i) = z_min;
        z_max_spots(i) = z_max;
    end
    
    % ==== Score computation for each spot - parallel loop
    %      Scores are calculated either
    %      - Based on the curvuture of the curve (smallest eigenvalues of Hessian matrix) 
    %          ??? Herve mentioned that they are multiplied by the maximum intensity of the region - can't find this. 
    %      - Based on standard deviation of spot 


    disp('... Score Computation...');


    if strcmp(flag_struct.score,'Curvature')

        if(flag_struct.parallel)
            parfor k = 1:N_Spots
                quality_score(k) = - min(eig(hessian_finite_differences_v1(sub_spots_filt{k},round(PSF.xy_pix+1),round(PSF.z_pix))));
            end
        else
            for k = 1:N_Spots
                quality_score(k) = - min(eig(hessian_finite_differences_v1(sub_spots_filt{k},round(PSF.xy_pix+1),round(PSF.z_pix))));
            end       
        end

    elseif strcmp(flag_struct.score,'Standard deviation')

        if(flag_struct.parallel)
            parfor k = 1:N_Spots
                quality_score(k) = std(sub_spots_filt{k}(:));
            end
        else
            for k = 1:N_Spots
                quality_score(k) = std(sub_spots_filt{k}(:));
            end
        end
    end

    %= Metric for tresholding    
    quality_score_norm = (quality_score')/max(quality_score);  % Relative score based on curvature - normalized with max score

    %==== Prepare matrix to store results of geometric thresholding  
    spots_detected(:,4)   = y_min_spots;
    spots_detected(:,5)   = y_max_spots;
    spots_detected(:,6)   = x_min_spots;             %- Thresholded or not (based on quality score)
    spots_detected(:,7)   = x_max_spots;
    spots_detected(:,8)   = z_min_spots;
    spots_detected(:,9)   = z_max_spots;             %- Thresholded or not (based on quality score)
    
    spots_detected(:,10)   = quality_score;
    spots_detected(:,11)   = quality_score_norm;
    spots_detected(:,12)   = 0;                      %- Thresholded or not (based on quality score)   

end      
