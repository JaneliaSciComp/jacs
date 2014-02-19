//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.02.18 at 02:42:09 PM EST 
//


package org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _BlastOutputIterations_QNAME = new QName("", "BlastOutput_iterations");
    private final static QName _BlastOutputParam_QNAME = new QName("", "BlastOutput_param");
    private final static QName _BlastOutput_QNAME = new QName("", "BlastOutput");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link HspType }
     * 
     */
    public HspType createHspType() {
        return new HspType();
    }

    /**
     * Create an instance of {@link Statistics }
     * 
     */
    public Statistics createStatistics() {
        return new Statistics();
    }

    /**
     * Create an instance of {@link Iteration }
     * 
     */
    public Iteration createIteration() {
        return new Iteration();
    }

    /**
     * Create an instance of {@link HitType }
     * 
     */
    public HitType createHitType() {
        return new HitType();
    }

    /**
     * Create an instance of {@link Hit }
     * 
     */
    public Hit createHit() {
        return new Hit();
    }

    /**
     * Create an instance of {@link ParametersType }
     * 
     */
    public ParametersType createParametersType() {
        return new ParametersType();
    }

    /**
     * Create an instance of {@link IterationType }
     * 
     */
    public IterationType createIterationType() {
        return new IterationType();
    }

    /**
     * Create an instance of {@link BlastOutputType }
     * 
     */
    public BlastOutputType createBlastOutputType() {
        return new BlastOutputType();
    }

    /**
     * Create an instance of {@link StatisticsType }
     * 
     */
    public StatisticsType createStatisticsType() {
        return new StatisticsType();
    }

    /**
     * Create an instance of {@link Parameters }
     * 
     */
    public Parameters createParameters() {
        return new Parameters();
    }

    /**
     * Create an instance of {@link Hsp }
     * 
     */
    public Hsp createHsp() {
        return new Hsp();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Iteration }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "BlastOutput_iterations")
    public JAXBElement<Iteration> createBlastOutputIterations(Iteration value) {
        return new JAXBElement<Iteration>(_BlastOutputIterations_QNAME, Iteration.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Parameters }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "BlastOutput_param")
    public JAXBElement<Parameters> createBlastOutputParam(Parameters value) {
        return new JAXBElement<Parameters>(_BlastOutputParam_QNAME, Parameters.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BlastOutputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "BlastOutput")
    public JAXBElement<BlastOutputType> createBlastOutput(BlastOutputType value) {
        return new JAXBElement<BlastOutputType>(_BlastOutput_QNAME, BlastOutputType.class, null, value);
    }

}
