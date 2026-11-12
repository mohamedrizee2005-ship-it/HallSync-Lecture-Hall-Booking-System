package lk.ac.kln.hallsync.model;

public record HallFacility(int hallId, String hallLabel, int facilityId,
                           String facilityName, int quantity, int workingQuantity) { }
