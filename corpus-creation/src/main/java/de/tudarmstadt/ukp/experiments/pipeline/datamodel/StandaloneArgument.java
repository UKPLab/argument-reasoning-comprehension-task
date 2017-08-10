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

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.xml.sax.SAXException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * For further annotations, we don't need the entire debate
 *
 * @author Ivan Habernal
 */
public class StandaloneArgument
        extends Argument
{
    public static final String UNCLEAR_STANCE_TEXT = "Neither of them / The comment does not take a stance on the debate question / The comment talks about something else";

    private static final String BOTH_STANCES = "Discusses pros and cons of both sides but remains neutral";

    private DebateMetaData debateMetaData;

    private SortedSet<String> stances;

    private boolean annotatedSarcastic;

    // true if the annotated stance is BOTH_STANCES
    private boolean annotatedStanceBothSides = false;

    /**
     * This might differ from the original stance as given in {@code this.stance}
     */
    private String annotatedStance;

    // embedded annotations
    private String base64JCas;

    public StandaloneArgument()
    {
    }

    /**
     * Creates a deep copy of the argument
     *
     * @param argument argument
     */
    public StandaloneArgument(StandaloneArgument argument)
    {
        // copy all fields
        this.setAuthor(argument.getAuthor());
        this.setVoteUpCount(argument.getVoteUpCount());
        this.setVoteDownCount(argument.getVoteDownCount());
        this.setStance(argument.getStance());
        this.setText(argument.getText());
        this.setParentId(argument.getParentId());
        this.setId(argument.getId());
        this.setOriginalHTML(argument.getOriginalHTML());
        this.setTimestamp(argument.getTimestamp());

        this.stances = new TreeSet<>(argument.stances);
        this.annotatedSarcastic = argument.annotatedSarcastic;
        this.annotatedStance = argument.annotatedStance;
        this.annotatedStanceBothSides = argument.annotatedStanceBothSides;
        this.base64JCas = argument.base64JCas;
        this.debateMetaData = argument.debateMetaData;
    }

    public static SortedMap<String, Sentence> extractSentenceIDsAndContent(
            StandaloneArgument argument)
            throws IOException
    {
        JCas jCas = argument.getJCas();
        // extract sentences
        SortedMap<String, Sentence> result = new TreeMap<>();

        ArrayList<Sentence> sentences = new ArrayList<>(JCasUtil.select(jCas, Sentence.class));
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);

            // create unique id by combining argument id and sentence position
            String sentenceId = getSentenceID(argument, i);

            result.put(sentenceId, sentence);
        }

        //        System.out.println("extractSentenceIDsAndContent result keys: " + result.keySet());

        return result;
    }

    public static String getSentenceID(Argument argument, int position)
    {
        return String.format(Locale.ENGLISH, "%s_%02d", argument.getId(), position);
    }

    public JCas getJCas()
            throws RuntimeException
    {
        if (base64JCas == null) {
            return null;
        }

        try {
            byte[] bytes = new BASE64Decoder()
                    .decodeBuffer(new ByteArrayInputStream(base64JCas.getBytes("utf-8")));
            JCas jCas = JCasFactory.createJCas();
            XmiCasDeserializer.deserialize(new ByteArrayInputStream(bytes), jCas.getCas());

            return jCas;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setJCas(JCas jCas)
            throws IOException
    {
        // now convert to XMI
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        try {
            XmiCasSerializer.serialize(jCas.getCas(), byteOutputStream);
        }
        catch (SAXException e) {
            throw new IOException(e);
        }

        // encode to base64
        this.base64JCas = new BASE64Encoder().encode(byteOutputStream.toByteArray());
    }

    public StandaloneArgument(Argument argument, Debate debate)
    {
        // copy all fields
        setAuthor(argument.getAuthor());
        setVoteUpCount(argument.getVoteUpCount());
        setVoteDownCount(argument.getVoteDownCount());
        setStance(argument.getStance());
        setText(argument.getText());
        setParentId(argument.getParentId());
        setId(argument.getId());
        setOriginalHTML(argument.getOriginalHTML());
        setTimestamp(argument.getTimestamp());

        if (argument instanceof StandaloneArgument) {
            this.stances = new TreeSet<>(((StandaloneArgument) argument).getStances());

            if (this.stances.size() != 2) {
                throw new IllegalStateException(
                        "Expected 2 stances but there was: " + this.stances.size());
            }

            this.debateMetaData = ((StandaloneArgument) argument).getDebateMetaData();
        }
        else {
            this.debateMetaData = debate.getDebateMetaData();
            SortedSet<String> existingStances = debate.getStances();
            if (existingStances.size() != 2) {
                throw new IllegalStateException(
                        "Expected 2 stances, got " + existingStances + " for debate " + debate
                                .getDebateMetaData());
            }
            this.stances = new TreeSet<>(existingStances);
        }
    }

    public DebateMetaData getDebateMetaData()
    {
        return debateMetaData;
    }

    public void setDebateMetaData(DebateMetaData debateMetaData)
    {
        this.debateMetaData = debateMetaData;
    }

    public SortedSet<String> getStances()
    {
        return stances;
    }

    public void setStances(SortedSet<String> stances)
    {
        this.stances = stances;
    }

    public String getAnnotatedStance()
    {
        return annotatedStance;
    }

    /**
     * Returns the opposite stance as annotated (e.g. annotated = "Yes" would return "No")
     *
     * @return opposing stance
     * @throws IllegalStateException if there is no annotated stance or stances size is not 2
     */
    public String getStanceOpposingToAnnotatedStance()
            throws IllegalStateException
    {
        if (annotatedStance == null) {
            throw new IllegalStateException("this.annotatedStance is null");
        }

        if (this.stances.size() != 2) {
            throw new IllegalStateException("2 stances required but has " + stances);
        }

        TreeSet<String> stancesCopy = new TreeSet<>(stances);
        stancesCopy.remove(annotatedStance);

        if (stancesCopy.size() != 1) {
            throw new IllegalStateException(
                    "Something is wrong here. Stances copy: " + stancesCopy + ", annotated stance: "
                            + annotatedStance);
        }

        return stancesCopy.iterator().next();
    }

    public void setAnnotatedStance(String annotatedStance)
    {
        this.annotatedStance = annotatedStance;
    }

    /**
     * Returns a sorted map of four stance types (side 1, side 2, both sides, no stance)
     *
     * @return sorted map
     */
    public Map<String, Integer> getMappingAllStancesToInt()
    {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();

        // add two existing
        int counter = 0;
        for (String stance : this.stances) {
            result.put(stance, counter);
            counter++;
        }

        // add both stances but neutral as 3
        result.put(BOTH_STANCES, 3);

        // add "no stance" as 2
        result.put(UNCLEAR_STANCE_TEXT, 2);

        return result;
    }

    /**
     * Returns a reverse map of {@linkplain #getMappingAllStancesToInt()}
     *
     * @return sorted map
     */
    public Map<Integer, String> getMappingIntToAllStances()
    {
        LinkedHashMap<Integer, String> result = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : getMappingAllStancesToInt().entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }

        return result;
    }

    public void setAnnotatedStance(int annotatedStanceInt)
    {
        // for no stance, we don't assign anything
        if (annotatedStanceInt == getMappingAllStancesToInt().get(UNCLEAR_STANCE_TEXT)) {
            return;
        }

        if (annotatedStanceInt == getMappingAllStancesToInt().get(BOTH_STANCES)) {
            this.annotatedStanceBothSides = true;
        }

        this.annotatedStance = getMappingIntToAllStances().get(annotatedStanceInt);

        if (this.annotatedStance == null) {
            System.out.println(getMappingAllStancesToInt());
            System.out.println(getMappingIntToAllStances());

            throw new IllegalArgumentException("Unknown stance: " + annotatedStanceInt);
        }
    }

    public void setAnnotatedSarcastic(boolean annotatedSarcastic)
    {
        this.annotatedSarcastic = annotatedSarcastic;
    }

    public boolean isAnnotatedSarcastic()
    {
        return annotatedSarcastic;
    }

    public boolean isAnnotatedStanceBothSides()
    {
        return annotatedStanceBothSides;
    }

    public int getAnnotatedStanceAsInt()
    {
        Map<String, Integer> mapping = this.getMappingAllStancesToInt();

        if (this.getAnnotatedStance() == null) {
            return mapping.get(UNCLEAR_STANCE_TEXT);
        }
        else {
            return mapping.get(this.getAnnotatedStance());
        }
    }
}
