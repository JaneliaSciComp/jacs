function cell_prop = TxSite_detect_v1(image_struct,parameters)

%= Extract parameters
int_th     = parameters.int_th;
conn       = parameters.conn;
min_dist   = parameters.min_dist;
flags      = parameters.flags;
crop_image = parameters.crop_image;
pixel_size = parameters.pixel_size;
cell_prop  = parameters.cell_prop;


crop_xy_pix = ceil(crop_image.xy_nm / pixel_size.xy);
crop_z_pix  = ceil(crop_image.z_nm / pixel_size.z);

N_cells = length(cell_prop);

[dim_Y dim_X dim_Z]= size(image_struct.data);


%- Delete all saved TxSites
for i_cell = 1:N_cells
    cell_prop(i_cell).pos_TS = {};
end


%= Connected components
img_xyz = image_struct.data;
img_bin = img_xyz>int_th;
CC      = bwconncomp(img_bin,conn);

img_det = zeros(size(img_bin));


%= Position of sites
for i_site = 1: CC.NumObjects
         
    status_good = 1;
    
    ind_lin = CC.PixelIdxList{i_site};
    
    img_det(ind_lin) = 1;
    
    
    [coord(i_site).Y coord(i_site).X coord(i_site).Z] = ind2sub(size(img_bin),ind_lin);
    
    coord(i_site).X_center = round(mean(coord(i_site).X));
    coord(i_site).Y_center = round(mean(coord(i_site).Y));
    coord(i_site).Z_center = round(mean(coord(i_site).Z));
    
    coord_center(i_site,:) = [coord(i_site).X_center coord(i_site).Y_center coord(i_site).Z_center];
    site_summary(i_site,:) = [i_site,img_xyz(coord(i_site).Y_center, coord(i_site).X_center, coord(i_site).Z_center)];
    
end


%== Avoid clusters by calculating pairwise distance


%- Pairwise distance
dist_center = pdist(coord_center,'euclidean');
dist_center = squareform(dist_center);


%- Distance > 0 (self-distance) and smaller than certain threshold
[ind_row,ind_col] = find(dist_center < min_dist & dist_center > 0 );
ind_pair = find(ind_col > ind_row); % Matrix is symmetric - take only one value 


ind_good = (1:CC.NumObjects)';
for i = 1:length(ind_pair)
    
   %- Get intensity of respective site
   ind_1st =  ind_row(ind_pair(i));
   ind_2nd =  ind_col(ind_pair(i));
   
   INT_1st = site_summary(ind_1st,2);
   INT_2nd = site_summary(ind_2nd,2);
  
   %- Delete site with smaller intensity from list 
   if INT_1st > INT_2nd       
       ind_delete = find(ind_good == ind_2nd);
   else
       ind_delete = find(ind_good == ind_1st);
   end
    
   if not(isempty(ind_delete))
       ind_good(ind_delete) = [];
   end
       
end




%== See to which cell site belongs
i_site_good = 1;
for i = 1: length(ind_good)    
    
    i_site = ind_good(i);
    
    x_min = round(coord(i_site).X_center - crop_xy_pix);
    x_max = round(coord(i_site).X_center + crop_xy_pix);
    
    y_min = round(coord(i_site).Y_center - crop_xy_pix);
    y_max = round(coord(i_site).Y_center + crop_xy_pix);    

    z_min = round(coord(i_site).Z_center - crop_z_pix);
    z_max = round(coord(i_site).Z_center + crop_z_pix);
    
    if x_min >1 && x_max < dim_X &&  y_min >1 && y_max < dim_Y && z_min >1 && z_max < dim_Z
        
        %- Find cell to which TxSite belongs
        ind_cell_TS = [];
        for i_cell = 1:N_cells
            cell_X = cell_prop(i_cell).x;
            cell_Y = cell_prop(i_cell).y;   

            in_cell = inpolygon(coord(i_site).X_center,coord(i_site).Y_center,cell_X,cell_Y);
  
            if in_cell
                ind_cell_TS = i_cell; 
            end
                
        end


        if not(isempty(ind_cell_TS))

            if isfield(cell_prop(ind_cell_TS),'pos_TS')
                N_TS = length(cell_prop(ind_cell_TS).pos_TS);
            else
                N_TS = 0;
            end
            
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).coord        = coord(i_site);
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).label        = ['TxS_auto_', num2str(i_site)];
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).status_QUANT = 0;
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).auto         = 1;

            cell_prop(ind_cell_TS).pos_TS(N_TS+1).x_min = x_min;
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).y_min = y_min;
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).z_min = z_min;

            cell_prop(ind_cell_TS).pos_TS(N_TS+1).x_max = x_max;
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).y_max = y_max;
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).z_max = z_max;

            cell_prop(ind_cell_TS).pos_TS(N_TS+1).x = [x_min x_max x_max x_min];
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).y = [y_min y_min y_max y_max];
            
            cell_prop(ind_cell_TS).pos_TS(N_TS+1).ind_cell = ind_cell_TS;
            

            TxSite(i_site_good).coord = coord;
            i_site_good = i_site_good + 1;
        end
        
    end
    
end

N_sites_good = i_site_good-1;

if flags.output
    
    disp(['Number of sites [Total]: ', num2str(N_sites)])
    disp(['Number of sites [After quality check]: ', num2str(N_sites_good)])
    
    img_reg_MIP_xy = max(img_reg,[],3);
    img_det_MIP_xy = max(img_det,[],3);
    img_MIP_xy = max(img_xyz,[],3);
    
    %== Plot raw image
    figure
    ax(1) = subplot(2,1,1);
    imshow(img_MIP_xy,[])
    title('Raw image')
    
    %- Plot sites
    hold on
    for i_site = 1: N_sites_good
    
        coord = TxSite(i_site).coord;
    
        X_pos = coord.X_center;
        Y_pos = coord.Y_center;
    
        plot(X_pos,Y_pos,'+g')
        
    end
    hold off
    
    %== Plot connected components
    ax(2) = subplot(2,1,2);
    imshow(img_reg_MIP_xy,[])
    title('Detected connected components')

    colormap Hot

    linkaxes([ax(1) ax(2) ],'xy');  
end