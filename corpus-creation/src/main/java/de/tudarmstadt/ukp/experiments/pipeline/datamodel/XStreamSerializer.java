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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Ivan Habernal
 */
public class XStreamSerializer
{
    protected static XStream xStream = null;

    public static String serializeToXML(Debate debate)
    {
        StringWriter sw = new StringWriter();
        sw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        getXStream().marshal(debate, new PrettyPrintWriter(new PrintWriter(sw)));

        return sw.toString();
    }

    public static void serializeToXml(List<StandaloneArgument> argumentList, File file)
            throws IOException
    {
        OutputStream os = new FileOutputStream(file);
        if (file.getName().endsWith(".gz")) {
            os = new GZIPOutputStream(new FileOutputStream(file));
        }

        PrintWriter pw = new PrintWriter(os);
        pw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        getXStream().marshal(argumentList, new PrettyPrintWriter(pw));
        IOUtils.closeQuietly(os);
    }

    public static void serializeReasonsToXml(List<ReasonClaimWarrantContainer> reasonList,
            File file)
            throws IOException
    {
        OutputStream os = new FileOutputStream(file);
        if (file.getName().endsWith(".gz")) {
            os = new GZIPOutputStream(new FileOutputStream(file));
        }

        PrintWriter pw = new PrintWriter(os);
        pw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        getXStream().marshal(reasonList, new PrettyPrintWriter(pw));
        IOUtils.closeQuietly(os);
    }

    @SuppressWarnings("unchecked")
    public static List<ReasonClaimWarrantContainer> deserializeReasonListFromXML(File xml)
            throws IOException
    {
        InputStream is = new FileInputStream(xml);
        if (xml.getName().endsWith(".gz")) {
            is = new GZIPInputStream(new FileInputStream(xml));
        }

        List<ReasonClaimWarrantContainer> result = (List<ReasonClaimWarrantContainer>) getXStream()
                .fromXML(is);
        IOUtils.closeQuietly(is);

        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<StandaloneArgument> deserializeArgumentListFromXML(File xml)
            throws IOException
    {
        InputStream is = new FileInputStream(xml);
        if (xml.getName().endsWith(".gz")) {
            is = new GZIPInputStream(new FileInputStream(xml));
        }

        List<StandaloneArgument> result = (List<StandaloneArgument>) getXStream().fromXML(is);
        IOUtils.closeQuietly(is);

        return result;
    }

    public static Debate deserializeFromXML(String xml)
    {
        return (Debate) getXStream().fromXML(xml);
    }

    public static Debate deserializeFromXML(File xml)
    {
        return (Debate) getXStream().fromXML(xml);
    }

    public static XStream getXStream()
    {
        if (xStream == null) {
            xStream = new XStream(new StaxDriver());
            xStream.alias("argument", Argument.class);
            xStream.alias("debate", Debate.class);
            xStream.alias("standaloneArgument", StandaloneArgument.class);

            xStream.alias("xxx.sampling.datamodel.ReasonClaimWarrantContainer", ReasonClaimWarrantContainer.class);
        }
        return xStream;
    }
}
