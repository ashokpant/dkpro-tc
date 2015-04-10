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
package de.tudarmstadt.ukp.dkpro.tc.groovyexamples.single.unit;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import de.tudarmstadt.ukp.dkpro.tc.groovyexamples.single.unit.BrownUnitPosDemo;
import de.tudarmstadt.ukp.dkpro.tc.groovyexamples.utils.GroovyDemosTest_Base;

/**
 * This test just ensures that the experiment runs without throwing
 * any exception.
 * 
 * @author Oliver Ferschke, Emily Jamison
 * 
 */
public class NERUnitDemoTest extends GroovyDemosTest_Base
{
    NERUnitDemo groovyExperiment;

    @Before
    public void setup()
        throws Exception
    {
        super.setup();
        
        groovyExperiment = new NERUnitDemo();
    }

    @Test
    public void testGroovyCrossValidation()
        throws Exception
    {
        groovyExperiment.runCrossValidation();
    }
}