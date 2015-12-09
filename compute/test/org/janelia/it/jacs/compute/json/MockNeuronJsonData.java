/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.json;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * This "test" will produce a lot of Mock JSon data, sufficient for parse/
 * production testing.
 * @author fosterl
 */
public class MockNeuronJsonData implements Serializable {
    private long id;
    private String name;
    private Date createDate;

    /**
     * @return the geoAnnotations
     */
    public List<GeoAnnotation> getGeoAnnotations() {
        return geoAnnotations;
    }

    /**
     * @param geoAnnotations the geoAnnotations to set
     */
    public void setGeoAnnotations(List<GeoAnnotation> geoAnnotations) {
        this.geoAnnotations = geoAnnotations;
    }

    /**
     * @return the structuredTextAnnotations
     */
    public List<StructuredTextAnnotation> getStructuredTextAnnotations() {
        return structuredTextAnnotations;
    }

    /**
     * @param structuredTextAnnotations the structuredTextAnnotations to set
     */
    public void setStructuredTextAnnotations(List<StructuredTextAnnotation> structuredTextAnnotations) {
        this.structuredTextAnnotations = structuredTextAnnotations;
    }

    /**
     * @return the anchoredPaths
     */
    public List<AnchoredPath> getAnchoredPaths() {
        return anchoredPaths;
    }

    /**
     * @param anchoredPaths the anchoredPaths to set
     */
    public void setAnchoredPaths(List<AnchoredPath> anchoredPaths) {
        this.anchoredPaths = anchoredPaths;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the creationDate
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     * @param creationDate the creationDate to set
     */
    public void setCreateDate(Date creationDate) {
        this.createDate = creationDate;
    }

    public static class GeoAnnotation implements Serializable {
        private long id;
        private long parentId;
        private double radius;
        private double x,y,z;

        /**
         * @return the id
         */
        public long getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(long id) {
            this.id = id;
        }

        /**
         * @return the parentId
         */
        public long getParentId() {
            return parentId;
        }

        /**
         * @param parentId the parentId to set
         */
        public void setParentId(long parentId) {
            this.parentId = parentId;
        }

        /**
         * @return the radius
         */
        public double getRadius() {
            return radius;
        }

        /**
         * @param radius the radius to set
         */
        public void setRadius(double radius) {
            this.radius = radius;
        }

        /**
         * @return the x
         */
        public double getX() {
            return x;
        }

        /**
         * @param x the x to set
         */
        public void setX(double x) {
            this.x = x;
        }

        /**
         * @return the y
         */
        public double getY() {
            return y;
        }

        /**
         * @param y the y to set
         */
        public void setY(double y) {
            this.y = y;
        }

        /**
         * @return the z
         */
        public double getZ() {
            return z;
        }

        /**
         * @param z the z to set
         */
        public void setZ(double z) {
            this.z = z;
        }
    }
    
    private List<GeoAnnotation> geoAnnotations;
    
    public static class StructuredTextAnnotation implements Serializable {
        private long id;
        private long parentId;
        private int parentType;
        private int fmtVersion;
        private String data;

        /**
         * @return the id
         */
        public long getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(long id) {
            this.id = id;
        }

        /**
         * @return the parentId
         */
        public long getParentId() {
            return parentId;
        }

        /**
         * @param parentId the parentId to set
         */
        public void setParentId(long parentId) {
            this.parentId = parentId;
        }

        /**
         * @return the parentType
         */
        public int getParentType() {
            return parentType;
        }

        /**
         * @param parentType the parentType to set
         */
        public void setParentType(int parentType) {
            this.parentType = parentType;
        }

        /**
         * @return the fmtVersion
         */
        public int getFmtVersion() {
            return fmtVersion;
        }

        /**
         * @param fmtVersion the fmtVersion to set
         */
        public void setFmtVersion(int fmtVersion) {
            this.fmtVersion = fmtVersion;
        }

        /**
         * @return the data
         */
        public String getData() {
            return data;
        }

        /**
         * @param data the data to set
         */
        public void setData(String data) {
            this.data = data;
        }
    }
    
    private List<StructuredTextAnnotation> structuredTextAnnotations;

    public static class AnchoredPath implements Serializable {

        /**
         * @return the endPoints
         */
        public EndPoints getEndPoints() {
            return endPoints;
        }

        /**
         * @param endPoints the endPoints to set
         */
        public void setEndPoints(EndPoints endPoints) {
            this.endPoints = endPoints;
        }

        /**
         * @return the points
         */
        public double[][] getPoints() {
            return points;
        }

        /**
         * @param points the points to set
         */
        public void setPoints(double[][] points) {
            this.points = points;
        }
        public static class EndPoints {
            private double x1;
            private double y1;
            private double z1;
            private double x2;
            private double y2;
            private double z2;

            /**
             * @return the x1
             */
            public double getX1() {
                return x1;
            }

            /**
             * @param x1 the x1 to set
             */
            public void setX1(double x1) {
                this.x1 = x1;
            }

            /**
             * @return the y1
             */
            public double getY1() {
                return y1;
            }

            /**
             * @param y1 the y1 to set
             */
            public void setY1(double y1) {
                this.y1 = y1;
            }

            /**
             * @return the z1
             */
            public double getZ1() {
                return z1;
            }

            /**
             * @param z1 the z1 to set
             */
            public void setZ1(double z1) {
                this.z1 = z1;
            }

            /**
             * @return the x2
             */
            public double getX2() {
                return x2;
            }

            /**
             * @param x2 the x2 to set
             */
            public void setX2(double x2) {
                this.x2 = x2;
            }

            /**
             * @return the y2
             */
            public double getY2() {
                return y2;
            }

            /**
             * @param y2 the y2 to set
             */
            public void setY2(double y2) {
                this.y2 = y2;
            }

            /**
             * @return the z2
             */
            public double getZ2() {
                return z2;
            }

            /**
             * @param z2 the z2 to set
             */
            public void setZ2(double z2) {
                this.z2 = z2;
            }
        }
        private EndPoints endPoints;
        private double[][] points;
    }
    
    private List<AnchoredPath> anchoredPaths;
}
