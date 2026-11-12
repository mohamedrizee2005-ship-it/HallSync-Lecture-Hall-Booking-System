package lk.ac.kln.hallsync.model;

import java.time.LocalDateTime;

/** Administrator-only review data. Password hashes are deliberately excluded. */
public record AccountApplication(int id,int accountId,Integer departmentId,String department,
        String fullName,String email,String username,String staffId,String role,String status,boolean active,
        LocalDateTime submittedAt,Integer reviewerId,String reviewerName,LocalDateTime reviewedAt,String reviewNote) { }
