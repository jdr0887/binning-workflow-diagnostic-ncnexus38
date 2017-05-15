package org.renci.canvas.binning.diagnostic.ncnexus38.commons;

import java.util.ArrayList;
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
import org.renci.canvas.dao.genome1k.model.SNPPopulationMaxFrequency;
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
    public List<MaxFrequency> call() throws BinningException {
        logger.debug("ENTERING call()");
        List<MaxFrequency> results = new ArrayList<>();

        try {

            DiagnosticResultVersion diagnosticResultVersion = getDaoBean().getDiagnosticResultVersionDAO()
                    .findById(getBinningJob().getListVersion());
            logger.info(diagnosticResultVersion.toString());

            MaxFrequencySource snpMaxFrequencySource = getDaoBean().getMaxFrequencySourceDAO().findById("snp");
            MaxFrequencySource indelMaxFrequencySource = getDaoBean().getMaxFrequencySourceDAO().findById("indel");
            MaxFrequencySource noneMaxFrequencySource = getDaoBean().getMaxFrequencySourceDAO().findById("none");

            List<LocatedVariant> locatedVariantList = getDaoBean().getLocatedVariantDAO()
                    .findByAssemblyId(getBinningJob().getAssembly().getId());

            if (CollectionUtils.isNotEmpty(locatedVariantList)) {
                logger.info(String.format("locatedVariantList.size(): %d", locatedVariantList.size()));

                ExecutorService es = Executors.newFixedThreadPool(2);

                for (LocatedVariant locatedVariant : locatedVariantList) {

                    logger.info(locatedVariant.toString());

                    List<CanonicalAllele> foundCanonicalAlleles = getDaoBean().getCanonicalAlleleDAO()
                            .findByLocatedVariantId(locatedVariant.getId());

                    if (CollectionUtils.isNotEmpty(foundCanonicalAlleles)) {
                        CanonicalAllele ca = foundCanonicalAlleles.get(0);

                        Optional<LocatedVariant> optionalLVFrom37 = ca.getLocatedVariants().stream()
                                .filter(a -> a.getGenomeRef().getId().equals(2)).findAny();
                        if (optionalLVFrom37.isPresent()) {
                            LocatedVariant lv = optionalLVFrom37.get();
                            logger.info(lv.toString());

                            es.submit(() -> {

                                try {
                                    List<SNPPopulationMaxFrequency> snpPopulationMaxFrequencyList = getDaoBean()
                                            .getSNPPopulationMaxFrequencyDAO()
                                            .findByLocatedVariantIdAndVersion(lv.getId(), diagnosticResultVersion.getGen1000SnpVersion());

                                    if (CollectionUtils.isNotEmpty(snpPopulationMaxFrequencyList)) {

                                        MaxFrequencyPK key = new MaxFrequencyPK(locatedVariant.getId(),
                                                diagnosticResultVersion.getGen1000SnpVersion());
                                        MaxFrequency maxFrequency = getDaoBean().getMaxFrequencyDAO().findById(key);
                                        if (maxFrequency != null) {
                                            // has already been created
                                            return;
                                        }
                                        maxFrequency = new MaxFrequency(key, snpMaxFrequencySource);
                                        maxFrequency.setMaxAlleleFreq(snpPopulationMaxFrequencyList.get(0).getMaxAlleleFrequency());
                                        maxFrequency.setLocatedVariant(locatedVariant);
                                        results.add(maxFrequency);
                                        return;
                                    }

                                    List<IndelMaxFrequency> indelMaxFrequencyList = getDaoBean().getIndelMaxFrequencyDAO()
                                            .findByLocatedVariantIdAndVersion(lv.getId(), diagnosticResultVersion.getGen1000IndelVersion());

                                    if (CollectionUtils.isNotEmpty(indelMaxFrequencyList)) {
                                        MaxFrequencyPK key = new MaxFrequencyPK(locatedVariant.getId(),
                                                diagnosticResultVersion.getGen1000IndelVersion());
                                        MaxFrequency maxFrequency = getDaoBean().getMaxFrequencyDAO().findById(key);
                                        if (maxFrequency != null) {
                                            // has already been created
                                            return;
                                        }
                                        maxFrequency = new MaxFrequency(key, indelMaxFrequencySource);
                                        maxFrequency.setMaxAlleleFreq(indelMaxFrequencyList.get(0).getMaxAlleleFrequency());
                                        maxFrequency.setLocatedVariant(locatedVariant);
                                        results.add(maxFrequency);
                                        return;
                                    }

                                    if (CollectionUtils.isEmpty(snpPopulationMaxFrequencyList)
                                            && CollectionUtils.isEmpty(indelMaxFrequencyList)) {
                                        MaxFrequencyPK key = new MaxFrequencyPK(locatedVariant.getId(), 0);
                                        MaxFrequency maxFrequency = getDaoBean().getMaxFrequencyDAO().findById(key);
                                        if (maxFrequency != null) {
                                            // has already been created
                                            return;
                                        }
                                        maxFrequency = new MaxFrequency(key, noneMaxFrequencySource);
                                        maxFrequency.setMaxAlleleFreq(0D);
                                        maxFrequency.setLocatedVariant(locatedVariant);
                                        results.add(maxFrequency);
                                    }
                                } catch (CANVASDAOException e) {
                                    e.printStackTrace();
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

        return results;
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
