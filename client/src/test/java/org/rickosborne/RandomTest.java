package org.rickosborne;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomTest {
    @Test
    void randomWithSeed() {
        final Random rng = new Random(12345L);
        Assertions.assertEquals("5c9f20d6, 8361b331, eed8a922, eac80778, d545798e, 9a4ef89, 5393ea7f, 1fea4064, 3c4b499b, 90bc6fa", rng.ints(10).mapToObj((n) -> Integer.toHexString(n)).collect(Collectors.joining(", ")));
    }

    @Test
    void randomWithSeed100() {
        final Random rng = new Random(12345L);
        final int[] ints = new int[10];
        for (int i = 0; i < 10; i++) ints[i] = rng.nextInt(100);
        Assertions.assertEquals(
            "51, 80, 41, 28, 55, 84, 75, 2, 1, 89",
            Arrays.stream(ints).mapToObj((b) -> String.valueOf(b)).collect(Collectors.joining(", "))
        );
    }
}
