function  [sub_spots sub_spots_filt] = spots_predetect_mosaic_v1(image_struct,img_mask,spots_detected,flag_struct)
%
%
% spots_detected(:,1:3) ... xyz position of detected local maximum in image
% spots_detected(:,4)   ... Quality score;
% spots_detected(:,5)   ... Quality score normalized with maximum
% spots_detected(:,6)   ... Thresholded or not (based on quality score) 
%
% v4 Consider size of stack
%
% v3, May
% - Consider case when no spots are detected
%
% v2 April 14, 2010
% - Move open and close Matlabpool for parrallel computing out of function. This way
%   pool will only be openend once when called in a loop - saves time!
%
% v1 March 28, 2010
% - Original implementation

N_Spots        = size(spots_detected,1);    % Number of candidates 


%= Some parameters
image          = image_struct.data;
image_filt     = image_struct.data_filtered;
sub_spots      = cell(N_Spots,1);
sub_spots_filt = cell(N_Spots,1);  


if N_Spots

    %===  Extract immediate environment for each spot in 3d
    disp('... sub-spot mosaicing...');    

    for i = 1:N_Spots            
        
        y_min = spots_detected(i,4);
        y_max = spots_detected(i,5);
        
        x_min = spots_detected(i,6);
        x_max = spots_detected(i,7);
        
        z_min = spots_detected(i,8);
        z_max = spots_detected(i,9);        
        
       %- For raw data
        sub_spots{i} = double(image(y_min:y_max,x_min:x_max,z_min:z_max));

        %- For filtered data                        
        sub_spots_filt{i} = double(image_filt(y_min:y_max,x_min:x_max,z_min:z_max));         
        
 
    end
       
    %- Plot if defined    
    if flag_struct.output

        if isempty(img_mask)           
            img_mask.max_xy    = max(image_struct.data_filtered,[],3);
            img_mask.max_xz    = squeeze(max(image_struct.data_filtered,[],1));
        end

        figure
        subplot(3,1,1)
        imshow(img_mask.max_xy,[]); hold on 
        plot(spots_detected(:,2),spots_detected(:,1),'b+','MarkerSize',10)
        hold off
        title('All detected spots')

        subplot(3,1,2)
        imshow(img_mask.max_xy,[]); hold on 
        plot(spots_detected(ind_th_in,2),spots_detected(ind_th_in,1),'g+','MarkerSize',10) 
        hold off
        title('Remaining spots')  

        subplot(3,1,3)
        imshow(img_mask.max_xy,[]); hold on 
        plot(spots_detected(ind_th_out,2),spots_detected(ind_th_out,1),'r+','MarkerSize',10) 
        hold off
        title('Removed spots')
        colormap(hot)

    end
end      
