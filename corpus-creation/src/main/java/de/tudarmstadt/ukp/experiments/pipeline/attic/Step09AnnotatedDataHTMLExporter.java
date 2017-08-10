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

import de.tudarmstadt.ukp.dkpro.argumentation.io.writer.ArgumentDumpWriter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

import java.io.File;
import java.nio.file.Files;

/**
 * @author Ivan Habernal
 */
@Deprecated // use AnnotatedDataExporter
public class Step09AnnotatedDataHTMLExporter
{

    public static void exportToHTML(File inputFile, File outputFile)
            throws Exception
    {
        File intermediateXMIsFile = File.createTempFile("temp", ".xmi.tar.gz");

        SingleXMLToXMIExporter.exportToXMIs(inputFile, intermediateXMIsFile);

        SimplePipeline.runPipeline(
                CollectionReaderFactory.createReaderDescription(
                        CompressedXmiReader.class,
                        CompressedXmiReader.PARAM_SOURCE_LOCATION, intermediateXMIsFile
                ),
                AnalysisEngineFactory.createEngineDescription(ArgumentsToHTMLExporter.class,
                        ArgumentsToHTMLExporter.PARAM_OUTPUT_FILE, outputFile),
                AnalysisEngineFactory.createEngineDescription(
                        ArgumentDumpWriter.class
                )
        );

        Files.delete(intermediateXMIsFile.toPath());
    }

    public static void main(String[] args)
            throws Exception
    {
        //        File inputFile = new File("/home/user-ukp/IdeaProjects/acl2017/mturk_private/annotation-task/data/arguments-with-EDUs-test.xml.gz");
        //        File outputFile = new File("/home/user-ukp/data2/acl2017/08-gold-claims/arguments.html");
        //        exportToHTML(inputFile, outputFile);

        // 03-claim pilot mine
        //        exportToHTML(new File("/home/user-ukp/data2/acl2017/08-gold-standard/arguments-03-pilot-mine.xml.gz"),
        //                new File("/home/user-ukp/data2/acl2017/08-gold-standard/arguments-03-pilot-mine.html"));
        // 03-claim pilot workers
        //        exportToHTML(new File("/home/user-ukp/data2/acl2017/08-gold-standard/arguments-03-pilot-workers.xml.gz"),
        //                new File("/home/user-ukp/data2/acl2017/08-gold-standard/arguments-03-pilot-workers.html"));
        // 04-claim pilot workers
        //        exportToHTML(new File("/home/user-ukp/data2/acl2017/08-gold-standard/arguments-04-pilot-workers.xml.gz"),
        //                new File("/home/user-ukp/data2/acl2017/08-gold-standard/arguments-04-pilot-workers.html"));
        // 05-claim pilot workers
//        exportToHTML(new File(
//                        "/home/user-ukp/data2/emnlp2017/08-gold-standard/arguments-05-pilot-workers.xml.gz"),
//                new File(
//                        "/home/user-ukp/data2/emnlp2017/08-gold-standard/arguments-05-pilot-workers.html"));

        // pilot reasons
        exportToHTML(new File("mturk/annotation-task/data/30-pilot-reasons-task.xml.gz"),
                new File("mturk/annotation-task/data/30-pilot-reasons-task.html"));
    }
}
