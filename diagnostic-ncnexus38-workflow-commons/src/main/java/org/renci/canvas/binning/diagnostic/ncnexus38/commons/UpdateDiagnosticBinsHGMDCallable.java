package org.renci.canvas.binning.diagnostic.ncnexus38.commons;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.renci.canvas.binning.core.BinningException;
import org.renci.canvas.binning.core.grch37.diagnostic.AbstractUpdateDiagnosticBinsCallable;
import org.renci.canvas.binning.core.grch38.BinResultsFinalDiagnosticFactory;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.BinResultsFinalDiagnostic;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.canvas.dao.jpa.CANVASDAOManager;
import org.renci.canvas.dao.refseq.model.Variants_80_4;
import org.renci.canvas.dao.var.model.CanonicalAllele;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateDiagnosticBinsHGMDCallable implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDiagnosticBinsHGMDCallable.class);

    private CANVASDAOBeanService daoBean;

    private DiagnosticBinningJob binningJob;

    public UpdateDiagnosticBinsHGMDCallable(CANVASDAOBeanService daoBean, DiagnosticBinningJob binningJob) {
        super();
        this.daoBean = daoBean;
        this.binningJob = binningJob;
    }

    @Override
    public Void call() throws BinningException {
        logger.debug("ENTERING call()");

        try {

            BinResultsFinalDiagnosticFactory binResultsFinalDiagnosticFactory = BinResultsFinalDiagnosticFactory.getInstance(daoBean);

            logger.info("Deleting BinResultsFinalDiagnostic instances by assembly id = {}", binningJob.getAssembly().getId());
            daoBean.getBinResultsFinalDiagnosticDAO().deleteByAssemblyId(binningJob.getAssembly().getId());

            List<LocatedVariant> locatedVariantList = daoBean.getLocatedVariantDAO().findByAssemblyId(binningJob.getAssembly().getId());

            List<Variants_80_4> variants = new ArrayList<>();

            if (CollectionUtils.isNotEmpty(locatedVariantList)) {
                logger.info(String.format("locatedVariantList.size(): %d", locatedVariantList.size()));
                for (LocatedVariant locatedVariant : locatedVariantList) {
                    List<Variants_80_4> foundVariants = daoBean.getVariants_80_4_DAO().findByLocatedVariantId(locatedVariant.getId());
                    if (CollectionUtils.isNotEmpty(foundVariants)) {
                        variants.addAll(foundVariants);
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(variants)) {

                logger.info(String.format("variants.size(): %d", variants.size()));

                ExecutorService es = Executors.newFixedThreadPool(4);
                for (Variants_80_4 variant : variants) {

                    es.submit(() -> {
                        try {

                            List<CanonicalAllele> foundCanonicalAlleles = daoBean.getCanonicalAlleleDAO()
                                    .findByLocatedVariantId(variant.getLocatedVariant().getId());

                            if (CollectionUtils.isNotEmpty(foundCanonicalAlleles)) {
                                CanonicalAllele canonicalAllele = foundCanonicalAlleles.get(0);
                                Optional<LocatedVariant> optionalLocatedVariant = canonicalAllele.getLocatedVariants().stream()
                                        .filter(a -> a.getGenomeRef().getId().equals(2)).findAny();
                                if (optionalLocatedVariant.isPresent()) {

                                    // we done't have hgmd data for 38, get from 37
                                    LocatedVariant locatedVariant37 = optionalLocatedVariant.get();
                                    logger.info(locatedVariant37.toString());

                                    // hgmd - known pathogenic(1)
                                    BinResultsFinalDiagnostic binResultsFinalDiagnostic = binResultsFinalDiagnosticFactory
                                            .findHGMDKnownPathogenic(binningJob, variant, locatedVariant37);
                                    if (binResultsFinalDiagnostic != null) {
                                        List<BinResultsFinalDiagnostic> foundBinResultsFinalDiagnostics = daoBean
                                                .getBinResultsFinalDiagnosticDAO()
                                                .findByKeyAndHGMDDiseaseClassId(binResultsFinalDiagnostic.getId(), 1);
                                        if (CollectionUtils.isEmpty(foundBinResultsFinalDiagnostics)) {
                                            logger.info(binResultsFinalDiagnostic.toString());
                                            daoBean.getBinResultsFinalDiagnosticDAO().save(binResultsFinalDiagnostic);
                                        }
                                    }

                                    // hgmd - likely pathogenic(2)
                                    binResultsFinalDiagnostic = binResultsFinalDiagnosticFactory.findHGMDLikelyPathogenic(binningJob,
                                            variant, locatedVariant37);
                                    if (binResultsFinalDiagnostic != null) {
                                        List<BinResultsFinalDiagnostic> foundBinResultsFinalDiagnostics = daoBean
                                                .getBinResultsFinalDiagnosticDAO()
                                                .findByKeyAndHGMDDiseaseClassId(binResultsFinalDiagnostic.getId(), 2);
                                        if (CollectionUtils.isEmpty(foundBinResultsFinalDiagnostics)) {
                                            logger.info(binResultsFinalDiagnostic.toString());
                                            daoBean.getBinResultsFinalDiagnosticDAO().save(binResultsFinalDiagnostic);
                                        }
                                    }

                                    // hgmd - possibly pathogenic(3)
                                    binResultsFinalDiagnostic = binResultsFinalDiagnosticFactory.findHGMDPossiblyPathogenic(binningJob,
                                            variant, locatedVariant37);
                                    if (binResultsFinalDiagnostic != null) {
                                        List<BinResultsFinalDiagnostic> foundBinResultsFinalDiagnostics = daoBean
                                                .getBinResultsFinalDiagnosticDAO()
                                                .findByKeyAndHGMDDiseaseClassId(binResultsFinalDiagnostic.getId(), 3);
                                        if (CollectionUtils.isEmpty(foundBinResultsFinalDiagnostics)) {
                                            logger.info(binResultsFinalDiagnostic.toString());
                                            daoBean.getBinResultsFinalDiagnosticDAO().save(binResultsFinalDiagnostic);
                                        }
                                    }

                                    // hgmd - uncertain significance(4)
                                    binResultsFinalDiagnostic = binResultsFinalDiagnosticFactory.findHGMDUncertainSignificance(binningJob,
                                            variant, locatedVariant37);
                                    if (binResultsFinalDiagnostic != null) {
                                        List<BinResultsFinalDiagnostic> foundBinResultsFinalDiagnostics = daoBean
                                                .getBinResultsFinalDiagnosticDAO()
                                                .findByKeyAndHGMDDiseaseClassId(binResultsFinalDiagnostic.getId(), 4);
                                        if (CollectionUtils.isEmpty(foundBinResultsFinalDiagnostics)) {
                                            logger.info(binResultsFinalDiagnostic.toString());
                                            daoBean.getBinResultsFinalDiagnosticDAO().save(binResultsFinalDiagnostic);
                                        }
                                    }

                                    // hgmd - likely benign(5)
                                    binResultsFinalDiagnostic = binResultsFinalDiagnosticFactory.findHGMDLikelyBenign(binningJob, variant,
                                            locatedVariant37);
                                    if (binResultsFinalDiagnostic != null) {
                                        List<BinResultsFinalDiagnostic> foundBinResultsFinalDiagnostics = daoBean
                                                .getBinResultsFinalDiagnosticDAO()
                                                .findByKeyAndHGMDDiseaseClassId(binResultsFinalDiagnostic.getId(), 5);
                                        if (CollectionUtils.isEmpty(foundBinResultsFinalDiagnostics)) {
                                            logger.info(binResultsFinalDiagnostic.toString());
                                            daoBean.getBinResultsFinalDiagnosticDAO().save(binResultsFinalDiagnostic);
                                        }
                                    }

                                    // hgmd almost certainly benign(6)
                                    binResultsFinalDiagnostic = binResultsFinalDiagnosticFactory.findHGMDAlmostCertainlyBenign(binningJob,
                                            variant, locatedVariant37);
                                    if (binResultsFinalDiagnostic != null) {
                                        List<BinResultsFinalDiagnostic> foundBinResultsFinalDiagnostics = daoBean
                                                .getBinResultsFinalDiagnosticDAO()
                                                .findByKeyAndHGMDDiseaseClassId(binResultsFinalDiagnostic.getId(), 6);
                                        if (CollectionUtils.isEmpty(foundBinResultsFinalDiagnostics)) {
                                            logger.info(binResultsFinalDiagnostic.toString());
                                            daoBean.getBinResultsFinalDiagnosticDAO().save(binResultsFinalDiagnostic);
                                        }
                                    }

                                }
                            }

                        } catch (CANVASDAOException e) {
                            logger.error(e.getMessage(), e);
                        }

                    });
                }

                es.shutdown();
                if (!es.awaitTermination(4L, TimeUnit.HOURS)) {
                    es.shutdownNow();
                }

            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new BinningException(e);
        }

        return null;
    }

    public static void main(String[] args) {
        try {
            CANVASDAOManager daoMgr = CANVASDAOManager.getInstance();
            DiagnosticBinningJob binningJob = daoMgr.getDAOBean().getDiagnosticBinningJobDAO().findById(4218);
            UpdateDiagnosticBinsHGMDCallable callable = new UpdateDiagnosticBinsHGMDCallable(daoMgr.getDAOBean(), binningJob);
            callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
