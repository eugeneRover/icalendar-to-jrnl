package ep;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Can work in two modes:
 * 1. Convert ICS (iCalendar format) file to JRNL format:
 *    java -jar ./target/icalendar-to-jrnl.jar -f /path/to/your/calendar.ics | jrnl --import
 *    You may export iCalendar format from Google Calendar by clicking on the gear icon top-right and selecting "Settings" -> "Select desired calendar" -> "Export calendar"
 * 
 * 2. Get events data from Google Calendar by Secret iCal URL and convert to JRNL format
 *    java -jar ./target/icalendar-to-jrnl.jar -u https://calendar.google.com/calendar/ical/your_secret_ical_url | jrnl --import
 *   
 *
 *
 */
public class App {

    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter OUTPUT_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static void printUsageAndExit() {
        System.err.println("Usage:\n  -f <path-to-ics>\n  -u <google-calendar-secret-ical-url>");
    }

    private static void printSecretUrlInstructions() {
        System.err.println("Provide the Secret iCal URL of your Google Calendar.\n"
            + "Find it in Google Calendar: Gear icon top-right  -> Settings -> Select desired calendar -> Integrate calendar -> Secret address in iCal format.\n"
            + "Usage: -u <secret-ical-url>");
    }

    public static void main( String[] args ) throws IOException, ParserException {

        if (args.length < 1) {
            printUsageAndExit();
            return;
        }

        String mode = args[0];
        Calendar calendar;

        if ("-f".equals(mode)) {
            if (args.length < 2) {
                printUsageAndExit();
                return;
            }
            try (FileInputStream fin = new FileInputStream(args[1])) {
                CalendarBuilder builder = new CalendarBuilder();
                calendar = builder.build(fin);
            }
        } else if ("-u".equals(mode)) {
            if (args.length < 2 || args[1] == null || args[1].isBlank()) {
                printSecretUrlInstructions();
                return;
            }
            String icalUrl = args[1];
            try (InputStream in = fetchIcs(icalUrl)) {
                CalendarBuilder builder = new CalendarBuilder();
                calendar = builder.build(in);
            } catch (Exception e) {
                throw new IOException("Failed to fetch or parse iCal URL: " + e.getMessage(), e);
            }
        } else {
            printUsageAndExit();
            return;
        }

        JrnlImport imp1 = new JrnlImport(
            calendar.getComponents().stream()
                .map(c->{

                    Optional<Property> optDtstart = c.getProperty("DTSTART");
                    if (optDtstart.isEmpty()) return null;
                    String[] dt = dtstartToJrnlFormat(optDtstart.get().getValue());
                    String date = dt[0];
                    String time = dt[1];

                    Optional<Property> optSummary = c.getProperty("SUMMARY");
                    if (optSummary.isEmpty()) return null;
                    String[] tb = summaryToTitleAndBody(optSummary.get().getValue());
                    String title = tb[0];
                    String body = tb[1];

                    return new JrnlImport.JrnlImportEntry(
                        title, body, date, time, List.of(), false
                    );
                })
                .filter(Objects::nonNull)
                .toList()
        );

        String output = writeToJrnlFormat(imp1);
        System.out.printf("%s\r\n",output );
    }

    private static InputStream fetchIcs(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return new ByteArrayInputStream(response.body());
        }
        throw new IOException("HTTP status: " + response.statusCode());
    }

    private static String writeToJrnlFormat(JrnlImport imp1) {
        StringBuilder sb = new StringBuilder();
        imp1.getEntries().forEach(d->{
            sb.append("[")
                .append(d.getDate())
                .append(" ")
                .append(d.getTime())
                .append("] ")
                .append(d.getTitle())
                .append("\r\n");
            if (d.getBody()!=null && d.getBody().length()>0){
                sb.append(d.getBody()).append("\r\n");
            }
            sb.append("\r\n");
        });

        return sb.toString();
    }

    private static String[] dtstartToJrnlFormat(String value) {
        LocalDateTime ldt = LocalDateTime.parse(value, INPUT_FMT);
        return new String[]{
            ldt.toLocalDate().format(OUTPUT_DATE_FMT),
            ldt.toLocalTime().format(OUTPUT_TIME_FMT)
        };

    }

    private static String[] summaryToTitleAndBody(String value) {
        String[] tb = value.split("\\.", 2);
        String title = tb[0];
        String body = "";
        if (tb.length<2){

        }
        else {
            body = tb[1];
            title = title+".";
        }
        return new String[]{title, body};
    }


}
