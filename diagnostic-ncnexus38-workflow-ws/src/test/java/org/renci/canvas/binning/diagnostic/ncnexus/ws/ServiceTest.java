package org.renci.canvas.binning.diagnostic.ncnexus.ws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Test;
import org.renci.canvas.binning.core.diagnostic.DiagnosticBinningJobInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

public class ServiceTest {

    @Test
    public void testSubmit() {

        List<Object> providers = new ArrayList<>();
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
        mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
        provider.setMapper(mapper);
        providers.add(provider);

        // new DiagnosticBinningJobInfo("NCX_00002", "F", 53, 48),
        // List<DiagnosticBinningJobInfo> jobs = Arrays.asList(new DiagnosticBinningJobInfo("NCX_00004", "F", 52, 48),
        // new DiagnosticBinningJobInfo("NCX_00004", "F", 53, 48), new DiagnosticBinningJobInfo("NCX_00002", "F", 52, 48),
        // new DiagnosticBinningJobInfo("NCX_00075", "E", 53, 48), new DiagnosticBinningJobInfo("NCX_00082", "M", 53, 48),
        // new DiagnosticBinningJobInfo("NCX_00103", "F", 53, 48), new DiagnosticBinningJobInfo("NCX_00082", "M", 51, 48),
        // new DiagnosticBinningJobInfo("NCX_00136", "M", 51, 48), new DiagnosticBinningJobInfo("NCX_00092", "F", 53, 48),
        // new DiagnosticBinningJobInfo("NCX_00197", "M", 53, 48), new DiagnosticBinningJobInfo("NCX_00110", "E", 53, 48),
        // new DiagnosticBinningJobInfo("NCX_00092", "F", 51, 48), new DiagnosticBinningJobInfo("NCX_00103", "F", 51, 48),
        // new DiagnosticBinningJobInfo("NCX_00197", "M", 52, 48), new DiagnosticBinningJobInfo("NCX_00136", "M", 53, 48));

        String restServiceURL = String.format("http://%1$s:%2$d/cxf/%3$s/%3$sService", "152.54.3.113", 8181, "DiagnosticNCNEXUS38");

        WebClient client = WebClient.create(restServiceURL, providers).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .path("submit");

        // 5000 NCX_00004 NCNEXUS38 F 53 48 36351
        // 5001 NCX_00004 NCNEXUS38 F 52 48 36352
        // 5002 NCX_00002 NCNEXUS38 F 53 75 36353

        // List<DiagnosticBinningJobInfo> jobs = Arrays.asList(new DiagnosticBinningJobInfo("NCX_00004", "F", 53, 77),
        // new DiagnosticBinningJobInfo("NCX_00004", "F", 52, 77));

        // List<DiagnosticBinningJobInfo> jobs = Arrays.asList(new DiagnosticBinningJobInfo("NCX_00057", "F", 52, 77),
        // new DiagnosticBinningJobInfo("NCX_00063", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00063", "M", 52, 77),
        // new DiagnosticBinningJobInfo("NCX_00071", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00071", "F", 52, 77),
        // new DiagnosticBinningJobInfo("NCX_00118", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00118", "M", 51, 77),
        // new DiagnosticBinningJobInfo("NCX_00129", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00129", "F", 51, 77),
        // new DiagnosticBinningJobInfo("NCX_00130", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00130", "F", 51, 77),
        // new DiagnosticBinningJobInfo("NCX_00149", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00149", "F", 51, 77),
        // new DiagnosticBinningJobInfo("NCX_00150", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00150", "F", 51, 77),
        // new DiagnosticBinningJobInfo("NCX_00172", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00172", "F", 51, 77),
        // new DiagnosticBinningJobInfo("NCX_00173", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00173", "M", 51, 77),
        // new DiagnosticBinningJobInfo("NCX_00199", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00199", "M", 52, 77),
        // new DiagnosticBinningJobInfo("NCX_00210", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00210", "M", 51, 77),
        // new DiagnosticBinningJobInfo("NCX_00182", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00183", "M", 53, 77),
        // new DiagnosticBinningJobInfo("NCX_00188", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00193", "F", 53, 77),
        // new DiagnosticBinningJobInfo("NCX_00002", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00002", "F", 52, 77),
        // new DiagnosticBinningJobInfo("NCX_00004", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00004", "F", 52, 77),
        // new DiagnosticBinningJobInfo("NCX_00020", "M", 8, 77), new DiagnosticBinningJobInfo("NCX_00020", "M", 53, 77),
        // new DiagnosticBinningJobInfo("NCX_00020", "M", 49, 77), new DiagnosticBinningJobInfo("NCX_00020", "M", 50, 77),
        // new DiagnosticBinningJobInfo("NCX_00030", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00030", "M", 52, 77),
        // new DiagnosticBinningJobInfo("NCX_00067", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00067", "M", 52, 77),
        // new DiagnosticBinningJobInfo("NCX_00090", "F", 51, 77), new DiagnosticBinningJobInfo("NCX_00090", "F", 53, 77),
        // new DiagnosticBinningJobInfo("NCX_00090", "F", 52, 77), new DiagnosticBinningJobInfo("NCX_00100", "F", 53, 77),
        // new DiagnosticBinningJobInfo("NCX_00100", "F", 51, 77), new DiagnosticBinningJobInfo("NCX_00101", "F", 53, 77),
        // new DiagnosticBinningJobInfo("NCX_00101", "F", 51, 77));

        List<DiagnosticBinningJobInfo> jobs = Arrays.asList(new DiagnosticBinningJobInfo("NCX_00074", "F", 53, 77),
                new DiagnosticBinningJobInfo("NCX_00123", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00134", "M", 53, 77),
                new DiagnosticBinningJobInfo("NCX_00139", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00141", "M", 53, 77),
                new DiagnosticBinningJobInfo("NCX_00160", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00013", "F", 53, 77),
                new DiagnosticBinningJobInfo("NCX_00013", "F", 52, 77), new DiagnosticBinningJobInfo("NCX_00057", "F", 53, 77),
                new DiagnosticBinningJobInfo("NCX_00029", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00075", "F", 53, 77),
                new DiagnosticBinningJobInfo("NCX_00082", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00082", "M", 51, 77),
                new DiagnosticBinningJobInfo("NCX_00092", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00092", "F", 51, 77),
                new DiagnosticBinningJobInfo("NCX_00103", "F", 53, 77), new DiagnosticBinningJobInfo("NCX_00103", "F", 51, 77),
                new DiagnosticBinningJobInfo("NCX_00136", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00136", "M", 51, 77),
                new DiagnosticBinningJobInfo("NCX_00197", "M", 53, 77), new DiagnosticBinningJobInfo("NCX_00197", "M", 52, 77));

        // DiagnosticBinningJobInfo info = new DiagnosticBinningJobInfo("NCX_00002", "F", 53, 76);
        // Response response = client.path("submit").post(info);
        // String id = response.readEntity(String.class);
        // System.out.println(id);

        for (DiagnosticBinningJobInfo job : jobs) {
            Response response = client.post(job);
            String id = response.readEntity(String.class);
            System.out.println(id);
        }
    }

    @Test
    public void scratch() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
            mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
            DiagnosticBinningJobInfo info = new DiagnosticBinningJobInfo("jdr-test", "M", 46, 40);
            String jsonInString = mapper.writeValueAsString(info);
            System.out.println(jsonInString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

}
