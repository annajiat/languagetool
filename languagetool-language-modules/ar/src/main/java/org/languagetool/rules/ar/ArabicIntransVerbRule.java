/*
 * LanguageTool, a natural language style checker
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
package org.languagetool.rules.ar;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.language.Arabic;
import org.languagetool.rules.*;
import org.languagetool.synthesis.ar.ArabicSynthesizer;
import org.languagetool.tagging.ar.ArabicTagManager;
import org.languagetool.tagging.ar.ArabicTagger;

import java.util.*;

public class ArabicIntransVerbRule extends AbstractSimpleReplaceRule2 {

  public static final String AR_VERB_INTRANS_DIRECT_REPLACE = "AR_VERB_INTRANSITIVE_DIRECT";

  private static final String FILE_NAME = "/ar/verb_intrans_to_trans.txt";
  private static final Locale AR_LOCALE = new Locale("ar");

  private final ArabicTagger tagger;
  private final ArabicTagManager tagmanager;
  private final ArabicSynthesizer synthesizer;
  private final List<Map<String, SuggestionWithMessage>> wrongWords;

  public ArabicIntransVerbRule(ResourceBundle messages) {
    super(messages, new Arabic());
    tagger = new ArabicTagger();
    tagger.enableNewStylePronounTag();
    tagmanager = new ArabicTagManager();
    synthesizer = new ArabicSynthesizer(new Arabic());

    super.setCategory(Categories.MISC.getCategory(messages));
    setLocQualityIssueType(ITSIssueType.Inconsistency);
    //FIXME: choose another example
    addExamplePair(Example.wrong("قال <marker>كشفت</marker> الأمر الخفي."),
      Example.fixed("قال <marker>كشفت عن</marker> الأمر الخفي."));

    // get wrong words from resource file
    wrongWords = getWrongWords(false);
  }

  @Override
  public String getId() {
    return AR_VERB_INTRANS_DIRECT_REPLACE;
  }

  @Override
  public String getDescription() {
    return "َIntransitive verbs corrected to direct transitive";
  }

  @Override
  public final List<String> getFileNames() {
    return Collections.singletonList(FILE_NAME);
  }

  @Override
  public String getShort() {
    return "أفعال متعدية، يخطئ في تعديتها بحرف";
  }

  @Override
  public String getMessage() {
    return "'$match' الفعل خاطئ في التعدية بحرف: $suggestions";
  }

  @Override
  public String getSuggestionsSeparator() {
    return " أو ";
  }

  @Override
  public Locale getLocale() {
    return AR_LOCALE;
  }

  @Override
  public RuleMatch[] match(AnalyzedSentence sentence) {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    if (wrongWords.size() == 0) {
      return toRuleMatchArray(ruleMatches);
    }
    AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();
    int prevTokenIndex = 0;
    for (int i = 1; i < tokens.length; i++) {  // ignoring token 0, i.e., SENT_START
      AnalyzedTokenReadings token = tokens[i];
      AnalyzedTokenReadings prevToken = prevTokenIndex > 0 ? tokens[prevTokenIndex] : null;
      String prevTokenStr = prevTokenIndex > 0 ? tokens[prevTokenIndex].getToken() : null;

      if (prevTokenStr != null) {
        // test if the first token is a verb
        boolean is_attached_verb_transitive = isAttachedTransitiveVerb(prevToken);

        // test if the preposition token is suitable for verb token (previous)
        List <String> prepositions = new ArrayList<>();
        String sug_msg = "";
        SuggestionWithMessage prepositionsWithMessage = getProperPrepositionForTransitiveVerb(prevToken);
        if(prepositionsWithMessage!=null)
        {
          prepositions = Arrays.asList(prepositionsWithMessage.getSuggestion().split("\\|"));
          sug_msg = prepositionsWithMessage.getMessage();
          sug_msg = sug_msg != null ? sug_msg : "";
        }
        // the current token can be a preposition or any words else
        // test if the token is in the suitable prepositions
        // browse all next  tokens to assure that proper preposition doesn't exist
        boolean is_wrong_preposition = false;
        for(int next_i=i; next_i <tokens.length;next_i ++ ) {
          AnalyzedTokenReadings current_token = tokens[next_i];
          is_wrong_preposition = isWrongPreposition(current_token, prepositions);
          if(is_wrong_preposition) break;
        }
        // the verb is not attached and the next token is a preposition to be removed
        // we give the correct new form
        if (is_attached_verb_transitive && is_wrong_preposition) {
          // generate suggestion according to prepositions to be removed
          // generate a new form of verb according to current token
          String verb = generateAttachedVerbForm(prevToken, token);
          String  replacement = "<suggestion>" + verb + "</suggestion>";

          String msg =  "' الفعل " + prevTokenStr + " ' متعدٍ بنفسه،" + sug_msg +". فهل تقصد؟"+ replacement.toString();
          RuleMatch match = new RuleMatch(
            this, sentence, prevToken.getStartPos(), token.getEndPos(),
            prevToken.getStartPos(), token.getEndPos(), msg, "خطأ في الفعل المتعدي ");
          ruleMatches.add(match);
        }
      }

      if (isAttachedTransitiveVerb(token)) {
        prevTokenIndex = i;
      } else {
        prevTokenIndex = 0;
      }
    }
    return toRuleMatchArray(ruleMatches);
  }

  private boolean isAttachedTransitiveVerb(AnalyzedTokenReadings mytoken) {
    List<AnalyzedToken> verbTokenList = mytoken.getReadings();

    for (AnalyzedToken verbTok : verbTokenList) {
      String verbLemma = verbTok.getLemma();
      String verbPostag = verbTok.getPOSTag();

      // if postag is attached
      // test if verb is in the verb list
      if (verbPostag != null)// && verbPostag.endsWith("H"))
      {
        // lookup in WrongWords
        SuggestionWithMessage verbLemmaMatch = wrongWords.get(wrongWords.size() - 1).get(verbLemma);
        // The lemma is found in the dictionary file
        if (verbLemmaMatch != null)
          return true;
      }

    }
    return false;
  }

  /* if the word is a transitive verb, we get proper preposition in order to test it*/

  private SuggestionWithMessage getProperPrepositionForTransitiveVerb(AnalyzedTokenReadings mytoken) {
    List<AnalyzedToken> verbTokenList = mytoken.getReadings();

    // keep the suitable postags
//    List<String> replacements = new ArrayList<>();

    for (AnalyzedToken verbTok : verbTokenList) {
      String verbLemma = verbTok.getLemma();
      String verbPostag = verbTok.getPOSTag();

      // if postag is attached
      // test if verb is in the verb list
      if (verbPostag != null)
      {
        // lookup in WrongWords
        SuggestionWithMessage verbLemmaMatch = wrongWords.get(wrongWords.size() - 1).get(verbLemma);

        // The lemma is found in the dictionary file
        if (verbLemmaMatch != null) {
          return verbLemmaMatch;
        }
      }
    }
    return null;
  }

  private static boolean isWrongPreposition(AnalyzedTokenReadings nextToken, List<String> prepositionList) {
    //test if the next token  is the wrong preposition for the previous token as verbtoken

    for(AnalyzedToken tok : nextToken.getReadings()) {
        if (prepositionList.contains(tok.getLemma()))
        return true;
    }

    // FIXME: case of BEH and LAM
    return false;
  }

