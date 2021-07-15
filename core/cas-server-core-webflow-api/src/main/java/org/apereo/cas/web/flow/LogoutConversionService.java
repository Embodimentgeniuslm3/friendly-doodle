package org.apereo.cas.web.flow;

import org.springframework.binding.convert.service.DefaultConversionService;

/**
 * Special conversion service with a {@link CompositeFlowExecutionKeyConverter}.
 *
 * @author Jerome Leleu
 * @since 4.0.0
 */
public class LogoutConversionService extends DefaultConversionService {

    /**
     * Build a new conversion service with a {@link CompositeFlowExecutionKeyConverter}.
     */
    public LogoutConversionService() {
        addConverter(new CompositeFlowExecutionKeyConverter());
    }
}
