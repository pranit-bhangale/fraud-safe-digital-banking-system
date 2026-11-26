package com.pranit.praya.guardrail.normalizer;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.regex.Pattern;

@UtilityClass
public  class TextNormalizer {

    private static final Pattern INVISIBLE =
            Pattern.compile("[\\u200B-\\u200F\\u202A-\\u202E\\u2060\\uFEFF\\u00AD]");
    private static final Pattern WHITESPACE =
            Pattern.compile("\\s+");


    public static String normalize(String text) {
        if (text == null || text.isEmpty()) return "";
        String n = Normalizer.normalize(text, Normalizer.Form.NFKC);
        n = INVISIBLE.matcher(n).replaceAll("");
        return WHITESPACE.matcher(n).replaceAll(" ").trim();
    }
}
