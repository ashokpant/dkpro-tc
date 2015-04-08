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
package de.tudarmstadt.ukp.dkpro.tc.integrationtest

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription

import org.apache.uima.analysis_engine.AnalysisEngineDescription
import org.apache.uima.resource.ResourceInitializationException

import weka.classifiers.bayes.NaiveBayes
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter
import de.tudarmstadt.ukp.dkpro.lab.Lab
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy
import de.tudarmstadt.ukp.dkpro.tc.core.Constants
import de.tudarmstadt.ukp.dkpro.tc.examples.io.TwentyNewsgroupsCorpusReader
import de.tudarmstadt.ukp.dkpro.tc.features.length.NrOfTokensDFE
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.LuceneNGramDFE
import de.tudarmstadt.ukp.dkpro.tc.ml.ExperimentCrossValidation
import de.tudarmstadt.ukp.dkpro.tc.ml.ExperimentTrainTest
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchCrossValidationReport;
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchOutcomeIDReport;
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchTrainTestReport;
import de.tudarmstadt.ukp.dkpro.tc.weka.WekaClassificationAdapter
import de.tudarmstadt.ukp.dkpro.tc.weka.report.WekaClassificationReport

/**
 * Experiment setup used to test extreme configuration settings like empty feature extractors etc.
 * The experiment setup was adapted from the TwentyNewsgroups example.
 *
 * @author zesch
 * @author Oliver Ferschke
 */
public class ExtremeConfigurationSettingsExperiment implements Constants {

    def experimentName = "ExtremeConfigurationSettingsTest"

    // === PARAMETERS===========================================================

    def corpusFilePathTrain = "src/main/resources/data/twentynewsgroups/train"
    def corpusFilePathTest  ="src/main/resources/data/twentynewsgroups/test"
    def languageCode = "en"
    def numFolds = 2
    def manyFolds = 10

    // === DIMENSIONS===========================================================

    def dimReaders = Dimension.createBundle("readers", [
        readerTest: TwentyNewsgroupsCorpusReader.class,
        readerTestParams: [
            TwentyNewsgroupsCorpusReader.PARAM_SOURCE_LOCATION,
            corpusFilePathTest,
            TwentyNewsgroupsCorpusReader.PARAM_LANGUAGE,
            languageCode,
            TwentyNewsgroupsCorpusReader.PARAM_PATTERNS,
            TwentyNewsgroupsCorpusReader.INCLUDE_PREFIX + "*/*.txt"
        ],
        readerTrain: TwentyNewsgroupsCorpusReader.class,
        readerTrainParams: [
            TwentyNewsgroupsCorpusReader.PARAM_SOURCE_LOCATION,
            corpusFilePathTrain,
            TwentyNewsgroupsCorpusReader.PARAM_LANGUAGE,
            languageCode,
            TwentyNewsgroupsCorpusReader.PARAM_PATTERNS,
            TwentyNewsgroupsCorpusReader.INCLUDE_PREFIX + "*/*.txt"]
    ])

    def dimLearningMode = Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL)
    def dimFeatureMode = Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT)

    //UIMA parameters for FE configuration
    def dimPipelineParameters = Dimension.create(
    DIM_PIPELINE_PARAMS,
    [
        "TopK",
        "500",
        LuceneNGramDFE.PARAM_NGRAM_MIN_N,
        1,
        LuceneNGramDFE.PARAM_NGRAM_MAX_N,
        3
    ]
    )

    //UIMA parameters for FE configuration
    def dimPipelineParametersEmpty = Dimension.create(DIM_PIPELINE_PARAMS, [])

    def dimClassificationArgs =
    Dimension.create(
    DIM_CLASSIFICATION_ARGS,
    [
        [NaiveBayes.class.name].toArray()
    ] as Object[]
    )

    def dimClassificationArgsEmpty =
    Dimension.create(DIM_CLASSIFICATION_ARGS, [] as Object[])

    def dimFeatureSets = Dimension.create(
    DIM_FEATURE_SET,
    [
        [
            NrOfTokensDFE.class.name,
            LuceneNGramDFE.class.name
        ].toArray()
    ] as Object[]
    )

    def dimFeatureSetsEmpty = Dimension.create(DIM_FEATURE_SET, [] as Object[])

    // === Test =========================================================

    public void runEmptyPipelineParameters() throws Exception
    {

        ExperimentCrossValidation batchTask = [
            experimentName: experimentName + "-CV-Groovy",
            type: "Evaluation-"+ experimentName +"-CV-Groovy",
            preprocessing: getPreprocessing(),
            machineLearningAdapter: WekaClassificationAdapter,
            innerReports: [
                WekaClassificationReport
            ],
            parameterSpace : [
                dimReaders,
                dimFeatureMode,
                dimLearningMode,
                dimClassificationArgs,
                dimFeatureSets,
                dimPipelineParametersEmpty
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [
                BatchCrossValidationReport
            ],
            numFolds: numFolds]

        Lab.getInstance().run(batchTask)

        ExperimentTrainTest TrainTestExperiment = [
            experimentName: experimentName + "-TrainTest-Groovy",
            type: "Evaluation-"+ experimentName +"-TrainTest-Groovy",
            preprocessing: getPreprocessing(),
            machineLearningAdapter: WekaClassificationAdapter,
            innerReports: [
                WekaClassificationReport
            ],
            parameterSpace : [
                dimReaders,
                dimFeatureMode,
                dimLearningMode,
                dimClassificationArgs,
                dimFeatureSets,
                dimPipelineParametersEmpty
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [
                BatchTrainTestReport,
                BatchOutcomeIDReport]
        ]

        // Run
        Lab.getInstance().run(TrainTestExperiment)
    }

    public void runEmptyFeatureExtractorSet() throws Exception
    {

        ExperimentCrossValidation batchTask = [
            experimentName: experimentName + "-CV-Groovy",
            type: "Evaluation-"+ experimentName +"-CV-Groovy",
            preprocessing: getPreprocessing(),
            machineLearningAdapter: WekaClassificationAdapter,
            innerReports: [
                WekaClassificationReport
            ],
            parameterSpace : [
                dimReaders,
                dimFeatureMode,
                dimLearningMode,
                dimClassificationArgs,
                dimFeatureSetsEmpty,
                dimPipelineParameters
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [
                BatchCrossValidationReport
            ],
            numFolds: numFolds]

        Lab.getInstance().run(batchTask)

        ExperimentTrainTest TrainTestExperiment = [
            experimentName: experimentName + "-TrainTest-Groovy",
            type: "Evaluation-"+ experimentName +"-TrainTest-Groovy",
            preprocessing: getPreprocessing(),
            machineLearningAdapter: WekaClassificationAdapter,
            innerReports: [
                WekaClassificationReport
            ],
            parameterSpace : [
                dimReaders,
                dimFeatureMode,
                dimLearningMode,
                dimClassificationArgs,
                dimFeatureSetsEmpty,
                dimPipelineParameters
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [
                BatchTrainTestReport,
                BatchOutcomeIDReport]
        ]

        // Run
        Lab.getInstance().run(TrainTestExperiment)
    }

    private AnalysisEngineDescription getPreprocessing()
    throws ResourceInitializationException
    {
        return createEngineDescription(
        createEngineDescription(BreakIteratorSegmenter.class),
        createEngineDescription(OpenNlpPosTagger.class)
        )
    }
}