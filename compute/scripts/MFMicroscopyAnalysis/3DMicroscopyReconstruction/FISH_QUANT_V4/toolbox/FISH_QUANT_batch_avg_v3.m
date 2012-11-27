function [spot_avg spot_os_avg average par_microscope] = FISH_QUANT_batch_avg_v3(handles)


%- Various flags
%flag_output    = 1;
flag_os        = 1;
par_microscope = handles.par_microscope;
pixel_size     = par_microscope.pixel_size;
average        = handles.average;
col_par        = handles.col_par;

%=== Area to consider around the spots +/- in xy and z
%- Crop region for fit

dlgTitle = 'Parameters for averaging';

prompt_avg(1) = {'Size of region around spot XY [+/-pixel]'};
prompt_avg(2) = {'Size of region around pixels Z [+-/ pixel]'};
prompt_avg(3) = {'Factor for oversampling [XY]'};
prompt_avg(4) = {'Factor for oversampling [Z]'};
prompt_avg(5) = {'Background subtracted form each spot (Y=1, N=0)'};
prompt_avg(6) = {'Offset in X and Y [Pix] - used for BGD calc'};


defaultValue_avg{1} = num2str(average.crop.xy);
defaultValue_avg{2} = num2str(average.crop.z);
defaultValue_avg{3} = num2str(average.fact_os.xy);
defaultValue_avg{4} = num2str(average.fact_os.z);
defaultValue_avg{5} = num2str(0);
defaultValue_avg{6} = num2str(0);

userValue = inputdlg(prompt_avg,dlgTitle,1,defaultValue_avg);

if( ~ isempty(userValue))
    average.crop.xy    = str2double(userValue{1});
    average.crop.z     = str2double(userValue{2}); 
    average.fact_os.xy = str2double(userValue{3});
    average.fact_os.z  = str2double(userValue{4});     
    flag_bgd           = str2double(userValue{5}); 
    offset             = str2double(userValue{6}); 

    pixel_size_os.xy             = pixel_size.xy / handles.average.fact_os.xy;
    pixel_size_os.z              = pixel_size.z  / handles.average.fact_os.z;
    par_microscope.pixel_size_os = pixel_size_os;

    %- Average relevant parameter
    path_name = handles.path_name_image;
    cell_summary = handles.cell_summary;


    %=== Average first image
    ind_cell     = 1;
    file_name    = cell_summary(ind_cell,1).name_image;
    image_struct = load_stack_data_v4(fullfile(path_name,file_name));
    image        = image_struct.data;
    ind_spots    = cell_summary(ind_cell,1).thresh.in;
    spots_fit    = cell_summary(ind_cell,1).spots_fit;
    spots_fit    = spots_fit(ind_spots == 1,:);

    par_spots    = spots_fit(:,[col_par.pos_y col_par.pos_x col_par.pos_z col_par.bgd]);
    
    %- Parameters for averaging
    parameters.pixel_size  = pixel_size;
    parameters.par_spots   = par_spots;
    parameters.par_crop    = average.crop;
    parameters.fact_os     = average.fact_os;
    parameters.offset      = offset;
    parameters.flag_os     = flag_os;
    parameters.flag_output = 0;
    parameters.flag_bgd    = flag_bgd;
        
        
    %- Parameters needed for function call
    disp(['Processing ', num2str(ind_cell), ', of ' , num2str(size(cell_summary,1))]) 
    [aux1 aux2 img_sum] = PSF_3D_average_spots_v8(image,[],parameters);


    %- Average the rest
    for ind_cell = 2:size(cell_summary,1)
        disp(['Processing ', num2str(ind_cell), ', of ' , num2str(size(cell_summary,1))]) 

        file_name    = cell_summary(ind_cell,1).name_image;
        image_struct = load_stack_data_v4(fullfile(path_name,file_name));
        image        = image_struct.data;
        ind_spots    = cell_summary(ind_cell,1).thresh.in;
        spots_fit    = cell_summary(ind_cell,1).spots_fit;
        spots_fit    = spots_fit(ind_spots == 1,:);
        
        par_spots    = spots_fit(:,[col_par.pos_y col_par.pos_x col_par.pos_z col_par.bgd]);
        parameters.par_spots   = par_spots;

        %- Parameters needed for function call
        [aux1 aux2 img_sum] = PSF_3D_average_spots_v8(image,img_sum,parameters);
        
    end


    %- Calculate averaged image
    spot_avg       = img_sum.spot_sum/img_sum.N_sum;
    spot_os_avg    = img_sum.spot_os_sum /img_sum.N_sum; 


    %- Extract area without buffer zone
    if flag_os
        fact_os = average.fact_os;
        spot_os_avg  = spot_os_avg(2*fact_os.xy:end-2*fact_os.xy,2*fact_os.xy:end-2*fact_os.xy,2*fact_os.z+1:end-2*fact_os.z);
    end


    spot_xy  = max(spot_avg,[],3);                      
    spot_xz  = squeeze(max(spot_avg,[],2));                          
    spot_yz  = squeeze(max(spot_avg,[],1)); 

    spot_us_xy  = max(spot_os_avg,[],3);                      
    spot_us_xz  = squeeze(max(spot_os_avg,[],2));  
    spot_us_yz  = squeeze(max(spot_os_avg,[],1)); 


    [dim.Y dim.X dim.Z] = size(spot_os_avg);

    %- All projections
    figure
    subplot(3,2,1)
    imshow(spot_xy ,[],'XData', [0 dim.X*pixel_size.xy],'YData',[0 dim.Y*pixel_size.xy])
    title('Normal sampling - XY')

    subplot(3,2,3)
    imshow(spot_xz',[],'XData', [0 dim.X*pixel_size.xy],'YData',[0 dim.Z*pixel_size.z])
    title('Normal sampling - XZ')

    subplot(3,2,5)
    imshow(spot_yz',[],'XData', [0 dim.Y*pixel_size.xy],'YData',[0 dim.Z*pixel_size.z])
    title('Normal sampling - YZ')

    subplot(3,2,2)
    imshow(spot_us_xy ,[],'XData', [0 dim.X*pixel_size.xy*fact_os.xy],'YData',[0 dim.Y*pixel_size.xy*fact_os.xy])
    title('Over-sampling - XY')

    subplot(3,2,4)
    imshow(spot_us_xz',[], 'XData', [0 dim.X*pixel_size.xy*fact_os.xy],'YData',[0 dim.Z*pixel_size.z*fact_os.z])
    title('Over-sampling - XZ')

    subplot(3,2,6)
    imshow(spot_us_yz',[], 'XData', [0 dim.Y*pixel_size.xy*fact_os.xy],'YData',[0 dim.Z*pixel_size.z*fact_os.z])
    title('Over-sampling - YZ')
    colormap hot
else
    spot_avg     = [];
    spot_os_avg  = [];
end