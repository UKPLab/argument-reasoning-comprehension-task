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

package de.tudarmstadt.ukp.experiments.roomfordebate;

import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import de.tudarmstadt.ukp.experiments.model.RFDArticle;
import de.tudarmstadt.ukp.experiments.model.RFDComment;
import de.tudarmstadt.ukp.experiments.model.RFDDebate;
import de.tudarmstadt.ukp.experiments.model.XStreamTools;
import de.tudarmstadt.ukp.experiments.roomfordebate.utils.Utils;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracting data from HTML debates and commentaries and writes the into a XML file
 *
 * @author Ivan Habernal
 */
public class DebateHTMLParser
{
    /**
     * For each URL contains stances (either yes/no or manually entered)
     */
    private final Map<String, SortedSet<String>> urlStances;

    public DebateHTMLParser()
            throws IOException
    {
        urlStances = new HashMap<>();

        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("rfd-controversies/rfd-manual-cleaning-controversies.tsv");

        CSVParser parser = new CSVParser(new InputStreamReader(inputStream), CSVFormat.TDF);
        for (CSVRecord csvRecord : parser) {
            boolean keepRFDDebate = "".equals(csvRecord.get(0));

            if (keepRFDDebate) {
                String url = csvRecord.get(5);

                SortedSet<String> stances = new TreeSet<>();
                // yes/no stance?
                if ("y".equals(csvRecord.get(3))) {
                    stances.addAll(Arrays.asList("Yes", "No"));
                }
                else {
                    stances.addAll(Arrays.asList(csvRecord.get(3), csvRecord.get(4)));
                }

                if (stances.size() != 2) {
                    throw new IllegalStateException(
                            "Expected 2 stances but got " + stances + "; " + csvRecord.get(1));
                }

                urlStances.put(url, stances);
            }
        }
    }

    public RFDDebate extractRFDDebate(String html)
            throws Exception
    {
        RFDDebate result = new RFDDebate();

        Document doc = Jsoup.parse(html);

        Element element = doc.select("Article.rfd").iterator().next();

        //		System.out.println(element);

        String dateText = element.select("p.pubdate").text().replaceAll("Updated[\\s]+", "");
        // time
        try {
            DateFormat df = new SimpleDateFormat("MMM dd, yyyy, hh:mm aaa", Locale.ENGLISH);
            Date date = df.parse(dateText);
        }
        catch (ParseException e) {
            // June 24, 2015
            DateFormat df = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            Date date = df.parse(dateText);
        }

        // debate title
        result.setDebateTitle(
                Utils.normalize(doc.select("div.nytint-discussion-overview > h2").text()));

        // debate url
        result.setDebateUrl(doc.select("div.nytint-discussion-overview > h2 > a").iterator().next()
                .attr("href"));

        // debate description
        result.setDebateDescription(Utils.normalize(
                ((TextNode) doc.select("div.nytint-discussion-overview > p").iterator()
                        .next()
                        .childNodes().iterator().next()).text()));

        // topics
        for (Element a : element.select("p.nytint-tags > a")) {
            result.getTopics().add(a.attr("href"));
        }

        return result;
    }

    public RFDArticle extractRFDArticle(String html)
            throws Exception
    {
        RFDArticle result = new RFDArticle();

        Document doc = Jsoup.parse(html);

        Element element = doc.select("Article.rfd").iterator().next();

        //		System.out.println(element);

        String dateText = element.select("p.pubdate").text().replaceAll("Updated[\\s]+", "");
        // time
        try {
            DateFormat df = new SimpleDateFormat("MMM dd, yyyy, hh:mm aaa", Locale.ENGLISH);
            Date date = df.parse(dateText);
            result.setTimestamp(date);
        }
        catch (ParseException e) {
            // June 24, 2015
            DateFormat df = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            Date date = df.parse(dateText);
            result.setTimestamp(date);
        }

        // url
        String url = doc.select("link[rel=canonical]").iterator().next().attr("href");
        result.setUrl(url);

        // title
        result.setTitle(Utils.normalize(element.select("h1").text()));

        // text
        StringBuilder sb = new StringBuilder();
        for (Element p : element.select("div.nytint-post > p")) {
            sb.append(p.text());
            sb.append("\n");
        }
        result.setText(Utils.normalize(sb.toString()));

        // author
        result.setAuthor(element.select("div.nytint-mugshots > img").iterator().next().attr("alt"));

        // author info
        result.setAuthorInfo(Utils.normalize(doc.select("p.nytint-post-leadin").text()));

        // quoted text
        result.setQuotation(Utils.normalize(doc.select("div.entry > blockquote").text()));

        result.getCommentList().addAll(extractRFDComments(html));

        return result;
    }

