package org.babelomics.csvs.lib.models.prs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import java.util.List;
/**
 * @author grg.
 */
@Entity(noClassnameStored = true)
public class PrsGraphic {

    public static String GENOME = "G";
    public static String EXOME = "E";

    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("idPgs")
    private String idPgs;

    @Property("gid")
    private String gid;

    @Property("exome")
    private Double exome;

    @Property("genome")
    private Double genome;

    public PrsGraphic() {
    }

    public String getIdPgs() {
        return idPgs;
    }

    public void setIdPgs(String idPgs) {
        this.idPgs = idPgs;
    }

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public Double getExome() {
        return exome;
    }

    public void setExome(Double exome) {
        this.exome = exome;
    }

    public Double getGenome() {
        return genome;
    }

    public void setGenome(Double genome) {
        this.genome = genome;
    }

    @Override
    public String toString() {
        return "PrsGraphic{" +
                "id=" + id +
                ", idPgs='" + idPgs + '\'' +
                ", gid='" + gid + '\'' +
                ", exome=" + exome +
                ", genome=" + genome +
                '}';
    }
}
