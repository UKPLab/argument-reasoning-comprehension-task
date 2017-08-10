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

package de.tudarmstadt.ukp.experiments.pipeline.attic;

import de.tudarmstadt.ukp.experiments.pipeline.gold.MTurkOutputReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Ivan Habernal
 */
@Deprecated
public class MTurkRejections
{
    public static List<String> getAssignmentIDs(String workerID, MTurkOutputReader outputReader)
    {
        List<String> result = new ArrayList<>();

        for (Map<String, String> row : outputReader) {
            if (workerID.equals(row.get("workerid"))) {
                result.add(row.get("assignmentid"));
            }
        }

        return result;
    }

    public static void printRejectionsFile(MTurkOutputReader outputReader,
            Map<String, String> rejectionsAndReasons)
    {
        for (Map.Entry<String, String> entry : rejectionsAndReasons.entrySet()) {
            for (String assignmentId : getAssignmentIDs(entry.getKey(), outputReader)) {
                System.out.println(assignmentId + "\t\"" + entry.getValue() + "\"");
            }
        }
    }

    public static void main(String[] args)
            throws IOException
    {
        // 32-reasons-batch-0001-5000-2883args-task
        /*
        Map<String, String> rejections32_1 = new TreeMap<>();
        rejections32_1.put("A2QNWWUS8VICC2",
                "Dear worker, I'm sorry for rejecting your HITs but you provided completely random answers and your average submission time was only 11 seconds/HIT which is not even sufficient to read the given texts.");
        printRejectionsFile(new MTurkOutputReader(false, new File(
                        "mturk/annotation-task/32-reasons-batch-0001-5000-2883args-task.output.csv")),
                rejections32_1);
                */

        // 32-reasons-batch-0001-5000-2883args-task
        Map<String, String> rejections61_1 = new TreeMap<>();
        rejections61_1.put("A3VURMEKHPIATF",
                "Dear worker, I'm sorry but we manually verified your answers and they were mostly just random clicks. Furthermore, your submissions times for several HITs were under 20s for which is not even possible to properly read the questions, let alone to provide a well though-out answer.");
        rejections61_1.put("AB98SGS280TY5",
                "Dear worker, I'm sorry but we manually verified your answers and they were mostly just random clicks. Furthermore, your submissions times for several HITs were under 20s for which is not even possible to properly read the questions, let alone to provide a well though-out answer.");
        rejections61_1.put("ARG392N6HWZCJ",
                "Dear worker, I'm sorry but we manually verified your answers and they were mostly just random clicks. Furthermore, your submissions times for several HITs were under 20s for which is not even possible to properly read the questions, let alone to provide a well though-out answer.");
        printRejectionsFile(new MTurkOutputReader(false, new File(
                        "mturk/annotation-task/61-reason-disambiguation-batch-0001-5000-4294reasons-0060-4294-task.output.csv")),
                rejections61_1);
    }
}
