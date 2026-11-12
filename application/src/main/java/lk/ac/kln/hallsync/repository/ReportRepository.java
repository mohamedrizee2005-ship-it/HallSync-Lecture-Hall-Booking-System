package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.*;
import java.sql.SQLException;

public interface ReportRepository { ReportTable load(ReportKind kind) throws SQLException; }
