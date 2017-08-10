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

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgumentWithSinglePremise;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MACEHelper;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MTurkOutputReader;
import de.tudarmstadt.ukp.experiments.pipeline.gold.SingleWorkerAssignment;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Annotating alternative warrants
 *
 * @author Ivan Habernal
 */
public class Step5bAlternativeWarrantGoldAnnotator
{

    // for example "Answer.8889573_850_stance_1";
    private static final Pattern STANCE_PATTERN = Pattern
            .compile("^Answer\\.(?<reasonId>\\d+_\\d+)_stance_group$");

    private static final Pattern WARRANT_PATTERN = Pattern
            .compile("^Answer\\.(?<reasonId>\\d+_\\d+)_opposingStance_warrant$");

    private static final Pattern WARRANT_IMPOSSIBLE = Pattern
            .compile("^Answer\\.(?<reasonId>\\d+_\\d+)_opposingStance_warrant_impossible");

    private static final Pattern WARRANT_FAMILIAR = Pattern
            .compile("^Answer\\.(?<reasonId>\\d+_\\d+)_familiarGroup$");

    public static void extractWarrants(List<File> files, File inputFile, File outputFile,
            File workerStatisticsOutputCSV)
            throws Exception
    {
        File[] filesArray = files.toArray(new File[files.size()]);
        MTurkOutputReader outputReader = new MTurkOutputReader(false, filesArray);

        // argId, set of assignments
        SortedMap<String, SortedSet<SingleWorkerAssignment<String>>> globalAssignments = new TreeMap<>();

        // global warrant impossible -- reasonId; workerIds = if present, then marked as impossible
        Map<String, Set<String>> mapWarrantImpossible = new TreeMap<>();

        // reason id < worker id, familiarity >
        Map<String, Map<String, String>> familiarity = new TreeMap<>();

        for (Map<String, String> row : outputReader) {
            System.out.println(row);

            String workerId = row.get("workerid");
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy",
                    Locale.ENGLISH);
            Date time = dateFormat.parse(row.get("assignmentaccepttime"));

            for (String key : row.keySet()) {
                Matcher impossibleWarrantMatcher = WARRANT_IMPOSSIBLE.matcher(key);
                if (impossibleWarrantMatcher.matches()) {
                    String reasonId = impossibleWarrantMatcher.group("reasonId");
                    if ("on".equals(row.get(key))) {
                        mapWarrantImpossible.putIfAbsent(reasonId, new TreeSet<>());
                        mapWarrantImpossible.get(reasonId).add(workerId);

                        // get reason why impossible
                        String impossibleKey = "Answer." + reasonId + "_impossible_why";
                        row.get(impossibleKey);

                        // add empty string
                        globalAssignments.putIfAbsent(reasonId, new TreeSet<>());
                        globalAssignments.get(reasonId)
                                .add(new SingleWorkerAssignment<>(workerId, time,
                                        "IMPOSSIBLE: " + row.get(impossibleKey)));
                    }
                }

                Matcher familiarMatcher = WARRANT_FAMILIAR.matcher(key);
                if (familiarMatcher.matches()) {
                    String reasonId = familiarMatcher.group("reasonId");
                    String value = row.get(key);

                    familiarity.putIfAbsent(reasonId, new TreeMap<>());
                    familiarity.get(reasonId).put(workerId, value);
                }

                //                System.out.println(key);
                Matcher warrantMatcher = WARRANT_PATTERN.matcher(key);
                if (warrantMatcher.matches()) {
                    String reasonId = warrantMatcher.group("reasonId");
                    String text = row.get(key);

                    globalAssignments.putIfAbsent(reasonId, new TreeSet<>());
                    globalAssignments.get(reasonId)
                            .add(new SingleWorkerAssignment<>(workerId, time, text));
                }
            }
        }

        System.out.println(mapWarrantImpossible);

        // no competences, just collect all workers
        Map<String, Double> workerCompetences = new TreeMap<>();
        for (String workerID : MACEHelper.collectWorkerIDs(globalAssignments)) {
            workerCompetences.put(workerID, 0.0);
        }

        if (workerStatisticsOutputCSV != null) {
            FileUtils.write(workerStatisticsOutputCSV, MACEHelper.saveWorkerStatisticsToCSV(
                    workerCompetences, filesArray
            ));
        }

