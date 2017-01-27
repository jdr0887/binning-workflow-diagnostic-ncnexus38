package org.renci.binning.diagnostic.ncnexus38.commons;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

public class Scratch {

    @Test
    public void scratch() throws IOException {
        String participantDir = Files.createTempDirectory(String.format("NCNEXUS38-%s-", "NCG_00020")).toFile().getAbsolutePath();
        System.out.println(participantDir);
    }
}
