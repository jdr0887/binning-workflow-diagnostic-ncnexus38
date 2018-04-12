package org.renci.canvas.binning.diagnostic.ncnexus38.commons;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
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
import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_37_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.liftover.LiftOver;
import htsjdk.samtools.util.Interval;

public class LoadVCFCallable extends AbstractLoadVCFCallable {

    private static final Logger logger = LoggerFactory.getLogger(LoadVCFCallable.class);

    private GenomeRef genomeRef4LiftOver;

    private List<GenomeRefSeq> genomeRefSeqs4LiftOver;

    private LiftOver liftOver;

    public LoadVCFCallable(CANVASDAOBeanService daoBean, DiagnosticBinningJob binningJob) {
        super(daoBean, binningJob);
        File chainFile = new File(String.format("%s/liftOver", System.getProperty("karaf.data")), "hg38ToHg19.over.chain.gz");
        this.liftOver = new LiftOver(chainFile);
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
        return excludesFilter;
    }

    @Override
    public GenomeRef getDefaultGenomeRef() {
        GenomeRef genomeRef = null;
        try {
            genomeRef = getDaoBean().getGenomeRefDAO().findById(4);
        } catch (CANVASDAOException e) {
            logger.error(e.getMessage(), e);
        }
        return genomeRef;
    }

    @Override
    public GenomeRef getLiftOverGenomeRef() {
        GenomeRef genomeRef = null;
        try {
            genomeRef = getDaoBean().getGenomeRefDAO().findById(2);
        } catch (CANVASDAOException e) {
            logger.error(e.getMessage(), e);
        }
        return genomeRef;
    }

    @Override
    public LocatedVariant liftOver(LocatedVariant locatedVariant) throws BinningException {
        logger.debug("ENTERING liftOver(LocatedVariant)");
        LocatedVariant ret = null;
        try {
            Interval interval = new Interval(String.format("chr%s", locatedVariant.getGenomeRefSeq().getContig()),
                    locatedVariant.getPosition(), locatedVariant.getEndPosition());
            Interval loInterval = this.liftOver.liftOver(interval);
            if (loInterval != null) {

                if (interval.length() != loInterval.length()) {
                    return null;
                }

                if (this.genomeRef4LiftOver == null) {
                    this.genomeRef4LiftOver = getDaoBean().getGenomeRefDAO().findById(2);
                }

                if (this.genomeRefSeqs4LiftOver == null) {
                    this.genomeRefSeqs4LiftOver = getDaoBean().getGenomeRefSeqDAO().findByGenomeRefIdAndSeqType(genomeRef4LiftOver.getId(),
                            "Chromosome");
                }

                GenomeRefSeq liftOverGenomeRefSeq = this.genomeRefSeqs4LiftOver.stream()
                        .filter(a -> a.getContig().equals(locatedVariant.getGenomeRefSeq().getContig())).findFirst().orElse(null);
                if (liftOverGenomeRefSeq == null) {
                    throw new BinningException("GenomeRefSeq not found");
                }
                logger.info(liftOverGenomeRefSeq.toString());

                GeReSe4jBuild gereseq4jMgr = GeReSe4jBuild_37_3.getInstance();
                String referenceSequence = gereseq4jMgr.getRegion(liftOverGenomeRefSeq.getId(),
                        Range.between(loInterval.getStart(), loInterval.getEnd()), true);
                if (StringUtils.isNotEmpty(referenceSequence) && locatedVariant.getRef().equals(referenceSequence)) {
                    ret = new LocatedVariant(genomeRef4LiftOver, liftOverGenomeRefSeq, loInterval.getStart(), loInterval.getEnd(),
                            locatedVariant.getVariantType(), locatedVariant.getRef(), locatedVariant.getSeq());
                }

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
        avuMap.put("MaPSeqSystem", "prod");
        avuMap.put("MaPSeqWorkflowName", "NCNEXUS38MergeVC");
        avuMap.put("MaPSeqJobName", "GATKVariantAnnotator");
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
