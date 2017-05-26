package org.renci.canvas.binning.diagnostic.ncnexus38.executor;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagnosticNCNEXUS38Task implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosticNCNEXUS38Task.class);

    private Integer binningJobId;

    public DiagnosticNCNEXUS38Task() {
        super();
    }

    public DiagnosticNCNEXUS38Task(Integer binningJobId) {
        super();
        this.binningJobId = binningJobId;
    }

    @Override
    public void run() {
        logger.debug("ENTERING run()");
        try {
            BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();

            ServiceReference<RuntimeService> runtimeServiceReference = bundleContext.getServiceReference(RuntimeService.class);
            RuntimeService runtimeService = bundleContext.getService(runtimeServiceReference);

            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("binningJobId", binningJobId);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ncnexus38_diagnostic_binning", variables);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public Integer getBinningJobId() {
        return binningJobId;
    }

    public void setBinningJobId(Integer binningJobId) {
        this.binningJobId = binningJobId;
    }

}
