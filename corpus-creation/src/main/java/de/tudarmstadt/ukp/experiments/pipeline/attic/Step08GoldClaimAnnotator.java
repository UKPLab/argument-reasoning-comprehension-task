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

package de.tudarmstadt.ukp.experiments.pipeline.attic;

import de.tudarmstadt.ukp.dkpro.argumentation.types.*;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.json.JSONArray;
import org.json.JSONObject;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MACEHelper;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MTurkOutputReader;
import de.tudarmstadt.ukp.experiments.pipeline.gold.SingleWorkerAssignment;
import de.tudarmstadt.ukp.experiments.pipeline.uima.bio.UnitEntry;
import de.tudarmstadt.ukp.experiments.pipeline.uima.bio.UnitSequence;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument.getSentenceID;

/**
 * (c) 2016 Ivan Habernal
 */
@Deprecated // not used
public class Step08GoldClaimAnnotator
{

    public enum SentenceLabel
    {
        CLAIM_I {
            @Override
            public String toString()
            {
                return "Claim-I";
            }
        }, CLAIM_B {
        @Override
        public String toString()
        {
            return "Claim-B";
        }
    }, O
    }

    public static void estimateGoldLabels(File mTurkOutputCSVFile, File originalXMLArgumentsFile,
            File outputFile)
            throws Exception
    {
        // we want filled rows...
        HashSet<String> additionalRequiredFields = new HashSet<>();
        //        additionalRequiredFields.add("workerid");

        MTurkOutputReader outputReader = new MTurkOutputReader(additionalRequiredFields, false,
                mTurkOutputCSVFile);

        // first level; {argument ID: worker assignments}
        // these must be exactly 5 per item
        SortedMap<String, SortedSet<SingleWorkerAssignment<Integer>>> stanceAssignments = new TreeMap<>();

        // second level: arg ID: worker assignments as string
        // must be also 5
        SortedMap<String, SortedSet<SingleWorkerAssignment<JSONArray>>> globalClaimAssignments = new TreeMap<>();

        //        Answer.arg160373_stance_group
        //        Answer.arg247535_stance_group=1
        //        Answer.arg247535_stance_group=1

        for (Map<String, String> row : outputReader) {
            //            System.out.println(row);

            String workerId = row.get("workerid");
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy",
                    Locale.ENGLISH);
            Date time = dateFormat.parse(row.get("assignmentaccepttime"));

            // stances
            for (String columnName : row.keySet()) {
                if (columnName.contains("_stance_group")) {
                    String argId = columnName.replaceAll("Answer.", "")
                            .replaceAll("_stance_group", "");

                    Integer stanceValue = Integer.valueOf(row.get(columnName));

                    // create and fill a new object
                    SingleWorkerAssignment<Integer> stanceAssignment = new SingleWorkerAssignment<>(
                            workerId, time, stanceValue);

                    // and update the global map
                    if (!stanceAssignments.containsKey(argId)) {
                        stanceAssignments
                                .put(argId, new TreeSet<SingleWorkerAssignment<Integer>>());
                    }
                    stanceAssignments.get(argId).add(stanceAssignment);

                    //                    System.out.println(stanceAssignment);
                }
            }

            // claims
            String jsonResults = row.get("Answer.collectedAnnotationResults");

            JSONObject obj = new JSONObject(jsonResults);
            //            System.out.println(obj);
            //            String pageName = obj.getJSONObject("pageInfo").getString("pageName");

            // iterate over argIDs
            for (Object argId : obj.keySet()) {
                JSONObject argumentJsonAnnotations = obj.getJSONObject(argId.toString());
                JSONArray segments = argumentJsonAnnotations.getJSONArray("segments");
                boolean implicitClaim = argumentJsonAnnotations.getBoolean("implicitClaim");

                SingleWorkerAssignment<JSONArray> singleWorkerAssignment = new SingleWorkerAssignment<>(
                        workerId, time, segments);

                if (!globalClaimAssignments.containsKey(argId.toString())) {
                    globalClaimAssignments.put(argId.toString(),
                            new TreeSet<SingleWorkerAssignment<JSONArray>>());
                }
                globalClaimAssignments.get(argId.toString()).add(singleWorkerAssignment);
            }
        }

