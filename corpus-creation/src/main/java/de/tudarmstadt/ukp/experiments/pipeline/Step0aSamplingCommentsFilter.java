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

import org.apache.commons.io.FileUtils;
import de.tudarmstadt.ukp.experiments.model.RFDArticle;
import de.tudarmstadt.ukp.experiments.model.RFDComment;
import de.tudarmstadt.ukp.experiments.model.RFDDebate;
import de.tudarmstadt.ukp.experiments.model.XStreamTools;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.*;
import de.tudarmstadt.ukp.experiments.pipeline.filters.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The first step in the pipeline. Given the set of heuristic filters, retains only comments that
 * fulfil the given criteria.
 * <p>
 * Part of the "Sampling comments" box in the paper. Results into 11,820 exported comments.
 *
 * @author Ivan Habernal
 */
public class Step0aSamplingCommentsFilter
{
    /**
     * Processes the debates and extract the required debates with arguments
     *
     * @param inputFile all debates
     * @param outputDir output
     * @throws IOException IO Exception
     */
    @SuppressWarnings("unchecked")
    public static void processData(File inputFile, File outputDir)
            throws IOException
    {
        int totalArgumentsLeftCounter = 0;

        List<? extends ArgumentSamplingFilter> filters = Arrays.asList(
                new ArgumentWordCountFilter(),
                new PointsReceivedFilter(1),
                new NoUrlFilter(),
                new RootArgumentFilter(),
                new DirectResponsesFilter(),
                new QuotationFilter(4)
        );

        List<? extends DebateSamplingFilter> debatePreFilters = Collections.singletonList(
                new MinimumRootArgumentCountFilterRFD(5)
        );

        // read all debates and filter them
        List<RFDDebate> rfdDebates = (List<RFDDebate>) XStreamTools.getXStream().fromXML(inputFile);

        for (RFDDebate rfdDebate : rfdDebates) {

            Debate debate = convertToDebate(rfdDebate);

            // run all pre-filters
            boolean keepDebatePre = true;
            for (DebateSamplingFilter debateSamplingFilter : debatePreFilters) {
                keepDebatePre &= debateSamplingFilter.keepDebate(debate);
            }

            if (keepDebatePre) {
                // create copy of the debate
                Debate debateCopy = new Debate();
                debateCopy.setDebateMetaData(debate.getDebateMetaData());

                for (Argument argument : debate.getArgumentList()) {
                    boolean keepArgument = true;

                    // run all filters
                    for (ArgumentSamplingFilter filter : filters) {
                        keepArgument &= filter.keepArgument(argument, debate);
                    }

                    // copy to the result
                    if (keepArgument) {
                        debateCopy.getArgumentList().add(argument);
                    }
                }

                System.out.println(debateCopy.getDebateMetaData().getUrl() + "\t" + debateCopy
                        .getDebateMetaData().getTitle());

                totalArgumentsLeftCounter += debateCopy.getArgumentList().size();

                // write the output
                String xml = XStreamSerializer.serializeToXML(debateCopy);
                FileUtils.writeStringToFile(
                        new File(outputDir, rfdDebate.getURLShortened() + ".xml"), xml,
                        "utf-8");
            }
        }

        System.out.println("Total arguments exported: " + totalArgumentsLeftCounter);
    }

    private static Debate convertToDebate(RFDDebate rfdDebate)
    {
        Debate result = new Debate();

        DebateMetaData metaData = new DebateMetaData();
        metaData.setDescription(rfdDebate.getDebateDescription());
        metaData.setTitle(rfdDebate.getDebateTitle());
        metaData.setUrl(rfdDebate.getDebateUrl());
        result.setDebateMetaData(metaData);

        for (RFDArticle article : rfdDebate.getArticleList()) {
            for (RFDComment comment : article.getCommentList()) {
                StandaloneArgument argument = new StandaloneArgument();
                SortedSet<String> stances = rfdDebate.getStances();
                if (stances.size() != 2) {
                    throw new IllegalStateException(
                            "2 stances expected but got " + stances + ", debate: " + rfdDebate
                                    .getDebateTitle());
                }
                argument.setStances(new TreeSet<>(stances));
                argument.setTimestamp(comment.getTimestamp());
                argument.setId(comment.getId());
                argument.setAuthor(comment.getAuthor());
                argument.setParentId(comment.getParentId());
                argument.setVoteUpCount(comment.getVoteUpCount());
                argument.setText(comment.getText());
                argument.setDebateMetaData(metaData);

                result.getArgumentList().add(argument);
            }
        }

        return result;

    }

    public static void main(String[] args)
            throws IOException
    {
        // path to room-for-debate-2010-2016-debates-with-polar-questions.xml
        File inputFile = new File(args[0]);

        // output directory
        File outputDir = new File(args[1]);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        processData(inputFile, outputDir);
    }
}
