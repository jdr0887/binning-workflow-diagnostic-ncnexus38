package org.renci.canvas.binning.diagnostic.ncnexus38.commons;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.renci.canvas.binning.core.BinningException;
import org.renci.canvas.binning.core.diagnostic.AbstractUpdateFrequenciesCallable;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.canvas.dao.clinbin.model.DiagnosticResultVersion;
import org.renci.canvas.dao.clinbin.model.MaxFrequency;
import org.renci.canvas.dao.clinbin.model.MaxFrequencyPK;
import org.renci.canvas.dao.clinbin.model.MaxFrequencySource;
import org.renci.canvas.dao.genome1k.model.IndelMaxFrequency;
import org.renci.canvas.dao.genome1k.model.IndelMaxFrequencyPK;
import org.renci.canvas.dao.genome1k.model.SNPPopulationMaxFrequency;
import org.renci.canvas.dao.genome1k.model.SNPPopulationMaxFrequencyPK;
import org.renci.canvas.dao.jpa.CANVASDAOManager;
import org.renci.canvas.dao.var.model.CanonicalAllele;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateFrequenciesCallable extends AbstractUpdateFrequenciesCallable {

    private static final Logger logger = LoggerFactory.getLogger(UpdateFrequenciesCallable.class);

    public UpdateFrequenciesCallable(CANVASDAOBeanService daoBean, DiagnosticBinningJob binningJob) {
        super(daoBean, binningJob);
    }

    @Override
    public Void call() throws BinningException {
        logger.debug("ENTERING call()");
        try {

            DiagnosticResultVersion diagnosticResultVersion = getBinningJob().getDiagnosticResultVersion();
            logger.info(diagnosticResultVersion.toString());

            MaxFrequencySource snpMaxFrequencySource = getDaoBean().getMaxFrequencySourceDAO().findById("snp");
            MaxFrequencySource indelMaxFrequencySource = getDaoBean().getMaxFrequencySourceDAO().findById("indel");
            MaxFrequencySource noneMaxFrequencySource = getDaoBean().getMaxFrequencySourceDAO().findById("none");

            List<LocatedVariant> locatedVariantList = getDaoBean().getLocatedVariantDAO()
                    .findByAssemblyId(getBinningJob().getAssembly().getId());

            if (CollectionUtils.isNotEmpty(locatedVariantList)) {
                logger.info(String.format("locatedVariantList.size(): %d", locatedVariantList.size()));

                ExecutorService es = Executors.newFixedThreadPool(6);

                for (LocatedVariant locatedVariant : locatedVariantList) {

                    List<CanonicalAllele> foundCanonicalAlleles = getDaoBean().getCanonicalAlleleDAO()
                            .findByLocatedVariantId(locatedVariant.getId());

                    if (CollectionUtils.isNotEmpty(foundCanonicalAlleles)) {
                        CanonicalAllele ca = foundCanonicalAlleles.get(0);

                        Optional<LocatedVariant> optionalLVFrom37 = ca.getLocatedVariants().stream()
                                .filter(a -> a.getGenomeRef().getId().equals(2)).findAny();
                        if (optionalLVFrom37.isPresent()) {
                            LocatedVariant lv = optionalLVFrom37.get();

                            es.submit(() -> {

                                logger.debug(locatedVariant.getGenomeRef().toString());
                                logger.debug(locatedVariant.toString());

                                logger.debug(lv.getGenomeRef().toString());
                                logger.debug(lv.toString());

                                try {
                                    SNPPopulationMaxFrequency snpPopulationMaxFrequency = getDaoBean().getSNPPopulationMaxFrequencyDAO()
                                            .findById(new SNPPopulationMaxFrequencyPK(lv.getId(),
                                                    diagnosticResultVersion.getGen1000SnpVersion()));

                                    if (snpPopulationMaxFrequency != null) {

                                        MaxFrequencyPK key = new MaxFrequencyPK(locatedVariant.getId(),
                                                diagnosticResultVersion.getGen1000SnpVersion());
                                        MaxFrequency maxFrequency = getDaoBean().getMaxFrequencyDAO().findById(key);
                                        if (maxFrequency == null) {
                                            maxFrequency = new MaxFrequency(key, snpMaxFrequencySource);
                                            maxFrequency.setMaxAlleleFreq(snpPopulationMaxFrequency.getMaxAlleleFrequency());
                                            maxFrequency.setLocatedVariant(locatedVariant);
                                            logger.info(maxFrequency.toString());
                                            getDaoBean().getMaxFrequencyDAO().save(maxFrequency);
                                        }
                                        return;
                                    }

                                    IndelMaxFrequency indelMaxFrequency = getDaoBean().getIndelMaxFrequencyDAO().findById(
                                            new IndelMaxFrequencyPK(lv.getId(), diagnosticResultVersion.getGen1000IndelVersion()));

                                    if (indelMaxFrequency != null) {
                                        MaxFrequencyPK key = new MaxFrequencyPK(locatedVariant.getId(),
                                                diagnosticResultVersion.getGen1000IndelVersion());
                                        MaxFrequency maxFrequency = getDaoBean().getMaxFrequencyDAO().findById(key);
                                        if (maxFrequency == null) {
                                            maxFrequency = new MaxFrequency(key, indelMaxFrequencySource);
                                            maxFrequency.setMaxAlleleFreq(indelMaxFrequency.getMaxAlleleFrequency());
                                            maxFrequency.setLocatedVariant(locatedVariant);
                                            logger.info(maxFrequency.toString());
                                            getDaoBean().getMaxFrequencyDAO().save(maxFrequency);
                                        }
                                        return;
                                    }

                                    if (snpPopulationMaxFrequency == null && indelMaxFrequency == null) {
                                        MaxFrequencyPK key = new MaxFrequencyPK(locatedVariant.getId(), 0);
                                        MaxFrequency maxFrequency = getDaoBean().getMaxFrequencyDAO().findById(key);
                                        if (maxFrequency == null) {
                                            maxFrequency = new MaxFrequency(key, noneMaxFrequencySource);
                                            maxFrequency.setMaxAlleleFreq(0D);
                                            maxFrequency.setLocatedVariant(locatedVariant);
                                            logger.info(maxFrequency.toString());
                                            getDaoBean().getMaxFrequencyDAO().save(maxFrequency);
                                        }
                                    }
                                } catch (CANVASDAOException e) {
                                    logger.error(e.getMessage(), e);
                                }

                            });

                        }

                    }

                }
                es.shutdown();
                if (!es.awaitTermination(1L, TimeUnit.HOURS)) {
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
            UpdateFrequenciesCallable callable = new UpdateFrequenciesCallable(daoMgr.getDAOBean(), binningJob);
            callable.call();
        } catch (CANVASDAOException | BinningException e) {
            e.printStackTrace();
        }
    }

}
