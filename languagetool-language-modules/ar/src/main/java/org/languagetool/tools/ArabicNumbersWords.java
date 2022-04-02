package org.languagetool.tools;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;


/**
 * @author Mouamle (https://github.com/MouamleH) 07/09/2021
 * @author bluemix (https://github.com/bluemix) 23/08/15
 */
public class ArabicNumbersWords {

  public static String numberToArabicWords(String n) {
    return numberToArabicWords(n, false);
  }

  public static String numberToArabicWords(String number, boolean isFeminine) {
    return numberToArabicWords(new BigInteger(number), isFeminine);
  }
  public static String numberToArabicWords(String number, boolean isFeminine, boolean isAttached, String inflectionCase) {
    return numberToArabicWords(new BigInteger(number), isFeminine, isAttached, inflectionCase);
  }

  public static String numberToArabicWords(BigInteger number, boolean isFeminine) {
    return convertToArabic(number, isFeminine).trim();
  }
  public static String numberToArabicWords(BigInteger number, boolean isFeminine, boolean isAttached, String inflectionCase) {
    return convertToArabic(number, isFeminine, isAttached, inflectionCase).trim();
  }

  @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
  private static String convertToArabic(BigInteger number, boolean isFeminine)
  {
    return convertToArabic(number, isFeminine, false, "");
  }
  @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
  private static String convertToArabic(BigInteger number, boolean isFeminine, boolean isAttached, String inflectionCase) {
    if (number.equals(BigInteger.ZERO)) {
      return "صفر";
    }    else if (number.equals(BigInteger.ONE)) {
      return "واحد";
    }
    else if (number.equals(new BigInteger("2"))) {
      return getDigitInflectedStatus(2,0, false, false, inflectionCase);
    }
    BigDecimal tempNumber = new BigDecimal(number);

    StringBuilder result = new StringBuilder();
    short group = 0;

    while (tempNumber.compareTo(BigDecimal.ONE) >= 0) {

      // separate number into groups
      BigDecimal numberToProcess = tempNumber.remainder(new BigDecimal("1000"));

      tempNumber = tempNumber.divide(new BigDecimal("1000"));

      // convert group into its text
      final int tempValue = tempNumber.setScale(0, RoundingMode.FLOOR).intValue();
      String groupDescription = processArabicGroup(numberToProcess.intValue(), group, tempValue, isFeminine, isAttached, inflectionCase);

      if (!groupDescription.isEmpty()) { // here we add the new converted group to the previous concatenated text
        if (group > 0) {
          if (result.length() > 0) {
            result.insert(0, "و" + "");
          }
//
//          System.out.println("NumberTo process: "+ numberToProcess.toString() + Boolean.toString((numberToProcess.compareTo(new BigDecimal(2)) != 0)));
          int numberToProcessIntValue= numberToProcess.intValue();
//          System.out.println(two.toString());
          if (numberToProcess.intValue() != 2) {
            if (numberToProcess.remainder(new BigDecimal("100")).intValue() != 1) {
              if (numberToProcess.compareTo(new BigDecimal("3")) >= 0 && numberToProcess.compareTo(new BigDecimal("10")) <= 0) { // for numbers between 3 and 9 we use plural name
                result.insert(0, ArabicNumbersWordsConstants.arabicPluralGroups.get(group) + " ");
              } else {
                if (result.length() > 0) { // use appending case
//                  System.out.println("result: "+ result.toString());
                  result.insert(0, ArabicNumbersWordsConstants.arabicAppendedGroup.get(group) + " ");
//                  System.out.println("result2: "+ result.toString());
                } else {
                  result.insert(0, ArabicNumbersWordsConstants.arabicGroup.get(group) + " "); // use normal case
                }
              }
            }
          }
        }
        result.insert(0, groupDescription + " ");
      }

      group++;
    }

    return ((result.length() > 0) ? result.toString() : "");
  }