//  private String getCorrectVerbForm(AnalyzedTokenReadings token) {
//
//    return generateUnattachedNewForm(token);
//  }
//
//  private String getCorrectPrepositionForm(String prepositionLemma, AnalyzedTokenReadings prevtoken) {
//    return generateAttachedNewForm(prepositionLemma, prevtoken);
//  }
//
//  /* generate a new form according to a specific postag*/
//  private String generateNewForm(String word, String posTag, char flag) {
//    // generate new from word form
//    String newposTag = tagmanager.setFlag(posTag, "PRONOUN", flag);
//    // set conjunction flag
//    newposTag = tagmanager.setFlag(newposTag, "CONJ", '-');
//    newposTag = tagmanager.setFlag(newposTag, "ISTIQBAL", '-');
//    // FIXME: remove the specific flag for option D
////    if (flag != '-')
////      newposTag = tagmanager.setFlag(newposTag, "OPTION", 'D');
//    // generate the new preposition according to modified postag
//    AnalyzedToken newToken = new AnalyzedToken(word, newposTag, word);
//    String[] newwordList = synthesizer.synthesize(newToken, newposTag);
//    String newWord = "";
//    if (newwordList.length != 0) {
//      newWord = newwordList[0];
//      for(int k=0; k<newwordList.length; k++) {
//        System.out.println("generateNewForm" + newwordList[k] + " " + newposTag);
//      }
//    }
//    return newWord;
//  }
//
//  /* generate a new form according to a specific postag, this form is Un-Attached*/
//  private String generateUnattachedNewForm(AnalyzedTokenReadings token) {
//    String lemma = token.getReadings().get(0).getLemma();
//    String postag = token.getReadings().get(0).getPOSTag();
//    return generateNewForm(lemma, postag, '-');
//  }
//
//  /* generate a new form according to a specific postag, this form is Attached*/
//  private String generateAttachedNewForm(String prepositionLemma, AnalyzedTokenReadings prevtoken) {
//    // FIXME ; generate multiple cases
//    String postag = "PR-;---;---";
//    String prevPosTag = prevtoken.getReadings().get(0).getPOSTag();
//    char flag = tagmanager.getFlag(prevPosTag, "PRONOUN");
//    return generateNewForm(prepositionLemma, postag, flag);
//  }
  /* generate a new form according to a specific postag, this form is Attached*/
  private String generateAttachedVerbForm(AnalyzedTokenReadings verbToken, AnalyzedTokenReadings prepositionToken) {
    // extract verb postag
    // extract preposition postag
    // get pronoun flag
    // regenerate verb form with original postag and new flag to add Pronoun if exists
    String suffix = tagger.getEnclitic(prepositionToken.getAnalyzedToken(0));

    String newWord = synthesizer.setEnclitic(verbToken.getAnalyzedToken(0),suffix);
    //debug only
//    System.out.println("Synthesizer:"+newWord);
    return newWord;
    // FIXME: to remove
//    String vPostag = verbToken.getReadings().get(0).getPOSTag();
//    String pPostag = prepositionToken.getReadings().get(0).getPOSTag();
//    char pflag = tagmanager.getFlag(pPostag, "PRONOUN");
//    char vflag = '-';
//    if (pflag!= '-') {vflag = 'H';}
//    String verbLemma = verbToken.getReadings().get(0).getLemma();
//    String verbStem = generateNewForm(verbLemma, vPostag, vflag);
//    String suffix = tagger.getEnclitic(prepositionToken);
//    String prefix = tagger.getProcletic(verbToken);
//    String verbWord = prefix+verbStem+suffix;
//
//    // debug
//    String newWordMethod = synthesizer.setEnclitic(verbToken,suffix);
//    System.out.println("Lemma:"+verbLemma+" Word:"+verbWord+" vPosTag:"+vPostag+" vflag:"+vflag+ " pflag:"+pflag +" prepo:"+prepositionToken.toString());
//    System.out.println("Synthesizer:"+newWordMethod);
//    return verbWord;
  }
}

