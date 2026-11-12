package lk.ac.kln.hallsync.model;

import java.time.LocalDateTime;

public record HallBlock(int id, int hallId, String hallLabel, String recordedBy,
                        LocalDateTime start, LocalDateTime end, String reason, String status) { }
