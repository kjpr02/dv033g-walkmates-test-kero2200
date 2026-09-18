package com.walkmates.lab1;

import com.walkmates.model.Seeker;
import com.walkmates.model.TrustTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Lab 1, Part B — specification-based tests for {@link Seeker}.
 *
 * <p>Design your tests on paper first (equivalence partitions, boundary values, decision table)
 * from {@code docs/REQUIREMENTS.md} FR-1.1 / FR-1.3 / FR-1.2, then implement them here. One
 * worked example is provided; the {@code TODO}s are yours.</p>
 */
class SeekerSpecBasedTest {

    // ---- Worked example: boundary value at the maximum single top-up (FR-1.3) ----
    @Test
    @DisplayName("Top-up exactly at the 5000 SEK single-transaction maximum is accepted")
    void topUpAtSingleMaximumIsAccepted() {
        Seeker seeker = new Seeker("sam@example.com", "Sam", "0707654321");

        seeker.addFunds(Seeker.MAX_SINGLE_TOP_UP); // 5000.00, the boundary value

        assertThat(seeker.getBalance()).isEqualTo(Seeker.MAX_SINGLE_TOP_UP);
    }

    private Seeker newSeeker() {
        return new Seeker("sam@example.com", "Sam", "0701234567");
    }

    // FR-1.1: Email equivalence partitions

    @Test
    void validEmailIsAccepted() {
        Seeker seeker = newSeeker();

        assertThat(seeker.getEmail()).isEqualTo("sam@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "sam.example.com",
            "sam@@example.com",
            "@example.com",
            "sam@example"
    })
    void invalidEmailIsRejected(String email) {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker(email, "Sam", "0701234567"));
    }

    @Test
    void emailLongerThan254CharactersIsRejected() {
        String email = "a".repeat(243) + "@example.com";

        assertThrows(IllegalArgumentException.class,
                () -> new Seeker(email, "Sam", "0701234567"));
    }

    // FR-1.1: Display-name equivalence partitions

    @ParameterizedTest
    @ValueSource(strings = {"Sam", "Anna Maria", "Anne-Marie", "O'Neil"})
    void validDisplayNameIsAccepted(String name) {
        Seeker seeker = new Seeker(
                "sam@example.com", name, "0701234567");

        assertThat(seeker.getDisplayName()).isEqualTo(name);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"A", "Sam1", "Sam!"})
    void invalidDisplayNameIsRejected(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("sam@example.com", name, "0701234567"));
    }

    @Test
    void displayNameLongerThan40CharactersIsRejected() {
        String name = "A".repeat(41);

        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("sam@example.com", name, "0701234567"));
    }

    // FR-1.1: Phone-number equivalence partitions

    @ParameterizedTest
    @ValueSource(strings = {"0701234567", "+4671234567"})
    void validPhoneNumberIsAccepted(String phone) {
        Seeker seeker = new Seeker("sam@example.com", "Sam", phone);

        assertThat(seeker.getPhoneNumber()).isEqualTo(phone);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "0801234567",
            "070123456",
            "07012345678",
            "070123456A"
    })
    void invalidPhoneNumberIsRejected(String phone) {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("sam@example.com", "Sam", phone));
    }

    // FR-1.3: Wallet equivalence partitions

    @Test
    void validTopUpIncreasesExistingBalance() {
        Seeker seeker = newSeeker();
        seeker.addFunds(100.00);

        seeker.addFunds(250.00);

        assertThat(seeker.getBalance()).isEqualTo(350.00);
    }

    @ParameterizedTest
    @ValueSource(doubles = {5.00, 6000.00})
    void topUpOutsideTransactionLimitsIsRejected(double amount) {
        Seeker seeker = newSeeker();

        assertThrows(IllegalArgumentException.class,
                () -> seeker.addFunds(amount));

        assertThat(seeker.getBalance()).isEqualTo(0.00);
    }

    @Test
    void validTransactionThatExceedsBalanceLimitIsRejected() {
        Seeker seeker = seekerWith19900Balance();

        assertThrows(IllegalArgumentException.class,
                () -> seeker.addFunds(200.00));

        assertThat(seeker.getBalance()).isEqualTo(19900.00);
    }

    // FR-1.3: Minimum and maximum transaction boundaries

    @ParameterizedTest
    @ValueSource(doubles = {10.00, 10.01, 4999.99, 5000.00})
    void topUpAtValidTransactionBoundaryIsAccepted(double amount) {
        Seeker seeker = newSeeker();

        seeker.addFunds(amount);

        assertThat(seeker.getBalance()).isEqualTo(amount);
    }

    @ParameterizedTest
    @ValueSource(doubles = {9.99, 5000.01})
    void topUpOutsideTransactionBoundaryIsRejected(double amount) {
        Seeker seeker = newSeeker();

        assertThrows(IllegalArgumentException.class,
                () -> seeker.addFunds(amount));

        assertThat(seeker.getBalance()).isEqualTo(0.00);
    }

    // FR-1.3: Resulting-balance boundaries

    @ParameterizedTest
    @CsvSource({
            "99.99, 19999.99",
            "100.00, 20000.00"
    })
    void topUpWithinBalanceBoundaryIsAccepted(
            double amount, double expectedBalance) {

        Seeker seeker = seekerWith19900Balance();

        seeker.addFunds(amount);

        assertThat(seeker.getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    void topUpOneOreAboveMaximumBalanceIsRejected() {
        Seeker seeker = seekerWith19900Balance();

        assertThrows(IllegalArgumentException.class,
                () -> seeker.addFunds(100.01));

        assertThat(seeker.getBalance()).isEqualTo(19900.00);
    }

    private Seeker seekerWith19900Balance() {
        Seeker seeker = newSeeker();
        seeker.addFunds(5000.00);
        seeker.addFunds(5000.00);
        seeker.addFunds(5000.00);
        seeker.addFunds(4900.00);
        return seeker;
    }

    // FR-1.2: Trust-tier decision table

    @ParameterizedTest
    @CsvSource({
            "NEW,        1,  0.15",
            "VERIFIED,   3,  0.12",
            "TRUSTED,    5,  0.08",
            "PRO_SITTER, 10, 0.05"
    })
    void trustTierProvidesExpectedLimits(
            TrustTier tier, int expectedMaxBookings, double expectedFee) {

        Seeker seeker = newSeeker();
        seeker.setTrustTier(tier);

        assertThat(seeker.getMaxConcurrentBookings())
                .isEqualTo(expectedMaxBookings);
        assertThat(seeker.getTrustTier().getPlatformFee())
                .isEqualTo(expectedFee);
    }

    @Test
    @DisplayName("Adding 250 SEK to a new seeker gives a 250.00 balance")
    void addingFundsWorks() {
        Seeker seeker = new Seeker("you@example.com", "You", "0701234567");  // Arrange
        seeker.addFunds(250.00);                                              // Act
        assertThat(seeker.getBalance()).isEqualTo(250.00);                   // Assert
    }
}
