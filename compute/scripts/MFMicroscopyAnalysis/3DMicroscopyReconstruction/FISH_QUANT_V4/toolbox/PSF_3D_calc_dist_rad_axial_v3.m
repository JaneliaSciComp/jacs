function [psf_radial_bin range] = PSF_3D_calc_dist_rad_axial_v3(img,pixel_size,factor_resample,range,center,flag_output)
%
% Function to calculate radial-axial intensity distribution
%
% Florian Mueller, muef@gmx.net
%
% === INPUT PARAMETER
% img      ... 3d with actual image
%
% par_spots ... Defines properties of spots 
%       1st col  ... y-coordinates
%       2nd col  ... x-coordinates
%       3rd col  ... z-coordinates
%
% par_crop ... Specifies cropping area (see also flag_crop)
%       par_crop.xy  ... in xy (+/- pixel from center)
%       par_crop.z   ... in z (+/- pixel from center)
%
% pixel_size ... Pixel-size
%       pixel_size.xy  ... in xy 
%       pixel_size.z   ... in z 
%
% fact_os ... Specifies oversampling in xy and z
%       fact_os.xy  ... in xy 
%       fact_os.z   ... in z 
%
% === FLAGS
%  flag_os     ... Oversampling will be performed  
%  flag_output ... Output plots will be shown

% === OUTPUT PARAMETERS
%
% === VERSION HISTORY
%
% v1 Feb 8,2011
% - Initial implementation
%
% v2 March 4, 2011
% - Changed output

[dim.Y dim.X dim.Z] = size(img);

if isempty(factor_resample)
    factor_resample = 1;
end

%=== Define center of PSF (if not specified already)

if isempty (center)

    %=== Define range for reconstructed image
    range.X = 1:dim.X;
    range.Y = 1:dim.Y;
    range.Z = 1:dim.Z;

    %== Find pixel with maximum intensity
    [dum max_ind_lin_3d] = max(img(:));
    [max_ind_y,max_ind_x,max_ind_z] = ind2sub(size(img),max_ind_lin_3d);

    center.x_pix = max_ind_x;
    center.y_pix = max_ind_y;
end



%=== Analyze radial distribution of image
psf_radial = [];
radius_all = [];

psf_stack_z = {};


%- Calculate the radius of each spot
r_mask = zeros(dim.Y,dim.X);

for iX = 1:dim.X
    for iY=1:dim.Y
        rPix           = sqrt( (range.X(iX)-center.x_pix)^2 + (range.Y(iY)-center.y_pix )^2); 
        r_mask(iY,iX)  = rPix;
    end
end

r_mask_lin = r_mask(:);




%- Calculate the intensity of 
for iZ = 1:dim.Z
    
    disp(['Processing focal plane ', num2str(iZ), ' of ', num2str(dim.Z)])
    
    radial_int      = zeros(dim.X*dim.Y,2);   
    
    int_z     = img(:,:,iZ);
    int_z_lin = int_z(:);
    
    radial_int(:,1) = r_mask_lin;
    radial_int(:,2) = int_z_lin;
    
    %- Sort profile
    radial_int_sort = sortrows(radial_int,1);

    %- Average intensities of unique radii
    radius_unique     = unique(radial_int_sort(:,1));
    radial_int_unique = zeros(length(radius_unique),3);
    
    for i = 1:length(radius_unique)

        ind_loop = find(radial_int_sort(:,1) == radius_unique(i));

        int_avg = mean(radial_int_sort(ind_loop,2));
        int_std = std(radial_int_sort(ind_loop,2));

        radial_int_unique(i,1) = radius_unique(i);
        radial_int_unique(i,2) = int_avg;
        radial_int_unique(i,3) = int_std;
    end

    %- All the information
    psf_stack_z{iZ}.data = radial_int;
    psf_stack_z{iZ}.range             = range;
    psf_stack_z{iZ}.radial_int        = radial_int;
    psf_stack_z{iZ}.radial_int_unique = radial_int_unique;


    psf_radial(iZ,:) = radial_int_unique(:,2);
    radius_all(iZ,:) = radial_int_unique(:,1);
end

radius_avg = mean(radius_all,1);

range.Z_nm  = range.Z*pixel_size.z;

range.r_all_pix = radius_avg;
range.r_all_nm  = range.r_all_pix*pixel_size.xy;



%- Radii are not equidistant - map back on pixel-values 
%  Use factor_resample for this

rad_max        = ceil(max(radius_avg));
N_bin          = rad_max*factor_resample;
psf_radial_bin = zeros(dim.Z,N_bin);
rad_bin        = zeros(N_bin,1);

rad_bin = (1:N_bin)/factor_resample;

for iR = 1:N_bin

    if iR == 1          
        bin_min = 0;
    else
        bin_min = rad_bin(iR-1);
    end
        
    bin_max = rad_bin(iR);

    ind_rad = find(radius_avg > bin_min & radius_avg <= bin_max);
    psf_radial_bin(:,iR) = mean(psf_radial(:,ind_rad),2);
   
end

range.r_bin    = rad_bin;
range.r_bin_nm = rad_bin*pixel_size.xy;


if flag_output
        
    %- Plot entire range of radii    
    X = range.r_all_nm;
    Y = range.Z_nm;
        
    figure
    
    %- Plot binned adii    
    subplot(1,2,1)
    imshow(psf_radial_bin, [],'Colormap',jet,'InitialMagnification',800,'XData', range.r_bin_nm, 'YData', range.Z_nm)
    xlabel('Radius')
    ylabel('Z')
    title('PSF (binned): radial-axial. Normal scale')

    subplot(1,2,2)
    imshow(log2(psf_radial_bin-min(psf_radial_bin(:))+1), [],'Colormap',jet,'InitialMagnification',800,'XData', range.r_bin_nm, 'YData', range.Z_nm)
    xlabel('Radius')
    ylabel('Z')
    title('PSF (binned): radial-axial. Log scale')
    

end

