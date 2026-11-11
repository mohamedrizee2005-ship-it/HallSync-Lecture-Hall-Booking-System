# HallSync-Lecture-Hall-Booking-System

## Purpose

HallSync is a university group project to manage lecture hall availability and booking requests for the Faculty of Science, University of Kelaniya, Sri Lanka.

The project aims to prevent conflicting approved bookings, help authorised users find suitable halls, and maintain booking records. The planned system will use MySQL and object-oriented programming (OOP).

## Users

| User | Responsibility |
| --- | --- |
| Student | View lecture hall details and availability. |
| Lecturer | Submit booking requests and manage their own requests according to the booking rules. |
| Administrator | Manage lecture halls and review, approve, or reject booking requests. |

## Basic Booking Workflow

1. A lecturer selects a hall, booking date, start time, and end time, and provides the purpose and expected attendance.
2. The system records the submitted request with the status **Pending**.
3. An authorised administrator reviews the request.
4. Before approval, the system checks that the hall has sufficient capacity and that the requested period does not overlap an approved booking or an active hall closure.
5. An administrator must not approve their own booking request.
6. A successful approval changes the request to **Approved**. A declined request is recorded as **Rejected**.

A **Pending** request does not reserve the hall. Availability must be checked again when a request is approved.
