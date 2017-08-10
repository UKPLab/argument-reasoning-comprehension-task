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

import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.Frequency;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MACEHelper;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MTurkOutputReader;
import de.tudarmstadt.ukp.experiments.pipeline.gold.SingleWorkerAssignment;
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentsFilter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivan Habernal
 */
public class Step6bAlternativeWarrantValidationHITGoldAnnotator
{

    private static final Pattern ORIGINAL_REASON = Pattern
            .compile("^Answer\\.(?<reasonClaimWarrantId>\\d+_\\d+_[^_]+)_original_reason_group$");

    private static final Pattern HOW_HARD = Pattern
            .compile("^Answer\\.(?<reasonClaimWarrantId>\\d+_\\d+_[^_]+)_familiarGroup$");

    private static final Pattern HOW_LOGICAL = Pattern
            .compile("^Answer\\.(?<reasonClaimWarrantId>\\d+_\\d+_[^_]+)_logicalGroup$");

    public static SortedMap<String, String> annotateWithGoldLabels(List<File> files,
            List<File> originalXMLArgumentsFiles, File outputFile, File workerStatisticsOutputCSV,
            double maceThreshold, WorkerAssignmentsFilter assignmentsFilter)
            throws Exception
    {
        File[] filesArray = files.toArray(new File[files.size()]);
        MTurkOutputReader outputReader = new MTurkOutputReader(false, filesArray);

        // argId, set of assignments
        SortedMap<String, SortedSet<SingleWorkerAssignment<Integer>>> globalAssignments = new TreeMap<>();

        SortedMap<String, SortedSet<SingleWorkerAssignment<Integer>>> globalHard = new TreeMap<>();

        SortedMap<String, SortedSet<SingleWorkerAssignment<Integer>>> globalLogic = new TreeMap<>();

        for (Map<String, String> row : outputReader) {
            System.out.println(row);

            String workerId = row.get("workerid");
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy",
                    Locale.ENGLISH);
            Date time = dateFormat.parse(row.get("assignmentaccepttime"));

            for (String key : row.keySet()) {
                Matcher impossibleWarrantMatcher = ORIGINAL_REASON.matcher(key);
                if (impossibleWarrantMatcher.matches()) {
                    String reasonClaimWarrantId = impossibleWarrantMatcher
                            .group("reasonClaimWarrantId");
                    int value = applyLabelMergingStrategy(Integer.valueOf(row.get(key)));
                    // add empty string
                    globalAssignments.putIfAbsent(reasonClaimWarrantId, new TreeSet<>());
                    globalAssignments.get(reasonClaimWarrantId)
                            .add(new SingleWorkerAssignment<>(workerId, time, value));
                }

                Matcher howHardMatcher = HOW_HARD.matcher(key);
                if (howHardMatcher.matches()) {
                    String reasonClaimWarrantId = howHardMatcher.group("reasonClaimWarrantId");
                    int value = Integer.valueOf(row.get(key));

                    globalHard.putIfAbsent(reasonClaimWarrantId, new TreeSet<>());
                    globalHard.get(reasonClaimWarrantId)
                            .add(new SingleWorkerAssignment<>(workerId, time, value));
                }

                //                System.out.println(key);
                Matcher howLogicalMatcher = HOW_LOGICAL.matcher(key);
                if (howLogicalMatcher.matches()) {
                    String reasonClaimWarrantId = howLogicalMatcher.group("reasonClaimWarrantId");
                    int value = Integer.valueOf(row.get(key));

                    globalLogic.putIfAbsent(reasonClaimWarrantId, new TreeSet<>());
                    globalLogic.get(reasonClaimWarrantId)
                            .add(new SingleWorkerAssignment<>(workerId, time, value));
                }
            }
        }

        if (assignmentsFilter != null) {
            globalAssignments = assignmentsFilter.filterAssignments(globalAssignments);
        }

        System.out.println(globalAssignments);

        MACEHelper.GoldLabelEstimationResultContainer maceEstimates = MACEHelper
                .estimateGoldLabels(globalAssignments, maceThreshold);

        if (workerStatisticsOutputCSV != null) {
            FileUtils.write(workerStatisticsOutputCSV, MACEHelper
                    .saveWorkerStatisticsToCSV(maceEstimates.workerCompetences, filesArray));
        }

        // get "good" data = marked as "1" ("0" was the distractor, "2" both irrelevant)
        System.out.println(maceEstimates.goldLabels);
        Frequency frequency = new Frequency();
        maceEstimates.goldLabels.values().forEach(s -> frequency.addValue(s != null ? s : "-"));
        System.out.println(frequency);

        // storing results of this ReasonClaimWarrantContainer
        List<ReasonClaimWarrantContainer> resultContainer = new ArrayList<>();

        List<ReasonClaimWarrantContainer> allData = new ArrayList<>();
        for (File originalXMLArgumentsFile : originalXMLArgumentsFiles) {
            allData.addAll(
                    XStreamSerializer.deserializeReasonListFromXML(originalXMLArgumentsFile));
        }

        Set<String> uniqueIDs = new TreeSet<>();
        // check we have unique ID's!!!
        for (ReasonClaimWarrantContainer claimWarrantContainer : allData) {
            String id = claimWarrantContainer.getReasonClaimWarrantId();
            if (uniqueIDs.contains(id)) {
                throw new IllegalStateException("Repeated ID: " + id);
            }

            uniqueIDs.add(id);
        }

