package org.renci.canvas.binning.diagnostic.ncnexus38.commons;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.renci.canvas.binning.core.BinningException;
import org.renci.canvas.binning.core.IRODSUtils;
import org.renci.canvas.binning.core.diagnostic.AbstractLoadVCFCallable;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.canvas.dao.jpa.CANVASDAOManager;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.liftover.LiftOver;
import htsjdk.samtools.util.Interval;

public class LoadVCFCallable extends AbstractLoadVCFCallable {

    private static final Logger logger = LoggerFactory.getLogger(LoadVCFCallable.class);

    public LoadVCFCallable(CANVASDAOBeanService daoBean, DiagnosticBinningJob binningJob) {
        super(daoBean, binningJob);
    }

    @Override
    public String getLabName() {
        return "NCNEXUS38";
    }

    @Override
    public String getLibraryName() {
        return "FreeBayes";
    }

    @Override
    public String getStudyName() {
        return "NCNEXUS38";
    }

    @Override
    public Set<String> getExcludesFilter() {
        logger.debug("ENTERING getExcludesFilter()");
        Set<String> excludesFilter = new HashSet<>();
        excludesFilter.add("LowQual");
        return excludesFilter;
    }

    @Override
    public GenomeRef getGenomeRef() {
        GenomeRef genomeRef = null;
        try {
            genomeRef = getDaoBean().getGenomeRefDAO().findById(4);
        } catch (CANVASDAOException e) {
            e.printStackTrace();
        }
        return genomeRef;
    }

    @Override
    public LocatedVariant liftOver(LocatedVariant locatedVariant) throws BinningException {
        logger.debug("ENTERING liftOver(LocatedVariant)");
        LocatedVariant ret = null;
        try {
            File chainFile = new File(String.format("%s/liftOver", System.getProperty("karaf.data")), "hg38ToHg19.over.chain.gz");
            LiftOver liftOver = new LiftOver(chainFile);
            Interval interval = new Interval(String.format("chr%s", locatedVariant.getGenomeRefSeq().getContig()),
                    locatedVariant.getPosition(), locatedVariant.getEndPosition());
            Interval loInterval = liftOver.liftOver(interval);
            if (loInterval != null) {
                GenomeRef genomeRef = getDaoBean().getGenomeRefDAO().findById(2);
                logger.info("from {} to {}", locatedVariant.getGenomeRef().toString(), genomeRef.toString());
                List<GenomeRefSeq> genomeRefSeqList = getDaoBean().getGenomeRefSeqDAO().findByRefIdAndContigAndSeqTypeAndAccessionPrefix(
                        genomeRef.getId(), locatedVariant.getGenomeRefSeq().getContig(), "Chromosome", "NC_");
                if (CollectionUtils.isEmpty(genomeRefSeqList)) {
                    throw new BinningException("GenomeRefSeq not found");
                }
                ret = new LocatedVariant(genomeRef, genomeRefSeqList.get(0), loInterval.getStart(), loInterval.getEnd(),
                        locatedVariant.getVariantType(), locatedVariant.getRef(), locatedVariant.getSeq());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return ret;
    }

    @Override
    public File getVCF(String participant) throws BinningException {
        logger.debug("ENTERING getVCF(String)");
        Map<String, String> avuMap = new HashMap<String, String>();
        avuMap.put("ParticipantId", participant);
        avuMap.put("MaPSeqWorkflowName", "NCNEXUS38DX");
        avuMap.put("MaPSeqJobName", "FilterVariant");
        avuMap.put("MaPSeqMimeType", "TEXT_VCF");
        String irodsFile = IRODSUtils.findFile(avuMap);
        logger.info("irodsFile: {}", irodsFile);
        Path participantPath = Paths.get(System.getProperty("karaf.data"), "tmp", "NCNEXUS38", participant);
        participantPath.toFile().mkdirs();
        File vcfFile = IRODSUtils.getFile(irodsFile, participantPath.toString());
        logger.info("vcfFile: {}", vcfFile.getAbsolutePath());
        return vcfFile;
    }

    public static void main(String[] args) {
        try {
            CANVASDAOManager daoMgr = CANVASDAOManager.getInstance();
            DiagnosticBinningJob binningJob = daoMgr.getDAOBean().getDiagnosticBinningJobDAO().findById(4218);
            LoadVCFCallable callable = new LoadVCFCallable(daoMgr.getDAOBean(), binningJob);
            callable.call();
        } catch (CANVASDAOException | BinningException e) {
            e.printStackTrace();
        }
    }

}
