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

package de.tudarmstadt.ukp.experiments.pipeline.utils;

import de.tudarmstadt.ukp.dkpro.argumentation.types.*;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils for printing annotated argument to other formats (plain text, HTML, LaTeX, etc.)
 *
 * @author Ivan Habernal
 */
public class ArgumentPrinterUtils
{
    /**
     * Returns true if the token has a preceding whitespace in the original document
     *
     * @param token token
     * @param jCas  jcas
     * @return boolen
     */
    public static boolean hasSpaceBefore(Token token, JCas jCas)
    {
        // select previous token(s)
        List<Token> prevTokens = JCasUtil.selectPreceding(jCas, Token.class, token, 1);

        Paragraph paragraph = JCasUtil.selectCovering(jCas, Paragraph.class, token).iterator()
                .next();

        return !prevTokens.isEmpty() && (prevTokens.iterator().next().getEnd() != token.getBegin())
                && (token.getBegin() != paragraph.getBegin());
    }

    /**
     * Returns true, if the argument component annotation begins at this token
     *
     * @param t    token
     * @param jCas jcas
     * @return boolean
     */
    public static ArgumentComponent argAnnotationBegins(Token t, JCas jCas)
    {
        List<ArgumentComponent> argumentAnnotations = new ArrayList<>();

        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Claim.class, t.getBegin(), t.getEnd()));
        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Backing.class, t.getBegin(), t.getEnd()));
        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Premise.class, t.getBegin(), t.getEnd()));
        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Rebuttal.class, t.getBegin(), t.getEnd()));
        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Refutation.class, t.getBegin(), t.getEnd()));

        if (!argumentAnnotations.isEmpty() && argumentAnnotations.get(0).getBegin() == t
                .getBegin()) {
            return argumentAnnotations.get(0);
        }

        return null;
    }

    /**
     * Returns true, if the argument component annotation ends at this token
     *
     * @param t    token
     * @param jCas jcas
     * @return boolean
     */
    public static boolean argAnnotationEnds(Token t, JCas jCas)
    {
        List<ArgumentComponent> argumentAnnotations = new ArrayList<>();

        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Claim.class, t.getBegin(), t.getEnd()));
        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Backing.class, t.getBegin(), t.getEnd()));
        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Premise.class, t.getBegin(), t.getEnd()));
        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Rebuttal.class, t.getBegin(), t.getEnd()));
        argumentAnnotations
                .addAll(JCasUtil.selectCovering(jCas, Refutation.class, t.getBegin(), t.getEnd()));

        return !argumentAnnotations.isEmpty() && argumentAnnotations.get(0).getEnd() == t.getEnd();
    }

    /**
     * Returns a covering sentence if it starts at the token, null otherwise
     *
     * @param t token
     * @return sentence or null
     */
    public static Sentence sentenceStartsOnToken(Token t)
    {
        List<Sentence> sentences = JCasUtil.selectCovering(Sentence.class, t);

        return (!sentences.isEmpty() && sentences.get(0).getBegin() == t.getBegin()) ?
                sentences.get(0) :
                null;
    }

    public static int getSentenceNumber(Sentence sentence, JCas jCas)
    {
        ArrayList<Sentence> sentences = new ArrayList<>(JCasUtil.select(jCas, Sentence.class));

        for (int i = 0; i < sentences.size(); i++) {
            Sentence s = sentences.get(i);

            if (s.getBegin() == sentence.getBegin()) {
                return i;
            }
        }

        throw new IllegalStateException();
    }
}
