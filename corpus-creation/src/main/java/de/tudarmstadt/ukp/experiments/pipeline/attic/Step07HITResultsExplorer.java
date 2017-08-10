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
import java.util.HashSet;
import java.util.Map;

/**
 * (c) 2016 Ivan Habernal
 */
@Deprecated
public class Step07HITResultsExplorer
{

    public static void readComments(File file)
            throws Exception
    {
        // we want filled rows...
        HashSet<String> additionalRequiredFields = new HashSet<>();
        //        additionalRequiredFields.add("workerid");

        MTurkOutputReader outputReader = new MTurkOutputReader(additionalRequiredFields, false,
                file);

        // SortedMap<Date, String> dateHITid = new TreeMap<>();

        for (Map<String, String> row : outputReader) {
            String comment = row.get("Answer.feedback");
            //            System.out.println(row.keySet());
            if (comment != null && !comment.isEmpty()) {
                System.out.println(
                        row.get("workerid") + ", " + comment + " [AssID " + row.get("assignmentid")
                                + "; HITid " + row.get("hitid") + "]");
                System.out.println("---");
//                System.out.println(row);
            }

            //            if ("A2FSQBC1OU8WN8".equals(row.get("workerid"))) {
            //                dateHITid.put(MACEHelper.DATE_FORMAT.parse(row.get("assignmentsubmittime")),
            //                        row.get("hitid") + "_" + row.get("assignmentid"));
            //            }
        }

        //        System.out.println(dateHITid);
    }

    public static void main(String[] args)
            throws Exception
    {
        //        readComments(new File("/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/03-pilot-claims-task.output.csv"));
        //        readComments(new File("/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/05-pilot-claims-task.output.csv"));
        //        readComments(new File("/home/habi/IdeaProjects/emnlp2017/mturk/annotation-task/20-pilot-stance-task.output.csv"));
        //        readComments(new File("/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/20-pilot-stance-task.output.csv"));
        //        readComments(new File("/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/21-pilot-stance-task.output.csv"));
        //        readComments(new File("/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/21-pilot-stance-task.output.csv"));
        //        System.out.println("====================");
        //        readComments(new File("/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/30-pilot-reasons-task.output.csv"));
        //        readComments(new File("/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/31-pilot-reasons-task.output.csv"));
        //        System.out.println("====================");
        //        readComments(new File("/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/40-pilot-warrants-task.output.csv"));
        //        readComments(new File("/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/41-pilot-warrants-task.output.csv"));
        //        readComments(new File("/home/user-ukp/IdeaProjects/emnlp2017/mturk/annotation-task/40-pilot-reason-gist-task.output.csv"));
        //        readComments(new File("mturk/annotation-task/22-stance-batch-0001-5000-task.output.csv"));
        //        readComments(new File(
        //                "mturk/annotation-task/32-reasons-batch-0001-5000-2883args-task.output.csv"));
//        readComments(new File(
//                "mturk/annotation-task/42-reasons-gist-batch-0001-5000-5119reasons-task.output.csv"));
//        readComments(new File(
//                "mturk/annotation-task/52-pilot-warrants-task.output.csv"));
//        readComments(new File(
//                "mturk/annotation-task/55-warrants-batch-0001-5000-4294reasons-0001-0600-task.output.csv"));
//        readComments(new File(
//                "mturk/annotation-task/61-reason-disambiguation-batch-0001-5000-4294reasons-0060-4294-task.output.csv"));
        readComments(new File("mturk/annotation-task/81-001-600aw-validation-batch-0050-2390-reason-claim-pairs-task.output.csv"));

    }
}
