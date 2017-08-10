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

package de.tudarmstadt.ukp.experiments.pipeline.gold;

import java.util.SortedMap;

/**
 * Simple container for estimated results from MACE
 */
public class MACEOutputContainer
{
    private SortedMap<Integer, String> goldLabelPredictions;
    private SortedMap<String, Double> competences;

    public SortedMap<Integer, String> getGoldLabelPredictions()
    {
        return goldLabelPredictions;
    }

    public void setGoldLabelPredictions(
            SortedMap<Integer, String> goldLabelPredictions)
    {
        this.goldLabelPredictions = goldLabelPredictions;
    }

    public SortedMap<String, Double> getCompetences()
    {
        return competences;
    }

    public void setCompetences(SortedMap<String, Double> competences)
    {
        this.competences = competences;
    }
}
