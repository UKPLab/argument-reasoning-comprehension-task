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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * (c) 2017 Ivan Habernal
 */
public class RandomClassifier extends Classifier
{
    private final Random random;

    public RandomClassifier(long seed)
    {
        this.random = new Random(seed);
    }

    @Override
    protected Set<PredictedInstance> makePredictions(Set<SingleInstance> testData)
    {
        Set<PredictedInstance> result = new HashSet<>();
        for (SingleInstance instance : testData) {
            PredictedInstance predictedInstance = new PredictedInstance(instance);
            predictedInstance.setPredictedLabelW0orW1(random.nextInt(2));
            result.add(predictedInstance);
        }

        return result;
    }

    @Override
    void train(Set<SingleInstance> trainingData)
    {
        // empty
    }
}
