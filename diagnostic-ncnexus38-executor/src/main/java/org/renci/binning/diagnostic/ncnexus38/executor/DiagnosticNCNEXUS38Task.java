package org.renci.binning.diagnostic.ncnexus38.executor;

import static org.renci.binning.core.Constants.BINNING_HOME;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
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

            ServiceReference<RepositoryService> repositoryServiceReference = bundleContext.getServiceReference(RepositoryService.class);
            RepositoryService repositoryService = bundleContext.getService(repositoryServiceReference);

            ServiceReference<RuntimeService> runtimeServiceReference = bundleContext.getServiceReference(RuntimeService.class);
            RuntimeService runtimeService = bundleContext.getService(runtimeServiceReference);

            repositoryService.createDeployment().addClasspathResource("org/renci/binning/diagnostic/ncnexus/executor/ncnexus.bpmn20.xml").deploy();

            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("binningJobId", binningJobId);
            variables.put("irods.home", "/projects/mapseq/apps/irods-4.2.0/icommands");
            // variables.put("process.data.dir", "/opt/Bin2/process_data");

            String binningHome = System.getenv(BINNING_HOME);
            variables.put(BINNING_HOME, binningHome);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ncnexus_diagnostic_binning", variables);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Integer getBinningJobId() {
        return binningJobId;
    }

    public void setBinningJobId(Integer binningJobId) {
        this.binningJobId = binningJobId;
    }

}
