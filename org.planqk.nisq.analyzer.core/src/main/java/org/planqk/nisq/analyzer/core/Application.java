package org.planqk.nisq.analyzer.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@EnableAsync
@SpringBootApplication(scanBasePackages = "org.planqk.nisq.analyzer.*")
@EnableJpaRepositories("org.planqk.nisq.analyzer.*")
@EntityScan("org.planqk.nisq.analyzer.*")
@OpenAPIDefinition(info = @Info(title = "NISQ Analyzer API", version = "1.4.0", license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html"), contact = @Contact(url = "https://github.com/UST-QuAntiL/nisq-analyzer", name = "GitHub Repository")))
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

    /**
     * Launch the embedded Tomcat server.
     *
     * See `application.properties` for its configuration.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