        // let's have a look
        System.out.println("Args with stance: " + stanceAssignments.size());
        System.out.println("Args with claim: " + globalClaimAssignments.size());

        SortedMap<String, String> estimatedStanceAssignments = MACEHelper.estimateGoldLabels(
                stanceAssignments, 0.95).goldLabels;
        System.out.println(estimatedStanceAssignments);

        // read all arguments from the original corpus
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(originalXMLArgumentsFile);

        SortedMap<String, SortedSet<SingleWorkerAssignment<SentenceLabel>>> globalSentenceAssignments = new TreeMap<>();

        for (StandaloneArgument argument : arguments) {
            // process only annotated ones
            if (globalClaimAssignments.containsKey(argument.getId())) {
                updateSentenceAssignmentsForArgument(argument, globalClaimAssignments,
                        globalSentenceAssignments);
            }
        }

        System.out.println(globalSentenceAssignments);
        System.out.println("----------");

        SortedMap<String, String> goldClaims = MACEHelper.estimateGoldLabels(globalSentenceAssignments, 0.95).goldLabels;
        System.out.println(goldClaims);

        // now add the annotations back to the jcas and save
        List<StandaloneArgument> annotatedArguments = new ArrayList<>();

        SortedSet<String> ignoredArguments = new TreeSet<>();

