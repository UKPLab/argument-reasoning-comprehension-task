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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * POJO container for a single article
 *
 * @author Ivan Habernal
 */
public class RFDArticle
{
    private String url;

    private Date timestamp;

    private String title;

    private String author;

    private String authorInfo;

    private String quotation;

    private String text;

    private List<RFDComment> commentList = new ArrayList<>();

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public void setAuthorInfo(String authorInfo)
    {
        this.authorInfo = authorInfo;
    }

    public String getAuthorInfo()
    {
        return authorInfo;
    }

    public void setQuotation(String quotation)
    {
        this.quotation = quotation;
    }

    public String getQuotation()
    {
        return quotation;
    }

    public List<RFDComment> getCommentList()
    {
        return commentList;
    }

    public void setCommentList(List<RFDComment> commentList)
    {
        this.commentList = commentList;
    }

    @Override public String toString()
    {
        return "Article{" +
                "author='" + author + '\'' +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", authorInfo='" + authorInfo + '\'' +
                ", quotation='" + quotation + '\'' +
                ", commentList=" + commentList +
                '}';
    }
}
