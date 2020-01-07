package cm.homeautomation.realbondownload;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class MailCheckScheduler {

  private static final Logger log = LoggerFactory.getLogger(MailCheckScheduler.class);

  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

  @Autowired
  Application application;

  @Scheduled(initialDelay = 5000, fixedRate = 1000 * 60 * 10) // 1000ms * 60 * 10 => 10 minutes
  public void reportCurrentTime() {
    log.info("The time is now {}", dateFormat.format(new Date()));
    application.checkMails();
  }
}