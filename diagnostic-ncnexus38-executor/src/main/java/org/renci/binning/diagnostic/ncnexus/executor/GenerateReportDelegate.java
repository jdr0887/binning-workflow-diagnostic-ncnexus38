package org.renci.binning.diagnostic.ncnexus.executor;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.renci.binning.dao.BinningDAOBeanService;
import org.renci.binning.dao.BinningDAOException;
import org.renci.binning.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.binning.dao.clinbin.model.Report;
import org.renci.binning.diagnostic.ncnexus.commons.GenerateReportCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateReportDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(GenerateReportDelegate.class);

    public GenerateReportDelegate() {
        super();
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.debug("ENTERING execute(DelegateExecution)");

        Map<String, Object> variables = execution.getVariables();

        BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<BinningDAOBeanService> daoBeanServiceReference = bundleContext.getServiceReference(BinningDAOBeanService.class);
        BinningDAOBeanService daoBean = bundleContext.getService(daoBeanServiceReference);

        Integer binningJobId = null;
        Object o = variables.get("binningJobId");
        if (o != null && o instanceof Integer) {
            binningJobId = (Integer) o;
        }

        try {
            DiagnosticBinningJob binningJob = daoBean.getDiagnosticBinningJobDAO().findById(binningJobId);
            binningJob.setStatus(daoBean.getDiagnosticStatusTypeDAO().findById("Generating Report"));
            daoBean.getDiagnosticBinningJobDAO().save(binningJob);
            logger.info(binningJob.toString());

            Report report = Executors.newSingleThreadExecutor().submit(new GenerateReportCallable(daoBean, binningJob)).get();
            logger.info(report.toString());
            daoBean.getReportDAO().save(report);

            binningJob = daoBean.getDiagnosticBinningJobDAO().findById(binningJobId);
            binningJob.setStatus(daoBean.getDiagnosticStatusTypeDAO().findById("Generated Report"));
            daoBean.getDiagnosticBinningJobDAO().save(binningJob);
            logger.info(binningJob.toString());

        } catch (Exception e) {
            try {
                DiagnosticBinningJob binningJob = daoBean.getDiagnosticBinningJobDAO().findById(binningJobId);
                binningJob.setStop(new Date());
                binningJob.setFailureMessage(e.getMessage());
                binningJob.setStatus(daoBean.getDiagnosticStatusTypeDAO().findById("Failed"));
                daoBean.getDiagnosticBinningJobDAO().save(binningJob);
                logger.info(binningJob.toString());
            } catch (BinningDAOException e1) {
                e1.printStackTrace();
            }
        }

    }

}
