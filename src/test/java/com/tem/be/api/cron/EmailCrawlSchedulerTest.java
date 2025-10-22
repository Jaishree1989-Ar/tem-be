package com.tem.be.api.cron;

import com.tem.be.api.service.EmailCrawlerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the {@link EmailCrawlScheduler}.
 * <p>
 * This class ensures that the scheduler correctly delegates the crawling task
 * to the {@link EmailCrawlerService}.
 */
@ExtendWith(MockitoExtension.class)
class EmailCrawlSchedulerTest {

    /**
     * Mocked service that the scheduler is expected to call.
     */
    @Mock
    private EmailCrawlerService emailCrawlerService;

    /**
     * The scheduler instance under test, with its mocked dependency injected.
     */
    @InjectMocks
    private EmailCrawlScheduler emailCrawlScheduler;

    /**
     * Tests that when the {@code triggerEmailCrawl} method is invoked, it makes a single
     * call to the {@code findPdfAttachments} method on the crawler service.
     */
    @Test
    @DisplayName("triggerEmailCrawl - Should call EmailCrawlerService.findPdfAttachments")
    void triggerEmailCrawl_shouldCallService() {
        // Act
        emailCrawlScheduler.triggerEmailCrawl();

        // Assert
        // Verify that the findPdfAttachments method on the service was called exactly once.
        verify(emailCrawlerService, times(1)).findPdfAttachments();
    }
}
