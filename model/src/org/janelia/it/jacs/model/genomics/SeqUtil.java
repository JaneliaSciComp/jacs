
package org.janelia.it.jacs.model.genomics;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Jan 4, 2007
 * Time: 5:11:21 PM
 */
public class SeqUtil {

    public static String reverse(String sequence) {
        if (sequence != null) {
            char[] seqChars = sequence.toCharArray();
            char ch;
            int len = seqChars.length;
            int j = len - 1;
            for (int i = 0; i < j; i++) {
                ch = seqChars[i];
                seqChars[i] = seqChars[j];
                seqChars[j] = ch;
                j--;
            }
            return new String(seqChars);
        }
        else {
            return null;
        }
    }

    public static String convertText(String sequence, String fromElements, String toElements) {

        if (fromElements.length() != toElements.length())
            throw new SequenceException(
                    "Invalid conversion request, \"from\" and \"to\" character sets must be of equal length:\n"
                            .concat("\"").concat(fromElements).concat("\" vs \n")
                            .concat("\"").concat(toElements).concat("\"."));

        String fromSet = fromElements.toUpperCase().concat(fromElements.toLowerCase());
        String toSet = toElements.toUpperCase().concat(toElements.toLowerCase());
        Map<Character, Character> translationMap = new HashMap<Character, Character>();
        for (int i = 0; i < fromSet.length(); i++) {
            translationMap.put(fromSet.charAt(i),
                    toSet.charAt(i));
        }

        Character newCh;
        char newChars[] = sequence.toCharArray();
        for (int i = 0; i < sequence.length(); i++) {
            newCh = translationMap.get(sequence.charAt(i));
            if (newCh == null) {
                throw new SequenceException(
                        "Conversion failed, invalid character in sequence, \""
                                .concat(sequence.substring(i, i + 1)).concat("\"."));
            }
            newChars[i] = newCh;
        }
        return new String(newChars);
    }

    public static void validateText(String sequence, String charSet) {

        String validSet = charSet.toUpperCase().concat(charSet.toLowerCase());
        for (int i = 0; i < sequence.length(); i++) {
            if (validSet.indexOf(sequence.charAt(i)) == -1)
                throw new SequenceException(
                        "Validation failed, invalid character in sequence \""
                                .concat(sequence.substring(i, i + 1)).concat("\"."));
        }
    }

    public static String cleanSequence(String sequence) {
        if (sequence != null) {
            char[] seqChars = sequence.toCharArray();
            int j = 0;
            for (int i = 0; i < seqChars.length; i++) {
                if (seqChars[i] == '\n' ||
                        seqChars[i] == '\r' ||
                        seqChars[i] == '\f' ||
                        seqChars[i] == '\t' ||
                        seqChars[i] == ' ')
                    j++;
                else if (j > 0)
                    seqChars[i - j] = seqChars[i];
            }
            if (j > 0)
                sequence = new String(seqChars).substring(0, seqChars.length - j);
        }
        return sequence;
    }
}
