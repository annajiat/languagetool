/* LanguageTool, a natural language style checker
 * Copyright (C) 2020 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.rules.ar;

import org.jetbrains.annotations.Nullable;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.language.Arabic;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.SimpleReplaceDataLoader;
import org.languagetool.rules.patterns.RuleFilter;
import org.languagetool.synthesis.ar.ArabicSynthesizer;
import org.languagetool.tagging.ar.ArabicTagManager;
import org.languagetool.tagging.ar.ArabicTagger;
import org.languagetool.tools.ArabicNumbersWords;

import java.util.*;

import static java.lang.Math.min;
import static java.lang.Math.subtractExact;

/**
 * Filter that maps suggestion for numeric phrases.
 *
 */
public class NumberPhraseFilter extends RuleFilter {

  private final ArabicTagger tagger = new ArabicTagger();
  private static final ArabicTagManager tagmanager = new ArabicTagManager();
  private final ArabicSynthesizer synthesizer = new ArabicSynthesizer(new Arabic());




  @Nullable
  @Override
  public RuleMatch acceptRuleMatch(RuleMatch match, Map<String, String> arguments, int patternTokenPos, AnalyzedTokenReadings[] patternTokens) {

    // get the previous word
    String previousWord = arguments.getOrDefault("previous","");
    // previous word index in token list
    int previousWordPos =0;
    if(arguments.get("previousPos")!=null)
      try
      {if(arguments.get("previousPos")!=null)
        previousWordPos = Integer.valueOf(arguments.get("previousPos"));
      } catch (NumberFormatException e) {
        e.printStackTrace();
        previousWordPos = 0;
      }

    // get the inflect mark
    String inflectArg = arguments.getOrDefault("inflect","");
    // get the next  word as units
    String nextWord = arguments.getOrDefault("next","");
    int nextWordPos = 0;
    try
    {
     nextWordPos = Integer.valueOf(arguments.getOrDefault("nextPos", "0"));
    } catch (NumberFormatException e) {
      e.printStackTrace();
      nextWordPos = 0;
    }
    /// get all numeric tokens
    int end_pos = (nextWordPos>0) ? Integer.min(nextWordPos, patternTokens.length): patternTokens.length;
    int start_pos = (previousWordPos>0) ? previousWordPos: 0;
    List<String> numWordTokens =  new ArrayList<>();
    for(int i = start_pos; i< end_pos; i++)
//    for(int i = 1; i< patternTokens.length-1; i++)
//    for(int i = 0; i< patternTokens.length; i++)
      numWordTokens.add(patternTokens[i].getToken().trim());
    String numPhrase = String.join(" ", numWordTokens);
    /* extract features from previous */

    boolean feminin = false;
    boolean attached = false;
    String inflection = getInflectedCase(patternTokens, previousWordPos, inflectArg);


    List<String> suggestionList = ArabicNumbersWords.getSuggestionsNumericPhrase(numPhrase,feminin, attached, inflection);

    RuleMatch newMatch = new RuleMatch(match.getRule(), match.getSentence(), match.getFromPos(), match.getToPos(), match.getMessage(), match.getShortMessage());

    if(!suggestionList.isEmpty())
    {
      for(String sug: suggestionList)
        newMatch.addSuggestedReplacement(sug);
    }
//    else {
//      // Fake suggestion just for test
//      newMatch.setSuggestedReplacement("No Suggestion for {"+numPhrase+"}");
//    }
//    newMatch.setSuggestedReplacement("Fake Suggestion#"+numPhrase+"**"+numWordTokens.toString());
    return newMatch;


  }

  // extract inflection case
  private static String getInflectedCase(AnalyzedTokenReadings[] patternTokens, int previousPos, String inflect)
  {
    if(inflect!="")
      return inflect;
    // if the previous is Jar
    if(previousPos>=0 && previousPos <patternTokens.length) {
      AnalyzedTokenReadings previousToken = patternTokens[previousPos];
      for(AnalyzedToken tk: patternTokens[previousPos]) {
        if(tk.getPOSTag()!=null && tk.getPOSTag().startsWith("PR"))
          return "jar";
      }
//      if(previousToken.hasAnyPartialPosTag("PR"))
//      {
//        System.out.println("GetInflectedCase "+previousToken.getToken()+"jar");
//        return "jar";
//      }

    }
    String firstWord = patternTokens[previousPos+1].getToken();
    if(firstWord.startsWith("ب")
      ||firstWord.startsWith("ل")
      ||firstWord.startsWith("ك")
    )
      return "jar";
  return "";
  }
  // extract inflection case
  private static boolean getFemininCase(AnalyzedTokenReadings[] patternTokens, int nextPos)
  {
    // if the previous is Jar
    for(AnalyzedToken tk: patternTokens[nextPos]) {
      if(tagmanager.isFeminin(tk.getPOSTag()))
        return true;
    }
  return false;
  }


}
