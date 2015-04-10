/*******************************************************************************
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.tc.features.ngram;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.api.features.DocumentFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;
import de.tudarmstadt.ukp.dkpro.tc.api.features.meta.MetaDependent;
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.meta.ChunkTripleMetaCollector;

/**
 * Extracts NP-VP-NP triples from chunked sentences.
 * Uses all triples that appeared in the training data with a frequency above the threshold as features.
 */
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk" })
public class ChunkTripleDFE
    extends FeatureExtractorResource_ImplBase
    implements DocumentFeatureExtractor, MetaDependent
{

    public static final String PARAM_CHUNK_TRIPLE_FD_FILE = "chunkTripleFdFile";
    @ConfigurationParameter(name = PARAM_CHUNK_TRIPLE_FD_FILE, mandatory = true)
    private String chunkTripleFdFile;

    public static final String PARAM_CHUNK_TRIPLE_THRESHOLD = "chunkTripleThreshold";
    @ConfigurationParameter(name = PARAM_CHUNK_TRIPLE_THRESHOLD, mandatory = false, defaultValue = "2")
    private int chunkTripleThreshold;

    public static final String PARAM_CHUNK_TRIPLE_LOWER_CASE = "chunkTripleLowerCase";
    @ConfigurationParameter(name = PARAM_CHUNK_TRIPLE_LOWER_CASE, mandatory = false, defaultValue = "true")
    private boolean chunkTripleLowerCase;

    protected Set<String> tripleSet;

    private FrequencyDistribution<String> trainingFD;

    @Override
    public List<Feature> extract(JCas jcas)
        throws TextClassificationException
    {
        // if(focusAnnotation!=null){
        // throw new TextClassificationException(new
        // UnsupportedOperationException("FocusAnnotation not yet supported!"));
        // }
        List<Feature> features = new ArrayList<Feature>();

        Set<String> triples = ChunkTripleMetaCollector.getTriples(jcas, chunkTripleLowerCase);
        for (String featureTriple : tripleSet) {
            if (triples.contains(featureTriple)) {
                features.add(new Feature("lexicalTriple_" + featureTriple, 1));
            }
            else {
                features.add(new Feature("lexicalTriple_" + featureTriple, 0));
            }
        }

        return features;
    }

    @Override
    public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
        throws ResourceInitializationException
    {
        if (!super.initialize(aSpecifier, aAdditionalParams)) {
            return false;
        }

        tripleSet = loadTriples();

        return true;
    }

    private Set<String> loadTriples()
        throws ResourceInitializationException
    {

        Set<String> tripleSet = new HashSet<String>();

        try {
            trainingFD = new FrequencyDistribution<String>();
            trainingFD.load(new File(chunkTripleFdFile));
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        catch (ClassNotFoundException e) {
            throw new ResourceInitializationException(e);
        }

        for (String key : trainingFD.getKeys()) {
            if (trainingFD.getCount(key) > chunkTripleThreshold) {
                tripleSet.add(key);
            }
        }

        return tripleSet;
    }

    @Override
    public List<AnalysisEngineDescription> getMetaCollectorClasses()
        throws ResourceInitializationException
    {
        return asList(createEngineDescription(ChunkTripleMetaCollector.class));
    }
}