package com.github.pfumagalli.demavenizer.parser;

import java.net.URI;

public class ParseException extends RuntimeException {

    public ParseException(URI uri, Throwable cause) {
        super("Exception parsing " + uri.toASCIIString(), cause);
    }

}
