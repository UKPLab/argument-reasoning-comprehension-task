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

package de.tudarmstadt.ukp.experiments.pipeline.filters;

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Argument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Debate;
import de.tudarmstadt.ukp.experiments.pipeline.utils.CollectionUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author Ivan Habernal
 */
public class QuotationFilter
        implements ArgumentSamplingFilter
{
    private final int maxWordsInQuotationAllowed;

    public QuotationFilter(int maxWordsInQuotationAllowed)
    {
        this.maxWordsInQuotationAllowed = maxWordsInQuotationAllowed;
    }

    @Override
    public boolean keepArgument(Argument argument, Debate parentDebate)
            throws IOException
    {
        SortedMap<String, Integer> quotesLengthsMap = new TreeMap<>();
        for (String quote : findAllQuotedText(argument.getText())) {
            quotesLengthsMap.put(quote, quote.length());
        }

        // no quotes = alright
        if (quotesLengthsMap.isEmpty()) {
            return true;
        }

        LinkedHashMap<String, Integer> longestFirst = CollectionUtils
                .sortByValue(quotesLengthsMap, false);
        String longestQuote = longestFirst.entrySet().iterator().next().getKey();

        // we allow maximum N words
        boolean result = longestQuote.split("\\s+").length > maxWordsInQuotationAllowed;

//        if (result) {
//            System.out.println("Too long: '" + longestQuote + "'   " + longestFirst);
//        }

        return result;
    }

    /**
     * Returns all strings between pairs of double quotes
     *
     * @param s string
     * @return list of strings (without the quotes); may be empty but never null
     */
    public static List<String> findAllQuotedText(String s)
    {
        List<String> result = new ArrayList<>();
        int offset = -1;
        boolean noMoreLeft = false;

        while (!noMoreLeft) {
            int openingParenthesisIndex = s.indexOf('"', offset);
            if (openingParenthesisIndex > offset) {
                int closingParenthesisIndex = s.indexOf('"', openingParenthesisIndex + 1);

                if (closingParenthesisIndex > offset) {
                    String textBetween = s
                            .substring(openingParenthesisIndex + 1, closingParenthesisIndex);

                    result.add(textBetween);
                    offset = closingParenthesisIndex + 1;
                }
                else {
                    noMoreLeft = true;
                }
            }
            else {
                noMoreLeft = true;
            }
        }

        return result;
    }

//    public static void main(String[] args)
    //    {
    //        String s =
    //                "\" ...what parent does not want his or her child to have access to literature, philosophy and the arts? Who thinks those are dispensable luxuries for educated professionals in an advanced society?\" "
    //                        + "When a dancer \"from the\" New York City Ballet is paid a multi-million dollar salary and a baseball player has to hope for patron donations and funding from the NEA, then you can ask rhetorical questions such as these. "
    //                        + "In the United States having a Government job means working at the Post Office, or having some bureaucratic position pushing paper around. In Europe a Government job can mean you are a stage hand or costume person at La Scala Opera House. They have VERY different ideas as to the importance of the arts and what it says about them as people. Parents do not teach their children that the arts are important or else these programs wouldn't be falling apart. Even the music and film industries in the US aren't about art as much as they are about fame, fashion, and stardom. The fact is none of the arts are seen as viable professions in our country, but merely hobbies to pursue during your free time while your real career ambitions lean towards Wall Street or Corporate America where you can manipulate the markets and make your billions. This is the state of our \"culture\". ";
    //
    //        String s2 = "The fascination with French, in particular, is a social hangover from the era when French was the \"language of culture\" in Europe and upper class Americans felt they and their children needed to learn French in order to climb the social ladder. The \"language of culture\" in Europe and elsewhere since WWII has been English.\" and dummy text ...";
    //
    //        System.out.println(findAllQuotedText(s2));
    //
    //    }
}
