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

import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * @author Ivan Habernal
 */
public class URLsFromWarcExtractor
{
    public static void extractAllDebates(File inFolder, File outFile)
            throws IOException
    {
        File[] files = inFolder.listFiles();
        if (files == null) {
            throw new IOException("No such dir: " + inFolder);
        }

        SortedSet<String> allDebateTitles = new TreeSet<>();
        List<RoomForDebateMetaData> allMetaData = new ArrayList<>();

        for (File f : files) {
            DataInputStream dataInputStream = new DataInputStream(
                    new GZIPInputStream(new FileInputStream(f)));
            while (true) {
                try {
                    //Reads the next record from the file.
                    WARCRecord wc = new WARCRecord(dataInputStream);

                    // detect charset
                    byte[] bytes = wc.getContent();

                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                    String html = IOUtils.toString(byteArrayInputStream, "utf-8");

                    String url = wc.getHeader().getTargetURI();
                    RoomForDebateMetaData metaData = extractDebateDataFromURL(url);

                    if (metaData != null) {
                        if (metaData.title == null) {
                            allDebateTitles.add(metaData.debate);
                        }

                        allMetaData.add(metaData);
                    }
                }
                catch (EOFException e) {
                    break;
                }
            }
        }

        System.out.println(allDebateTitles);

        System.out.println("Unfiltered debates: " + allMetaData.size());

        // second pass to remove all pages that swapped debate topic and its title
        Iterator<RoomForDebateMetaData> iterator = allMetaData.iterator();
        while (iterator.hasNext()) {
            RoomForDebateMetaData metaData = iterator.next();
            if (metaData.title != null && allDebateTitles.contains(metaData.title)) {
                iterator.remove();
            }
        }

        System.out.println("Filtered debates: " + allMetaData.size());

        PrintWriter pw = new PrintWriter(new FileWriter(outFile));
        Collections.sort(allMetaData, new Comparator<RoomForDebateMetaData>()
        {
            @Override
            public int compare(RoomForDebateMetaData o1,
                    RoomForDebateMetaData o2)
            {
                return o2.date.compareTo(o1.date);
            }
        });

        for (RoomForDebateMetaData metaData : allMetaData) {
            if (metaData.title != null) {
                pw.println(metaData.url);
            }
        }
        IOUtils.closeQuietly(pw);
    }

    private static RoomForDebateMetaData extractDebateDataFromURL(String targetURI)
    {
        if (targetURI == null) {
            return null;
        }

        //        http://www.nytimes.com/roomfordebate/2016/05/10/will-regulating-e-cigarettes-mean-fewer-will-quit-smoking/the-fda-rules-ignore-harm-reduction-benefits-for-adult-smokers
        String trimmed = targetURI.replaceAll("^http://www.nytimes.com/roomfordebate/?", "");

        String[] split = trimmed.split("/");

        RoomForDebateMetaData result = new RoomForDebateMetaData();

        result.url = targetURI;

        if (split.length > 3) {
            int year = Integer.valueOf(split[0]);
            int month = Integer.valueOf(split[1]);
            int day = Integer.valueOf(split[2]);
            Calendar calendar = new GregorianCalendar();
            calendar.set(year, month, day);

            result.date = new Date(calendar.getTimeInMillis());
        }

        if (split.length == 5) {
            result.debate = split[3];
            result.title = split[4];

            return result;
        }

        if (split.length == 4 && !split[3].endsWith(".rss")) {
            result.debate = split[3];

            return result;
        }

        return null;
    }

    private static class RoomForDebateMetaData
    {
        Date date;
        String debate;
        String title;
        String url;
    }

    public static void main(String[] args)
            throws IOException
    {
        extractAllDebates(new File("/mnt/apu/crawling/roomfordebate-warc-exported"),
                new File("/tmp/urls.txt"));
    }
}
