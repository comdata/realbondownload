package cm.homeautomation.realbondownload;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            // System.out.println("Let's inspect the beans provided by Spring Boot:");

            /*
             * String[] beanNames = ctx.getBeanDefinitionNames(); Arrays.sort(beanNames);
             * for (String beanName : beanNames) { System.out.println(beanName); }
             */
            String url = "https://trxmail1.payback.de/go/9p5oiaqtiws90vwgv3xvrofx36obln035wrs4ck4o6wu/18?t_id=1241337351";

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
            // System.out.println(bonResponse2.getBody().toString());
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

            for (BonPosition bonPosition : bonPositionList) {
                System.out.println(bonPosition);
            }

            // find meta data

            String[] dateTime = doc.select("div.row").select("div.col-sm-6").select("h4").first().text().split(" ");
            String pattern = "dd.MM.yyyy HH:mm:ss";
            DateFormat df = new SimpleDateFormat(pattern);

            Date shoppingDate = df.parse(dateTime[3] + " " + dateTime[4]);
            System.out.println(shoppingDate);

/*
 <div class="col-sm-6">
        <ul class="list -unstyled">
            <li>
                <h4>
                    Summe (inkl. aller Rabatte):<span class="float-right">
                    89,15 &euro;</span>
                </h4>
            </li>
            
            <li>
                PAYBACK Punkte auf diesen Einkauf:
                <span class="float-right">
                    44 °P
                </span>
            </li>
            <li>
                PAYBACK Sonderpunkte:
                <span class="float-right">
                    0 °P
                </span>
            </li>
        </ul>

*/

            // System.out.println("done parsing");
        };
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