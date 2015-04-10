/**
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package de.tudarmstadt.ukp.dkpro.tc.groovyexamples.single.document

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription

import org.apache.uima.analysis_engine.AnalysisEngineDescription
import org.apache.uima.resource.ResourceInitializationException

import weka.classifiers.bayes.NaiveBayes
import weka.classifiers.functions.SMO
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter
import de.tudarmstadt.ukp.dkpro.lab.Lab
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy
import de.tudarmstadt.ukp.dkpro.tc.core.Constants
import de.tudarmstadt.ukp.dkpro.tc.core.task.ExtractFeaturesTask
import de.tudarmstadt.ukp.dkpro.tc.core.task.MetaInfoTask
import de.tudarmstadt.ukp.dkpro.tc.core.task.PreprocessTask
import de.tudarmstadt.ukp.dkpro.tc.examples.io.TwentyNewsgroupsCorpusReader
import de.tudarmstadt.ukp.dkpro.tc.examples.util.DemoUtils
import de.tudarmstadt.ukp.dkpro.tc.features.length.NrOfTokensDFE
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.LuceneNGramDFE
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.base.FrequencyDistributionNGramFeatureExtractorBase
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchOutcomeIDReport
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchTrainTestReport
import de.tudarmstadt.ukp.dkpro.tc.weka.WekaClassificationAdapter
import de.tudarmstadt.ukp.dkpro.tc.weka.report.WekaClassificationReport
import de.tudarmstadt.ukp.dkpro.tc.weka.report.WekaOutcomeIDReport
import de.tudarmstadt.ukp.dkpro.tc.weka.task.WekaTestTask

/**
 * Groovy-Version of the TwentyNewsgroupsExperiment
 *
 * The TwentyNewsgroupsGroovyExtendedExperiment does the same as TwentyNewsgroupsGroovyExperiment,
 * but it manually sets up the sub-tasks and builds a generic batch task.
 *
 * In TwentyNewsgroupsGroovyExperiment, this is done automatically in the {@link CrossValidationExperiment} and {@link TrainTestExperiment},
 * which is more convenient, but less flexible.
 *
 * Currently only supports train-test setup.
 *
 * @author Oliver Ferschke
 * @author daxenberger
 */
public class TwentyNewsgroupsDemoExtended implements Constants{

    // === PARAMETERS===========================================================

