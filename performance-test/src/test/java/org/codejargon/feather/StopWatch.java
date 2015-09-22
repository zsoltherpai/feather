package org.codejargon.feather;

public class StopWatch {
    public static void millis(String description, Runnable runnable) {
        long start = System.currentTimeMillis();
        runnable.run();
        long end = System.currentTimeMillis();
        System.out.println(String.format("%s %s milliseconds.", description, end -start));
    }
}
