package lk.ac.kln.hallsync.service;

import lk.ac.kln.hallsync.auth.AuthenticatedUser;

/** Shared role checks; database write procedures independently recheck current roles. */
public final class AccessRules {
    private AccessRules() { }
    public static int signedIn(AuthenticatedUser user) {
        if (user == null) { throw new SecurityException("Sign in before using this feature."); }
        return user.getAccountId();
    }
    public static int administrator(AuthenticatedUser user) {
        int id = signedIn(user);
        if (!user.isAdministrator()) { throw new SecurityException("Only an administrator may perform this action."); }
        return id;
    }
    public static int requester(AuthenticatedUser user) {
        int id = signedIn(user);
        if (!user.isAdministrator() && !"Lecturer".equals(user.getRole())) {
            throw new SecurityException("Only lecturers or authorised staff may request bookings.");
        }
        return id;
    }
}