  private static String processArabicGroup(int groupNumber, int groupLevel, int remainingNumber, boolean isFeminine)
  {
    return processArabicGroup(groupNumber, groupLevel, remainingNumber, isFeminine, false, "");
  }
  private static String processArabicGroup(int groupNumber, int groupLevel, int remainingNumber, boolean isFeminine, Boolean isAttached, String inflectionCase) {
    int tens = groupNumber % 100;

    int hundreds = groupNumber / 100;

    String result = "";

    if (hundreds > 0) {
      if (tens == 0 && hundreds == 2) { // حالة المضاف
        if (groupLevel == 0) {
          result = getDigitHundredJarStatus(hundreds, inflectionCase);
        } else {
//          result = ArabicNumbersWordsConstants.arabicAppendedTwos.get(0);
          result = getDigitTwosJarStatus(0, inflectionCase, true);
        }
      } else { //  الحالة العادية
        result = getDigitHundredJarStatus(hundreds, inflectionCase);
      }
    }

    if (tens > 0) {
      if (tens < 20) { // if we are processing under 20 numbers
        if (tens == 2 && hundreds == 0 && groupLevel > 0) { // This is special case for number 2 when it comes alone in the group
//          result = ArabicNumbersWordsConstants.arabicTwos.get(groupLevel);//  في حالة الافراد
          result = getDigitTwosJarStatus(groupLevel, inflectionCase, false);;//  في حالة الافراد
        } else { // General case
          if (!result.isEmpty())
            result += " و";

          if (tens == 1 && groupLevel > 0)
            result += ArabicNumbersWordsConstants.arabicGroup.get(groupLevel);
          else if ((tens == 1 || tens == 2)
            && (groupLevel == 0 || groupLevel == -1)
            && hundreds == 0
            && remainingNumber == 0)
            result += ""; // Special case for 1 and 2 numbers like: ليرة سورية و ليرتان سوريتان
          else
            result += getDigitInflectedStatus(tens, groupLevel, isFeminine, isAttached, inflectionCase);// Get Feminine status for this digit
        }
      } else {
        int ones = tens % 10;
        tens = (tens / 10) - 2; // 20's offset

        if (ones > 0) {
          if (!result.isEmpty())
            result += " و";

          // Get Feminine status for this digit
          result += getDigitInflectedStatus(ones, groupLevel, isFeminine, isAttached, inflectionCase);
        }

        if (!result.isEmpty())
          result += " و";

        // Get Tens text
        // get ten text for inflected case jar or nasb
        result += getDigitTensJarStatus(tens, inflectionCase);
//        result += ArabicNumbersWordsConstants.arabicTens.get(tens);
      }
    }

    return result;
  }
  private static String getDigitFeminineStatus(int digit, int groupLevel, boolean isFeminine)
  {
    return getDigitInflectedStatus(digit, groupLevel, isFeminine, false, "" );
  }
  private static String getDigitInflectedStatus(int digit, int groupLevel, boolean isFeminine, Boolean isAttached, String inflectionCase) {
    if(inflectionCase.equals("jar")) {
      if (groupLevel == -1 || groupLevel == 0) {
        if (!isFeminine) {
          return ArabicNumbersWordsConstants.arabicJarOnes.get(digit);
        }
        return ArabicNumbersWordsConstants.arabicJarFeminineOnes.get(digit);
      }
    }
    else
    {
      if (groupLevel == -1 || groupLevel == 0) {
        if (!isFeminine) {
          return ArabicNumbersWordsConstants.arabicOnes.get(digit);
        }
        return ArabicNumbersWordsConstants.arabicFeminineOnes.get(digit);
      }
    }
    return ArabicNumbersWordsConstants.arabicOnes.get(digit);

  }
  private static String getDigitTensJarStatus(int digit, String inflectionCase) {

    if(inflectionCase.equals("jar") || inflectionCase.equals("jar"))
        return ArabicNumbersWordsConstants.arabicJarTens.get(digit);
    return ArabicNumbersWordsConstants.arabicTens.get(digit);
  }
  private static String getDigitHundredJarStatus(int digit, String inflectionCase) {

    if(inflectionCase.equals("jar") || inflectionCase.equals("jar"))
        return ArabicNumbersWordsConstants.arabicJarHundreds.get(digit);
    return ArabicNumbersWordsConstants.arabicHundreds.get(digit);
  }
  private static String getDigitTwosJarStatus(int digit, String inflectionCase, boolean isAppended) {
    if(!isAppended) {
      if (inflectionCase.equals("jar") || inflectionCase.equals("jar")) {
        return ArabicNumbersWordsConstants.arabicJarTwos.get(digit);
      }
        return ArabicNumbersWordsConstants.arabicTwos.get(digit);
      }
    else{
      if (inflectionCase.equals("jar") || inflectionCase.equals("jar")) {
        return ArabicNumbersWordsConstants.arabicJarAppendedTwos.get(digit);
      }
      return ArabicNumbersWordsConstants.arabicAppendedTwos.get(digit);
    }


  }

  private static boolean hasNoGroup(StringBuilder word)
  {
    String aword = word.toString();
    return (aword.equals("ألف")
    ||aword.equals("ألفين")
    ||aword.equals("ألفان")
      );

  }

