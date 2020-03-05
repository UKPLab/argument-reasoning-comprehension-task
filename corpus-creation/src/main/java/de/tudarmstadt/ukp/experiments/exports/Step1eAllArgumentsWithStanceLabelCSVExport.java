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

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static de.tudarmstadt.ukp.experiments.pipeline.AbstractArgumentHITCreator.checkConsistencyOfData;

/**
 * Exporting 3,365 arguments (stance-taking comments) including sarcastic in a CSV format
 *
 * @author Ivan Habernal
 */
public class Step1eAllArgumentsWithStanceLabelCSVExport
{
    public static void export(File inputFile, File outputFile)
            throws IOException
    {
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(inputFile);

        checkConsistencyOfData(arguments);

        List<StandaloneArgument> annotatedArguments = new ArrayList<>();

        CSVPrinter csvPrinter = new CSVPrinter(
                new OutputStreamWriter(new FileOutputStream(outputFile), "utf-8"),
                CSVFormat.TDF.withHeader("author", "voteUpCount", "voteDownCount", "text", "id",
                        "timestamp", "title", "description", "url", "stance1", "stance2",
                        "annotatedSarcastic", "annotatedStanceBothSides", "annotatedStance"));

        List<StandaloneArgument> argumentList = arguments.stream()
                .filter(a -> a.getAnnotatedStanceAsInt() == 0 || a.getAnnotatedStanceAsInt() == 1)
                .collect(Collectors.toList());

        // consistency check
        if (3365 != argumentList.size()) {
            throw new IllegalStateException("Expected 3365 arguments, got " + argumentList.size());
        }

        for (StandaloneArgument a : argumentList) {
            // print to CSV
            csvPrinter.printRecord(
                    a.getAuthor(),
                    a.getVoteUpCount(),
                    a.getVoteDownCount(),
                    a.getText().replaceAll("\n", " "),
                    a.getId(),
                    String.format(Locale.ENGLISH, "%s", a.getTimestamp()),
                    a.getDebateMetaData().getTitle(),
                    a.getDebateMetaData().getDescription(),
                    a.getDebateMetaData().getUrl(),
                    a.getStances().first(),
                    a.getStances().last(),
                    a.isAnnotatedSarcastic(),
                    a.isAnnotatedStanceBothSides(),
                    a.getAnnotatedStance()
            );
        }

        /*
         * Annotated with one of the two stances          2884        58%
         * The same as above but also sarcastic argument   481        10%
         */

        csvPrinter.flush();
        IOUtils.closeQuietly(csvPrinter);
    }

    public static void main(String[] args)
            throws IOException
    {
        export(new File("mturk/annotation-task/data/22-stance-batch-0001-5000-all.xml.gz"),
                new File("mturk/annotation-task/data/exported-3365-stance-taking-arguments.tsv"));

    }
}
