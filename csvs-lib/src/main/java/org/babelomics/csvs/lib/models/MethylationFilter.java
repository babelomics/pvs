package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Property;

/**
 * @author grg
 */
@Entity(noClassnameStored = true)
public class MethylationFilter {

    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("n")
    private String name;

    @Indexed(name = "index_methylation_fid", unique = true)
    @Property("fid")
    private String fId;

    // @Transient
    @Property("s")
    private Integer samples;

    @Property("filter")
    private String filter;

    @Property("order")
    private Integer order;

    public MethylationFilter() {
        this.samples = 0;
    }

    public MethylationFilter(String fId, String name) {
        this();
        this.fId = fId;
        this.name = name;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getfId() {
        return fId;
    }

    public void setfId(String fId) {
        this.fId = fId;
    }

    public Integer getSamples() {
        return samples;
    }

    public void setSamples(Integer samples) {
        this.samples = samples;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "MethylationFilter{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", fId=" + fId +
                ", samples=" + samples +
                ", filter=" + filter +
                ", order=" + order +
                '}';
    }
}
