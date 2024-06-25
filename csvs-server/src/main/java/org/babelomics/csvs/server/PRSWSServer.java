package org.babelomics.csvs.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.models.DiseaseGroup;
import org.babelomics.csvs.lib.models.Pgs;
import org.babelomics.csvs.lib.models.prs.PgsGraphic;
import org.babelomics.csvs.lib.ws.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Path("/prs")
@Api(value = "prs", description = "Polygenic risk score")
@Produces(MediaType.APPLICATION_JSON)
public class PRSWSServer extends CSVSWSServer {

    public PRSWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
            throws IOException {
        super(version, uriInfo, hsr);
    }


    private void initString(String names, List<String> nameList) {
        if (names.length() > 0){
            String[] namesSplits = StringUtils.split(names, ",");
            for (String n: namesSplits){
                nameList.add(n);
            }
        }
    }

    @GET
    @Path("/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get prs")
    public Response getPrs(
            @ApiParam(value = "searchPrs") @QueryParam("searchPrs") @DefaultValue("") String searchPrs,
            @ApiParam(value = "adSources") @QueryParam("adSources") @DefaultValue("") String adSources,
            @ApiParam(value = "adScores") @QueryParam("adScores") @DefaultValue("") String adScores,
            @ApiParam(value = "adListPgs") @QueryParam("adListPgs") @DefaultValue("") String adListPgs,
            @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("10") int limit,
            @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip,
            @ApiParam(value = "skipCount") @QueryParam("skipCount") @DefaultValue("false") boolean skipCount
    ) {

        List<String> searchPrsList = new ArrayList<>();
        List<String> adSourcesList = new ArrayList<>();
        List<String> adScoresList = new ArrayList<>();
        List<String> adLisPgsList = new ArrayList<>();

        initString(searchPrs, searchPrsList);
        initString(adSources, adSourcesList);
        initString(adScores, adScoresList);
        initString(adListPgs, adLisPgsList);

        MutableLong count = new MutableLong(-1);

        Iterable<Pgs> prs = null;

        try {
            prs = qm.getPRS( searchPrsList, adSourcesList, adScoresList,adLisPgsList, skip, limit, skipCount, count);
        } catch (Exception e1){
            return  createErrorResponse(e1);
        }

        QueryResponse qr = createQueryResponse(prs);
        qr.setNumTotalResults(count.getValue());

        qr.addQueryOption("searchPrs", searchPrs);
        qr.addQueryOption("adSources", adSources);
        qr.addQueryOption("adScores", adScores);
        qr.addQueryOption("adListPgs", adListPgs);
        qr.addQueryOption("limit", limit);
        qr.addQueryOption("skip", skip);

        return createOkResponse(qr);
    }


    @GET
    @Path("/graphic")
    @Produces("application/json")
    @ApiOperation(value = "Get data graphic from prs")
    public Response getPrs(
            @ApiParam(value = "idPgs", required = true) @NotNull @QueryParam("idPgs") String idPgs,
            @ApiParam(value = "sequencingType") @QueryParam("sequencingType") String sequencingType,
            @ApiParam(value = "diseases") @QueryParam("diseases") String diseases
            ) {
        MutableLong count = new MutableLong(-1);
        List<PgsGraphic> prsGraphics = null;
        List<Integer> diseaseList = new ArrayList<>();

        try {
            if (diseases != null && diseases.length() > 0) {
                String[] disSplits = diseases.split(",");
                for (String d : disSplits) {
                    diseaseList.add(Integer.valueOf(d));
                }
            }
            prsGraphics = qm.getGraphicPRS( idPgs, sequencingType, diseaseList, count );
        } catch (Exception e1) {
            return  createErrorResponse(e1);
        }

        QueryResponse qr = createQueryResponse(prsGraphics);
        qr.setNumTotalResults(count.getValue());

        qr.addQueryOption("idPgs", idPgs);
        if (sequencingType != null) {
            qr.addQueryOption("sequencingType", sequencingType);
        }
        if (diseases != null) {
            qr.addQueryOption("diseases", diseases);
        }

        return createOkResponse(qr);
    }


    @GET
    @Path("/ancestry/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get prs")
    public Response getAncestries(
            @ApiParam(value = "ad") @QueryParam("ad") @DefaultValue("") String ad
    ) {
        MutableLong count = new MutableLong(-1);
        List<String> ancestries = null;

        try {
            ancestries = qm.getAncestries(ad, count);
        } catch (Exception e1){
            return  createErrorResponse(e1);
        }

        QueryResponse qr = createQueryResponse(ancestries);
        qr.setNumTotalResults(count.getValue());

        qr.addQueryOption("ad", ad);

        return createOkResponse(qr);
    }

    @GET
    @Path("/diseases")
    @Produces("application/json")
    @ApiOperation(value = "List diseases")
    public Response getAllDiseasesPRS() {

        List<DiseaseGroup> res = qm.getAllDiseasePRS();

        QueryResponse qr = createQueryResponse(res);
        qr.setNumResults(qr.getNumTotalResults());

        return createOkResponse(qr);

    }
}
