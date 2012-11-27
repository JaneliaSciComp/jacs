function psf_rec = PSF_3D_reconstruct_from_radial_v2(psf_radial,PSF_r_nm ,range_rec,center_rec,flag_output)
%
% Function to reconstruct xyz PSF from radial intensity image. 
%
% Florian Mueller, muef@gmx.net
%
% === INPUT PARAMETER
% psf_radial   ... Radial-axial PSF
%
% pixel_size_rec ... Pixel-size of reconstruction
%                    In Z should be a multiple of the pixel-size in Z of
%                    radial-axial image
%       pixel_size.xy  ... in xy 
%       pixel_size.z   ... in z 
%
% center_rel   ... Relative position of center in pixel. Default [0.5 0.5 0.5]
%       center_rel.x  ... in x 
%       center_rel.y  ... in y 
%       center_rel.z  ... in z 

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
% v2 March 8, 2011
% - Changed some parameter-names - length-scales is nanometer!!!


%% Some parameters
dim_rec.Y = length(range_rec.Y_nm);
dim_rec.X = length(range_rec.X_nm);
dim_rec.Z = length(range_rec.Z_nm);

psf_rec_full_z = zeros(dim_rec.Y, dim_rec.X, size(psf_radial,1));
psf_img_rec    = zeros(dim_rec.Y, dim_rec.X, dim_rec.Z);

% Asignment during development - will be input parameters
%range = range_os;


%% Calculat radius for each pixel
for iX = 1:dim_rec.X
    for iY=1:dim_rec.Y
        rPix    = sqrt( (range_rec.X_nm(iX)-center_rec.x_nm)^2 + (range_rec.Y_nm(iY)-center_rec.y_nm )^2);
        r_mask(iY,iX)  = rPix;   

    end
end
    
r_mask_lin_sort = unique(sort(r_mask(:)));
    




%% Loop over all pixels and reconstruct image
psf_rec_full_z = zeros(dim_rec.Y, dim_rec.X, size(psf_radial,1));
int_min = min(psf_radial(:));

for iX = 1:dim_rec.X
    for iY=1:dim_rec.Y

        
        r_loop = r_mask(iY,iX);
        
        
        r_avg_min = r_loop - 180*0.5;
        r_avg_max = r_loop + 180*0.5;
        
        ind_r_avg = find(PSF_r_nm  > r_avg_min & PSF_r_nm  <= r_avg_max); 
        
        %- To avoid division by zero for empty range
            if not(isempty(ind_r_avg))
                avg_z = mean(psf_radial(:,ind_r_avg),2);
            else
                avg_z = int_min;
            end
        
        
        


        
        
        
% OLD IMPLEMENTATION - new one is better!
%       r_avg_min = r_mask(iY,iX) ; 

%        ind_r_min = find(r_mask_lin_sort == r_avg_min);
%         %- Catch largest radius --> no range for averaging is defined
%         if ind_r_min < length(r_mask_lin_sort)
% 
%             r_avg_max = r_mask_lin_sort(ind_r_min +1);
%             ind_r_avg = find(PSF_r_nm  > r_avg_min & PSF_r_nm  <= r_avg_max);                                   
%             
%             %- To avoid division by zero for empty range
%             if not(isempty(ind_r_avg))
%                 avg_z = mean(psf_radial(:,ind_r_avg),2);
%             else
%                 avg_z = int_min;
%             end
%                 
%         else
%             avg_z = int_min;
%         end
            

        psf_rec_full_z(iY,iX,:) = avg_z;

    end
end
        
    
%% Average z-stacks together
N_avg_z   = (size(psf_radial,1)/dim_rec.Z);


for iZ = 1: dim_rec.Z
    iStart = (iZ-1)*N_avg_z+1;
    iEnd   = iZ*N_avg_z;
       
    psf_rec(:,:,iZ) = mean(psf_rec_full_z(:,:,iStart:iEnd),3);    
end


%% Show results
if flag_output
    img_PSF_xy = max(psf_rec,[],3);
    img_PSF_xz = squeeze(max(psf_rec,[],1));
    img_PSF_yz = squeeze(max(psf_rec,[],2));

    figure
    subplot(2,2,2)
    imshow(img_PSF_xz',[],'XData', [range_rec.X_nm(1) range_rec.X_nm(end)],'YData',[range_rec.Z_nm(1) range_rec.Z_nm(end)])
    title('XZ')
       
    subplot(2,2,3)
    imshow(img_PSF_yz,[],'XData', [range_rec.Z_nm(1) range_rec.Z_nm(end)],'YData',[range_rec.Y_nm(1) range_rec.Y_nm(end)])
    title('YZ')
    
    subplot(2,2,4)
    imshow(img_PSF_xy,[],'XData', [range_rec.X_nm(1) range_rec.X_nm(end)],'YData',[range_rec.Y_nm(1) range_rec.Y_nm(end)])
    title('XY')
    colormap jet
end
