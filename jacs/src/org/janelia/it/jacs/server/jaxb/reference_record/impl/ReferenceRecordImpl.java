
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.10.27 at 12:05:11 PM EDT 
//


package org.janelia.it.jacs.server.jaxb.reference_record.impl;

import com.sun.msv.grammar.Grammar;
import com.sun.msv.verifier.DocumentDeclaration;
import com.sun.msv.verifier.regexp.REDocumentDeclaration;
import com.sun.xml.bind.JAXBObject;
import com.sun.xml.bind.RIElement;
import com.sun.xml.bind.validator.SchemaDeserializer;
import org.janelia.it.jacs.server.jaxb.reference_record.ReferenceRecord;
import org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.AbstractUnmarshallingEventHandlerImpl;
import org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.UnmarshallableObject;
import org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.UnmarshallingContext;
import org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.UnmarshallingEventHandler;
import org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.ValidatableObject;
import org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.XMLSerializable;
import org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.lang.Class;
import java.lang.Object;
import java.lang.RuntimeException;
import java.lang.String;

public class ReferenceRecordImpl
        extends ReferenceRecordTypeImpl
        implements ReferenceRecord, RIElement, JAXBObject, UnmarshallableObject, XMLSerializable, ValidatableObject {

    public final static Class version = (JAXBVersion.class);
    private static Grammar schemaFragment;

    private final static Class PRIMARY_INTERFACE_CLASS() {
        return (ReferenceRecord.class);
    }

    public String ____jaxb_ri____getNamespaceURI() {
        return "";
    }

    public String ____jaxb_ri____getLocalName() {
        return "reference_record";
    }

    public UnmarshallingEventHandler createUnmarshaller(UnmarshallingContext context) {
        return new ReferenceRecordImpl.Unmarshaller(context);
    }

    public void serializeBody(XMLSerializer context)
            throws SAXException {
        context.startElement("", "reference_record");
        super.serializeURIs(context);
        context.endNamespaceDecls();
        super.serializeAttributes(context);
        context.endAttributes();
        super.serializeBody(context);
        context.endElement();
    }

    public void serializeAttributes(XMLSerializer context)
            throws SAXException {
    }

    public void serializeURIs(XMLSerializer context)
            throws SAXException {
    }

    public Class getPrimaryInterface() {
        return (ReferenceRecord.class);
    }

    public DocumentDeclaration createRawValidator() {
        if (schemaFragment == null) {
            schemaFragment = SchemaDeserializer.deserialize((
                    "\u00ac\u00ed\u0000\u0005sr\u0000\'com.sun.msv.grammar.trex.ElementPattern\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000"
                            + "\tnameClasst\u0000\u001fLcom/sun/msv/grammar/NameClass;xr\u0000\u001ecom.sun.msv."
                            + "grammar.ElementExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002Z\u0000\u001aignoreUndeclaredAttributesL\u0000"
                            + "\fcontentModelt\u0000 Lcom/sun/msv/grammar/Expression;xr\u0000\u001ecom.sun."
                            + "msv.grammar.Expression\u00f8\u0018\u0082\u00e8N5~O\u0002\u0000\u0002L\u0000\u0013epsilonReducibilityt\u0000\u0013Lj"
                            + "ava/lang/Boolean;L\u0000\u000bexpandedExpq\u0000~\u0000\u0003xppp\u0000sr\u0000\u001fcom.sun.msv.gra"
                            + "mmar.SequenceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.sun.msv.grammar.BinaryExp"
                            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0004exp1q\u0000~\u0000\u0003L\u0000\u0004exp2q\u0000~\u0000\u0003xq\u0000~\u0000\u0004ppsq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0007pps"
                            + "q\u0000~\u0000\u0007ppsq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~"
                            + "\u0000\u0007ppsr\u0000\u001bcom.sun.msv.grammar.DataExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\u0002dtt\u0000\u001fLorg/r"
                            + "elaxng/datatype/Datatype;L\u0000\u0006exceptq\u0000~\u0000\u0003L\u0000\u0004namet\u0000\u001dLcom/sun/ms"
                            + "v/util/StringPair;xq\u0000~\u0000\u0004ppsr\u0000#com.sun.msv.datatype.xsd.Strin"
                            + "gType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001Z\u0000\risAlwaysValidxr\u0000*com.sun.msv.datatype.xsd"
                            + ".BuiltinAtomicType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000%com.sun.msv.datatype.xsd.Co"
                            + "ncreteType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\'com.sun.msv.datatype.xsd.XSDatatype"
                            + "Impl\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\fnamespaceUrit\u0000\u0012Ljava/lang/String;L\u0000\btypeNa"
                            + "meq\u0000~\u0000\u001cL\u0000\nwhiteSpacet\u0000.Lcom/sun/msv/datatype/xsd/WhiteSpaceP"
                            + "rocessor;xpt\u0000 http://www.w3.org/2001/XMLSchemat\u0000\u0006stringsr\u00005c"
                            + "om.sun.msv.datatype.xsd.WhiteSpaceProcessor$Preserve\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001"
                            + "\u0002\u0000\u0000xr\u0000,com.sun.msv.datatype.xsd.WhiteSpaceProcessor\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002"
                            + "\u0000\u0000xp\u0001sr\u00000com.sun.msv.grammar.Expression$NullSetExpression\u0000\u0000\u0000"
                            + "\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0004ppsr\u0000\u001bcom.sun.msv.util.StringPair\u00d0t\u001ejB\u008f\u008d\u00a0\u0002\u0000\u0002L\u0000"
                            + "\tlocalNameq\u0000~\u0000\u001cL\u0000\fnamespaceURIq\u0000~\u0000\u001cxpq\u0000~\u0000 q\u0000~\u0000\u001fsr\u0000\u001dcom.sun.m"
                            + "sv.grammar.ChoiceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\bppsr\u0000 com.sun.msv.gramm"
                            + "ar.AttributeExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0003expq\u0000~\u0000\u0003L\u0000\tnameClassq\u0000~\u0000\u0001xq\u0000~\u0000\u0004"
                            + "sr\u0000\u0011java.lang.Boolean\u00cd r\u0080\u00d5\u009c\u00fa\u00ee\u0002\u0000\u0001Z\u0000\u0005valuexp\u0000psq\u0000~\u0000\u0014ppsr\u0000\"com."
                            + "sun.msv.datatype.xsd.QnameType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0019q\u0000~\u0000\u001ft\u0000\u0005QName"
                            + "sr\u00005com.sun.msv.datatype.xsd.WhiteSpaceProcessor$Collapse\u0000\u0000\u0000"
                            + "\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\"q\u0000~\u0000%sq\u0000~\u0000&q\u0000~\u00001q\u0000~\u0000\u001fsr\u0000#com.sun.msv.grammar.S"
                            + "impleNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\tlocalNameq\u0000~\u0000\u001cL\u0000\fnamespaceURIq\u0000~"
                            + "\u0000\u001cxr\u0000\u001dcom.sun.msv.grammar.NameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpt\u0000\u0004typet\u0000)ht"
                            + "tp://www.w3.org/2001/XMLSchema-instancesr\u00000com.sun.msv.gramm"
                            + "ar.Expression$EpsilonExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0004sq\u0000~\u0000,\u0001q\u0000~\u0000;"
                            + "sq\u0000~\u00005t\u0000\u0005titlet\u0000\u0000sq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000(ppsr\u0000 com.s"
                            + "un.msv.grammar.OneOrMoreExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001ccom.sun.msv.gramma"
                            + "r.UnaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\u0003expq\u0000~\u0000\u0003xq\u0000~\u0000\u0004q\u0000~\u0000-psq\u0000~\u0000*q\u0000~\u0000-psr\u00002"
                            + "com.sun.msv.grammar.Expression$AnyStringExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000"
                            + "\u0000xq\u0000~\u0000\u0004q\u0000~\u0000<q\u0000~\u0000Isr\u0000 com.sun.msv.grammar.AnyNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
                            + "\u0001\u0002\u0000\u0000xq\u0000~\u00006q\u0000~\u0000;sq\u0000~\u00005t\u0000Lorg.jcvi.camera.server.jaxb.referenc"
                            + "e_record.ReferenceRecordType.AuthorsTypet\u0000+http://java.sun.c"
                            + "om/jaxb/xjc/dummy-elementssq\u0000~\u0000(ppsq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000.q\u0000~\u00007q\u0000~\u0000"
                            + ";sq\u0000~\u00005t\u0000\u0007authorsq\u0000~\u0000?sq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000(ppsq\u0000~"
                            + "\u0000Dq\u0000~\u0000-psq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000Iq\u0000~\u0000Kq\u0000~\u0000;sq\u0000~\u00005t\u0000Porg.jcvi.camera."
                            + "server.jaxb.reference_record.ReferenceRecordType.Publication"
                            + "Typeq\u0000~\u0000Nsq\u0000~\u0000(ppsq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000.q\u0000~\u00007q\u0000~\u0000;sq\u0000~\u00005t\u0000\u000bpublica"
                            + "tionq\u0000~\u0000?sq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000\u0017sq\u0000~\u0000(ppsq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000.q\u0000~\u0000"
                            + "7q\u0000~\u0000;sq\u0000~\u00005t\u0000\u0007summaryq\u0000~\u0000?sq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000\u0017sq\u0000~\u0000(ppsq\u0000"
                            + "~\u0000*q\u0000~\u0000-pq\u0000~\u0000.q\u0000~\u00007q\u0000~\u0000;sq\u0000~\u00005t\u0000\babstractq\u0000~\u0000?sq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000"
                            + "\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000(ppsq\u0000~\u0000Dq\u0000~\u0000-psq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000Iq\u0000~\u0000Kq\u0000~\u0000;s"
                            + "q\u0000~\u00005t\u00005org.jcvi.camera.server.jaxb.reference_record.TextTyp"
                            + "eq\u0000~\u0000Nsq\u0000~\u0000(ppsq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000.q\u0000~\u00007q\u0000~\u0000;sq\u0000~\u00005t\u0000\tfull_textq"
                            + "\u0000~\u0000?sq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000(ppsq\u0000~\u0000Dq\u0000~\u0000-psq\u0000~\u0000*q\u0000~\u0000"
                            + "-pq\u0000~\u0000Iq\u0000~\u0000Kq\u0000~\u0000;sq\u0000~\u00005q\u0000~\u0000rq\u0000~\u0000Nsq\u0000~\u0000(ppsq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000.q\u0000"
                            + "~\u00007q\u0000~\u0000;sq\u0000~\u00005t\u0000\u0011supplemental_textq\u0000~\u0000?sq\u0000~\u0000(ppsq\u0000~\u0000\u0000q\u0000~\u0000-p\u0000"
                            + "sq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000(ppsq\u0000~\u0000Dq\u0000~\u0000-psq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000Iq\u0000~\u0000Kq"
                            + "\u0000~\u0000;sq\u0000~\u00005t\u0000Qorg.jcvi.camera.server.jaxb.reference_record.Re"
                            + "ferenceRecordType.CombinedDataTypeq\u0000~\u0000Nsq\u0000~\u0000(ppsq\u0000~\u0000*q\u0000~\u0000-pq"
                            + "\u0000~\u0000.q\u0000~\u00007q\u0000~\u0000;sq\u0000~\u00005t\u0000\rcombined_dataq\u0000~\u0000?q\u0000~\u0000;sq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000"
                            + "\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000(ppsq\u0000~\u0000Dq\u0000~\u0000-psq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000Iq\u0000~\u0000Kq\u0000~\u0000;s"
                            + "q\u0000~\u00005t\u0000Iorg.jcvi.camera.server.jaxb.reference_record.Referen"
                            + "ceRecordType.DataTypeq\u0000~\u0000Nsq\u0000~\u0000(ppsq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000.q\u0000~\u00007q\u0000~\u0000"
                            + ";sq\u0000~\u00005t\u0000\u0004dataq\u0000~\u0000?sq\u0000~\u0000(ppsq\u0000~\u0000*q\u0000~\u0000-pq\u0000~\u0000.q\u0000~\u00007q\u0000~\u0000;sq\u0000~\u00005"
                            + "t\u0000\u0010reference_recordq\u0000~\u0000?sr\u0000\"com.sun.msv.grammar.ExpressionPo"
                            + "ol\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\bexpTablet\u0000/Lcom/sun/msv/grammar/ExpressionPo"
                            + "ol$ClosedHash;xpsr\u0000-com.sun.msv.grammar.ExpressionPool$Close"
                            + "dHash\u00d7j\u00d0N\u00ef\u00e8\u00ed\u001c\u0003\u0000\u0003I\u0000\u0005countB\u0000\rstreamVersionL\u0000\u0006parentt\u0000$Lcom/sun"
                            + "/msv/grammar/ExpressionPool;xp\u0000\u0000\u0000)\u0001pq\u0000~\u0000\u0082q\u0000~\u0000\u000fq\u0000~\u0000Aq\u0000~\u0000Tq\u0000~\u0000"
                            + "lq\u0000~\u0000xq\u0000~\u0000\u0084q\u0000~\u0000\u0090q\u0000~\u0000\u0013q\u0000~\u0000`q\u0000~\u0000fq\u0000~\u0000Fq\u0000~\u0000Wq\u0000~\u0000oq\u0000~\u0000{q\u0000~\u0000\u0087q\u0000~\u0000"
                            + "\u0093q\u0000~\u0000\u000bq\u0000~\u0000\u0011q\u0000~\u0000\rq\u0000~\u0000\fq\u0000~\u0000\u000eq\u0000~\u0000\tq\u0000~\u0000\nq\u0000~\u0000Cq\u0000~\u0000Vq\u0000~\u0000nq\u0000~\u0000zq\u0000~\u0000"
                            + ")q\u0000~\u0000Oq\u0000~\u0000[q\u0000~\u0000aq\u0000~\u0000gq\u0000~\u0000sq\u0000~\u0000~q\u0000~\u0000\u0086q\u0000~\u0000\u008bq\u0000~\u0000\u0092q\u0000~\u0000\u0097q\u0000~\u0000\u009bq\u0000~\u0000"
                            + "\u0010x"));
        }
        return new REDocumentDeclaration(schemaFragment);
    }

    public class Unmarshaller
            extends AbstractUnmarshallingEventHandlerImpl {


        public Unmarshaller(UnmarshallingContext context) {
            super(context, "----");
        }

        protected Unmarshaller(UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public Object owner() {
            return ReferenceRecordImpl.this;
        }

        public void enterElement(String ___uri, String ___local, String ___qname, Attributes __atts)
                throws SAXException {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case 1:
                        if (("title" == ___local) && ("" == ___uri)) {
                            spawnHandlerFromEnterElement((((ReferenceRecordTypeImpl) ReferenceRecordImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return;
                        }
                        break;
                    case 0:
                        if (("reference_record" == ___local) && ("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return;
                        }
                        break;
                    case 3:
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return;
                }
                super.enterElement(___uri, ___local, ___qname, __atts);
                break;
            }
        }

        public void leaveElement(String ___uri, String ___local, String ___qname)
                throws SAXException {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case 3:
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return;
                    case 2:
                        if (("reference_record" == ___local) && ("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return;
                        }
                        break;
                }
                super.leaveElement(___uri, ___local, ___qname);
                break;
            }
        }

        public void enterAttribute(String ___uri, String ___local, String ___qname)
                throws SAXException {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case 3:
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
                        return;
                }
                super.enterAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void leaveAttribute(String ___uri, String ___local, String ___qname)
                throws SAXException {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case 3:
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
                        return;
                }
                super.leaveAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void handleText(final String value)
                throws SAXException {
            int attIdx;
            outer:
            while (true) {
                try {
                    switch (state) {
                        case 3:
                            revertToParentFromText(value);
                            return;
                    }
                }
                catch (RuntimeException e) {
                    handleUnexpectedTextException(value, e);
                }
                break;
            }
        }

    }

}
