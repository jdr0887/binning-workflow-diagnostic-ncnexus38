package org.renci.canvas.binning.diagnostic.ncnexus38.commons;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.renci.canvas.binning.core.BinningException;
import org.renci.canvas.binning.core.GATKDepthInterval;
import org.renci.canvas.binning.core.IRODSUtils;
import org.renci.canvas.binning.core.diagnostic.AbstractLoadCoverageCallable;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.DXCoverage;
import org.renci.canvas.dao.clinbin.model.DXCoveragePK;
import org.renci.canvas.dao.clinbin.model.DXExons;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.canvas.dao.jpa.CANVASDAOManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadCoverageCallable extends AbstractLoadCoverageCallable {

    private static final Logger logger = LoggerFactory.getLogger(LoadCoverageCallable.class);

    public LoadCoverageCallable(CANVASDAOBeanService daoBean, DiagnosticBinningJob binningJob) {
        super(daoBean, binningJob);
    }

    @Override
    public File getAllIntervalsFile(Integer listVersion) {
        logger.debug("ENTERING getAllIntervalsFile(Integer)");
        String binningIntervalsHome = System.getenv("BINNING_INTERVALS_HOME");
        File allIntervalsFile = new File(String.format("%s/NCNEXUS38/all/allintervals.v%d.txt", binningIntervalsHome, listVersion));
        logger.info("all intervals file: {}", allIntervalsFile.getAbsolutePath());
        return allIntervalsFile;
    }

    @Override
    public File getDepthFile(String participant, Integer listVersion) throws BinningException {
        logger.debug("ENTERING getDepthFile(String, Integer)");
        Map<String, String> avuMap = new HashMap<String, String>();
        avuMap.put("ParticipantId", participant);
        avuMap.put("DxVersion", listVersion.toString());
        avuMap.put("MaPSeqWorkflowName", "NCNEXUS38DX");
        avuMap.put("MaPSeqJobName", "SAMToolsDepthToGATKDOCFormatConverter");
        avuMap.put("MaPSeqMimeType", "TEXT_PLAIN");
        String irodsFile = IRODSUtils.findFile(avuMap, String.format(".depth.v%s.txt", listVersion));
        logger.info("irodsFile = {}", irodsFile);
        Path participantPath = Paths.get(System.getProperty("karaf.data"), "tmp", "NCNEXUS38", participant);
        participantPath.toFile().mkdirs();
        File depthFile = IRODSUtils.getFile(irodsFile, participantPath.toString());
        logger.info("depthFile: {}", depthFile.getAbsolutePath());
        return depthFile;
    }

    @Override
    public Void call() throws BinningException {
        logger.debug("ENTERING call()");

        try {

            SortedSet<GATKDepthInterval> allIntervalSet = new TreeSet<GATKDepthInterval>();

            File depthFile = getDepthFile(getBinningJob().getParticipant(), getBinningJob().getDiagnosticResultVersion().getId());
            try (Stream<String> stream = Files.lines(depthFile.toPath())) {
                stream.forEach(a -> {
                    if (!a.startsWith("Target")) {
                        allIntervalSet.add(new GATKDepthInterval(a));
                    }
                });
            }

            // load exon coverage
            for (GATKDepthInterval interval : allIntervalSet) {
                logger.debug(interval.toString());
                String chromosome = interval.getContig();
                Integer start = interval.getStartPosition();
                Integer end = interval.getEndPosition();
                if (end == null) {
                    end = start;
                }

                List<DXExons> dxExonList = getDaoBean().getDXExonsDAO().findByListVersionAndChromosomeAndRange(
                        getBinningJob().getDiagnosticResultVersion().getId(), chromosome, start, end);
                if (CollectionUtils.isNotEmpty(dxExonList)) {

                    logger.debug("dxExonList.size(): {}", dxExonList.size());

                    ExecutorService es = Executors.newFixedThreadPool(4);

                    for (DXExons dxExon : dxExonList) {
                        es.submit(() -> {

                            try {
                                logger.debug(dxExon.toString());

                                DXCoveragePK key = new DXCoveragePK(dxExon.getId(), getBinningJob().getParticipant());
                                DXCoverage dxCoverage = getDaoBean().getDXCoverageDAO().findById(key);
                                if (dxCoverage == null) {
                                    dxCoverage = new DXCoverage(key);
                                }

                                dxCoverage.setExon(dxExon);
                                dxCoverage.setFractionGreaterThan1(interval.getSamplePercentAbove1() * 0.01);
                                dxCoverage.setFractionGreaterThan2(interval.getSamplePercentAbove2() * 0.01);
                                dxCoverage.setFractionGreaterThan5(interval.getSamplePercentAbove5() * 0.01);
                                dxCoverage.setFractionGreaterThan8(interval.getSamplePercentAbove8() * 0.01);
                                dxCoverage.setFractionGreaterThan10(interval.getSamplePercentAbove10() * 0.01);
                                dxCoverage.setFractionGreaterThan15(interval.getSamplePercentAbove15() * 0.01);
                                dxCoverage.setFractionGreaterThan20(interval.getSamplePercentAbove20() * 0.01);
                                dxCoverage.setFractionGreaterThan30(interval.getSamplePercentAbove30() * 0.01);
                                dxCoverage.setFractionGreaterThan50(interval.getSamplePercentAbove50() * 0.01);
                                logger.debug(dxCoverage.toString());
                                getDaoBean().getDXCoverageDAO().save(dxCoverage);
                            } catch (CANVASDAOException e) {
                                logger.error(e.getMessage(), e);
                            }

                        });
                    }
                    es.shutdown();
                    if (!es.awaitTermination(1L, TimeUnit.HOURS)) {
                        es.shutdownNow();
                    }
                }

            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new BinningException(e);
        }
        return null;
    }

    @Override
    public void processIntervals(SortedSet<GATKDepthInterval> allIntervalSet, File depthFile, String participant, Integer listVersion)
            throws BinningException {
        logger.debug("ENTERING processIntervals(SortedSet<GATKDepthInterval>, File, String, Integer)");
        // this has been done in the mapseq
    }

    public static void main(String[] args) {
        try {
            CANVASDAOManager daoMgr = CANVASDAOManager.getInstance();
            DiagnosticBinningJob binningJob = daoMgr.getDAOBean().getDiagnosticBinningJobDAO().findById(4218);
            LoadCoverageCallable callable = new LoadCoverageCallable(daoMgr.getDAOBean(), binningJob);
            callable.call();
        } catch (CANVASDAOException | BinningException e) {
            e.printStackTrace();
        }
    }

}
