package com.walkmates.lab2;

import com.walkmates.model.Seeker;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.service.NotificationService;
import com.walkmates.service.PaymentService;
import com.walkmates.service.SeekerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SeekerServiceTest {
    private SeekerRepository seekers;
    private PaymentService payments;
    private NotificationService notifications;

    private SeekerService service;
    private Seeker seeker;

    private static final String PAYMENT_METHOD = "test-payment-method";
    private static final double TOP_UP = 250.00;

    @BeforeEach
    void setUp() {
        seekers = mock(SeekerRepository.class);
        payments = mock(PaymentService.class);
        notifications = mock(NotificationService.class);

        service = new SeekerService(seekers, payments, notifications);

        seeker = new Seeker("sam@example.com", "Sam", "0701234567");
        seeker.addFunds(100.00);

        when(seekers.findById(seeker.getId())).thenReturn(Optional.of(seeker));
    }

    @Test
    void successfulPaymentCreditsWalletAndSavesSeeker()
            throws PaymentService.PaymentException {
        when(payments.charge(seeker.getId(), PAYMENT_METHOD, TOP_UP)).thenReturn("confirmation-123");
        when(seekers.save(seeker)).thenReturn(seeker);

        Seeker result = service.topUp(seeker.getId(), PAYMENT_METHOD, TOP_UP);

        assertThat(result).isSameAs(seeker);
        assertThat(seeker.getBalance()).isEqualTo(350.00);

        InOrder order = inOrder(payments, seekers);
        order.verify(payments).charge(seeker.getId(), PAYMENT_METHOD, TOP_UP);
        order.verify(seekers).save(seeker);
    }

    @Test
    void declinedPaymentLeavesWalletUnchanged()
            throws PaymentService.PaymentException {
        PaymentService.PaymentException decline = new PaymentService.PaymentException("Payment declined");

        when(payments.charge(seeker.getId(), PAYMENT_METHOD, TOP_UP)).thenThrow(decline);

        assertThatThrownBy(() -> service.topUp(seeker.getId(), PAYMENT_METHOD, TOP_UP)).isSameAs(decline);

        assertThat(seeker.getBalance()).isEqualTo(100.00);

        verify(payments).charge(seeker.getId(), PAYMENT_METHOD, TOP_UP);
        verify(seekers, never()).save(any(Seeker.class));
    }

    @Test
    void paymentTimeoutLeavesWalletUnchanged()
            throws PaymentService.PaymentException {
        PaymentService.PaymentTimeoutException timeout =
                new PaymentService.PaymentTimeoutException("Payment gateway timed out");

        when(payments.charge(seeker.getId(), PAYMENT_METHOD, TOP_UP)).thenThrow(timeout);

        assertThatThrownBy(() -> service.topUp(seeker.getId(), PAYMENT_METHOD, TOP_UP)).isSameAs(timeout);

        assertThat(seeker.getBalance()).isEqualTo(100.00);

        verify(payments).charge(seeker.getId(), PAYMENT_METHOD, TOP_UP);
        verify(seekers, never()).save(any(Seeker.class));
    }
}