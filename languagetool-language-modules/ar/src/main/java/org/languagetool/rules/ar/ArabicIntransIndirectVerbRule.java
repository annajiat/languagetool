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

public class ArabicIntransIndirectVerbRule extends AbstractSimpleReplaceRule2 {

  public static final String AR_VERB_INTRANS_INDIRECT_REPLACE = "AR_VERB_INTRANSITIVE_INDIRECT";

  private static final String FILE_NAME = "/ar/verb_intrans_to_intrans.txt";
  private static final Locale AR_LOCALE = new Locale("ar");

  private final ArabicTagger tagger;
  private final ArabicTagManager tagmanager;
  private final ArabicSynthesizer synthesizer;
  private final List<Map<String, SuggestionWithMessage>> wrongWords;

  public ArabicIntransIndirectVerbRule(ResourceBundle messages) {
    super(messages, new Arabic());
    tagger = new ArabicTagger();
    tagger.enableNewStylePronounTag();
    tagmanager = new ArabicTagManager();
    synthesizer = new ArabicSynthesizer(new Arabic());

    super.setCategory(Categories.MISC.getCategory(messages));
    setLocQualityIssueType(ITSIssueType.Inconsistency);
    //FIXME: choose another example
    addExamplePair(Example.wrong("الولد <marker>يتردد على</marker> المعهد."),
      Example.fixed("الولد <marker>يتردد إلى</marker> المعهد."));

    // get wrong words from resource file
    wrongWords = getWrongWords(false);
  }

  @Override
  public String getId() {
    return AR_VERB_INTRANS_INDIRECT_REPLACE;
  }

  @Override
  public String getDescription() {
    return "َIntransitive verbs corrected to indirect transitive";
  }

  @Override
  public final List<String> getFileNames() {
    return Collections.singletonList(FILE_NAME);
  }

  @Override
  public String getShort() {
    return "أفعال متعدية بحرف، الصواب تعديتها بحرف آخر";
  }

