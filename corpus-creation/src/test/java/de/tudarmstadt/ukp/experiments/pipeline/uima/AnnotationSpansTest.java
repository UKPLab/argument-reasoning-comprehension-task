package de.tudarmstadt.ukp.experiments.pipeline.uima;

import de.tudarmstadt.ukp.dkpro.argumentation.types.Premise;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ivan Habernal
 */
public class AnnotationSpansTest
{
    JCas jCas;

    @Before
    public void setUp()
            throws Exception
    {
        jCas = JCasFactory.createJCas();
        jCas.setDocumentText("s0t0 s0t2 s1t0 s2t0 s2t0 s3t0");
        jCas.setDocumentLanguage("en");

        Sentence s0 = new Sentence(jCas, 0, 9);
        s0.addToIndexes();

        Sentence s1 = new Sentence(jCas, 10, 14);
        s1.addToIndexes();

        Sentence s2 = new Sentence(jCas, 15, 24);
        s2.addToIndexes();

        Sentence s3 = new Sentence(jCas, 25, 29);
        s3.addToIndexes();

        Premise p1 = new Premise(jCas, 0, 14);
        p1.addToIndexes();

        Premise p2 = new Premise(jCas, 25, 29);
        p2.addToIndexes();

        System.out.println("'" + s0.getCoveredText() + "'");
        System.out.println("'" + s1.getCoveredText() + "'");
        System.out.println("'" + s2.getCoveredText() + "'");
        System.out.println("'" + s3.getCoveredText() + "'");
        System.out.println("p1: '" + p1.getCoveredText() + "'");
        System.out.println("p2: '" + p2.getCoveredText() + "'");
    }

    @Test
    public void extractAnnotationSpans()
            throws Exception
    {
        System.out.println(AnnotationSpans.extractAnnotationSpans(jCas));
    }

}