package org.renci.binning.diagnostic.ncnexus.commands;

import static org.renci.binning.core.Constants.BINNING_HOME;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.binning.dao.BinningDAOBeanService;
import org.renci.binning.dao.BinningDAOException;
import org.renci.binning.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.binning.diagnostic.ncnexus.commons.LoadCoverageCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "diagnostic-ncnexus", name = "load-coverage", description = "Load Coverage")
@Service
public class LoadCoverageAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(LoadCoverageAction.class);

    @Reference
    private BinningDAOBeanService binningDAOBeanService;

    @Option(name = "--binningJobId", description = "DiagnosticBinningJob Identifier", required = true, multiValued = false)
    private Integer binningJobId;

    public LoadCoverageAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        DiagnosticBinningJob binningJob = binningDAOBeanService.getDiagnosticBinningJobDAO().findById(binningJobId);
        logger.info(binningJob.toString());

        try {

            binningJob.setStatus(binningDAOBeanService.getDiagnosticStatusTypeDAO().findById("Coverage loading"));
            binningDAOBeanService.getDiagnosticBinningJobDAO().save(binningJob);

            String binningHome = System.getenv(BINNING_HOME);

            ExecutorService es = Executors.newSingleThreadExecutor();
            es.submit(new LoadCoverageCallable(binningDAOBeanService, binningJob, binningHome));
            es.shutdown();
            es.awaitTermination(1L, TimeUnit.DAYS);

            binningJob.setStatus(binningDAOBeanService.getDiagnosticStatusTypeDAO().findById("Coverage loaded"));
            binningDAOBeanService.getDiagnosticBinningJobDAO().save(binningJob);

        } catch (Exception e) {
            try {
                binningJob.setStop(new Date());
                binningJob.setFailureMessage(e.getMessage());
                binningJob.setStatus(binningDAOBeanService.getDiagnosticStatusTypeDAO().findById("Failed"));
                binningDAOBeanService.getDiagnosticBinningJobDAO().save(binningJob);
            } catch (BinningDAOException e1) {
                e1.printStackTrace();
            }
        }
        return null;

    }

}
