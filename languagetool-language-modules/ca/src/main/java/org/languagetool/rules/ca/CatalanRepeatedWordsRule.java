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
package org.languagetool.rules.ca;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.languagetool.Language;
import org.languagetool.rules.AbstractRepeatedWordsRule;

public class CatalanRepeatedWordsRule extends AbstractRepeatedWordsRule{

  public CatalanRepeatedWordsRule(ResourceBundle messages, Language language) {
    super(messages, language);
    //super.setDefaultTempOff();
  }
  
  private static final Map<String, List<String>> wordsToCheck = loadWords("/ca/synonyms.txt");
  
  @Override
  protected String getMessage() {
    return "Ja heu usat aquesta paraula abans. Podeu usar un sinònim per a fer més interesant el text, llevat que la repetició sigui intencionada.";
  }

  @Override
  public String getId() {
    return "CA_REPEATED_WORDS";
  }

  @Override
  public String getDescription() {
    return ("Sinònims per a paraules repetides.");
  }

  @Override
  protected Map<String, List<String>> getWordsToCheck() {
    return wordsToCheck;
  }

  @Override
  protected String getShortMessage() {
    return "Paraula repetida";
  }

}
