function [cell_prop file_name par_microscope file_name_settings file_name_image_filtered] = FISH_QUANT_load_results_v7(file_name)
% Function to read in outline definition for cells

%- Prepare structure
cell_prop = struct('label', {}, 'x', {}, 'y', {}, 'pos_TS', {}, 'spots_fit', {},'thresh', {});

%- Open file
fid  =  fopen(file_name,'r');

%=== Read in header of file

%-  Line 1-2: Header row
C = textscan(fid,'%s',1,'delimiter','\n');
C = textscan(fid,'%s',1,'delimiter','\n');

%-  Line 3: Comment
C = textscan(fid,'%s',1,'delimiter','\n');

%-  Line 4: Key word FILE
C = textscan(fid,'%s',1,'delimiter','\t'); 

%-  Line 4: File name
C = textscan(fid,'%s',1,'delimiter','\n');   
file_name = char(C{1});


%-  Line 5: Key word FILTERED
C = textscan(fid,'%s',1,'delimiter','\t'); 

%-  Line 5: File name of filtered image
C = textscan(fid,'%s',1,'delimiter','\n');   
file_name_image_filtered = char(C{1});


%-  Line 6: Key-word PARAMETERS
C = textscan(fid,'%s',1,'delimiter','\n');

%-  Line 7: Description of parameters of microscope
C = textscan(fid,'%s',1,'delimiter','\n');

%-  Line 8: Parameters of microscope
C = textscan(fid,'%f32',6,'delimiter','\t');
par_microscope.pixel_size.xy  = double(C{1}(1));
par_microscope.pixel_size.z   = double(C{1}(2));
par_microscope.RI  = double(C{1}(3));
par_microscope.Ex  = double(C{1}(4));
par_microscope.Em  = double(C{1}(5));
par_microscope.NA  = double(C{1}(6));

C = textscan(fid,'%s',1,'delimiter','\n');
par_microscope.type = char(C{1});

%-  Line 9: Detection settings
C   = textscan(fid,'%s',1,'delimiter','\t');     % Key word: ANALYSIS-SETTINGS
C   = textscan(fid,'%s',1,'delimiter','\n');     % File for detection settings
file_name_settings = char(C{1});

%=== Running index for each cell per image
ind_cell = 1;   % Initialize block index

%- Line 10: Key-word CELL
C   = textscan(fid,'%s',1,'delimiter','\t');     % KEY WORD CELL

while (~feof(fid)) 
    
    %- Line 9: Identifier of cell
    C = textscan(fid,'%s',1,'delimiter','\n');
    
    cell_prop(ind_cell).label = char(C{1});    
    
    %=== Polygon of cell
    
    %- Line 11: x-coordinates - stop when string 'END' is found
    C   = textscan(fid,'%s',1,'delimiter','\t'); % Key word 'X_POS';
    
    C   = textscan(fid,'%s',1,'delimiter','\t');
    str = C{1};    
    i=1;    
    while not(strcmp(str,'END'))
        cell_prop(ind_cell).x(i) = str2double(str);
        
        %- Read in next one
        i   = i+1;
        C   = textscan(fid,'%s',1,'delimiter','\t');
        str = C{1};
    end
    
    C = textscan(fid,'%s',1,'delimiter','\n');   % Read-in line-change
    
    %- Line 12: y-coordinates - stop when string 'END' is found
    C   = textscan(fid,'%s',1,'delimiter','\t'); % Key word 'Y_POS';
    
    C = textscan(fid,'%s',1,'delimiter','\t');
    str = C{1};
    i=1;
    
    while not(strcmp(str,'END'))
        cell_prop(ind_cell).y(i) = str2double(str);
        
        %- Read in next one
        i   = i+1;
        C   = textscan(fid,'%s',1,'delimiter','\t');
        str = C{1};
    end
    
    C = textscan(fid,'%s',1,'delimiter','\n');   % Read-in line-change       
        
    %== Polygon of transcription site 
    
    %- Test if there is a transcription site specified    
    C = textscan(fid,'%s',1,'delimiter','\t');
    k = 1;
    
    while strcmp(C{1},'TxSite')
    
        C = textscan(fid,'%s',1,'delimiter','\n'); % Identifier of TS       
        cell_prop(ind_cell).pos_TS(k).label = char(C{1});                
        
        %- Read x-coordinates: first line
        C   = textscan(fid,'%s',1,'delimiter','\t'); % Key word 'X_POS';
        C   = textscan(fid,'%s',1,'delimiter','\t');
        str = C{1};    
        i=1;    
        while not(strcmp(str,'END'))
            cell_prop(ind_cell).pos_TS(k).x(i) = str2double(str);

            %- Read in next one
            i   = i+1;
            C   = textscan(fid,'%s',1,'delimiter','\t');
            str = C{1};
        end

        C = textscan(fid,'%s',1,'delimiter','\n');   % Read-in line-change

        %- Read y-coordinates: first line
        C = textscan(fid,'%s',1,'delimiter','\t'); % Key word 'Y_POS';
        C = textscan(fid,'%s',1,'delimiter','\t');
        str = C{1};
        i=1;

        while not(strcmp(str,'END'))
            cell_prop(ind_cell).pos_TS(k).y(i) = str2double(str);

            %- Read in next one
            i   = i+1;
            C   = textscan(fid,'%s',1,'delimiter','\t');
            str = C{1};
        end
        
        C = textscan(fid,'%s',1,'delimiter','\n');   % Read-in line-change
        
        %- Read-in next line and test if it is a TS.site 
        %  If not will be tested for next block
        C = textscan(fid,'%s',1,'delimiter','\t');
        k = k+1;
    end
    
    %- Results of spot detection
    str = C{1};
    if strcmp(str,'SPOTS')
        C = textscan(fid,'\n');                          % Line-break 
        C = textscan(fid,'%s \t',29,'delimiter','\t');   % Header row
        C = textscan(fid,'%s',1,'delimiter','\n');       % Read-in line-change
        
        C = textscan(fid,'%f32',29,'delimiter','\t');    % First line of detected spots    
        iSpot = 1; 
        spots_par = [];
        
        while isequal(size(C{1}),[29 1])
            spots_par(iSpot,:) = C{1}(:);
        
            C = textscan(fid,'%s',1,'delimiter','\n');       % Read-in line-change        
            C = textscan(fid,'%f32',29,'delimiter','\t');    % Next line of detected spots
            iSpot = iSpot + 1;     
        end
        
        cell_prop(ind_cell).spots_fit  = spots_par(:,1:16);
        cell_prop(ind_cell).spots_detected  = spots_par(:,17:28);
        cell_prop(ind_cell).thresh.in  = (spots_par(:,29));
        cell_prop(ind_cell).thresh.all = ones(size(spots_par(:,29)));
        
        C = textscan(fid,'%s',1,'delimiter','\t');  
    end
    
    ind_cell = ind_cell+1;
end
      
fclose ('all');

