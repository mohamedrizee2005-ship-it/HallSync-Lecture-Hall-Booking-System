package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.DatabaseConnectionFactory;
import lk.ac.kln.hallsync.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class JdbcReportRepository implements ReportRepository {
    private final DatabaseConnectionFactory connections;
    public JdbcReportRepository(DatabaseConnectionFactory connections) { this.connections = connections; }
    @Override public ReportTable load(ReportKind kind) throws SQLException { return load(kind,null,null); }
    @Override public ReportTable load(ReportKind kind,java.time.LocalDate from,java.time.LocalDate to) throws SQLException {
        boolean dated=from!=null&&to!=null;

        String sql = switch (kind) {
            case BookingDetails -> "SELECT * FROM v_booking_details ORDER BY BookingDate,StartTime,BookingID";
            case StatusTotals -> "SELECT * FROM v_booking_status_totals ORDER BY BookingStatus";
            case HallFacilities -> "SELECT * FROM v_hall_facilities ORDER BY HallID,FacilityID";
            case HallUsage -> "SELECT * FROM v_hall_usage ORDER BY BookingDate,HallID";
        };
        if(dated)sql=switch(kind){
            case BookingDetails -> "SELECT * FROM v_booking_details WHERE BookingDate BETWEEN ? AND ? ORDER BY BookingDate,StartTime,BookingID";
            case StatusTotals -> "SELECT BookingStatus,COUNT(*) AS BookingCount FROM v_booking_details WHERE BookingDate BETWEEN ? AND ? GROUP BY BookingStatus ORDER BY BookingStatus";
            case HallUsage -> "SELECT * FROM v_hall_usage WHERE BookingDate BETWEEN ? AND ? ORDER BY BookingDate,HallID";
            case HallFacilities -> sql;
        };
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(sql)) {
            if(dated&&kind!=ReportKind.HallFacilities){s.setObject(1,from);s.setObject(2,to);}
            try(ResultSet r=s.executeQuery()) {
            List<String> columns = new ArrayList<>(); List<List<String>> rows = new ArrayList<>();
            int count = r.getMetaData().getColumnCount();
            for (int i = 1; i <= count; i++) { columns.add(r.getMetaData().getColumnLabel(i)); }
            while (r.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= count; i++) { String value = r.getString(i); row.add(value == null ? "" : value); }
                rows.add(row);
            }
            return new ReportTable(columns, rows);
            }
        }
    }
}
