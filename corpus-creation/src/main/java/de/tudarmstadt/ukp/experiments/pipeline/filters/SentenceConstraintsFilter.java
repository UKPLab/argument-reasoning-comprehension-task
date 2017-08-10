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

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * (c) 2016 Ivan Habernal
 */
public class SentenceConstraintsFilter
        extends AbstractLinguisticFeaturesFilter
{
    private static final int MAX_SENTENCE_LENGTH = 220;

    @Override
    public boolean keepArgument(JCas jCas)
    {
        List<Sentence> sentences = new ArrayList<>(JCasUtil.select(jCas, Sentence.class));

        // remove one-sentence arguments
        if (sentences.size() == 1) {
            return false;
        }

        for (Sentence s : sentences) {
            if (s.getCoveredText().length() > MAX_SENTENCE_LENGTH) {
                return false;
            }
        }

        return true;
    }
}
