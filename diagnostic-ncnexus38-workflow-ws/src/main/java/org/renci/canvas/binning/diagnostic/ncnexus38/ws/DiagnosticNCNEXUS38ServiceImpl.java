package org.renci.canvas.binning.diagnostic.ncnexus38.ws;

import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.renci.canvas.binning.core.BinningExecutorService;
import org.renci.canvas.binning.core.diagnostic.DiagnosticBinningJobInfo;
import org.renci.canvas.binning.diagnostic.ncnexus38.executor.DiagnosticNCNEXUS38Task;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.DX;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.canvas.dao.clinbin.model.DiagnosticResultVersion;
import org.renci.canvas.dao.clinbin.model.DiagnosticStatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagnosticNCNEXUS38ServiceImpl implements DiagnosticNCNEXUS38Service {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosticNCNEXUS38ServiceImpl.class);

    private CANVASDAOBeanService daoBeanService;

    private BinningExecutorService binningExecutorService;

    public DiagnosticNCNEXUS38ServiceImpl() {
        super();
    }

    @Override
    public Response submit(DiagnosticBinningJobInfo info) {
        logger.debug("ENTERING submit(DiagnosticBinningJobInfo)");
        
        logger.info(info.toString());
        DiagnosticBinningJob binningJob = new DiagnosticBinningJob();
        
        try {
            binningJob.setStudy("NCNEXUS38");
            binningJob.setGender(info.getGender());
            binningJob.setParticipant(info.getParticipant());
            DiagnosticResultVersion diagnosticResultVersion = daoBeanService.getDiagnosticResultVersionDAO()
                    .findById(Integer.valueOf(info.getListVersion()));
            logger.info(diagnosticResultVersion.toString());
            binningJob.setDiagnosticResultVersion(diagnosticResultVersion);
            binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Requested"));
            DX dx = daoBeanService.getDXDAO().findById(Integer.valueOf(info.getDxId()));
            logger.info(dx.toString());
            binningJob.setDx(dx);
            List<DiagnosticBinningJob> foundBinningJobs = daoBeanService.getDiagnosticBinningJobDAO().findByExample(binningJob);
            if (CollectionUtils.isNotEmpty(foundBinningJobs)) {
                binningJob = foundBinningJobs.get(0);
                binningJob.setFailureMessage("");
                binningJob.setStop(null);
                daoBeanService.getDiagnosticBinningJobDAO().save(binningJob);
            } else {
                binningJob.setId(daoBeanService.getDiagnosticBinningJobDAO().save(binningJob));
            }
            info.setId(binningJob.getId());
            logger.info(binningJob.toString());

            binningExecutorService.getExecutor().submit(new DiagnosticNCNEXUS38Task(binningJob.getId()));

        } catch (CANVASDAOException e) {
            logger.error(e.getMessage(), e);
            return Response.serverError().build();
        }
        return Response.ok(info).build();
    }

    @Override
    public DiagnosticStatusType status(Integer binningJobId) {
        logger.debug("ENTERING status(Integer)");
        try {
            DiagnosticBinningJob foundBinningJob = daoBeanService.getDiagnosticBinningJobDAO().findById(binningJobId);
            logger.info(foundBinningJob.toString());
            return foundBinningJob.getStatus();
        } catch (CANVASDAOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public BinningExecutorService getBinningExecutorService() {
        return binningExecutorService;
    }

    public void setBinningExecutorService(BinningExecutorService binningExecutorService) {
        this.binningExecutorService = binningExecutorService;
    }

    public CANVASDAOBeanService getDaoBeanService() {
        return daoBeanService;
    }

    public void setDaoBeanService(CANVASDAOBeanService daoBeanService) {
        this.daoBeanService = daoBeanService;
    }

}
