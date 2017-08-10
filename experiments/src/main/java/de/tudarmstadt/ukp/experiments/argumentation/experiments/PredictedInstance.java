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

/**
 * (c) 2017 Ivan Habernal
 */
public class PredictedInstance
        extends SingleInstance
{
    private Integer predictedLabelW0orW1;

    public PredictedInstance(SingleInstance original)
    {
        super(original);
    }

    public Integer getPredictedLabelW0orW1()
    {
        return predictedLabelW0orW1;
    }

    public void setPredictedLabelW0orW1(Integer predictedLabelW0orW1)
    {
        if (predictedLabelW0orW1 < 0 || predictedLabelW0orW1 > 1) {
            throw new IllegalArgumentException("Invalid argument: " + predictedLabelW0orW1);
        }

        this.predictedLabelW0orW1 = predictedLabelW0orW1;
    }
}
