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

package de.tudarmstadt.ukp.experiments.pipeline;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import de.tudarmstadt.ukp.dkpro.core.tokit.ParagraphSplitter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Argument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Debate;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.uima.ArkTweetTokenizerFixed;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Text segmenter; consists of three consecutive steps, this is the first step
 *
 * @author Ivan Habernal
 */
public class Step0bTextSegmenterA
{
    private static AnalysisEngineDescription pipelineSingleton;

    /**
     * Creates a tokenizing pipeline
     *
     * @throws IOException exception
     */
    private static AnalysisEngineDescription getPipeline()
            throws IOException
    {
        if (pipelineSingleton == null) {
            try {
                pipelineSingleton = AnalysisEngineFactory.createEngineDescription(
                        AnalysisEngineFactory.createEngineDescription(ParagraphSplitter.class,
                                ParagraphSplitter.PARAM_SPLIT_PATTERN,
                                ParagraphSplitter.SINGLE_LINE_BREAKS_PATTERN),
                        AnalysisEngineFactory.createEngineDescription(ArkTweetTokenizerFixed.class),
                        AnalysisEngineFactory.createEngineDescription(StanfordSegmenter.class,
                                StanfordSegmenter.PARAM_WRITE_TOKEN, false,
                                StanfordSegmenter.PARAM_ZONE_TYPES,
                                Paragraph.class.getCanonicalName()));
            }
            catch (ResourceInitializationException e) {
                throw new IOException();
            }
        }

        return pipelineSingleton;
    }

    private static void copyParagraphAndTokenAnnotations(JCas source, JCas target)
    {
        if (!source.getDocumentText().equals(target.getDocumentText())) {
            throw new IllegalArgumentException("Source and target have different content");
        }

        for (Paragraph p : JCasUtil.select(source, Paragraph.class)) {
            Paragraph paragraph = new Paragraph(target);
            paragraph.setBegin(p.getBegin());
            paragraph.setEnd(p.getEnd());
            paragraph.addToIndexes();
        }

        for (Token t : JCasUtil.select(source, Token.class)) {
            Token token = new Token(target);
            token.setBegin(t.getBegin());
            token.setEnd(t.getEnd());
            token.addToIndexes();
        }
    }

    private static void annotate(StandaloneArgument argument, PrintWriter outputTXTFile,
            Map<String, List<List<String>>> segmentedEDUs)
            throws IOException
    {
        try {
            // original jcas, for automatic segmenting with tokens and sentences
            JCas originalJCas = initializeJCas(argument);

            SimplePipeline.runPipeline(originalJCas, getPipeline());

            // for re-annotation with manual sentences, paragraph, and tokens
            JCas segmentedJCas = initializeJCas(argument);
            copyParagraphAndTokenAnnotations(originalJCas, segmentedJCas);

            // now for each sentence collect tokens and run EDU segmenter
            for (Sentence sentence : JCasUtil.select(originalJCas, Sentence.class)) {

                List<Token> tokens = JCasUtil.selectCovered(Token.class, sentence);
                List<String> tokenWords = new ArrayList<>();

                for (Token token : tokens) {
                    tokenWords.add(token.getCoveredText());
                }

                String text = StringUtils.join(tokenWords, " ");
                String sentenceID = argument.getId() + "_" + sentence.getBegin();

                // either output for external segmenting or annotate
                if (outputTXTFile != null && segmentedEDUs == null) {
                    outputTXTFile.println(sentenceID + "\t" + text);
                    System.out.println("Writing " + sentenceID);
                }
                else if (outputTXTFile == null && segmentedEDUs != null) {

                    List<List<String>> collectedEDUs = segmentedEDUs.get(sentenceID);

                    if (collectedEDUs == null) {
                        throw new IllegalStateException(
                                "Cannot find EDUs for sentence " + sentenceID);
                    }

                    reAnnotatedSentencesFromEDUs(segmentedJCas, collectedEDUs, tokenWords, tokens);
                }
                else {
                    throw new IllegalStateException();
                }
            }

            // save back
            argument.setJCas(segmentedJCas);
        }
        catch (UIMAException | IOException e) {
            throw new IOException(e);
        }
    }

