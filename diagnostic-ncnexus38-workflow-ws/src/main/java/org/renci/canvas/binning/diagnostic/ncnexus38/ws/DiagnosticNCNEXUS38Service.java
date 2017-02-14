package org.renci.canvas.binning.diagnostic.ncnexus38.ws;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.renci.canvas.binning.core.diagnostic.DiagnosticBinningJobInfo;
import org.renci.canvas.dao.clinbin.model.DiagnosticStatusType;

@Path("/DiagnosticNCNEXUS38Service/")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface DiagnosticNCNEXUS38Service {

    @POST
    @Path("/submit")
    public Response submit(DiagnosticBinningJobInfo info);

    @GET
    @Path("/status/{binningJobId}")
    public DiagnosticStatusType status(@PathParam("binningJobId") Integer binningJobId);

}
