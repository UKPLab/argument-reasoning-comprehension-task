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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Fetches all URLs into HTML files
 *
 * @author Ivan Habernal
 */
public class MainCrawler
{
    public static void main(String[] args)
            throws Exception
    {
        String chromeDriverFile = args[0];
        String urlListFile = args[1];
        File outputDir = new File(args[2]);

        DebateFetcher debateFetcher = new DebateFetcher(chromeDriverFile);

        // read URLs to fetch first
        List<String> urlList = IOUtils.readLines(new FileInputStream(urlListFile));
        System.out.println(urlList.size() + " URLs to fetch");

        for (String url : urlList) {
            String name = url.replaceAll("^http://", "").replaceAll("/", "___") + ".html";

            System.out.println("Fetching " + url);
            String html = debateFetcher.fetchDiscussionPage(url);

            // save to file
            File outputFile = new File(outputDir, name);
            FileUtils.write(outputFile, html, "utf-8");
            System.out.println("Written " + outputFile);
        }

        debateFetcher.finishFetching();
    }
}
