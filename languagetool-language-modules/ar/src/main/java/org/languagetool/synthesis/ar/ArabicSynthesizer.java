/* LanguageTool, a natural language style checker
 * Copyright (C) 2019 Sohaib Afifi, Taha Zerrouki
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
package org.languagetool.synthesis.ar;

import morfologik.stemming.IStemmer;
import morfologik.stemming.WordData;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.synthesis.BaseSynthesizer;
import org.languagetool.tagging.ar.ArabicTagManager;
import org.languagetool.tagging.ar.ArabicTagger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arabic word form synthesizer.
 * Based on part-of-speech lists in Public Domain. See readme.txt for details,
 * the POS tagset is described in arabic_tags_description.txt.
 * <p>
 * There are two special additions:
 * <ol>
 *    <li>+GF - tag that adds  feminine gender to word</li>
 *    <li>+GM - a tag that adds masculine gender to word</li>
 * </ol>
 *
 * @author Taha Zerrouki
 * @since 4.9
 */
public class ArabicSynthesizer extends BaseSynthesizer {

  private static final String RESOURCE_FILENAME = "/ar/arabic_synth.dict";
  private static final String TAGS_FILE_NAME = "/ar/arabic_tags.txt";

  private final ArabicTagManager tagmanager = new ArabicTagManager();
  private final ArabicTagger tagger = new ArabicTagger();

  public ArabicSynthesizer(Language lang) {
    super(RESOURCE_FILENAME, TAGS_FILE_NAME, lang);
  }

  /**
   * Get a form of a given AnalyzedToken, where the form is defined by a
   * part-of-speech tag.
   *
   * @param token  AnalyzedToken to be inflected.
   * @param posTag A desired part-of-speech tag.
   * @return String value - inflected word.
   */
  @Override
  public String[] synthesize(AnalyzedToken token, String posTag) {
    IStemmer synthesizer = createStemmer();
    List<WordData> wordData = synthesizer.lookup(token.getLemma() + "|" + posTag);
    List<String> wordForms = new ArrayList<>();
    String stem;
    for (WordData wd : wordData) {
      // ajust some stems
      stem = correctStem(wd.getStem().toString(), posTag);
      //debug only
//      System.out.println("ArabicSynthesizer:stem:"+stem+" "+ posTag);
      wordForms.add(stem);
    }
    return wordForms.toArray(new String[0]);
  }

  /**
   * Special English regexp based synthesizer that allows adding articles
   * when the regexp-based tag ends with a special signature {@code \\+INDT} or {@code \\+DT}.
   *
   * @since 2.5
   */
  @Override
  public String[] synthesize(AnalyzedToken token, String posTag,
                             boolean posTagRegExp) throws IOException {

    if (posTag != null && posTagRegExp) {
      String myPosTag = posTag;
      initPossibleTags();
      myPosTag = correctTag(myPosTag);

      Pattern p = Pattern.compile(myPosTag);
      List<String> results = new ArrayList<>();
      String stem;
      for (String tag : possibleTags) {
        Matcher m = p.matcher(tag);
        if (m.matches() && token.getLemma() != null) {
          // local result
          List<String> result_one = lookup(token.getLemma(), tag);
          for (String wd : result_one) {
            // adjust some stems according to original postag
            stem = correctStem(wd, posTag);
            results.add(stem);
          }
        }
      }
      return results.toArray(new String[0]);
    }

    return synthesize(token, posTag);
  }



  /* correct tags  */

  public String correctTag(String postag) {
    String mypostag = postag;
    if (postag == null) return null;
    // remove attached pronouns
    mypostag = tagmanager.setConjunction(mypostag, "-");

    // remove Alef Lam definite article
    mypostag = tagmanager.setDefinite(mypostag, "-");

    // change all pronouns to one kind
    mypostag = tagmanager.unifyPronounTag(mypostag);

    return mypostag;
  }

  @Override
  public String getPosTagCorrection(String posTag) {
    return correctTag(posTag);
  }


  /* correct stem to generate stems to be attached with pronouns  */
  public String correctStem(String stem, String postag) {
    String correct_stem = stem;
    if (postag == null) return stem;
    if (tagmanager.isAttached(postag)) {
      correct_stem = correct_stem.replaceAll("ه$", "");
    }

    if (tagmanager.isDefinite(postag)) {
      String prefix = tagmanager.getDefinitePrefix(postag);// can handle ال & لل
      correct_stem = prefix + correct_stem;
    }
    if (tagmanager.hasJar(postag)) {
      String prefix = tagmanager.getJarPrefix(postag);
      correct_stem = prefix + correct_stem;
    }
    if (tagmanager.hasConjunction(postag)) {
      String prefix = tagmanager.getConjunctionPrefix(postag);
      correct_stem = prefix + correct_stem;

    }
    return correct_stem;
  }

  /**
   * @return set a new enclitic for the given word,
   */
  public String setEnclitic(AnalyzedToken token, String suffix) {
    // if the suffix is not empty
    // save procletic
    // ajust postag to get synthesed words
    // set enclitic flag
    // synthesis => lookup for stems with similar postag and has enclitic flag
    // Add procletic and enclitic to stem
    // return new word
    String postag = token.getPOSTag();
    String word = token.getToken();
    if (postag.isEmpty())
      return word;
    /* The flag is by defaul equal to '-' , if suffix => "H" */
    char flag = (suffix.isEmpty()) ? '-':'H';
    // save procletic
    String procletic = tagger.getProcletic(token);
    // set enclitic flag
    String newposTag = tagmanager.setFlag(postag, "PRONOUN", flag);
    //adjust procletics
    newposTag = tagmanager.setProcleticFlags(newposTag);
    // synthesis => lookup for stems with similar postag and has enclitic flag
    String lemma = token.getLemma();
    AnalyzedToken newToken = new AnalyzedToken(lemma, newposTag, lemma);
    String[] newwordList = synthesize(newToken, newposTag);

    String stem = "";
    if (newwordList.length != 0) {
      stem = newwordList[0];
      //FIXME: make a general solution
      if(tagmanager.isStopWord(newposTag) && flag =='H')
        stem = stem.replaceAll("ه$", "");
//      stem = correctStem(stem, postag);
      //debug only
//      for(int k=0; k<newwordList.length; k++) {
//        System.out.println("ArabicSynthesizer:setEnclitic()" + newwordList[k] + " " + newposTag);
//      }
    }
    else // no word generated
    //FIXME: handle stopwords generation
      stem  = "("+word+")";
    //debug only
//    System.out.println("ArabicSynthesizer:setEnclitic(), lemma:" + lemma + " postag:" + newposTag);
    String newWord = procletic+stem+suffix;
    return newWord;
  }

}