        for (ReasonClaimWarrantContainer reasonClaimWarrantContainer : allData) {
            String id = reasonClaimWarrantContainer.getReasonClaimWarrantId();

            if (maceEstimates.goldLabels.containsKey(id)) {
                String label = maceEstimates.goldLabels.get(id);

                double hardScore = getHardScore(id, globalHard, maceEstimates.workerCompetences);
                double logicScore = getLogicScore(id, globalLogic, maceEstimates.workerCompetences);

                if ("1".equals(label)) {
                    // create output reasonClaimWarrantContainer
                    reasonClaimWarrantContainer.setHardScore(hardScore);
                    reasonClaimWarrantContainer.setLogicScore(logicScore);
                    resultContainer.add(reasonClaimWarrantContainer);

                    debugResult(reasonClaimWarrantContainer, label, hardScore, logicScore);
                }
            }
        }

        System.out.println("Saved " + resultContainer.size() + " reason-claim pairs");
        XStreamSerializer.serializeReasonsToXml(resultContainer, outputFile);

        return maceEstimates.goldLabels;
    }

    private static void debugResult(ReasonClaimWarrantContainer reasonClaimWarrantContainer,
            String label, double hardScore, double logicScore)
    {
        System.out.printf(Locale.ENGLISH, "%s\t%.1f\t%.1f\t%s\t%s\t%s%n", label, hardScore,
                logicScore, reasonClaimWarrantContainer.getAlternativeWarrant(),
                reasonClaimWarrantContainer.getReasonGist(),
                reasonClaimWarrantContainer.getStanceOpposingToAnnotatedStance());
    }

    /**
     * We want to merge 0 and 2 into one class (since both are irrelevant answers...)
     *
     * @param label 0, 1, or 2
     * @return 0 or 1
     */
    private static int applyLabelMergingStrategy(int label)
    {
        if (label == 0 || label == 2) {
            return 0;
        }
        else if (label == 1) {
            return 1;
        }

        throw new IllegalArgumentException("Unexpected value: " + label);
    }

    private static double getLogicScore(String id,
            SortedMap<String, SortedSet<SingleWorkerAssignment<Integer>>> globalLogic,
            Map<String, Double> workerCompetences)
    {
        if (!globalLogic.containsKey(id)) {
            return Double.NaN;
        }

        double competencesSum = 0;
        for (SingleWorkerAssignment<Integer> swa : globalLogic.get(id)) {
            competencesSum += workerCompetences.get(swa.getWorkerID());
        }

        double finalScore = 0;
        for (SingleWorkerAssignment<Integer> swa : globalLogic.get(id)) {
            // normalized weight
            double competence = workerCompetences.get(swa.getWorkerID()) / competencesSum;
            finalScore += competence * Double.valueOf(swa.getLabel());
        }

        return finalScore;
    }

    private static double getHardScore(String id,
            SortedMap<String, SortedSet<SingleWorkerAssignment<Integer>>> globalHard,
            Map<String, Double> workerCompetences)
    {
        if (!globalHard.containsKey(id)) {
            return Double.NaN;
        }

        double competencesSum = 0;
        for (SingleWorkerAssignment<Integer> swa : globalHard.get(id)) {
            competencesSum += workerCompetences.get(swa.getWorkerID());
        }

        double finalScore = 0;
        for (SingleWorkerAssignment<Integer> swa : globalHard.get(id)) {
            // normalized weight
            double competence = workerCompetences.get(swa.getWorkerID()) / competencesSum;
            finalScore += competence * Double.valueOf(swa.getLabel());
        }

        return finalScore;
    }

    public static void main(String[] args)
            throws Exception
    {
        // 80+81+82 batch merged
        annotateWithGoldLabels(Arrays.asList(
                new File("mturk/annotation-task/80-aw-validation-pilot-task.output.csv"), new File(
                        "mturk/annotation-task/81-001-600aw-validation-batch-0050-2390-reason-claim-pairs-task.output.csv"),
                new File(
                        "mturk/annotation-task/82-600-1955aw-validation-batch-5342-reason-claim-pairs-task.output.csv")),
                Arrays.asList(new File(
                                "mturk/annotation-task/data/71-alternative-warrants-batch-0001-5000-001-600aw-batch-2390reason-claim-pairs-with-distracting-reasons.xml.gz"),
                        new File(
                                "/home/habi/IdeaProjects/emnlp2017/mturk/annotation-task/data/72-alternative-warrants-batch-0001-5000-600-1955w-batch-5342reason-claim-pairs-with-distracting-reasons.xml.gz")),
                new File(
                        "mturk/annotation-task/data/80-aw-validation-batch-0001-5000-all-batches-3791reason-claim-pairs.xml.gz"),
                new File(
                        "mturk/annotation-task/80-aw-validation-batch-all2390-reason-claim-pairs-task.worker-stats.csv"),
                0.95, null);

        // 82 batch TODO merge all three!!
        //        annotateWithGoldLabels(Arrays.asList(
        //                ), new File(
        //                        "mturk/annotation-task/data/71-alternative-warrants-batch-0001-5000-001-600aw-batch-2390reason-claim-pairs-with-distracting-reasons.xml.gz"),
        //                new File("/tmp/out.gz"),
        //                new File("mturk/annotation-task/82-600-1955aw-validation-batch-5342-reason-claim-pairs-task.worker-stats.csv"),
        //                0.90, null);
    }

}
