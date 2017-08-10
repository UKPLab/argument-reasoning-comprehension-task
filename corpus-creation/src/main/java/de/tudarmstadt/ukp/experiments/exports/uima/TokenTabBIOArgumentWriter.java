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
package de.tudarmstadt.ukp.experiments.exports.uima;

import de.tudarmstadt.ukp.dkpro.argumentation.types.BIOTokenArgumentAnnotation;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Exports each document into a text file in which each line contains a token and its BIO-tag
 * delimited by a tab character. For example
 * <pre>
 * Lorem tab Claim-B
 * ipsum tab Claim-I
 * dolor tab O
 * sit tab O
 * amet tab O
 * , tab O
 * consectetur tab Premise-B
 * adipiscing tab Premise-I
 * elit tab Premise-B
 * ...
 * </pre>
 *
 * @author Ivan Habernal
 */
public class TokenTabBIOArgumentWriter
        extends JCasFileWriter_ImplBase
{

    @Override public void process(JCas jCas)
            throws AnalysisEngineProcessException
    {
        try (OutputStream docOS = getOutputStream(jCas, ".txt")) {
            Collection<BIOTokenArgumentAnnotation> bioTokenArgumentAnnotations = JCasUtil
                    .select(jCas, BIOTokenArgumentAnnotation.class);

            if (bioTokenArgumentAnnotations.isEmpty()) {
                throw new IllegalStateException(
                        "No annotations of type BIOTokenArgumentAnnotation found. Make sure you run ArgumentTokenBIOAnnotator in the pipeline.");
            }

            PrintWriter pw = new PrintWriter(docOS, true);

            for (BIOTokenArgumentAnnotation tokenAnnotation : bioTokenArgumentAnnotations) {
                pw.printf("%s\t%s%n", tokenAnnotation.getCoveredText(), tokenAnnotation.getTag());
            }

            IOUtils.closeQuietly(docOS);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}
