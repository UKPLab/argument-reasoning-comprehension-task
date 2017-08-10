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

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivan Habernal
 */
@Deprecated // delete later
public class StanceTypoRepair
{
    public static void repairType(String fileName)
            throws IOException
    {
        System.out.println("Processing " + fileName);

        File file = new File(fileName);
        String wrong = "No, the laws should not be taughened";
        String correct = "No, the laws should not be toughened";

        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(file);

        int counter = 0;

        List<StandaloneArgument> repaired = new ArrayList<>();
        for (StandaloneArgument sa : arguments) {
            if (sa.getStances().contains(wrong)) {
                assert sa.getStances().remove(wrong);
                sa.getStances().add(correct);
                counter++;
            }

            repaired.add(sa);
        }

        System.out.println("Repaired " + counter);

        XStreamSerializer.serializeToXml(repaired, file);
    }

    public static void main(String[] args)
            throws IOException
    {
        repairType("mturk/annotation-task/data/21-pilot-stance-task.xml.gz");
        repairType(
                "mturk/annotation-task/data/21-pilot-stance-task-half-done-only-with-clear-stances.xml.gz");
        repairType(
                "mturk/annotation-task/data/21-pilot-stance-task-only-with-clear-stances.xml.gz");
        repairType("mturk/annotation-task/data/22-stance-batch-0001-5000-all.xml.gz");
        repairType(
                "mturk/annotation-task/data/22-stance-batch-0001-5000-only-with-clear-stances.xml.gz");
        repairType("mturk/annotation-task/data/30-pilot-reasons-task-for-40-pilot.xml.gz");
        repairType("mturk/annotation-task/data/32-reasons-batch-0001-5000-2026args-gold.xml.gz");
        repairType("mturk/annotation-task/data/arguments-with-full-segmentation-rfd.xml.gz");
    }
}
