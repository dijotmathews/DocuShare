package com.dijo.usecase006;

import java.util.concurrent.atomic.AtomicInteger;

/** Unique, well-formed PANs for tests that share one database. */
final class TestPans {

    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    private TestPans() {
    }

    static String next() {
        return "TESTA" + COUNTER.incrementAndGet() + "Z";
    }
}
