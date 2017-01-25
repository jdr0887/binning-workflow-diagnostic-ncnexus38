package org.renci.binning.diagnostic.ncnexus.commands;

import java.util.Date;
import java.util.concurrent.Executors;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.binning.dao.BinningDAOBeanService;
import org.renci.binning.dao.BinningDAOException;
import org.renci.binning.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.binning.dao.clinbin.model.Report;
import org.renci.binning.diagnostic.ncnexus.commons.GenerateReportCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "diagnostic-ncnexus", name = "generate-report", description = "Generate Report")
@Service
public class GenerateReportAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(GenerateReportAction.class);

    @Reference
    private BinningDAOBeanService binningDAOBeanService;

    @Option(name = "--binningJobId", description = "DiagnosticBinningJob Identifier", required = true, multiValued = false)
    private Integer binningJobId;

    public GenerateReportAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        DiagnosticBinningJob binningJob = binningDAOBeanService.getDiagnosticBinningJobDAO().findById(binningJobId);
        logger.info(binningJob.toString());

        try {

            binningJob.setStatus(binningDAOBeanService.getDiagnosticStatusTypeDAO().findById("Generating Report"));
            binningDAOBeanService.getDiagnosticBinningJobDAO().save(binningJob);

            Report report = Executors.newSingleThreadExecutor().submit(new GenerateReportCallable(binningDAOBeanService, binningJob)).get();
            logger.info(report.toString());
            binningDAOBeanService.getReportDAO().save(report);

            binningJob.setStatus(binningDAOBeanService.getDiagnosticStatusTypeDAO().findById("Generated Report"));
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
