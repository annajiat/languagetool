/* LanguageTool, a natural language style checker 
 * Copyright (C) 2021 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.rules.en;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.languagetool.Language;
import org.languagetool.rules.AbstractRepeatedWordsRule;

public class EnglishRepeatedWordsRule extends AbstractRepeatedWordsRule{

  public EnglishRepeatedWordsRule(ResourceBundle messages, Language language) {
    super(messages, language);
    super.setDefaultTempOff();
  }
  
  private static final Map<String, List<String>> wordsToCheck = loadWords("/en/synonyms.txt");
  
  @Override
  protected String getMessage() {
    return "You have used this word before. Using a synonym could make your text more interesting to read, unless the repetition is intentional.";
  }

  @Override
  public String getId() {
    return "EN_REPEATED_WORDS";
  }

  @Override
  public String getDescription() {
    return ("Suggest synonyms for repeated words.");
  }

  @Override
  protected Map<String, List<String>> getWordsToCheck() {
    return wordsToCheck;
  }

  @Override
  protected String getShortMessage() {
    return "Repeated word";
  }

}
