package it.uniroma3.extractor.triples;

import it.uniroma3.extractor.bean.Lector;
import it.uniroma3.extractor.bean.WikiLanguage.Lang;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author matteo
 */
public class PlaceholderFilterGerman extends PlaceholderFilter {

    private static Set<String> nationalities;

    private final List<Pattern> positions = Arrays.asList(
        Pattern.compile("\\b("
            + "süd(en)?(-)?westen|"
            + "süd(en)?(-)?osten|"
            + "nord(en)?(-)?osten|"
            + "nord(en)?(-)?westen|"
            + "süd(en)?(-)?zentral|"
            + "nord(en)?(-)?zentral|"
            + "westen(-)?zentral|"
            + "süd(en)?(-)?zentral|"
            + "zentral(-)?nord(en)?|"
            + "zentral(-)?süd(en)?|"
            + "zentral(-)?osten?|"
            + "zentral(-)?westen"
            + ")\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b("
            + "norden|"
            + "süden|"
            + "westen|"
            + "osten"
            + ")\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b("
            + "nord|"
            + "süd|"
            + "westen|"
            + "osten)\\b", Pattern.CASE_INSENSITIVE)
    ); //Arrays.asList mantiene l'ordine dall'alto verso il basso
    private final List<Pattern> lenghts = Arrays.asList(
        Pattern.compile("#YEAR#\\s?(km|kilometre(s)?|mi|ft|yd|m)(s?)\\b"),
        Pattern.compile("\\d+(\\s\\d+)*\\s?(km|kilometer(s)?|mi|ft|yd|m)(s?)\\b")
        );
    private final List<Pattern> ordinals = Arrays.asList(
        Pattern.compile("\\b\\d*1st\\b"),
        Pattern.compile("\\b\\d*2nd\\b"),
        Pattern.compile("\\b\\d*3rd\\b"),
        Pattern.compile("\\b(\\d)*\\dth\\b"),
        Pattern.compile("\\b(erste|zweite|dritte|vierte|fünfte)\\b")
    );
    private final List<Pattern> months = Arrays.asList(
        Pattern.compile("\\b(januar|februar|märz|april|mai|juni|juli|august|"
            + "september|oktober|november|dezember)\\b", Pattern.CASE_INSENSITIVE)
    );
    private final List<Pattern> date = Arrays.asList(
        Pattern.compile("#YEAR#(\\s|,\\s)#DAY#"),
        Pattern.compile("#DAY#(\\s|,\\s)#YEAR#")
    );
    private final List<Pattern> day = Arrays.asList(
        Pattern.compile("([0-3]?[0-9]–)?[0-3]?[0-9]\\s#MONTH#"),
        Pattern.compile("#MONTH#\\s([0-3]?[0-9]–)?[0-3]?[0-9]")
    );
    private final List<Pattern> year = Arrays.asList(Pattern.compile("\\b((1|2)\\d\\d\\d)\\b"));
    private final List<Pattern> era = Arrays.asList(
        Pattern.compile("\\b#YEAR#s\\b"),
        Pattern.compile("\\b#[0-9]0s\\b")
    );

    /**
     *
     */
    public PlaceholderFilterGerman() {

        super();

    }


    protected void fetch(){

        //placeholder2patterns.put(POSITION, new ArrayList<>());


        placeholder2patterns.put(POSITION, positions);
        placeholder2patterns.put(LENGHT, lenghts);
        placeholder2patterns.put(DATE, date);
        placeholder2patterns.put(DAY, day);
        placeholder2patterns.put(MONTH, months);
        placeholder2patterns.put(YEAR, year);
        placeholder2patterns.put(ERA, era);
        placeholder2patterns.put(ORDINAL, ordinals);

    }


    /**
     * @param phrase
     * @return
     */
    private String replaceNationalities(String phrase) {
        if (Lector.getLang().equals(Lang.en) || Lector.getLang().equals(Lang.es)) {
            for (String nat : nationalities) {
                nat = nat.replaceAll("_", " ");
                Pattern NAT = Pattern.compile("\\b" + nat + "\\b", Pattern.CASE_INSENSITIVE);
                phrase = NAT.matcher(phrase).replaceAll("#NAT#");
            }
        }
        return phrase;
    }

    /**
     * Eliminate parethesis.
     *
     * @param phrase
     * @return
     */
    public String preprocess(String phrase) {
        phrase = Lector.getTextParser().removeParenthesis(phrase);
        phrase = phrase.toLowerCase();


        phrase = replace(phrase);

        phrase = replaceNationalities(phrase);


        phrase = phrase.replaceAll("''", "");
        phrase = phrase.replaceAll("\"", "");

	/* remove possible special characters at the beginning or at the end.
     * for example, we want to remove phrases that begin with ", ', -
	 * but not with 's. For this reason we check the space after the character.
	 */
        if (phrase.startsWith("' ") || phrase.startsWith("\" ") || phrase.startsWith("- ") || phrase.startsWith(": "))
            phrase = phrase.substring(1).trim();
        if (phrase.endsWith(" '") || phrase.endsWith(" \"") || phrase.endsWith(" -") || phrase.endsWith(" :"))
            phrase = phrase.substring(0, phrase.length() - 1).trim();

        Pattern pattern = Pattern.compile("([A-Za-z0-9,'´#\\.\\- ]+)");
        Matcher matcher = pattern.matcher(phrase);
        if (!matcher.matches())
            phrase = "";
        else {
            pattern = Pattern.compile("([,'´#\\.\\- ]+)");
            matcher = pattern.matcher(phrase);
            if (matcher.matches())
                phrase = "";
        }

	/*
	 * Some nationalities are cutted, e.g. [Canad]ian or [French]ese
	 */
        String[] initialNatCutted = new String[]{"n ", "ese ", "ian "};
        for (String inc : initialNatCutted) {
            if (phrase.startsWith(inc))
                phrase = phrase.substring(inc.length());
        }

        return phrase = phrase.trim();
    }
}
