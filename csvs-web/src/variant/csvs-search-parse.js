var CSVSSearchParse = {
        _parse: function(response) {
            var me = this;
            var data = response.result;
            var positions = [];
            var positionsSearch = [];
            var positionsDoubleVariant = [];
            var doubleVariant = [];
            var referencePosition = [];
            var referenceVariant = [];
            //Cellbase use - never * o nothing, we need change, annotate and change
            for (var i = 0; i < data.length; i++) {
                var row = data[i];
                // Add field to copy when dobleclick
                data[i].copy = row.chromosome + ":" + row.position + " " + row.reference + ">" + row.alternate;

                //Add posisition to search pathopedia
                positionsSearch.push(row.chromosome + ":" + row.position + ":" + row.reference + ":" + row.alternate);

                if (row.alternate == "") {
                    positionsDoubleVariant.push(i);
                    doubleVariant.push(row.alternate);
                    row.alternate = "-";
                }

                if (row.alternate != undefined && (row.alternate).indexOf(",") != -1) {
                    positionsDoubleVariant.push(i);
                    doubleVariant.push(row.alternate);
                    row.alternate = row.alternate.split(",")[0];
                    if(row.alternate == ""){
                        row.alternate = "-";
                    }
                }

                if (row.reference == "") {
                    referencePosition.push(i);
                    referenceVariant.push(row.reference);
                    row.reference = "-";
                }

                var variant = row.chromosome + ":" + row.position + ":" + row.reference + ":" + row.alternate;
                positions.push(variant);
            }

            // Pathopedia
            var query = positionsSearch.join(",");
            if (positionsSearch.length > 0) {
                CSVSManager.pathologies.fetch({
                    id: query,
                    request: {
                        async: false,
                        success: function (response) {
                            if (response != undefined && response.numResults > 0) {
                                for (var i = 0; i < response.result.length; i++) {
                                    var obj = response.result[i];
                                    if (obj.total > 0) {
                                        for (var j = 0; j < data.length; j++) {

                                            if (obj.variant.chromosome == data[j].chromosome &&
                                                obj.variant.position == data[j].position) {

                                                var alt = ("alternateIni" in data[j]) ? data[j].alternateIni :  data[j].alternate;
                                                var ref = ("referenceIni" in data[j]) ? data[j].referenceIni :  data[j].reference;
                                                if (ref == "-")
                                                    ref ="";


                                                if (obj.variant.alternate == alt && obj.variant.reference == ref) {

                                                    data[j].pathopedia = {
                                                        "mapTotalTypeOpinion": obj.mapTotalTypeOpinion.valueOf(),
                                                        "total": obj.total,
                                                    };
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        },
                         error: function(response){
                             console.log(response);
                         }
                    }
                });
            }

            // Pharma
            var query = positionsSearch.join(",");
            if (positionsSearch.length > 0) {

                var positionsRs = [];
                for (var i = 0; i < data.length; i++) {
                    positionsRs.push(data[i].ids);
                }

                CSVSManager.pharmacogenomics.annotation({
                    query: { variants: query, rs: positionsRs.join(",")},
                    request: {
                        async: false,
                        success: function (response) {
                            if (response != undefined && response.numResults > 0) {
                                for (var i = 0; i < response.result.length; i++) {
                                    var obj = response.result[i];

                                    for (var j = 0; j < obj.variants.length; j++) {
                                        var objVariant = obj.variants[j];
                                        for (var indexData = 0; indexData < data.length; indexData++) {

                                            if ((objVariant.chromosome == data[indexData].chromosome &&
                                                objVariant.position == data[indexData].position)  || objVariant.ids == data[indexData].ids) {

                                                var alt = ("alternateIni" in data[indexData]) ? data[indexData].alternateIni : data[indexData].alternate;
                                                var ref = ("referenceIni" in data[indexData]) ? data[indexData].referenceIni : data[indexData].reference;
                                                if (ref == "-")
                                                    ref = "";

                                                if (objVariant.reference == ref && (objVariant.alternate == ref || objVariant.alternate == alt)) {
                                                //if ((objVariant.reference == ref) || objVariant.ids == data[indexData].ids) {
                                                    if (data[indexData].pharma == undefined) {
                                                        data[indexData].pharma = [];
                                                    }
                                                     var genePA = obj.pharmaIds != null ? obj.pharmaIds.filter(function(item ){ return item.ty == "gene" && item.name == obj.gene}):null;
                                                     var starAllelePA = obj.pharmaIds != null ? obj.pharmaIds.filter(function(item){return item.ty === "haplotype" && item.name == obj.gene +  (obj.starAllele.startsWith("*") ? "" : " ") + obj.starAllele}) : null;
                                                     var idsPA = obj.pharmaIds != null && data[indexData].ids != null ? obj.pharmaIds.filter(function(item){return item.ty === "variant" && item.name == data[indexData].ids}) : null;

                                                      data[indexData].pharma.push({
                                                        "gene": obj.gene,
                                                        "starAllele": obj.starAllele,
                                                        "r": ref,
                                                        "a": objVariant.alternate,
                                                        "numVariants":  obj.variants != null ? obj.variants.length : "",
                                                        "genePA": genePA != null && genePA.length > 0 ? genePA[0].idPA:null,
                                                        "starAllelePA": starAllelePA != null && starAllelePA.length > 0 ? starAllelePA[0].idPA : null,
                                                        "idsPA": idsPA != null && idsPA.length > 0 ? idsPA[0].idPA : null
                                                    });
                                                }
                                            }
                                        }
                                    }

                                }

                            }
                        },
                         error: function(response){
                             console.log(response);
                         }
                    }
                });
            }

            // Secondary findings
            var query = positionsSearch.join(",");
            if (positionsSearch.length > 0) {
                var positionsRs = [];
                for (var i = 0; i < data.length; i++) {
                    positionsRs.push(data[i].ids);
                }

                CSVSManager.secondaryFindings.annotation({
                    query: { variants: query, rs: positionsRs.join(",")},
                    request: {
                        async: false,
                        success: function (response) {
                            if (response != undefined && response.numResults > 0) {
                                for (var i = 0; i < response.result.length; i++) {
                                    var objVariant = response.result[i];
                                    for (var indexData = 0; indexData < data.length; indexData++) {

                                        if ((objVariant.chromosome == data[indexData].chromosome &&
                                            objVariant.position == data[indexData].position)  || objVariant.ids == data[indexData].ids) {

                                            var alt = ("alternateIni" in data[indexData]) ? data[indexData].alternateIni : data[indexData].alternate;
                                            var ref = ("referenceIni" in data[indexData]) ? data[indexData].referenceIni : data[indexData].reference;
                                            if (ref == "-")
                                                ref = "";
                                            if (alt == "-")
                                                alt = "";

                                            if (objVariant.reference == ref && (objVariant.alternate == alt || objVariant.alternate == alt)) {
                                            //if ((objVariant.reference == ref) || objVariant.ids == data[indexData].ids) {
                                                if (data[indexData].secFindings == undefined) {
                                                    data[indexData].secFindings = [];
                                                }
                                                  data[indexData].secFindings.push({
                                                    "gene": objVariant.gene,
                                                    "r": ref,
                                                    "a": objVariant.alternate,
                                                    "omims" : objVariant.omims,
                                                    "inheritance": objVariant.inheritance,
                                                    "genericPhenotype": objVariant.genericPhenotype
                                                });
                                            }
                                        }
                                    }


                                }

                            }
                        },
                         error: function(response){
                             console.log(response);
                         }
                    }
                });
            }

            // Methylation
            var regionQuery = data.map(d => d.chromosome+":"+d.position+"-"+d.position).join(",");
            if (positionsSearch.length > 0) {
                var positionsMethy = [];
                for (var i = 0; i < data.length; i++) {
                    positionsMethy.push(data[i].ids);
                }

                CSVSManager.methylation.annotation({
                    query: { regions: regionQuery, diseases: response.queryOptions.diseases, all:true },
                    request: {
                        async: false,
                        success: function (response) {
                            if (response != undefined && response.numResults > 0) {
                                for (var i = 0; i < response.result.length; i++) {
                                    var objVMethylation = response.result[i];
                                    for (var indexData = 0; indexData < data.length; indexData++) {
                                        if ((objVMethylation.chromosome == data[indexData].chromosome &&
                                            objVMethylation.position == data[indexData].position)) {
                                                if (data[indexData].annots != null){
                                                    data[indexData].annots = {}
                                                }
                                                data[indexData].annots.methylation = objVMethylation.annots;
                                        }
                                    }
                                }
                            }
                        },
                         error: function(response){
                             console.log(response);
                         }
                    }
                });
            }

            // Annotate
            var query = positions.join(",");
            if (positions.length > 0) {

                CellBaseManager.get({
                    host: CELLBASE_HOST,
                    version: CELLBASE_VERSION,
                    species: 'hsapiens',
                    category: 'genomic',
                    subCategory: 'variant',
                    resource: 'annotation',
                    query: query,
                    async: false,
                    success: function(response) {
                        try {
                            var annotData = response.response;
                            for (var i = 0; i < annotData.length; i++) {
                                var obj = annotData[i];
                                if (obj.result.length > 0) {
                                    var annots = obj.result[0];
                                    if (data[i].annots != null){
                                        data[i].annots = Object.assign(data[i].annots, annots);
                                    } else {
                                        data[i].annots = annots;
                                    }
                                }
                            }
                        } catch (e) {}
                    },
                    error: function(e) {
                        // debugger
                    }
                });
            }

            for (var z = 0; z < positionsDoubleVariant.length; z++) {
                data[positionsDoubleVariant[z]].alternate = doubleVariant[z];
            }
            // Get value "" or "*" from CSVS
            for (var z = 0; z < referencePosition.length; z++) {
                data[referencePosition[z]].reference =  referenceVariant[z];
             }

            for (var i = 0; i < data.length; i++) {
                var variant = data[i];

                if (variant.annots == null) {
                    continue;
                }

                // Annotate SNPid
                if (variant.annots.id != null && variant.annots.id) {
                    variant.id = variant.annots.id;
                }

                // Annotate Cons
                if (variant.annots.conservation != null && variant.annots.conservation.length > 0) {

                    for (var j = 0, l = variant.annots.conservation.length; j < l; j++) {
                        var crs = variant.annots.conservation[j];
                        if (crs) {
                            score = crs.score.toFixed(3)
                            switch (crs.source.toLowerCase()) {
                                case 'phastcons':
                                    variant.phastcons = score;
                                    break;
                                case 'phylop':
                                    variant.phylop = score;
                                    break;
                                case 'gerp':
                                    variant.gerp = score;
                                    break;
                                default:
                            }
                        }
                    }
                }


                // Annotate CT && Subs. Score
                if (variant.annots.consequenceTypes != null && variant.annots.consequenceTypes.length > 0) {

                    var sif = -1;
                    var sifDesc = "";
                    var poly = -1;
                    var polyDesc = "";
                    var genes = [];

                    for (var j = 0, l = variant.annots.consequenceTypes.length; j < l; j++) {
                        var ct = variant.annots.consequenceTypes[j];

                        if (ct != null && ct != "undefined") {
                            if (ct.geneName != null && ct.geneName.length > 0 && genes.indexOf(ct.geneName) < 0) {
                                genes.push(ct.geneName);
                            }

                            if (ct.proteinVariantAnnotation != null && ct.proteinVariantAnnotation.substitutionScores != null && ct.proteinVariantAnnotation.substitutionScores.length > 0) {
                                var auxSift = null;
                                var auxPoly = null;
                                for (var k = 0, lP = ct.proteinVariantAnnotation.substitutionScores.length; k < lP; k++) {
                                    var pss = ct.proteinVariantAnnotation.substitutionScores[k];
                                    switch (pss.source.toLowerCase()) {
                                        case 'sift':
                                            auxSift = pss;
                                            break;
                                        case 'polyphen':
                                            auxPoly = pss;
                                            break;
                                    }
                                }

                                if (auxPoly != null && auxSift != null) {
                                    if (auxPoly.score > poly) {
                                        sif = auxSift.score;
                                        poly = auxPoly.score;

                                        sifDesc = auxSift.description;
                                        polyDesc = auxPoly.description;

                                    }
                                }

                            }
                        }
                    }
                    if (sif != -1 && poly != -1) {
                        variant.sift = sif;
                        variant.polyphen = poly;
                    }

                    if (genes.length > 0) {
                        variant.genes = genes.join(",");
                    }
                }

                // Functional Score
                if (variant.annots.functionalScore != null && variant.annots.functionalScore.length > 0) {
                    var cadd = null;
                    for (var j = 0; j < variant.annots.functionalScore.length; j++) {
                        var cadd_elem = variant.annots.functionalScore[j];
                        switch (cadd_elem.source.toLowerCase()) {
                            case 'cadd_raw':
                                //cadd = cadd_elem.score.toFixed(3);
                                break;
                            // it is used in prioritization
                            case 'cadd_scaled':
                                cadd = cadd_elem.score.toFixed(3);
                                break;
                        }
                    }
                    if (cadd != null) {
                        variant.cadd = cadd;
                    }
                }

                // Clinical
                variant.phenotypes = {
                    cosmic: "",
                    gwas: "",
                    clinvar: ""
                };

                if (variant.annots.variantTraitAssociation != null) {
                    var clinical = variant.annots.variantTraitAssociation;

                    var phenotypesCosmic = [];
                    var phenotypesGwas = [];
                    var phenotypesClinvar = [];
                    if (clinical.cosmic != null) {
                        for (var j = 0; j < clinical.cosmic.length; j++) {
                            var cosmic = clinical.cosmic[j];
                            if (cosmic.primaryHistology != null && cosmic.primaryHistology != "") {
                                phenotypesCosmic.push(cosmic.primaryHistology);
                            }
                        }
                    }

                    if (clinical.gwas != null) {
                        for (var j = 0; j < clinical.gwas.length; j++) {
                            var gwas = clinical.gwas[j];
                            for (var k = 0; k < gwas.traits.length; k++) {
                                var trait = gwas.traits[k];
                                if (trait != null && trait != "") {
                                    phenotypesGwas.push(trait);
                                }
                            }
                        }
                    }

                    if (clinical.clinvar != null) {
                        for (var j = 0; j < clinical.clinvar.length; j++) {
                            var clinvar = clinical.clinvar[j];
                            for (var k = 0; k < clinvar.traits.length; k++) {
                                var trait = clinvar.traits[k];
                                if (trait != null && trait != "") {
                                    phenotypesClinvar.push(trait);
                                }
                            }
                        }
                    }

                    var phenotypesCosmicFinal = phenotypesCosmic.filter(function(item, pos) {
                        return phenotypesCosmic.indexOf(item) == pos;
                    });
                    var phenotypesGwasFinal = phenotypesGwas.filter(function(item, pos) {
                        return phenotypesGwas.indexOf(item) == pos;
                    });
                    var phenotypesClinvarFinal = phenotypesClinvar.filter(function(item, pos) {
                        return phenotypesClinvar.indexOf(item) == pos;
                    });


                    variant.phenotypes = {
                        cosmic: phenotypesCosmicFinal.join(","),
                        gwas: phenotypesGwasFinal.join(","),
                        clinvar: phenotypesClinvarFinal.join(",")
                    };
                }

                if (variant.annots.populationFrequencies != null) {

                    for (var j = 0; j < variant.annots.populationFrequencies.length; j++) {

                        var pfreq = variant.annots.populationFrequencies[j];

                        if (pfreq.study.toLowerCase() === "1kg_phase3" ||
                            pfreq.study === "1kG_phase3_chrMT" ||
                            pfreq.study === "1kG_phase3_chrY"
                        ) {
                            switch (pfreq.population) {
                                case "ALL":
                                    variant.aaf1000G_PHASE_3_ALL = Number(pfreq.altAlleleFreq).toFixed(3);
                                    break;
                                case "SAS":
                                    variant.aaf1000G_PHASE_3_SAS = Number(pfreq.altAlleleFreq).toFixed(3)
                                    break;
                                case "EAS":
                                    variant.aaf1000G_PHASE_3_EAS = Number(pfreq.altAlleleFreq).toFixed(3)
                                    break;
                                case "AMR":
                                    variant.aaf1000G_PHASE_3_AMR = Number(pfreq.altAlleleFreq).toFixed(3)
                                    break;
                                case "AFR":
                                    variant.aaf1000G_PHASE_3_AFR = Number(pfreq.altAlleleFreq).toFixed(3)
                                    break;
                                case "EUR":
                                    variant.aaf1000G_PHASE_3_EUR = Number(pfreq.altAlleleFreq).toFixed(3);
                                    break;

                            }
                        } else if (pfreq.study.toLowerCase() === "esp6500") {
                            switch (pfreq.population) {
                                case "EA":
                                    variant.aafESP_EA = Number(pfreq.altAlleleFreq).toFixed(3);
                                    break;
                                case "AA":
                                    variant.aafESP_AA = Number(pfreq.altAlleleFreq).toFixed(3);
                                    break;
                                case "ALL":
                                    variant.aafESP_ALL = Number(pfreq.altAlleleFreq).toFixed(3);
                                    break;


                            }
                        } else if (pfreq.study.toLowerCase() === "exac") {
                            switch (pfreq.population) {
                                case "ALL":
                                    variant.aafexac_ALL = Number(pfreq.altAlleleFreq).toFixed(3);
                                    break;
                                case "EUR":
                                    variant.aafexac_EUR = Number(pfreq.altAlleleFreq).toFixed(3)
                                    break;
                            }
                        } else if (pfreq.study.toLowerCase() === "gnomad_genomes") {
                            switch (pfreq.population) {
                                case "ALL":
                                    variant.aafGNOMAD_GEN_ALL = Number(pfreq.altAlleleFreq).toFixed(3);
                                    break;
                            }
                        }   else if (pfreq.study.toLowerCase() === "gnomad_exomes") {
                            switch (pfreq.population) {
                                case "ALL":
                                    variant.aafGNOMAD_EXO_ALL = Number(pfreq.altAlleleFreq).toFixed(3);
                                    break;
                            }
                        }
                    }
                }
            }

            return data;
        },
        _parseTotal: function(response) {
            if (response != null) {
                var total = response.numTotalResults;
            } else {
                total = 0;
            }
            return total;
        },
}