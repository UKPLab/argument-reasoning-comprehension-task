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

import de.tudarmstadt.ukp.dkpro.argumentation.types.ArgumentUnitUtils;
import de.tudarmstadt.ukp.dkpro.argumentation.types.Premise;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Export a single tab-separated file with 1,927 arguments and their abstractive summaries.
 * The summaries were created by compiling gists of the reasons thus convey the most important
 * information from the argument. All relevant metadata are stored in metadata.csv.
 *
 * @author Ivan Habernal
 */
public class Step3eExportGistForSummarization
{
    private static void export(File argumentsWithReasonsFile, File argumentsWithGistFile,
            File outputDir)
            throws Exception
    {
        List<StandaloneArgument> arguments = ExportHelper
                .copyReasonAnnotationsWithGistOnly(
                        argumentsWithReasonsFile, argumentsWithGistFile);

        String metaDataCSV = ExportHelper.exportMetaDataToCSV(arguments);
        FileUtils.write(new File(outputDir, "metadata.csv"), metaDataCSV, "utf-8");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // extract
        for (StandaloneArgument argument : arguments) {
            JCas jCas = argument.getJCas();

            // collect all gists
            String gistsOnly = JCasUtil.select(jCas, Premise.class).stream()
                    .filter(premise -> StringUtils
                            .isNotEmpty(ArgumentUnitUtils.getProperty(premise, "gist")))
                    .map(premise -> {
                        // remove last dot if present
                        return ArgumentUnitUtils.getProperty(premise, "gist")
                                .replaceAll("\\.$", "").trim();
                    }).collect(Collectors.joining(". ")).concat(".");

            String text = jCas.getDocumentText().replaceAll("\n", " ");
            String id = argument.getId();

            pw.printf(Locale.ENGLISH, "%s\t%s\t%s%n", id, text, gistsOnly);

            System.out.println(gistsOnly);
        }

        pw.flush();
        FileUtils.write(new File(outputDir, "argument-summarized.tsv"), sw.toString());
    }

    public static void main(String[] args)
            throws Exception
    {
        File argumentsWithReasons = new File(
                "mturk/annotation-task/data/32-reasons-batch-0001-5000-2026args-gold.xml.gz");

        File argumentsWithGist = new File(
                "mturk/annotation-task/data/42-reasons-gist-batch-0001-5000-4294reasons.xml.gz");
        File output = new File(
                "mturk/annotation-task/data/exported-1927-summarized-arguments/");

        export(argumentsWithReasons, argumentsWithGist, output);
    }
}
