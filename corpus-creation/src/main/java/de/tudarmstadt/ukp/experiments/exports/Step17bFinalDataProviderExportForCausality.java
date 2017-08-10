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

package de.tudarmstadt.ukp.experiments.exports;

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.ReasonClaimWarrantContainer;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Exports only warrants and conclusions; used for some experiments with causality; not published
 *
 * @author Ivan Habernal
 */
public class Step17bFinalDataProviderExportForCausality
{
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

        printToFile(allFinalAnnotatedPairs, new File(outputDirectory, "warrants-claims.tsv"));
    }

    /**
     * Writes collection to a text file in UTF-8 encoding.
     * Line format:
     * <pre>
     *     id W/AW warrant claim
     * </pre>
     *
     * @param collection collection
     * @param outputFile output file
     * @throws IOException exception
     */
    public static void printToFile(Collection<ReasonClaimWarrantContainer> collection,
            File outputFile)
            throws IOException
    {
        PrintWriter pw = new PrintWriter(outputFile, "utf-8");

        // print header
        pw.println(
                "#id\ttype\twarrant\tclaim");

        for (ReasonClaimWarrantContainer container : collection) {
            // first print original warrant
            printEntry(pw, container.getReasonClaimWarrantId(), "W", container.getOriginalWarrant(),
                    container.getAnnotatedStance());
            // then alternative warrant
            printEntry(pw, container.getReasonClaimWarrantId(), "AW",
                    container.getAlternativeWarrant(), container.getAnnotatedStance());
        }

        IOUtils.closeQuietly(pw);
    }

    static void printEntry(PrintWriter pw, String reasonClaimWarrantId, String type, String warrant,
            String annotatedStance)
    {
        pw.printf(Locale.ENGLISH, "%s\t%s\t%s\t%s%n", reasonClaimWarrantId, type, warrant,
                annotatedStance);
    }

    public static void main(String[] args)
            throws Exception
    {
        prepareTrainDevTestData(new File(
                        "mturk/annotation-task/data/96-original-warrant-batch-0001-5000-final-1970-good-reason-claim-pairs.xml.gz"),
                new File("mturk/annotation-task/data/experiments"));
    }
}
