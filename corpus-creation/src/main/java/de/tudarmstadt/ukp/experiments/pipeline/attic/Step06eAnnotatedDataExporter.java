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

import de.tudarmstadt.ukp.dkpro.argumentation.types.ArgumentComponent;
import de.tudarmstadt.ukp.dkpro.argumentation.types.Claim;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.StandaloneArgument;
import de.tudarmstadt.ukp.experiments.pipeline.datamodel.XStreamSerializer;
import de.tudarmstadt.ukp.experiments.pipeline.utils.ArgumentPrinterUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivan Habernal
 */
@Deprecated
public class Step06eAnnotatedDataExporter
{
    public void saveToHTML(File originalXMLArgumentsFile, File outputFile)
            throws IOException
    {
        // read all arguments from the original corpus
        List<StandaloneArgument> arguments = XStreamSerializer
                .deserializeArgumentListFromXML(originalXMLArgumentsFile);

        PrintWriter out = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));

        // print header
        printHeader(out);

        for (StandaloneArgument argument : arguments) {
            out.println(argumentToHTML(argument));
            System.out.println(argument.getId());
        }

        // print footer
        printFooter(out);

        IOUtils.closeQuietly(out);
    }

    public static List<String> renderDocumentToHtmlParagraphs(JCas jCas)
    {
        List<String> result = new ArrayList<>();

        // iterate over paragraphs
        for (Paragraph p : JCasUtil.select(jCas, Paragraph.class)) {
            StringWriter writer = new StringWriter();
            PrintWriter out = new PrintWriter(writer);
            // iterate over tokens
            for (Token t : JCasUtil.selectCovered(jCas, Token.class, p)) {
                // print token's preceding space if needed
                if (ArgumentPrinterUtils.hasSpaceBefore(t, jCas)) {
                    out.print(" ");
                }

                // does an argument concept begin here?
                ArgumentComponent argumentConcept = ArgumentPrinterUtils
                        .argAnnotationBegins(t, jCas);
                if (argumentConcept != null) {
                    String additionalInfo = "";
                    if (argumentConcept instanceof Claim) {
                        additionalInfo = " [stance: " + ((Claim) argumentConcept).getStance() + "]";
                    }
                    out.printf("<span class=\"component\">%s%s:</span> <span class=\"%s\">",
                            argumentConcept.getClass().getSimpleName().toLowerCase(),
                            additionalInfo,
                            argumentConcept.getClass().getSimpleName().toLowerCase());
                }

                Sentence sentence = ArgumentPrinterUtils.sentenceStartsOnToken(t);
                if (sentence != null) {
                    out.printf("<span class=\"sentence\">S%d</span>",
                            ArgumentPrinterUtils.getSentenceNumber(sentence, jCas));
                }

                // print token
                out.print(t.getCoveredText());

                // does an argument concept end here?
                if (ArgumentPrinterUtils.argAnnotationEnds(t, jCas)) {
                    out.print("</span>");
                }
            }

            result.add(writer.toString());
        }

        return result;
    }

    String argumentToHTML(StandaloneArgument argument)
            throws IOException
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        out.printf("<h1>%s</h1>\n<h2>%s</h2>\n", argument.getDebateMetaData().getTitle(),
                argument.getDebateMetaData().getDescription());

        // print paragraphs
        for (String paragraph : renderDocumentToHtmlParagraphs(argument.getJCas())) {
            out.printf("<p>%s</p>%n", paragraph);
        }

        // stance
        out.printf(
                "<p><span class=\"component\">Stance:</span> <span class=\"claim\">%s</span></p>",
                argument.getAnnotatedStance());

        // sarcastic
        out.printf(
                "<p><span class=\"component\">Sarcastic:</span> <span class=\"appeal\">%s</span></p>",
                argument.isAnnotatedSarcastic());

        out.printf("<hr />");

        IOUtils.closeQuietly(out);
        return sw.toString();
    }

    private void printFooter(PrintWriter out)
    {
        out.printf("</body>\n</html>");
    }

    private void printHeader(PrintWriter out)
            throws IOException
    {
        String css = StringUtils.join(IOUtils.readLines(this.getClass().getClassLoader()
                .getResourceAsStream("argument-to-html-exporter.css"), "utf-8"), "\n");

        out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html PUBLIC"
                + " \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\"http://www.w3.org/TR/xhtml1/"
                + "DTD/strict.dtd\">\n<html xmlns=\"http://www.w3.org/TR/xhtml1/strict\" >\n"
                + "<head>\n<title>%s</title>\n" + "<link rel=\"stylesheet\" href=\"style.css\"/>\n"
                + " <meta http-equiv=\"content-type\" content=\"application/xhtml+xml; charset=UTF-8\" />\n"
                + "<style>\n" + css + "\n</style>\n"
                + "</head>\n<body>\n");
    }

    public static void main(String[] args)
            throws Exception
    {
        //        saveOnlyNonSarcasticArgumentsWithStance(
        //                new File("mturk/annotation-task/data/21-pilot-stance-task.xml.gz"),
        //                new File(
        //                        "mturk/annotation-task/data/21-pilot-stance-task-only-with-clear-stances.xml.gz"));

        //        new AnnotatedDataExporter()
        //                .saveToHTML(new File("mturk/annotation-task/data/30-pilot-reasons-task.xml.gz"),
        //                        new File("mturk/annotation-task/data/30-pilot-reasons-task.html"));

        new Step06eAnnotatedDataExporter()
                .saveToHTML(new File(
                                "mturk/annotation-task/data/32-reasons-batch-0001-5000-2361args-gold.xml.gz"),
                        new File(
                                "mturk/annotation-task/data/32-reasons-batch-0001-5000-2361args-gold.html"));

    }
}
