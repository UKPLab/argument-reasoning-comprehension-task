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

package de.tudarmstadt.ukp.experiments.exports;

import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.util.List;

/**
 * Exports gold-labeled reason spans to XMI files in the DKPro-Argumentation format
 * {@link http://github.com/dkpro/dkpro-argumentation/}.
 * <p>
 * Each reason (Premise) contains the corresponding gist.
 * <p>
 * Along with the XMI files, {@code metadata.csv} is also generated, which contains
 * all information (IDs = file names, annotated stance, debate title, etc.)
 *
 * @author Ivan Habernal
 */
public class Step3dExportGistToXMIFiles
{
    private static void export(File argumentsWithReasonsFile, File argumentsWithGistFile,
            File outputDir)
            throws Exception
    {
        List<StandaloneArgument> arguments = ExportHelper.copyReasonAnnotationsWithGistOnly(
                argumentsWithReasonsFile, argumentsWithGistFile);

        String metaDataCSV = ExportHelper.exportMetaDataToCSV(arguments);
        FileUtils.write(new File(outputDir, "metadata.csv"), metaDataCSV, "utf-8");

        // and export them all as XMI files using standard DKPro pipeline
        for (StandaloneArgument argument : arguments) {
            JCas jCas = argument.getJCas();
            SimplePipeline.runPipeline(jCas, AnalysisEngineFactory.createEngineDescription(
                    XmiWriter.class,
                    XmiWriter.PARAM_TARGET_LOCATION, outputDir,
                    XmiWriter.PARAM_USE_DOCUMENT_ID, true,
                    XmiWriter.PARAM_OVERWRITE, true
            ));
        }
    }

    public static void main(String[] args)
            throws Exception
    {
        File argumentsWithReasons = new File(
                "mturk/annotation-task/data/32-reasons-batch-0001-5000-2026args-gold.xml.gz");

        File argumentsWithGist = new File(
                "mturk/annotation-task/data/42-reasons-gist-batch-0001-5000-4294reasons.xml.gz");
        File output = new File(
                "mturk/annotation-task/data/exported-1927-arguments-with-gold-reasons-xmi/");

        export(argumentsWithReasons, argumentsWithGist, output);
    }

}
