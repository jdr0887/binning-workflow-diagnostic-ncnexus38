package org.renci.binning.diagnostic.ncnexus38.commons;

import org.renci.binning.core.BinningException;
import org.renci.binning.core.grch37.diagnostic.AbstractUpdateDiagnosticBinsCallable;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.canvas.dao.jpa.CANVASDAOManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateDiagnosticBinsCallable extends AbstractUpdateDiagnosticBinsCallable {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDiagnosticBinsCallable.class);

    public UpdateDiagnosticBinsCallable(CANVASDAOBeanService daoBean, DiagnosticBinningJob binningJob) {
        super(daoBean, binningJob);
    }

    public static void main(String[] args) {
        try {
            CANVASDAOManager daoMgr = CANVASDAOManager.getInstance();
            DiagnosticBinningJob binningJob = daoMgr.getDAOBean().getDiagnosticBinningJobDAO().findById(4218);
            UpdateDiagnosticBinsCallable callable = new UpdateDiagnosticBinsCallable(daoMgr.getDAOBean(), binningJob);
            callable.call();
        } catch (CANVASDAOException | BinningException e) {
            e.printStackTrace();
        }
    }

}
