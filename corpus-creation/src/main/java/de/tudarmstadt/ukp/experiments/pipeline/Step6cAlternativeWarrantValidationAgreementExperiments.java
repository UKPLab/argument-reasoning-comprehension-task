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

import com.github.habernal.confusionmatrix.ConfusionMatrix;
import com.google.common.collect.TreeBasedTable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.CohenKappaAgreement;
import org.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import org.dkpro.statistics.agreement.distance.NominalDistanceFunction;
import de.tudarmstadt.ukp.experiments.pipeline.gold.WorkerAssignmentFilterRandomized;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static de.tudarmstadt.ukp.experiments.pipeline.Step1cStanceAgreementExperiments.printTable;

/**
 * @author Ivan Habernal
 */
public class Step6cAlternativeWarrantValidationAgreementExperiments
{

    public static void main(String[] args)
            throws Exception
    {
        final File csvFile = new File(
                "mturk/annotation-task/80-aw-validation-pilot-task.output.csv");
        final File argumentsFile = new File(
                "mturk/annotation-task/data/71-alternative-warrants-batch-0001-5000-001-600aw-batch-2390reason-claim-pairs-with-distracting-reasons.xml.gz");

        TreeBasedTable<Integer, Double, DescriptiveStatistics> table = TreeBasedTable.create();

        final int requiredAssignmentsSize = 14;

        IntStream.range(1, 8).parallel().forEach(crowdSize -> {

            Arrays.asList(0.75, 0.80, 0.85, 0.9, 0.95, 1.0).parallelStream().forEach(maceThreshold -> {
                // ten random repeats
                for (int i = 0; i < 20; i++) {
                    Random random = new Random(i);

                    try {
                        File crowdExpert1 = File.createTempFile("crowd1", ".xml.gz");
                        File crowdExpert2 = File.createTempFile("crowd2", ".xml.gz");

                        SortedMap<String, String> goldEstimationResult1 = Step6bAlternativeWarrantValidationHITGoldAnnotator
                                .annotateWithGoldLabels(Collections.singletonList(csvFile),
                                        Arrays.asList(argumentsFile), crowdExpert1, null, maceThreshold,
                                        new WorkerAssignmentFilterRandomized(
                                                requiredAssignmentsSize, 1, crowdSize, random));

                        SortedMap<String, String> goldEstimationResult2 = Step6bAlternativeWarrantValidationHITGoldAnnotator
                                .annotateWithGoldLabels(Collections.singletonList(csvFile),
                                        Arrays.asList(argumentsFile), crowdExpert2, null, maceThreshold,
                                        new WorkerAssignmentFilterRandomized(
                                                requiredAssignmentsSize, 2, crowdSize, random));

                        double kappa = computeKappa(goldEstimationResult1, goldEstimationResult2);

                        synchronized (table) {
                            if (!table.contains(crowdSize, maceThreshold)) {
                                table.put(crowdSize, maceThreshold, new DescriptiveStatistics());
                            }
                            table.get(crowdSize, maceThreshold).addValue(kappa);
                        }

                        FileUtils.forceDelete(crowdExpert1);
                        FileUtils.forceDelete(crowdExpert2);

                        synchronized (table) {
                            System.out.println("===================================");
                            printTable(table);
                            System.out.println("===================================");
                        }
                    }
                    catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }

                }
                //        System.out.println("Kappas:");
                //        for (Map.Entry<Integer, Double> entry : kappas.entrySet()) {
                //            System.out.printf("%d\t%.2f%n", entry.getKey(), entry.getValue());
                //        }
                printTable(table);
            });
        });
    }


    private static double computeKappa(SortedMap<String, String> crowdExpert1,
            SortedMap<String, String> crowdExpert2)
            throws IOException
    {
        Set<String> expert1IDs = crowdExpert1.keySet();
        Set<String> expert2IDs = crowdExpert2.keySet();

        // intersection
        SortedSet<String> intersectionIDs = new TreeSet<>();
        intersectionIDs.addAll(expert1IDs);
        intersectionIDs.retainAll(expert2IDs);

        CodingAnnotationStudy study = new CodingAnnotationStudy(2);

        ConfusionMatrix cm = new ConfusionMatrix();

        for (String id : intersectionIDs) {
            String expert1 = crowdExpert1.get(id);
            String expert2 = crowdExpert2.get(id);

            if (expert1 != null && expert2 != null) {
                study.addItem(expert1, expert2);

                cm.increaseValue(expert1, expert2);
            }

            System.out.println(expert1 + "\t" + expert2);
        }

        KrippendorffAlphaAgreement alpha = new KrippendorffAlphaAgreement(study,
                new NominalDistanceFunction());
        CohenKappaAgreement kappa = new CohenKappaAgreement(study);

        System.out.println("Alpha: " + alpha.calculateAgreement());
        System.out.println("Kappa: " + kappa.calculateAgreement());

//        new ContingencyMatrixPrinter().print(System.out, study);
//        return kappa.calculateAgreement();

        return cm.getPrecisionForLabel("1");
    }
}
