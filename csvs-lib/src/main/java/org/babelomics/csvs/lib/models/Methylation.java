package org.babelomics.csvs.lib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.Map;

/**
 * @author GRG
 */
@Entity( value ="Methylation", noClassnameStored = true)
@Indexes({
        @Index(name = "index_methylation_chr_pos", fields = {@Field("c"),@Field("p")}),
        @Index(name = "index_code", fields = {@Field("code")})
})
public class Methylation {

    @JsonIgnore
    @Id
    private ObjectId id;

    @Property("c")
    private String chromosome;

    @Property("p")
    private int position;

    @Property("value")
    private String value;

    @Property("s")
    private String sample;

    @Property("code")
    private String code;

    @Property("d")
    private String subpopulation;

    @Property("tech")
    private String technology;

    @Property("gender")
    private String gender;

    @Property("tissue")
    private String tissue;

    @Property("age")
    private Integer age;

    @Property("an")
    private Map<String, Object> annots;

    public Methylation() {
    }

    public Methylation(String chromosome, int position) {
        this();
        this.chromosome = chromosome;
        this.position = position;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSubpopulation() {
        return subpopulation;
    }

    public void setSubpopulation(String subpopulation) {
        this.subpopulation = subpopulation;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getTissue() {
        return tissue;
    }

    public void setTissue(String tissue) {
        this.tissue = tissue;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Map<String, Object> getAnnots() {
        return annots;
    }

    public void setAnnots(Map<String, Object> annots) {
        this.annots = annots;
    }

    @Override
    public String toString() {
        return "Methylation{" +
                "chromosome='" + chromosome + '\'' +
                ", position=" + position +
                ", value='" + value + '\'' +
                ", sample='" + sample + '\'' +
                ", code='" + code + '\'' +
                ", subpopulation='" + subpopulation + '\'' +
                ", technology='" + technology + '\'' +
                ", gender='" + gender + '\'' +
                ", tissue='" + tissue + '\'' +
                ", age='" + age + '\'' +
            "}";
    }
}
