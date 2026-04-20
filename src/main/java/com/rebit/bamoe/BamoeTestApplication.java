package com.rebit.bamoe;

import org.kie.kogito.correlation.CompositeCorrelation;
import org.kie.kogito.correlation.Correlation;
import org.kie.kogito.correlation.CorrelationInstance;
import org.kie.kogito.correlation.CorrelationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.Optional;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.rebit.bamoe",
        "org.kie.kogito.app",
        "org.kie.kogito.services",
        "org.drools.bpmn2",
        "org.drools.project.model"
})
public class BamoeTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BamoeTestApplication.class, args);
    }

    /**
     * Kogito's generated process class requires a CorrelationService bean.
     * This no-op implementation is sufficient for basic human-task workflows
     * that don't use event-based correlation (message events, signals, etc.).
     */
    @Bean
    public CorrelationService correlationService() {
        return new CorrelationService() {
//            @Override
            public CorrelationInstance create(CompositeCorrelation correlation, String correlatedId) {
                return null;
            }

//            @Override
            public Optional<CorrelationInstance> find(CompositeCorrelation correlation) {
                return Optional.empty();
            }

            @Override
            public CorrelationInstance create(Correlation correlation, String s) {
                return null;
            }

            @Override
            public Optional<CorrelationInstance> find(Correlation correlation) {
                return Optional.empty();
            }

            @Override
            public Optional<CorrelationInstance> findByCorrelatedId(String correlatedId) {
                return Optional.empty();
            }

            @Override
            public void delete(Correlation correlation) {

            }

            //            @Override
            public void delete(CorrelationInstance correlationInstance) {
                // no-op
            }
        };
    }
}