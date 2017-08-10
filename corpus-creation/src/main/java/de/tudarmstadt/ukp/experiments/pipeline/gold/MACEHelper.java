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

package de.tudarmstadt.ukp.experiments.pipeline.gold;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import de.tudarmstadt.ukp.experiments.pipeline.utils.CollectionUtils;
import de.tudarmstadt.ukp.experiments.pipeline.utils.TableUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Ivan Habernal
 */
public class MACEHelper
{
    public static String saveWorkerStatisticsToCSV(Map<String, Double> workerCompetences,
            File ... mTurkOutputCSVFiles)
            throws Exception
    {
        MTurkOutputReader outputReader = new MTurkOutputReader(false, mTurkOutputCSVFiles);

        Table<String, String, String> table = TreeBasedTable.create();

        Map<String, List<Map<String, Object>>> collected = new TreeMap<>();
        for (String workerId : workerCompetences.keySet()) {
            collected.put(workerId, new ArrayList<Map<String, Object>>());
        }

        // average submission time + std.dev.
        // number of HITs
        // all comments concatenated
        for (Map<String, String> row : outputReader) {
            String workerId = row.get("workerid");

            if (workerCompetences.keySet().contains(workerId)) {

                Date accept = DATE_FORMAT.parse(row.get("assignmentaccepttime"));
                Date submit = DATE_FORMAT.parse(row.get("assignmentsubmittime"));
                int seconds = (int) (submit.getTime() - accept.getTime()) / 1000;
                String feedback = row.get("Answer.feedback");

                int nonReasonsCounter = 0;
                for (String columnName : row.keySet()) {
                    if (columnName.contains("_no_reason")) {
                        nonReasonsCounter++;
                    }
                }

                Map<String, Object> singleHITCollected = new HashMap<>();
                singleHITCollected.put("seconds", seconds);
                singleHITCollected.put("feedback", feedback);
                // get HITid and assignment id
                singleHITCollected.put("hitid", row.get("hitid"));
                singleHITCollected.put("assignmentid", row.get("assignmentid"));
                singleHITCollected.put("nonreasons", nonReasonsCounter);

                collected.get(workerId).add(singleHITCollected);
            }
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : collected.entrySet()) {
            // collect all
            int numberOfHITs = entry.getValue().size();
            DescriptiveStatistics ds = new DescriptiveStatistics();
            List<String> feedbacks = new ArrayList<>();
            int nonReasonCount = 0;

            String firstHITid = null;
            String firstAssignmentID = null;

            for (Map<String, Object> singleEntry : entry.getValue()) {
                int seconds = (int) singleEntry.get("seconds");
                String feedback = (String) singleEntry.get("feedback");

                ds.addValue(seconds);

                if (feedback != null && !feedback.trim().isEmpty()) {
                    feedbacks.add(feedback.trim());
                }

                if (firstAssignmentID == null && firstHITid == null) {
                    firstHITid = (String) singleEntry.get("hitid");
                    firstAssignmentID = (String) singleEntry.get("assignmentid");
                }

                nonReasonCount += (int) singleEntry.get("nonreasons");
            }

            table.put(entry.getKey(), "hits", String.valueOf(numberOfHITs));
            table.put(entry.getKey(), "timeAvg", String.format(Locale.ENGLISH, "%.0f", ds.getMean()));
            table.put(entry.getKey(), "timeMin", String.format(Locale.ENGLISH, "%.0f", ds.getMin()));
            table.put(entry.getKey(), "timeMax", String.format(Locale.ENGLISH, "%.0f", ds.getMax()));
            table.put(entry.getKey(), "timeStdev",
                    String.format(Locale.ENGLISH, "%.0f", ds.getStandardDeviation()));
            table.put(entry.getKey(), "feedbacks", StringUtils.join(feedbacks, ";"));
            table.put(entry.getKey(), "competence",
                    String.format(Locale.ENGLISH, "%.3f", workerCompetences.get(entry.getKey())));
            table.put(entry.getKey(),"firstHIT", firstHITid != null ? firstHITid : "null");
            table.put(entry.getKey(),"firstAssignment", firstAssignmentID != null ? firstAssignmentID : "null");
            table.put(entry.getKey(), "nonReasonCouns", String.valueOf(nonReasonCount));
        }

        String result = TableUtils.tableToCsv(table);

        return result;
    }

    public static class GoldLabelEstimationResultContainer
    {
        public SortedMap<String, String> goldLabels;
        public Map<String, Double> workerCompetences;
    }

    /**
     * Date format for assignment time
     */
    public static final String DATE_FORMAT_PATTERN = "EEE MMM d HH:mm:ss zzz yyyy";

