function saveresults(result, filename, directory, batchmode, spec)
%SAVERESULTS   Write results to disk (with WRITESHEET).
%   saveresults(result, filename, directory, batchmode, spec) saves a result matrix to
%   .csv file that can be opened using a spreadsheet program.
%
%   In:
%   result, result cell array created using CALCPROPS
%   filename, name of the image file from which results were calculated.
%   directory, folder where results are saved (in batch mode).
%   batchmode, if BATCHMODE = 1, results are saved to default directory with
%              default filename, nothing is asked from the user.
%   spec, if spec = 1, results of specifically stained cells are saved in
%         addition to the total count

% Copyright (C) Jyrki Selinummi, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.

% whether to save in xls or csv files
global writemode;
xls=isequal(writemode,'xls');

% name of the folder where figures were saved if not in batch mode
% This is the default save directory for the .csv results
global savedirectory;

% batch processing or not
if nargin<4
    batchmode=0;
end

% remove .jpg or other extension from filename, and add .CSV to save in
% excel (comma separated values) format
dots=find(filename=='.');
if ~isempty(dots)
    % leave out everything after the last '.' in filename
    newfilename=filename(1:dots(end)-1);
end

% add .CSV
if xls
    file=[newfilename '.XLS'];
else
    file=[newfilename '.CSV'];
end
filt = cellstr([file]);

if ~batchmode
    if ~isequal(savedirectory,0)
        [file,directory,filterindex] = uiputfile([savedirectory file],'Save Result Data Sheet As');
    else
        [file,directory,filterindex] = uiputfile(file,'Save Result Data Sheet As');
    end
    if isequal(file,0)
        return;
    end
    
    % check if .csv has to be added to end of filename or not. Now it is
    % impossible to save file without .csv ending if filter '*.CSV' is selected
    % same applies to XLS
    if filterindex==1
        d=filt{filterindex};
        rest=d(end-3:end);
        if size(file,2)>3
            if xls
                if (isequal(file(1,size(file,2)-3:end),'.xls')|isequal(file(1,size(file,2)-3:end),'.XLS'))
                    rest=[];
                end
            else
                if (isequal(file(1,size(file,2)-3:end),'.csv')|isequal(file(1,size(file,2)-3:end),'.CSV'))
                    rest=[];
                end
            end
        end
        
    else 
        rest=[];
    end
else
    rest=[];
end

% name of the folder+file
folder=fullfile(directory,file,rest);
% if ispc==1
%     % pc-computer
%     folder=[directory '\' file rest];
% else
%     % UNIX
%     folder=[directory '/' file rest];
% end


% save as ascii values, separated by a ','

 
% folder is the filename, result{1}{1} is a cell array of headings and
% result{1}{2} a cell array of data columns
writesheet({folder result{1}{1} result{1}{2}},0,'csv');

% save the mean values to the end of file
writesheet({folder result{2}{1} result{2}{2}},1,'csv');

% last line to state whether the results are in pixels or in um
% this solution is only TEMPORARY
global trash;
ratio=trash;
if isequal(ratio,'----')
    writesheet({folder {'Unit of measure: pixels'} {}},1,'csv');
else
    writesheet({folder {'Unit of measure: µm'} {}},1,'csv');
end


% save number of cells to a summary file, if in batch mode
if batchmode
    if ispc==1
        % pc-computer
        if xls
            folder=[directory '\' 'summary.xls'];
        else
            folder=[directory '\' 'summary.csv'];
        end
    else
        % UNIX
        if xls
            folder=[directory '/' 'summary.xls'];
        else
            folder=[directory '/' 'summary.csv'];
        end
    end

    % check if the summary file already exists, if not, insert headers to
    % the first line
    if ~exist(folder)
        if spec
            columnnames={'Total cell count (Figure1)' 'Number of cells that exist in both Figures' 'Mean area of cells' 'Mean volume'...
        'Mean length' 'Mean width' 'Mean intensity' 'Mean of intensity maxima' 'Mean of solidities' 'Mean of compactness'};
        else
            columnnames={'Total cell count (Figure1)' 'Mean area of cells' 'Mean volume'...
        'Mean length' 'Mean width' 'Mean intensity' 'Mean of intensity maxima' 'Mean of solidities' 'Mean of compactness'};
        end
        if isequal(ratio,'----')
            writesheet({folder {'Unit of measure: pixels'} {}},0,'csv');
        else
            writesheet({folder {'Unit of measure: µm'} {}},0,'csv');
        end
        writesheet({folder columnnames {}},1,'csv');
    end

    % Append the results to a previous summary.csv
    % # of cells
    global nos;
    result{2}{2}(1)=nos;
    % how many cells existed in the specifically stained image
    if spec
        global inspecific;
        result{2}{2}(2)=inspecific;
    end
    
    columnnames={filename};
    writesheet({folder columnnames result{2}{2}},1,'csv');
end
