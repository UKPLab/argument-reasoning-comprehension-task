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

import com.google.common.collect.TreeBasedTable;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MACEHelper;
import de.tudarmstadt.ukp.experiments.pipeline.gold.MTurkOutputReader;
import de.tudarmstadt.ukp.experiments.pipeline.gold.SingleWorkerAssignment;
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentsFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.MathException;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computes the human upper-bound values as reported in Figure 4
 * <p>
 * (c) 2017 Ivan Habernal
 */
public class Step10bUpperBoundStatistics
{

    private static final Pattern WARRANT_PATTERN = Pattern
            .compile("^Answer\\.(?<reasonClaimWarrantId>\\d+_\\d+_[^_]+)_warrant_group$");

    private static final Pattern FAMILIAR = Pattern
            .compile("^Answer\\.(?<reasonClaimWarrantId>\\d+_\\d+_[^_]+)_familiarGroup$");

    public static void annotateWithGoldLabels(List<File> mTurkOutputCSVFile,
            File originalXMLArgumentsFile, File outputFile, double maceThreshold,
            WorkerAssignmentsFilter assignmentsFilter, File workerStatisticsOutputCSV)
            throws Exception
    {
        MTurkOutputReader outputReader = new MTurkOutputReader(true,
                mTurkOutputCSVFile.toArray(new File[mTurkOutputCSVFile.size()]));

        // {argument ID: worker assignments}
        // these are sorted by date
        SortedSet<SingleWorkerAssignment<WorkerAccuracy>> assignments = new TreeSet<>();

        DescriptiveStatistics overallAccuracy = new DescriptiveStatistics();

        SortedMap<String, DescriptiveStatistics> familiarityAssignments = new TreeMap<>();

        Map<String, Double> allWorkerIds = new HashMap<>();

        for (Map<String, String> row : outputReader) {
            int education = Integer.valueOf(row.get("Answer.education"));
            int training = Integer.valueOf(row.get("Answer.training"));

            String workerId = row.get("workerid");
            String acceptTimeString = row.get("assignmentaccepttime");

            if (allWorkerIds.containsKey(workerId)) {
                System.err.println("Worker has more than one submission, reject: " + workerId
                        + ", assignment: " + row.get("assignmentid") + " hitid: " + row
                        .get("hitid"));
            }
            else {

                Date time = new SimpleDateFormat(MACEHelper.DATE_FORMAT_PATTERN, Locale.ENGLISH)
                        .parse(acceptTimeString);

                List<String> workersCorrectAnswers = new ArrayList<>();
                List<String> workersIncorrectAnswers = new ArrayList<>();

                DescriptiveStatistics familiarValues = new DescriptiveStatistics();

                // correct answers
                for (String columnName : row.keySet()) {
                    Matcher warrantMatcher = WARRANT_PATTERN.matcher(columnName);
                    if (warrantMatcher.matches()) {
                        String reasonClaimWarrantId = warrantMatcher.group("reasonClaimWarrantId");

                        boolean correct = (1 == Integer.valueOf(row.get(columnName)));
                        if (correct) {
                            workersCorrectAnswers.add(reasonClaimWarrantId);
                        }
                        else {
                            workersIncorrectAnswers.add(reasonClaimWarrantId);
                        }
                    }

                    Matcher familiarMatcher = FAMILIAR.matcher(columnName);
                    if (familiarMatcher.matches()) {

                        int value = 0;
                        switch (row.get(columnName)) {
                        case "somewhat":
                            value = 1;
                            break;
                        case "no":
                            value = 0;
                            break;
                        case "yes":
                            value = 2;
                            break;
                        }

                        familiarValues.addValue(value);
                    }
                }

                familiarityAssignments.put(workerId, familiarValues);

                if (workersCorrectAnswers.size() + workersIncorrectAnswers.size() != 10) {
                    throw new IllegalStateException("Someting wrong...");
                }

                // compute accuracy
                double accuracy = (double) workersCorrectAnswers.size() / 10.0;

                System.out.println(workerId);
                System.out.println(accuracy);
                overallAccuracy.addValue(accuracy);

                SingleWorkerAssignment<WorkerAccuracy> assignment = new SingleWorkerAssignment<>(
                        workerId, time, new WorkerAccuracy(accuracy, education, training));

                assignments.add(assignment);

                allWorkerIds.put(workerId, accuracy);
            }
        }

        // save worker statistics
        if (workerStatisticsOutputCSV != null) {
            FileUtils.write(workerStatisticsOutputCSV,
                    MACEHelper.saveWorkerStatisticsToCSV(allWorkerIds, mTurkOutputCSVFile.get(0)));
        }

        TreeBasedTable<Integer, Integer, DescriptiveStatistics> educationTrainingAccuracy = TreeBasedTable
                .create();

        TreeBasedTable<Integer, Integer, CorrelationVectors> correlationTable = TreeBasedTable
                .create();

        for (int education = 1; education <= 6; education++) {
            for (int training = 1; training <= 3; training++) {
                educationTrainingAccuracy.put(education, training, new DescriptiveStatistics());
                correlationTable.put(education, training, new CorrelationVectors());
            }
        }

        for (SingleWorkerAssignment<WorkerAccuracy> assignment : assignments) {
            educationTrainingAccuracy
                    .get(assignment.getLabel().education, assignment.getLabel().training)
                    .addValue(assignment.getLabel().accuracy);

            // update correlation table
            CorrelationVectors correlationVectors = correlationTable
                    .get(assignment.getLabel().education, assignment.getLabel().training);
            correlationVectors.x
                    .add(familiarityAssignments.get(assignment.getWorkerID()).getMean());
            correlationVectors.y.add(assignment.getLabel().accuracy);
        }

        System.out.println("Average human upper bound table (Figure 3)");
        printTable(educationTrainingAccuracy);

        System.out.println("Correlation table; not reported");
        printTable2(correlationTable);

        System.out.println(overallAccuracy.getMean());
        System.out.println(overallAccuracy.getStandardDeviation());
        System.out.println(overallAccuracy.getN());
        System.out.println(overallAccuracy.getMin());
        System.out.println(overallAccuracy.getMax());

    }