  /*
     Convert arabic text into number, for example convert تسعة وعشرون = >29.
     Example:
         >>> text2number(u"خمسمئة وثلاث وعشرون")
         523
     @param text: input text
     @type text: unicode
     @return: number extracted from text
     @rtype: integer
 */
  public static Integer textToNumber(String text)
  {
    text = ArabicStringTools.removeTashkeel(text);
    List<String> words = Arrays.asList(text.split(" "));
    return textToNumber(words);
  }
  public static Integer textToNumber(List<String> words) {

    // the result total is 0
    Integer total = 0;
    // the partial total for the three number
    Integer partial = 0;;
    // print words
    for(String word : words)
    {
      if (!word.isEmpty() && !word.equals("واحد")
        && (word.startsWith("و")
        || word.startsWith("ف")
        || word.startsWith("ب")
        || word.startsWith("ك")
        || word.startsWith("ل"))
      )
      { // strip first char
        word = word.substring(1);
      }
      if(!word.equals("واحد")  && word.startsWith("و"))
      {// strip first char
        word = word.substring(1);
      }
      if(isNumericWord(word))
      {
        Integer actualnumber = getNumericWordValue(word);
        if (actualnumber % 1000 == 0)
        {
          // the case of 1000 or 1 million
          if (partial == 0)
          {partial = 1;}
          total += partial * actualnumber;
          // re-initiate the partial total
          partial = 0;
        }
        else
        {
          partial += getNumericWordValue(word);
        }
      }
    }
    // add the final partial to total
    total += partial;
    return total;
  }
  /* get suitable unit form */
  public static String getUnitForm(Integer n, String unitLemma, String inflection)
  {
    ArabicUnitsHelper unithelper = new ArabicUnitsHelper();
    Map<String, String> phraseMap = numberToWordsWithUnitsMap(n, unitLemma, inflection, unithelper);
    return phraseMap.getOrDefault("unit","");

  }
  /* generate a phrase number with unit */
  public static  String numberToWordsWithUnits(Integer n, String unit, String inflection)
  {
    ArabicUnitsHelper unithelper = new ArabicUnitsHelper();
    Map<String, String> phraseMap = numberToWordsWithUnitsMap(n, unit, inflection, unithelper);
    return phraseMap.getOrDefault("all","");
  }
  /* generate a phrase number with unit */
  public static   Map<String, String> numberToWordsWithUnitsMap(Integer n, String unit, String inflection, ArabicUnitsHelper unithelper) {


      // get feminin from unit
      boolean feminin = unithelper.isFeminin(unit);
      // inflection
      // generate phrase from number
      String numberPhrase = ArabicNumbersWords.numberToArabicWords(Integer.toString(n), feminin, true, inflection);
      StringBuilder phrase = new StringBuilder("");
      String new_unit = "";
      // generate suitable unit
      if (n.equals(0)) {
        new_unit = unithelper.getPluralForm(unit, "nasb");
        phrase.append("لا");
        phrase.append(" ");
        phrase.append(new_unit);
      } else if (n.equals(1)) { // دينار واحد
        // دينارا واحدا
        // دينارٍ واحدٍ
        new_unit = unithelper.getOneForm(unit, inflection);
        phrase.append(new_unit);
        phrase.append(" ");
        phrase.append(numberPhrase);
      } else if (n.equals(2)) { // ديناران
        // دينارين
        new_unit = unithelper.getTwoForm(unit, inflection);
        phrase.append(new_unit);
      } else if (n % 100 == 1) { // مئة دينار ودينار
        // ألف دينار ودينار
        // regenerate the phrase number for n-1
        // than add unit for hundreds
        // than add one unit
        // مئة دينار
        // ودينار
        String numberPhrase_hundred = ArabicNumbersWords.numberToArabicWords(Integer.toString(n - 1), feminin, true, inflection);
        String new_unit_hundred = unithelper.getOneForm(unit, "jar");
        // unit for one
        String new_unit_one = unithelper.getOneForm(unit, inflection);
        new_unit = new_unit_one;
        phrase.append(numberPhrase_hundred);
        phrase.append(" ");
        phrase.append(new_unit_hundred);
        phrase.append(" ");
        phrase.append("و");
        phrase.append(new_unit_one);

      } else if (n % 100 == 2) { // مئة دينار ودينارين
        // ألف دينار ودينارين
        // regenerate the phrase number for n-1
        // than add unit for hundreds
        // than add two unit
        // مئة دينار
        // ودينارين
        String numberPhrase_hundred = ArabicNumbersWords.numberToArabicWords(Integer.toString(n - 2), feminin, true, inflection);
        String new_unit_hundred = unithelper.getOneForm(unit, "jar");
        // unit for two
        String new_unit_two = unithelper.getTwoForm(unit, inflection);
        new_unit = new_unit_two;
        phrase.append(numberPhrase_hundred);
        phrase.append(" ");
        phrase.append(new_unit_hundred);
        phrase.append(" ");
        phrase.append("و");
        phrase.append(new_unit_two);

      } else if (n % 100 >= 3 && n % 100 <= 10) { // خمسة دنانير
        // عشرة رجال
        new_unit = unithelper.getPluralForm(unit, "jar");
        phrase.append(numberPhrase);
        phrase.append(" ");
        phrase.append(new_unit);

      } else if (n % 100 >= 11) { // أحد عشر رجلا
        // ثمانون رجلا
        // تسعة وتسعون رجلا
        new_unit = unithelper.getOneForm(unit, "nasb");
        phrase.append(numberPhrase);
        phrase.append(" ");
        phrase.append(new_unit);

      } else if (n % 100 == 0) { // مئة دينار
        // ثمانون ألف دينار
        // تسعة وتسعون مليون دينار
        new_unit = unithelper.getOneForm(unit, "jar");
        phrase.append(numberPhrase);
        phrase.append(" ");
        phrase.append(new_unit);
      } else {
        phrase.append(numberPhrase);
        phrase.append(" **");
        phrase.append(unit + "**");
      }
      Map<String, String> phraseMap = new HashMap<>();
      phraseMap.put("all", phrase.toString());
      phraseMap.put("phrase", numberPhrase);
      phraseMap.put("unit", new_unit);
      phraseMap.put("number", String.valueOf(n));
      phraseMap.put("unitLemma", unit);
      phraseMap.put("inflection", inflection);
      phraseMap.put("feminin", String.valueOf(feminin));
      return phraseMap;
    }

