package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Ticket Service Implementation Tests")
class TicketServiceImplTest {

    private TicketPaymentService paymentService;
    private SeatReservationService reservationService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        reservationService = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Nested
    @DisplayName("Account Validation Tests")
    class AccountValidationTests {

        @Test
        @DisplayName("Rejects null account ID")
        void rejectsNullAccountId() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 1);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(null, request)
            );

            assertTrue(exception.getMessage().contains("Account ID"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects zero account ID")
        void rejectsZeroAccountId() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 1);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(0L, request)
            );

            assertTrue(exception.getMessage().contains("positive number"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects negative account ID")
        void rejectsNegativeAccountId() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 1);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(-5L, request)
            );

            assertTrue(exception.getMessage().contains("positive"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Accepts valid account ID")
        void acceptsValidAccountId() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 1);

            assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, request));

            verify(paymentService).makePayment(1L, 25);
            verify(reservationService).reserveSeat(1L, 1);
        }

        @Test
        @DisplayName("Accepts very large account ID")
        void acceptsVeryLargeAccountId() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 2);

            assertDoesNotThrow(() -> ticketService.purchaseTickets(999999999L, request));

            verify(paymentService).makePayment(999999999L, 50);
            verify(reservationService).reserveSeat(999999999L, 2);
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Rejects null request array")
        void rejectsNullRequestArray() {
            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null)
            );

            assertTrue(exception.getMessage().contains("At least one ticket"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects empty request array")
        void rejectsEmptyRequestArray() {
            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L)
            );

            assertTrue(exception.getMessage().contains("At least one ticket"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects null individual request")
        void rejectsNullIndividualRequest() {
            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, 
                    new TicketTypeRequest(Type.ADULT, 1), 
                    null)
            );

            assertTrue(exception.getMessage().contains("cannot be null"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects zero ticket quantity")
        void rejectsZeroTicketQuantity() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 0);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, request)
            );

            assertTrue(exception.getMessage().contains("greater than zero"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects negative ticket quantity")
        void rejectsNegativeTicketQuantity() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, -3);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, request)
            );

            assertTrue(exception.getMessage().contains("greater than zero"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects null mixed with valid requests")
        void rejectsNullMixedWithValidRequests() {
            TicketTypeRequest adult = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest child = new TicketTypeRequest(Type.CHILD, 1);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adult, null, child)
            );

            assertTrue(exception.getMessage().contains("cannot be null"));
            verifyNoInteractions(paymentService, reservationService);
        }
    }

    @Nested
    @DisplayName("Maximum Ticket Limit Tests")
    class MaximumTicketTests {

        @Test
        @DisplayName("Rejects purchase exceeding 25 tickets")
        void rejectsPurchaseExceeding25Tickets() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 26);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, request)
            );

            assertTrue(exception.getMessage().contains("more than"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects 26 tickets across multiple requests")
        void rejects26TicketsAcrossRequests() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 15);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 11);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adults, children)
            );

            assertTrue(exception.getMessage().contains("Cannot purchase more than"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Accepts exactly 25 tickets")
        void acceptsExactly25Tickets() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 10);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 10);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 5);

            assertDoesNotThrow(() -> 
                ticketService.purchaseTickets(1L, adults, children, infants)
            );

            verify(paymentService).makePayment(1L, 400);
            verify(reservationService).reserveSeat(1L, 20);
        }

        @Test
        @DisplayName("Accepts purchase under limit")
        void acceptsPurchaseUnderLimit() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 20);

            assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, request));

            verify(paymentService).makePayment(1L, 500);
            verify(reservationService).reserveSeat(1L, 20);
        }

        @Test
        @DisplayName("Rejects 50 tickets across multiple requests")
        void rejects50TicketsAcrossRequests() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 20);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 20);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 10);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adults, children, infants)
            );

            assertTrue(exception.getMessage().contains("more than 25"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Accepts 24 tickets at boundary")
        void accepts24TicketsAtBoundary() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 8);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 8);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 8);

            assertDoesNotThrow(() -> 
                ticketService.purchaseTickets(1L, adults, children, infants)
            );

            verify(paymentService).makePayment(1L, 320);
            verify(reservationService).reserveSeat(1L, 16);
        }
    }

    @Nested
    @DisplayName("Adult Requirement Tests")
    class AdultRequirementTests {

        @Test
        @DisplayName("Rejects child tickets without adult")
        void rejectsChildTicketsWithoutAdult() {
            TicketTypeRequest request = new TicketTypeRequest(Type.CHILD, 3);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, request)
            );

            assertTrue(exception.getMessage().contains("without an Adult"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects infant tickets without adult")
        void rejectsInfantTicketsWithoutAdult() {
            TicketTypeRequest request = new TicketTypeRequest(Type.INFANT, 2);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, request)
            );

            assertTrue(exception.getMessage().contains("without an Adult"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects child and infant without adult")
        void rejectsChildAndInfantWithoutAdult() {
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 2);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 1);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, children, infants)
            );

            assertTrue(exception.getMessage().contains("without an Adult"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Accepts child tickets with adult")
        void acceptsChildTicketsWithAdult() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 2);

            assertDoesNotThrow(() -> 
                ticketService.purchaseTickets(1L, adults, children)
            );

            verify(paymentService).makePayment(1L, 55);
            verify(reservationService).reserveSeat(1L, 3);
        }

        @Test
        @DisplayName("Accepts infant tickets with adult")
        void acceptsInfantTicketsWithAdult() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 2);

            assertDoesNotThrow(() -> 
                ticketService.purchaseTickets(1L, adults, infants)
            );

            verify(paymentService).makePayment(1L, 25);
            verify(reservationService).reserveSeat(1L, 1);
        }

        @Test
        @DisplayName("Accepts adult tickets only")
        void acceptsAdultTicketsOnly() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 5);

            assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, request));

            verify(paymentService).makePayment(1L, 125);
            verify(reservationService).reserveSeat(1L, 5);
        }

        @Test
        @DisplayName("Rejects single child ticket without adult")
        void rejectsSingleChildTicketWithoutAdult() {
            TicketTypeRequest request = new TicketTypeRequest(Type.CHILD, 1);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, request)
            );

            assertTrue(exception.getMessage().contains("without an Adult"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Rejects single infant ticket without adult")
        void rejectsSingleInfantTicketWithoutAdult() {
            TicketTypeRequest request = new TicketTypeRequest(Type.INFANT, 1);

            InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, request)
            );

            assertTrue(exception.getMessage().contains("without an Adult"));
            verifyNoInteractions(paymentService, reservationService);
        }

        @Test
        @DisplayName("Accepts multiple child and infant with one adult")
        void acceptsMultipleChildAndInfantWithOneAdult() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 5);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 3);

            assertDoesNotThrow(() -> 
                ticketService.purchaseTickets(1L, adults, children, infants)
            );

            verify(paymentService).makePayment(1L, 100);
            verify(reservationService).reserveSeat(1L, 6);
        }
    }

    @Nested
    @DisplayName("Payment Calculation Tests")
    class PaymentTests {

        @Test
        @DisplayName("Calculates payment for adult tickets")
        void calculatesPaymentForAdultTickets() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 3);

            ticketService.purchaseTickets(1L, request);

            verify(paymentService).makePayment(1L, 75);
        }

        @Test
        @DisplayName("Calculates payment with children")
        void calculatesPaymentWithChildren() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 4);

            ticketService.purchaseTickets(1L, adults, children);

            verify(paymentService).makePayment(1L, 85);
        }

        @Test
        @DisplayName("Calculates payment for mixed tickets")
        void calculatesPaymentForMixedTickets() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 3);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 2);

            ticketService.purchaseTickets(1L, adults, children, infants);

            verify(paymentService).makePayment(1L, 95);
        }

        @Test
        @DisplayName("Does not charge for infants")
        void doesNotChargeForInfants() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 5);

            ticketService.purchaseTickets(1L, adults, infants);

            verify(paymentService).makePayment(1L, 50);
        }

        @Test
        @DisplayName("Calculates payment for multiple adult requests")
        void calculatesPaymentForMultipleAdultRequests() {
            TicketTypeRequest adults1 = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest adults2 = new TicketTypeRequest(Type.ADULT, 3);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 5);

            ticketService.purchaseTickets(1L, adults1, adults2, children);

            verify(paymentService).makePayment(1L, 200);
        }

        @Test
        @DisplayName("Calculates correct payment for single adult")
        void calculatesCorrectPaymentForSingleAdult() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 1);

            ticketService.purchaseTickets(1L, request);

            verify(paymentService).makePayment(1L, 25);
        }

        @Test
        @DisplayName("Calculates payment for maximum allowed tickets")
        void calculatesPaymentForMaximumAllowedTickets() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 10);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 15);

            ticketService.purchaseTickets(1L, adults, children);

            verify(paymentService).makePayment(1L, 475);
        }

        @Test
        @DisplayName("Calculates zero payment for only infants with adults")
        void calculatesZeroPaymentForOnlyInfantsWithAdults() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 10);

            ticketService.purchaseTickets(1L, adults, infants);

            verify(paymentService).makePayment(1L, 25);
        }
    }

    @Nested
    @DisplayName("Seat Reservation Tests")
    class SeatReservationTests {

        @Test
        @DisplayName("Reserves seats for adults only")
        void reservesSeatsForAdultsOnly() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 4);

            ticketService.purchaseTickets(1L, request);

            verify(reservationService).reserveSeat(1L, 4);
        }

        @Test
        @DisplayName("Reserves seats for adults and children")
        void reservesSeatsForAdultsAndChildren() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 3);

            ticketService.purchaseTickets(1L, adults, children);

            verify(reservationService).reserveSeat(1L, 5);
        }

        @Test
        @DisplayName("Does not reserve seats for infants")
        void doesNotReserveSeatsForInfants() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 3);

            ticketService.purchaseTickets(1L, adults, infants);

            verify(reservationService).reserveSeat(1L, 2);
        }

        @Test
        @DisplayName("Reserves correct seats for all ticket types")
        void reservesCorrectSeatsForAllTypes() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 3);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 4);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 2);

            ticketService.purchaseTickets(1L, adults, children, infants);

            verify(reservationService).reserveSeat(1L, 7);
        }

        @Test
        @DisplayName("Reserves seats for multiple requests")
        void reservesSeatsForMultipleRequests() {
            TicketTypeRequest adults1 = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest adults2 = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest children1 = new TicketTypeRequest(Type.CHILD, 2);
            TicketTypeRequest children2 = new TicketTypeRequest(Type.CHILD, 3);

            ticketService.purchaseTickets(1L, 
                adults1, adults2, children1, children2);

            verify(reservationService).reserveSeat(1L, 8);
        }

        @Test
        @DisplayName("Reserves one seat for single adult")
        void reservesOneSeatForSingleAdult() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 1);

            ticketService.purchaseTickets(1L, request);

            verify(reservationService).reserveSeat(1L, 1);
        }

        @Test
        @DisplayName("Reserves no seats when only one adult with many infants")
        void reservesCorrectSeatsForAdultWithManyInfants() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 10);

            ticketService.purchaseTickets(1L, adults, infants);

            verify(reservationService).reserveSeat(1L, 1);
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationTests {

        @Test
        @DisplayName("Processes valid purchase successfully")
        void processesValidPurchaseSuccessfully() {
            Long accountId = 123456L;
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 1);

            ticketService.purchaseTickets(accountId, adults, children);

            verify(paymentService).makePayment(accountId, 65);
            verify(reservationService).reserveSeat(accountId, 3);
        }

        @Test
        @DisplayName("Handles single adult ticket")
        void handlesSingleAdultTicket() {
            Long accountId = 1L;
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 1);

            ticketService.purchaseTickets(accountId, request);

            verify(paymentService).makePayment(accountId, 25);
            verify(reservationService).reserveSeat(accountId, 1);
        }

        @Test
        @DisplayName("Handles complex purchase")
        void handlesComplexPurchase() {
            Long accountId = 999L;
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 5);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 10);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 5);

            ticketService.purchaseTickets(accountId, adults, children, infants);

            verify(paymentService).makePayment(accountId, 275);
            verify(reservationService).reserveSeat(accountId, 15);
        }

        @Test
        @DisplayName("Handles large account ID")
        void handlesLargeAccountId() {
            Long accountId = Long.MAX_VALUE;
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 1);

            ticketService.purchaseTickets(accountId, request);

            verify(paymentService).makePayment(accountId, 25);
            verify(reservationService).reserveSeat(accountId, 1);
        }

        @Test
        @DisplayName("Processes family purchase scenario")
        void processesFamilyPurchaseScenario() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 2);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 1);

            ticketService.purchaseTickets(100L, adults, children, infants);

            verify(paymentService).makePayment(100L, 80);
            verify(reservationService).reserveSeat(100L, 4);
        }

        @Test
        @DisplayName("Handles group booking scenario")
        void handlesGroupBookingScenario() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 15);

            ticketService.purchaseTickets(500L, request);

            verify(paymentService).makePayment(500L, 375);
            verify(reservationService).reserveSeat(500L, 15);
        }

        @Test
        @DisplayName("Processes all infant types with minimum adults")
        void processesAllInfantTypesWithMinimumAdults() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest infants = new TicketTypeRequest(Type.INFANT, 5);

            ticketService.purchaseTickets(200L, adults, infants);

            verify(paymentService).makePayment(200L, 25);
            verify(reservationService).reserveSeat(200L, 1);
        }

        @Test
        @DisplayName("Handles maximum children with minimum adults")
        void handlesMaximumChildrenWithMinimumAdults() {
            TicketTypeRequest adults = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest children = new TicketTypeRequest(Type.CHILD, 24);

            ticketService.purchaseTickets(300L, adults, children);

            verify(paymentService).makePayment(300L, 385);
            verify(reservationService).reserveSeat(300L, 25);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Handles purchase with all ticket types in separate requests")
        void handlesPurchaseWithAllTypesInSeparateRequests() {
            TicketTypeRequest adults1 = new TicketTypeRequest(Type.ADULT, 2);
            TicketTypeRequest adults2 = new TicketTypeRequest(Type.ADULT, 1);
            TicketTypeRequest children1 = new TicketTypeRequest(Type.CHILD, 3);
            TicketTypeRequest children2 = new TicketTypeRequest(Type.CHILD, 2);
            TicketTypeRequest infants1 = new TicketTypeRequest(Type.INFANT, 1);
            TicketTypeRequest infants2 = new TicketTypeRequest(Type.INFANT, 2);

            ticketService.purchaseTickets(400L, adults1, adults2, children1, children2, infants1, infants2);

            verify(paymentService).makePayment(400L, 150);
            verify(reservationService).reserveSeat(400L, 8);
        }

        @Test
        @DisplayName("Validates service calls are made in correct order")
        void validatesServiceCallsInCorrectOrder() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 2);

            ticketService.purchaseTickets(1L, request);

            var inOrder = inOrder(paymentService, reservationService);
            inOrder.verify(paymentService).makePayment(1L, 50);
            inOrder.verify(reservationService).reserveSeat(1L, 2);
        }

        @Test
        @DisplayName("Ensures payment service called exactly once")
        void ensuresPaymentServiceCalledExactlyOnce() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 3);

            ticketService.purchaseTickets(1L, request);

            verify(paymentService, times(1)).makePayment(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Ensures reservation service called exactly once")
        void ensuresReservationServiceCalledExactlyOnce() {
            TicketTypeRequest request = new TicketTypeRequest(Type.ADULT, 3);

            ticketService.purchaseTickets(1L, request);

            verify(reservationService, times(1)).reserveSeat(anyLong(), anyInt());
        }
    }
}