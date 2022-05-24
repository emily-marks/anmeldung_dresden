import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import utils.AllBueros;
import utils.Month;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Main {

    public static LocalDate nearestDate;
    public static String bueroName;
    public static final LocalDate CURRENT_BOOKING = LocalDate.of(2022, java.time.Month.JUNE, 21);
    public static final String RED = "\033[0;31m";
    public static final String BLUE = "\033[0;34m";
    public static final String RESET = "\033[0m";

    public static void main(String[] args) throws IOException, InterruptedException {
        while(true) {
            for (int i = 0; i <= 8; i++) findForBuero(i);
            if (nearestDate.compareTo(CURRENT_BOOKING) < 0) {
                String message = "You can book " + nearestDate.format(DateTimeFormatter.ofPattern("dd.MM.yy")) + " in " + bueroName + " now";
                sendToEmail(message);
                System.out.println(message);
            } else {
                System.out.println(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm")) + "    " + RED + "No" + RESET + " available dates in June");
            }
            Thread.sleep(600000);
        }
    }

    private static void findForBuero(int i) throws IOException {
        URL url = new URL("https://termine.dresden.de/netappoint/index.php?company=stadtdresden-bb&cur_cause=" + i + "&step=2");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        int status = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
//        System.out.println(content);
        Document doc = Jsoup.parse(content.toString());
        String month = getFirstAvailableMonth(doc);
        String earliestDay = getEarliestDay(doc);
        if (earliestDay == null) {
//            System.out.println(AllBueros.getBueroById(i).name() + " has no available dates");
            return;
        }
        LocalDate dateOfCurrentBuero = LocalDate.of(2022, Month.getMonthId(month), Integer.parseInt(earliestDay.trim()));

        if (nearestDate == null || dateOfCurrentBuero.compareTo(nearestDate) < 0) {
            nearestDate = dateOfCurrentBuero;
            bueroName = AllBueros.getBueroById(i).name();
        }
//        if (Month.getMonthByName(month).isJune()) {
//            System.out.println(AllBueros.getBueroById(i) + ": " + month + ", first date = " + earliestDay);
//        }
        in.close();
    }

    private static String getFirstAvailableMonth(Document doc) {
        return doc.select("h2.nat_navigation_currentmonth").select("abbr").html().split(" ")[0];
    }

    // hardcoded attributes
    private static String getEarliestDay(Document doc) {
        Element firstDayInMonth = doc.select("a.nat_calendar_weekday_bookable").first();
        return firstDayInMonth == null ? null : firstDayInMonth.childNodes().stream().filter(node -> node instanceof TextNode && !((TextNode) node).isBlank()).findAny().get().toString();
    }

    private static void sendToEmail(String info) {
        String to = "to" ;
        String from = "from";

        String host = "smtp.yandex.com";
        Properties properties = System.getProperties();

        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        // Get the Session object.// and pass username and password
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {

            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {

                return new javax.mail.PasswordAuthentication(from, "xxx");

            }
        });

        // Used to debug SMTP issues
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("New appointment for Anmeldung is available now");
            message.setText(info);

            Transport.send(message);
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