  /* check if the given numeric phrase is correct and wekll spelling according to spectfic options */
  public static boolean checkNumericPhrase(String phrase_input, boolean feminin,  boolean attached, String inflection) {
    String phrase = ArabicStringTools.removeTashkeel(phrase_input);
    Integer x = ArabicNumbersWords.textToNumber(phrase);
    String autoPhrase = ArabicNumbersWords.numberToArabicWords(String.valueOf(x), feminin, attached, inflection);
    autoPhrase = ArabicStringTools.removeTashkeel(autoPhrase);
//    if(!autoPhrase.equals(phrase));
//    {
//      if(debug)
//        System.out.println("checkNumericPhrase::Input:'"+ phrase + "' Ouput: X: "+ x + " String:'"+ autoPhrase+"'");
//    }
    return autoPhrase.equals(phrase);
  }

  /* Generate suggestions fro  given numeric phrase if it's incorrect and wekll spelling according to spectfic options */
  public static List<String> getSuggestionsNumericPhrase(String phrase_input, boolean feminin,  boolean attached, String inflection) {
    List<String> suggestions = new ArrayList<>();
    String phrase = ArabicStringTools.removeTashkeel(phrase_input);
    Integer x = ArabicNumbersWords.textToNumber(phrase);
    String autoPhrase = ArabicNumbersWords.numberToArabicWords(String.valueOf(x), feminin, attached, inflection);
    autoPhrase = ArabicStringTools.removeTashkeel(autoPhrase);
    if(!autoPhrase.equals(phrase))
    {
      // if inflection is null generate all cases
      if(inflection!= null  && !inflection.isEmpty())
        suggestions.add(autoPhrase);
      else
      { //Raf3
        inflection = "raf3";
        autoPhrase = ArabicNumbersWords.numberToArabicWords(String.valueOf(x), feminin, attached, inflection);
        suggestions.add(autoPhrase);
        //nasb+jar
        inflection = "jar";
        autoPhrase = ArabicNumbersWords.numberToArabicWords(String.valueOf(x), feminin, attached, inflection);
        suggestions.add(autoPhrase);
      }
    }
    return suggestions;
  }
  /* return if the word is a numeric word */
  public static boolean isNumericWord(String word)
  {
    return ArabicNumbersWordsConstants.NUMBER_WORDS.containsKey(word);
  }
  /* return if the word is a numeric word */
  public static Integer getNumericWordValue(String word)
  {
    return ArabicNumbersWordsConstants.NUMBER_WORDS.get(word);
  }

}
