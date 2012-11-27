function state=writesheet(packet,append,format,dlm,newline)
%WRITESHEET   Write result cell array to disk.
%   state=writesheet(packet,append,format,dlm,newline)
%   writes result packets to disk in CSV-format
%
%   In:
%   packet, cell array consisting of a file name, headings and data
%   append, if append = 1, results are appended into an existing file
%   format, write in 'CSV' or in 'XLS' (omitted)
%   dlm, delimiter between data values
%   newline, character displaying the end of a line
%
%   Out:
%   state, if state = 1, writing succeeded
%

% Copyright (C) Jyrki Selinummi, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.

% name of the file to be saved
filename=packet{1};

% headings of data columns
headings=packet{2};

% data matrix
data=packet{3};

% default newline-character
if nargin<5
    if ispc
        newline = sprintf('\r\n');
    else
        newline = sprintf('\n');
    end
end

% default delimiter is ';'
if nargin<4
    dlm=';';
end
% if delimiter is tabulator 'tab', change it to '\t'
if isequal(dlm,'tab')
    dlm=sprintf('\t');
end

% if append "flag" is up, the data is added to the end of the file
if append
    fid = fopen(filename ,'ab');
else
    fid = fopen(filename ,'wb');
end

if fid == (-1)
    state=0;
    return
end

% first print the headings

for col=1:size(headings,2)
    % write a heading
    fwrite(fid, headings{col}, 'uchar');
    
    % write delimiter after all but the last string
    if col~=size(headings,2)
        fwrite(fid, dlm, 'uchar');
    end
end

% newline after the headings, if headings are printed
if size(headings,2)>0
    fwrite(fid, newline, 'char');
end

% print the data columns
for row=1:size(data,1)
    for col=1:size(data,2)
        % write data
        fwrite(fid, num2str(data(row,col)));

        % write delimiter after all but the last data
        if col~=size(data,2)
            fwrite(fid, dlm, 'uchar');
        end
    end
    % newline after each row
    fwrite(fid, newline, 'char');
end


fclose(fid);
state=1;


