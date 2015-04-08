/*******************************************************************************
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universit??t Darmstadt
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

/* First created by JCasGen Tue May 06 17:51:57 CEST 2014 */
package de.tudarmstadt.ukp.dkpro.tc.api.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed Nov 05 14:39:52 CET 2014
 * XML source: /Users/zesch/Documents/workspace/dkpro-tc/dkpro-tc-api/src/main/resources/desc/type/TextClassification.xml
 * @generated */
public class TextClassificationUnit extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(TextClassificationUnit.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected TextClassificationUnit() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public TextClassificationUnit(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public TextClassificationUnit(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public TextClassificationUnit(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
  //*--------------*
  //* Feature: suffix

  /** getter for suffix - gets A customizable suffix that will be attached to the instance id.

This can be used to add additional information e.g. for identifying the unit.
   * @generated
   * @return value of the feature 
   */
  public String getSuffix() {
    if (TextClassificationUnit_Type.featOkTst && ((TextClassificationUnit_Type)jcasType).casFeat_suffix == null)
      jcasType.jcas.throwFeatMissing("suffix", "de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TextClassificationUnit_Type)jcasType).casFeatCode_suffix);}
    
  /** setter for suffix - sets A customizable suffix that will be attached to the instance id.

This can be used to add additional information e.g. for identifying the unit. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setSuffix(String v) {
    if (TextClassificationUnit_Type.featOkTst && ((TextClassificationUnit_Type)jcasType).casFeat_suffix == null)
      jcasType.jcas.throwFeatMissing("suffix", "de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit");
    jcasType.ll_cas.ll_setStringValue(addr, ((TextClassificationUnit_Type)jcasType).casFeatCode_suffix, v);}    
   
    
  //*--------------*
  //* Feature: id

  /** getter for id - gets 
   * @generated
   * @return value of the feature 
   */
  public int getId() {
    if (TextClassificationUnit_Type.featOkTst && ((TextClassificationUnit_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit");
    return jcasType.ll_cas.ll_getIntValue(addr, ((TextClassificationUnit_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(int v) {
    if (TextClassificationUnit_Type.featOkTst && ((TextClassificationUnit_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit");
    jcasType.ll_cas.ll_setIntValue(addr, ((TextClassificationUnit_Type)jcasType).casFeatCode_id, v);}    
  }

    