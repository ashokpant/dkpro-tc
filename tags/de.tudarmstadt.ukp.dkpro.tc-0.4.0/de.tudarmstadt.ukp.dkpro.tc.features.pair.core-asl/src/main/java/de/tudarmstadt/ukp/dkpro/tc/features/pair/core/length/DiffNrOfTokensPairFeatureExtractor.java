package de.tudarmstadt.ukp.dkpro.tc.features.pair.core.length;

import java.util.Arrays;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;
import de.tudarmstadt.ukp.dkpro.tc.api.features.PairFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.exception.TextClassificationException;

/**
 * Pair-wise feature extractor Computes the number of tokens in a view and returns the difference of
 * both views.
 * 
 * @author nico.erbs@gmail.com
 * 
 */
public class DiffNrOfTokensPairFeatureExtractor
    extends FeatureExtractorResource_ImplBase
    implements PairFeatureExtractor
{

    @Override
    public List<Feature> extract(JCas view1, JCas view2)
        throws TextClassificationException
    {
        return Arrays.asList(
                new Feature("DiffNrOfTokens",
                        JCasUtil.select(view1, Token.class).size()
                                - JCasUtil.select(view2, Token.class).size())
                );
    }
}
