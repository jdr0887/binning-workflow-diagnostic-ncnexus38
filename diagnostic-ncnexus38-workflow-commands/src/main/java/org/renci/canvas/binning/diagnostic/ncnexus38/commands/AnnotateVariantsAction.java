package org.renci.canvas.binning.diagnostic.ncnexus38.commands;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.binning.diagnostic.ncnexus38.commons.AnnotateVariantsCallable;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.canvas.dao.refseq.model.Variants_78_4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "diagnostic-ncnexus38", name = "annotate-variants", description = "Annotate Variants")
@Service
public class AnnotateVariantsAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(AnnotateVariantsAction.class);

    @Reference
    private CANVASDAOBeanService daoBeanService;

    @Option(name = "--binningJobId", description = "DiagnosticBinningJob Identifier", required = true, multiValued = false)
    private Integer binningJobId;

    public AnnotateVariantsAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        DiagnosticBinningJob binningJob = daoBeanService.getDiagnosticBinningJobDAO().findById(binningJobId);
        logger.info(binningJob.toString());

        Executors.newSingleThreadExecutor().execute(() -> {

            try {

                binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Annotating variants"));
                daoBeanService.getDiagnosticBinningJobDAO().save(binningJob);

                List<Variants_78_4> variants = Executors.newSingleThreadExecutor()
                        .submit(new AnnotateVariantsCallable(daoBeanService, binningJob)).get();
                if (CollectionUtils.isNotEmpty(variants)) {
                    logger.info(String.format("saving %d Variants_61_2 instances", variants.size()));
                    for (Variants_78_4 variant : variants) {
                        logger.info(variant.toString());
                        daoBeanService.getVariants_78_4_DAO().save(variant);
                    }
                }

                binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Annotated variants"));
                daoBeanService.getDiagnosticBinningJobDAO().save(binningJob);

            } catch (Exception e) {
                try {
                    binningJob.setStop(new Date());
                    binningJob.setFailureMessage(e.getMessage());
                    binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Failed"));
                    daoBeanService.getDiagnosticBinningJobDAO().save(binningJob);
                } catch (CANVASDAOException e1) {
                    e1.printStackTrace();
                }
            }

        });

        return null;
    }

    public Integer getBinningJobId() {
        return binningJobId;
    }

    public void setBinningJobId(Integer binningJobId) {
        this.binningJobId = binningJobId;
    }

}
