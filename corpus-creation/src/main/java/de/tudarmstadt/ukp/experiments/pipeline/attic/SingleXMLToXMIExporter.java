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

import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * @author Ivan Habernal
 */
@Deprecated
public class SingleXMLToXMIExporter
{
    public static void exportToXMIs(File inputFile, File outputFile)
            throws Exception
    {
        // create temp directory
        File tempDir = Files.createTempDirectory("tmp").toFile();

        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(inputFile);

        if (arguments.isEmpty()) {
            throw new IllegalStateException("Empty source argument list");
        }

        for (StandaloneArgument argument : arguments) {
            System.out.println("Saving " + argument.getId());

            // delete typesystem.xml
            File typeSystemFile = new File(tempDir, "typesystem.xml");
            typeSystemFile.delete();

            SimplePipeline.runPipeline(argument.getJCas(),
                    AnalysisEngineFactory.createEngineDescription(
                            XmiWriter.class,
                            XmiWriter.PARAM_TARGET_LOCATION, tempDir
                    ));
        }

        // now run the second pipeline and write output to a single tar.gz file
        SimplePipeline.runPipeline(CollectionReaderFactory.createReaderDescription(
                XmiReader.class, XmiReader.PARAM_SOURCE_LOCATION, tempDir,
                XmiReader.PARAM_PATTERNS, XmiReader.INCLUDE_PREFIX + "*.xmi"
                ),
                AnalysisEngineFactory.createEngineDescription(
                        CompressedXmiWriter.class,
                        CompressedXmiWriter.PARAM_OUTPUT_FILE, outputFile
                )
        );

        // delete the temp dir
        FileUtils.deleteDirectory(tempDir);
    }

}
