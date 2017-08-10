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

package de.tudarmstadt.ukp.experiments.pipeline.roomfordebate;

import org.apache.commons.math.stat.Frequency;
import de.tudarmstadt.ukp.experiments.model.RFDArticle;
import de.tudarmstadt.ukp.experiments.model.RFDComment;
import de.tudarmstadt.ukp.experiments.model.RFDDebate;
import de.tudarmstadt.ukp.experiments.model.XStreamTools;
import de.tudarmstadt.ukp.experiments.pipeline.utils.CollectionUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Some descriptive statistics over the raw corpus
 *
 * @author Ivan Habernal
 */
public class RawRoomForDebateCorpusStatisticsExplorer
{
    @SuppressWarnings("unchecked")
    public static void main(String[] args)
    {
        // path to room-for-debate-2010-2016-debates-with-polar-questions.xml
        String f = args[0];

        Map<String, Frequency> statistics = new TreeMap<>();

        List<RFDDebate> debates = (List<RFDDebate>) XStreamTools.getXStream()
                .fromXML(new File(f));

        SortedMap<String, List<String>> urlsAndTitles = new TreeMap<>();

        int rootWithAtLeastN = 0;

        List<String> commentsToPrint = new ArrayList<>();

        System.out.println("Debates: " + debates.size());
        // articles
        List<RFDArticle> articles = new ArrayList<>();
        for (RFDDebate debate : debates) {
            articles.addAll(debate.getArticleList());

            CollectionUtils.put(statistics, "articlesPerDebate", new Frequency())
                    .addValue(debate.getArticleList().size());

            for (String topic : debate.getTopics()) {
                CollectionUtils.put(statistics, "topics", new Frequency())
                        .addValue(topic);
            }

            String description = debate.getDebateDescription();
            String[] split = debate.getDebateUrl().split("/");
            String url = split[split.length - 1];
            String title = debate.getDebateTitle();
            if (!urlsAndTitles.containsKey(url)) {
                urlsAndTitles.put(url, new ArrayList<String>());
            }
            urlsAndTitles.get(url).addAll(Arrays.asList(title, description));

            for (RFDArticle article : debate.getArticleList()) {
                CollectionUtils.put(statistics, "commentsPerArticle", new Frequency())
                        .addValue(article.getCommentList().size());

                int rootCommentsCount = 0;

                for (RFDComment comment : article.getCommentList()) {
                    CollectionUtils.put(statistics, "length", new Frequency())
                            .addValue((comment.getText().length() / 10) * 10);
                    CollectionUtils.put(statistics, "voteUpCount", new Frequency())
                            .addValue(comment.getVoteUpCount());

                    if (comment.getVoteUpCount() >= 10 && comment.getParentId() == null) {
                        rootWithAtLeastN++;

                        commentsToPrint.add(printCommentForManualExamination(debate, article,
                                comment));

                    }

                    if (comment.getParentId() == null) {
                        ++rootCommentsCount;
                    }
                }

                CollectionUtils.put(statistics, "commentsPerArticleRoot", new Frequency())
                        .addValue(rootCommentsCount);

                CollectionUtils.put(statistics, "rootCommentsWithAtLeastNPoints", new Frequency())
                        .addValue(rootWithAtLeastN);
            }

        }

        System.out.println("Articles: " + articles.size());

        for (Map.Entry<String, Frequency> entry : statistics.entrySet()) {
            System.out.println("--------------");
            System.out.println(entry.getKey());
            System.out.println(entry.getValue().toString());
            System.out.println(CollectionUtils.frequencyToStatistics(entry.getValue()));
        }

        System.out.println(rootWithAtLeastN);

        /*
        Collections.shuffle(commentsToPrint);
        //        System.out.println(
        //                org.apache.commons.lang.StringUtils.join(commentsToPrint.subList(0, 50), "\n\n"));

        for (Map.Entry<String, List<String>> entry : urlsAndTitles.entrySet()) {
            System.out.println(
                    entry.getValue().get(0) + "\t" + entry.getValue().get(1) + "\t" + entry
                            .getKey());
        }
        */


    }

    private static String printCommentForManualExamination(RFDDebate debate, RFDArticle article,
            RFDComment comment)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("=== " + debate.getDebateTitle() + " ====\n" +
                debate.getDebateDescription() + "\n#" + comment.getId() + ": " +
                comment.getAuthor() + " (" + comment.isAuthorTrusted() + ") " + comment
                .getAuthorLocation() + ", " + comment.getVoteUpCount() + ":\n" +
                comment.getText());

        return sw.toString();
    }

}
