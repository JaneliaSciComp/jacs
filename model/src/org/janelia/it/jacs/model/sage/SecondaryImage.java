package org.janelia.it.jacs.model.sage;

// Generated Jan 14, 2014 11:00:59 AM by Hibernate Tools 3.4.0.CR1

import java.io.Serializable;
import java.util.Date;

/**
 * SecondaryImage generated by hbm2java
 */
public class SecondaryImage implements Serializable {

    private Integer id;
    private Image image;
    private CvTerm productType;
    private String name;
    private String path;
    private String url;
    private Date createDate;

    public SecondaryImage() {
    }

    public SecondaryImage(Image image, CvTerm productType, String name, String path, String url, Date createDate) {
        this.image = image;
        this.productType = productType;
        this.name = name;
        this.path = path;
        this.url = url;
        this.createDate = createDate;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Image getImage() {
        return this.image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public CvTerm getProductType() {
        return this.productType;
    }

    public void setProductType(CvTerm productType) {
        this.productType = productType;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

}
