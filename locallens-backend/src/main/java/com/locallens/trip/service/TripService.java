package com.locallens.trip.service;

import com.locallens.common.exception.ResourceNotFoundException;
import com.locallens.trip.dto.CreateTripRequest;
import com.locallens.trip.model.TripDocument;
import com.locallens.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final GridFsTemplate gridFsTemplate;

    public TripDocument createTrip(String userId, CreateTripRequest req) {
        TripDocument trip = new TripDocument();
        trip.setUserId(userId);
        trip.setTitle(req.title());

        TripDocument.Destination dest = new TripDocument.Destination();
        dest.setCity(req.destinationCity());
        dest.setCountry(req.destinationCountry());
        dest.setLocation(new GeoJsonPoint(req.lng(), req.lat()));
        trip.setDestination(dest);

        trip.setStartDate(req.startDate());
        trip.setEndDate(req.endDate());
        trip.setDurationDays((int) ChronoUnit.DAYS.between(req.startDate(), req.endDate()));
        trip.setGroupSize(req.groupSize());
        trip.setTravelStyle(req.travelStyle());
        trip.setInterests(req.interests());
        trip.setAccessibilityNeeds(req.accessibilityNeeds());

        TripDocument.Budget budget = new TripDocument.Budget();
        budget.setTotal(req.budgetTotal());
        budget.setCurrency(req.budgetCurrency());
        budget.setBreakdown(new TripDocument.BudgetBreakdown());
        trip.setBudget(budget);

        trip.setStatus("DRAFT");
        trip.setItineraryVersion(0);
        return tripRepository.save(trip);
    }

    public List<TripDocument> getUserTrips(String userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public TripDocument getTripById(String tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
    }

    public void deleteTrip(String tripId) {
        tripRepository.deleteById(tripId);
    }

    public List<TripDocument> getActiveTrips(LocalDate date) {
        return tripRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                "ACTIVE", date, date);
    }

    public String uploadDocument(String tripId, MultipartFile file, String docType) throws IOException {
        TripDocument trip = getTripById(tripId);
        String gridFsId = gridFsTemplate.store(
                file.getInputStream(), file.getOriginalFilename(), file.getContentType()).toString();

        TripDocument.TripDoc doc = new TripDocument.TripDoc();
        doc.setType(docType);
        doc.setFileName(file.getOriginalFilename());
        doc.setGridFsFileId(gridFsId);
        doc.setUploadedAt(Instant.now());
        trip.getDocuments().add(doc);
        tripRepository.save(trip);
        return gridFsId;
    }
}
