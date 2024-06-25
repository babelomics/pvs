package org.babelomics.csvs.server;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.babelomics.csvs.lib.models.Methylation;
import org.babelomics.csvs.lib.models.MethylationFilter;
import org.babelomics.csvs.lib.ws.QueryResponse;
import org.opencb.biodata.models.feature.Region;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Path("/methylation")
@Api(value = "methylation", description = "Methylation")
@Produces(MediaType.APPLICATION_JSON)
public class MethylationWSServer extends CSVSWSServer {

    public MethylationWSServer(@DefaultValue("") @PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr)
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
    @Path("/filters")
    @Produces("application/json")
    @ApiOperation(value = "List methylation filters")
    public Response getAllFilters(
            @ApiParam(value = "filter") @QueryParam("filter") @DefaultValue("") String filter
            ) {
        List<MethylationFilter> res = qm.getMethylationFilter(filter);

        QueryResponse qr = createQueryResponse(res);
        qr.setNumResults(qr.getNumTotalResults());

        return createOkResponse(qr);
    }

    @GET
    @Path("/fetch")
    @Produces("application/json")
    @ApiOperation(value = "Get methylation (graphic)")
    public Response getMethylation(
            @ApiParam(value = "regions") @QueryParam("regions") @DefaultValue("") String regions,
            @ApiParam(value = "diseases") @QueryParam("diseases") @DefaultValue("") String diseases,
            @ApiParam(value = "technologies") @QueryParam("technologies") @DefaultValue("") String technologies,
            @ApiParam(value = "tissues") @QueryParam("tissues") @DefaultValue("") String tissues,
            @ApiParam(value = "genders") @QueryParam("genders") @DefaultValue("") String genders,
            @ApiParam(value = "age") @QueryParam("age") @DefaultValue("") String age,
            @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("-1") int limit,
            @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip,
            @ApiParam(value = "skipCount") @QueryParam("skipCount") @DefaultValue("false") boolean skipCount
    ) {
        List<Region> regionList = new ArrayList<>();
        List<String> diseaseList = new ArrayList<>();
        List<String> technologyList = new ArrayList<>();
        List<String>  tissueList  = new ArrayList<>();
        List<String>  genderList = new ArrayList<>();
        Integer ageMin = null, ageMax = null;
        int regionsSize = 0;

        if (regions.length() > 0) {
            String[] regSplits = regions.split(",");
            for (String s : regSplits) {
                Region r = Region.parseRegion(s);
                regionList.add(r);
                regionsSize += r.getEnd() - r.getStart();
            }
        }
        if (diseases.length() > 0) {
            String[] splits = diseases.split(",",-1);
            if (splits != null && splits.length > 0){
                diseaseList.addAll (Arrays.asList(splits));
            }
        }

        if (technologies.length() > 0) {
            String[] splits = technologies.split(",",-1);
            if (splits != null && splits.length > 0){
                technologyList.addAll (Arrays.asList(splits));
            }
        }

        if (tissues.length() > 0) {
            String[] splits = tissues.split(",",-1);
            if (splits != null && splits.length > 0){
                tissueList.addAll (Arrays.asList(splits));
            }
        }

        if (genders.length() > 0) {
            String[] splits = genders.split(",",-1);
            if (splits != null && splits.length > 0){
                genderList.addAll (Arrays.asList(splits));
            }
        }

        if (age.length() > 1 && age.contains("-")) {
            String[] splits = age.split("-",-1);
            if (splits != null && splits[0].length() > 0){
                try {
                    ageMin = Integer.parseInt(splits[0]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            if (splits != null && splits[1].length() > 1){
                try {
                    ageMax = Integer.parseInt(splits[1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        MutableLong count = new MutableLong(-1);

        Iterable<Methylation> meth = null;

        try {
            meth = qm.getMethylation(regionList, diseaseList, technologyList, tissueList, genderList, ageMin, ageMax, skip, limit, skipCount, count);
        } catch (Exception e1){
            return  createErrorResponse(e1);
        }

        QueryResponse qr = createQueryResponse(meth);
        qr.setNumTotalResults(count.getValue());

        qr.addQueryOption("regions", regions);
        qr.addQueryOption("diseases", diseases);
        qr.addQueryOption("technologies", technologies);
        qr.addQueryOption("tissues",  tissues);
        qr.addQueryOption("genders", genders);
        qr.addQueryOption("age", age);
        qr.addQueryOption("limit", limit);
        qr.addQueryOption("skip", skip);

        return createOkResponse(qr);
    }


    @GET
    @Path("/annotation")
    @Produces("application/json")
    @ApiOperation(value = "Get methylation annotation (avg, tdDev ...")
    public Response getMethylationAnnotation(
            @ApiParam(value = "regions") @QueryParam("regions") @DefaultValue("") String regions,
            @ApiParam(value = "diseases") @QueryParam("diseases") @DefaultValue("") String diseases,
            @ApiParam(value = "technologies") @QueryParam("technologies") @DefaultValue("") String technologies,
            @ApiParam(value = "tissues") @QueryParam("tissues") @DefaultValue("") String tissues,
            @ApiParam(value = "genders") @QueryParam("genders") @DefaultValue("") String genders,
            @ApiParam(value = "age") @QueryParam("age") @DefaultValue("") String age,
            @ApiParam(value = "all") @QueryParam("all") @DefaultValue("false") boolean all,
            @ApiParam(value = "limit") @QueryParam("limit") @DefaultValue("-1") int limit,
            @ApiParam(value = "skip") @QueryParam("skip") @DefaultValue("0") int skip,
            @ApiParam(value = "skipCount") @QueryParam("skipCount") @DefaultValue("false") boolean skipCount
    ) {
        List<Region> regionList = new ArrayList<>();
        List<String> diseaseList = new ArrayList<>();
        List<String> technologyList = new ArrayList<>();
        List<String>  tissueList  = new ArrayList<>();
        List<String>  genderList = new ArrayList<>();
        Integer ageMin = null, ageMax = null;

        int regionsSize = 0;

        if (regions.length() > 0) {
            String[] regSplits = regions.split(",");
            for (String s : regSplits) {
                Region r = Region.parseRegion(s);
                regionList.add(r);
                regionsSize += r.getEnd() - r.getStart();
            }
        }
        if (diseases.length() > 0) {
            String[] splits = diseases.split(",",-1);
            if (splits != null && splits.length > 0){
                diseaseList.addAll (Arrays.asList(splits));
            }
        }
        if (technologies.length() > 0) {
            String[] splits = technologies.split(",", -1);
            if (splits != null && splits.length > 0){
               technologyList.addAll (Arrays.asList(splits));
            }
        }

        if (tissues.length() > 0) {
            String[] splits = tissues.split(",", -1);
            if (splits != null && splits.length > 0){
                tissueList.addAll (Arrays.asList(splits));
            }
        }

        if (genders.length() > 0) {
            String[] splits = genders.split(",", -1);
            if (splits != null && splits.length > 0){
                genderList.addAll (Arrays.asList(splits));
            }
        }
        if (age.length() > 0) {
            String[] splits = age.split("-",-1);
            if (splits != null && splits[0].length() > 0){
                try {
                    ageMin = Integer.parseInt(splits[0]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            if (splits != null && splits[1].length() > 1){
                try {
                    ageMax = Integer.parseInt(splits[1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        MutableLong count = new MutableLong(-1);

        Iterable<Methylation> meth = null;

        try {
            meth = qm.getAnnotationMethylation(regionList, diseaseList, technologyList, tissueList, genderList, ageMin, ageMax, all, skip, limit, skipCount, count);
        } catch (Exception e1) {
            return  createErrorResponse(e1);
        }

        QueryResponse qr = createQueryResponse(meth);
        qr.setNumTotalResults(count.getValue());

        qr.addQueryOption("regions", regions);
        qr.addQueryOption("diseases", diseases);
        qr.addQueryOption("technologies", technologies);
        qr.addQueryOption("tissues",  tissues);
        qr.addQueryOption("genders", genders);
        qr.addQueryOption("age", age);
        qr.addQueryOption("limit", limit);
        qr.addQueryOption("skip", skip);

        return createOkResponse(qr);
    }
}
