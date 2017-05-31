package it.uniroma3.extractor.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.uniroma3.extractor.bean.WikiArticle;
import it.uniroma3.extractor.bean.WikiLanguage;
import it.uniroma3.extractor.bean.WikiArticle.ArticleType;
/**
 * This parser is able to assign a specific type to 
 * the article using its wikid or original wiki-markup.
 * 
 * @author matteo
 *
 */
public class ArticleTyper {

    private WikiLanguage lang;

    /**
     * 
     * @param lang
     */
    public ArticleTyper(WikiLanguage lang){
	this.lang = lang;
    }

    /**
     * Check if the article can be a REDIRECT using the whole text.
     * if it contains the reference #REDIRECT.
     * 
     * @param blocks
     * @return
     */
    private boolean checkIsRedirect(WikiArticle article){
	for(String red : lang.getRedirectIdentifiers()){
	    Pattern REDIRECT = Pattern.compile("#" + red, Pattern.CASE_INSENSITIVE);
	    Matcher m = REDIRECT.matcher(article.getOriginalMarkup());
	    if (m.find())
		return true;
	}
	return false;

    }

    /**
     * Check if the article can be a DATE ARTICLE using the templates in the abstract section.
     * 
     * @param blocks
     * @return
     */
    private boolean checkIsDate(WikiArticle article){
	for (String da_id : lang.getDayArticleIdentifiers()){
	    if (article.getOriginalMarkup().contains(da_id))
		return true;
	}
	return false;
    }

    /**
     * Check if the article can be a DISAMBIGUAITON using the text in the abstract 
     * section or a template in the whole article.
     * 
     * @param blocks
     * @param article
     * @return
     */
    private boolean checkIsDisambiguation(WikiArticle article){
	for (String p : lang.getDisambiguationIdentifiers()){
	    Pattern DIS = Pattern.compile("\\{\\{" + "[^\\(\\[]*?" + "\\b(" + p + ")\\b" + "([^\\)\\]]*?)" + "\\}\\}", 
		    Pattern.CASE_INSENSITIVE);
	    Matcher m = DIS.matcher(article.getOriginalMarkup());
	    while (m.find()){
		if (!m.group(2).contains("needed")){
		    return true;
		}
	    }
	}
	return false;
    }

    /**
     * Find the ArticleType using the wikid.
     * 
     * Generally particular types of articles have particular wikids 
     * such as "Category:Italian footballer" or "Discussion:something else".
     *  
     * @param article
     * @return
     */
    public ArticleType findArticleType(WikiArticle article){
	/* Filter to capture REDIRECT articles */
	if (checkIsRedirect(article))
	    return ArticleType.REDIRECT;

	/* Filter to capture DISAMBIGUATION articles */
	if (checkIsDisambiguation(article))
	    return ArticleType.DISAMBIGUATION;

	/* Filter to capture DATE articles */
	if (checkIsDate(article))
	    return ArticleType.DATE;

	/* Filter to capture WIKIPEDIA portal articles */
	for (String portalHook : lang.getPortalIdentifiers())
	    if (article.getWikid().startsWith(portalHook + ":")){
		return ArticleType.PORTAL;
	    }

	/* Filter to capture FILE articles */
	for (String fileHook : lang.getFileIdentifiers())
	    if (article.getWikid().startsWith(fileHook + ":")){
		return ArticleType.FILE;
	    }

	/* Filter to capture HELP articles */
	for (String helpHook : lang.getHelpIdentifiers())
	    if (article.getWikid().startsWith(helpHook + ":")){
		return ArticleType.HELP;
	    }

	/* Filter to capture CATEGORY articles */
	for (String categoryHook : lang.getCategoryIdentifiers())
	    if (article.getWikid().startsWith(categoryHook + ":")){
		return ArticleType.CATEGORY;
	    }

	/* Filter to capture TEMPLATE articles */
	for (String templateHook : lang.getTemplateIdentifiers())
	    if (article.getWikid().startsWith(templateHook + ":")){
		return ArticleType.TEMPLATE;
	    }

	/* Filter to capture DISCUSSION articles */
	for (String discussionHook : lang.getDiscussionIdentifiers())
	    if (article.getWikid().startsWith(discussionHook + ":")){
		return ArticleType.DISCUSSION;
	    }

	/* Filter to capture LIST articles */
	for (String listHook : lang.getListIdentifiers())
	    if (article.getWikid().startsWith(listHook)){
		return ArticleType.LIST;
	    }

	return ArticleType.ARTICLE;
    }

}