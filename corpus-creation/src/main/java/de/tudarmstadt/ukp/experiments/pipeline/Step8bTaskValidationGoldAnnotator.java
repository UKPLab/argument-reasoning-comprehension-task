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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.Frequency;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MACEHelper;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MTurkOutputReader;
import de.tudarmstadt.ukp.experiments.pipeline.gold.SingleWorkerAssignment;
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentsFilter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * (c) 2017 Ivan Habernal
 */
public class Step8bTaskValidationGoldAnnotator
{

    private static final Pattern WARRANT_PATTERN = Pattern
            .compile("^Answer\\.(?<reasonClaimWarrantId>\\d+_\\d+_[^_]+)_warrant_group$");

    public static SortedMap<String, String> annotateWithGoldLabels(File mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter)
            throws Exception
    {
        return annotateWithGoldLabels(Collections.singletonList(mTurkOutputCSVFile),
                originalXMLArgumentsFile, outputFile, maceThreshold, assignmentsFilter, null);
    }

    public static SortedMap<String, String> annotateWithGoldLabels(List<File> mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter)
            throws Exception
    {
        return annotateWithGoldLabels(mTurkOutputCSVFile, originalXMLArgumentsFile, outputFile,
                maceThreshold, assignmentsFilter, null);
    }

    public static SortedMap<String, String> annotateWithGoldLabels(List<File> mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter, File workerStatisticsOutputCSV)
            throws Exception
    {
        MTurkOutputReader outputReader = new MTurkOutputReader(false,
                mTurkOutputCSVFile.toArray(new File[mTurkOutputCSVFile.size()]));

        // {argument ID: worker assignments}
        // these are sorted by date
        SortedMap<String, SortedSet<SingleWorkerAssignment<Integer>>> warrantAssignments = new TreeMap<>();

        for (Map<String, String> row : outputReader) {

            String workerId = row.get("workerid");
            String acceptTimeString = row.get("assignmentaccepttime");

            Date time = new SimpleDateFormat(MACEHelper.DATE_FORMAT_PATTERN, Locale.ENGLISH)
                    .parse(acceptTimeString);

            // stances
            for (String columnName : row.keySet()) {
                Matcher impossibleWarrantMatcher = WARRANT_PATTERN.matcher(columnName);
                if (impossibleWarrantMatcher.matches()) {
                    String reasonClaimWarrantId = impossibleWarrantMatcher
                            .group("reasonClaimWarrantId");

                    int stanceValue = Integer.valueOf(row.get(columnName));

                    //                    if (stanceValue == 0 || stanceValue == 1) {

                    // create and fill a new object
                    SingleWorkerAssignment<Integer> stanceAssignment = new SingleWorkerAssignment<>(
                            workerId, time, stanceValue);

                    warrantAssignments.putIfAbsent(reasonClaimWarrantId, new TreeSet<>());
                    warrantAssignments.get(reasonClaimWarrantId).add(stanceAssignment);
                    //                    }
                }
            }
        }

        // any filtering of assignments?
        if (assignmentsFilter != null) {
            warrantAssignments = assignmentsFilter.filterAssignments(warrantAssignments);
        }

        //        if (true)
        //            System.exit(1);

        MACEHelper.GoldLabelEstimationResultContainer estimatedResultsContainer = MACEHelper
                .estimateGoldLabels(warrantAssignments, maceThreshold);

        // save worker statistics
        if (workerStatisticsOutputCSV != null) {
            FileUtils.write(workerStatisticsOutputCSV, MACEHelper
                    .saveWorkerStatisticsToCSV(estimatedResultsContainer.workerCompetences,
                            mTurkOutputCSVFile.get(0)));
        }

        List<ReasonClaimWarrantContainer> allData = XStreamSerializer
                .deserializeReasonListFromXML(originalXMLArgumentsFile);

        Frequency frequency = new Frequency();

        Map<String, CorrectedInstance> correctedInstanceMap = loadCorrectedInstancesFromCSV();

        List<ReasonClaimWarrantContainer> result = new ArrayList<>();

        for (ReasonClaimWarrantContainer reasonClaimWarrantContainer : allData) {
            String id = reasonClaimWarrantContainer.getReasonClaimWarrantId();

            if (estimatedResultsContainer.goldLabels.get(id) != null) {
                Integer estimatedValue = Integer
                        .valueOf(estimatedResultsContainer.goldLabels.get(id));

                // it might have been corrected by an expert
                if (correctedInstanceMap.containsKey(id)) {
                    estimatedValue = 1;
                    CorrectedInstance correctedInstance = correctedInstanceMap.get(id);
                    reasonClaimWarrantContainer
                            .setOriginalWarrant(correctedInstance.originalWarrant);
                    reasonClaimWarrantContainer
                            .setAlternativeWarrant(correctedInstance.alternativeWarrant);
                }

                // unclear might have got a strong vote but if the majority was for it...
                // (2 out of three) -- let' fix it
                long numberOfOnes = warrantAssignments.get(id).stream()
                        .filter(it -> it.getLabel() == 1).count();
                if (estimatedValue == 2 && numberOfOnes >= 2) {
                    estimatedValue = 1;
                }

                if (1 == estimatedValue) {
                    result.add(reasonClaimWarrantContainer);
                }

                // update stats
                frequency.addValue((long) estimatedValue);

                /*
                if (estimatedValue == 2 && numberOfOnes > 0) {
                    System.out.printf(Locale.ENGLISH,
                            "%s\t%d %s And since [explanation], I claim that %s%n\t 1 %s%n\t 0 %s\n",
                            reasonClaimWarrantContainer.getReasonClaimWarrantId(), estimatedValue,
                            reasonClaimWarrantContainer.getReasonGist(),
                            reasonClaimWarrantContainer.getAnnotatedStance(),
                            reasonClaimWarrantContainer.getOriginalWarrant(),
                            reasonClaimWarrantContainer.getAlternativeWarrant());

                    //                    System.out.println(warrantAssignments.get(id));
                    //                    System.out.println(numberOfOnes);
                }
                */

            }
        }

        // save annotated arguments to output xml
        System.out.println("Saving " + result.size() + " items");

        System.out.println(frequency);
        XStreamSerializer.serializeReasonsToXml(result, outputFile);

        // debug
        for (ReasonClaimWarrantContainer c : result) {

        }

        return estimatedResultsContainer.goldLabels;
    }

