//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.02.18 at 02:42:09 PM EST 
//


package org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for BlastOutputType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BlastOutputType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="BlastOutput_program" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="BlastOutput_version" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="BlastOutput_reference" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="BlastOutput_db" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="BlastOutput_query-ID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="BlastOutput_query-def" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="BlastOutput_query-len" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *         &lt;element name="BlastOutput_query-seq" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="BlastOutput_param" type="{}Parameters"/>
 *         &lt;element name="BlastOutput_iterations" type="{}Iteration"/>
 *         &lt;element name="BlastOutput_mbstat" type="{}Statistics" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BlastOutputType", propOrder = {
    "blastOutputProgram",
    "blastOutputVersion",
    "blastOutputReference",
    "blastOutputDb",
    "blastOutputQueryID",
    "blastOutputQueryDef",
    "blastOutputQueryLen",
    "blastOutputQuerySeq",
    "blastOutputParam",
    "blastOutputIterations",
    "blastOutputMbstat"
})
public class BlastOutputType {

    @XmlElement(name = "BlastOutput_program", required = true)
    protected String blastOutputProgram;
    @XmlElement(name = "BlastOutput_version", required = true)
    protected String blastOutputVersion;
    @XmlElement(name = "BlastOutput_reference", required = true)
    protected String blastOutputReference;
    @XmlElement(name = "BlastOutput_db", required = true)
    protected String blastOutputDb;
    @XmlElement(name = "BlastOutput_query-ID", required = true)
    protected String blastOutputQueryID;
    @XmlElement(name = "BlastOutput_query-def", required = true)
    protected String blastOutputQueryDef;
    @XmlElement(name = "BlastOutput_query-len", required = true)
    protected BigInteger blastOutputQueryLen;
    @XmlElement(name = "BlastOutput_query-seq")
    protected String blastOutputQuerySeq;
    @XmlElement(name = "BlastOutput_param", required = true)
    protected Parameters blastOutputParam;
    @XmlElement(name = "BlastOutput_iterations", required = true)
    protected Iteration blastOutputIterations;
    @XmlElement(name = "BlastOutput_mbstat")
    protected Statistics blastOutputMbstat;

    /**
     * Gets the value of the blastOutputProgram property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBlastOutputProgram() {
        return blastOutputProgram;
    }

    /**
     * Sets the value of the blastOutputProgram property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBlastOutputProgram(String value) {
        this.blastOutputProgram = value;
    }

    /**
     * Gets the value of the blastOutputVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBlastOutputVersion() {
        return blastOutputVersion;
    }

    /**
     * Sets the value of the blastOutputVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBlastOutputVersion(String value) {
        this.blastOutputVersion = value;
    }

    /**
     * Gets the value of the blastOutputReference property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBlastOutputReference() {
        return blastOutputReference;
    }

    /**
     * Sets the value of the blastOutputReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBlastOutputReference(String value) {
        this.blastOutputReference = value;
    }

    /**
     * Gets the value of the blastOutputDb property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBlastOutputDb() {
        return blastOutputDb;
    }

    /**
     * Sets the value of the blastOutputDb property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBlastOutputDb(String value) {
        this.blastOutputDb = value;
    }

    /**
     * Gets the value of the blastOutputQueryID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBlastOutputQueryID() {
        return blastOutputQueryID;
    }

    /**
     * Sets the value of the blastOutputQueryID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBlastOutputQueryID(String value) {
        this.blastOutputQueryID = value;
    }

    /**
     * Gets the value of the blastOutputQueryDef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBlastOutputQueryDef() {
        return blastOutputQueryDef;
    }

    /**
     * Sets the value of the blastOutputQueryDef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBlastOutputQueryDef(String value) {
        this.blastOutputQueryDef = value;
    }

    /**
     * Gets the value of the blastOutputQueryLen property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getBlastOutputQueryLen() {
        return blastOutputQueryLen;
    }

    /**
     * Sets the value of the blastOutputQueryLen property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setBlastOutputQueryLen(BigInteger value) {
        this.blastOutputQueryLen = value;
    }

    /**
     * Gets the value of the blastOutputQuerySeq property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBlastOutputQuerySeq() {
        return blastOutputQuerySeq;
    }

    /**
     * Sets the value of the blastOutputQuerySeq property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBlastOutputQuerySeq(String value) {
        this.blastOutputQuerySeq = value;
    }

    /**
     * Gets the value of the blastOutputParam property.
     * 
     * @return
     *     possible object is
     *     {@link Parameters }
     *     
     */
    public Parameters getBlastOutputParam() {
        return blastOutputParam;
    }

    /**
     * Sets the value of the blastOutputParam property.
     * 
     * @param value
     *     allowed object is
     *     {@link Parameters }
     *     
     */
    public void setBlastOutputParam(Parameters value) {
        this.blastOutputParam = value;
    }

    /**
     * Gets the value of the blastOutputIterations property.
     * 
     * @return
     *     possible object is
     *     {@link Iteration }
     *     
     */
    public Iteration getBlastOutputIterations() {
        return blastOutputIterations;
    }

    /**
     * Sets the value of the blastOutputIterations property.
     * 
     * @param value
     *     allowed object is
     *     {@link Iteration }
     *     
     */
    public void setBlastOutputIterations(Iteration value) {
        this.blastOutputIterations = value;
    }

    /**
     * Gets the value of the blastOutputMbstat property.
     * 
     * @return
     *     possible object is
     *     {@link Statistics }
     *     
     */
    public Statistics getBlastOutputMbstat() {
        return blastOutputMbstat;
    }

    /**
     * Sets the value of the blastOutputMbstat property.
     * 
     * @param value
     *     allowed object is
     *     {@link Statistics }
     *     
     */
    public void setBlastOutputMbstat(Statistics value) {
        this.blastOutputMbstat = value;
    }

}
