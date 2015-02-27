package com.github.pfumagalli.demavenizer.ivy;

public final class Normalizer {

    private Normalizer() {
        throw new IllegalStateException("Do not construct");
    }

    public static final String normalizeWhitespace(CharSequence input) {
        if (input == null) return null;
        if (input.length() == 0) return "";

        final StringBuilder builder = new StringBuilder(input.length() + 1).append(' ');
        boolean whitespace = true;
        for (int x = 0; x < input.length(); x ++) {
            final char c = input.charAt(x);
            if (Character.isWhitespace(c)) {
                if (whitespace) continue;
                builder.append(' ');
                whitespace = true;
            } else if ((c == '\u00A0') || (c == '\u2007') || (c == '\u202F')) {
                if (whitespace) continue;
                builder.append(' ');
                whitespace = true;
            } else {
                builder.append(c);
                whitespace = false;
            }
        }

        if (whitespace) {
            return builder.subSequence(1, builder.length() - 1).toString();
        } else {
            return builder.subSequence(1, builder.length()).toString();
        }
    }
}
