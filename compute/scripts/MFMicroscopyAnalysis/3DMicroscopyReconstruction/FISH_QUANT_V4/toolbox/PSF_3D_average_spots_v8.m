function [spot_avg spot_os_avg img_sum] = PSF_3D_average_spots_v8(img,img_sum,parameters)
% Florian Mueller, muef@gmx.net
%
% === INPUT PARAMETER
% img      ... 3d with actual image
%
% par_spots ... Defines properties of spots 
%       1st col  ... y-coordinates
%       2nd col  ... x-coordinates
%       3rd col  ... z-coordinates
%       4th col  ... BGD intensity
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
%
% offset  ... offset in X and Y which brings the averaging area away from
%             the actual center and can be used to determine background images.
%
% === FLAGS
%  flag_os     ... Oversampling will be performed  
%  flag_output ... Output plots will be shown
%  flag_bgd    ... Indicates if background should be subtracted from each
%                  spot before averaging.

% === OUTPUT PARAMETERS
%
% === VERSION HISTORY
%
% v1 Feb 8,2011
% - Initial implementation
%
% v2 March 8,2011
% - Corrected bug with 16-bit images and double conversion
%
% v3 March 28,2011
% - Few more bugs corrected


pixel_size  = parameters.pixel_size;
par_spots   = parameters.par_spots;
par_crop    = parameters.par_crop;
fact_os     = parameters.fact_os;
offset      = parameters.offset;
flag_os     = parameters.flag_os;
flag_output = parameters.flag_output;
flag_bgd    = parameters.flag_bgd;



lp = par_crop.xy;                   % Size of detection zone in xy 
lz = par_crop.z;                    % Size of detection zone in z 

dim_crop.X = 2*lp+1;
dim_crop.Y = 2*lp+1;
dim_crop.Z = 2*lz+1;


% Get random orientation for offset
offset_x = (2*round(rand(1,1))-1)* offset;
offset_y = (2*round(rand(1,1))-1)* offset;

%- Some variables 
[dim.Y dim.X dim.Z] = size(img);
N_spots             = size(par_spots,1);


%== Structure for oversampling including buffer zones
%   Consider if results from ealier loop are shown
if isempty(img_sum)
    spot_sum            = zeros(2*lp+1,2*lp+1,2*lz+1);
    
    spot_os_sum = zeros(fact_os.xy*(dim_crop.Y) + 2*fact_os.xy, ...
                        fact_os.xy*(dim_crop.Y) + 2*fact_os.xy,...
                        fact_os.z *(dim_crop.Z) + 2*fact_os.z);
    spot_os_temp = spot_os_sum;
    N_sum    = 0;
else
    spot_sum     = img_sum.spot_sum;
    spot_os_sum  = img_sum.spot_os_sum;
    spot_os_temp = zeros(size(spot_os_sum));
    N_sum        = img_sum.N_sum;
end


%=== Loop over all spots

for i=1:N_spots

    disp(['Processing spot ', num2str(i), ' of ', num2str(N_spots)])


    %- Determine offset by which each sub-image will be moved 
    x_pix.val = par_spots(i,2)/pixel_size.xy;
    x_pix.floor         = floor( x_pix.val)-offset_x;
    x_pix.rem           = x_pix.val-x_pix.floor-offset_x;
    x_pix.subpix        = ceil(x_pix.rem*fact_os.xy);
    x_pix.subpix_offset = floor(fact_os.xy/2)+1-x_pix.subpix ; 

    y_pix.val = par_spots(i,1)/pixel_size.xy;
    y_pix.floor      = floor( y_pix.val)-offset_y;
    y_pix.rem        = y_pix.val-y_pix.floor-offset_y;
    y_pix.subpix     = ceil(y_pix.rem*fact_os.xy);
    y_pix.subpix_offset = floor(fact_os.xy/2)+1-y_pix.subpix ; 

    z_pix.val = par_spots(i,3)/pixel_size.z;  
    z_pix.floor      = floor(z_pix.val);
    z_pix.rem        = z_pix.val-z_pix.floor;
    z_pix.subpix     = ceil(z_pix.rem*fact_os.z);
    z_pix.subpix_offset = floor(fact_os.z/2)+1-z_pix.subpix ; 


    % Make sure that region arround pixel is within image
    if  x_pix.floor-lp >= 1 && x_pix.floor+lp <= dim.X && ...
        y_pix.floor-lp >= 1 && y_pix.floor+lp <= dim.Y && ...
        z_pix.floor-lz >= 1 && z_pix.floor+lz <= dim.Z
    
       img_crop = img(y_pix.floor-lp:y_pix.floor+lp,...
                      x_pix.floor-lp:x_pix.floor+lp,...
                      z_pix.floor-lz:z_pix.floor+lz);

       if flag_os
           % Generate over-sampled image
           spot_os = spot_os_temp;
           for iY=1:dim_crop.Y 
               for iX=1:dim_crop.X
                   for iZ=1:dim_crop.Z

                       X.start = iX*fact_os.xy + 1 + x_pix.subpix_offset;
                       X.end   = (iX+1)*fact_os.xy + x_pix.subpix_offset;
                       Y.start = iY*fact_os.xy+ 1  + y_pix.subpix_offset;
                       Y.end   = (iY+1)*fact_os.xy + y_pix.subpix_offset;
                       Z.start = iZ*fact_os.z + 1  + z_pix.subpix_offset;
                       Z.end   = (iZ+1)*fact_os.z  + z_pix.subpix_offset;


                       spot_os(Y.start:Y.end,X.start:X.end,Z.start:Z.end) = img_crop(iY,iX,iZ); 
                   end
               end
           end
       else
           spot_os = img_crop;
       end
       
       if flag_bgd
           bgd = par_spots(i,4);
           spot_os  = spot_os  - bgd;
           img_crop = img_crop - bgd;
       end

       spot_os_sum = spot_os_sum + spot_os;
       spot_sum    = spot_sum    + img_crop;
       
       N_sum = N_sum+1;
    else
         disp('Spot not considered in averaging (averaging range too large).')
       
    end

    
