package com.tem.be.api.cron;

import com.tem.be.api.service.EmailCrawlerService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Manages the scheduled and manual execution of the email crawling process.
 * <p>
 * This component serves a dual purpose:
 * As a scheduler, it automatically triggers the email crawl on a predefined cron schedule.
 * This is useful for testing, back-filling data, or re-running a failed job without waiting for the next schedule.
 */
@Log4j2
@Component
@RequestMapping("/cron") // Exposes endpoints under the /cron path
public class EmailCrawlScheduler {

    private final EmailCrawlerService emailCrawlerService;

    /**
     * Constructs the scheduler with the necessary service dependency.
     *
     * @param emailCrawlerService The service that performs the actual email crawling and processing logic.
     */
    @Autowired
    public EmailCrawlScheduler(EmailCrawlerService emailCrawlerService) {
        this.emailCrawlerService = emailCrawlerService;
    }

    /**
     * Triggers the email attachment processing job.
     *
     * <p><b>Scheduled Execution:</b></p>
     * This method is configured to run automatically based on the cron expression provided
     * in the {@code @Scheduled} annotation.
     * <p><b>Cron Expression Breakdown: {@code "0 0 2 1 * ?"}</b></p>
     * <ul>
     *   <li>{@code 0}   - Second (at the start of the minute)</li>
     *   <li>{@code 0}   - Minute (at the start of the hour)</li>
     *   <li>{@code 2}   - Hour (2 AM)</li>
     *   <li>{@code 1}   - Day of the month (the 1st)</li>
     *   <li>{@code *}   - Month (every month)</li>
     *   <li>{@code ?}   - Day of the week (any, as the day of the month is specified)</li>
     * </ul>
     * This results in the job running at <strong>2:00 AM on the 1st day of every month</strong>.
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @PostMapping("/email-crawl")
    public void triggerEmailCrawl() {
        log.info("SCHEDULER: Starting monthly email crawl job...");
        emailCrawlerService.findPdfAttachments();
        log.info("SCHEDULER: Monthly email crawl job finished successfully.");
    }
}
