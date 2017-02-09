package org.renci.binning.diagnostic.ncnexus38.commons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.renci.binning.core.BinningException;
import org.renci.binning.core.GATKDepthInterval;
import org.renci.binning.core.IRODSUtils;
import org.renci.binning.core.SAMToolsDepthInterval;
import org.renci.binning.core.diagnostic.AbstractLoadCoverageCallable;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
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
        avuMap.put("MaPSeqWorkflowName", "NCNEXUS38VariantCalling");
        avuMap.put("MaPSeqJobName", "SAMToolsDepth");
        avuMap.put("MaPSeqMimeType", "TEXT_PLAIN");
        String irodsFile = IRODSUtils.findFile(avuMap, ".depth.txt");
        logger.info("irodsFile = {}", irodsFile);
        Path participantPath = Paths.get(System.getProperty("karaf.data"), "tmp", "GS", participant);
        participantPath.toFile().mkdirs();
        File depthFile = IRODSUtils.getFile(irodsFile, participantPath.toString());
        logger.info("depthFile: {}", depthFile.getAbsolutePath());
        return depthFile;
    }

    @Override
    public void processIntervals(SortedSet<GATKDepthInterval> allIntervalSet, File depthFile, String participant, Integer listVersion)
            throws BinningException {
        logger.debug("ENTERING processIntervals(SortedSet<GATKDepthInterval>, File, String, Integer)");
        List<SAMToolsDepthInterval> samtoolsDepthIntervals = new ArrayList<>();

        try (FileReader fr = new FileReader(depthFile); BufferedReader br = new BufferedReader(fr)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                samtoolsDepthIntervals.add(new SAMToolsDepthInterval(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ExecutorService es = Executors.newFixedThreadPool(4);
            for (GATKDepthInterval gatkDepthInterval : allIntervalSet) {

                es.submit(() -> {
                    Map<Integer, Integer> percentageMap = new HashMap<Integer, Integer>();

                    percentageMap.put(1, 0);
                    percentageMap.put(2, 0);
                    percentageMap.put(5, 0);
                    percentageMap.put(8, 0);
                    percentageMap.put(10, 0);
                    percentageMap.put(15, 0);
                    percentageMap.put(20, 0);
                    percentageMap.put(30, 0);
                    percentageMap.put(50, 0);

                    int total = 0;
                    for (SAMToolsDepthInterval samtoolsDepthInterval : samtoolsDepthIntervals) {
                        if (!gatkDepthInterval.getContig().equals(samtoolsDepthInterval.getContig())) {
                            continue;
                        }
                        if (!gatkDepthInterval.getPositionRange().contains(samtoolsDepthInterval.getPosition())) {
                            continue;
                        }

                        total += samtoolsDepthInterval.getCoverage();

                        for (Integer key : percentageMap.keySet()) {
                            if (samtoolsDepthInterval.getCoverage() >= key) {
                                percentageMap.put(key, percentageMap.get(key) + 1);
                            }
                        }

                    }
                    gatkDepthInterval.setTotalCoverage(total);
                    gatkDepthInterval
                            .setAverageCoverage(Double.valueOf(1D * gatkDepthInterval.getTotalCoverage() / gatkDepthInterval.getLength()));
                    gatkDepthInterval.setSamplePercentAbove1(Double.valueOf(100D * percentageMap.get(1) / gatkDepthInterval.getLength()));
                    gatkDepthInterval.setSamplePercentAbove2(Double.valueOf(100D * percentageMap.get(2) / gatkDepthInterval.getLength()));
                    gatkDepthInterval.setSamplePercentAbove5(Double.valueOf(100D * percentageMap.get(5) / gatkDepthInterval.getLength()));
                    gatkDepthInterval.setSamplePercentAbove8(Double.valueOf(100D * percentageMap.get(8) / gatkDepthInterval.getLength()));
                    gatkDepthInterval.setSamplePercentAbove10(Double.valueOf(100D * percentageMap.get(10) / gatkDepthInterval.getLength()));
                    gatkDepthInterval.setSamplePercentAbove15(Double.valueOf(100D * percentageMap.get(15) / gatkDepthInterval.getLength()));
                    gatkDepthInterval.setSamplePercentAbove20(Double.valueOf(100D * percentageMap.get(20) / gatkDepthInterval.getLength()));
                    gatkDepthInterval.setSamplePercentAbove30(Double.valueOf(100D * percentageMap.get(30) / gatkDepthInterval.getLength()));
                    gatkDepthInterval.setSamplePercentAbove50(Double.valueOf(100D * percentageMap.get(50) / gatkDepthInterval.getLength()));

                });
            }
            es.shutdown();
            es.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

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
