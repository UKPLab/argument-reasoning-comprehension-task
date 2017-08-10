/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.experiments.roomfordebate.utils;

/**
 * Utils for Web text normalization
 *
 * @author Ivan Habernal
 */
public class Utils
{

    private static final String WHITESPACE_CHARS = "" + "\\u0009" // CHARACTER TABULATION
            //				+ "\\u000A" // LINE FEED (LF)
            + "\\u000B" // LINE TABULATION
            //				+ "\\u000C" // FORM FEED (FF)
            //				+ "\\u000D" // CARRIAGE RETURN (CR)
            + "\\u0009" // horizontal tab
            + "\\u0010" // Data Link Escape
            + "\\u0020" // SPACE
            + "\\u0085" // NEXT LINE (NEL)
            + "\\u00A0" // NO-BREAK SPACE
            + "\\u1680" // OGHAM SPACE MARK
            + "\\u180E" // MONGOLIAN VOWEL SEPARATOR
            + "\\u2000" // EN QUAD
            + "\\u2001" // EM QUAD
            + "\\u2002" // EN SPACE
            + "\\u2003" // EM SPACE
            + "\\u2004" // THREE-PER-EM SPACE
            + "\\u2005" // FOUR-PER-EM SPACE
            + "\\u2006" // SIX-PER-EM SPACE
            + "\\u2007" // FIGURE SPACE
            + "\\u2008" // PUNCTUATION SPACE
            + "\\u2009" // THIN SPACE
            + "\\u200A" // HAIR SPACE
            + "\\u2028" // LINE SEPARATOR
            + "\\u2029" // PARAGRAPH SEPARATOR
            + "\\u202F" // NARROW NO-BREAK SPACE
            + "\\u205F" // MEDIUM MATHEMATICAL SPACE
            + "\\u3000";

    private static final String WHITESPACE_CHAR_CLASS = "[" + WHITESPACE_CHARS + "]";

    /**
     * Replaces all sorts of "weird" unicode whitespaces by a normal whitespace, removes unicode
     * control characters.
     *
     * @param text text
     * @return new text text
     */
    public static String normalizeWhitespaceAndRemoveUnicodeControlChars(String text)
    {
        // first replace all control characters except newlines
        String result = text.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "");
        // all weird whitespaces
        return result.replaceAll(WHITESPACE_CHAR_CLASS + "+", " ");
    }

    /**
     * Normalizes the given string - unifying whitespaces, quotations, and dashes
     *
     * @param text text
     * @return normalized text
     */
    public static String normalize(String text)
    {
        String result = text.replaceAll("\\n+", "\n");

        result = normalizeWhitespaceAndRemoveUnicodeControlChars(result);

        // trim the lines
        result = result.replaceAll("\\n" + WHITESPACE_CHAR_CLASS + "+", "\n");
        result = result.replaceAll(WHITESPACE_CHAR_CLASS + "+\\n", "\n");

        // dashes
        String dashChars = "" + "\\u2012" // figure dash
                + "\\u2013" // en dash
                + "\\u2014" // em dash
                + "\\u2015" // horizontal bar
                + "\\u2053" // swung dash
                ;
        result = result.replaceAll("[" + dashChars + "]+", "-");

        // elipsis
        result = result.replaceAll("\\u2026", "...");

        // quotation marks
        result = result.replaceAll("[“”«»„‟]", "\"");
        result = result.replaceAll("[‘’‚‛‹›`]", "'");

        // keep only latin-friendly characters
        result = result.replaceAll(
                "[[^\\p{InBasic_Latin}\\p{InLatin_1_Supplement}]+[\\u00A0-\\u00BF]+]", " ");

        // mal-formed input (unicode characters already escaped)
        result = result.replaceAll("%u2013", "-");
        result = result.replaceAll("%u2019", "'");
        result = result.replaceAll("%u2014", "-");
        result = result.replaceAll("%u201C", "\"");
        result = result.replaceAll("%u201D", "\"");

        return result.trim();
    }
}
