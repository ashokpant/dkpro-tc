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

import java.io.File;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.api.features.meta.MetaCollector;

public abstract class LuceneBasedMetaCollector
    extends MetaCollector
{
    public final static String LUCENE_DIR = "lucence";

    public static final String LUCENE_ID_FIELD = "id";

    public static final String PARAM_TARGET_LOCATION = ComponentParameters.PARAM_TARGET_LOCATION;
    @ConfigurationParameter(name = PARAM_TARGET_LOCATION, mandatory = true)
    private File luceneDir;

    // this is a static singleton as different Lucene-based meta collectors will use the same writer
    protected static IndexWriter indexWriter = null;

    private String currentDocumentId;
    private Document currentDocument;

    private FieldType fieldType;

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_44, null);

        if (indexWriter == null) {
            try {
                indexWriter = new IndexWriter(FSDirectory.open(luceneDir), config);
            } catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        }

        currentDocumentId = null;
        currentDocument = null;

        fieldType = new FieldType();
        fieldType.setIndexed(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setStored(true);
        fieldType.setOmitNorms(true);
        fieldType.setTokenized(false);
        fieldType.freeze();
    }

    protected void initializeDocument(JCas jcas) {
        if (currentDocument == null || !currentDocumentId.equals(getDocumentId(jcas))) {
            currentDocumentId = getDocumentId(jcas);
            if (currentDocumentId == null) {
                throw new IllegalArgumentException(
                        "Document has no id. id: " + DocumentMetaData.get(jcas).getDocumentId()
                                + ", title: " +
                                DocumentMetaData.get(jcas).getDocumentTitle() + ", uri: "
                                + DocumentMetaData.get(jcas).getDocumentUri());
            }
            currentDocument = new Document();
            currentDocument.add(new StringField(
                    LUCENE_ID_FIELD,
                    currentDocumentId,
                    Field.Store.YES
            ));
        }
    }

    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {

        initializeDocument(jcas);
        FrequencyDistribution<String> documentNGrams;
        try{
            documentNGrams = getNgramsFD(jcas);
        }catch(TextClassificationException e){
            throw new AnalysisEngineProcessException(e);
        }

        for (String ngram : documentNGrams.getKeys()) {
            // As a result of discussion, we add a field for each ngram per doc, 
            // not just each ngram type per doc.
            for (int i=0;i<documentNGrams.getCount(ngram);i++){
                addField(jcas, getFieldName(), ngram);
            }
        }

        try {
            writeToIndex();
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    protected void addField(JCas jcas, String fieldName, String value)
    	throws AnalysisEngineProcessException
    {
        if (currentDocument == null) {
        	throw new AnalysisEngineProcessException(new Throwable("Document not initialized. "
        			+ "Probably a lucene-based meta collector that calls addField() before initializeDocument()"));
        }

        Field field = new Field(
                fieldName,
                value,
                fieldType
        );
        currentDocument.add(field);
    }

    protected void writeToIndex()
        throws IOException
    {
    	if (currentDocument == null) {
    		throw new IOException("Lucene document not initialized. Fatal error.");
    	}
        indexWriter.addDocument(currentDocument);
    }

    @Override
    public void collectionProcessComplete()
        throws AnalysisEngineProcessException
    {
        super.collectionProcessComplete();

        if (indexWriter != null) {
            try {
                indexWriter.commit();
                indexWriter.close();
                indexWriter = null;
            } catch (AlreadyClosedException e) {
                // ignore, as multiple meta collectors write in the same index 
                // and will all try to close the index
            } catch (CorruptIndexException e) {
                throw new AnalysisEngineProcessException(e);
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }

    }

    protected String getDocumentId(JCas jcas)
    {
        return DocumentMetaData.get(jcas).getDocumentId();
    }

    protected abstract FrequencyDistribution<String> getNgramsFD(JCas jcas)
            throws TextClassificationException;

    protected abstract String getFieldName();
}