end

disp('   ')
disp(['# of spots for averaging: ' , num2str(N_sum)])

%- Calculate averaged spots
spot_avg       = spot_sum/N_sum;
spot_os_avg    = spot_os_sum /N_sum;  

%- Save status for averaging in batch mode
img_sum.spot_sum    = spot_sum;
img_sum.spot_os_sum = spot_os_sum;
img_sum.N_sum       = N_sum;


%- Extract area without buffer zone
if flag_os
    spot_os_avg    = spot_os_avg(2*fact_os.xy:end-2*fact_os.xy,2*fact_os.xy:end-2*fact_os.xy,2*fact_os.z+1:end-2*fact_os.z);
end


if flag_output

    spot_xy  = max(spot_avg,[],3);                      
    spot_xz  = squeeze(max(spot_avg,[],2));                          
    spot_yz  = squeeze(max(spot_avg,[],1)); 
    
    spot_us_xy  = max(spot_os_avg,[],3);                      
    spot_us_xz  = squeeze(max(spot_os_avg,[],2));  
    spot_us_yz  = squeeze(max(spot_os_avg,[],1)); 

    %- All projections
    figure
    subplot(3,2,1)
    imshow(spot_xy ,[],'XData', [0 dim_crop.X*pixel_size.xy],'YData',[0 dim_crop.Y*pixel_size.xy])
    title('Normal sampling - XY')

    subplot(3,2,3)
    imshow(spot_xz',[],'XData', [0 dim_crop.X*pixel_size.xy],'YData',[0 dim_crop.Z*pixel_size.z])
    title('Normal sampling - XZ')
    
    subplot(3,2,5)
    imshow(spot_yz',[],'XData', [0 dim_crop.Y*pixel_size.xy],'YData',[0 dim_crop.Z*pixel_size.z])
    title('Normal sampling - YZ')
    
    subplot(3,2,2)
    imshow(spot_us_xy ,[],'XData', [0 dim_crop.X*pixel_size.xy*fact_os.xy],'YData',[0 dim_crop.Y*pixel_size.xy*fact_os.xy])
    title('Over-sampling - XY')

    subplot(3,2,4)
    imshow(spot_us_xz',[], 'XData', [0 dim_crop.X*pixel_size.xy*fact_os.xy],'YData',[0 dim_crop.Z*pixel_size.z*fact_os.z])
    title('Over-sampling - XZ')
    
    subplot(3,2,6)
    imshow(spot_us_yz',[], 'XData', [0 dim_crop.Y*pixel_size.xy*fact_os.xy],'YData',[0 dim_crop.Z*pixel_size.z*fact_os.z])
    title('Over-sampling - YZ')
    colormap jet
    
%     %- Only XY 
%     figure
%     subplot(1,2,1)
%     imshow(spot_xy ,[],'XData', [0 dim_crop.X*pixel_size.xy],'YData',[0 dim_crop.Y*pixel_size.xy])
%     title('Normal sampling - XY')
%     colormap jet
%      
%     subplot(1,2,2)
%     imshow(spot_us_xy ,[],'XData', [0 dim_crop.X*pixel_size.xy*fact_os.xy],'YData',[0 dim_crop.Y*pixel_size.xy*fact_os.xy])
%     title('Over-sampling - XY')
%     colormap jet
    
end