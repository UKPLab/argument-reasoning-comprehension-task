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

package de.tudarmstadt.ukp.experiments.model;

import java.util.*;

/**
 * POJO container for a debate
 *
 * @author Ivan Habernal
 */
public class RFDDebate
{
    private String debateTitle;

    private String debateUrl;

    private String debateDescription;

    private SortedSet<String> stances = new TreeSet<>();

    private Set<String> topics = new HashSet<String>();

    private List<RFDArticle> articleList = new ArrayList<>();

    public String getDebateTitle()
    {
        return debateTitle;
    }

    public void setDebateTitle(String debateTitle)
    {
        this.debateTitle = debateTitle;
    }

    public String getDebateUrl()
    {
        return debateUrl;
    }

    public void setDebateUrl(String debateUrl)
    {
        this.debateUrl = debateUrl;
    }

    public String getDebateDescription()
    {
        return debateDescription;
    }

    public void setDebateDescription(String debateDescription)
    {
        this.debateDescription = debateDescription;
    }

    public Set<String> getTopics()
    {
        return topics;
    }

    public void setTopics(Set<String> topics)
    {
        this.topics = topics;
    }

    public List<RFDArticle> getArticleList()
    {
        return articleList;
    }

    public void setArticleList(List<RFDArticle> articleList)
    {
        this.articleList = articleList;
    }

    public SortedSet<String> getStances()
    {
        return stances;
    }

    public void setStances(SortedSet<String> stances)
    {
        this.stances = stances;
    }

    /**
     * Returns a part of URL that contains the debate title
     *
     * @return part of url
     */
    public String getURLShortened()
    {
        return this.debateUrl.split("/")[5];
    }
}
