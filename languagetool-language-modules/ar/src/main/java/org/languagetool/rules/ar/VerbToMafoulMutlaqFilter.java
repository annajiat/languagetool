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
import org.languagetool.rules.patterns.RuleFilter;
import org.languagetool.tagging.ar.ArabicTagger;
import org.languagetool.synthesis.ar.ArabicSynthesizer;

import java.util.*;

/**
 * Filter that maps suggestion from adverb to adjective.
 * Also see https://www.ef.com/wwen/english-resources/english-grammar/forming-adverbs-adjectives/
 * @since 4.9
 */
public class VerbToMafoulMutlaqFilter extends RuleFilter {

  private final ArabicTagger tagger = new ArabicTagger();
  private final ArabicSynthesizer synthesizer = new ArabicSynthesizer(new Arabic());
  private final Map<String,String> verb2masdar = new HashMap<String, String>() {{
    // tri letters verb:
    put("عَمِلَ", "عمل");
    put("أَعْمَلَ", "إعمال");
    put("عَمَّلَ", "تعميل");
    put("ضَرَبَ", "ضرب");
    put("أَكَلَ", "أكل");
    put("سَأَلَ", "سؤال");
    // regular ones:
    // non tri letters verb
    put("أَجَابَ", "إجابة");

    //
    put("twice", "second"); // special case
    // TODO: add more or maybe use https://github.com/simplenlg/simplenlg?
    //put("", "");
  }};


  @Nullable
  @Override
  public RuleMatch acceptRuleMatch(RuleMatch match, Map<String, String> arguments, int patternTokenPos, AnalyzedTokenReadings[] patternTokens) {
//    match.setSuggestedReplacement("Taha");
//    return match;

    String verb = arguments.get("verb");
    List<String> verbLemmas = tagger.getLemmas(patternTokens[0], "verb");
    String adj = arguments.get("adj");
    String masdar = verb2masdar.get(verb);
    // generate multiple masdar from verb lemmas list */
    List<String> masdarList = new ArrayList<>();
    for(String lemma: verbLemmas)
    {
      String msdr = verb2masdar.get(lemma);
      if (msdr!=null) {
        String inflectedMasdar = synthesizer.inflectMafoulMutlq(msdr);
        masdarList.add(inflectedMasdar);
      }
    }
    // only for debug
    System.out.println("verb: "+verb);
    System.out.println("verb Lemma: "+ verbLemmas.toString());
    System.out.println("masdar Lemma: "+ masdarList.toString());
    System.out.println("adj: "+adj);
    System.out.println("masdar: "+masdar);
    System.out.println("tokens: "+ Arrays.deepToString(patternTokens));

    String inflectedAdj = synthesizer.inflectAdjectiveTanwinNasb(adj);
    for( String  msdr: masdarList)
    {
      match.addSuggestedReplacement(verb +" "+msdr + " " + inflectedAdj);
    }
    return match;
  }

}
