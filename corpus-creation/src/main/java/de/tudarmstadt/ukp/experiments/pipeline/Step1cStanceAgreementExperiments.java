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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.CohenKappaAgreement;
import org.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import org.dkpro.statistics.agreement.distance.NominalDistanceFunction;
import org.dkpro.statistics.agreement.visualization.ContingencyMatrixPrinter;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentFilterRandomized;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static de.tudarmstadt.ukp.experiments.pipeline.Step1bGoldStanceAnnotator.annotateWithGoldLabels;

/**
 * Computes values for "Figure: Agreement scores on 98 comments in stance annotations."
 * Takes some time to finish.
 *
 * @author Ivan Habernal
 */
public class Step1cStanceAgreementExperiments
{
    public static void main(String[] args)
            throws Exception
    {
        final File csvFile = new File(
                "mturk/annotation-task/21-pilot-stance-task.output.csv");
        final File argumentsFile = new File(
                "mturk/annotation-task/data/arguments-with-full-segmentation-rfd.xml.gz");

        TreeBasedTable<Integer, Double, DescriptiveStatistics> table = TreeBasedTable.create();

        for (int crowdSize = 1; crowdSize <= 9; crowdSize++) {
            for (Double maceThreshold : Arrays.asList(0.85, 0.9, 0.95, 1.0)) {
                // ten random repeats
                for (int i = 0; i < 20; i++) {
                    Random random = new Random(i);

                    File crowdExpert1 = File.createTempFile("crowd1", ".xml.gz");
                    File crowdExpert2 = File.createTempFile("crowd2", ".xml.gz");

                    annotateWithGoldLabels(csvFile, argumentsFile, crowdExpert1, maceThreshold,
                            new WorkerAssignmentFilterRandomized(18, 1, crowdSize, random));

                    annotateWithGoldLabels(csvFile, argumentsFile, crowdExpert2, maceThreshold,
                            new WorkerAssignmentFilterRandomized(18, 2, crowdSize, random));

                    double kappa = computeKappa(crowdExpert1, crowdExpert2);

                    if (!table.contains(crowdSize, maceThreshold)) {
                        table.put(crowdSize, maceThreshold, new DescriptiveStatistics());
                    }
                    table.get(crowdSize, maceThreshold).addValue(kappa);

                    FileUtils.forceDelete(crowdExpert1);
                    FileUtils.forceDelete(crowdExpert2);
                }
            }
        }
        printTable(table);

    }

    public static void printTable(TreeBasedTable<Integer, Double, DescriptiveStatistics> table)
    {
        System.out.printf("\t%s%n", StringUtils.join(table.columnKeySet(), "\t\t"));
        for (Map.Entry<Integer, Map<Double, DescriptiveStatistics>> entry : table.rowMap()
                .entrySet()) {
            System.out.printf("%s\t", entry.getKey());
            for (DescriptiveStatistics ds : entry.getValue().values()) {
                System.out.printf("%.2f\t%2f\t", ds.getMean(), ds.getStandardDeviation());
            }
            System.out.println();
        }
    }

    private static double computeKappa(File crowdExpert1, File crowdExpert2)
            throws IOException
    {
        // read all arguments from the original corpus
        List<StandaloneArgument> expert1Arguments = XStreamSerializer
                .deserializeArgumentListFromXML(crowdExpert1);
        List<StandaloneArgument> expert2Arguments = XStreamSerializer
                .deserializeArgumentListFromXML(crowdExpert2);

        SortedMap<String, String> expert1Labels = new TreeMap<>();
        SortedMap<String, String> expert2Labels = new TreeMap<>();
        for (StandaloneArgument argument : expert1Arguments) {
            expert1Labels.put(argument.getId(), String.valueOf(argument.getAnnotatedStanceAsInt()));
        }
        for (StandaloneArgument argument : expert2Arguments) {
            expert2Labels.put(argument.getId(), String.valueOf(argument.getAnnotatedStanceAsInt()));
        }

        System.out.println(expert1Labels);
        System.out.println(expert2Labels);

        // intersection
        SortedSet<String> intersectionIDs = new TreeSet<>();
        intersectionIDs.addAll(expert1Labels.keySet());
        intersectionIDs.retainAll(expert2Labels.keySet());

        CodingAnnotationStudy study = new CodingAnnotationStudy(2);

        for (String id : intersectionIDs) {
            String expert1 = expert1Labels.get(id);
            String expert2 = expert2Labels.get(id);
            study.addItem(expert1, expert2);

            System.out.println(expert1 + "\t" + expert2);
        }

        KrippendorffAlphaAgreement alpha = new KrippendorffAlphaAgreement(study,
                new NominalDistanceFunction());
        CohenKappaAgreement kappa = new CohenKappaAgreement(study);

        System.out.println("Alpha: " + alpha.calculateAgreement());
        System.out.println("Kappa: " + kappa.calculateAgreement());

        new ContingencyMatrixPrinter().print(System.out, study);

        return kappa.calculateAgreement();
    }
}
