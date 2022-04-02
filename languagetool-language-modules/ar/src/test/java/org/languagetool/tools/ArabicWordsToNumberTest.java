/* LanguageTool, a natural language style checker
 * Copyright (C) 2021 Sohaib Afifi, Taha Zerrouki
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.tools;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**

   * @author Taha Zerrouki
 */




public class ArabicWordsToNumberTest {
  final boolean debug= false;

  @Test
  public void testNumberPhrase() {
   String text ="تسعمئة وثلاث وعشرون ألفا وتسعمئة وواحد";
   int x = ArabicNumbersWords.textToNumber(text);
   System.out.println("Phrase to Number text: "+ text +" "+Integer.toString(x));

  }
  @Test
  public void testBidiNumberPhrase() {
//    String text ="خمسمئة وثلاث وعشرون ألفا وتسعمئة وواحد";
    String text =" ثمانية وتسعون ألفاً وتسعمائة وخمسة وثمانون";
    Integer x =  ArabicNumbersWords.textToNumber(text);
    String text2 = ArabicNumbersWords.numberToArabicWords(Integer.toString(x),true,false,"jar");
    System.out.println("text: "+ text +" detected "+Integer.toString(x)+ " |"+text2);

  }
  @Test
  public void testNumberPhraseRandom() {

    for (Integer i = 1000; i < 99000; i++) {
      String text = ArabicNumbersWords.numberToArabicWords(Integer.toString(i));
      Integer x = ArabicNumbersWords.textToNumber(text);
      if (!i.equals(x)) {
        System.out.println("text: " + text + " detected " + Integer.toString(x) + " != " + Integer.toString(i));
      }

    }
  }
@Test
  public void testUnitsHelper() {
 // test gender
    assertEquals(ArabicUnitsHelper.isFeminin("ليرة"), true);
    assertEquals(ArabicUnitsHelper.isFeminin("دينار"), false);
    assertEquals(ArabicUnitsHelper.isFeminin("فلس"), false);
// test forms
  assertEquals(ArabicUnitsHelper.getOneForm("ليرة" ,"jar"), "ليرةٍ");
  assertEquals(ArabicUnitsHelper.getPluralForm("ليرة" ,"jar"), "ليراتٍ");
  assertEquals(ArabicUnitsHelper.getTwoForm("دولار" ,"jar"), "دولارين");
  assertEquals(ArabicUnitsHelper.getTwoForm("أوقية" ,"jar"), "[[أوقية]]");
    }

    @Test
  public void testNumberToWordsWithUnits() {

    String unit = "دينار";
    String inflection = "jar";
    Integer[] numbers = new Integer[] {0,1,2,3,11,12,14,34,100,125,134,1922, 1245701,102, 10000};
    for( Integer n: numbers)
    {
      String phrase = numberToWordsWithUnits(n, unit, inflection);
      System.out.println("N: "+ Integer.toString(n)+ " phrase:"+phrase);
    }
    }

    /* generate a phrase number with unit */
  public String numberToWordsWithUnits(Integer n, String unit, String inflection) {
    ArabicUnitsHelper unithelper = new ArabicUnitsHelper();
    // get feminin from unit
    boolean feminin = unithelper.isFeminin(unit);
    // inflection
    // generate phrase from number
    String numberPhrase = ArabicNumbersWords.numberToArabicWords(Integer.toString(n),feminin, false, "");
    StringBuilder phrase = new StringBuilder("");
    // generate suitable unit
    if(n.equals(0))
    {
      String new_unit = unithelper.getPluralForm(unit, "nasb");
      phrase.append("لا");
      phrase.append(" ");
      phrase.append(new_unit);
    }
    else if(n.equals(1))
    { // دينار واحد
      // دينارا واحدا
      // دينارٍ واحدٍ
      String new_unit = unithelper.getOneForm(unit, inflection);
      phrase.append(new_unit);
      phrase.append(" ");
      phrase.append(numberPhrase);
    }    else if(n.equals(2))
    { // ديناران
      // دينارين
      String new_unit = unithelper.getTwoForm(unit, inflection);
      phrase.append(new_unit);
    }

    else if(n%100==1)
    { // مئة دينار ودينار
      // ألف دينار ودينار
      // regenerate the phrase number for n-1
      // than add unit for hundreds
      // than add one unit
      // مئة دينار
      // ودينار
      String numberPhrase_hundred = ArabicNumbersWords.numberToArabicWords(Integer.toString(n-1),feminin, false, "");
      String new_unit_hundred = unithelper.getOneForm(unit, "jar");
      // unit for one
      String new_unit_one = unithelper.getOneForm(unit, inflection);
      phrase.append(numberPhrase_hundred);
      phrase.append(" ");
      phrase.append(new_unit_hundred);
      phrase.append(" ");
      phrase.append("و");
      phrase.append(new_unit_one);

    }    else if(n%100==2)
    { // مئة دينار ودينارين
      // ألف دينار ودينارين
      // regenerate the phrase number for n-1
      // than add unit for hundreds
      // than add two unit
      // مئة دينار
      // ودينارين
      String numberPhrase_hundred = ArabicNumbersWords.numberToArabicWords(Integer.toString(n-2),feminin, false, "");
      String new_unit_hundred = unithelper.getOneForm(unit, "jar");
      // unit for two
      String new_unit_two = unithelper.getTwoForm(unit, inflection);
      phrase.append(numberPhrase_hundred);
      phrase.append(" ");
      phrase.append(new_unit_hundred);
      phrase.append(" ");
      phrase.append("و");
      phrase.append(new_unit_two);

    }
    else if(n%100>=3 && n%100<=10)
    { // خمسة دنانير
      // عشرة رجال
      String new_unit = unithelper.getPluralForm(unit, "jar");
      phrase.append(numberPhrase);
      phrase.append(" ");
      phrase.append(new_unit);

    }
    else if(n%100>=11)
    { // أحد عشر رجلا
      // ثمانون رجلا
      // تسعة وتسعون رجلا
      String new_unit = unithelper.getOneForm(unit, "nasb");
      phrase.append(numberPhrase);
      phrase.append(" ");
      phrase.append(new_unit);

    }
    else if(n%100==0)
    { // مئة دينار
      // ثمانون ألف دينار
      // تسعة وتسعون مليون دينار
      String new_unit = unithelper.getOneForm(unit, "jar");
      phrase.append(numberPhrase);
      phrase.append(" ");
      phrase.append(new_unit);
    }
    else
    {
      phrase.append(numberPhrase);
      phrase.append(" **");
      phrase.append(unit+"**");
    }
    return phrase.toString();
  }

}
