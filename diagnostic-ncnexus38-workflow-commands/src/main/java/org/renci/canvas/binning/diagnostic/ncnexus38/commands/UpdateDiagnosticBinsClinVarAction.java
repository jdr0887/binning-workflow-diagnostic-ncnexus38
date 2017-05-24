package org.renci.canvas.binning.diagnostic.ncnexus38.commands;

import java.util.Date;
import java.util.concurrent.Executors;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.binning.diagnostic.ncnexus38.commons.UpdateDiagnosticBinsClinVarCallable;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "diagnostic-ncnexus38", name = "update-diagnostic-bins-clinvar", description = "Update Diagnostic Bins ClinVar")
@Service
public class UpdateDiagnosticBinsClinVarAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDiagnosticBinsClinVarAction.class);

    @Reference
    private CANVASDAOBeanService daoBeanService;

    @Option(name = "--binningJobId", description = "DiagnosticBinningJob Identifier", required = true, multiValued = false)
    private Integer binningJobId;

    public UpdateDiagnosticBinsClinVarAction() {
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
                binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Updating dx bins"));
                daoBeanService.getDiagnosticBinningJobDAO().save(binningJob);

                Executors.newSingleThreadExecutor().submit(new UpdateDiagnosticBinsClinVarCallable(daoBeanService, binningJob)).get();

                binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Updated dx bins"));
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