    public static Map<String, CorrectedInstance> loadCorrectedInstancesFromCSV()
            throws IOException
    {
        Map<String, CorrectedInstance> result = new TreeMap<>();
        // read corrections
        List<String> fileNames = Arrays.asList("mturk/annotation-task/97-post-validation.csv",
                "mturk/annotation-task/97-post-validation2.csv");
        for (String fileName : fileNames) {
            CSVParser csvParser = CSVParser
                    .parse(new File(fileName), Charset.forName("utf-8"), CSVFormat.RFC4180);

            Iterator<CSVRecord> iterator = csvParser.iterator();

            while (iterator.hasNext()) {
                CSVRecord firstLine = iterator.next();
                CSVRecord secondLine = iterator.next();
                CSVRecord thirdLine = iterator.next();

                String id = firstLine.get(0);
                boolean skipRecord = "x".equals(firstLine.get(1)) || firstLine.get(1).isEmpty();

                if (!skipRecord) {
                    int correctLabel = Integer.valueOf(firstLine.get(1));

                    //                String[] split = secondLine.get(2).split("\\W", 2);
                    //                System.out.println(Arrays.toString(split));
                    int secondLineLabel = Integer.valueOf(secondLine.get(2).split("\\W", 2)[0]);
                    String secondLineText = secondLine.get(2).split("\\W", 2)[1];

                    int thirdLineLabel = Integer.valueOf(thirdLine.get(2).split("\\W", 2)[0]);
                    String thirdLineText = thirdLine.get(2).split("\\W", 2)[1];

                    System.out.println(correctLabel);
                    System.out.println(secondLineLabel + ", " + secondLineText);
                    System.out.println(thirdLineLabel + ", " + thirdLineText);

                    String originalWarrant;
                    String alternativeWarrant;
                    if (correctLabel == secondLineLabel) {
                        originalWarrant = secondLineText;
                        alternativeWarrant = thirdLineText;
                    }
                    else {
                        originalWarrant = thirdLineText;
                        alternativeWarrant = secondLineText;
                    }

                    CorrectedInstance correctedInstance = new CorrectedInstance(originalWarrant,
                            alternativeWarrant);
//                    System.out.println(correctedInstance);

                    result.put(id, correctedInstance);
                }
            }

            System.out.println(result.size());
        }
        return result;
    }

    static class CorrectedInstance
    {
        public final String originalWarrant;
        public final String alternativeWarrant;

        public CorrectedInstance(String originalWarrant, String alternativeWarrant)
        {
            this.originalWarrant = originalWarrant;
            this.alternativeWarrant = alternativeWarrant;
        }

        @Override
        public String toString()
        {
            return "CorrectedInstance{" + "originalWarrant='" + originalWarrant + '\''
                    + ", alternativeWarrant='" + alternativeWarrant + '\'' + '}';
        }
    }

    public static void main(String[] args)
            throws Exception
    {
        // 95 pilot
        //        annotateWithGoldLabels(
        //                Arrays.asList(
        //                        new File("mturk/annotation-task/95-validation-task-pilot-task.output.csv")),
        //                new File(
        //                        "mturk/annotation-task/data/92-original-warrant-batch-0001-5000-2447-good-reason-claim-pairs.xml.gz"),
        //                new File("/tmp/out.gz"),
        //                1.00, null,
        //                new File("mturk/annotation-task/95-validation-task-pilot-task.worker-stats.csv"));

        // 95 pilot
        annotateWithGoldLabels(Collections.singletonList(new File(
                        "mturk/annotation-task/96-validation-task-batch-0001-5000-full-batch-2447-task.output.csv")),
                new File(
                        "mturk/annotation-task/data/92-original-warrant-batch-0001-5000-2447-good-reason-claim-pairs.xml.gz"),
                new File(
                        "mturk/annotation-task/data/96-original-warrant-batch-0001-5000-final-1970-good-reason-claim-pairs.xml.gz"),
                1.00, null, new File(
                        "mturk/annotation-task/96-validation-task-batch-0001-5000-full-batch-2447-task.worker-stats.csv"));

        //        loadCorrectedInstancesFromCSV();
    }
}
