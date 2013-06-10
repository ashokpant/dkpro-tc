package de.tudarmstadt.ukp.dkpro.tc.features.pair.similarity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathException;
import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathFactory;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.PairFeatureExtractorResource_ImplBase;
import de.tudarmstadt.ukp.dkpro.tc.exception.TextClassificationException;
import de.tudarmstadt.ukp.similarity.algorithms.api.JCasTextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.dkpro.resource.TextSimilarityResourceBase;

public class SimilarityPairFeatureExtractor
    extends PairFeatureExtractorResource_ImplBase
{
    public static final String PARAM_SEGMENT_FEATURE_PATH = "SegmentFeaturePath";
    @ConfigurationParameter(name=PARAM_SEGMENT_FEATURE_PATH, mandatory=true, defaultValue="de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token")
    private String segmentFeaturePath;
    
    public static final String PARAM_TEXT_SIMILARITY_RESOURCE = "TextRelatednessResource";
    @ExternalResource(key=PARAM_TEXT_SIMILARITY_RESOURCE, mandatory=true)
    private TextSimilarityResourceBase measure;
    
    @Override
    public List<Feature> extract(JCas view1, JCas view2)
        throws TextClassificationException
    {
        try {
            double similarity;
            switch (measure.getMode()) {
                case text:
                    similarity = measure.getSimilarity(view1.getDocumentText(), view2.getDocumentText());
                    break;
                case jcas:
                    similarity = ((JCasTextSimilarityMeasure) measure).getSimilarity(view1, view2);
                    break;
                default: 
                    List<String> f1 = getItems(view1);
                    List<String> f2 = getItems(view2);
                    
                    // Remove "_" tokens
                    for (int i = f1.size() - 1; i >= 0; i--)
                    {
                        if (f1.get(i) == null || f1.get(i).equals("_")) {
                            f1.remove(i);
                        }
                    }
                    for (int i = f2.size() - 1; i >= 0; i--)
                    {
                        if (f2.get(i) == null || f2.get(i).equals("_")) {
                            f2.remove(i);
                        }
                    }
                    
                    similarity = measure.getSimilarity(f1, f2);
            }

            return Arrays.asList(
                    new Feature("Similarity" + measure.getName(), similarity)
            );
        }
        catch (FeaturePathException e) {
            throw new TextClassificationException(e);
        }
        catch (SimilarityException e) {
            throw new TextClassificationException(e);
        }
        
    }
    
    private List<String> getItems(JCas view)
        throws FeaturePathException
    {
        List<String> items = new ArrayList<String>();
        
        for (Map.Entry<AnnotationFS, String> entry : 
            FeaturePathFactory.select(view.getCas(), segmentFeaturePath))
        {
            items.add(entry.getValue());
        }
        
        return items;
    }
}