  @Override
  public String getMessage() {
    return "'$match' الفعل يتعدى بحرف آخرف: $suggestions";
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
      AnalyzedTokenReadings nextToken = i+1 < tokens.length ? tokens[i+1] : null;
      String prevTokenStr = prevTokenIndex > 0 ? tokens[prevTokenIndex].getToken() : null;

      if (prevTokenStr != null) {
        // test if the first token is a verb
        boolean is_candidate_verb = isCandidateVerb(prevToken, token);

        // if the string composed of two tokens is candidate,
        // get suggestion preposition
        if(is_candidate_verb) {
          // test if the preposition token is suitable for verb token (previous)
          List <String> prepositions = new ArrayList<>();
          String sug_msg = "";
          StringBuilder replacement = new StringBuilder("");
          SuggestionWithMessage prepositionsWithMessage = getSuggestedPreposition(prevToken, token);
          if(prepositionsWithMessage!=null)
          {
            prepositions = Arrays.asList(prepositionsWithMessage.getSuggestion().split("\\|"));
            sug_msg = prepositionsWithMessage.getMessage();
            sug_msg = sug_msg != null ? sug_msg : "";
          for(String prep : prepositions)
          {
            String inflectPrep = inflectSuggestedPreposition(token, prep);
            replacement.append( "<suggestion>"+prevTokenStr+" "+ inflectPrep +"</suggestion>");
          }
          }
//          String sug_msg = "Taha";

//          String replacement = "<suggestion>Taha Zerrouki</suggestion>";
          String msg =  "' الفعل " + prevTokenStr + " ' متعدِ بحرف آخر ،" + sug_msg +". فهل تقصد؟"+ replacement.toString();
          RuleMatch match = new RuleMatch(
            this, sentence, prevToken.getStartPos(), token.getEndPos(),
            prevToken.getStartPos(), token.getEndPos(), msg, "خطأ في الفعل المتعدي بحرف ");
          ruleMatches.add(match);
        }
/*
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
        if (is_candidate_verb && is_wrong_preposition) {
          // generate suggestion according to prepositions to be removed
          // generate a new form of verb according to current token
          String verb = generateAttachedVerbForm(prevToken, token);
          String  replacement = "<suggestion>" + verb + "</suggestion>";
          //FIXME: add the intermediate tokens to the suggestion
          // إذا كانت الكلمتان متباعدتان، إدراج لالجملة الوسيطة في الاقتراحات
          String msg =  "' الفعل " + prevTokenStr + " ' متعدٍ بنفسه،" + sug_msg +". فهل تقصد؟"+ replacement.toString();
          RuleMatch match = new RuleMatch(
            this, sentence, prevToken.getStartPos(), token.getEndPos(),
            prevToken.getStartPos(), token.getEndPos(), msg, "خطأ في الفعل المتعدي ");
          ruleMatches.add(match);
        }
        */
      }

      if (nextToken!=null && isCandidateVerb(token, nextToken)) {
        prevTokenIndex = i;
      } else {
        prevTokenIndex = 0;
      }
    }
    return toRuleMatchArray(ruleMatches);
  }

  /* lookup for candidat verbs with preposition */
  private boolean isCandidateVerb(AnalyzedTokenReadings mytoken, AnalyzedTokenReadings nexttoken) {
  return (getSuggestedPreposition(mytoken, nexttoken)!=null);
  }

  /* lookup for candidat verbs with preposition */
  private SuggestionWithMessage getSuggestedPreposition(AnalyzedTokenReadings mytoken, AnalyzedTokenReadings nexttoken) {

    List<AnalyzedToken> verbTokenList = mytoken.getReadings();
    List<AnalyzedToken> prepTokenList = nexttoken.getReadings();
    for (AnalyzedToken verbTok : verbTokenList) {
      String verbLemma = verbTok.getLemma();
      String verbPostag = verbTok.getPOSTag();

      // if postag is attached
      // test if verb is in the verb list
      if (verbPostag != null && tagmanager.isVerb(verbPostag)) {
        for (AnalyzedToken prepTok : prepTokenList) {
          String prepLemma = prepTok.getLemma();
          String prepPostag = prepTok.getPOSTag();
          //FIXME: add isBreak to tagmannager
          // problem with pos tagging system for Partical
          boolean isNotBreak = tagmanager.getFlag(prepPostag, "CONJ") != 'W';
//          char flagw = tagmanager.getFlag(prepPostag, "CONJ");
          //debug only
//          System.out.println("ArabicIntransIndirectVerbRule.java: word:"+ prepTok.getToken()+" postag:"+prepPostag + " flag"+flagw);
//          System.out.println("ArabicIntransIndirectVerbRule.java: is not Break: isNotbreak:"+ Boolean.toString(isNotBreak));
          if (prepPostag != null && tagmanager.isStopWord(prepPostag) && isNotBreak)
          {
            // the candidate string is composed of verb + preposition
            String candidateString = verbLemma + " " + prepLemma;
            // lookup in WrongWords
            SuggestionWithMessage verbLemmaMatch = wrongWords.get(wrongWords.size() - 1).get(candidateString);
          // The lemma is found in the dictionary file
          return verbLemmaMatch;
        }
      }

    }
  }
    return null;
  }



  /* generate a new form according to a specific postag, this form is Attached*/
  private String inflectSuggestedPreposition(AnalyzedTokenReadings currentPrepToken, String suggPrepLemma) {
    // extract verb postag
    // extract preposition postag
    // get pronoun flag
    // regenerate verb form with original postag and new flag to add Pronoun if exists
    String postag = "PRD;---;---";
    String suffix = tagger.getEnclitic(currentPrepToken.getAnalyzedToken(0));
    AnalyzedToken suggPrepToken= new AnalyzedToken(suggPrepLemma, postag ,suggPrepLemma);
    String newWord = synthesizer.setEnclitic(suggPrepToken,suffix);
    //debug only
//    System.out.println("Synthesizer:"+newWord);
    return newWord;
    // FIXME: to remove

  }
}

