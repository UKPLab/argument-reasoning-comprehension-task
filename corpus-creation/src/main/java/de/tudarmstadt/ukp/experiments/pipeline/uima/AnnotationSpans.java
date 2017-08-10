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

package de.tudarmstadt.ukp.experiments.pipeline.uima;

import de.tudarmstadt.ukp.dkpro.argumentation.types.ArgumentComponent;
import de.tudarmstadt.ukp.dkpro.argumentation.types.ArgumentUnitUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public class AnnotationSpans
{
    public static AnnotationSpans extractAnnotationSpans(JCas jCas)
    {
        BidiMap sentenceBeginIndexToCharacterIndex = new TreeBidiMap();
        BidiMap sentenceEndIndexToCharacterIndex = new TreeBidiMap();

        List<Sentence> sentences = new ArrayList<>(JCasUtil.select(jCas, Sentence.class));
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);

            sentenceBeginIndexToCharacterIndex.put(i, sentence.getBegin());
            sentenceEndIndexToCharacterIndex.put(i, sentence.getEnd());
        }

        //        System.out.println(sentenceBeginIndexToCharacterIndex);
        //        System.out.println(sentenceEndIndexToCharacterIndex);

        AnnotationSpans annotationSpans = new AnnotationSpans(
                sentenceBeginIndexToCharacterIndex.size());

        Collection<ArgumentComponent> components = JCasUtil.select(jCas, ArgumentComponent.class);

        for (ArgumentComponent component : components) {
            if (!ArgumentUnitUtils.isImplicit(component)) {
                //            System.out.println("=====");
                //            System.out.println(component.getCoveredText());
                int relativeOffset = (int) sentenceBeginIndexToCharacterIndex
                        .getKey(component.getBegin());

                int endingSentenceIndex = (int) sentenceEndIndexToCharacterIndex
                        .getKey(component.getEnd());

                int length = endingSentenceIndex - relativeOffset + 1;

                String type = component.getType().getShortName();

                SingleAnnotationSpan singleAnnotationSpan = new SingleAnnotationSpan(type,
                        relativeOffset, length);

                annotationSpans.getAnnotationSpans().add(singleAnnotationSpan);
            }
        }

        return annotationSpans;
    }

    private final int documentLength;
    private final List<SingleAnnotationSpan> annotationSpans = new ArrayList<>();

    public AnnotationSpans(int documentLength)
    {
        this.documentLength = documentLength;
    }

    public int getDocumentLength()
    {
        return documentLength;
    }

    public List<SingleAnnotationSpan> getAnnotationSpans()
    {
        return annotationSpans;
    }

    @Override public String toString()
    {
        return "AnnotationSpans{" +
                "documentLength=" + documentLength +
                ", annotationSpans=" + annotationSpans +
                '}';
    }

    public static class SingleAnnotationSpan
    {
        private final String type;
        private final int relativeOffset;
        private final int length;

        public SingleAnnotationSpan(String type, int relativeOffset, int length)
        {
            this.type = type;
            this.relativeOffset = relativeOffset;
            this.length = length;
        }

        public String getType()
        {
            return type;
        }

        public int getRelativeOffset()
        {
            return relativeOffset;
        }

        public int getLength()
        {
            return length;
        }

        @Override public String toString()
        {
            return "SingleAnnotationSpan{" +
                    "type='" + type + '\'' +
                    ", relativeOffset=" + relativeOffset +
                    ", length=" + length +
                    '}';
        }
    }

}
