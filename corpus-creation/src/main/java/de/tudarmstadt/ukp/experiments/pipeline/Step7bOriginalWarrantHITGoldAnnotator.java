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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * (c) 2017 Ivan Habernal
 */
public class Step7bOriginalWarrantHITGoldAnnotator
{
    private static final Pattern ORIGINAL_REASON = Pattern
            .compile("^Answer\\.(?<reasonClaimWarrantId>\\d+_\\d+_[^_]+)_fixed_warrant$");

    private static final Pattern IMPOSSIBLE = Pattern.compile(
            "^Answer\\.(?<reasonClaimWarrantId>\\d+_\\d+_[^_]+)_fixed_warrant_impossible$");

    private static final Pattern IMPOSSIBLE_WHY = Pattern
            .compile("^Answer\\.(?<reasonClaimWarrantId>\\d+_\\d+_[^_]+)_impossible_why$");

    public static void annotateWithGoldLabels(List<File> files, File originalXMLArgumentsFile,
            File outputFile, File workerStatisticsOutputCSV)
            throws Exception
    {
        File[] filesArray = files.toArray(new File[files.size()]);
        MTurkOutputReader outputReader = new MTurkOutputReader(false, filesArray);

        // argId, set of assignments
        SortedMap<String, SortedSet<SingleWorkerAssignment<String>>> globalAssignments = new TreeMap<>();
        SortedMap<String, SortedSet<SingleWorkerAssignment<String>>> impossibleAssignments = new TreeMap<>();

        SortedMap<String, Double> mockWorkerCompetences = new TreeMap<>();

        for (Map<String, String> row : outputReader) {
            //            System.out.println(row);

            String workerId = row.get("workerid");
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy",
                    Locale.ENGLISH);
            Date time = dateFormat.parse(row.get("assignmentaccepttime"));

            // add mock competence
            mockWorkerCompetences.put(workerId, 0.0);

            for (String key : row.keySet()) {
                Matcher impossibleWarrantMatcher = ORIGINAL_REASON.matcher(key);
                if (impossibleWarrantMatcher.matches()) {
                    String reasonClaimWarrantId = impossibleWarrantMatcher
                            .group("reasonClaimWarrantId");
                    String value = row.get(key);
                    // add empty string
                    globalAssignments.putIfAbsent(reasonClaimWarrantId, new TreeSet<>());
                    globalAssignments.get(reasonClaimWarrantId)
                            .add(new SingleWorkerAssignment<>(workerId, time, value));
                }

                Matcher impossibleWhyMatcher = IMPOSSIBLE_WHY.matcher(key);
                if (impossibleWhyMatcher.matches()) {
                    String reasonClaimWarrantId = impossibleWhyMatcher
                            .group("reasonClaimWarrantId");
                    String value = "IMPOSSIBLE " + row.get(key);

                    impossibleAssignments.putIfAbsent(reasonClaimWarrantId, new TreeSet<>());
                    impossibleAssignments.get(reasonClaimWarrantId)
                            .add(new SingleWorkerAssignment<>(workerId, time, value));
                }
            }
        }

        System.out.println(globalAssignments);

        if (workerStatisticsOutputCSV != null) {
            FileUtils.write(workerStatisticsOutputCSV,
                    MACEHelper.saveWorkerStatisticsToCSV(mockWorkerCompetences, filesArray));
        }

        // storing results of this ReasonClaimWarrantContainer
        List<ReasonClaimWarrantContainer> resultContainer = new ArrayList<>();

        List<ReasonClaimWarrantContainer> allData = XStreamSerializer
                .deserializeReasonListFromXML(originalXMLArgumentsFile);

        Frequency twistedWarrants = new Frequency();

