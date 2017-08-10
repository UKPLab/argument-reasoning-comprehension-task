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

package de.tudarmstadt.ukp.experiments.pipeline.datamodel;

import de.tudarmstadt.ukp.dkpro.argumentation.types.ArgumentUnitUtils;
import de.tudarmstadt.ukp.dkpro.argumentation.types.Premise;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public class StandaloneArgumentWithSinglePremise
        extends StandaloneArgument
{
    final StandalonePremise premise;

    public StandaloneArgumentWithSinglePremise(StandaloneArgument argument,
            StandalonePremise premise)
    {
        super(argument);
        if (premise == null) {
            throw new IllegalArgumentException("param premise is null");
        }
        this.premise = premise;
    }

    public static List<StandaloneArgumentWithSinglePremise> extractPremises(
            StandaloneArgument standaloneArgument)
            throws IOException
    {
        List<StandaloneArgumentWithSinglePremise> result = new ArrayList<>();

        JCas jCas = standaloneArgument.getJCas();
        Collection<Premise> premises = new ArrayList<>(JCasUtil.select(jCas, Premise.class));
        if (premises.isEmpty()) {
            System.err.println("Only argument with annotated premises are allowed here");
            return result;
        }

        for (Premise premise : premises) {
            StandalonePremise standalonePremise = new StandalonePremise();

            // fill data
            standalonePremise.setGist(ArgumentUnitUtils.getProperty(premise, "gist"));
            standalonePremise.setDisambiguatedStance(
                    ArgumentUnitUtils.getProperty(premise, "disambiguatedStance"));
            standalonePremise.setOriginalText(premise.getCoveredText());
            standalonePremise
                    .setPremiseId(createUniquePremiseId(standaloneArgument.getId(), premise));

            StandaloneArgumentWithSinglePremise s = new StandaloneArgumentWithSinglePremise(
                    standaloneArgument, standalonePremise);
            result.add(s);
        }

        return result;
    }

    /**
     * Creates a unique ID by combining argument ID and the begin position of the premise
     *
     * @param argumentId argument id
     * @param premise    premise
     * @return string
     */
    public static String createUniquePremiseId(String argumentId, Premise premise)
    {
        return argumentId + "_" + premise.getBegin();
    }

    public StandalonePremise getPremise()
    {
        return premise;
    }

    public static class StandalonePremise
    {
        String premiseId;
        String originalText;
        String gist;
        String disambiguatedStance;

        public StandalonePremise()
        {
        }

        public String getPremiseId()
        {
            return premiseId;
        }

        public void setPremiseId(String premiseId)
        {
            this.premiseId = premiseId;
        }

        public String getOriginalText()
        {
            return originalText;
        }

        public void setOriginalText(String originalText)
        {
            this.originalText = originalText;
        }

        public String getGist()
        {
            return gist;
        }

        public void setGist(String gist)
        {
            this.gist = gist;
        }

        public String getDisambiguatedStance()
        {
            return disambiguatedStance;
        }

        public void setDisambiguatedStance(String disambiguatedStance)
        {
            this.disambiguatedStance = disambiguatedStance;
        }
    }
}
