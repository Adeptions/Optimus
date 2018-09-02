package com.adpetions.optimus.entities;

import com.adpetions.optimus.exceptions.TransformException;

import java.util.HashMap;
import java.util.Map;

public class HtmlEntityReferenceResolver implements EntityReferenceResolver {
    private static final String[] RAW_HTML_ENTITY_REFERENCES = new String[]{
            "quot", "\"", //  (34)    quotation mark (APL quote)
            "amp", "\u0026", // (38)    ampersand
            "apos", "\u0027", // (39)    apostrophe (apostrophe-quote); see below
            "lt", "\u003C", // (60)    less-than sign
            "gt", "\u003E", // (62)    greater-than sign
            "nbsp", "\u00A0", // (160)    no-break space (non-breaking space)[d]
            "iexcl", "\u00A1", // (161)    inverted exclamation mark
            "cent", "\u00A2", // (162)    cent sign
            "pound", "\u00A3", // (163)    pound sign
            "curren", "\u00A4", // (164)    currency sign
            "yen", "\u00A5", // (165)    yen sign (yuan sign)
            "brvbar", "\u00A6", // (166)    broken bar (broken vertical bar)
            "sect", "\u00A7", // (167)    section sign
            "uml", "\u00A8", // (168)    diaeresis (spacing diaeresis); see Germanic umlaut
            "copy", "\u00A9", // (169)    copyright symbol
            "ordf", "\u00AA", // (170)    feminine ordinal indicator
            "laquo", "\u00AB", // (171)    left-pointing double angle quotation mark (left pointing guillemet)
            "not", "\u00AC", // (172)    not sign
            "shy", "\u00AD", // (173)    soft hyphen (discretionary hyphen)
            "reg", "\u00AE", // (174)    registered sign (registered trademark symbol)
            "macr", "\u00AF", // (175)    macron (spacing macron, overline, APL overbar)
            "deg", "\u00B0", // (176)    degree symbol
            "plusmn", "\u00B1", // (177)    plus-minus sign (plus-or-minus sign)
            "sup2", "\u00B2", // (178)    superscript two (superscript digit two, squared)
            "sup3", "\u00B3", // (179)    superscript three (superscript digit three, cubed)
            "acute", "\u00B4", // (180)    acute accent (spacing acute)
            "micro", "\u00B5", // (181)    micro sign
            "para", "\u00B6", // (182)    pilcrow sign (paragraph sign)
            "middot", "\u00B7", // (183)    middle dot (Georgian comma, Greek middle dot)
            "cedil", "\u00B8", // (184)    cedilla (spacing cedilla)
            "sup1", "\u00B9", // (185)    superscript one (superscript digit one)
            "ordm", "\u00BA", // (186)    masculine ordinal indicator
            "raquo", "\u00BB", // (187)    right-pointing double angle quotation mark (right pointing guillemet)
            "frac14", "\u00BC", // (188)    vulgar fraction one quarter (fraction one quarter)
            "frac12", "\u00BD", // (189)    vulgar fraction one half (fraction one half)
            "frac34", "\u00BE", // (190)    vulgar fraction three quarters (fraction three quarters)
            "iquest", "\u00BF", // (191)    inverted question mark (turned question mark)
            "Agrave", "\u00C0", // (192)    Latin capital letter A with grave accent (Latin capital letter A grave)
            "Aacute", "\u00C1", // (193)    Latin capital letter A with acute accent
            "Acirc", "\u00C2", // (194)    Latin capital letter A with circumflex
            "Atilde", "\u00C3", // (195)    Latin capital letter A with tilde
            "Auml", "\u00C4", // (196)    Latin capital letter A with diaeresis
            "Aring", "\u00C5", // (197)    Latin capital letter A with ring above (Latin capital letter A ring)
            "AElig", "\u00C6", // (198)    Latin capital letter AE (Latin capital ligature AE)
            "Ccedil", "\u00C7", // (199)    Latin capital letter C with cedilla
            "Egrave", "\u00C8", // (200)    Latin capital letter E with grave accent
            "Eacute", "\u00C9", // (201)    Latin capital letter E with acute accent
            "Ecirc", "\u00CA", // (202)    Latin capital letter E with circumflex
            "Euml", "\u00CB", // (203)    Latin capital letter E with diaeresis
            "Igrave", "\u00CC", // (204)    Latin capital letter I with grave accent
            "Iacute", "\u00CD", // (205)    Latin capital letter I with acute accent
            "Icirc", "\u00CE", // (206)    Latin capital letter I with circumflex
            "Iuml", "\u00CF", // (207)    Latin capital letter I with diaeresis
            "ETH", "\u00D0", // (208)    Latin capital letter Eth
            "Ntilde", "\u00D1", // (209)    Latin capital letter N with tilde
            "Ograve", "\u00D2", // (210)    Latin capital letter O with grave accent
            "Oacute", "\u00D3", // (211)    Latin capital letter O with acute accent
            "Ocirc", "\u00D4", // (212)    Latin capital letter O with circumflex
            "Otilde", "\u00D5", // (213)    Latin capital letter O with tilde
            "Ouml", "\u00D6", // (214)    Latin capital letter O with diaeresis
            "times", "\u00D7", // (215)    multiplication sign
            "Oslash", "\u00D8", // (216)    Latin capital letter O with stroke (Latin capital letter O slash)
            "Ugrave", "\u00D9", // (217)    Latin capital letter U with grave accent
            "Uacute", "\u00DA", // (218)    Latin capital letter U with acute accent
            "Ucirc", "\u00DB", // (219)    Latin capital letter U with circumflex
            "Uuml", "\u00DC", // (220)    Latin capital letter U with diaeresis
            "Yacute", "\u00DD", // (221)    Latin capital letter Y with acute accent
            "THORN", "\u00DE", // (222)    Latin capital letter THORN
            "szlig", "\u00DF", // (223)    Latin small letter sharp s (ess-zed); see German Eszett
            "agrave", "\u00E0", // (224)    Latin small letter a with grave accent
            "aacute", "\u00E1", // (225)    Latin small letter a with acute accent
            "acirc", "\u00E2", // (226)    Latin small letter a with circumflex
            "atilde", "\u00E3", // (227)    Latin small letter a with tilde
            "auml", "\u00E4", // (228)    Latin small letter a with diaeresis
            "aring", "\u00E5", // (229)    Latin small letter a with ring above
            "aelig", "\u00E6", // (230)    Latin small letter ae (Latin small ligature ae)
            "ccedil", "\u00E7", // (231)    Latin small letter c with cedilla
            "egrave", "\u00E8", // (232)    Latin small letter e with grave accent
            "eacute", "\u00E9", // (233)    Latin small letter e with acute accent
            "ecirc", "\u00EA", // (234)    Latin small letter e with circumflex
            "euml", "\u00EB", // (235)    Latin small letter e with diaeresis
            "igrave", "\u00EC", // (236)    Latin small letter i with grave accent
            "iacute", "\u00ED", // (237)    Latin small letter i with acute accent
            "icirc", "\u00EE", // (238)    Latin small letter i with circumflex
            "iuml", "\u00EF", // (239)    Latin small letter i with diaeresis
            "eth", "\u00F0", // (240)    Latin small letter eth
            "ntilde", "\u00F1", // (241)    Latin small letter n with tilde
            "ograve", "\u00F2", // (242)    Latin small letter o with grave accent
            "oacute", "\u00F3", // (243)    Latin small letter o with acute accent
            "ocirc", "\u00F4", // (244)    Latin small letter o with circumflex
            "otilde", "\u00F5", // (245)    Latin small letter o with tilde
            "ouml", "\u00F6", // (246)    Latin small letter o with diaeresis
            "divide", "\u00F7", // (247)    division sign (obelus)
            "oslash", "\u00F8", // (248)    Latin small letter o with stroke (Latin small letter o slash)
            "ugrave", "\u00F9", // (249)    Latin small letter u with grave accent
            "uacute", "\u00FA", // (250)    Latin small letter u with acute accent
            "ucirc", "\u00FB", // (251)    Latin small letter u with circumflex
            "uuml", "\u00FC", // (252)    Latin small letter u with diaeresis
            "yacute", "\u00FD", // (253)    Latin small letter y with acute accent
            "thorn", "\u00FE", // (254)    Latin small letter thorn
            "yuml", "\u00FF", // (255)    Latin small letter y with diaeresis
            "OElig", "\u0152", // (338)    Latin capital ligature oe[e]
            "oelig", "\u0153", // (339)    Latin small ligature oe[e]
            "Scaron", "\u0160", // (352)    Latin capital letter s with caron
            "scaron", "\u0161", // (353)    Latin small letter s with caron
            "Yuml", "\u0178", // (376)    Latin capital letter y with diaeresis
            "fnof", "\u0192", // (402)    Latin small letter f with hook (function, florin)
            "circ", "\u02C6", // (710)    modifier letter circumflex accent
            "tilde", "\u02DC", // (732)    small tilde
            "Alpha", "\u0391", // (913)    Greek capital letter Alpha
            "Beta", "\u0392", // (914)    Greek capital letter Beta
            "Gamma", "\u0393", // (915)    Greek capital letter Gamma
            "Delta", "\u0394", // (916)    Greek capital letter Delta
            "Epsilon", "\u0395", // (917)    Greek capital letter Epsilon
            "Zeta", "\u0396", // (918)    Greek capital letter Zeta
            "Eta", "\u0397", // (919)    Greek capital letter Eta
            "Theta", "\u0398", // (920)    Greek capital letter Theta
            "Iota", "\u0399", // (921)    Greek capital letter Iota
            "Kappa", "\u039A", // (922)    Greek capital letter Kappa
            "Lambda", "\u039B", // (923)    Greek capital letter Lambda
            "Mu", "\u039C", // (924)    Greek capital letter Mu
            "Nu", "\u039D", // (925)    Greek capital letter Nu
            "Xi", "\u039E", // (926)    Greek capital letter Xi
            "Omicron", "\u039F", // (927)    Greek capital letter Omicron
            "Pi", "\u03A0", // (928)    Greek capital letter Pi
            "Rho", "\u03A1", // (929)    Greek capital letter Rho
            "Sigma", "\u03A3", // (931)    Greek capital letter Sigma
            "Tau", "\u03A4", // (932)    Greek capital letter Tau
            "Upsilon", "\u03A5", // (933)    Greek capital letter Upsilon
            "Phi", "\u03A6", // (934)    Greek capital letter Phi
            "Chi", "\u03A7", // (935)    Greek capital letter Chi
            "Psi", "\u03A8", // (936)    Greek capital letter Psi
            "Omega", "\u03A9", // (937)    Greek capital letter Omega
            "alpha", "\u03B1", // (945)    Greek small letter alpha
            "beta", "\u03B2", // (946)    Greek small letter beta
            "gamma", "\u03B3", // (947)    Greek small letter gamma
            "delta", "\u03B4", // (948)    Greek small letter delta
            "epsilon", "\u03B5", // (949)    Greek small letter epsilon
            "zeta", "\u03B6", // (950)    Greek small letter zeta
            "eta", "\u03B7", // (951)    Greek small letter eta
            "theta", "\u03B8", // (952)    Greek small letter theta
            "iota", "\u03B9", // (953)    Greek small letter iota
            "kappa", "\u03BA", // (954)    Greek small letter kappa
            "lambda", "\u03BB", // (955)    Greek small letter lambda
            "mu", "\u03BC", // (956)    Greek small letter mu
            "nu", "\u03BD", // (957)    Greek small letter nu
            "xi", "\u03BE", // (958)    Greek small letter xi
            "omicron", "\u03BF", // (959)    Greek small letter omicron
            "pi", "\u03C0", // (960)    Greek small letter pi
            "rho", "\u03C1", // (961)    Greek small letter rho
            "sigmaf", "\u03C2", // (962)    Greek small letter final sigma
            "sigma", "\u03C3", // (963)    Greek small letter sigma
            "tau", "\u03C4", // (964)    Greek small letter tau
            "upsilon", "\u03C5", // (965)    Greek small letter upsilon
            "phi", "\u03C6", // (966)    Greek small letter phi
            "chi", "\u03C7", // (967)    Greek small letter chi
            "psi", "\u03C8", // (968)    Greek small letter psi
            "omega", "\u03C9", // (969)    Greek small letter omega
            "thetasym", "\u03D1", // (977)    Greek theta symbol
            "upsih", "\u03D2", // (978)    Greek Upsilon with hook symbol
            "piv", "\u03D6", // (982)    Greek pi symbol
            "ensp", "\u2002", // (8194)    en space[d]
            "emsp", "\u2003", // (8195)    em space[d]
            "thinsp", "\u2009", // (8201)    thin space[d]
            "zwnj", "\u200C", // (8204)    zero-width non-joiner
            "zwj", "\u200D", // (8205)    zero-width joiner
            "lrm", "\u200E", // (8206)    left-to-right mark
            "rlm", "\u200F", // (8207)    right-to-left mark
            "ndash", "\u2013", // (8211)    en dash
            "mdash", "\u2014", // (8212)    em dash
            "lsquo", "\u2018", // (8216)    left single quotation mark
            "rsquo", "\u2019", // (8217)    right single quotation mark
            "sbquo", "\u201A", // (8218)    single low-9 quotation mark
            "ldquo", "\u201C", // (8220)    left double quotation mark
            "rdquo", "\u201D", // (8221)    right double quotation mark
            "bdquo", "\u201E", // (8222)    double low-9 quotation mark
            "dagger", "\u2020", // (8224)    dagger, obelisk
            "Dagger", "\u2021", // (8225)    double dagger, double obelisk
            "bull", "\u2022", // (8226)    bullet (black small circle)[f]
            "hellip", "\u2026", // (8230)    horizontal ellipsis (three dot leader)
            "permil", "\u2030", // (8240)    per mille sign
            "prime", "\u2032", // (8242)    prime (minutes, feet)
            "Prime", "\u2033", // (8243)    double prime (seconds, inches)
            "lsaquo", "\u2039", // (8249)    ISO proposed    single left-pointing angle quotation mark[g]
            "rsaquo", "\u203A", // (8250)    ISO proposed    single right-pointing angle quotation mark[g]
            "oline", "\u203E", // (8254)    overline (spacing overscore)
            "frasl", "\u2044", // (8260)    fraction slash (solidus)
            "euro", "\u20AC", // (8364)    euro sign
            "image", "\u2111", // (8465)    black-letter capital I (imaginary part)
            "weierp", "\u2118", // (8472)    script capital P (power set, Weierstrass p)
            "real", "\u211C", // (8476)    black-letter capital R (real part symbol)
            "trade", "\u2122", // (8482)    trademark symbol
            "alefsym", "\u2135", // (8501)    alef symbol (first transfinite cardinal)[h]
            "larr", "\u2190", // (8592)    leftwards arrow
            "uarr", "\u2191", // (8593)    upwards arrow
            "rarr", "\u2192", // (8594)    rightwards arrow
            "darr", "\u2193", // (8595)    downwards arrow
            "harr", "\u2194", // (8596)    left right arrow
            "crarr", "\u21B5", // (8629)    downwards arrow with corner leftwards (carriage return)
            "lArr", "\u21D0", // (8656)    leftwards double arrow[i]
            "uArr", "\u21D1", // (8657)    upwards double arrow
            "rArr", "\u21D2", // (8658)    rightwards double arrow[j]
            "dArr", "\u21D3", // (8659)    downwards double arrow
            "hArr", "\u21D4", // (8660)    left right double arrow
            "forall", "\u2200", // (8704)    for all
            "part", "\u2202", // (8706)    partial differential
            "exist", "\u2203", // (8707)    there exists
            "empty", "\u2205", // (8709)    ISOamso    empty set (null set); see also U+8960, ⌀
            "nabla", "\u2207", // (8711)    del or nabla (vector differential operator)
            "isin", "\u2208", // (8712)    element of
            "notin", "\u2209", // (8713)    not an element of
            "ni", "\u220B", // (8715)    contains as member
            "prod", "\u220F", // (8719)    n-ary product (product sign)[k]
            "sum", "\u2211", // (8721)    n-ary summation[l]
            "minus", "\u2212", // (8722)    minus sign
            "lowast", "\u2217", // (8727)    asterisk operator
            "radic", "\u221A", // (8730)    square root (radical sign)
            "prop", "\u221D", // (8733)    proportional to
            "infin", "\u221E", // (8734)    infinity
            "ang", "\u2220", // (8736)    angle
            "and", "\u2227", // (8743)    logical and (wedge)
            "or", "\u2228", // (8744)    logical or (vee)
            "cap", "\u2229", // (8745)    intersection (cap)
            "cup", "\u222A", // (8746)    union (cup)
            "int", "\u222B", // (8747)    integral
            "there4", "\u2234", // (8756)    therefore sign
            "sim", "\u223C", // (8764)    tilde operator (varies with, similar to)[m]
            "cong", "\u2245", // (8773)    congruent to
            "asymp", "\u2248", // (8776)    almost equal to (asymptotic to)
            "ne", "\u2260", // (8800)    not equal to
            "equiv", "\u2261", // (8801)    identical to; sometimes used for 'equivalent to'
            "le", "\u2264", // (8804)    less-than or equal to
            "ge", "\u2265", // (8805)    greater-than or equal to
            "sub", "\u2282", // (8834)    subset of
            "sup", "\u2283", // (8835)    superset of[n]
            "nsub", "\u2284", // (8836)    not a subset of
            "sube", "\u2286", // (8838)    subset of or equal to
            "supe", "\u2287", // (8839)    superset of or equal to
            "oplus", "\u2295", // (8853)    circled plus (direct sum)
            "otimes", "\u2297", // (8855)    circled times (vector product)
            "perp", "\u22A5", // (8869)    up tack (orthogonal to, perpendicular)[o]
            "sdot", "\u22C5", // (8901)    dot operator[p]
            "vellip", "\u22EE", // (8942)    vertical ellipsis
            "lceil", "\u2308", // (8968)    left ceiling (APL upstile)
            "rceil", "\u2309", // (8969)    right ceiling
            "lfloor", "\u230A", // (8970)    left floor (APL downstile)
            "rfloor", "\u230B", // (8971)    right floor
            "lang", "\u2329", // (9001)    left-pointing angle bracket (bra)[q]
            "rang", "\u232A", // (9002)    right-pointing angle bracket (ket)[r]
            "loz", "\u25CA", // (9674)    lozenge
            "spades", "\u2660", // (9824)    black spade suit[f]
            "clubs", "\u2663", // (9827)    black club suit (shamrock)[f]
            "hearts", "\u2665", // (9829)    black heart suit (valentine)[f]
            "diams", "\u2666" // (9830)    black diamond suit[f]
    };
    private static final Map<String,String> htmlEntityReferencesLookup = new HashMap<>();

    static {
        for (int i = 0, max = RAW_HTML_ENTITY_REFERENCES.length; i < max; i += 2) {
            htmlEntityReferencesLookup.put(RAW_HTML_ENTITY_REFERENCES[i], RAW_HTML_ENTITY_REFERENCES[i + 1]);
        }
    }

    @Override
    public String resolveEntityReference(String entityReference) throws TransformException {
        return htmlEntityReferencesLookup.get(entityReference);
    }
}
