package de.tudarmstadt.ukp.dkpro.tc.features.style;

import static de.tudarmstadt.ukp.dkpro.tc.core.util.FeatureTestUtil.assertFeature;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.List;

import junit.framework.Assert;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;

public class ExclamationFeatureExtractorTest
{
    @Test
    public void exclamationRatioFeatureExtractorTest()
        throws Exception
    {
        AnalysisEngineDescription desc = createEngineDescription(BreakIteratorSegmenter.class);
        AnalysisEngine engine = createEngine(desc);

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText("He is a tester!!! Tester! Is he? Oh yes.");
        engine.process(jcas);

        ExclamationFeatureExtractor extractor = new ExclamationFeatureExtractor();
        List<Feature> features = extractor.extract(jcas);

        Assert.assertEquals(1, features.size());

        for (Feature feature : features) {
            assertFeature(ExclamationFeatureExtractor.FEATURE_NAME, 0.5, feature);
        }
    }
}