        for (ReasonClaimWarrantContainer reasonClaimWarrantContainer : allData) {
            String id = reasonClaimWarrantContainer.getReasonClaimWarrantId();

            if (globalAssignments.containsKey(id) || impossibleAssignments.containsKey(id)) {
                debugItem(reasonClaimWarrantContainer, globalAssignments.get(id),
                        impossibleAssignments.get(id));

                // and save
                if (globalAssignments.containsKey(id)) {
                    reasonClaimWarrantContainer
                            .setOriginalWarrant(globalAssignments.get(id).first().getLabel());

                    resultContainer.add(reasonClaimWarrantContainer);
                    twistedWarrants.addValue("Original_warrant_ok");
                }

                if (impossibleAssignments.containsKey(id)) {
                    twistedWarrants.addValue("Original_warrant_impossible");
                }
            }
        }

        System.out.println("Saved " + resultContainer.size() + " reason-claim pairs");
        XStreamSerializer.serializeReasonsToXml(resultContainer, outputFile);

        System.out.println(twistedWarrants);
    }

    private static void debugItem(ReasonClaimWarrantContainer reasonClaimWarrantContainer,
            Set<SingleWorkerAssignment<String>> globalAssignments,
            SortedSet<SingleWorkerAssignment<String>> impossibleAssignments)
    {
        System.out.printf(Locale.ENGLISH, "%s And since ***[explanation] %s***, I claim that %s%n",
                reasonClaimWarrantContainer.getReasonGist(),
                reasonClaimWarrantContainer.getAlternativeWarrant(),
                reasonClaimWarrantContainer.getAnnotatedStance());
        if (globalAssignments != null) {
            for (SingleWorkerAssignment<String> assignment : globalAssignments) {
                System.out.println(assignment.getWorkerID() + ": " + assignment.getLabel());
            }
        }
        if (impossibleAssignments != null) {
            for (SingleWorkerAssignment<String> assignment : impossibleAssignments) {
                System.out.println(
                        assignment.getWorkerID() + ": IMPOSSIBLE: " + assignment.getLabel());
            }
        }
        System.out.println("=====================");
    }

    public static void main(String[] args)
            throws Exception
    {
        // 90 pilot
        //        annotateWithGoldLabels(Arrays.asList(
        //                new File("mturk/annotation-task/90-original-warrant-pilot-task.output.csv")),
        //                new File(
        //                        "mturk/annotation-task/data/80-aw-validation-batch-0001-5000-all-batches-3791reason-claim-pairs.xml.gz"),
        //                new File("/tmp/out.gz"),
        //                new File("mturk/annotation-task/90-original-warrant-pilot-task.worker-stats.csv"));

        // 91 batch
        //        annotateWithGoldLabels(Arrays.asList(
        //                new File("mturk/annotation-task/91-original-warrant-batch-0001-5000-batch-0100-0600-task.output.csv")),
        //                new File(
        //                        "mturk/annotation-task/data/80-aw-validation-batch-0001-5000-all-batches-3791reason-claim-pairs.xml.gz"),
        //                new File("/tmp/out.gz"),
        //                new File("mturk/annotation-task/91-original-warrant-batch-0001-5000-batch-0100-0600-task.worker-stats.csv"));

        // 92 batch
        //        annotateWithGoldLabels(Arrays.asList(
        //                new File("mturk/annotation-task/92-original-warrant-batch-0001-5000-batch-0600-2613-task.output.csv")),
        //                new File(
        //                        "mturk/annotation-task/data/80-aw-validation-batch-0001-5000-all-batches-3791reason-claim-pairs.xml.gz"),
        //                new File("/tmp/out.gz"),
        //                new File("mturk/annotation-task/92-original-warrant-batch-0001-5000-batch-0600-2613-task.worker-stats.csv"));

        // both batches
        annotateWithGoldLabels(Arrays.asList(
                new File(
                        "mturk/annotation-task/91-original-warrant-batch-0001-5000-batch-0100-0600-task.output.csv"),
                new File(
                        "mturk/annotation-task/92-original-warrant-batch-0001-5000-batch-0600-2613-task.output.csv")),
                new File(
                        "mturk/annotation-task/data/80-aw-validation-batch-0001-5000-all-batches-3791reason-claim-pairs.xml.gz"),
                new File("mturk/annotation-task/data/92-original-warrant-batch-0001-5000-2447-good-reason-claim-pairs.xml.gz"),
                null);

    }
}
