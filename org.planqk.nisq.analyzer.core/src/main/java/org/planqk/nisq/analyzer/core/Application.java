package org.planqk.nisq.analyzer.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(scanBasePackages = "org.planqk.nisq.analyzer.*")
@EnableJpaRepositories("org.planqk.nisq.analyzer.*")
@EntityScan("org.planqk.nisq.analyzer.*")
public class Application extends SpringBootServletInitializer {

    final private static Logger LOG = LoggerFactory.getLogger(Application.class);

    public Application() {
        logReadyMessage();
    }

    private static void logReadyMessage() {
        final String readyMessage = "\n===================================================\n" +
                "NISQ Analyzer IS READY TO USE!\n" +
                "===================================================";
        LOG.info(readyMessage);
    }
}
