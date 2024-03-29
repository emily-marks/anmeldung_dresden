import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import utils.AllBueros;
import utils.Month;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.util.Map.Entry.comparingByValue;
import static utils.Constants.*;
import static utils.EmailUtils.sendToEmail;

public class Main {

    public static LocalDate nearestDate;
    public static Map<String, LocalDate> batchAppointments = new HashMap<>();
    public static String officeName;
    public static final LocalDate CURRENT_BOOKING = LocalDate.of(2023, java.time.Month.DECEMBER, 15);
    private final static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {
//        bürger_büro();
        führerscheinbehörde();
    }

    private static void führerscheinbehörde() throws InterruptedException, IOException {
        while (true) {
            nearestDate = checkMostRecentDate();

            if (nearestDate.compareTo(CURRENT_BOOKING) < 0) {
                String message = "You can book " + nearestDate.format(DateTimeFormatter.ofPattern("dd.MM.yy"));
                sendToEmail(message);
                logger.info(message);
                break;
            } else {
                logger.info("Last date available: " + nearestDate);
            }
            Thread.sleep(600000);
        }
    }

    private static void bürger_büro() throws IOException, InterruptedException {
        while (true) {
            for (int i = 0; i <= 8; i++) checkMostRecentDateByOffice(i);

            if (nearestDate.compareTo(CURRENT_BOOKING) < 0) {
                String message = "You can book " + nearestDate.format(DateTimeFormatter.ofPattern("dd.MM.yy")) + " in " + officeName + " now";
                sendToEmail(message);
                logger.info(message);
                break;
            } else {
                printSorted(batchAppointments);
                logger.info("No available appointments.");
            }
            Thread.sleep(600000);
        }
    }

    private static void checkMostRecentDateByOffice(int officeId) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader( getConnection(officeId).getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        Document doc = Jsoup.parse(content.toString());
        String month = getFirstAvailableMonth(doc);
        String year = getFirstAvailableYear(doc);
        String dayOfMonth = getFirstDay(doc);

        // skip office
        if (dayOfMonth == null) {
            return;
        }
        String officeName = AllBueros.getBueroById(officeId).name();
        LocalDate dateOfCurrentOffice = LocalDate.of(parseInt(year), Month.getMonthId(month), parseInt(dayOfMonth.trim()));

        batchAppointments.put(officeName, dateOfCurrentOffice);

        if (nearestDate == null || dateOfCurrentOffice.compareTo(nearestDate) < 0) {
            nearestDate = dateOfCurrentOffice;
            Main.officeName = officeName;
        }
        in.close();
    }

    private static Document read(HttpURLConnection con) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        Document doc = Jsoup.parse(content.toString());
        in.close();
        return doc;
    }

    private static LocalDate checkMostRecentDate() throws IOException {
        Document basePage = read(getFbBaseConnection());
        String zf = getZF(basePage);
        Document bookingPage = read(getFbConnection(zf));
        String month = getFirstAvailableMonth(bookingPage);
        String year = "2023";
        String dayOfMonth = getFirstDay(bookingPage);

        LocalDate date = LocalDate.of(parseInt(year), Month.getMonthId(month), parseInt(dayOfMonth.trim()));
        return date;
    }

    private static HttpURLConnection getConnection(int officeId) throws IOException {
        URL url = new URL(APPOINTMENT_BÜRGERBÜRO_URL + officeId + STEP_PARAM);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", CONTENT_TYPE);
        return con;
    }

    private static HttpURLConnection getFbBaseConnection() throws IOException {
        URL url = new URL(APPOINTMENT_FÜHRERSCHEINBEHÖRDE_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", CONTENT_TYPE);
        return con;
    }

    private static HttpURLConnection getFbConnection(String zf) throws IOException {
        String FORM_DATA="casetype_104=1&sentcasetypes=Weiter";
        URL url = new URL(APPOINTMENT_FÜHRERSCHEINBEHÖRDE_URL + STEP_PARAM + DIENSTLEISTUNG_PARAM + zf + FORM_DATA);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", CONTENT_TYPE);
        con.setDoOutput(true);
        try( DataOutputStream wr = new DataOutputStream( con.getOutputStream())) {
            wr.write( FORM_DATA.getBytes(StandardCharsets.UTF_8) );
        }
//        String urlParameters = "Content-Disposition: form-data; casetype_104=1; sentcasetypes=Weiter\r\n";
//        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
//        wr.writeBytes(urlParameters);
//        wr.flush();
//        wr.close();
        return con;
    }

    private static String getZF(Document doc) {
        return doc.select("div.terminbuchung")
                .select("form")
                .attr("action")
                .split("zf=")[1];
    }

    private static String getFirstAvailableMonth(Document doc) {
        return doc.select("h2.nat_navigation_currentmonth")
                .select("abbr")
                .html()
                .split(" ")[0];
    }

    private static String getFirstAvailableYear(Document doc) {
        return doc.select("h2.nat_navigation_currentmonth")
                .select("abbr")
                .html()
                .split(" ")[1];
    }

    // hardcoded attributes
    private static String getFirstDay(Document doc) {
        Element firstDayInMonth = doc.select("a.nat_calendar_weekday_bookable").first();
        return firstDayInMonth == null ? null : firstDayInMonth
                .childNodes().stream()
                .filter(node -> node instanceof TextNode && !((TextNode) node).isBlank())
                .findAny()
                .get()
                .toString();
    }

    private static void printSorted(Map<String, LocalDate> appointments) {
        appointments.entrySet()
                .stream()
                .sorted(comparingByValue())
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue().format(DateTimeFormatter.ofPattern("dd.MM.yy"))));
    }
}