        for (StandaloneArgument argument : arguments) {
            // process only annotated ones
            if (globalClaimAssignments.containsKey(argument.getId())) {
                SortedMap<String, Sentence> hitArgumentSentenceIDs = StandaloneArgument
                        .extractSentenceIDsAndContent(argument);

                // first assign the annotated stance
                String annotatedStanceStr = estimatedStanceAssignments.get(argument.getId());
                if (annotatedStanceStr == null) {
                    System.out.println(
                            "++ Argument stance problematic (low score from MACE): " + argument
                                    .getText());
                }
                else {
                    int annotatedStanceInt = Integer.valueOf(annotatedStanceStr);
                    argument.setAnnotatedStance(annotatedStanceInt);
                }

                // make sure all sentences are annotated; some might be null due to high disagreement
                Set<String> problematicSentenceIDs = new TreeSet<>();
                // collect estimated annotations for all sentences from this argument
                SortedMap<String, String> argumentSentenceLabels = new TreeMap<>();

                for (String sentenceID : hitArgumentSentenceIDs.keySet()) {
                    if (goldClaims.get(sentenceID) == null) {
                        problematicSentenceIDs.add(sentenceID);
                    }
                    else {
                        argumentSentenceLabels.put(sentenceID, goldClaims.get(sentenceID));
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

                // so we only retain arguments with OK stance and sentence annotations
                if (argument.getAnnotatedStance() != null && problematicSentenceIDs.isEmpty()) {
                    annotateExplicitClaims(argument, argumentSentenceLabels);
                    annotatedArguments.add(argument);
                }
                else {
                    ignoredArguments.add(argument.getId());

                    // but just for inspection, add implicit stance

                }
            }
        }

        System.out.println(ignoredArguments.size()
                + " skipped because of unclear stance or problematic sentences by MACE score. "
                + ignoredArguments);
        System.out.println(annotatedArguments.size() + " arguments were annotated successfully");

        // save annotated arguments to output xml
        XStreamSerializer.serializeToXml(annotatedArguments, outputFile);

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
    private static void updateSentenceAssignmentsForArgument(
            StandaloneArgument argument,
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
                    sentenceAssignments.put(sentenceID,
                            new TreeSet<SingleWorkerAssignment<SentenceLabel>>());
                }
                bufferOfCurrentArgumentSentenceIDs.add(sentenceID);
            }

            //            System.out.println("assignmentObject: " + assignmentObject);
            // array of arrays (multiple segments; each segment is an array of sentence IDs)
            JSONArray segments = assignmentObject.getLabel();

            for (int segmentNo = 0; segmentNo < segments.length(); segmentNo++) {
                JSONArray segment = segments.getJSONArray(segmentNo);

                //                System.out.println("segment: " + segment);

                // the first element is CLAIM-B, the rest ist CLAIM-I
                String firstSentenceID = segment.getString(0);
                SortedSet<SingleWorkerAssignment<SentenceLabel>> workerAssignments = sentenceAssignments
                        .get(firstSentenceID);

                if (workerAssignments == null) {
                    throw new IllegalStateException(
                            "No worker assignments for firstSentenceID " + firstSentenceID);
                }

                workerAssignments.add(new SingleWorkerAssignment<>(workerID, date,
                        SentenceLabel.CLAIM_B));
                // and remove from the buffer of local sentences
                bufferOfCurrentArgumentSentenceIDs.remove(firstSentenceID);

                // and the rest as CLAIM-I
                for (int i = 1; i < segment.length(); i++) {
                    String sentenceID = segment.getString(i);

                    // add to the global map
                    sentenceAssignments.get(sentenceID)
                            .add(new SingleWorkerAssignment<>(workerID, date,
                                    SentenceLabel.CLAIM_I));
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
    private static void annotateExplicitClaims(StandaloneArgument argument,
            SortedMap<String, String> argumentSentenceLabels)
            throws IOException
    {
        if (argument.getAnnotatedStance() == null) {
            throw new IllegalArgumentException("Argument must have a non-null stance");
        }

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

        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);

            // create unique id by combining argument id and sentence position
            String sentenceId = getSentenceID(argument, i);

            UnitEntry sentencePrediction = sequence.getUnits().get(i);

            String tag = sentencePrediction.getTag();

            System.out.printf(Locale.ENGLISH, "%-10s%s%n", tag, sentence.getCoveredText());

            // sequence change
            if (!tag.equals(previousTag)) {
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

        // check whether there are any Claim annotations and add an implicit one
        if (JCasUtil.select(jCas, Claim.class).isEmpty()) {
            Claim implicitClaim = new Claim(jCas, 0, 0);
            implicitClaim.setStance(argument.getAnnotatedStance());
            ArgumentUnitUtils.setIsImplicit(implicitClaim, true);
            implicitClaim.addToIndexes();
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

    //    /**
    //     * Prepares CSV file for MACE (see http://www.isi.edu/publications/licensed-sw/mace/)
    //     *
    //     * @param argumentPairs annotated data
    //     * @param turkerIDs     sorted list of turker IDs
    //     * @return single string in the proper MACE format
    //     */
    //    public static String prepareCSV(List<AnnotatedArgumentPair> argumentPairs,
    //            List<String> turkerIDs)
    //    {
    //
    //    }

    public static void main(String[] args)
            throws Exception
    {
        //        estimateGoldLabels(new File(args[0]), new File(args[1]));

        // 02-pilot-claims
        //        estimateGoldLabels(new File(
        //                        "/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/02-pilot-claims-task.output.csv"),
        //                new File(
        //                        "/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/data/arguments-02-pilot-claims.xml.gz"),
        //                new File(
        //                        "/home/user-ukp/data2/acl2017/08-gold-claims/arguments-02-pilot-claims.xml.gz"));

        // 03-pilot-claims sandbox (= my annotations)
//                        estimateGoldLabels(new File(
//                                        "/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/03-pilot-claims-task.output.sandbox.csv"),
//                                new File(
//                                        "/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/data/arguments-03-pilot-claims-EDUs.xml.gz"),
//                                new File(
//                                        "/home/user-ukp/data2/acl2017/08-gold-standard/arguments-03-pilot-mine.xml.gz"));

        // 03-pilot-claims workers
//        estimateGoldLabels(new File(
//                        "/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/03-pilot-claims-task.output.csv"),
//                new File(
//                        "/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/data/arguments-03-pilot-claims-EDUs.xml.gz"),
//                new File(
//                        "/home/user-ukp/data2/acl2017/08-gold-standard/arguments-03-pilot-workers.xml.gz"));

        // 04-pilot-claims workers
//        estimateGoldLabels(new File(
//                        "/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/04-pilot-claims-task.output.csv"),
//                new File(
//                        "/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/data/arguments-03-pilot-claims-EDUs.xml.gz"),
//                new File(
//                        "/home/user-ukp/data2/acl2017/08-gold-standard/arguments-04-pilot-workers.xml.gz"));

        // 05-pilot-claims workers
        estimateGoldLabels(new File(
                        "/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/05-pilot-claims-task.output.csv"),
                new File(
                        "/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/data/arguments-with-EDU-rfd.xml.gz"),
                new File(
                        "/home/user-ukp/data2/emnlp2017/08-gold-standard/arguments-05-pilot-workers.xml.gz"));


    }
}