    /**
     * Not thread safe!!!!
     */
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            DATE_FORMAT_PATTERN,
            Locale.ENGLISH);

    public static <T> GoldLabelEstimationResultContainer estimateGoldLabels(
            SortedMap<String, SortedSet<SingleWorkerAssignment<T>>> allAssignments,
            double maceThreshold)
            throws IOException
    {
        SortedMap<String, String> goldLabels = new TreeMap<>();

        List<String> sortedWorkerIDs = new ArrayList<>(collectWorkerIDs(allAssignments));

        System.out.println("All worker IDs: " + sortedWorkerIDs);

        // for each item we need an array of annotations, i.e.
        // label1,,,label1,label2,label1,,,
        // whose size is number of annotators and annotators are identified by position (array index)

        // storing the resulting lines
        List<String> csvLines = new ArrayList<>();

        for (SortedSet<SingleWorkerAssignment<T>> singleArgumentAssignments : allAssignments
                .values()) {
            // for storing individual assignments
            String[] assignmentsArray = new String[sortedWorkerIDs.size()];
            // fill with empty strings
            Arrays.fill(assignmentsArray, "");

            for (SingleWorkerAssignment<T> assignment : singleArgumentAssignments) {
                // get the turker index
                int turkerIndex = Collections
                        .binarySearch(sortedWorkerIDs, assignment.getWorkerID());

                // and set the label on the correct position in the array
                assignmentsArray[turkerIndex] = assignment.getLabel().toString();
            }

            // concatenate with comma
            String line = StringUtils.join(assignmentsArray, ",");

            System.out.println(line);

            csvLines.add(line);
        }

        // add empty line at the end
        csvLines.add("");

        String preparedCSV = StringUtils.join(csvLines, "\n");

        // save CSV and run MACE
        Path tmpDir = Files.createTempDirectory("mace");
        File maceInputFile = new File(tmpDir.toFile(), "input.csv");
        FileUtils.writeStringToFile(maceInputFile, preparedCSV, "utf-8");

        File outputPredictions = new File(tmpDir.toFile(), "predictions.txt");
        File outputCompetence = new File(tmpDir.toFile(), "competence.txt");

        // run MACE
        MACE.main(
                new String[] { "--iterations", "500", "--threshold", String.valueOf(maceThreshold),
                        "--restarts", "50", "--outputPredictions",
                        outputPredictions.getAbsolutePath(), "--outputCompetence",
                        outputCompetence.getAbsolutePath(), maceInputFile.getAbsolutePath() });

        // read back the predictions and competence
        List<String> predictions = FileUtils.readLines(outputPredictions, "utf-8");

        // check the output
        if (predictions.size() != allAssignments.size()) {
            throw new IllegalStateException(
                    "Wrong size of the predicted file; expected " + allAssignments.size()
                            + " lines but was " + predictions.size());
        }

        String competenceRaw = FileUtils.readFileToString(outputCompetence, "utf-8");
        String[] competence = competenceRaw.split("\t");
        if (competence.length != sortedWorkerIDs.size()) {
            throw new IllegalStateException(
                    "Expected " + sortedWorkerIDs.size() + " competence number, got "
                            + competence.length);
        }

        // rank turkers by competence
        Map<String, Double> turkerIDCompetenceMap = new TreeMap<>();
        for (int i = 0; i < sortedWorkerIDs.size(); i++) {
            turkerIDCompetenceMap.put(sortedWorkerIDs.get(i), Double.valueOf(competence[i]));
        }

        // sort by value descending
        Map<String, Double> sortedCompetences = CollectionUtils
                .sortByValue(turkerIDCompetenceMap, false);
        System.out.println("Sorted turker competences: " + sortedCompetences);

        // assign the gold label and competence
        ArrayList<String> allStanceAssignmentsIDsSorted = new ArrayList<>(
                allAssignments.keySet());

        for (int i = 0; i < allAssignments.size(); i++) {
            String argID = allStanceAssignmentsIDsSorted.get(i);
            String goldLabel = predictions.get(i).trim();

            // might be empty
            if (!goldLabel.isEmpty()) {
                // so far the gold label has format aXXX_aYYY_a1, aXXX_aYYY_a2, or aXXX_aYYY_equal
                // strip now only the gold label
                goldLabels.put(argID, goldLabel);
            }
            else {
                System.err.println("Empty gold label for ID " + argID);
                goldLabels.put(argID, null);
            }
        }

        GoldLabelEstimationResultContainer result = new GoldLabelEstimationResultContainer();
        result.goldLabels = goldLabels;
        result.workerCompetences = sortedCompetences;

        return result;
    }

    public static <T> SortedSet<String> collectWorkerIDs(
            SortedMap<String, SortedSet<SingleWorkerAssignment<T>>> assignments)
    {
        SortedSet<String> result = new TreeSet<>();
        for (SortedSet<SingleWorkerAssignment<T>> set : assignments.values()) {
            for (SingleWorkerAssignment assignment : set) {
                result.add(assignment.getWorkerID());
            }
        }

        return result;
    }
}
