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
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.Argument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Ivan Habernal
 */
public class ExportHelper
{
    public static String exportMetaDataToCSV(List<StandaloneArgument> arguments)
            throws IOException
    {
        StringWriter sw = new StringWriter();
        CSVPrinter csvPrinter = new CSVPrinter(sw, CSVFormat.DEFAULT.withHeader(
                "id", "author", "annotatedStance", "timestamp", "debateMetaData.title",
                "debateMetaData.description", "debateMetaData.url"
        ));

        for (StandaloneArgument argument : arguments) {
            csvPrinter.printRecord(
                    argument.getId(),
                    argument.getAuthor(),
                    argument.getAnnotatedStance(),
                    argument.getTimestamp(),
                    argument.getDebateMetaData().getTitle(),
                    argument.getDebateMetaData().getDescription(),
                    argument.getDebateMetaData().getUrl()
            );
        }

        sw.flush();

        return sw.toString();
    }

    public static List<StandaloneArgument> copyReasonAnnotationsWithGistOnly(
            File argumentsWithReasonsFile, File argumentsWithGistFile)
            throws Exception
    {
        // read all arguments
        List<StandaloneArgument> argumentsWithReasons = XStreamSerializer
                .deserializeArgumentListFromXML(argumentsWithReasonsFile);

        // convert to a map by id
        Map<String, StandaloneArgument> argumentsWithGistMap = XStreamSerializer
                .deserializeArgumentListFromXML(argumentsWithGistFile).stream()
                .collect(Collectors.toMap(Argument::getId, Function.identity()));

        List<StandaloneArgument> result = new ArrayList<>();
        for (StandaloneArgument standaloneArgument : argumentsWithReasons) {
            // create a deep copy
            StandaloneArgument copy = new StandaloneArgument(standaloneArgument);

            // clean all Premise annotations
            JCas jCas = copy.getJCas();
            Collection<Premise> premises = JCasUtil.select(jCas, Premise.class);
            for (Premise p : premises) {
                p.removeFromIndexes(jCas);
            }

            copy.setJCas(jCas);
            final JCas jCas2 = copy.getJCas();

            // make sure there are no premises
            if (!JCasUtil.select(jCas2, Premise.class).isEmpty()) {
                throw new IllegalStateException("Removing annotations went wrong");
            }

            // now find the same argument with the annotated gist
            if (argumentsWithGistMap.containsKey(copy.getId())) {
                StandaloneArgument gistAnnotated = argumentsWithGistMap.get(copy.getId());

                // find all premises with gist
                List<Premise> premisesWithGist = JCasUtil
                        .select(gistAnnotated.getJCas(), Premise.class)
                        .stream()
                        .filter(premise -> ArgumentUnitUtils.getProperty(premise, "gist") != null)
                        .collect(Collectors.toList());

                // only actual premises with gist!
                if (!premisesWithGist.isEmpty()) {
                    // and annotate them in the argument copy
                    premisesWithGist.forEach(p -> {
                        Premise premiseCopy = new Premise(jCas2, p.getBegin(), p.getEnd());
                        String gist = ArgumentUnitUtils.getProperty(p, "gist");
                        ArgumentUnitUtils.setProperty(premiseCopy, "gist", gist);
                        premiseCopy.addToIndexes();
                    });

                    // and put the annotations back
                    copy.setJCas(jCas2);

                    // now we have only argument with correct premises with gist
                    result.add(copy);
                }
            }
        }

        return result;
    }
}
