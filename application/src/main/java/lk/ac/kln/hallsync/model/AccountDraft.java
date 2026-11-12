package lk.ac.kln.hallsync.model;

/** Passwords are passed separately as char arrays and are not part of this model. */
public record AccountDraft(Integer departmentId, String fullName, String email, String username) { }
