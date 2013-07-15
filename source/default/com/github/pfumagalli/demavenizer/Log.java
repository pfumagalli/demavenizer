package com.github.pfumagalli.demavenizer;

public final class Log {

    private Log() {
        throw new IllegalStateException();
    }

    public static void info(String message) {
        System.err.println("\u001b[32m" + message + "\u001b[0m");
    }

    public static void warn(String message) {
        System.err.println("\u001b[33m" + message + "\u001b[0m");
    }

    public static void error(String message) {
        System.err.println("\u001b[31m" + message + "\u001b[0m");
    }

    public static void error(String message, Exception exception) {
        System.err.println("\u001b[31m" + message + "\u001b[0m");
        System.err.print("\u001b[33m");
        exception.printStackTrace(System.err);
        System.err.println("\u001b[0m");
    }
}
