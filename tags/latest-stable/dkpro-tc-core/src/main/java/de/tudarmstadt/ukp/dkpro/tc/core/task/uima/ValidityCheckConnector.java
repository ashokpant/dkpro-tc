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
package de.tudarmstadt.ukp.dkpro.tc.core.task.uima;

import java.util.Collection;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.api.features.ClassificationUnitFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.api.features.DocumentFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;
import de.tudarmstadt.ukp.dkpro.tc.api.features.PairFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationOutcome;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.task.ValidityCheckTask;

/**
 * UIMA analysis engine that is used in the {@link ValidityCheckTask} to test error conditions on
 * the CAS.
 * 
 */
public class ValidityCheckConnector
    extends ConnectorBase
{

    /**
     * Bipartition threshold used in multi-label experiments
     */
    public static final String PARAM_BIPARTITION_THRESHOLD = "bipartitionThreshold";
    @ConfigurationParameter(name = PARAM_BIPARTITION_THRESHOLD, mandatory = false)
    private String bipartitionThreshold;

    @ConfigurationParameter(name = PARAM_DATA_WRITER_CLASS, mandatory = true)
    private String dataWriter;

    @ConfigurationParameter(name = PARAM_FEATURE_EXTRACTORS, mandatory = true)
    protected String[] featureExtractors;

    @ConfigurationParameter(name = PARAM_LEARNING_MODE, mandatory = true, defaultValue = Constants.LM_SINGLE_LABEL)
    private String learningMode;

    @ConfigurationParameter(name = PARAM_FEATURE_MODE, mandatory = true, defaultValue = Constants.FM_DOCUMENT)
    private String featureMode;

    @ConfigurationParameter(name = PARAM_DEVELOPER_MODE, mandatory = true, defaultValue = "false")
    private boolean developerMode;

    private boolean firstCall;
    private int featureModeI;
    private int learningModeI;

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        firstCall = true;
    }

    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {

        // make sure this class is only called once per pipeline
        if (firstCall) {
            firstCall = false;

            if (DocumentMetaData.get(jcas).getDocumentId() == null) {
                throw new AnalysisEngineProcessException(new TextClassificationException(
                        "Please set a Document ID for all of your input files."));
            }

            if (featureModeI == 0) {
                if (featureMode.equals(Constants.FM_DOCUMENT))
                    featureModeI = 1;
                else if (featureMode.equals(Constants.FM_UNIT))
                    featureModeI = 2;
                else if (featureMode.equals(Constants.FM_PAIR))
                    featureModeI = 3;
                else if (featureMode.equals(Constants.FM_SEQUENCE)) {
                    featureModeI = 4;
                }
                else
                    throw new AnalysisEngineProcessException(new TextClassificationException(
                            "Please set a valid feature mode"));
            }

            if (learningModeI == 0) {
                if (learningMode.equals(Constants.LM_SINGLE_LABEL))
                    learningModeI = 1;
                else if (learningMode.equals(Constants.LM_MULTI_LABEL))
                    learningModeI = 2;
                else if (learningMode.equals(Constants.LM_REGRESSION))
                    learningModeI = 3;
                else
                    throw new AnalysisEngineProcessException(new TextClassificationException(
                            "Please set a valid learning mode"));
            }

            getLogger().log(Level.INFO, "--- checking validity of experiment setup ---");

            Collection<TextClassificationOutcome> outcomes = JCasUtil.select(jcas,
                    TextClassificationOutcome.class);
            Collection<TextClassificationUnit> classificationUnits = JCasUtil.select(jcas,
                    TextClassificationUnit.class);

            // whether outcome annotation are present at all
            if (outcomes.size() == 0) {
                throw new AnalysisEngineProcessException(
                        new TextClassificationException(
                                "No TextClassificationOutcome annotation found. "
                                        + "The reader must make sure that the expected outcome of the classification is annotated accordingly."));
            }

            // iff multi-label classification is active, no single-label data writer may be used
            if (learningModeI == 2) {
                if (dataWriter.equals("de.tudarmstadt.ukp.dkpro.tc.weka.WekaDataWriter")) {
                    throw new AnalysisEngineProcessException(
                            new TextClassificationException(
                                    "Your experiment is configured to be multi-label. Please use a DataWriter, which is able to handle multi-label data."));
                }
                if (bipartitionThreshold == null) {
                    throw new AnalysisEngineProcessException(
                            new TextClassificationException(
                                    "Your experiment is configured to be multi-label. Please set a bipartition threshold."));
                }
            }

            // iff single-label is configured, there may not be more than one outcome annotation per
            // CAS, except the experiment is unit or sequence classification
            if (learningModeI != 2 && featureModeI != 2 && featureModeI != 4 && outcomes.size() > 2) {
                throw new AnalysisEngineProcessException(
                        new TextClassificationException(
                                "Your experiment is configured to be single-label, but I found more than one outcome annotation for "
                                        + DocumentMetaData.get(jcas).getDocumentUri()
                                        + ". Please configure your project to be multi-label or make sure to have only one outcome per instance."));
            }

            // iff unit/sequence classification is active, there must be classificationUnit
            // annotations, each
            // labeled with an outcome annotation
            if (featureModeI == 2 || featureModeI == 4) {
                if (classificationUnits.size() == 0) {
                    throw new AnalysisEngineProcessException(
                            new TextClassificationException(
                                    "Your experiment is configured to have classification units. Please add classification unit annotations to the CAS while reading your initial files."));
                }
                else {
                    for (TextClassificationUnit classificationUnit : classificationUnits) {
                        if (JCasUtil.selectCovered(jcas, TextClassificationOutcome.class,
                                classificationUnit).size() == 0) {
                            throw new AnalysisEngineProcessException(
                                    new TextClassificationException(
                                            "I did not find an outcome annotation for "
                                                    + classificationUnit.getCoveredText()
                                                    + ". Please add outcome annotations for all classification units."));
                        }
                    }
                }
            }

            // iff pair classification is set, 2 views need to be present
            if (featureModeI == 3) {
                try {
                    jcas.getView(Constants.PART_ONE);
                    jcas.getView(Constants.PART_TWO);
                }
                catch (CASException e) {
                    throw new AnalysisEngineProcessException(new TextClassificationException(
                            "Your experiment is configured to be pair classification, but I could not find the two views "
                                    + Constants.PART_ONE + " and "
                                    + Constants.PART_TWO
                                    + ". Please use a reader that inhereits from "
                                    + Constants.class.getName()));
                }
            }

            // iff sequence classification is enabled, we currently only support single-label
            // classification
            if (featureModeI == 4 && learningModeI != 1) {
                throw new AnalysisEngineProcessException(
                        new TextClassificationException(
                                "In sequence mode, only single-label learning is possible. Please set the learning mode to single-label."));
            }

            // verify feature extractors are valid within the specified mode
            try {
                switch (featureModeI) {
                case 1:
                    for (String featExt : featureExtractors) {
                        FeatureExtractorResource_ImplBase featExtC = (FeatureExtractorResource_ImplBase) Class
                                .forName(featExt).newInstance();
                        if (!(featExtC instanceof DocumentFeatureExtractor)) {
                            throw new AnalysisEngineProcessException(
                                    new TextClassificationException(featExt
                                            + " is not a valid Document Feature Extractor."));
                        }
                        if (featExtC instanceof DocumentFeatureExtractor
                                && (featExtC instanceof ClassificationUnitFeatureExtractor || featExtC instanceof PairFeatureExtractor)) {
                            throw new AnalysisEngineProcessException(
                                    new TextClassificationException(featExt
                                            + ": Feature Extractors need to define a unique type."));
                        }
                    }
                    break;
                case 2:
                    testUnitFE(featureExtractors, developerMode);
                    break;
                case 3:
                    for (String featExt : featureExtractors) {
                        FeatureExtractorResource_ImplBase featExtC = (FeatureExtractorResource_ImplBase) Class
                                .forName(featExt).newInstance();
                        if (!(featExtC instanceof PairFeatureExtractor)) {
                            throw new AnalysisEngineProcessException(
                                    new TextClassificationException(featExt
                                            + " is not a valid Pair Feature Extractor."));
                        }
                        if (featExtC instanceof PairFeatureExtractor
                                && (featExtC instanceof DocumentFeatureExtractor || featExtC instanceof ClassificationUnitFeatureExtractor)) {
                            throw new AnalysisEngineProcessException(
                                    new TextClassificationException(featExt
                                            + ": Feature Extractors need to define a unique type."));
                        }
                    }
                    break;
                case 4:
                    testUnitFE(featureExtractors, developerMode);
                    break;
                default:
                    throw new AnalysisEngineProcessException("Please set a valid learning mode",
                            null);

                }
            }
            catch (ClassNotFoundException e) {
                throw new AnalysisEngineProcessException(e);
            }
            catch (InstantiationException e) {
                throw new AnalysisEngineProcessException(e);
            }
            catch (IllegalAccessException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    private static void testUnitFE(String[] featureExtractors, boolean developerMode)
        throws AnalysisEngineProcessException, InstantiationException, IllegalAccessException,
        ClassNotFoundException
    {
        for (String featExt : featureExtractors) {
            FeatureExtractorResource_ImplBase featExtC = (FeatureExtractorResource_ImplBase) Class
                    .forName(featExt).newInstance();
            if (!(featExtC instanceof ClassificationUnitFeatureExtractor)) {
                if (developerMode && featExtC instanceof DocumentFeatureExtractor) {
                    // we have the user carrying any consequences...
                }
                else {
                    throw new AnalysisEngineProcessException(new TextClassificationException(
                            featExt + " is not a valid Unit Feature Extractor."));
                }
            }
            if (featExtC instanceof ClassificationUnitFeatureExtractor
                    && (featExtC instanceof DocumentFeatureExtractor || featExtC instanceof PairFeatureExtractor)) {
                throw new AnalysisEngineProcessException(new TextClassificationException(featExt
                        + ": Feature Extractors need to define a unique type."));
            }
        }
    }
}