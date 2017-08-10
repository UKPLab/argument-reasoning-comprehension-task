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

package de.tudarmstadt.ukp.experiments.argumentation.experiments;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * (c) 2017 Ivan Habernal
 */
public class ClassificationMain
{
    public static Set<SingleInstance> loadData(File file)
            throws IOException
    {
        Set<SingleInstance> result = new HashSet<>();

        List<String> lines = IOUtils
                .readLines(new InputStreamReader(new FileInputStream(file), "utf-8"));

        for (String line : lines) {
            if (!line.startsWith("#")) {
                result.add(new SingleInstance(line));
            }
        }

        // make sure there are all instances (minus one for the comment line)
        if (result.size() != (lines.size() - 1)) {
            throw new IllegalStateException("Inconsistent input");
        }

        return result;
    }

    public static void runExperiment(Classifier classifier, File trainingData, File devData,
            File testData)
            throws IOException
    {
        // load training data
        Set<SingleInstance> training = loadData(trainingData);
        Set<SingleInstance> dev = loadData(devData);
        Set<SingleInstance> test = loadData(testData);

        // train
        classifier.train(training);

        // test on dev
        Set<PredictedInstance> predictionsDev = classifier.makePredictions(dev);

        // test on test
        Set<PredictedInstance> predictionsTest = classifier.makePredictions(test);

        // evaluate
        System.out.println(classifier.getClass().getSimpleName());
        System.out.printf(Locale.ENGLISH, "Accuracy_dev\t%.3f%n",
                Evaluator.computeAccuracy(predictionsDev));
        System.out.printf(Locale.ENGLISH, "Accuracy_test\t%.3f%n",
                Evaluator.computeAccuracy(predictionsTest));
    }

    public static void main(String[] args)
            throws IOException
    {
        File mainDir = new File("mturk/annotation-task/data/final");
        File trainingData = new File(mainDir, "train.tsv");
        File devData = new File(mainDir, "dev.tsv");
        File testData = new File(mainDir, "test.tsv");

        // random
        IntStream.range(0, 3).parallel().forEach(value -> {
            try {
                runExperiment(new RandomClassifier((long) value), trainingData, devData, testData);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });

        // LM
        runExperiment(new LMClassifier("apu", 8090), trainingData, devData, testData);
    }
}
