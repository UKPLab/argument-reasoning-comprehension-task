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

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This implementation requires a running REST service (a server) with the pre-trained language
 * model. The server is not part of the source code, neither is the pre-trained model (due to its
 * size, roughly 1.8 GB). Contact us if you want the experiment with the language model baseline.
 * <p>
 * (c) 2017 Ivan Habernal
 */
public class LMClassifier
        extends Classifier
{
    private final RestTemplate restTemplate;
    private final String uri;

    /**
     * Creates a new REST service client and connects to the language model
     *
     * @param server server
     * @param port   port
     */
    public LMClassifier(String server, int port)
    {
        uri = "http://" + server + ":" + port + "/logprob/{sentence}";

        Map<String, String> params = new HashMap<>();
        params.put("sentence", "This is a test");

        restTemplate = new RestTemplate();
        restTemplate.getForObject(uri, Double.class, params);
    }

    public double getLogLikelihood(String sentence)
    {
        Map<String, String> params = new HashMap<>();
        params.put("sentence", sentence);

        try {
            return restTemplate.getForObject(uri, Double.class, params);
        }
        catch (HttpClientErrorException exception) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    @Override
    protected Set<PredictedInstance> makePredictions(Set<SingleInstance> testData)
    {
        Set<PredictedInstance> result = new HashSet<>();

        for (SingleInstance instance : testData) {
            double w0Likelihood = getLogLikelihood(instance.getWarrant0());
            double w1Likelihood = getLogLikelihood(instance.getWarrant1());

            // lower probability is the correct one
            int w0orW1 = w0Likelihood < w1Likelihood ? 0 : 1;

            PredictedInstance predictedInstance = new PredictedInstance(instance);
            predictedInstance.setPredictedLabelW0orW1(w0orW1);
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
