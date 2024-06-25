var CSVSMethylationGVColumns = {
     columns : [{

               name: 'chromosome',
               title: "Chr",
               disableSort: true,
               width: 40
           }, {
               name: 'position',
               title: 'Position',
               disableSort: true,
               width: 70
           }, {
               name: 'alleles',
               title: 'Alleles',
               width: 50,
               disableSort: true,
               formula: function(row) {
                   return row.reference + ">" + row.alternate;
               }
           }, {
               name: 'genes',
               title: 'Gene',
               width: 100,
               defaultValue: ".",
               disableSort: true,
               disableTooltip: true,
               formula: function(row){
                   if (row.genes != "" && row.genes != "." && row.genes !=  undefined) {
                       var genes = row.genes.split(',');
                       var result=[];
                       genes.forEach(
                           function(g) {
                               result.push("<a class='link' href='https://www.genecards.org/cgi-bin/carddisp.pl?gene=" + g  +"' target='_blank' " +
                                   "title=" + g + ">"+g+"</a>");
                           }
                       );
                       return result.join(",");
                   } else
                       return row.genes;
           }
           }, {
               name: 'id',
               title: 'Id',
               width: 90,
               defaultValue: ".",
               disableSort: true,
               disableTooltip: true,
               formula: function(row){
                   if (row.id != undefined) {
                       if (row.id != "" && row.id != ".")
                           return "<a class='link' href='https://www.ncbi.nlm.nih.gov/snp/" + row.id + "' target='_blank' " +
                               "title=" + row.id + ">" + row.id + "</a>";
                       else
                           return row.id;
                   } else
                       return ".";
               }
           },{
               title: '<span title="Genotype counts:\n 0/0: homozygous reference\n 0/1: heterozygous\n 1/1: homozygous alterntive\n ./.: missing">Genotypes <sup><i class="fa fa-info-circle aria-hidden="true"></i></sup></span>',
               name: 'genotypes',
               width: 200,
               columns: [{
                   name: "gt00",
                   width: 50,
                   title: '0/0',
                   disableSort: true,
                   formula: function(row) {
                       if (row.stats != null && row.stats.gt00 != null)
                           return row.stats.gt00;
                       else
                           return ".";
                       }
               }, {
                   name: "gt01",
                   width: 50,
                   title: '0/1',
                   disableSort: true,
                   formula: function(row) {
                       if (row.stats != null && row.stats.gt01 != null)
                           return row.stats.gt01;
                       else
                           return ".";
                       }
               }, {
                   name: "gt11",
                   width: 50,
                   title: '1/1',
                   formula: function(row) {
                       if (row.stats != null && row.stats.gt11 != null)
                           return row.stats.gt11;
                       else
                           return ".";
                       }
               }, {
                   name: "gtmissing",
                   width: 50,
                   title: './.',
                   disableSort: true,
                   formula: function(row) {
                       if (row.stats != null && row.stats.gtmissing != null)
                           return row.stats.gtmissing;
                       else
                           return ".";
                   }
               }]
           }, {
               title: '<span title="Allele Frequency:\n 0 Freq: allele frequency for reference\n 1 Freq: allele frequency for alternative\n MAF: Minor Allele Frequency, the lowest value between 0 Freq and 1 Freq">Freq. <sup><i class="fa fa-info-circle aria-hidden="true"></i></sup></span>',
               name: "freq",
               width: 150,
               disableSort: true,
               columns: [{
                   name: "refFreq",
                   width: 50,
                   title:   '0 Freq',
                   formula: function(row) {
                       if (row.stats != null && row.stats.refFreq != null)
                           return row.stats.refFreq;
                       else
                           return ".";
                   }
               }, {
                   name: "altFreq",
                   width: 50,
                   title: '1 Freq',
                   disableSort: true,
                   formula: function(row) {
                       if (row.stats != null && row.stats.altFreq != null)
                           return row.stats.altFreq;
                       else
                           return ".";
                   }
               }, {
                   name: "maf",
                   width: 50,
                   title: 'MAF',
                   disableSort: true,
                   formula: function(row) {
                       if (row.stats != null && row.stats.maf != null)
                           return row.stats.maf;
                       else
                           return ".";
                   }
               }]

                }, {
                   title: "<span title='Alternate Allele Frequency in 1000 genomes project database (phase 3)'>1000G AAF (phase 3) <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
                   width: 150,
                   name: 'aaf1000G3',
                   columns: [{
                       name: "aaf1000G_PHASE_3_ALL",
                       title: "ALL",
                       defaultValue: ".",
                       disableSort: true,
                       formula: function(row) {
                           if (row.aaf1000G_PHASE_3_ALL != null) {
                               return row.aaf1000G_PHASE_3_ALL;
                           } else {
                               return ".";
                           }
                       },
                   }, {
                       name: "aaf1000G_PHASE_3_AMR",
                       title:  "<span title='AMR: American '>AMR <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
                       defaultValue: ".",
                       visible: false,
                       disableSort: true,
                       formula: function(row) {
                           if (row.aaf1000G_PHASE_3_AMR != null) {
                               return row.aaf1000G_PHASE_3_AMR;
                           } else {
                               return ".";
                           }
                       },

                   }, {
                       name: "aaf1000G_PHASE_3_SAS",
                       title:  "<span title='SAS: South Asian'>SAS <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
                       defaultValue: ".",
                       visible: false,
                       formula: function(row) {
                           if (row.aaf1000G_PHASE_3_SAS != null) {
                               return row.aaf1000G_PHASE_3_SAS;
                           } else {
                               return ".";
                           }
                       }
                   }, {
                       name: "aaf1000G_PHASE_3_EAS",
                       title:  "<span title='EAS: East Asian'>EAS <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
                       defaultValue: ".",
                       visible: false,
                       disableSort: true,
                       formula: function(row) {
                           if (row.aaf1000G_PHASE_3_EAS != null) {
                               return row.aaf1000G_PHASE_3_EAS;
                           } else {
                               return ".";
                           }
                       }
                   }, {
                       name: "aaf1000G_PHASE_3_AFR",
                       title: "<span title='AFR: African'>AFR <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
                       defaultValue: ".",
                       visible: false,
                       disableSort: true,
                       formula: function(row) {
                           if (row.aaf1000G_PHASE_3_AFR != null) {
                               return row.aaf1000G_PHASE_3_AFR;
                           } else {
                               return ".";
                           }
                       }
                   }, {
                       name: "aaf1000G_PHASE_3_EUR",
                       title: "<span title='EUR: European'>EUR <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
                       defaultValue: ".",
                       disableSort: true,
                       formula: function(row) {
                           if (row.aaf1000G_PHASE_3_EUR != null) {
                               return row.aaf1000G_PHASE_3_EUR;
                           } else {
                               return ".";
                           }
                       },
                   } ]

               }, {
                   title: "<span title='Alternate Allele Frequency in Exome Aggregation Consortium (ExAC) database'>ExAC AAF <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
                   width: 65,
                   name: 'aafexac',
                   columns: [{
                           name: "aafexac_ALL",
                           title: "ALL",
                           defaultValue: ".",
                           disableSort: true,
                           formula: function(row) {
                               if (row.aafexac_ALL != null) {
                                   return row.aafexac_ALL;
                               } else {
                                   return ".";
                               }
                           }
                       }
                   ]
               }, {
                   title: "<span title='Alternate Allele Frequency in Exome Sequencing Project (ESP) database'>ESP6500 AAF <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
                   width: 100,
                   name: 'aaf6500',
                   columns: [{
                       name: "aafESP_ALL",
                       title: "ALL",
                       defaultValue: ".",
                       disableSort: true,
                       formula: function (row) {
                           if (row.aafESP_ALL != null) {
                               return row.aafESP_ALL;
                           } else {
                               return ".";
                           }
                       }
                   }, {
                       name: "aafESP_EA",
                       title: "<span title='EA: European American'>EA <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
                       defaultValue: ".",
                       disableSort: true,
                       formula: function (row) {
                           if (row.aafESP_EA != null) {
                               return row.aafESP_EA;
                           } else {
                               return ".";
                           }
                       }
               }]
           },  {
               title: "<span title='Alternate Allele Frequency in Genome Aggregation Database (gnomAD)'>gnomAD AAF <sup><i class=\"fa fa-info-circle\" aria-hidden=\"true\"></i></sup></span>",
               width: 130,
               name: 'gnomAD',
               columns: [{
                   name: "aafGNOMAD_GEN_ALL",
                   title: "GENOME ALL",
                   defaultValue: ".",
                   disableSort: true,
                   formula: function (row) {
                       if (row.aafGNOMAD_GEN_ALL != null) {
                           return row.aafGNOMAD_GEN_ALL;
                       } else {
                           return ".";
                       }
                   }
               }, {
                   name: "aafGNOMAD_EXO_ALL",
                   title: "EXOME ALL",
                   defaultValue: ".",
                   disableSort: true,
                   formula: function (row) {
                       if (row.aafGNOMAD_EXO_ALL != null) {
                           return row.aafGNOMAD_EXO_ALL;
                       } else {
                           return ".";
                       }
                   }
               }]
           }, {
               name: 'sift',
               title: '<span title="SIFT score predicts whether an amino acid substitution affects protein function. SIFT value less than 0.05 represents a \'deleterious\' prediction. SIFT value greater than or equal to 0.05 represents a \'tolerated\' prediction">SIFT <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               width: 50,
               disableSort: true,
               formula: function(row) {
                   if (row.sift !== null && typeof row.sift !== 'undefined') {
                       return row.sift;
                   } else {
                       return ".";
                   }
               },
               styleFormula: function(row) {
                   return CSVSSearchColumns._getStyleFormula(row.sift, 'sift')
               }
           },  {
               name: 'polyphen',
               title: '<span title="Polyphen score predicts the possible impact of an aninoacid subsitution on the structure and function of a protein. Polyphen scores can be benign (<0.446), possibly damaging (0.446-0.908) or probably damaging (>0.908)">Polyphen <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               width: 60,
               disableSort: true,
               formula: function(row) {
                   if (row.polyphen !== null  && typeof row.polyphen !== 'undefined') {
                       return row.polyphen;
                   } else {
                       return ".";
                   }
               },
               styleFormula: function(row) {
                   return CSVSSearchColumns._getStyleFormula(row.polyphen, 'polyphen')
               }
           }, {
               name: 'phastcons',
               title: '<span title="phastCons scores represent probabilities of negative selection and range between 0 and 1">phastCons <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               visible: false,
               width: 60,
               disableSort: true,
               formula: function(row) {
                   if (row.phastcons !== null  && typeof row.phastcons !== 'undefined') {
                       return row.phastcons;
                   } else {
                       return ".";
                   }
               },
           }, {
               name: 'phylop',
               title: '<span title="phyloP scores  measure the level of conservation of positions. Positive scores measure conservation whereas negative scores measure acceleration">phyloP <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               visible: false,
               width: 50,
               disableSort: true,
               formula: function(row) {
                   if (row.phylop !== null  && typeof row.phylop !== 'undefined') {
                       return row.phylop;
                   } else {
                       return ".";
                   }
               }
           }, {
               name: 'gerp',
               title: '<span title="GERP score estimates the level of conservation of positions. Positive scores represents a substitution deficit and this indicate that a site may be under evolutionary constraint. Negative scores indicate that a site is probably evolving neutrally. Some author suggest that scores >=2 indicate evolutionary constraint and >=3 indicate purifying selection">GERP <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               width: 50,
               disableSort: true,
               formula: function(row) {
                   if (row.gerp !== null  && typeof row.gerp !== 'undefined') {
                       return row.gerp;
                   } else {
                       return ".";
                   }
               },
               styleFormula: function(row) {
                   return CSVSSearchColumns._getStyleFormula(row.gerp,"gerp")
               }
           }, {
               name: 'cadd',
               title: '<span title="CADD tool scores the deleteriousness of snvs and indels. Higher values indicate more likely to have deleterious effects">CADD <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               width: 50,
               disableSort: true,
               formula: function(row) {
                   if (row.cadd !== null  && typeof row.cadd !== 'undefined') {
                       return row.cadd;
                   } else {
                       return ".";
                   }
               },
               styleFormula: function(row) {
                   return CSVSSearchColumns._getStyleFormula(row.cadd, 'cadd')
               }
           }, {
               name: 'ct',
               title: '<span title="Worst consequence type found among all transcripts">Worst consequence type <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               width: 150,
               disableSort: true,
               formula: function (row) {
                   return !!row && !!row.annots && row.annots.displayConsequenceType ? row.annots.displayConsequenceType: "";
               },
               styleFormula: function(row) {
                   return !!row && !!row.annots && row.annots.displayConsequenceType ? CSVSSearchColumns._getStyleFormulaConsequence(row.annots.displayConsequenceType, 'ct') : {};
               }
           }, {
               name: 'phenotypes',
               title: '<span title="Information about relationships among human variations and Clinvar and Cosmic databases">Phenotypes <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               //width: 450,
               width: 300,
               columns: [{
                   name: 'clinvar',
                   title: 'Clinvar',
                   width: 150,
                   disableSort: true,
                   formula: function(row) {
                       try {
                           return row.phenotypes.clinvar;
                       } catch (e) {
                           return ".";
                       }
                   }
               }, {
                   name: 'cosmic',
                   title: 'Cosmic',
                   width: 150,
                   disableSort: true,
                   formula: function(row) {
                       try {
                           return row.phenotypes.cosmic;
                       } catch (e) {
                           return ".";
                       }

                   }
               }]
           }, {
               name: 'pharmacogenomics',
               title: '<span title=" Exist information about pharmacogenomics databases (https://www.pharmgkb.org/)">Pharmacogenomics <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               width: 150,
               defaultValue: "",
               disableSort: true,
               disableTooltip: true,
               formula: function (row) {
                   try {
                       if (row.pharma != undefined && row.pharma.length > 0) {
                           var result = [];
                           row.pharma.forEach(
                               function (p) {
                                   if (p.starAllele != null) {
                                       var search = p.gene + (p.starAllele.startsWith("*") ? "" : " ") + p.starAllele;
                                       if (p.starAllelePA != null) {
                                           result.push("<a class='link' href='https://www.pharmgkb.org/haplotype/" + p.starAllelePA + "' target='_blank' " +
                                               "title = \"" + search + "\">" + p.starAllele + " (" + p.r + ">" + p.a + ")" + "</a>");
                                       } else {

                                           result.push("<a class='link' href='https://www.pharmgkb.org/search?type=haplotype&query=" + search + "' target='_blank' " +
                                               "title = \"" + search + "\">" + p.starAllele + " (" + p.r + ">" + p.a + ")" + "</a>");
                                       }
                                   } else {
                                       return ".";
                                   }
                               }
                           );
                           return result.join(", ");
                       } else {
                           return "";
                       }
                   } catch (e) {
                       return "";
                   }
               }
           }, {
               name: 'secondaryFindings',
               title: '<span title=" For variants labeled as secondary finding according to ACMG recommendations, phenotype and OMIM disorder related with the gene are indicated, as well as the inheritance model for the disease (AD, autosomal dominant; AR, autosomal recessive; SD, semidominant; XL, X-linked). External link to the OMIM disorder is available if you click on the text (see documentation for more information)">Secondary findings <sup><i class="fa fa-info-circle" aria-hidden="true"></i></sup></span>',
               width: 150,
               defaultValue: "",
               disableSort: true,
               disableTooltip: true,
               formula: function (row) {
                   try {
                       if (row.secFindings != undefined && row.secFindings.length > 0) {
                           var result = [];
                           var tooltip=[]
                           row.secFindings.forEach(
                               function (sf) {
                                   result.push("(" + sf.inheritance + ") ");
                                   sf.omims.forEach(
                                       function (sfOmim) {
                                           var titleSF = "(" + sf.inheritance + ") " + "OMIM:" + sfOmim + " - " + sf.genericPhenotype + ", " +  sf.gene ;
                                           tooltip.push(titleSF);
                                           result.push("<a class='link' href='https://www.omim.org/entry/" + sfOmim + "' target='_blank' " +
                                           "title = \"" + titleSF + "\">" +
                                           "OMIM:" + sfOmim + "</a>");
                                       }
                                   )
                                   result.push(sf.genericPhenotype);
                               }
                           );
                           return "<span title=\"" + tooltip.join("; ")+ "\">" + result.join(", ") + "</span>";
                       } else {
                           return "";
                       }
                   } catch (e) {
                       return "";
                   }
               }
           }
       ],


        _getStyleFormula: function(row, field) {
            var style = {};

            var obj = null;
            if (row != undefined && row != null) {
                for (var i = 0; i < this.highlights.length && obj == null; i++) {
                    obj = !!this.highlights[i].groupValues && this.highlights[i].groupValues.find(h  => h.name === field);
                }

                if (obj != null && obj != undefined && !!row && !!obj.value) {
                    if (eval(row + " " + obj.op + " " + obj.value)) {
                        style.color = '#fff';
                        style.backgroundColor = '#f47070';
                    }
                }
            }
            return style;
        },

        _getStyleFormulaConsequence: function(row, field){
            var style = {};

            var obj = null;

            for (var i = 0; i < this.highlightsSO.groupValues.length && obj == null ; i++) {
                if ("category" in this.highlightsSO.groupValues[i]) {
                    obj = this.highlightsSO.groupValues[i].ops.find(h => h.name === row && h.checked);
                } else {
                     obj = this.highlightsSO.groupValues[i].ops.find(h => h.name === row && h.checked);
                }
            }

            if (obj != null && obj!= undefined && !!row){
                style.color = this.getColor(obj.impact);
            }
            return style;
        },

        getColor: function(impact){

            switch(impact){
                case "HIGH":
                    return  "red";

                case "MODERATE":
                    return "#ED760E";

                case "LOW":
                    return "blue";

                case "MODIFIER":
                    return "green";

                default:
                    return "grey";
            }
        },
}