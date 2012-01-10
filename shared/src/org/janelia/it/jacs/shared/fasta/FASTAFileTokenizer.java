
package org.janelia.it.jacs.shared.fasta;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.shared.utils.InFileChannelHandler;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.FileChannel;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Jan 31, 2008
 * Time: 10:31:17 AM
 */
public class FASTAFileTokenizer implements IsSerializable, Serializable {
    static public final int UNKNOWN = 0;
    static public final int EOF = 1;
    static public final int NL = 2;
    static public final int DEFLINESTART = 3;
    static public final int OTHERBYTE = 4;

    static public final int EMPTYLINE = 5;
    static public final int STARTDEFLINE = 6;
    static public final int DEFLINE = 7;
    static public final int SEQUENCELINE = 8;

    final static char[] sequenceChars = new char[]{
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '*', '-'
    };

    private int nLines;
    private int nCols;
    private InFileChannelHandler channelHandler;
    private int tokenValue;
    private int previousByte;
    private String fastaLine;
    private String fastaEntry;

    public FASTAFileTokenizer(FileChannel channel) {
        this(new InFileChannelHandler(channel));
    }

    public FASTAFileTokenizer(InFileChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
        tokenValue = UNKNOWN;
        previousByte = -1;
    }

    public FASTAFileTokenizer() {
    }

    public int getNLines() {
        return nLines;
    }

    public int getNCols() {
        return nCols;
    }

    public long getPosition() throws IOException {
        return channelHandler.getReadPosition();
    }

    public int getTokenValue() {
        return tokenValue;
    }

    public String getFastaEntry() {
        return fastaEntry;
    }

    public String getFastaLine() {
        return fastaLine;
    }

    public int nextToken() throws IOException {
        for (; ;) {
            if (channelHandler.eof()) {
                return EOF;
            }
            tokenValue = readNextByte();
            if (tokenValue == '\r') {
                // if this is DOS eol ignore the CR
            }
            else if (tokenValue == '\n') {
                nLines++;
                nCols = 0;
                return NL;
            }
            else if (tokenValue == '>') {
                nCols++;
                return DEFLINESTART;
            }
            else {
                nCols++;
                return OTHERBYTE;
            }
        }
    }

    public int nextFASTALine() throws IOException {
        StringBuffer fastaLineBuffer = new StringBuffer();
        for (; ;) {
            int b = readNextByte();
            if (b == -1) {
                break;
            }
            else if (b == '\r') {
                // if this is DOS eol ignore the CR
            }
            else if (b == '\n') {
                nLines++;
                nCols = 0;
                break;
            }
            else {
                nCols++;
                fastaLineBuffer.append((char) b);
            }
        }
        fastaLine = fastaLineBuffer.toString().trim();
        if (fastaLine.length() > 0) {
            if (fastaLine.charAt(0) == '>') {
                return STARTDEFLINE;
            }
            else {
                return SEQUENCELINE;
            }
        }
        else {
            return EMPTYLINE;
        }
    }

    public int nextFASTAEntry(boolean collectSequence) throws IOException {
        StringBuffer fastaEntryBuffer = new StringBuffer();
        int state = UNKNOWN;
        int nResidues = 0;
        for (; ;) {
            int b = readNextByte();
            if (b == -1) {
                break;
            }
            else if (b == '\r') {
                // if this is DOS eol ignore the CR
            }
            else if (b == '\n') {
                nLines++;
                nCols = 0;
                if (state == STARTDEFLINE) {
                    // just finished reading the defline -> start reading the sequence
                    state = SEQUENCELINE;
                    fastaEntryBuffer.append('\n');
                }
                else if (state == SEQUENCELINE) {
                    if (collectSequence) {
                        fastaEntryBuffer.append('\n');
                    }
                }
            }
            else if (b == '>') {
                nCols++;
                if (state == UNKNOWN) {
                    // start reading the defline
                    state = STARTDEFLINE;
                    fastaEntryBuffer.append('>');
                }
                else if (state == SEQUENCELINE) {
                    // this is actually part of the next entry
                    unreadByte(b);
                    break;
                }
                else {
                    // most likely there's a greater than in the defline
                    fastaEntryBuffer.append('>');
                }
            }
            else {
                nCols++;
                if (state == SEQUENCELINE) {
                    nResidues++;
                    if (collectSequence) {
                        fastaEntryBuffer.append((char) b);
                    }
                }
                else if (state == STARTDEFLINE) {
                    fastaEntryBuffer.append((char) b);
                }
            }
        }
        fastaEntry = fastaEntryBuffer.toString();
        return nResidues;
    }

    public int readNextByte() throws IOException {
        int b;
        if (previousByte != -1) {
            b = previousByte;
            previousByte = -1;
        }
        else {
            if (channelHandler.eof()) {
                b = -1;
            }
            else {
                b = channelHandler.readByte();
            }
        }
        return b;
    }

    public void unreadByte(int b) {
        previousByte = b;
    }

} // end FASTAFileTokenizer
