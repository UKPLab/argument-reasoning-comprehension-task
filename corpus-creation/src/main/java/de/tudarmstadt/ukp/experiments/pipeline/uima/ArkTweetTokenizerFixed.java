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

import cmu.arktweetnlp.Twokenize;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.CasAnnotator_ImplBase;

import java.util.List;

/**
 * Uses ArkTweetTokenizer but writes no sentence boundaries.
 *
 * @author Ivan Habernal
 */
public class ArkTweetTokenizerFixed
        extends CasAnnotator_ImplBase
{

    private Type tokenType;

    @Override
    public void typeSystemInit(TypeSystem aTypeSystem)
            throws AnalysisEngineProcessException
    {
        super.typeSystemInit(aTypeSystem);

        tokenType = aTypeSystem.getType(Token.class.getName());
    }

    @Override
    public void process(CAS cas)
            throws AnalysisEngineProcessException
    {
        String text = cas.getDocumentText();

        // NOTE: Twokenize provides a API call that performs a normalization first - this would
        // require a mapping to the text how it is present in the CAS object. Due to HTML escaping
        // that would become really messy, we use the call which does not perform any normalization
        List<String> tokenize = Twokenize.tokenize(text);
        int offset = 0;
        for (String t : tokenize) {
            int start = text.indexOf(t, offset);
            int end = start + t.length();
            createTokenAnnotation(cas, start, end);
            offset = end;
        }

    }

    private void createTokenAnnotation(CAS cas, int start, int end)
    {
        AnnotationFS tokenAnno = cas.createAnnotation(tokenType, start, end);
        cas.addFsToIndexes(tokenAnno);
    }
}
