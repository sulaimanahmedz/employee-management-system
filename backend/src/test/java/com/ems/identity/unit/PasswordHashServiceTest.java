package com.ems.identity.unit;

import com.ems.identity.service.PasswordHashService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHashServiceTest {
    private final PasswordHashService passwords = new PasswordHashService();

    @Test void hashesAreSaltedAndOnlyVerifyTheExactPassword() {
        String password = "Synthetic password 123!";
        String first = passwords.hash(password);
        String second = passwords.hash(password);
        assertNotEquals(first, second);
        assertTrue(first.startsWith("$2a$12$"));
        assertEquals(60, first.length());
        assertTrue(passwords.matches(password, first));
        assertTrue(passwords.matches(password, second));
        assertFalse(passwords.matches("incorrect password", first));
        assertFalse(passwords.matches(password + " ", first));
        assertFalse(passwords.matches(null, first));
        assertFalse(passwords.matches("x".repeat(73), first));
    }

    @Test void enforcesCharacterMinimumAndUtf8ByteMaximumWithoutTruncation() {
        assertThrows(IllegalArgumentException.class, () -> passwords.hash(null));
        assertThrows(IllegalArgumentException.class, () -> passwords.hash("x".repeat(14)));
        assertThrows(IllegalArgumentException.class, () -> passwords.hash("x".repeat(73)));
        assertThrows(IllegalArgumentException.class, () -> passwords.hash("😀".repeat(14)));
        assertThrows(IllegalArgumentException.class, () -> passwords.hash("😀".repeat(19)));
        for (String password : new String[]{"x".repeat(15), "x".repeat(72), "😀".repeat(18)}) {
            String hash = passwords.hash(password);
            assertTrue(passwords.matches(password, hash));
            assertFalse(passwords.matches(password + "suffix", hash));
        }
    }
}
