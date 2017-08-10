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

//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.RestTemplate;
import de.tudarmstadt.ukp.experiments.pipeline.gold.ReasonGistSelector;
import de.tudarmstadt.ukp.experiments.pipeline.gold.SingleWorkerAssignment;

import java.util.*;

/**
 * @author Ivan Habernal
 */
@Deprecated
public class ReasonGistSelectorLangModel
        implements ReasonGistSelector
{
/*
    private final RestTemplate restTemplate;
    private final String uri;
*/

    @Override public String selectFinalReasonGist(SortedSet<SingleWorkerAssignment<String>> value)
    {
        return null;
    }

    /**
     * Creates a new REST service client and connects to the language model
     *
     * @param server server
     * @param port   port
     */
/*
    public ReasonGistSelectorLangModel(String server, int port)
    {
        uri = "http://" + server + ":" + port + "/logprob/{sentence}";

        Map<String, String> params = new HashMap<>();
        params.put("sentence", "This is a test");

        restTemplate = new RestTemplate();
        restTemplate.getForObject(uri, Double.class, params);
    }

    public double getLogProbability(String sentence)
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
    public String selectFinalReasonGist(SortedSet<SingleWorkerAssignment<String>> value)
    {
        Map<String, Double> logProbSentences = new TreeMap<>();
        for (SingleWorkerAssignment<String> assignment : value) {
            String sentence = assignment.getLabel();
            Double logProb = getLogProbability(sentence);

            logProbSentences.put(sentence, logProb);
        }

        LinkedHashMap<String, Double> sortByValue = CollectionUtils
                .sortByValue(logProbSentences, false);

        System.out.println(sortByValue);

        // return sentence with the highest log prob
        return sortByValue.entrySet().iterator().next().getKey();
    }
*/


//    public static void main(String[] args)
//    {
//        new ReasonGistSelectorLangModel("apu", 8090);
//    }
}
