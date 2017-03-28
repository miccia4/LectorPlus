package it.uniroma3.entitydetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.uniroma3.configuration.Lector;
import it.uniroma3.model.WikiArticle;
import it.uniroma3.util.Pair;
import it.uniroma3.util.StanfordExpertNLP;

public class EntityDetector {
    
    /**
     * To match a name it has to be in a sentence sourrounded by two boarders (\\b) that are
     * not square bracket, _ or pipe | (which are terms that are inside a wikilink).
     * 
     * https://regex101.com/r/qdZyYl/4
     * 
     * @param name
     * @return
     */
    private String createRegexName(String name){
	return "(\\s[^\\sA-Z]++\\s|(?:^|\\. |\\n)(?:\\w++\\s)?)(" + Pattern.quote(name) + ")(?!\\s[A-Z][a-z]++|-|<)";
    }

    /**
     * To match a name it has to be in a sentence surrounded by two boarders (\\b) that are
     * not square bracket, _ or pipe | (which are terms that are inside a wikilink).
     * @param name
     * @return
     */
    private String createRegexSeed(String name){
	return "((?<![A-Z-]<)\\b)(" + Pattern.quote(name) + ")\\b(?![^<]*?>)";
    }


    /**
     * 
     * @param article
     * @return
     */
    private List<Pair<String, String>> getPronounRegex(WikiArticle article){
	List<Pair<String, String>> regexes = new ArrayList<Pair<String, String>>();
	String pronoun = article.getPronoun();
	if(pronoun != null){
	    if (!pronoun.equals("It"))
		regexes.add(Pair.make("((?<=\\, )(?<![A-Z-]<)\\b)(" + Pattern.quote(pronoun.toLowerCase()) + ")\\b(?![^<]*?>)", "PE-PRON<" + article.getWikid() + ">"));
	    regexes.add(Pair.make("((?<=\\. |\\n|^)(?<![A-Z-]<)\\b)(" + Pattern.quote(pronoun) + ")\\b(?![^<]*?>)", "PE-PRON<" + article.getWikid() + ">"));
	}
	return regexes;
    }

    /**
     * 
     * @param article
     * @return
     */
    private List<Pair<String, String>> getSeedRegex(WikiArticle article){
	List<Pair<String, String>> regexes = new ArrayList<Pair<String, String>>();
	for(String seed : article.getSeeds()){
	    if (seed != null){
		regexes.add(Pair.make(createRegexSeed("the " + seed.toLowerCase()), "PE-SEED<" + article.getWikid() + ">"));
		regexes.add(Pair.make(createRegexSeed("The " + seed.toLowerCase()), "PE-SEED<" + article.getWikid() + ">"));
	    }
	}
	return regexes;
    }

    /**
     * 
     * @param article
     * @return
     */
    private List<Pair<String, String>> getDisambiguationRegex(WikiArticle article){
	List<Pair<String, String>> regexes = new ArrayList<Pair<String, String>>();
	String disamb = article.getDisambiguation();
	if (disamb != null){
	    regexes.add(Pair.make(createRegexSeed("the " + disamb.toLowerCase()), "PE-DISAMB<" + article.getWikid() + ">"));
	    regexes.add(Pair.make(createRegexSeed("The " + disamb.toLowerCase()), "PE-DISAMB<" + article.getWikid() + ">"));
	}
	return regexes;
    }

    /**
     * 
     * @param article
     * @return
     */
    private List<Pair<String, String>> getNameRegex(WikiArticle article){
	List<Pair<String, String>> regexes = new ArrayList<Pair<String, String>>();
	regexes.add(Pair.make(createRegexName(article.getTitle()), "PE-TITLE<" + article.getWikid() + ">"));
	for(String alias : article.getAliases())
	    regexes.add(Pair.make(createRegexName(alias), "PE-ALIAS<" + article.getWikid() + ">"));
	if (article.getSubName() != null)
	    regexes.add(Pair.make(createRegexName(article.getSubName()), "PE-SUBTITLE<" + article.getWikid() + ">"));
	return regexes;
    }


    /**
     * 
     * @param article
     * @return
     */
    private Set<Pair<String, String>> getSecondaryEntitiesRegex(WikiArticle article){
	Set<Pair<String, String>> regexes2secentity = new HashSet<Pair<String, String>>();
	for(Map.Entry<String, Set<String>> sec_ent : article.getWikilinks().entrySet()){
	    for (String possibleName : sec_ent.getValue()){
		Pair<String, String> p = Pair.make(createRegexName(sec_ent.getKey()), possibleName);
		regexes2secentity.add(p);
	    }
	}
	return regexes2secentity;
    }
    
    /**
     * 
     * @param article
     * @param sentence
     * @param replacement
     * @param pattern
     * @return
     * @throws Exception
     */
    private static String applyRegex(WikiArticle article, String sentence, String replacement, String pattern) throws Exception{
	StringBuffer tmp = new StringBuffer();
	try{ 
	    Pattern p = Pattern.compile(pattern);
	    Matcher m = p.matcher(sentence);
	    while (m.find()){
		m.appendReplacement(tmp, Matcher.quoteReplacement(m.group(1)) + Matcher.quoteReplacement(replacement));
	    }
	    m.appendTail(tmp);
	}catch(Exception e){
	    e.printStackTrace();
	    throw new Exception();
	}
	return tmp.toString();
    }


    /**
     * 
     * 
     * @param article
     * @return
     */
    public WikiArticle augmentEvidence(WikiArticle article){
	/*
	 * Collect all the patterns for Primary Entity (PE)
	 */
	List<Pair<String, String>> regex2entity = new ArrayList<Pair<String, String>>();
	Set<Pair<String, String>> primaryEntityNames = new HashSet<Pair<String, String>>();
	primaryEntityNames.addAll(getNameRegex(article));
	primaryEntityNames.addAll(getPronounRegex(article));
	primaryEntityNames.addAll(getSeedRegex(article));
	primaryEntityNames.addAll(getDisambiguationRegex(article));
	regex2entity.addAll(primaryEntityNames);

	/*
	 * Adds a Secondary Entity (SE) only if it does not have a conflict of name with the primary entity! 
	 */
	for (Pair<String, String> secondaryEntity : getSecondaryEntitiesRegex(article)){
	    boolean createsConflict = false;
	    for (Pair<String, String> possiblePrimaryEntityName : primaryEntityNames){
		if (secondaryEntity.key.equals(possiblePrimaryEntityName.key)){
		    createsConflict = true;
		    break;
		}
	    }
	    if(!createsConflict){
		regex2entity.add(secondaryEntity);
	    }
	}

	/*
	 * Sort them.
	 */
	Collections.sort(regex2entity, new PatternComparator()); 

	/*
	 * Run everything!
	 */
	for(Map.Entry<String, String> block : article.getBlocks().entrySet()){
	    for(Pair<String, String> regex : regex2entity){
		try{

		    //System.out.println(regex.key + "\t" + regex.value);
		    article.getBlocks().put(block.getKey(), applyRegex(article, block.getValue(), regex.value, regex.key));

		}catch(Exception e){
		    System.out.println("Exception in:	" + article.getWikid());
		    System.out.println("Sentence:	" + block.getValue());
		    System.out.println("occurred for entity:	" + regex.value);
		    System.out.println("using the regex:	" + regex.key);
		    System.out.println("--------------------------------------------------");
		    break;
		}
	    }
	    article.getSentences().put(block.getKey(), Lector.getNLPExpert().processBlock(block.getValue()));

	}
	return article;

    }
}