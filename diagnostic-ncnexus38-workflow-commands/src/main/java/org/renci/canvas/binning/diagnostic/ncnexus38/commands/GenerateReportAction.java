package org.renci.canvas.binning.diagnostic.ncnexus38.commands;

import java.util.Date;
import java.util.concurrent.Executors;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.binning.diagnostic.ncnexus38.commons.GenerateReportCallable;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.canvas.dao.clinbin.model.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "diagnostic-ncnexus38", name = "generate-report", description = "Generate Report")
@Service
public class GenerateReportAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(GenerateReportAction.class);

    @Reference
    private CANVASDAOBeanService daoBeanService;

    @Option(name = "--binningJobId", description = "DiagnosticBinningJob Identifier", required = true, multiValued = false)
    private Integer binningJobId;

    public GenerateReportAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        DiagnosticBinningJob binningJob = daoBeanService.getDiagnosticBinningJobDAO().findById(binningJobId);
        logger.info(binningJob.toString());

        Executors.newSingleThreadExecutor().execute(() -> {
            long start = System.currentTimeMillis();

            try {

                binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Generating Report"));
                daoBeanService.getDiagnosticBinningJobDAO().save(binningJob);

                Report report = Executors.newSingleThreadExecutor().submit(new GenerateReportCallable(daoBeanService, binningJob))
                        .get();
                logger.info(report.toString());
                daoBeanService.getReportDAO().save(report);

                binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Generated Report"));
                daoBeanService.getDiagnosticBinningJobDAO().save(binningJob);

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                try {
                    binningJob.setStop(new Date());
                    binningJob.setFailureMessage(e.getMessage());
                    binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Failed"));
                    daoBeanService.getDiagnosticBinningJobDAO().save(binningJob);
                } catch (CANVASDAOException e1) {
                    logger.error(e1.getMessage(), e1);
                }
            }
            
            long end = System.currentTimeMillis();
            logger.info("total duration (seconds): {}", (end - start) / 1000);

        });

        return null;

    }

}
