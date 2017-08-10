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

import java.io.File;

import static de.tudarmstadt.ukp.experiments.pipeline.Step0bTextSegmenterA.runSegmenter;

/**
 * Text segmenter; consists of three consecutive steps, this is the third step
 * <p>
 * Same cmd parameters must be used in {@link Step0bTextSegmenterA} and {@link Step0bTextSegmenterB}
 *
 * @author Ivan Habernal
 */
public class Step0bTextSegmenterB
{

    public static void main(String[] args)
            throws Exception
    {
        File inputDir = new File(args[0]);
        File outputFile = new File(args[1]);
        File parentDir = outputFile.getParentFile();
        parentDir.mkdirs();

        File tempTXT = new File(args[2]);
        File tempEDUs = new File(args[3]);

        // make sure you ran the two previous steps (SegmenterA and ExternalEDUSegmenter)

        // second phase: annotate
        runSegmenter(inputDir, outputFile, null, tempEDUs);
    }

}
