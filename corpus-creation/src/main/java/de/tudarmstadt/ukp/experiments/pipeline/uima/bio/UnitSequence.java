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

package de.tudarmstadt.ukp.experiments.pipeline.uima.bio;

import java.util.ArrayList;
import java.util.List;

/**
* @author xxx
*/
public class UnitSequence
{
    private List<UnitEntry> units = new ArrayList<>();

    public List<UnitEntry> getUnits()
    {
        return units;
    }

    @Override public String toString()
    {
        return "Sequence{" +
                "tokens=" + units +
                '}';
    }
}
