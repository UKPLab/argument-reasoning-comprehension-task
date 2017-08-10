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

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.Frequency;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Out-of-vocabulary filter
 * <p>
 * (c) 2016 Ivan Habernal
 */
public class OOVFilter
        extends AbstractLinguisticFeaturesFilter
{
    private final SortedSet<String> vocabulary;

    private final static int THRESHOLD = 35;

    Frequency frequency = new Frequency();

    public OOVFilter()
            throws IOException
    {
        vocabulary = new TreeSet<String>();

        File wordFile = new File("/usr/share/dict/words");
        if (!wordFile.exists()) {
            throw new IOException("File not found " + wordFile);
        }

        // load vocabulary
        vocabulary.addAll(FileUtils.readLines(wordFile, "utf-8"));
    }

    @Override
    boolean keepArgument(JCas jCas)
    {
        Collection<Token> tokens = JCasUtil.select(jCas, Token.class);

        int oovWords = 0;

        for (Token token : tokens) {
            if (!vocabulary.contains(token.getCoveredText())) {
                oovWords++;
            }
        }

        frequency.addValue(oovWords);
        //        System.out.println(frequency);

        return oovWords <= THRESHOLD;
    }
}
