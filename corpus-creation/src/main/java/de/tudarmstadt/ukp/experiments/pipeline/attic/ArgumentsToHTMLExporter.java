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
import de.tudarmstadt.ukp.dkpro.argumentation.types.ArgumentUnitUtils;
import de.tudarmstadt.ukp.dkpro.argumentation.types.Claim;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import de.tudarmstadt.ukp.experiments.pipeline.utils.ArgumentPrinterUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports all annotated arguments into a single HTML file with convenient highlighting.
 *
 * @author Ivan Habernal
 */
@Deprecated
public class ArgumentsToHTMLExporter
        extends JCasConsumer_ImplBase
{
    /**
     * Output folder where the output HTML files are stored
     */
    public static final String PARAM_OUTPUT_FILE = "outputFile";

    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private File outputFile;

    private PrintWriter out;

    @Override
    public void initialize(UimaContext context)
            throws ResourceInitializationException
    {
        super.initialize(context);

        try {
            out = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));

            // print header
            printHeader(out);
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

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

    @Override
    public void process(JCas aJCas)
            throws AnalysisEngineProcessException
    {
        DocumentMetaData metaData = DocumentMetaData.get(aJCas);

        out.printf("<h1>%s</h1>\n<h2>%s</h2>\n", metaData.getDocumentId(),
                metaData.getDocumentTitle());

        // print paragraphs
        List<String> paragraphs = renderDocumentToHtmlParagraphs(aJCas);
        out.printf("<p>%s</p>", StringUtils.join(paragraphs, "<br/><br/>"));

        // implicit claim?
        for (Claim claim : JCasUtil.select(aJCas, Claim.class)) {
            if (ArgumentUnitUtils.isImplicit(claim)) {
                String claimText = claim.getStance();
                if (claimText == null) {
                    claimText = ArgumentUnitUtils
                            .getProperty(claim, ArgumentUnitUtils.PROP_KEY_REPHRASED_CONTENT);
                }
                out.printf(
                        "<p><span class=\"component\">Implicit claim:</span> <span class=\"claim\">%s</span></p>",
                        claimText);
            }
        }

        // appeal to emotions
        for (ArgumentComponent component : JCasUtil.select(aJCas, ArgumentComponent.class)) {
            if (ArgumentUnitUtils
                    .getProperty(component, ArgumentUnitUtils.PROP_KEY_IS_APPEAL_TO_EMOTION)
                    != null) {
                out.printf(
                        "<p><span class=\"component\">Appeal to emotions:</span> <span class=\"appeal\">%s</span></p>",
                        component.getCoveredText());
            }
        }

        out.printf("<hr />");

    }

    @Override
    public void collectionProcessComplete()
            throws AnalysisEngineProcessException
    {
        super.collectionProcessComplete();

        // print footer
        printFooter(out);

        IOUtils.closeQuietly(out);
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

}
