package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    
    private static final int MAXIMUM_TICKET_LIMIT = 25;
    private static final int PRICE_PER_ADULT = 25;
    private static final int PRICE_PER_CHILD = 15;
    private static final int PRICE_PER_INFANT = 0;
    
    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;
    
    public TicketServiceImpl(TicketPaymentService paymentService, 
                             SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) 
            throws InvalidPurchaseException {
        
        checkAccountValidity(accountId);
        checkRequestValidity(ticketTypeRequests);
        
        int adultCount = 0;
        int childCount = 0;
        int infantCount = 0;
        
        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT:
                    adultCount += request.getNoOfTickets();
                    break;
                case CHILD:
                    childCount += request.getNoOfTickets();
                    break;
                case INFANT:
                    infantCount += request.getNoOfTickets();
                    break;
            }
        }
        
        checkBusinessRules(adultCount, childCount, infantCount);
        
        int totalPayment = computeTotalCost(adultCount, childCount, infantCount);
        int seatsRequired = computeSeatsNeeded(adultCount, childCount);
        
        paymentService.makePayment(accountId, totalPayment);
        reservationService.reserveSeat(accountId, seatsRequired);
    }

    private int computeTotalCost(int adultCount, int childCount, int infantCount) {
        return (adultCount * PRICE_PER_ADULT) 
             + (childCount * PRICE_PER_CHILD) 
             + (infantCount * PRICE_PER_INFANT);
    }

    private int computeSeatsNeeded(int adultCount, int childCount) {
        // Infants sit on adult laps, so they don't need seats
        return adultCount + childCount;
    }

    private void checkAccountValidity(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account ID must be a positive number");
        }
    }

    private void checkRequestValidity(TicketTypeRequest... requests) {
        if (requests == null || requests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket must be requested");
        }
        
        for (TicketTypeRequest request : requests) {
            if (request == null) {
                throw new InvalidPurchaseException("Ticket request cannot be null");
            }
            if (request.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException("Number of tickets must be greater than zero");
            }
        }
    }

    private void checkBusinessRules(int adultCount, int childCount, int infantCount) {
        int totalTickets = adultCount + childCount + infantCount;
        
        if (totalTickets > MAXIMUM_TICKET_LIMIT) {
            throw new InvalidPurchaseException(
                String.format("Cannot purchase more than %d tickets. Requested: %d", 
                    MAXIMUM_TICKET_LIMIT, totalTickets)
            );
        }
        
        // Child and infant tickets require at least one adult
        if (adultCount == 0 && (childCount > 0 || infantCount > 0)) {
            throw new InvalidPurchaseException(
                "Child and Infant tickets cannot be purchased without an Adult ticket"
            );
        }
    }

}