-- HallSync - Step 1: create the project database.
-- Run once in MySQL Workbench. This step creates an empty database.
-- If a statement fails, stop and inspect the error before continuing.

CREATE DATABASE hallsync_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE hallsync_db;

SELECT DATABASE() AS CurrentDatabase;
