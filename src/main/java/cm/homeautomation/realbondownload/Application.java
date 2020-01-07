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

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public void readMails(String mailServer, String mailAddress, String password) {
        Properties props = new Properties();
        try {
            // props.load(new FileInputStream(new File("C:\\smtp.properties")));
            Session session = Session.getDefaultInstance(props, null);

            Store store = session.getStore("imaps");
            store.connect("smtp.gmail.com", mailAddress, password);

            Folder inbox = store.getFolder("inbox");
            inbox.open(Folder.READ_ONLY);
            int messageCount = inbox.getMessageCount();

            System.out.println("Total Messages:- " + messageCount);

            Message[] messages = inbox.getMessages();
            System.out.println("------------------------------");
            for (int i = 1; i < 100; i++) {
                Message message = messages[messages.length - i];
                String messageFrom = message.getFrom()[0].toString();
                String messageSubject = message.getSubject();

                if ("PAYBACK Service <service@payback.de>".equals(messageFrom)
                        && "Ihr neuer Punktestand!".equals(messageSubject)) {

                    System.out.println("Mail : " + messageFrom + "- " + messageSubject);
                    List<String> urls = urlFinder(getTextFromMessage(message));
                    System.out.println(urls);
                    System.out.println(urls.get(0));

                    fetchBon(urls.get(0));
                }
            }
            inbox.close(true);
            store.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
            }
        }
        return result;
    }

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

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            String mailServer = args[0];
            String mailAddress = args[1];
            String mailPassword = args[2];

            boolean fetchBon = false;

            readMails(mailServer, mailAddress, mailPassword);

            // System.out.println("done parsing");
        };
    }

    private void fetchBon(String bonInitialUrl) throws ParseException {

        EntityManager em = EntityManagerService.getNewManager();
        em.getTransaction().begin();

        // System.out.println("Let's inspect the beans provided by Spring Boot:");

        /*
         * String[] beanNames = ctx.getBeanDefinitionNames(); Arrays.sort(beanNames);
         * for (String beanName : beanNames) { System.out.println(beanName); }
         */
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

        // System.out.println("new location: " + newLocation);
        // System.out.println("===================================");
        ResponseEntity<String> newResponse = restTemplate.postForEntity(newLocation, request, String.class);

        // System.out.println(response);
        // System.out.println("===================================");
        // System.out.println(newResponse);
        // System.out.println("===================================");

        String bonLocation1 = newResponse.getHeaders().get("Location").get(0);

        ResponseEntity<String> bonResponse1 = restTemplate.postForEntity(bonLocation1, request, String.class);

        // System.out.println("BON RESPOONSE 1 ===================================");
        // System.out.println(bonResponse1);
        // System.out.println("===================================");

        String bonLocation2 = urlFinder(bonResponse1.getBody().toString()).get(0).replaceAll("&amp;", "&")
                .replaceAll("%25253D", "=").replaceAll("%252526", "&").replaceAll("http://", "https://");
        // System.out.println("BON URL 2 ===================================");
        // System.out.println(bonLocation2);

        ResponseEntity<String> bonResponse2 = restTemplate.postForEntity(bonLocation2, request, String.class);

        // System.out.println("BON RESPOONSE 2 ===================================");
        System.out.println(bonResponse2.getBody().toString());
        // System.out.println("===================================");

        Document doc = Jsoup.parse(bonResponse2.getBody().toString());

        Elements trs = doc.select("table.-striped").select("tbody").select("tr");
        // System.out.println(trs.size());
        List<BonPosition> bonPositionList = new ArrayList<>();
        for (Element tr : trs) {
            BonPosition bonPosition = new BonPosition();

            Elements tds = tr.select("td");

            String name = tds.get(0).text().toString();
            BigDecimal quantity = new BigDecimal(tds.get(1).text().toString());
            // System.out.println("BigDecimal: "+tds.get(2).text().toString());
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

        em.persist(bon);
        em.getTransaction().commit();

        /*
         * <div class="col-sm-6"> <ul class="list -unstyled"> <li> <h4> Summe (inkl.
         * aller Rabatte):<span class="float-right"> 89,15 &euro;</span> </h4> </li>
         * 
         * <li> PAYBACK Punkte auf diesen Einkauf: <span class="float-right"> 44 °P
         * </span> </li> <li> PAYBACK Sonderpunkte: <span class="float-right"> 0 °P
         * </span> </li> </ul>
         * 
         */
    }

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