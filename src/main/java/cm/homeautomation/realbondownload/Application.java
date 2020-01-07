package cm.homeautomation.realbondownload;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.validation.constraints.Pattern.Flag;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;

import cm.homeautomation.realbondownload.entities.Bon;
import cm.homeautomation.realbondownload.entities.BonPosition;

@SpringBootApplication
@EnableScheduling
public class Application {

    private static String mailServer;
    private static String mailAddress;
    private static String mailPassword;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * read mails from server
     * 
     */
    public void readMails(String mailServer, String mailAddress, String password) {
        Properties props = new Properties();
        try {
            Session session = Session.getDefaultInstance(props, null);

            Store store = session.getStore("imaps");
            store.connect(mailServer, mailAddress, password);

            Folder inbox = store.getFolder("inbox");
            inbox.open(Folder.READ_ONLY);
            int messageCount = inbox.getMessageCount();

            System.out.println("Total Messages:- " + messageCount);

            EntityManager em = EntityManagerService.getNewManager();

            Date currentDate= new Date();
            Date latestReceivedDate = new Date(currentDate.getTime()- 14*86400*1000);

            Message[] messages = inbox.getMessages();
            for (int i = 1; i < 100; i++) {
                Message message = messages[messages.length - i];
                String messageFrom = message.getFrom()[0].toString();
                String messageSubject = message.getSubject();
                String messageId = message.getHeader("Message-ID")[0];
                Date messageReceived = message.getReceivedDate();

                if (messageReceived.compareTo(latestReceivedDate)> 0) {

                    if ("PAYBACK Service <service@payback.de>".equals(messageFrom)
                            && "Ihr neuer Punktestand!".equals(messageSubject)) {

                        List<Bon> bonList = em.createQuery("select b from Bon b where b.messageId=:messageId", Bon.class)
                                .setParameter("messageId", messageId).getResultList();

                        //System.out.println("Mail : " + messageFrom + "- " + messageSubject + " - " + messageId);

                        if (bonList == null || bonList.isEmpty()) {

                            List<String> urls = urlFilterFinder(getTextFromMessage(message));
                            System.out.println(urls);
                            System.out.println(urls.get(2));

                            try {
                                fetchBon(urls.get(2), messageId);
                            } catch (Exception e) {
                                System.out.println("not storing");
                            }

                        }
                    }
                }
            }
            inbox.close(true);
            store.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * filter relevant URLs
     * 
     */
    public List<String> urlFilterFinder(String message) {
        List<String> urls = urlFinder(message);
        List<String> filteredUrls = new ArrayList<>();

        for (String url : urls) {

            if (url.startsWith("https://trxmail1.payback.de/go")) {
                filteredUrls.add(url);
            }
        }
        return filteredUrls;

    }

    /**
     * 
     */
    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            System.out.println("part: " + i);
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                String plainHtml = org.jsoup.Jsoup.parse(html).text();
                result = result + "\n" + html;
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
            }
        }
        return result;
    }

    /**
     * 
     */
    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    /**
     * entry point for application
     */
    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            mailServer = args[0];
            mailAddress = args[1];
            mailPassword = args[2];
        };
    }

    /**
     * external check call entry point
     * 
     */
    public void checkMails() {
        readMails(mailServer, mailAddress, mailPassword);
    }

    /**
     * fetch BON for single mail entry
     * 
     */
    private void fetchBon(String bonInitialUrl, String messageId) throws ParseException {

        EntityManager em = EntityManagerService.getNewManager();
        em.getTransaction().begin();

        String url = bonInitialUrl;

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
        Map map = new HashMap<String, String>();
        map.put("Content-Type", "application/json");
        headers.setAll(map);
        Map req_payload = new HashMap();
        req_payload.put("name", "piyush");

        HttpEntity<?> request = new HttpEntity<>(req_payload, headers);

        // Create a new RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Add the String message converter
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        String newLocation = response.getHeaders().get("Location").get(0);

        newLocation = newLocation.replace("https://www.payback.de/pb/ebon?t=", "").split("&")[0];

        ResponseEntity<String> newResponse = restTemplate.postForEntity(newLocation, request, String.class);

        String bonLocation1 = newResponse.getHeaders().get("Location").get(0);

        ResponseEntity<String> bonResponse1 = restTemplate.postForEntity(bonLocation1, request, String.class);

        String bonLocation2 = urlFinder(bonResponse1.getBody().toString()).get(0).replaceAll("&amp;", "&")
                .replaceAll("%25253D", "=").replaceAll("%252526", "&").replaceAll("http://", "https://");

        ResponseEntity<String> bonResponse2 = restTemplate.postForEntity(bonLocation2, request, String.class);

        System.out.println(bonResponse2.getBody().toString());

        Document doc = Jsoup.parse(bonResponse2.getBody().toString());

        Elements trs = doc.select("table.-striped").select("tbody").select("tr");

        List<BonPosition> bonPositionList = new ArrayList<>();
        for (Element tr : trs) {
            BonPosition bonPosition = new BonPosition();

            Elements tds = tr.select("td");

            String name = tds.get(0).text().toString();
            BigDecimal quantity = new BigDecimal(tds.get(1).text().toString());
            BigDecimal price = new BigDecimal(tds.get(2).text().toString().replace(",", "."));

            System.out.println(name + " - " + quantity + " - " + price);

            bonPosition.setName(name);
            bonPosition.setQuantity(quantity);
            bonPosition.setPrice(price);

            bonPositionList.add(bonPosition);
        }

        Bon bon = new Bon();

        for (BonPosition bonPosition : bonPositionList) {
            System.out.println(bonPosition);
        }

        bon.setBonPositions(bonPositionList);

        // find meta data

        String[] dateTime = doc.select("div.row").select("div.col-sm-6").select("h4").first().text().split(" ");
        String pattern = "dd.MM.yyyy HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);

        Date shoppingDate = df.parse(dateTime[3] + " " + dateTime[4]);
        System.out.println(shoppingDate);

        String price = doc.select("div.row").select("ul").select("li").select("span.float-right").first().text().trim()
                .split(" ")[0].replace(",", ".");

        String payback = doc.select("div.row").select("ul").select("li").select("span.float-right").get(1).text().trim()
                .split(" ")[0].replace(",", ".");
        String paybackExtra = doc.select("div.row").select("ul").select("li").select("span.float-right").get(2).text()
                .trim().split(" ")[0].replace(",", ".");

        System.out.println(price);

        bon.setBonDate(shoppingDate);
        bon.setPayback(new BigDecimal(payback));
        bon.setPaybackExtra(new BigDecimal(paybackExtra));
        bon.setPrice(new BigDecimal(price));
        bon.setMessageId(messageId);

        em.persist(bon);
        em.getTransaction().commit();
    }

    /**
     * find URLs
     */
    public List<String> urlFinder(String text) {
        List<String> containedUrls = new ArrayList<String>();
        String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);

        while (urlMatcher.find()) {
            containedUrls.add(text.substring(urlMatcher.start(0), urlMatcher.end(0)));
        }

        return containedUrls;
    }
}