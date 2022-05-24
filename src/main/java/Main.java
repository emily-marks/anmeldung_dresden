import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import utils.AllBueros;
import utils.Month;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static utils.Constants.*;
import static utils.EmailUtils.sendToEmail;

public class Main {

    public static LocalDate nearestDate;
    public static String officeName;
    public static final LocalDate CURRENT_BOOKING = LocalDate.of(2022, java.time.Month.JUNE, 21);
    private final static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        while (true) {
            for (int i = 0; i <= 8; i++) checkMostRecentDateByOffice(i);

            if (nearestDate.compareTo(CURRENT_BOOKING) < 0) {
                String message = "You can book " + nearestDate.format(DateTimeFormatter.ofPattern("dd.MM.yy")) + " in " + officeName + " now";
                sendToEmail(message);
                logger.info(message);
                break;
            } else {
                logger.info("No available dates at " + LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm")));
            }
            Thread.sleep(600000);
        }
    }

    private static void checkMostRecentDateByOffice(int officeId) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(getConnection(officeId).getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        Document doc = Jsoup.parse(content.toString());
        String month = getFirstAvailableMonth(doc);
        String dayOfMonth = getFirstDay(doc);

        // skip office
        if (dayOfMonth == null) {
            return;
        }

        LocalDate dateOfCurrentOffice = LocalDate.of(2022, Month.getMonthId(month), Integer.parseInt(dayOfMonth.trim()));

        if (nearestDate == null || dateOfCurrentOffice.compareTo(nearestDate) < 0) {
            nearestDate = dateOfCurrentOffice;
            officeName = AllBueros.getBueroById(officeId).name();
        }
        in.close();
    }

    private static HttpURLConnection getConnection(int officeId) throws IOException {
        URL url = new URL(APPOINTMENT_URL + officeId + STEP_PARAM);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", CONTENT_TYPE);
        return con;
    }

    private static String getFirstAvailableMonth(Document doc) {
        return doc.select("h2.nat_navigation_currentmonth")
                .select("abbr")
                .html()
                .split(" ")[0];
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
}