    def corpusFilePathTrain = "src/main/resources/data/twentynewsgroups/bydate-train"
    def corpusFilePathTest  ="src/main/resources/data/twentynewsgroups/bydate-test"
    def languageCode = "en"

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
            TwentyNewsgroupsCorpusReader.INCLUDE_PREFIX + "*/*.txt"
        ]
    ])

    def dimLearningMode = Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL)
    def dimFeatureMode = Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT)

    //UIMA parameters for FE configuration
    def dimPipelineParameters = Dimension.create(
    DIM_PIPELINE_PARAMS,
    [
        FrequencyDistributionNGramFeatureExtractorBase.PARAM_NGRAM_USE_TOP_K,
        "500",
        FrequencyDistributionNGramFeatureExtractorBase.PARAM_NGRAM_MIN_N,
        1,
        FrequencyDistributionNGramFeatureExtractorBase.PARAM_NGRAM_MAX_N,
        3
    ],
    [
        FrequencyDistributionNGramFeatureExtractorBase.PARAM_NGRAM_USE_TOP_K,
        "1000",
        FrequencyDistributionNGramFeatureExtractorBase.PARAM_NGRAM_MIN_N,
        1,
        FrequencyDistributionNGramFeatureExtractorBase.PARAM_NGRAM_MAX_N,
        3
    ])


    def dimClassificationArgs =
    Dimension.create(DIM_CLASSIFICATION_ARGS,
    [NaiveBayes.class.name],
    [SMO.class.name])

    def dimFeatureSets = Dimension.create(
    DIM_FEATURE_SET,
    [
        NrOfTokensDFE.class.name,
        LuceneNGramDFE.class.name
    ]
    )

    // === Experiments =========================================================

    /**
     * TrainTest Setting
     *
     * @throws Exception
     */
    protected void runTrainTest() throws Exception
    {
        /*
         * Define (instantiate) tasks
         */

        PreprocessTask preprocessTaskTrain = [
            preprocessing:getPreprocessing(),
            type: "Preprocessing-TwentyNewsgroups-Train",
            isTesting: false
        ]

        PreprocessTask preprocessTaskTest = [
            preprocessing:getPreprocessing(),
            type: "Preprocessing-TwentyNewsgroups-Test",
            isTesting: true
        ]

        MetaInfoTask metaTask = [
            type: "MetaInfoTask-TwentyNewsgroups-TrainTest",
        ]

        ExtractFeaturesTask featuresTrainTask = [
            type: "FeatureExtraction-TwentyNewsgroups-Train",
            isTesting: false,
            mlAdapter: WekaClassificationAdapter.instance
        ]

        ExtractFeaturesTask featuresTestTask = [
            type: "FeatureExtraction-TwentyNewsgroups-Test",
            isTesting: true,
            mlAdapter: WekaClassificationAdapter.instance
        ]

        WekaTestTask testTask = [
            type:"TestTask-TwentyNewsgroups",
            reports: [
                WekaClassificationReport,
                WekaOutcomeIDReport]
        ]


        /*
         * Wire tasks
         */
        metaTask.addImport(preprocessTaskTrain, PreprocessTask.OUTPUT_KEY_TRAIN, MetaInfoTask.INPUT_KEY)
        featuresTrainTask.addImport(preprocessTaskTrain, PreprocessTask.OUTPUT_KEY_TRAIN, ExtractFeaturesTask.INPUT_KEY)
        featuresTrainTask.addImport(metaTask, MetaInfoTask.META_KEY, MetaInfoTask.META_KEY)
        featuresTestTask.addImport(preprocessTaskTest, PreprocessTask.OUTPUT_KEY_TEST, ExtractFeaturesTask.INPUT_KEY)
        featuresTestTask.addImport(metaTask, MetaInfoTask.META_KEY, MetaInfoTask.META_KEY)
        featuresTestTask.addImport(featuresTrainTask, ExtractFeaturesTask.OUTPUT_KEY)
        testTask.addImport(featuresTrainTask, ExtractFeaturesTask.OUTPUT_KEY, Constants.TEST_TASK_INPUT_KEY_TRAINING_DATA)
        testTask.addImport(featuresTestTask, ExtractFeaturesTask.OUTPUT_KEY, Constants.TEST_TASK_INPUT_KEY_TEST_DATA)

        /*
         *	Wrap wired tasks in batch task
         */

        BatchTask batchTask = [
            type: "Evaluation-TwentyNewsgroups-TrainTest",
            parameterSpace : [
                dimReaders,
                dimLearningMode,
                dimFeatureMode,
                dimClassificationArgs,
                dimFeatureSets,
                dimPipelineParameters
            ],
            tasks:           [
                preprocessTaskTrain,
                preprocessTaskTest,
                metaTask,
                featuresTrainTask,
                featuresTestTask,
                testTask
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [
                BatchTrainTestReport,
                BatchOutcomeIDReport]
        ]

        // Run
        Lab.getInstance().run(batchTask)
    }

    private AnalysisEngineDescription getPreprocessing()
    throws ResourceInitializationException
    {
        return createEngineDescription(
        createEngineDescription(BreakIteratorSegmenter),
        createEngineDescription(OpenNlpPosTagger)
        )
    }

    public static void main(String[] args)
    {
		DemoUtils.setDkproHome(TwentyNewsgroupsDemoExtended.getSimpleName());
        new TwentyNewsgroupsDemoExtended().runTrainTest()
    }

}
