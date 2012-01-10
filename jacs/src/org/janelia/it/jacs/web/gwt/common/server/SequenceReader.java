
package org.janelia.it.jacs.web.gwt.common.server;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Dec 18, 2006
 * Time: 6:05:47 PM
 * <p/>
 * Reader to make FASTA out of raw sequence.
 */
public class SequenceReader extends Reader {

    private static final int SEQUENCE_LINE_LENGTH = 60;
    private static final String BREAK_SEQUENCE = System.getProperty("line.separator");
    private static final String FASTA_HEADER_SETTING_SEPARATOR = " /";
    private static final String FASTA_HEADER_SETTING_LR_SEPARATOR = "=";
    private static final int LENGTH_OF_BREAK = BREAK_SEQUENCE.length();

    private boolean _inHeader = true;
    private long _remainingInHeader;
    private long _remainingInText;
    private boolean _inText = false;
    private BaseSequenceEntity _entity;
    private StringBuffer _entityHeader;

    /**
     * Constructor takes the entity to be output.
     *
     * @param entity where to get sequence for fasta.
     */
    public SequenceReader(BaseSequenceEntity entity) {
        _entity = entity;
        _entityHeader = createEntityHeader();
        _remainingInText = _entity.getSequenceLength();
    }

    /**
     * Read characters into a portion of an array.  This method will block
     * until some input is available, an I/O error occurs, or the end of the
     * stream is reached.
     *
     * @param cbuf Destination buffer
     * @param off  Offset at which to start storing characters
     * @param len  Maximum number of characters to read
     * @return The number of characters read, or -1 if the end of the
     *         stream has been reached
     * @throws java.io.IOException If an I/O error occurs
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        int remainingLen = len;
        int totalActuallyRead = 0;
        if (_inHeader) {
            // Reads from header until it is exhausted or enough characters are read to satisfy request.
            //   If something is read here, the 'total actually read' will be greater than zero.
            //   We expect to ALWAYS have some header text.  Being at the END of header text does not imply
            //   end of file, because other text should follow.
            int actuallyRead = readFromHeader(cbuf, off, len);
            remainingLen -= actuallyRead;
            totalActuallyRead += actuallyRead;
            if (remainingLen > 0) {
                _inHeader = false;
                _inText = true;
            }
        }
        if (_inText) {
            // Reads from text of sequence until it is exhausted or enough characters are read to satisfy request.
            //   Reading from the text of the sequence can return exact number of chars requested, less than that
            //   but more than zero, or -1 for end-of-file.
            int actuallyRead = readFromText(cbuf, off + totalActuallyRead, len - totalActuallyRead);
            // This test covers eventuality: did not read anything in header, but
            // text of sequence is already empty.
            if (totalActuallyRead > 0 && actuallyRead > 0)
                totalActuallyRead += actuallyRead;
            else
                totalActuallyRead = actuallyRead;
        }
        return totalActuallyRead;
    }

    /**
     * Close the stream.  Once a stream has been closed, further read(),
     * ready(), mark(), or reset() invocations will throw an IOException.
     * Closing a previously-closed stream, however, has no effect.
     *
     * @throws java.io.IOException If an I/O error occurs
     */
    public void close() throws IOException {
        _remainingInHeader = -1;
        _remainingInText = -1;
    }

    /**
     * Reads from header, however many characters of those requested, remain. Can return
     * all those characters, or just however many are left.
     *
     * @param cbuf where to put the characters
     * @param off  where to start putting them.
     * @param len  how many are requested.
     * @return how many were actually read.
     */
    private int readFromHeader(char[] cbuf, int off, int len) {
        // Must return number-of-chars actually read.
        int nextHeaderPos = (int) (_entityHeader.length() - _remainingInHeader);
        int numToRead = len > (int) _remainingInHeader ?
                (int) _remainingInHeader :
                len;

        int srcEnd = nextHeaderPos + numToRead;
        checkBuffer(cbuf, off, numToRead);

        // srcBegin, srcEnd, dest[], destBegin.
        _entityHeader.getChars(nextHeaderPos, srcEnd, cbuf, off);

        _remainingInHeader -= numToRead;
        return numToRead;
    }

