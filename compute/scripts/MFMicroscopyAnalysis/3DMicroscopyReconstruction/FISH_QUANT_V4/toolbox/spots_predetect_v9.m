function  [spots_detected img_mask] = spots_predetect_v9(image_struct,options,flag_struct)
% Function returns pixel location of local maximum
%
% spots_detected(:,1:3) ... xyz position of detected local maximum in image
%
% v1 Feb 28, 2010
% - Original implementation
%
% v2 March 1, 2010
% - Restrict search on area of nucleus alone
%
% v3 March 2, 2010
% - Adjust to new definiton of outlines of cell and TS
%
% v4
%- Adjust to new defintion of cell_prop
%
% v5
% - new version which allow thresholding of the score parameter


%= Options
size_detect = options.size_detect;
detect_th   = options.detect_th;
cell_prop   = options.cell_prop;
pixel_size  = options.pixel_size;


%= Dimension of the image
%image      = image_struct.data;
image_filt = image_struct.data_filtered;
[dim.Y dim.X dim.Z] = size(image_filt);


%= Set image outside of nucleus to zero
image_filt_mask = image_filt;

if not(isempty(cell_prop))
    [X_grid,Y_grid] = meshgrid(1:dim.X,1:dim.Y);
    mask_nuc_2D = inpolygon(X_grid,Y_grid,cell_prop.x,cell_prop.y);
    mask_nuc_3D = repmat(mask_nuc_2D,[1,1,dim.Z]);   
    image_filt_mask(not(mask_nuc_3D)) = 0;
end    
   
img_mask.max_xy    = max(image_filt_mask,[],3);
img_mask.max_xz    = squeeze(max(image_filt_mask,[],1));


%===  Detect local maximum by nonmaximal suppression
% Comment: toolbox might have to be installed again if error message pop up.
% Give 3d coordinates of all identified non-suppressed point locations (n x d)
% Coordinates are integer - no sub-pixel information
% (1,1,1) is pixel in upper left corner on first focal plane

if isempty(detect_th)
    detect_th  = prctile(image_filt(:),99);
end

disp('... non maximum supression ...');
pos_max_suppr = nonMaxSupr(image_filt_mask, round([size_detect.xy size_detect.xy size_detect.z]),detect_th);


    
%===  Remove spots which are close to edge of image and sort rows
disp('... remove spots close to the edge ...');

ind_x   = (pos_max_suppr(:,1) > size_detect.xy) & (pos_max_suppr(:,1) <= dim.Y-size_detect.xy);
ind_y   = (pos_max_suppr(:,2) > size_detect.xy) & (pos_max_suppr(:,2) <= dim.X-size_detect.xy);


if not(flag_struct.region_smaller)
    ind_z   = (pos_max_suppr(:,3) > size_detect.z)  & (pos_max_suppr(:,3) <= dim.Z-size_detect.z);    
    ind_sel = ind_x & ind_y & ind_z;

else
    ind_sel = ind_x & ind_y;
end    

pos_in_img = sortrows(sortrows(sortrows(pos_max_suppr(ind_sel,:),1),2),3);   


%=== Remove spots which are not in the cell
%    v2: redundant since image is set to zero outside of cell
disp('... remove spots outside of cell and in transcription site ...');

pos_in_cell = pos_in_img;


%=== Remove spots which are within the transcription site(s)
%    Loop through the list of polynoms describing the transcription site   

pos_out_TS = pos_in_cell;

N_TS = size(cell_prop.pos_TS,2);

if N_TS > 0

    for k = 1:N_TS

        %- Find spots which are not in TS
        in_TS  = inpolygon(pos_out_TS(:,2),pos_out_TS(:,1),cell_prop.pos_TS(k).x,cell_prop.pos_TS(k).y); % Points defined in Positions inside the polygon
        aux_TS = find(not(in_TS==1));

        pos_dum(:,1) = pos_out_TS(aux_TS,1);
        pos_dum(:,2) = pos_out_TS(aux_TS,2);
        pos_dum(:,3) = pos_out_TS(aux_TS,3);        

        clearvars pos_out_TS;

        pos_out_TS = pos_dum;

        clearvars pos_dum;
    end   
else
    pos_out_TS = pos_in_img;
end

pos_spots_detected    = pos_out_TS;
spots_detected(:,1:3) = pos_spots_detected;

        
%=== Plot results of spot detection
%=   Subtract one from each value to center cross in pixel since we don't
%    have sub-pixel pointing accuracy (and the way matlab handles sub-pixel pointing)

if flag_struct.output
       
    figure
    h1 = subplot(3,2,1);
    imshow(img_mask.max_xy,[]); hold on           
    plot(pos_max_suppr(:,2),pos_max_suppr(:,1),'gx','MarkerSize',10)
    hold off
    title('All detected spots')

    h2 = subplot(3,2,2);
    imshow(img_mask.max_xz',[],'XData',[0 (dim.X-1)*pixel_size.xy],'YData',[0 (dim.Z-1)*pixel_size.z]); hold on           
    plot(pos_max_suppr(:,2)*pixel_size.xy-pixel_size.xy,pos_max_suppr(:,3)*pixel_size.z-pixel_size.z,'gx','MarkerSize',10)
    hold off
    title('All detected spots')

    h3 = subplot(3,2,3);
    imshow(img_mask.max_xy,[]); hold on           
    plot(pos_in_img(:,2),pos_in_img(:,1),'gx','MarkerSize',10)
    hold off
    title('Spots away from edge')

    h4 = subplot(3,2,4);
    imshow(img_mask.max_xz',[],'XData',[0 (dim.X-1)*pixel_size.xy],'YData',[0 (dim.Z-1)*pixel_size.z]); hold on            
    plot(pos_in_img(:,2)*pixel_size.xy-pixel_size.xy,pos_in_img(:,3)*pixel_size.z-pixel_size.z,'gx','MarkerSize',10)
    hold off
    title('Spots away from edge')

    h5 = subplot(3,2,5);
    imshow(img_mask.max_xy,[]); hold on           
    plot(pos_spots_detected(:,2),pos_spots_detected(:,1),'gx','MarkerSize',10)
    hold off
    title('In cell & outside TS')

    h6 = subplot(3,2,6);
    imshow(img_mask.max_xz',[],'XData',[0 (dim.X-1)*pixel_size.xy],'YData',[0 (dim.Z-1)*pixel_size.z]); hold on              
    plot(pos_spots_detected(:,2)*pixel_size.xy-pixel_size.xy,pos_spots_detected(:,3)*pixel_size.z-pixel_size.z,'gx','MarkerSize',10)
    hold off
    title('In cell & outside TS')
    colormap(hot)
    
    linkaxes([h1,h3,h5], 'xy');
    linkaxes([h2,h4,h6], 'xy');

    
end

   