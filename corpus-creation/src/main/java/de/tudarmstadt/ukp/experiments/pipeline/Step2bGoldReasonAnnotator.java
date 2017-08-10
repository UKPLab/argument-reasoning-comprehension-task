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

import de.tudarmstadt.ukp.dkpro.argumentation.types.*;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.*;
import de.tudarmstadt.ukp.experiments.pipeline.uima.bio.UnitEntry;
import de.tudarmstadt.ukp.experiments.pipeline.uima.bio.UnitSequence;
import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Annotates gold reason spans (Step 2 in the article)
 * <p>
 * (c) 2016 Ivan Habernal
 */
public class Step2bGoldReasonAnnotator
{

    public enum SentenceLabel
    {
        PREMISE_I {
            @Override
            public String toString()
            {
                return "Premise-I";
            }
        }, PREMISE_B {
        @Override
        public String toString()
        {
            return "Premise-B";
        }
    }, O
    }

    public static SortedMap<String, SortedSet<SingleWorkerAssignment<JSONArray>>> loadGlobalReasonAssignments(
            File mTurkOutputCSVFile)
            throws Exception
    {
        MTurkOutputReader outputReader = new MTurkOutputReader(false, mTurkOutputCSVFile);

        // argId, set of assignments -- the json array is a list of annotated segments
        SortedMap<String, SortedSet<SingleWorkerAssignment<JSONArray>>> globalReasonAssignments = new TreeMap<>();

        for (Map<String, String> row : outputReader) {

            String workerId = row.get("workerid");
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy",
                    Locale.ENGLISH);
            Date time = dateFormat.parse(row.get("assignmentaccepttime"));

            // claims
            String jsonResults = row.get("Answer.collectedAnnotationResults");

            JSONObject jsonObject = new JSONObject(jsonResults);

            // iterate over argIDs
            for (Object argIdObject : jsonObject.keySet()) {
                String argId = argIdObject.toString();
                JSONObject argumentJsonAnnotations = jsonObject.getJSONObject(argId);
                JSONArray segments = argumentJsonAnnotations.getJSONArray("segments");

                // we don't need this one in fact...
                boolean noReasons = argumentJsonAnnotations.getBoolean("noReasons");

                SingleWorkerAssignment<JSONArray> singleWorkerAssignment = new SingleWorkerAssignment<>(
                        workerId, time, segments);

                // update the global map
                if (!globalReasonAssignments.containsKey(argId)) {
                    globalReasonAssignments
                            .put(argId, new TreeSet<SingleWorkerAssignment<JSONArray>>());
                }
                globalReasonAssignments.get(argId).add(singleWorkerAssignment);
            }
        }
        return globalReasonAssignments;
    }

    public static SortedMap<String, SortedSet<SingleWorkerAssignment<SentenceLabel>>> loadGlobalSentenceAssignments(
            SortedMap<String, SortedSet<SingleWorkerAssignment<JSONArray>>> globalReasonAssignments,
            List<StandaloneArgument> arguments, WorkerAssignmentsFilter assignmentsFilter)
            throws Exception
    {

        // convert JSON arrays for segments to BIO annotations for each sentence
        SortedMap<String, SortedSet<SingleWorkerAssignment<SentenceLabel>>> globalSentenceAssignments = new TreeMap<>();

        for (StandaloneArgument argument : arguments) {
            // process only annotated ones
            if (globalReasonAssignments.containsKey(argument.getId())) {
                updateSentenceAssignmentsForArgument(argument, globalReasonAssignments,
                        globalSentenceAssignments);
            }
        }

        // any filtering of assignments?
        if (assignmentsFilter != null) {
            globalSentenceAssignments = assignmentsFilter
                    .filterAssignments(globalSentenceAssignments);
        }

        // convert JSON arrays for segments to BIO annotations for each sentence
        return globalSentenceAssignments;
    }

    /**
     * What to do with sentences that are rejected by their low score from MACE?
     * If true, the sentence label will "fallback" to O (no reason)
     * If false, the entire argument will be ignored
     */
    public static final boolean FALLBACK_STRATEGY_O = false;

    public static GoldEstimationResult annotateWithGoldLabels(File mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter, File workerStatisticsOutputCSV)
            throws Exception
    {
        // read all arguments from the original corpus
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(originalXMLArgumentsFile);

        SortedMap<String, SortedSet<SingleWorkerAssignment<JSONArray>>> globalReasonAssignments = loadGlobalReasonAssignments(
                mTurkOutputCSVFile);

        SortedMap<String, SortedSet<SingleWorkerAssignment<SentenceLabel>>> globalSentenceAssignments = loadGlobalSentenceAssignments(
                globalReasonAssignments, arguments, assignmentsFilter);

        MACEHelper.GoldLabelEstimationResultContainer estimatedGoldLabelsContainer = MACEHelper
                .estimateGoldLabels(globalSentenceAssignments, maceThreshold);
        SortedMap<String, String> goldReasons = estimatedGoldLabelsContainer.goldLabels;
        System.out.println(goldReasons);

        // now add the annotations back to the jcas and save
        List<StandaloneArgument> annotatedArguments = new ArrayList<>();

        SortedSet<String> ignoredArguments = new TreeSet<>();

        for (StandaloneArgument argument : arguments) {
            // process only annotated ones
            if (globalReasonAssignments.containsKey(argument.getId())) {
                SortedMap<String, Sentence> hitArgumentSentenceIDs = StandaloneArgument
                        .extractSentenceIDsAndContent(argument);

                // make sure all sentences are annotated; some might be null due to high disagreement
                Set<String> problematicSentenceIDs = new TreeSet<>();
                // collect estimated annotations for all sentences from this argument
                SortedMap<String, String> argumentSentenceLabels = new TreeMap<>();

                for (String sentenceID : hitArgumentSentenceIDs.keySet()) {
                    if (goldReasons.get(sentenceID) == null) {
                        // what to do with problematic sentences?
                        if (FALLBACK_STRATEGY_O) {
                            // fallback to O
                            argumentSentenceLabels.put(sentenceID, "O");
                        }
                        else {
                            // ignore argument completely
                            problematicSentenceIDs.add(sentenceID);
                        }
                    }
                    else {
                        argumentSentenceLabels.put(sentenceID, goldReasons.get(sentenceID));
                    }
                }

                if (!problematicSentenceIDs.isEmpty()) {
                    System.out.println("-- Problematic sentences (low score from MACE): " + argument
                            .getText());
                    for (String sentenceID : problematicSentenceIDs) {
                        System.out.println(
                                sentenceID + ": " + hitArgumentSentenceIDs.get(sentenceID)
                                        .getCoveredText());
                    }
                }

                // so we only retain arguments sentence annotations
                if (problematicSentenceIDs.isEmpty()) {
                    annotatePremises(argument, argumentSentenceLabels);

                    // ignore arguments with more than 6 reasons (these are rather suspicious)
                    if (JCasUtil.select(argument.getJCas(), Premise.class).size() > 6) {
                        ignoredArguments.add(argument.getId());
                    }
                    else {
                        annotatedArguments.add(argument);
                    }
                }
                else {
                    ignoredArguments.add(argument.getId());
                }
            }
        }

        // save worker statistics
        if (workerStatisticsOutputCSV != null) {
            FileUtils.write(workerStatisticsOutputCSV, MACEHelper
                    .saveWorkerStatisticsToCSV(estimatedGoldLabelsContainer.workerCompetences,
                            mTurkOutputCSVFile));
        }

        System.out.println(ignoredArguments.size()
                + " skipped because of low MACE score for EDUs" + ignoredArguments);
        System.out.println(annotatedArguments.size() + " arguments were annotated successfully");

        // save annotated arguments to output xml
        XStreamSerializer.serializeToXml(annotatedArguments, outputFile);

        return new GoldEstimationResult(annotatedArguments.size(), ignoredArguments.size());

    }

    /**
     * For the given argument, it collects all sentences and their respective MTurk annotations;
     * updates the {@code sentenceAssignments} map
     *
     * @param argument            argument
     * @param claimAssignments    global claim assignments
     * @param sentenceAssignments global sentence assignments
     * @throws IOException exception
     */

    private static void updateSentenceAssignmentsForArgument(StandaloneArgument argument,
            SortedMap<String, SortedSet<SingleWorkerAssignment<JSONArray>>> claimAssignments,
            SortedMap<String, SortedSet<SingleWorkerAssignment<SentenceLabel>>> sentenceAssignments)
            throws IOException
    {
        Set<String> hitArgumentSentenceIDs = StandaloneArgument
                .extractSentenceIDsAndContent(argument).keySet();

        // so we have all sentences for this argument, let's collect their annotations
        SortedSet<SingleWorkerAssignment<JSONArray>> singleWorkerAssignments = claimAssignments
                .get(argument.getId());

        for (SingleWorkerAssignment<JSONArray> assignmentObject : singleWorkerAssignments) {
            String workerID = assignmentObject.getWorkerID();
            Date date = assignmentObject.getAssignmentDate();

            // all sentences for this argument
            SortedSet<String> bufferOfCurrentArgumentSentenceIDs = new TreeSet<>();

            // update global map and local set
            for (String sentenceID : hitArgumentSentenceIDs) {
                // update the map
                if (!sentenceAssignments.containsKey(sentenceID)) {
                    sentenceAssignments
                            .put(sentenceID, new TreeSet<SingleWorkerAssignment<SentenceLabel>>());
                }
                bufferOfCurrentArgumentSentenceIDs.add(sentenceID);
            }

            // array of arrays (multiple segments; each segment is an array of sentence IDs)
            JSONArray segments = assignmentObject.getLabel();

            for (int segmentNo = 0; segmentNo < segments.length(); segmentNo++) {
                JSONArray segment = segments.getJSONArray(segmentNo);

                // the first element is CLAIM-B, the rest ist CLAIM-I
                String firstSentenceID = segment.getString(0);
                SortedSet<SingleWorkerAssignment<SentenceLabel>> workerAssignments = sentenceAssignments
                        .get(firstSentenceID);

                if (workerAssignments == null) {
                    throw new IllegalStateException(
                            "No worker assignments for firstSentenceID " + firstSentenceID);
                }

                workerAssignments
                        .add(new SingleWorkerAssignment<>(workerID, date, SentenceLabel.PREMISE_B));
                // and remove from the buffer of local sentences
                bufferOfCurrentArgumentSentenceIDs.remove(firstSentenceID);

                // and the rest as CLAIM-I
                for (int i = 1; i < segment.length(); i++) {
                    String sentenceID = segment.getString(i);

                    // add to the global map
                    sentenceAssignments.get(sentenceID)
                            .add(new SingleWorkerAssignment<>(workerID, date,
                                    SentenceLabel.PREMISE_I));
                    // and remove from the buffer of local sentences
                    bufferOfCurrentArgumentSentenceIDs.remove(firstSentenceID);
                }
            }

            // now in the local buffer there must be the O's remaining
            for (String sentenceID : bufferOfCurrentArgumentSentenceIDs) {
                // add to the global map
                sentenceAssignments.get(sentenceID)
                        .add(new SingleWorkerAssignment<>(workerID, date, SentenceLabel.O));
                // and remove from the buffer of local sentences
            }
        }
    }

    /**
     * Annotate claims in arguments's JCas with BIO tagging
     *
     * @param argument               argument
     * @param argumentSentenceLabels sentence IDs and estimated labels
     */
    private static void annotatePremises(StandaloneArgument argument,
            SortedMap<String, String> argumentSentenceLabels)
            throws IOException
    {
        JCas jCas = argument.getJCas();

        UnitSequence sequence = new UnitSequence();
        for (Map.Entry<String, String> entry : argumentSentenceLabels.entrySet()) {
            sequence.getUnits().add(new UnitEntry(entry.getKey(), entry.getValue()));
        }

        ArgumentComponent previousComponent = null;
        String previousTag = null;
        Sentence previousSentence = null;

        ArrayList<Sentence> sentences = new ArrayList<>(JCasUtil.select(jCas, Sentence.class));

        if (sentences.size() != argumentSentenceLabels.size()) {
            throw new IllegalArgumentException("JCas contains " + sentences.size()
                    + " sentence but the provided gold annotations had " + argumentSentenceLabels
                    .size());
        }

        System.out.println("+++ Debugging tags for argument " + argument.getId());

        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);

            UnitEntry sentencePrediction = sequence.getUnits().get(i);

            String tag = sentencePrediction.getTag();

            System.out.printf(Locale.ENGLISH, "%-10s%s%n", tag, sentence.getCoveredText());

            // sequence change -- either previous tag was different or there are two consecutive B
            if (!tag.equals(previousTag) || (previousTag.endsWith("-B") && tag.endsWith("-B"))) {
                // transition between O->X-B or X-I->O
                if (!tag.endsWith("-I")) {
                    // close the previous component
                    if (previousComponent != null) {
                        previousComponent.setEnd(previousSentence.getEnd());
                        previousComponent.addToIndexes();

                        previousComponent = null;
                    }
                }

                if (tag.endsWith("-B")) {
                    previousComponent = createAnnotation(tag, jCas);
                    previousComponent.setBegin(sentence.getBegin());

                    // set stance to the claim
                    if (previousComponent instanceof Claim) {
                        ((Claim) previousComponent).setStance(argument.getAnnotatedStance());
                    }
                }
            }

            previousTag = tag;
            previousSentence = sentence;
        }

        // close the last component if it ends on the last token
        if (previousComponent != null) {
            previousComponent.setEnd(previousSentence.getEnd());
            previousComponent.addToIndexes();
        }

        // save back to the argument
        argument.setJCas(jCas);
    }

    private static ArgumentComponent createAnnotation(String fullTag, JCas jCas)
    {
        String tag = fullTag.split("-")[0];

        switch (tag.toLowerCase()) {
        case "premise":
            return new Premise(jCas);
        case "backing":
            return new Backing(jCas);
        case "claim":
            return new Claim(jCas);
        case "rebuttal":
            return new Rebuttal(jCas);
        case "refutation":
            return new Refutation(jCas);
        default:
            throw new IllegalArgumentException("Unknown annotation type " + tag);
        }
    }

    public static void main(String[] args)
            throws Exception
    {
        // 32 batch
        annotateWithGoldLabels(new File(
                        "mturk/annotation-task/32-reasons-batch-0001-5000-2883args-task.output.csv"),
                new File(
                        "mturk/annotation-task/data/22-stance-batch-0001-5000-only-with-clear-stances.xml.gz"),
                new File(
                        "mturk/annotation-task/data/32-reasons-batch-0001-5000-2026args-gold.xml.gz"),
                0.96, null, new File(
                        "mturk/annotation-task/32-reasons-batch-0001-5000-2883args-task.worker-stats.csv"));

    }
}
