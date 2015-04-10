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
package de.tudarmstadt.ukp.dkpro.tc.features.ngram.meta;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.LuceneNGramDFE;

public class LuceneNGramMetaCollectorTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void luceneNgramMetaCollectorTest()
        throws Exception
    {
        File tmpDir = folder.newFolder();

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TextReader.class, 
                TextReader.PARAM_SOURCE_LOCATION, "src/test/resources/data/",
                TextReader.PARAM_LANGUAGE, "en",
                TextReader.PARAM_PATTERNS, "text*.txt"
        );
        
        AnalysisEngineDescription segmenter = AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class);
        
        AnalysisEngineDescription metaCollector = AnalysisEngineFactory.createEngineDescription(
                LuceneNGramMetaCollector.class,
                LuceneNGramDFE.PARAM_SOURCE_LOCATION, tmpDir
        );

        for (JCas jcas : new JCasIterable(reader, segmenter, metaCollector)) {
//            System.out.println(jcas.getDocumentText().length());
        }
        
        int i = 0;
        IndexReader index;
        try {
            index = DirectoryReader.open(FSDirectory.open(tmpDir));
            Fields fields = MultiFields.getFields(index);
            if (fields != null) {
                Terms terms = fields.terms(LuceneNGramDFE.LUCENE_NGRAM_FIELD);
                if (terms != null) {
                    TermsEnum termsEnum = terms.iterator(null);
//                    Bits liveDocs = MultiFields.getLiveDocs(index);
//                    DocsEnum docs = termsEnum.docs(liveDocs, null);
//                    int docId;
//                    while((docId = docs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
//                        index.g
//                    }
                    BytesRef text = null;
                    while ((text = termsEnum.next()) != null) {
//                        System.out.println(text.utf8ToString() + " - " + termsEnum.totalTermFreq());
//                        System.out.println(termsEnum.docFreq());
                        
                        if (text.utf8ToString().equals("this")) {
                            assertEquals(2, termsEnum.docFreq());
                            assertEquals(3, termsEnum.totalTermFreq());
                        }
                        
                        i++;
                    }
                }
            }
        }
        catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
        
       assertEquals(35, i);    
    }
    
    @Test
    public void emptyDocumentTest()
        throws Exception
    {
        File tmpDir = folder.newFolder();

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TextReader.class, 
                TextReader.PARAM_SOURCE_LOCATION, "src/test/resources/empty/",
                TextReader.PARAM_LANGUAGE, "en",
                TextReader.PARAM_PATTERNS, "empty*.txt"
        );
        
        AnalysisEngineDescription segmenter = AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class);
        
        AnalysisEngineDescription metaCollector = AnalysisEngineFactory.createEngineDescription(
                LuceneNGramMetaCollector.class,
                LuceneNGramDFE.PARAM_SOURCE_LOCATION, tmpDir
        );

        for (JCas jcas : new JCasIterable(reader, segmenter, metaCollector)) {
//            System.out.println(jcas.getDocumentText().length());
        }
    }
}