    static class CorrelationVectors
    {
        final List<Double> x = new ArrayList<>();
        final List<Double> y = new ArrayList<>();
    }

    static void printTable(TreeBasedTable<Integer, Integer, DescriptiveStatistics> table)
    {
        System.out.printf("\t%s%n", StringUtils.join(table.columnKeySet(), "\t\t\t"));
        for (Map.Entry<Integer, Map<Integer, DescriptiveStatistics>> entry : table.rowMap()
                .entrySet()) {
            System.out.printf("%s\t", entry.getKey());
            for (DescriptiveStatistics ds : entry.getValue().values()) {
                System.out.printf("%.2f\t%2f\t%d\t", ds.getMean(), ds.getStandardDeviation(),
                        ds.getN());
            }
            System.out.println();
        }
    }

    static void printTable2(TreeBasedTable<Integer, Integer, CorrelationVectors> table)
    {
        System.out.printf("\t%s%n", StringUtils.join(table.columnKeySet(), "\t\t\t"));
        for (Map.Entry<Integer, Map<Integer, CorrelationVectors>> entry : table.rowMap()
                .entrySet()) {
            System.out.printf("%s\t", entry.getKey());
            List<Double> allX = new ArrayList<>();
            List<Double> allY = new ArrayList<>();

            for (CorrelationVectors ds : entry.getValue().values()) {
                allX.addAll(ds.x);
                allY.addAll(ds.y);
                double[] correlation = computeCorrelation(allX, allY);
                System.out.printf("%.2f\t%.2f\t\t", correlation[0], correlation[1]);
            }
            System.out.println();
        }
    }

    private static double[] computeCorrelation(List<Double> allX, List<Double> allY)
    {
        if (allX.size() < 2) {
            return new double[] { Double.NaN, Double.NaN };
        }

        double[][] matrix = new double[allX.size()][];
        for (int i = 0; i < allX.size(); i++) {
            matrix[i] = new double[2];
            matrix[i][0] = allX.get(i);
            matrix[i][1] = allY.get(i);
        }

        PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation(matrix);

        try {
            double pValue = pearsonsCorrelation.getCorrelationPValues().getEntry(0, 1);
            double correlation = pearsonsCorrelation.getCorrelationMatrix().getEntry(0, 1);

            SpearmansCorrelation sc = new SpearmansCorrelation(new Array2DRowRealMatrix(matrix));

            double[] result = new double[2];

            double pValSC = sc.getRankCorrelation().getCorrelationPValues().getEntry(0, 1);
            double corrSC = sc.getCorrelationMatrix().getEntry(0, 1);

            result[0] = corrSC;
            result[1] = pValSC;

            return result;
        }
        catch (MathException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)
            throws Exception
    {
        annotateWithGoldLabels(Collections
                        .singletonList(new File("mturk/annotation-task/99-upper-bound-task.output.csv")),
                new File(
                        "mturk/annotation-task/data/96-original-warrant-batch-0001-5000-final-1970-good-reason-claim-pairs.xml.gz"),
                null, 1.00, null,
                new File("mturk/annotation-task/99-upper-bound-task.worker-stats.csv"));
    }

    private static class WorkerAccuracy
    {
        private final double accuracy;
        private final int education;
        private final int training;

        public WorkerAccuracy(double accuracy, int education, int training)
        {
            this.accuracy = accuracy;
            this.education = education;
            this.training = training;
        }
    }
}
