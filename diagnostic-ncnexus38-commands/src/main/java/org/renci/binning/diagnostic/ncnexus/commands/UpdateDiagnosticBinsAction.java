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
import org.renci.binning.diagnostic.ncnexus.commons.UpdateDiagnosticBinsCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "diagnostic-ncnexus", name = "update-diagnostic-bins", description = "Update Diagnostic Bins")
@Service
public class UpdateDiagnosticBinsAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDiagnosticBinsAction.class);

    @Reference
    private BinningDAOBeanService binningDAOBeanService;

    @Option(name = "--binningJobId", description = "DiagnosticBinningJob Identifier", required = true, multiValued = false)
    private Integer binningJobId;

    public UpdateDiagnosticBinsAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        DiagnosticBinningJob binningJob = binningDAOBeanService.getDiagnosticBinningJobDAO().findById(binningJobId);
        logger.info(binningJob.toString());

        try {
            binningJob.setStatus(binningDAOBeanService.getDiagnosticStatusTypeDAO().findById("Updating dx bins"));
            binningDAOBeanService.getDiagnosticBinningJobDAO().save(binningJob);

            Executors.newSingleThreadExecutor().submit(new UpdateDiagnosticBinsCallable(binningDAOBeanService, binningJob)).get();

            binningJob.setStatus(binningDAOBeanService.getDiagnosticStatusTypeDAO().findById("Updated dx bins"));
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
