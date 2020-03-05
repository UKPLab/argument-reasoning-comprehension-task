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
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Exports the final train/dev/test data for the argument reasoning comprehension task
 * in CSV format (used later for experiments).
 *
 * @author Ivan Habernal
 */
public class Step9aFinalDataProvider
{
    final static Random RANDOM = new Random(1234);

    public static void prepareTrainDevTestData(File inputFile, File outputDirectory)
            throws IOException
    {
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new IOException("Cannot create output dir " + outputDirectory);
            }
        }

        List<ReasonClaimWarrantContainer> allFinalAnnotatedPairs = XStreamSerializer
                .deserializeReasonListFromXML(inputFile);

        // final sets
        Set<ReasonClaimWarrantContainer> train = new HashSet<>();
        Set<ReasonClaimWarrantContainer> dev = new HashSet<>();
        Set<ReasonClaimWarrantContainer> test = new HashSet<>();

        // for statistics
        Map<Integer, Set<String>> yearDebates = new TreeMap<>();
        for (ReasonClaimWarrantContainer container : allFinalAnnotatedPairs) {
            int year = Integer.valueOf(container.getDebateMetaData().getUrl().split("/")[2]);

            yearDebates.putIfAbsent(year, new TreeSet<>());
            yearDebates.get(year).add(container.getReasonClaimWarrantId());

            // put into train/dev/test
            if (year < 2015) {
                train.add(container);
            }
            else if (year == 2015) {
                dev.add(container);
            }
            else {
                test.add(container);
            }
        }

        for (Map.Entry<Integer, Set<String>> entry : yearDebates.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue().size());
        }

        System.out.printf(Locale.ENGLISH, "Train\t%d%nDev\t%d%nTest\t%d", train.size(), dev.size(),
                test.size());

        printToFile(train, new File(outputDirectory, "train.tsv"), false, false);
        printToFile(train, new File(outputDirectory, "train-doubled.tsv"), false, true);
        printToFile(train, new File(outputDirectory, "train-w-swap.tsv"), true, false);
        printToFile(train, new File(outputDirectory, "train-w-swap-doubled.tsv"), true, true);
        printToFile(dev, new File(outputDirectory, "dev.tsv"), false, false);
        printToFile(test, new File(outputDirectory, "test.tsv"), false, false);
    }

    /**
     * Writes collection to a text file in UTF-8 encoding.
     * Line format:
     * <pre>
     *     id warrant0 warrant1 correctLabelW0orW1 reason claim debateTitle debateInfo
     * </pre>
     *
     * @param collection                   collection
     * @param outputFile                   output file
     * @param addSwappedW0andW1
     * @param doubleDataWithOpposingStance
     * @throws IOException exception
     */
    public static void printToFile(Collection<ReasonClaimWarrantContainer> collection,
            File outputFile, boolean addSwappedW0andW1, boolean doubleDataWithOpposingStance)
            throws IOException
    {
        PrintWriter pw = new PrintWriter(outputFile, "utf-8");

        // print header
        pw.println(
                "#id\twarrant0\twarrant1\tcorrectLabelW0orW1\treason\tclaim\tdebateTitle\tdebateInfo");

        for (ReasonClaimWarrantContainer container : collection) {
            String w0;
            String w1;
            Integer correctLabelW0orW1;

            Integer correctLabelW0orW1swapped;
            String w0Swapped;
            String w1Swapped;

            Integer incorrectLabelW0orW1;
            Integer incorrectLabelW0orW1Swapped;

            if (container.getOriginalWarrant().equals(container.getAlternativeWarrant())) {
                throw new IllegalArgumentException();
            }


            if (RANDOM.nextBoolean()) {
                w0 = container.getOriginalWarrant();
                w1 = container.getAlternativeWarrant();
                correctLabelW0orW1 = 0;
                incorrectLabelW0orW1 = 1;

                // and swap also the entire triple
                correctLabelW0orW1swapped = 1;
                incorrectLabelW0orW1Swapped = 0;
                w0Swapped = w1;
                w1Swapped = w0;
            }
            else {
                w0 = container.getAlternativeWarrant();
                w1 = container.getOriginalWarrant();
                correctLabelW0orW1 = 1;
                incorrectLabelW0orW1 = 0;

                // and swap also the entire triple
                correctLabelW0orW1swapped = 0;
                incorrectLabelW0orW1Swapped = 1;
                w0Swapped = w1;
                w1Swapped = w0;
            }

            printEntry(pw, container.getReasonClaimWarrantId(), w0, w1, correctLabelW0orW1,
                    container.getReasonGist(), container.getAnnotatedStance(),
                    container.getDebateMetaData().getTitle(),
                    container.getDebateMetaData().getDescription());

            if (addSwappedW0andW1) {
                printEntry(pw, container.getReasonClaimWarrantId(), w0Swapped, w1Swapped,
                        correctLabelW0orW1swapped, container.getReasonGist(),
                        container.getAnnotatedStance(), container.getDebateMetaData().getTitle(),
                        container.getDebateMetaData().getDescription());
            }

            if (doubleDataWithOpposingStance) {
                printEntry(pw, container.getReasonClaimWarrantId(), w0, w1, incorrectLabelW0orW1,
                        container.getReasonGist(), container.getStanceOpposingToAnnotatedStance(),
                        container.getDebateMetaData().getTitle(),
                        container.getDebateMetaData().getDescription());
            }

            if (doubleDataWithOpposingStance && addSwappedW0andW1) {
                printEntry(pw, container.getReasonClaimWarrantId(), w0Swapped, w1Swapped,
                        incorrectLabelW0orW1Swapped, container.getReasonGist(),
                        container.getStanceOpposingToAnnotatedStance(),
                        container.getDebateMetaData().getTitle(),
                        container.getDebateMetaData().getDescription());
            }
        }

        IOUtils.closeQuietly(pw);
    }

    static void printEntry(PrintWriter pw, String reasonClaimWarrantId, String w0, String w1,
            Integer correctLabelW0orW1, String reasonGist, String annotatedStance, String title,
            String description)
    {
        pw.printf(Locale.ENGLISH, "%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s%n", reasonClaimWarrantId, w0, w1,
                correctLabelW0orW1, reasonGist, annotatedStance, title, description);
    }

    public static void main(String[] args)
            throws Exception
    {
        prepareTrainDevTestData(new File(
                        "mturk/annotation-task/data/96-original-warrant-batch-0001-5000-final-1970-good-reason-claim-pairs.xml.gz"),
                new File("mturk/annotation-task/data/final"));
    }
}
