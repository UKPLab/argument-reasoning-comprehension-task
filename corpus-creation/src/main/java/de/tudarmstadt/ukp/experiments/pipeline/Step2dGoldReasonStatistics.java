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

import de.tudarmstadt.ukp.dkpro.argumentation.types.Premise;
import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public class Step2dGoldReasonStatistics
{
    public static void main(String[] args)
            throws IOException
    {
        File inputFile = new File(
                "mturk/annotation-task/data/32-reasons-batch-0001-5000-2026args-gold.xml.gz");

        // read all arguments from the original corpus
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(inputFile);
        System.out.println("Arguments: " + arguments.size());

        Frequency frequency = new Frequency();
        DescriptiveStatistics statistics = new DescriptiveStatistics();

        for (StandaloneArgument argument : arguments) {
            JCas jCas = argument.getJCas();
            Collection<Premise> premises = JCasUtil.select(jCas, Premise.class);

            frequency.addValue(premises.size());
            statistics.addValue(premises.size());
        }

        System.out.println(frequency);
        System.out.println(statistics.getSum());
        System.out.println(statistics.getMean());
        System.out.println(statistics.getStandardDeviation());
    }
}