    /**
     * Reads from the sequence text, however many chars requested, or however many
     * remain.  Can return all or just remnant, or can return -1 for end of file.
     *
     * @param cbuf where to put the characters
     * @param off  where to start putting them
     * @param len  how many characters requested
     * @return how many were read or -1.
     */
    private int readFromText(char[] cbuf, int off, int len) {
        // Cover the end of file case.
        if (_remainingInText <= 0)
            return -1;

        int nextTextPos = (int) (_entity.getSequenceLength() - _remainingInText);

        // Need to read a certain number of characters from our source, to which will be
        // added line breaks, prior to be delivered to the request.  After the line breaks
        // are added, the returned stored characters must equal the length requested,
        // unless the source is at an end.  To compensate for the difference between
        // read-from-source, and placed-into-storage, these calculations are done.
        int internalNumToRead;
        if (len > (int) _remainingInText) {
            int afterLastBreak = this.calculateAfterLastBreak(nextTextPos);
            int lengthWithBreaks = (int) _remainingInText + this.calculateNumberOfBreaks((int) _remainingInText,
                    afterLastBreak);
            if (lengthWithBreaks > len) {
                // Here, padding with breaks exceeds the requested size.
                internalNumToRead = recalculateLength((int) _remainingInText, nextTextPos);
            }
            else {
                // Here, can pad the entire internal remainder without exceeding requested size.
                internalNumToRead = (int) _remainingInText;
            }
        }
        else {
            // This case: definitely, request is for less than remain in the buffer,
            // so that the length to read must be shortened to allow for addition of
            // break characters at regular intervals.
            internalNumToRead = recalculateLength(len, nextTextPos);
        }

        int srcEnd = nextTextPos + internalNumToRead;
        checkBuffer(cbuf, off, internalNumToRead);

        // begin, end
        String relevantSequence = null; // Called method adds 1 to srcEnd
        try {
            relevantSequence = _entity.getBioSequence().subSequence(nextTextPos, srcEnd, 1).getSequence();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        relevantSequence = insertBreaksInto(relevantSequence, nextTextPos);

        // srcBegin, srcEnd, dest[], destBegin.
        relevantSequence.getChars(0, relevantSequence.length(), cbuf, off);
        _remainingInText -= internalNumToRead;

        return relevantSequence.length();
    }

    /**
     * In order to include line breaks, the true length must be re-calclated,
     * based on a need to make the output the same length as requested, but to
     * shorten the actual number of characters read, by the number of line
     * breaks * length of each line break.
     *
     * @param len         requested length
     * @param nextTextPos where to read next character?
     * @return corrected length for line break inclusion.
     */
    private int recalculateLength(int len, int nextTextPos) {
        // Figure out number of characters AFTER the most recent line break.
        int afterLastBreak = calculateAfterLastBreak(nextTextPos);
        // Add that to length, and do another mod test, to figure number of breaks
        // within requested sequence.
        int numberOfBreaks = calculateNumberOfBreaks(len, afterLastBreak);
        len -= numberOfBreaks * LENGTH_OF_BREAK;
        return len;
    }

    private int calculateAfterLastBreak(int nextTextPos) {
        return nextTextPos % SEQUENCE_LINE_LENGTH;

    }

    private int calculateNumberOfBreaks(int len, int afterLastBreak) {
        return (int) Math.floor((afterLastBreak + len) / SEQUENCE_LINE_LENGTH);
    }

    /**
     * Given sequence obtained from the entity, insert breaks into it, consistent
     * with FASTA format.
     *
     * @param sequence string of characters.
     * @return modified string of characters.
     */
    private String insertBreaksInto(String sequence, int nextTextPos) {
        // Figure out number of characters AFTER the most recent line break.
        int afterLastBreak = calculateAfterLastBreak(nextTextPos);
        StringBuffer returnBuffer = new StringBuffer(sequence.substring(0, afterLastBreak));
        for (int i = afterLastBreak; i <= sequence.length(); i += SEQUENCE_LINE_LENGTH) {
            int toAppend = SEQUENCE_LINE_LENGTH + i > sequence.length() ?
                    sequence.length() - i :
                    SEQUENCE_LINE_LENGTH;
            returnBuffer.append(sequence.substring(i, i + toAppend));
            if (toAppend == SEQUENCE_LINE_LENGTH)
                returnBuffer.append(BREAK_SEQUENCE);
        }

        return returnBuffer.toString();
    }

    /**
     * Checks whether buffer is sufficient for the request.
     *
     * @param cbuf      buffer to check.
     * @param off       where to put characters into buffer.
     * @param numToRead how many to read.
     */
    private void checkBuffer(char[] cbuf, int off, int numToRead) {
        if (cbuf == null)
            throw new IllegalArgumentException("Read request into null buffer");
        if (cbuf.length < off + numToRead)
            throw new IllegalArgumentException("Read request would overrun buffer");

    }

    /**
     * Given the entity contained herein, make a header (a defline).
     *
     * @return defline representing this entity.
     */
    private StringBuffer createEntityHeader() {
        StringBuffer returnBuffer = new StringBuffer();
        returnBuffer.append(">");
        returnBuffer.append(_entity.getCameraAcc());
        appendSettingToFastaHeader("description", _entity.getDescription(), returnBuffer);
        // Q: sample is causing slow-down of loading?
        // Q: should subclass for specific entity header?
//        appendSettingToFastaHeader("sample_id", _entity.getSample().getSampleAcc(), returnBuffer);
        appendSettingToFastaHeader("full_length", "" + _entity.getSequenceLength(), returnBuffer);

        returnBuffer.append(BREAK_SEQUENCE);
        _remainingInHeader = returnBuffer.length();

        return returnBuffer;
    }

    private void appendSettingToFastaHeader(String name, String value, StringBuffer buffer) {
        if (name != null && value != null) {
            buffer
                    .append(FASTA_HEADER_SETTING_SEPARATOR)
                    .append(name)
                    .append(FASTA_HEADER_SETTING_LR_SEPARATOR)
                    .append(value);
        }
    }

}
