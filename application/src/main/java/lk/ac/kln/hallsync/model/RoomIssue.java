package lk.ac.kln.hallsync.model;
import java.time.LocalDateTime;
public record RoomIssue(int id,int hallId,String hallLabel,int reporterId,String reporterName,String category,String details,String status,LocalDateTime createdAt,String reviewerName,LocalDateTime reviewedAt,String reviewNote) { }