    private static void reAnnotatedSentencesFromEDUs(JCas segmentedJCas,
            List<List<String>> collectedEDUs, List<String> tokenWords, List<Token> tokens)
    {
        int tokensInEDUs = 0;
        for (List<String> edu : collectedEDUs) {
            tokensInEDUs += edu.size();
        }

        // list of [begin, end] of all new sentences to be annotated
        List<List<Integer>> allNewSentenceBoundaries = new ArrayList<>();

        // there was some weird re-tokenization, let's annotated the entire segment as sentence
        // (fallback solution, doesn't hurt)
        if (tokensInEDUs != tokens.size()) {
            Integer begin = tokens.get(0).getBegin();
            Integer end = tokens.get(tokens.size() - 1).getEnd();
            allNewSentenceBoundaries.add(Arrays.asList(begin, end));
        }
        else {
            int currentOpeningTokenIndex = 0;
            int tokenIndex = 0;
            for (List<String> edu : collectedEDUs) {
                tokenIndex += edu.size();

                Integer begin = tokens.get(currentOpeningTokenIndex).getBegin();
                Integer end = tokens.get(tokenIndex - 1).getEnd();
                currentOpeningTokenIndex = tokenIndex;

                allNewSentenceBoundaries.add(Arrays.asList(begin, end));
            }
        }

        System.out.println("Original tokens: " + tokenWords);
        System.out.println("EDUs: " + collectedEDUs);
        System.out.println("New sentences boundaries: " + allNewSentenceBoundaries);

        // annotate all new sentences
        for (List<Integer> sentenceBoundaries : allNewSentenceBoundaries) {
            Sentence s = new Sentence(segmentedJCas);
            Integer begin = sentenceBoundaries.get(0);
            Integer end = sentenceBoundaries.get(1);
            s.setBegin(begin);
            s.setEnd(end);
            s.addToIndexes();
        }

    }

    private static JCas initializeJCas(StandaloneArgument argument)
            throws UIMAException
    {
        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentLanguage("en");
        jCas.setDocumentText(argument.getText());

        DocumentMetaData documentMetaData = DocumentMetaData.create(jCas);
        documentMetaData.setDocumentId(argument.getId());
        documentMetaData.setDocumentTitle(argument.getDebateMetaData().getTitle());
        documentMetaData.addToIndexes();
        return jCas;
    }

    @SuppressWarnings("unchecked")
    public static void runSegmenter(File inputDir, File outputFile, File outputTXTFile,
            File segmentedEDUsFile)
            throws IOException, ClassNotFoundException
    {
        List<StandaloneArgument> arguments = readFromDebates(inputDir);

        // create EDUSegmenter instance
        PrintWriter outputTXTFilePw = null;
        if (outputTXTFile != null) {
            outputTXTFilePw = new PrintWriter(outputTXTFile);
        }

        Map<String, List<List<String>>> segmentedEDUs = null;
        if (segmentedEDUsFile != null) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(segmentedEDUsFile));
            segmentedEDUs = (Map<String, List<List<String>>>) ois.readObject();
        }

        for (StandaloneArgument argument : arguments) {
            annotate(argument, outputTXTFilePw, segmentedEDUs);
        }

        IOUtils.closeQuietly(outputTXTFilePw);

        // save as XML
        XStreamSerializer.serializeToXml(arguments, outputFile);
    }

    /**
     * Reader
     *
     * @param inputDir dir with XML files
     * @return list of arguments
     */
    private static List<StandaloneArgument> readFromDebates(File inputDir)
    {
        List<StandaloneArgument> result = new ArrayList<>();

        // read all debates and filter them
        for (File file : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            Debate debate = XStreamSerializer.deserializeFromXML(file);

            for (Argument argument : debate.getArgumentList()) {
                result.add(new StandaloneArgument(argument, debate));
            }
        }

        return result;
    }

    public static void main(String[] args)
            throws Exception
    {
        File inputDir = new File(args[0]);
        File outputFile = new File(args[1]);
        File parentDir = outputFile.getParentFile();
        parentDir.mkdirs();

        File tempTXT = new File(args[2]);
        File tempEDUs = new File(args[3]);

        // first phase: export
        runSegmenter(inputDir, outputFile, tempTXT, null);

        // after this is finished, you must run ExternalEDUSegmenter from the module "segmenter"
    }


}
