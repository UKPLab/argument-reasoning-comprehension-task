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

import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.tudarmstadt.ukp.experiments.pipeline.AbstractArgumentHITCreator.checkConsistencyOfData;

/**
 * Exporting all arguments (stance-taking comments) including sarcastic; data not used in the article.
 *
 * @author Ivan Habernal
 */
public class Step1dAllArgumentsWithSarcasmExport
{
    public static void export(File inputFile, File outputFile)
            throws IOException
    {
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(inputFile);

        checkConsistencyOfData(arguments);

        List<StandaloneArgument> annotatedArguments = new ArrayList<>();

        // filtering
        for (StandaloneArgument argument : arguments) {
            if ((argument.getAnnotatedStanceAsInt() == 0
                    || argument.getAnnotatedStanceAsInt() == 1)) {
                annotatedArguments.add(argument);
            }
        }

        System.out.println(annotatedArguments.size());

        // save annotated arguments to output xml
        outputFile.getParentFile().mkdirs();
        XStreamSerializer.serializeToXml(annotatedArguments, outputFile);
    }

    public static void main(String[] args)
            throws IOException
    {
        export(new File("mturk/annotation-task/data/22-stance-batch-0001-5000-all.xml.gz"),
                new File("mturk/annotation-task/data/exported-stance-sarcasm.xml.gz"));
    }
}