        Map<String, StandaloneArgumentWithSinglePremise> mapReasonIdArgument = new TreeMap<>();

        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(inputFile);

        // load all data
        for (StandaloneArgument standaloneArgument : arguments) {
            for (StandaloneArgumentWithSinglePremise sa : StandaloneArgumentWithSinglePremise
                    .extractPremises(standaloneArgument)) {
                String premiseId = sa.getPremise().getPremiseId();

                if (globalAssignments.containsKey(premiseId)) {
                    mapReasonIdArgument.put(premiseId, sa);
                }
            }
        }

        // storing results of this ReasonClaimWarrantContainer
        List<ReasonClaimWarrantContainer> resultContainer = new ArrayList<>();

        for (Map.Entry<String, SortedSet<SingleWorkerAssignment<String>>> entry : globalAssignments
                .entrySet()) {

            String reasonId = entry.getKey();
            StandaloneArgumentWithSinglePremise sa = mapReasonIdArgument.get(reasonId);

            System.out.printf("====================%n%s\t\t%s [because] %s \t%s; %s%n", reasonId,
                    sa.getStanceOpposingToAnnotatedStance(),
                    sa.getPremise().getGist(),
                    sa.getDebateMetaData().getTitle(), sa.getDebateMetaData()
                            .getDescription()
            );

            List<SingleWorkerAssignment<String>> workerAssignments = new ArrayList<>(
                    entry.getValue());
            workerAssignments.sort(SingleWorkerAssignment.getComparatorByWorkerID());

            for (SingleWorkerAssignment<String> assignment : workerAssignments) {
                // get worker's vote
                String text = assignment.getLabel();

                boolean impossible = mapWarrantImpossible.getOrDefault(reasonId, new TreeSet<>())
                        .contains(assignment.getWorkerID());

                System.out.printf("%s\t%-14s\t(%-9s)\t%s%n", reasonId, assignment.getWorkerID(),
                        familiarity.get(reasonId).get(assignment.getWorkerID()), text);

                Integer workerFamiliarity;
                // familiarity to int
                switch (familiarity.get(reasonId).get(assignment.getWorkerID())) {
                case "no":
                    workerFamiliarity = -1;
                    break;
                case "somewhat":
                    workerFamiliarity = 0;
                    break;
                case "yes":
                    workerFamiliarity = 1;
                    break;
                default:
                    throw new IllegalStateException("unknown familiarity");
                }

                // save only those that are not "impossible"
                if (!impossible) {
                    // create resulting container
                    ReasonClaimWarrantContainer container = new ReasonClaimWarrantContainer(
                            reasonId, sa.getDebateMetaData(), sa.getAnnotatedStance(),
                            sa.getStanceOpposingToAnnotatedStance(),
                            sa.getPremise().getGist(), assignment.getWorkerID(), workerFamiliarity,
                            text);

                    resultContainer.add(container);
                }
            }
        }

        System.out.println("Saved " + resultContainer.size() + " reason-claim pairs");
        XStreamSerializer.serializeReasonsToXml(resultContainer, outputFile);
    }

    public static void main(String[] args)
            throws Exception
    {

        // 71 batch
        extractWarrants(Collections.singletonList(
                new File(
                        "mturk/annotation-task/71-alternative-warrants-batch-0001-5000-4235reasons-001-600-task.output.csv")
                ),
                new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/data/71-alternative-warrants-batch-0001-5000-001-600aw-batch-2390reason-claim-pairs.xml.gz"),
                new File(
                        "mturk/annotation-task/71-alternative-warrants-batch-0001-5000-4235reasons-001-600-task.worker-stats.csv"));
        // 72 batch
        extractWarrants(Collections.singletonList(
                new File(
                        "mturk/annotation-task/72-alternative-warrants-batch-0001-5000-4235reasons-600-1955-task.output.csv")
                ),
                new File(
                        "mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons.xml.gz"),
                new File(
                        "mturk/annotation-task/data/72-alternative-warrants-batch-0001-5000-600-1955w-batch-5342reason-claim-pairs.xml.gz"),
                new File(
                        "mturk/annotation-task/72-alternative-warrants-batch-0001-5000-4235reasons-600-1955-task.worker-stats.csv"));
    }

}