    /**
     * Extracts comments from the input html stream
     *
     * @param html html text
     * @return list of comments (never null)
     * @throws IOException exception
     */
    private List<RFDComment> extractRFDComments(String html)
            throws IOException
    {
        List<RFDComment> result = new ArrayList<>();

        Document doc = Jsoup.parse(html);

        for (Element element : doc.select("#commentsContainer Article")) {
            RFDComment comment = new RFDComment();

            // id
            comment.setId(element.attr("data-id"));
            // parent id
            comment.setParentId(!element.attr("data-parentid").equals("0") ?
                    element.attr("data-parentid") :
                    null);

            // previous comment id (if available)
            RFDComment previousRFDComment = result.isEmpty() ? null : result.get(result.size() - 1);
            // if the previous comment has parent, the current comment is a reaction to the previous one
            if (previousRFDComment != null && previousRFDComment.getParentId() != null) {
                comment.setPreviousPostId(previousRFDComment.getId());
            }

            // now metadata and content
            for (Node child : element.childNodes()) {
                if (child instanceof Element) {
                    Element childElement = (Element) child;

                    if ("header".equals(childElement.nodeName())) {
                        comment.setAuthor(Utils.normalize(
                                childElement.select("h3.commenter").iterator().next().text()));
                        comment.setAuthorLocation(Utils.normalize(
                                childElement.select("span.commenter-location").iterator().next()
                                        .text()));
                        comment.setAuthorTrusted(
                                childElement.select("i.trusted-icon").size() == 1);
                        // time
                        DateFormat df = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
                        String dateText = childElement.select("a.comment-time").text();
                        try {
                            Date date = df.parse(dateText);
                            comment.setTimestamp(date);
                        }
                        catch (ParseException e) {
                            // maybe it's "x days ago"
                            Pattern p = Pattern.compile("(\\d+) days ago");
                            Matcher m = p.matcher(dateText);
                            while (m.find()) {
                                // get the value
                                int xDaysAgo = Integer.valueOf(m.group(1));

                                // translate to Java date
                                Calendar cal = Calendar.getInstance();
                                cal.add(Calendar.DAY_OF_YEAR, (-xDaysAgo));
                                Date date = cal.getTime();

                                comment.setTimestamp(date);
                            }
                        }
                    }
                    // recommendations
                    else if ("footer".equals(childElement.nodeName())) {
                        Elements select = childElement.select("span.recommend-count");
                        if (!select.text().isEmpty()) {
                            comment.setVoteUpCount(Integer.valueOf(select.text()));
                        }
                    }
                    // the text
                    else if ("p".equals(childElement.nodeName())) {
                        String text = paragraphElementToString(childElement);

                        // and do some cleaning and normalization
                        String normalized = Utils.normalize(text);

                        comment.setText(normalized);
                    }
                }
            }

            result.add(comment);
        }

        return result;
    }

    /**
     * Extracts elements from the html comments (paragraph breaks, links)
     *
     * @param pElement paragraph element
     * @return plain text
     */
    public static String paragraphElementToString(Element pElement)
    {
        StringBuilder sb = new StringBuilder();
        for (Node child : pElement.childNodes()) {
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;

                sb.append(textNode.text());
            }
            else if (child instanceof Element) {
                Element element = (Element) child;

                // append new line for break
                if ("br".equals(element.tag().getName())) {
                    sb.append("\n");
                }
                else if ("a".equals(element.tag().getName())) {
                    // extract link from a.href
                    sb.append(" ").append(element.attr("href")).append(" ");
                }
                else {
                    // or just add the text
                    sb.append(" ").append(element.text()).append(" ");
                }
            }
        }

        return sb.toString();
    }

    private void assignStances(RFDDebate debate)
    {
        String url = debate.getDebateUrl().split("/")[5];
        if (this.urlStances.containsKey(url)) {
            // assign manually pre-defined
            debate.setStances(new TreeSet<>(this.urlStances.get(url)));
            System.out.println("Assigning stances to " + url + " - " + debate.getStances());
        }
        else {
            throw new IllegalArgumentException("Unknown debate: " + url);
        }
    }

    public void parse(File inputDir, File outputFile)
            throws Exception
    {
        Map<String, RFDDebate> debates = new TreeMap<>();

        for (File f : FileUtils.listFiles(inputDir, new String[] { "html" }, false)) {
            try {
                String html = FileUtils.readFileToString(f);
                // first identify debate
                RFDDebate debate = extractRFDDebate(html);

                // only for debates we have explicit stances
                String url = debate.getDebateUrl().split("/")[5];
                if (urlStances.containsKey(url)) {
                    // assign possible stances
                    assignStances(debate);

                    if (!debates.containsKey(debate.getDebateUrl())) {
                        debates.put(debate.getDebateUrl(), debate);
                    }

                    RFDArticle RFDArticle = extractRFDArticle(html);
                    debates.get(debate.getDebateUrl()).getArticleList().add(RFDArticle);
                }
            }
            catch (Exception e) {
                System.err.println("Error: " + f);
                throw e;
            }
        }

        // save to xStream
        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(outputFile), "utf-8");
        writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        XStreamTools.getXStream().marshal(
                new ArrayList<>(debates.values()), new PrettyPrintWriter(writer));
        IOUtils.closeQuietly(writer);
    }

    public static void main(String[] args)
            throws Exception
    {
        // ./roomfordebate-raw-html
        // ./room-for-debate-2010-2016-debates-with-polar-questions.xml
        new DebateHTMLParser().parse(new File(args[0]), new File(args[1]));
    }

}
