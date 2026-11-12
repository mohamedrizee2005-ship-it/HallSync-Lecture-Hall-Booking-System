package lk.ac.kln.hallsync.service;
import lk.ac.kln.hallsync.model.ScheduleEntry;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
/** RFC 5545 calendar with UTC event times and stable booking UIDs. */
public final class CalendarExporter {
 private static final DateTimeFormatter UTC=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
 private CalendarExporter(){}
 public static String export(List<ScheduleEntry> events){
  StringBuilder b=new StringBuilder();line(b,"BEGIN:VCALENDAR");line(b,"VERSION:2.0");line(b,"PRODID:-//HallSync//University of Kelaniya//EN");line(b,"CALSCALE:GREGORIAN");line(b,"METHOD:PUBLISH");
  for(var e:events){line(b,"BEGIN:VEVENT");line(b,"UID:booking-"+e.bookingId()+"@hallsync.local");line(b,"DTSTAMP:"+UTC.format(Instant.now()));line(b,"DTSTART:"+UTC.format(LocalDateTime.of(e.date(),e.start()).atZone(BookingService.ZONE).toInstant()));line(b,"DTEND:"+UTC.format(LocalDateTime.of(e.date(),e.end()).atZone(BookingService.ZONE).toInstant()));line(b,"SUMMARY:"+escape(e.courseCode()==null?e.type():e.courseCode()));line(b,"LOCATION:"+escape(e.hallLabel()+", University of Kelaniya"));line(b,"STATUS:CONFIRMED");line(b,"END:VEVENT");}
  line(b,"END:VCALENDAR");return b.toString();
 }
 private static String escape(String s){return s.replace("\\","\\\\").replace("\r\n","\n").replace("\r","\n").replace("\n","\\n").replace(";","\\;").replace(",","\\,");}
 private static void line(StringBuilder out,String s){int bytes=0;for(int p=0;p<s.length();){int cp=s.codePointAt(p);String ch=new String(Character.toChars(cp));int n=ch.getBytes(StandardCharsets.UTF_8).length;if(bytes+n>75){out.append("\r\n ");bytes=1;}out.append(ch);bytes+=n;p+=Character.charCount(cp);}out.append("\r\n");}
}
