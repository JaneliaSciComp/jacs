
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.10.13 at 01:45:00 PM EDT 
//


package org.janelia.it.jacs.server.jaxb.reference_record.impl;

public class AnyTypeImpl implements org.janelia.it.jacs.server.jaxb.reference_record.AnyType, com.sun.xml.bind.JAXBObject, org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.UnmarshallableObject, org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.XMLSerializable, org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.ValidatableObject {

    protected com.sun.xml.bind.util.ListImpl _Content;
    public final static java.lang.Class version = (org.janelia.it.jacs.server.jaxb.reference_record.impl.JAXBVersion.class);
    private static com.sun.msv.grammar.Grammar schemaFragment;

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (org.janelia.it.jacs.server.jaxb.reference_record.AnyType.class);
    }

    protected com.sun.xml.bind.util.ListImpl _getContent() {
        if (_Content == null) {
            _Content = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _Content;
    }

    public java.util.List getContent() {
        return _getContent();
    }

    public org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.UnmarshallingEventHandler createUnmarshaller(org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.UnmarshallingContext context) {
        return new org.janelia.it.jacs.server.jaxb.reference_record.impl.AnyTypeImpl.Unmarshaller(context);
    }

    public void serializeBody(org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.XMLSerializer context)
            throws org.xml.sax.SAXException {
        int idx1 = 0;
        final int len1 = ((_Content == null) ? 0 : _Content.size());
        while (idx1 != len1) {
            {
                java.lang.Object o = _Content.get(idx1);
                if (o instanceof java.lang.String) {
                    try {
                        context.text(((java.lang.String) _Content.get(idx1++)), "Content");
                    }
                    catch (java.lang.Exception e) {
                        org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.Util.handlePrintConversionException(this, e, context);
                    }
                }
                else {
                    if (o instanceof java.lang.Object) {
                        context.childAsBody(((com.sun.xml.bind.JAXBObject) _Content.get(idx1++)), "Content");
                    }
                    else {
                        org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.Util.handleTypeMismatchError(context, this, "Content", o);
                    }
                }
            }
        }
    }

    public void serializeAttributes(org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.XMLSerializer context)
            throws org.xml.sax.SAXException {
        int idx1 = 0;
        final int len1 = ((_Content == null) ? 0 : _Content.size());
        while (idx1 != len1) {
            {
                java.lang.Object o = _Content.get(idx1);
                if (o instanceof java.lang.String) {
                    try {
                        idx1 += 1;
                    }
                    catch (java.lang.Exception e) {
                        org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.Util.handlePrintConversionException(this, e, context);
                    }
                }
                else {
                    if (o instanceof java.lang.Object) {
                        idx1 += 1;
                    }
                    else {
                        org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.Util.handleTypeMismatchError(context, this, "Content", o);
                    }
                }
            }
        }
    }

    public void serializeURIs(org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.XMLSerializer context)
            throws org.xml.sax.SAXException {
        int idx1 = 0;
        final int len1 = ((_Content == null) ? 0 : _Content.size());
        while (idx1 != len1) {
            {
                java.lang.Object o = _Content.get(idx1);
                if (o instanceof java.lang.String) {
                    try {
                        idx1 += 1;
                    }
                    catch (java.lang.Exception e) {
                        org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.Util.handlePrintConversionException(this, e, context);
                    }
                }
                else {
                    if (o instanceof java.lang.Object) {
                        idx1 += 1;
                    }
                    else {
                        org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.Util.handleTypeMismatchError(context, this, "Content", o);
                    }
                }
            }
        }
    }

    public java.lang.Class getPrimaryInterface() {
        return (org.janelia.it.jacs.server.jaxb.reference_record.AnyType.class);
    }

    public com.sun.msv.verifier.DocumentDeclaration createRawValidator() {
        if (schemaFragment == null) {
            schemaFragment = com.sun.xml.bind.validator.SchemaDeserializer.deserialize((
                    "\u00ac\u00ed\u0000\u0005sr\u0000\u001ccom.sun.msv.grammar.MixedExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001ccom.sun.m"
                            + "sv.grammar.UnaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\u0003expt\u0000 Lcom/sun/msv/grammar/"
                            + "Expression;xr\u0000\u001ecom.sun.msv.grammar.Expression\u00f8\u0018\u0082\u00e8N5~O\u0002\u0000\u0002L\u0000\u0013e"
                            + "psilonReducibilityt\u0000\u0013Ljava/lang/Boolean;L\u0000\u000bexpandedExpq\u0000~\u0000\u0002x"
                            + "pppsr\u0000\u001dcom.sun.msv.grammar.ChoiceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.sun.m"
                            + "sv.grammar.BinaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0004exp1q\u0000~\u0000\u0002L\u0000\u0004exp2q\u0000~\u0000\u0002xq\u0000~\u0000"
                            + "\u0003ppsr\u0000 com.sun.msv.grammar.OneOrMoreExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0001sr\u0000\u0011"
                            + "java.lang.Boolean\u00cd r\u0080\u00d5\u009c\u00fa\u00ee\u0002\u0000\u0001Z\u0000\u0005valuexp\u0000psr\u0000\'com.sun.msv.gram"
                            + "mar.trex.ElementPattern\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\tnameClasst\u0000\u001fLcom/sun/ms"
                            + "v/grammar/NameClass;xr\u0000\u001ecom.sun.msv.grammar.ElementExp\u0000\u0000\u0000\u0000\u0000\u0000"
                            + "\u0000\u0001\u0002\u0000\u0002Z\u0000\u001aignoreUndeclaredAttributesL\u0000\fcontentModelq\u0000~\u0000\u0002xq\u0000~\u0000\u0003"
                            + "q\u0000~\u0000\fp\u0000sr\u0000 com.sun.msv.grammar.AttributeExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0003exp"
                            + "q\u0000~\u0000\u0002L\u0000\tnameClassq\u0000~\u0000\u000exq\u0000~\u0000\u0003ppsr\u00002com.sun.msv.grammar.Expres"
                            + "sion$AnyStringExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003sq\u0000~\u0000\u000b\u0001q\u0000~\u0000\u0014sr\u0000 com"
                            + ".sun.msv.grammar.AnyNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.sun.msv.gram"
                            + "mar.NameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpsr\u0000&com.sun.msv.grammar.NamespaceN"
                            + "ameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\fnamespaceURIt\u0000\u0012Ljava/lang/String;xq\u0000~\u0000"
                            + "\u0017t\u0000+http://java.sun.com/jaxb/xjc/dummy-elementssr\u00000com.sun.m"
                            + "sv.grammar.Expression$EpsilonExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003q\u0000~\u0000"
                            + "\u0015q\u0000~\u0000\u001esr\u0000\"com.sun.msv.grammar.ExpressionPool\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\bex"
                            + "pTablet\u0000/Lcom/sun/msv/grammar/ExpressionPool$ClosedHash;xpsr"
                            + "\u0000-com.sun.msv.grammar.ExpressionPool$ClosedHash\u00d7j\u00d0N\u00ef\u00e8\u00ed\u001c\u0003\u0000\u0003I\u0000"
                            + "\u0005countB\u0000\rstreamVersionL\u0000\u0006parentt\u0000$Lcom/sun/msv/grammar/Expre"
                            + "ssionPool;xp\u0000\u0000\u0000\u0003\u0001pq\u0000~\u0000\bq\u0000~\u0000\nq\u0000~\u0000\u0005x"));
        }
        return new com.sun.msv.verifier.regexp.REDocumentDeclaration(schemaFragment);
    }

    public class Unmarshaller
            extends org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.AbstractUnmarshallingEventHandlerImpl {


        public Unmarshaller(org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.UnmarshallingContext context) {
            super(context, "-");
        }

        protected Unmarshaller(org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return org.janelia.it.jacs.server.jaxb.reference_record.impl.AnyTypeImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
                throws org.xml.sax.SAXException {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case 0:
                        if (true) {
                            java.lang.Object co = spawnWildcard(0, ___uri, ___local, ___qname, __atts);
                            if (co != null) {
                                _getContent().add(co);
                            }
                            return;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return;
                }
                super.enterElement(___uri, ___local, ___qname, __atts);
                break;
            }
        }

        public void leaveElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
                throws org.xml.sax.SAXException {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case 0:
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return;
                }
                super.leaveElement(___uri, ___local, ___qname);
                break;
            }
        }

        public void enterAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
                throws org.xml.sax.SAXException {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case 0:
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
                        return;
                }
                super.enterAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void leaveAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
                throws org.xml.sax.SAXException {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case 0:
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
                        return;
                }
                super.leaveAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void handleText(final java.lang.String value)
                throws org.xml.sax.SAXException {
            int attIdx;
            outer:
            while (true) {
                try {
                    switch (state) {
                        case 0:
                            state = 0;
                            eatText1(value);
                            return;
                    }
                }
                catch (java.lang.RuntimeException e) {
                    handleUnexpectedTextException(value, e);
                }
                break;
            }
        }

        private void eatText1(final java.lang.String value)
                throws org.xml.sax.SAXException {
            try {
                _getContent().add(value);
            }
            catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

    }

}
