package org.renci.canvas.binning.diagnostic.ncnexus38.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.binning.core.BinningExecutorService;
import org.renci.canvas.binning.diagnostic.ncnexus38.executor.DiagnosticNCNEXUS38Task;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "diagnostic-ncnexus38", name = "restart-binning-job", description = "Restart Binning Job")
@Service
public class RestartBinningJobAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(RestartBinningJobAction.class);

    @Reference
    private CANVASDAOBeanService daoBeanService;

    @Reference
    private BinningExecutorService binningExecutorService;

    @Option(name = "--binningJobId", description = "Binning Job ID", required = true, multiValued = false)
    private Integer binningJobId;

    public RestartBinningJobAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        DiagnosticBinningJob binningJob = daoBeanService.getDiagnosticBinningJobDAO().findById(binningJobId);
        binningJob.setFailureMessage("");
        binningJob.setStop(null);
        binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Requested"));
        daoBeanService.getDiagnosticBinningJobDAO().save(binningJob);

        Runnable runnable2Remove = binningExecutorService.getExecutor().getQueue().stream().filter(a -> {
            if (a instanceof DiagnosticNCNEXUS38Task) {
                DiagnosticNCNEXUS38Task b = (DiagnosticNCNEXUS38Task) a;
                if (binningJob.getId().equals(b.getBinningJobId())) {
                    return true;
                }
            }
            return false;
        }).findFirst().orElse(null);

        if (runnable2Remove != null) {
            binningExecutorService.getExecutor().remove(runnable2Remove);
        }

        binningExecutorService.getExecutor().submit(new DiagnosticNCNEXUS38Task(binningJob.getId()));

        return binningJob.toString();
    }

    public Integer getBinningJobId() {
        return binningJobId;
    }

    public void setBinningJobId(Integer binningJobId) {
        this.binningJobId = binningJobId;
    }

}
