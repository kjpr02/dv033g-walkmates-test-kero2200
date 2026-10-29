package com.walkmates.lab2;

import com.walkmates.model.*;
import com.walkmates.repository.*;
import com.walkmates.service.BookingService;
import com.walkmates.service.NotificationService;
import com.walkmates.service.PricingCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private SeekerRepository seekers;
    private ListingRepository listings;
    private ProviderRepository providers;
    private BookingRepository bookings;
    private PricingCalculator pricing;
    private NotificationService notifications;

    private BookingService service;
    private Seeker seeker;
    private Provider provider;
    private Listing listing;

    @BeforeEach
    void setUp() {
        seekers = mock(SeekerRepository.class);
        listings = mock(ListingRepository.class);
        providers = mock(ProviderRepository.class);
        bookings = mock(BookingRepository.class);
        pricing = mock(PricingCalculator.class);
        notifications = mock(NotificationService.class);

        service = new BookingService(seekers, listings, providers, bookings, pricing, notifications);

        seeker = new Seeker("sam@example.com", "Sam", "0701234567");
        seeker.setTrustTier(TrustTier.VERIFIED);
        seeker.addFunds(1000.00);

        provider = new Provider("Test Provider", 62.39, 17.31, 20);
        listing = new Listing(provider.getId(), "Dog walk", "A walk", ListingType.DOG_WALK);

        when(seekers.findById(seeker.getId())).thenReturn(Optional.of(seeker));
        when(listings.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(providers.findById(provider.getId())).thenReturn(Optional.of(provider));

        when(listings.findByProviderId(provider.getId())).thenReturn(List.of(listing));
        when(bookings.findByListingId(listing.getId())).thenReturn(List.of());

        when(pricing.priceFor(any(Booking.class), same(listing), same(seeker))).thenReturn(89.60);
    }

    private Booking existingActiveBooking() {
        return new Booking(seeker.getId(), "other-listing", 60);
    }

    @Test
    void bookingAtSeekerLimitIsRejected() {
        when(bookings.findBySeekerId(seeker.getId())).thenReturn(List.of(
                        existingActiveBooking(),
                        existingActiveBooking(),
                        existingActiveBooking()));

        assertThatThrownBy(() -> service.createBooking(seeker.getId(), listing.getId(), 60))
                .isInstanceOf(BookingService.BookingRejectedException.class)
                .hasMessageContaining("Seeker booking limit reached");

        assertThat(seeker.getBalance()).isEqualTo(1000.00);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.AVAILABLE);

        verify(bookings, never()).save(any(Booking.class));
        verify(seekers, never()).save(any(Seeker.class));
        verify(listings, never()).save(any(Listing.class));
        verifyNoInteractions(notifications);
    }

    @Test
    void bookingOneBelowSeekerLimitIsAccepted() {
        when(bookings.findBySeekerId(
                seeker.getId())).thenReturn(List.of(existingActiveBooking(), existingActiveBooking()));

        Booking result = service.createBooking(seeker.getId(), listing.getId(), 60);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getPrice()).isEqualTo(89.60);
        assertThat(seeker.getBalance()).isEqualTo(910.40);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.BOOKED);

        verify(seekers).save(seeker);
        verify(listings).save(listing);
        verify(bookings).save(result);
        verify(notifications).sendBookingConfirmed(seeker, result);
    }
}
